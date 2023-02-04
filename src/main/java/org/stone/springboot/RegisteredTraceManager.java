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
package org.stone.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.pool.ConnectionPoolMonitorVo;
import org.stone.beeop.pool.ObjectPoolMonitorVo;
import org.stone.springboot.datasource.SpringBootDataSourceManager;
import org.stone.springboot.datasource.SpringBootDataSourceUtil;
import org.stone.springboot.sqlTrace.StatementTrace;
import org.stone.springboot.sqlTrace.StatementTraceAlert;
import org.stone.springboot.storage.RedisPushTask;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.util.CommonUtil.isBlank;

/**
 * springboot registered manager
 *
 * @author Chris Liao
 */
public final class RegisteredTraceManager {
    private final Map<String, RegisteredDataSource> dsMap;
    private final Map<String, RegisteredObjectSource> osMap;
    private final Logger Log = LoggerFactory.getLogger(SpringBootDataSourceManager.class);

    private boolean sqlTrace;
    private boolean sqlShow;
    private long sqlExecSlowTime;
    private int sqlTraceMaxSize;
    private AtomicInteger sqlTracedSize;
    private StatementTraceAlert sqlTraceAlert;
    private ScheduledThreadPoolExecutor timerExecutor;
    private ConcurrentLinkedDeque<StatementTrace> sqlTraceQueue;

    public RegisteredTraceManager() {
        this.dsMap = new ConcurrentHashMap<>(1);
        this.osMap = new ConcurrentHashMap<>(1);
    }

    //***************************************************************************************************************//
    //                                     1: manager init(1)                                                        //
    //***************************************************************************************************************//
    void init(StoneSpringbootConfig config) {
        if (sqlTrace = config.isSqlTrace()) {
            this.sqlShow = config.isSqlShow();
            this.sqlExecSlowTime = config.getSqlExecSlowTime();
            this.sqlTraceMaxSize = config.getSqlTraceMaxSize();
            this.sqlTraceAlert = config.getSqlExecAlertAction();
            this.sqlTracedSize = new AtomicInteger(0);
            this.sqlTraceQueue = new ConcurrentLinkedDeque<>();

            //sql trace timeout scan
            this.timerExecutor = new ScheduledThreadPoolExecutor(2, new SpringBootDsThreadFactory());
            this.timerExecutor.setKeepAliveTime(15, TimeUnit.SECONDS);
            this.timerExecutor.allowCoreThreadTimeOut(true);
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

    //***************************************************************************************************************//
    //                                     2: ds maintenance(4)                                                      //
    //***************************************************************************************************************//
    public int getDataSourceSize() {
        return dsMap.size();
    }

    public void addDataSource(RegisteredDataSource ds) {
        dsMap.put(ds.getDsId(), ds);
        ds.setTraceSql(sqlTrace);
    }

    public RegisteredDataSource getDataSource(String dsId) {
        return dsMap.get(dsId);
    }

    public void restartDataSourcePool(String dsId) {
        RegisteredDataSource ds = dsMap.get(dsId);
        if (ds != null) ds.restartPool();
    }

    //***************************************************************************************************************//
    //                                     2: os maintenance(3)                                                      //
    //***************************************************************************************************************//
    public int getObjectSourceSize() {
        return osMap.size();
    }

    public void addObjectSource(RegisteredObjectSource os) {
        osMap.put(os.getOsId(), os);
    }

    public RegisteredObjectSource getObjectSource(String osId) {
        return osMap.get(osId);
    }

    public void restartObjectSourcePool(String osId) {
        RegisteredObjectSource os = osMap.get(osId);
        if (os != null) os.restartPool();
    }

    //***************************************************************************************************************//
    //                                     3: Pool Monitor (2)                                                       //
    //***************************************************************************************************************//
    public List<ConnectionPoolMonitorVo> getDsPoolMonitorVoList() {
        List<ConnectionPoolMonitorVo> poolMonitorVoList = new ArrayList<>(dsMap.size());
        Iterator<RegisteredDataSource> iterator = dsMap.values().iterator();

        while (iterator.hasNext()) {
            RegisteredDataSource ds = iterator.next();
            ConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    public List<ObjectPoolMonitorVo> getOsPoolMonitorVoList() {
        List<ObjectPoolMonitorVo> poolMonitorVoList = new ArrayList<>(osMap.size());
        Iterator<RegisteredObjectSource> iterator = osMap.values().iterator();

        while (iterator.hasNext()) {
            RegisteredObjectSource os = iterator.next();
            ObjectPoolMonitorVo vo = os.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    //***************************************************************************************************************//
    //                                     4: sql-trace (1)                                                          //
    //***************************************************************************************************************//
    public Collection<StatementTrace> getSqlExecutionList() {
        return sqlTraceQueue;
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
            vo.setEndTime(SpringBootDataSourceUtil.formatDate(endDate));
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
        private final RegisteredTraceManager manager;
        private final LinkedList<StatementTrace> sqlAlertTempList;

        public SqlTraceTimeoutTask(RegisteredTraceManager manager, long sqlTraceTimeout) {
            this.manager = manager;
            this.sqlTraceTimeout = sqlTraceTimeout;
            this.sqlAlertTempList = new LinkedList<>();
        }

        public void run() {// check idle connection
            manager.removeTimeoutTrace(sqlAlertTempList, sqlTraceTimeout);
        }
    }
}
