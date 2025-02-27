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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

/**
 * @author Chris Liao
 */
public class ConnectionHandler implements InvocationHandler {
    private static final String Type_Statement = "Statement";
    private static final String Type_PreparedStatement = "PreparedStatement";
    private static final String Type_CallableStatement = "CallableStatement";
    private static final String Method_Statement = "createStatement";
    private static final String Method_PreparedStatement = "prepareStatement";
    private static final String Method_CallableStatement = "prepareCall";

    private final String dsId;
    private final Connection connection;
    private final SqlExecutionWorkshop statementPool;

    ConnectionHandler(Connection connection, String dsId, SqlExecutionWorkshop statementPool) {
        this.connection = connection;
        this.dsId = dsId;
        this.statementPool = statementPool;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1:create trace for method 'prepareStatement' or 'prepareCall'
        SqlExecution trace = null;
        String methodName = method.getName();
        if (Method_PreparedStatement.equals(methodName)) {
            trace = new SqlExecution(dsId, (String) args[0], Type_PreparedStatement);
        } else if (Method_CallableStatement.equals(methodName)) {
            trace = new SqlExecution(dsId, (String) args[0], Type_CallableStatement);
        }

        //2:execute connection method
        Object re = method.invoke(connection, args);

        //3:create sqlTrace proxy
        return switch (methodName) {
            case Method_Statement ->
                    SqlExecutionJdbcUtil.createStatement((Statement) re, Type_Statement, dsId, null, statementPool);
            case Method_PreparedStatement ->
                    SqlExecutionJdbcUtil.createStatement((Statement) re, Type_PreparedStatement, dsId, trace, statementPool);
            case Method_CallableStatement ->
                    SqlExecutionJdbcUtil.createStatement((Statement) re, Type_CallableStatement, dsId, trace, statementPool);
            default -> re;
        };
    }
}
