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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;

/*
 *  SQL Execute Trace entry
 *
 *  @author Chris Liao
 */
public class StatementExecution implements Serializable {
    private final String uuid;
    private final String dsId;
    private final boolean fromXA;

    private String sql;
    private String statementType;

    private long prepareStartTime;
    private long prepareEndTime;

    private String methodName;
    private long executeStartTime;
    private long executeEndTime;
    private long elapsedTime;

    private boolean successInd;
    private boolean slowInd;
    private boolean alertedInd;

    @JsonIgnore
    private Throwable failure;
    @JsonIgnore
    private Statement statement;

    public StatementExecution(String dsId, boolean fromXA) {
        this.uuid = UUID.randomUUID().toString();
        this.dsId = dsId;
        this.fromXA = fromXA;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDsId() {
        return dsId;
    }

    public boolean isFromXA() {
        return fromXA;
    }

    public void setSql(String sql, String statementType) {
        this.sql = sql;
        this.statementType = statementType;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public void cancelStatement() throws SQLException {
        if (statement != null) statement.cancel();
    }

    public String getSql() {
        return sql;
    }

    public String getStatementType() {
        return statementType;
    }

    public long getPrepareStartTime() {
        return prepareStartTime;
    }

    public void setPrepareStartTime(long prepareStartTime) {
        this.prepareStartTime = prepareStartTime;
    }

    public long getPrepareEndTime() {
        return prepareEndTime;
    }

    public void setPrepareEndTime(long prepareEndTime) {
        this.prepareEndTime = prepareEndTime;
    }

    public long getExecuteStartTime() {
        return executeStartTime;
    }

    public void setExecuteStartTime(long executeStartTime) {
        this.executeStartTime = executeStartTime;
    }

    public long getExecuteEndTime() {
        return executeEndTime;
    }

    public void setExecuteEndTime(long executeEndTime) {
        this.executeEndTime = executeEndTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isSlowInd() {
        return slowInd;
    }

    public void setSlowInd(boolean slowInd) {
        this.slowInd = slowInd;
    }

    public boolean isSuccessInd() {
        return successInd;
    }

    public void setSuccessInd(boolean successInd) {
        this.successInd = successInd;
    }

    public boolean isAlertedInd() {
        return alertedInd;
    }

    public void setAlertedInd(boolean alertedInd) {
        this.alertedInd = alertedInd;
    }

    public Throwable getFailure() {
        return failure;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String toString() {
        return sql;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementExecution that = (StatementExecution) o;
        return uuid.equals(that.uuid);
    }
}
