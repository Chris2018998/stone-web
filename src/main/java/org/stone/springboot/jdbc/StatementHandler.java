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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;

import static org.stone.springboot.jdbc.ConnectionHandler.Type_Statement;

/**
 * @author Chris Liao
 */
public final class StatementHandler implements InvocationHandler {
    private static final String Execute = "execute";
    private final String dsId;
    private final boolean fromXA;
    private final String prepareSql;
    private final long prepareStartTime;
    private final long prepareEndTime;
    private final Statement statement;
    private final String statementType;
    private final StatementExecutionCollector collector;

    StatementHandler(String dsId,
                     boolean fromXA,
                     Statement statement,
                     String statementType,
                     String prepareSql,
                     long prepareStartTime,
                     long prepareEndTime,
                     StatementExecutionCollector collector) {

        this.dsId = dsId;
        this.fromXA = fromXA;
        this.statement = statement;
        this.statementType = statementType;
        this.prepareSql = prepareSql;
        this.prepareStartTime = prepareStartTime;
        this.prepareEndTime = prepareEndTime;
        this.collector = collector;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        StatementExecution execution = null;
        String methodName = method.getName();
        if (methodName.startsWith(Execute)) {//execute method
            execution = new StatementExecution(dsId, fromXA);
            execution.setStatement(statement);
            execution.setMethodName(methodName);
            if (method.getParameterTypes().length == 0) {
                execution.setSql(prepareSql, statementType);
                execution.setPrepareStartTime(prepareStartTime);//set precompile start time
                execution.setPrepareEndTime(prepareEndTime);//set precompile start time
                execution.setExecuteStartTime(execution.getPrepareStartTime());
                collector.putStatementExecution(execution);
            } else {
                execution.setStatement(statement);
                execution.setSql((String) args[0], Type_Statement);
                execution.setExecuteStartTime(execution.getPrepareStartTime());
                collector.putStatementExecution(execution);
            }
        }

        //2: invoke statement method
        try {
            Object re = method.invoke(statement, args);
            if (execution != null) execution.setSuccessInd(true);
            return re;
        } catch (IllegalAccessException e) {
            if (execution != null) {
                execution.setFailure(e);
                execution.setSuccessInd(false);
            }
            throw e;
        } catch (InvocationTargetException e) {
            if (execution != null) {
                execution.setSuccessInd(false);
                Throwable cause = e.getCause();
                execution.setFailure(cause != null ? cause : e);
            }
            throw e;
        } finally {
            if (execution != null) {
                long executeEndTime = System.currentTimeMillis();
                execution.setExecuteEndTime(executeEndTime);
                long elapsedTime = executeEndTime - execution.getExecuteStartTime();
                execution.setElapsedTime(elapsedTime);
                execution.setSlowInd(elapsedTime >= collector.getSqlSlowThresholdTime());
            }
        }
    }
}
