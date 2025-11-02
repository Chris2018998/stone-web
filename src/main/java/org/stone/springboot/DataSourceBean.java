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

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeMethodExecutionListener;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.beecp.jta.BeeJtaDataSource;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A data Source wrapper
 *
 * @author Chris Liao
 */
public final class DataSourceBean implements DataSource, XADataSource {
    private final String dsId;
    private final boolean jndiDs;
    private final boolean primary;

    private final boolean isDs;
    private final boolean isXaDs;
    private final DataSource ds;
    private final XADataSource xaDs;

    private final boolean isBeeDs;
    private final boolean isBeeJtaDs;
    private final BeeDataSource beeDs;
    private final BeeJtaDataSource beeJtaDs;
    private final DataSourcePoolMonitorVo poolMonitorVo;

    public DataSourceBean(String dsId, boolean jndiDs, boolean primary, Object ids) {
        this.dsId = dsId;
        this.jndiDs = jndiDs;
        this.primary = primary;

        boolean isDs = false;
        boolean isXaDs = false;
        boolean isBeeDs = false;
        boolean isBeeJtaDs = false;

        DataSource ds = null;
        XADataSource xaDs = null;
        BeeDataSource beeDs = null;
        BeeJtaDataSource beeJtaDs = null;

        if (ids instanceof DataSource) {
            isDs = true;
            ds = (DataSource) ids;
        } else {
            isXaDs = true;
            xaDs = (XADataSource) ids;
        }

        if (ds instanceof BeeDataSource) {
            isBeeDs = true;
            beeDs = (BeeDataSource) ds;
        } else if (ds instanceof BeeJtaDataSource) {
            isBeeJtaDs = true;
            beeJtaDs = (BeeJtaDataSource) ds;
        }

        this.isDs = isDs;
        this.isXaDs = isXaDs;
        this.beeDs = beeDs;
        this.beeJtaDs = beeJtaDs;
        this.isBeeDs = isBeeDs;

        this.ds = ds;
        this.xaDs = xaDs;
        this.isBeeJtaDs = isBeeJtaDs;
        this.poolMonitorVo = new DataSourcePoolMonitorVo(dsId, UUID.randomUUID().toString());
    }

    //***************************************************************************************************************//
    //                                     1: Base properties (3)                                                    //
    //***************************************************************************************************************//
    public String getDsId() {
        return dsId;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String getDsUUID() {
        return poolMonitorVo.getDsUUID();
    }

    //***************************************************************************************************************//
    //                                     2: Connection get(4)                                                      //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (isDs) {
            return ds.getConnection();
        } else {
            return this.getXAConnection().getConnection();
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        if (isDs) {
            return ds.getConnection(username, password);
        } else {
            return this.getXAConnection(username, password).getConnection();
        }
    }

    public XAConnection getXAConnection() throws SQLException {
        if (!isXaDs) throw new SQLFeatureNotSupportedException("Current data source is a not XADataSource");
        return xaDs.getXAConnection();
    }

    public XAConnection getXAConnection(String username, String password) throws SQLException {
        if (!isXaDs) throw new SQLFeatureNotSupportedException("Current data source is a not XADataSource");
        return this.xaDs.getXAConnection(username, password);
    }

    //***************************************************************************************************************//
    //                                     3: Data Source Monitor VO                                                 //
    //***************************************************************************************************************//
    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        if (isBeeDs) {
            poolMonitorVo.setVo(beeDs.getPoolMonitorVo());
        } else if (isBeeJtaDs) {
            poolMonitorVo.setVo(beeJtaDs.getPoolMonitorVo());
        }
        return poolMonitorVo;
    }

    //***************************************************************************************************************//
    //                                     4: Data source close and start(3)                                         //
    //***************************************************************************************************************//
    public void close() throws SQLException {
        if (jndiDs) return;

        if (isBeeDs) {
            beeDs.close();
        } else if (isBeeJtaDs) {
            beeJtaDs.close();
        }
    }

    public void restart(boolean force) throws SQLException {
        if (isBeeDs) {
            ((BeeDataSource) ds).restart(force);
        } else if (isBeeJtaDs) {
            beeJtaDs.restart(force);
        }
    }

    public boolean isClosed() throws SQLException {
        if (isBeeDs) {
            return beeDs.isClosed();
        } else if (isBeeJtaDs) {
            return beeJtaDs.isClosed();
        } else {//jndi datasource
            return false;
        }
    }

    //***************************************************************************************************************//
    //                                     5: Log print(2)                                                              //
    //***************************************************************************************************************//
    public boolean isEnabledLogPrint() throws SQLException {
        if (isBeeDs) {
            return beeDs.isEnabledLogPrint();
        } else if (isBeeJtaDs) {
            return beeJtaDs.isEnabledLogPrint();
        } else {
            return false;
        }
    }

    public void enableLogPrint(boolean printRuntimeLog) throws SQLException {
        if (isBeeDs) {
            beeDs.enableLogPrint(printRuntimeLog);
        } else if (isBeeJtaDs) {
            beeJtaDs.enableLogPrint(printRuntimeLog);
        }
    }

    //***************************************************************************************************************//
    //                                     6: Method execution Log(5)                                                //
    //***************************************************************************************************************//
    public boolean isEnabledMethodExecutionLogCache() throws SQLException {
        if (isBeeDs) {
            return this.beeDs.isEnabledMethodExecutionLogCache();
        } else if (isBeeJtaDs) {
            return this.beeJtaDs.isEnabledMethodExecutionLogCache();
        } else {
            return false;
        }
    }

    public void enableMethodExecutionLogCache(boolean enable) throws SQLException {
        if (isBeeDs) {
            this.beeDs.enableMethodExecutionLogCache(enable);
        } else if (isBeeJtaDs) {
            this.beeJtaDs.enableMethodExecutionLogCache(enable);
        }
    }

    public List<BeeMethodExecutionLog> getMethodExecutionLog(int type) throws SQLException {
        if (isBeeDs) {
            return beeDs.getMethodExecutionLog(type);
        } else if (isBeeJtaDs) {
            return beeJtaDs.getMethodExecutionLog(type);
        } else {
            return Collections.emptyList();
        }
    }

    public boolean cancelStatement(String logId) throws SQLException {
        if (isBeeDs) {
            return beeDs.cancelStatement(logId);
        } else if (isBeeJtaDs) {
            return beeJtaDs.cancelStatement(logId);
        } else {
            return false;
        }
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener listener) throws SQLException {
        if (isBeeDs) {
            this.beeDs.setMethodExecutionListener(listener);
        } else if (isBeeJtaDs) {
            this.beeJtaDs.setMethodExecutionListener(listener);
        }
    }

    //***************************************************************************************************************//
    //                                    7: Other implementation methods (7)                                        //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return isDs ? ds.getLogWriter() : xaDs.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        if (isDs) {
            ds.setLogWriter(out);
        } else {
            xaDs.setLogWriter(out);
        }
    }

    public int getLoginTimeout() throws SQLException {
        return isDs ? ds.getLoginTimeout() : xaDs.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        if (isDs) {
            ds.setLoginTimeout(seconds);
        } else {
            xaDs.setLoginTimeout(seconds);
        }
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return isDs ? ds.getParentLogger() : xaDs.getParentLogger();
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
