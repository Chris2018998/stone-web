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
package org.stone.springboot.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.springboot.InternalScheduledService;
import org.stone.springboot.MonitoringConfigManager;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.text.DateFormat;
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
public class StatementExecutionCollector {
    private static final ThreadLocal<WeakReference<DateFormat>> DateFormatThreadLocal = new ThreadLocal<>();
    private final Logger Log = LoggerFactory.getLogger(StatementExecutionCollector.class);

    private final boolean sqlShow;
    private final long sqlSlowTime;
    private final int sqlTraceMaxSize;
    private final AtomicInteger sqlTracedSize;
    private final StatementSqlAlert sqlTraceAlert;
    private final ConcurrentLinkedDeque<StatementExecution> sqlTraceQueue;

    public StatementExecutionCollector(MonitoringConfigManager manager) {
        this.sqlShow = manager.isSqlShow();
        this.sqlSlowTime = manager.getSqlSlowTime();
        this.sqlTraceMaxSize = manager.getSqlQueueSize();
        this.sqlTraceAlert = manager.getSqlSlowAction();
        this.sqlTracedSize = new AtomicInteger(0);
        this.sqlTraceQueue = new ConcurrentLinkedDeque<>();

        //sql trace timeout scan
        InternalScheduledService.getInstance().
                scheduleAtFixedRate(new SqlTraceTimeoutTask(this, manager.getSqlTimeoutInQueue()),
                        0, manager.getSqlQueueScanPeriod(), MILLISECONDS);
    }

    public long getSqlSlowTime() {
        return sqlSlowTime;
    }

    public ConcurrentLinkedDeque<StatementExecution> getSqlTraceQueue() {
        return sqlTraceQueue;
    }

    public void putStatementExecution(StatementExecution execution) {
        sqlTraceQueue.offerFirst(execution);
        if (sqlTracedSize.incrementAndGet() > sqlTraceMaxSize) {
            sqlTraceQueue.pollLast();
            sqlTracedSize.decrementAndGet();
        }
    }

    public void cancelStatementExecution(String statementUUID) throws SQLException {
        for (StatementExecution statementExecution : sqlTraceQueue) {
            if (statementExecution.getUuid().equals(statementUUID)) {
                statementExecution.cancelStatement();
            }
        }
    }

    private void removeTimeoutTrace(LinkedList<StatementExecution> sqlAlertTempList, long sqlTimeoutInQueue) {
        Iterator<StatementExecution> iterator = sqlTraceQueue.descendingIterator();
        while (iterator.hasNext()) {
            StatementExecution execution = iterator.next();
            //1: set SLOW IND TO NOT COMPLETED execution
            if (execution.getExecuteEndTime() == 0L) {//although not end
                long elapsedTime = System.currentTimeMillis() - execution.getExecuteStartTime();
                execution.setSlowInd(elapsedTime >= sqlSlowTime);
            }

            //2: collect slow sql and error sql
            if (!execution.isAlertedInd()) {
                if (!execution.isSuccessInd() || execution.isSlowInd()) {
                    execution.setAlertedInd(true);
                    sqlAlertTempList.add(execution);
                    if (sqlShow) Log.info("{} sql:{}", execution.isSlowInd() ? "Slow" : "Error", execution.getSql());
                }
            }

            //3: remove timeout execution from trace queue
            if (execution.getExecuteEndTime() > 0L) {
                if (System.currentTimeMillis() - execution.getExecuteEndTime() >= sqlTimeoutInQueue) {
                    iterator.remove();
                    sqlTracedSize.decrementAndGet();
                }
            }
        }

        //execute alert action if exists slow sql or error sql
        if (sqlTraceAlert != null && !sqlAlertTempList.isEmpty()) {
            try {
                sqlTraceAlert.slowRun(sqlAlertTempList);
            } finally {
                sqlAlertTempList.clear();
            }
        }
    }

    //***************************************************************************************************************//
    //                                     5: timeout in collector                                                   //
    //***************************************************************************************************************//
    private static final class SqlTraceTimeoutTask implements Runnable {
        private final long sqlTimeoutInQueue;
        private final StatementExecutionCollector collector;
        private final LinkedList<StatementExecution> timeoutList;

        SqlTraceTimeoutTask(StatementExecutionCollector collector, long sqlTimeoutInQueue) {
            this.collector = collector;
            this.sqlTimeoutInQueue = sqlTimeoutInQueue;
            this.timeoutList = new LinkedList<>();
        }

        public void run() {
            timeoutList.clear();
            collector.removeTimeoutTrace(timeoutList, sqlTimeoutInQueue);
        }
    }
}
