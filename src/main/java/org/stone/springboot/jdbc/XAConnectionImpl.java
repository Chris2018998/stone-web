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

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

/*
 * XAConnection implementation
 *
 *  @author Chris Liao
 */
public class XAConnectionImpl implements XAConnection {
    private final String dsId;
    private final XAConnection xaCon;
    private final StatementExecutionCollector statementExecutionCollector;

    public XAConnectionImpl(String dsId, XAConnection xaCon, StatementExecutionCollector statementExecutionCollector) {
        this.dsId = dsId;
        this.xaCon = xaCon;
        this.statementExecutionCollector = statementExecutionCollector;
    }

    public XAResource getXAResource() throws SQLException {
        return xaCon.getXAResource();
    }

    public Connection getConnection() throws SQLException {
        Connection con = xaCon.getConnection();
        return statementExecutionCollector != null ? StatementJdbcUtil.createConnection(dsId, true, con, statementExecutionCollector) : con;
    }

    public void close() throws SQLException {
        xaCon.close();
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        xaCon.addConnectionEventListener(listener);
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        xaCon.removeConnectionEventListener(listener);
    }

    public void addStatementEventListener(StatementEventListener listener) {
        xaCon.addStatementEventListener(listener);
    }

    public void removeStatementEventListener(StatementEventListener listener) {
        xaCon.removeStatementEventListener(listener);
    }
}
