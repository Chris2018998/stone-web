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
package org.stone.springboot.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.springboot.InternalScheduledService;
import org.stone.springboot.MonitoringConfigManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * statement trace manager
 *
 * @author Chris Liao
 */
public class SqlExecutionWorkshop {
    private static final ThreadLocal<WeakReference<DateFormat>> DateFormatThreadLocal = new ThreadLocal<>();
    private final Logger Log = LoggerFactory.getLogger(SqlExecutionWorkshop.class);

    private final boolean sqlShow;
    private final long sqlExecSlowTime;
    private final int sqlTraceMaxSize;
    private final AtomicInteger sqlTracedSize;
    private final SqlExecutionSlowAction sqlTraceAlert;
    private final ConcurrentLinkedDeque<SqlExecution> sqlTraceQueue;

    public SqlExecutionWorkshop(MonitoringConfigManager manager) {
        this.sqlShow = manager.isSqlShow();
        this.sqlExecSlowTime = manager.getSqlSlowTime();
        this.sqlTraceMaxSize = manager.getSqlQueueSize();
        this.sqlTraceAlert = manager.getSqlSlowAction();
        this.sqlTracedSize = new AtomicInteger(0);
        this.sqlTraceQueue = new ConcurrentLinkedDeque<>();

        //sql trace timeout scan
        InternalScheduledService.getInstance().
                scheduleAtFixedRate(new SqlTraceTimeoutTask(this, manager.getSqlTimeoutInQueue()),
                        0, manager.getSqlQueueScanPeriod(), MILLISECONDS);
    }

    private void removeTimeoutTrace(LinkedList<SqlExecution> sqlAlertTempList, long sqlTraceTimeout) {
        Iterator<SqlExecution> iterator = sqlTraceQueue.descendingIterator();
        while (iterator.hasNext()) {
            SqlExecution vo = iterator.next();
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

    public ConcurrentLinkedDeque<SqlExecution> getSqlTraceQueue() {
        return sqlTraceQueue;
    }

    //add sqlTrace sql
    public Object traceSqlExecution(SqlExecution vo, Statement statement, Method method, Object[] args) throws Throwable {
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
            vo.setEndTime(SqlExecutionJdbcUtil.formatDate(endDate));
            vo.setTookTimeMs(vo.getEndTimeMs() - vo.getStartTimeMs());
            if (vo.isSuccessInd() && vo.getTookTimeMs() >= sqlExecSlowTime)//alert
                vo.setSlowInd(true);
        }
    }

    //***************************************************************************************************************//
    //                                     5: Pool Monitor (2)                                                       //
    //***************************************************************************************************************//
    private static final class SqlTraceTimeoutTask implements Runnable {
        private final long sqlTraceTimeout;
        private final SqlExecutionWorkshop manager;
        private final LinkedList<SqlExecution> sqlAlertTempList;

        SqlTraceTimeoutTask(SqlExecutionWorkshop manager, long sqlTraceTimeout) {
            this.manager = manager;
            this.sqlTraceTimeout = sqlTraceTimeout;
            this.sqlAlertTempList = new LinkedList<>();
        }

        public void run() {// check idle connection
            manager.removeTimeoutTrace(sqlAlertTempList, sqlTraceTimeout);
        }
    }
}
