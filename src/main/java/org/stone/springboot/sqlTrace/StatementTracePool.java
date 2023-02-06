/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.springboot.sqlTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.springboot.SpringRegisterUtil;
import org.stone.springboot.SpringSourceMonitorConfig;
import org.stone.springboot.SpringSourceMonitorManager;
import org.stone.springboot.storage.RedisPushTask;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.util.CommonUtil.isBlank;

/**
 * statement trace manager
 *
 * @author Chris Liao
 */
public class StatementTracePool {
    private final Logger Log = LoggerFactory.getLogger(SpringSourceMonitorManager.class);

    private boolean sqlTrace;
    private boolean sqlShow;
    private long sqlExecSlowTime;
    private int sqlTraceMaxSize;
    private AtomicInteger sqlTracedSize;
    private StatementTraceAlert sqlTraceAlert;
    private ConcurrentLinkedDeque<StatementTrace> sqlTraceQueue;

    public StatementTracePool() {
        this.sqlTraceQueue = new ConcurrentLinkedDeque<StatementTrace>();
    }

    public boolean isSqlTrace() {
        return sqlTrace;
    }

    public void initPool(SpringSourceMonitorConfig config) {
        if (sqlTrace = config.isSqlTrace()) {
            this.sqlShow = config.isSqlShow();
            this.sqlExecSlowTime = config.getSqlExecSlowTime();
            this.sqlTraceMaxSize = config.getSqlTraceMaxSize();
            this.sqlTraceAlert = config.getSqlExecAlertAction();
            this.sqlTracedSize = new AtomicInteger(0);
            this.sqlTraceQueue = new ConcurrentLinkedDeque<>();

            //sql trace timeout scan
            ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(2, new SpringBootDsThreadFactory());
            timerExecutor.setKeepAliveTime(15, TimeUnit.SECONDS);
            timerExecutor.allowCoreThreadTimeOut(true);
            timerExecutor.scheduleAtFixedRate(new SqlTraceTimeoutTask(this, config.getSqlTraceTimeout()),
                    0, config.getSqlTraceTimeoutScanPeriod(), MILLISECONDS);

            String redisHost = config.getRedisHost();
            if (!isBlank(redisHost)) {//send datasource info to redis
                JedisPoolConfig redisConfig = new JedisPoolConfig();
                redisConfig.setMinIdle(0);
                redisConfig.setMaxTotal(1);
                JedisPool pool = new JedisPool(redisConfig, redisHost, config.getRedisPort(), config.getRedisTimeoutMs(), config.getRedisUserId(), config.getRedisPassword());

                int expireSeconds = (int) MILLISECONDS.toSeconds(config.getRedisSendPeriod());
                timerExecutor.scheduleAtFixedRate(new RedisPushTask(pool, expireSeconds), 0, config.getRedisSendPeriod(), MILLISECONDS);
            }
        }
    }

    private void removeTimeoutTrace(LinkedList<StatementTrace> sqlAlertTempList, long sqlTraceTimeout) {
        Iterator<StatementTrace> iterator = sqlTraceQueue.descendingIterator();
        while (iterator.hasNext()) {
            StatementTrace vo = iterator.next();
            if (vo.getEndTimeMs() > 0 && (!vo.isSuccessInd() || vo.isSlowInd()) && !vo.isAlertedInd()) {//failed or slow
                vo.setAlertedInd(true);
                sqlAlertTempList.add(vo);
                if (sqlShow) Log.info("{} sql:{}", vo.isSlowInd() ? "Slow" : "Error", vo.getSql());
            }

            if (System.currentTimeMillis() - vo.getStartTimeMs() >= sqlTraceTimeout) {
                iterator.remove();
                sqlTracedSize.decrementAndGet();
            }
        }

        if (!sqlAlertTempList.isEmpty()) { //should be in short time
            try {
                sqlTraceAlert.alert(sqlAlertTempList);
            } finally {
                sqlAlertTempList.clear();
            }
        }
    }

    public ConcurrentLinkedDeque<StatementTrace> getSqlTraceQueue() {
        return sqlTraceQueue;
    }

    //add sqlTrace sql
    public Object traceSqlExecution(StatementTrace vo, Statement statement, Method method, Object[] args) throws Throwable {
        vo.setMethodName(method.getName());
        sqlTraceQueue.offerFirst(vo);
        if (sqlTracedSize.incrementAndGet() > sqlTraceMaxSize) {
            sqlTraceQueue.pollLast();
            sqlTracedSize.decrementAndGet();
        }

        try {
            if (sqlShow) Log.info("Executing sql:{}", vo.getSql());
            Object re = method.invoke(statement, args);
            vo.setSuccessInd(true);
            return re;
        } catch (InvocationTargetException e) {
            vo.setSuccessInd(false);
            Throwable failedCause = e.getCause();
            if (failedCause == null) failedCause = e;
            vo.setFailedCause(failedCause);
            throw failedCause;
        } catch (Throwable e) {
            vo.setSuccessInd(false);
            vo.setFailedCause(e);
            throw e;
        } finally {
            Date endDate = new Date();
            vo.setEndTimeMs(endDate.getTime());
            vo.setEndTime(SpringRegisterUtil.formatDate(endDate));
            vo.setTookTimeMs(vo.getEndTimeMs() - vo.getStartTimeMs());
            if (vo.isSuccessInd() && vo.getTookTimeMs() >= sqlExecSlowTime)//alert
                vo.setSlowInd(true);
        }
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitor (2)                                                       //
    //***************************************************************************************************************//
    private static final class SpringBootDsThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, "StoneThreadFactory");
            th.setDaemon(true);
            return th;
        }
    }

    private static final class SqlTraceTimeoutTask implements Runnable {
        private final long sqlTraceTimeout;
        private final StatementTracePool manager;
        private final LinkedList<StatementTrace> sqlAlertTempList;

        SqlTraceTimeoutTask(StatementTracePool manager, long sqlTraceTimeout) {
            this.manager = manager;
            this.sqlTraceTimeout = sqlTraceTimeout;
            this.sqlAlertTempList = new LinkedList<>();
        }

        public void run() {// check idle connection
            manager.removeTimeoutTrace(sqlAlertTempList, sqlTraceTimeout);
        }
    }
}
