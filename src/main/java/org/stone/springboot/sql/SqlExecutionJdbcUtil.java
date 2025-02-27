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
public class SqlExecutionJdbcUtil {
    private static final Class[] INTF_Connection = new Class[]{Connection.class};
    private static final Class[] INTF_CallableStatement = new Class[]{CallableStatement.class};
    private static final ClassLoader classLoader = SqlExecutionJdbcUtil.class.getClassLoader();
    private static final ThreadLocal<WeakReference<DateFormat>> DateFormatThreadLocal = new ThreadLocal<WeakReference<DateFormat>>();

    public static Connection createConnection(Connection delegate, String dsId, SqlExecutionWorkshop statementPool) {
        return (Connection) Proxy.newProxyInstance(
                classLoader,
                INTF_Connection,
                new ConnectionHandler(delegate, dsId, statementPool));
    }

    static Statement createStatement(Statement delegate, String statementType, String dsId, SqlExecution trace, SqlExecutionWorkshop statementPool) {
        return (Statement) Proxy.newProxyInstance(
                classLoader,
                INTF_CallableStatement,
                new SqlStatementHandler(delegate, statementType, dsId, trace, statementPool));
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
