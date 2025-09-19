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
package org.stone.springboot.jdbc.listener;

import org.stone.beecp.BeeConnectionInterceptor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Connection event listener
 *
 *  @author Chris Liao
 */
public class ConnectionListener implements BeeConnectionInterceptor {
    private final ConcurrentHashMap<Object, ConnectionGetEvent> connectionEvent = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, StatementSQLEvent> sqlExecutionEvent = new ConcurrentHashMap<>();
    private final int cacheSize = 100;
    private final long getTimeThreshold = 3000;
    private final long sqlTimeThreshold = 3000;

    /**
     * generate a key for operation trace.
     *
     * @return a unique key
     */
    public Object genKey() {
        return UUID.randomUUID();
    }

    //***************************************************************************************************************//
    //                              2: Trace on BeeConnectionPool get methods(getConnection/getXAConnection)                //
    //***************************************************************************************************************//

    /**
     * Records a start event of connection getting and XAConnection getting
     *
     * @param traceKey        is operation trace key
     * @param startTime       trace start time,unit:milliseconds
     * @param methodSignature is name of a trace method
     */
    public void beforeGetConnection(Object traceKey, String methodSignature, long startTime) {

    }

    /**
     * Record a success event of connection getting or XAConnection getting
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     */
    public void afterGetConnection(Object traceKey, String methodSignature, long startTime, long endTime) {

    }


    //***************************************************************************************************************//
    //                              3: trace on connection preparation methods(prepareStatement,prepareCall)        //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    public void beforePrepareSQL(Object traceKey, String methodSignature, long startTime, String sql) {

    }

    /**
     * Record a success event after invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    public void afterPrepareSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql) {

    }

    //***************************************************************************************************************//
    //                              4: Trace on PreparedStatement,CallableStatement                                  //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code PreparedStatement.executeXX()} method of a preparedStatement.
     * method list[PreparedStatement.execute(),PreparedStatement.executeQuery(),PreparedStatement.executeUpdate(),PreparedStatement.executeLargeUpdate()]
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    public void beforeExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, Object preparedKey, String sql) {

    }

    /**
     * Record a success event after invocation on {@code PreparedStatement.executeXX()} method of a PreparedStatement.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    public void afterExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, long endTime, Object preparedKey, String sql) {

    }


    //***************************************************************************************************************//
    //                              5: Trace on statement sql execution methods                                      //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     * method list[Statement.execute(String),Statement.executeQuery(String),Statement.executeUpdate(String),Statement.executeLargeUpdate(String)]
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    public void beforeExecuteSQL(Object traceKey, String methodSignature, long startTime, String sql) {

    }

    /**
     * Record a success event after invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    public void afterExecuteSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql) {

    }

    //***************************************************************************************************************//
    //                              6:   Exception                                                                   //
    //***************************************************************************************************************//

    /**
     * Record an exception event
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param e               is fail exception from trace method
     * @param preparedKey     is a sql preparation trace key
     * @param sql             is a failure sql,such as fail preparation or fail execution
     */
    public void onException(Object traceKey, String methodSignature, long startTime, long endTime, Throwable e, Object preparedKey, String sql) {

    }


}
