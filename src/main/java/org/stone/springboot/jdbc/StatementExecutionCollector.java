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
import org.stone.springboot.LocalScheduleService;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.*;

/**
 * statement trace manager
 *
 * @author Chris Liao
 */
public class StatementExecutionCollector {
    private final Logger Log = LoggerFactory.getLogger(StatementExecutionCollector.class);

    private boolean sqlShow = false;
    private int sqlQueueMaxSize = 100;
    private long sqlExpirationTime = MINUTES.toMillis(3L);
    private long sqlSlowThresholdTime = SECONDS.toMillis(10L);
    private long sqlQueueScanPeriodTime = sqlExpirationTime;
    private StatementSqlAlert sqlAlertAction;
    private AtomicInteger sqlCurrentQueueSize;
    private ConcurrentLinkedDeque<StatementExecution> sqlExecQueue;

    //***************************************************************************************************************//
    //                                     1: field setting(6)                                                       //
    //***************************************************************************************************************//
    public void setSqlShow(boolean sqlShow) {
        this.sqlShow = sqlShow;
    }

    public void setSqlQueueMaxSize(int sqlQueueMaxSize) {
        this.sqlQueueMaxSize = sqlQueueMaxSize;
    }

    public void setSqlExpirationTime(long sqlExpirationTime) {
        this.sqlExpirationTime = sqlExpirationTime;
    }

    public void setSqlQueueScanPeriodTime(long sqlQueueScanPeriodTime) {
        this.sqlQueueScanPeriodTime = sqlQueueScanPeriodTime;
    }

    public void setSqlAlertAction(StatementSqlAlert sqlAlertAction) {
        this.sqlAlertAction = sqlAlertAction;
    }

    public long getSqlSlowThresholdTime() {
        return sqlSlowThresholdTime;
    }

    public void setSqlSlowThresholdTime(long sqlSlowThresholdTime) {
        this.sqlSlowThresholdTime = sqlSlowThresholdTime;
    }

    //***************************************************************************************************************//
    //                                     2: initialize(1)                                                          //
    //***************************************************************************************************************//
    public void initialize() {
        this.sqlExecQueue = new ConcurrentLinkedDeque<>();
        this.sqlCurrentQueueSize = new AtomicInteger(0);

        //schedule a task to clear expiration execution in queue
        LocalScheduleService.getInstance().
                scheduleAtFixedRate(new ClearExpirationTask(this, sqlExpirationTime),
                        0L, sqlQueueScanPeriodTime, MILLISECONDS);
    }

    //***************************************************************************************************************//
    //                                     3: operation on queue(3)                                                  //
    //***************************************************************************************************************//
    public ConcurrentLinkedDeque<StatementExecution> getSqlExecQueue() {
        return sqlExecQueue;
    }

    public void putStatementExecution(StatementExecution execution) {
        sqlExecQueue.offerFirst(execution);
        if (sqlCurrentQueueSize.incrementAndGet() > sqlQueueMaxSize) {
            sqlExecQueue.pollLast();
            sqlCurrentQueueSize.decrementAndGet();
        }
    }

    public void cancelStatementExecution(String statementUUID) throws SQLException {
        for (StatementExecution statementExecution : sqlExecQueue) {
            if (statementExecution.getUuid().equals(statementUUID)) {
                statementExecution.cancelStatement();
            }
        }
    }

    //***************************************************************************************************************//
    //                                     4: Clear expiration trace in queue                                        //
    //***************************************************************************************************************//
    private void clearExpirationExecution(LinkedList<StatementExecution> sqlAlertTempList, long sqlExpirationTime) {
        Iterator<StatementExecution> iterator = sqlExecQueue.descendingIterator();
        while (iterator.hasNext()) {
            StatementExecution execution = iterator.next();
            //1: set SLOW IND TO NOT COMPLETED execution
            if (execution.getExecuteEndTime() == 0L) {//although not end
                long elapsedTime = System.currentTimeMillis() - execution.getExecuteStartTime();
                execution.setSlowInd(elapsedTime >= sqlSlowThresholdTime);
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
                if (System.currentTimeMillis() - execution.getExecuteEndTime() >= sqlExpirationTime) {
                    iterator.remove();
                    sqlCurrentQueueSize.decrementAndGet();
                }
            }
        }

        //execute alert action if exists slow sql or error sql
        if (sqlAlertAction != null && !sqlAlertTempList.isEmpty()) {
            try {
                sqlAlertAction.slowRun(sqlAlertTempList);
            } finally {
                sqlAlertTempList.clear();
            }
        }
    }

    //***************************************************************************************************************//
    //                                     5: A task to clear expiration trace in queue                              //
    //***************************************************************************************************************//
    private static final class ClearExpirationTask implements Runnable {
        private final long sqlExpirationTime;
        private final StatementExecutionCollector collector;
        private final LinkedList<StatementExecution> expirationList;

        ClearExpirationTask(StatementExecutionCollector collector, long sqlExpirationTime) {
            this.collector = collector;
            this.sqlExpirationTime = sqlExpirationTime;
            this.expirationList = new LinkedList<>();
        }

        public void run() {
            expirationList.clear();
            collector.clearExpirationExecution(expirationList, sqlExpirationTime);
        }
    }
}
