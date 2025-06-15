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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * @author Chris Liao
 */
public class ConnectionHandler implements InvocationHandler {
    public static final String Type_Statement = "Statement";
    public static final String Type_PreparedStatement = "PreparedStatement";
    public static final String Type_CallableStatement = "CallableStatement";

    private static final String Method_Statement = "createStatement";
    private static final String Method_PreparedStatement = "prepareStatement";
    private static final String Method_CallableStatement = "prepareCall";

    private final String dsId;
    private final boolean fromXA;
    private final Connection connection;
    private final StatementExecutionCollector collector;

    public ConnectionHandler(String dsId, boolean fromXA, Connection connection,
                             StatementExecutionCollector collector) {
        this.dsId = dsId;
        this.fromXA = fromXA;
        this.connection = connection;
        this.collector = collector;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1: record start time of sql precompile
        String methodName = method.getName();
        long prepareStartTime = 0L;
        if (Method_PreparedStatement.equals(methodName) || Method_CallableStatement.equals(methodName)) {
            prepareStartTime = System.currentTimeMillis();
        }

        //2: invoke connection method
        Object re = method.invoke(connection, args);

        //3: create reflection statement
        switch (methodName) {
            case Method_Statement -> {
                return StatementJdbcUtil.createStatement(dsId, fromXA, (Statement) re, Type_Statement, null, prepareStartTime, 0L, collector);
            }
            case Method_PreparedStatement -> {
                return StatementJdbcUtil.createStatement(dsId, fromXA, (PreparedStatement) re, Type_PreparedStatement, (String) args[0], prepareStartTime, System.currentTimeMillis(), collector);
            }
            case Method_CallableStatement -> {
                return StatementJdbcUtil.createStatement(dsId, fromXA, (CallableStatement) re, Type_CallableStatement, (String) args[0], prepareStartTime, System.currentTimeMillis(), collector);
            }
            default -> {
                return re;
            }
        }
    }
}
