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

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Chris Liao
 */
public class StatementJdbcUtil {
    private static final Class[] INTF_Connection = new Class[]{Connection.class};
    private static final Class[] INTF_CallableStatement = new Class[]{CallableStatement.class};
    private static final ClassLoader classLoader = StatementJdbcUtil.class.getClassLoader();
    private static final ThreadLocal<WeakReference<DateFormat>> DateFormatThreadLocal = new ThreadLocal<WeakReference<DateFormat>>();

    public static Connection createConnection(String dsId, boolean fromXA, Connection delegate,
                                              StatementExecutionCollector collector) {
        return (Connection) Proxy.newProxyInstance(
                classLoader,
                INTF_Connection,
                new ConnectionHandler(dsId, fromXA, delegate, collector));
    }

    static Statement createStatement(String dsId, boolean fromXA, Statement delegate, String statementType,
                                     String prepareSql, long prepareStartTime, long prepareEndTime, StatementExecutionCollector collector) {
        return (Statement) Proxy.newProxyInstance(
                classLoader,
                INTF_CallableStatement,
                new StatementHandler(dsId, fromXA, delegate, statementType, prepareSql, prepareStartTime, prepareEndTime, collector));
    }

    public static String formatDate(Date date) {
        WeakReference<DateFormat> reference = DateFormatThreadLocal.get();
        DateFormat dateFormat = reference != null ? reference.get() : null;
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            DateFormatThreadLocal.set(new WeakReference<>(dateFormat));
        }
        return dateFormat.format(date);
    }
}
