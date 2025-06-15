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
package org.stone.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.exception.PoolNotCreatedException;
import org.stone.beecp.jta.BeeJtaDataSource;
import org.stone.springboot.jdbc.StatementExecutionCollector;
import org.stone.springboot.jdbc.StatementJdbcUtil;
import org.stone.springboot.jdbc.XAConnectionImpl;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;

/**
 * A data Source wrapper
 *
 * @author Chris Liao
 */
public final class DataSourceBean implements DataSource, XADataSource {
    private final DataSource ds;
    private final XADataSource xaDs;

    private final String dsId;
    private final boolean jndiDs;
    private final boolean primary;
    private final boolean isBeeDs;
    private final boolean isBeeJtaDs;
    private final DataSourcePoolMonitorVo voWrapper;
    private final Logger log = LoggerFactory.getLogger(DataSourceBean.class);
    private StatementExecutionCollector statementExecutionCollector;


    public DataSourceBean(String dsId, boolean jndiDs, boolean primary, Object ds) {
        if (ds == null) throw new IllegalArgumentException("Data source can't be null");
        this.dsId = dsId;
        this.jndiDs = jndiDs;
        this.primary = primary;

        this.ds = ds instanceof DataSource ? (DataSource) ds : null;
        this.xaDs = ds instanceof XADataSource ? (XADataSource) ds : null;
        this.isBeeDs = ds instanceof BeeDataSource;
        this.isBeeJtaDs = ds instanceof BeeJtaDataSource;
        this.voWrapper = new DataSourcePoolMonitorVo(dsId, UUID.randomUUID().toString());
    }

    //***************************************************************************************************************//
    //                                     1: base properties (3)                                                    //
    //***************************************************************************************************************//
    public String getDsId() {
        return dsId;
    }

    public boolean isPrimary() {
        return primary;
    }

    void setStatementExecutionCollector(StatementExecutionCollector statementExecutionCollector) {
        this.statementExecutionCollector = statementExecutionCollector;
    }

    //***************************************************************************************************************//
    //                                     2: methods of getting connection(4)                                       //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (ds == null) throw new SQLFeatureNotSupportedException("Not provide features of dataSource");

        Connection con = ds.getConnection();
        return statementExecutionCollector == null ? con : StatementJdbcUtil.createConnection(dsId, false, con, statementExecutionCollector);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        if (ds == null) throw new SQLFeatureNotSupportedException("Not provide features of dataSource");

        Connection con = ds.getConnection(username, password);
        return statementExecutionCollector == null ? con : StatementJdbcUtil.createConnection(dsId, false, con, statementExecutionCollector);
    }

    public XAConnection getXAConnection() throws SQLException {
        if (xaDs == null) throw new SQLFeatureNotSupportedException("Not provide features of XADataSource");
        XAConnection xaCon = xaDs.getXAConnection();
        return new XAConnectionImpl(dsId, xaCon, statementExecutionCollector);
    }

    public XAConnection getXAConnection(String username, String password) throws SQLException {
        if (xaDs == null) throw new SQLFeatureNotSupportedException("Not provide features of XADataSource");
        XAConnection xaCon = xaDs.getXAConnection(username, password);
        return new XAConnectionImpl(dsId, xaCon, statementExecutionCollector);
    }

    //***************************************************************************************************************//
    //                                     3: implementation methods of bee data source(3)                           //
    //***************************************************************************************************************//
    void close() throws SQLException {
        if (jndiDs) return;
        if (ds == null) throw new SQLFeatureNotSupportedException("Not provide feature of dataSource");

        if (isBeeDs) {
            ((BeeDataSource) ds).close();
        } else if (isBeeJtaDs) {
            ((BeeJtaDataSource) ds).close();
        }
    }

    void clear(boolean force) throws SQLException {
        if (ds == null) throw new SQLFeatureNotSupportedException("Not provide feature of dataSource");
        if (isBeeDs) {
            ((BeeDataSource) ds).clear(force);
        } else if (isBeeJtaDs) {
            ((BeeJtaDataSource) ds).clear(force);
        }
    }

    BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        if (ds == null) throw new SQLFeatureNotSupportedException("Not provide feature of dataSource");
        try {
            if (isBeeDs) {
                voWrapper.setVo(((BeeDataSource) ds).getPoolMonitorVo());
            } else if (isBeeJtaDs) {
                voWrapper.setVo(((BeeJtaDataSource) ds).getPoolMonitorVo());
            }
        } catch (Throwable e) {
            if (!(e instanceof PoolNotCreatedException))
                log.warn("Failed to execute dataSource 'getPoolMonitorVo' method", e);
            return null;
        }
        return voWrapper;
    }

    //***************************************************************************************************************//
    //                                    4: Other implementation methods (7)                                        //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return ds != null ? ds.getLogWriter() : xaDs.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        if (ds != null) {
            ds.setLogWriter(out);
        } else {
            xaDs.setLogWriter(out);
        }
    }

    public int getLoginTimeout() throws SQLException {
        return ds != null ? ds.getLoginTimeout() : xaDs.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        if (ds != null) {
            ds.setLoginTimeout(seconds);
        } else {
            xaDs.setLoginTimeout(seconds);
        }
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds != null ? ds.getParentLogger() : xaDs.getParentLogger();
    }

    public boolean isWrapperFor(Class<?> clazz) {
        return clazz != null && clazz.isInstance(this);
    }

    public <T> T unwrap(Class<T> clazz) throws SQLException {
        if (clazz != null && clazz.isInstance(this))
            return clazz.cast(this);
        else
            throw new SQLException("Wrapped object was not an instance of " + clazz);
    }
}
