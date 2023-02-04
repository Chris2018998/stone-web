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
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.jta.BeeJtaDataSource;
import org.stone.beecp.pool.ConnectionPoolMonitorVo;
import org.stone.springboot.sqlTrace.StatementTracePool;
import org.stone.springboot.sqlTrace.StatementTraceUtil;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;

import static org.stone.springboot.assembly.SpringBootDataSourceUtil.tryToCloseDataSource;

/**
 * springboot registered datasource
 *
 * @author Chris Liao
 */
public final class SpringDataSource implements DataSource {
    private final static Logger Log = LoggerFactory.getLogger(SpringDataSource.class);
    private final String dsId;
    private final String dsUUID;
    private final DataSource ds;
    private final boolean jndiDs;
    private final boolean isBeeDs;

    private boolean primary;
    private StatementTracePool statementPool;
    private Method poolRestartMethod;
    private Method poolMonitorVoMethod;
    private boolean notSetBeeDsId = true;

    public SpringDataSource(String dsId, DataSource ds, boolean jndiDs) {
        this.dsId = dsId;
        this.ds = ds;
        this.jndiDs = jndiDs;

        this.isBeeDs = ds instanceof BeeDataSource || ds instanceof BeeJtaDataSource;
        if (isBeeDs) readBeeDsMethods();
        this.dsUUID = "SpringDs_" + UUID.randomUUID().toString();
    }

    String getDsId() {
        return dsId;
    }

    boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setStatementPool(StatementTracePool statementPool) {
        this.statementPool = statementPool;
    }

    public Connection getConnection() throws SQLException {
        Connection con = ds.getConnection();
        return statementPool.isSqlTrace() ? StatementTraceUtil.createConnection(con, dsId, dsUUID, statementPool) : con;
    }

    public Connection getConnection(String username, String password) throws SQLException {
        Connection con = ds.getConnection(username, password);
        return statementPool.isSqlTrace() ? StatementTraceUtil.createConnection(con, dsId, dsUUID, statementPool) : con;
    }

    public PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds.getParentLogger();
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

    public void close() {
        if (!jndiDs) tryToCloseDataSource(ds);
    }

    public void restartPool() {
        if (poolRestartMethod != null) {
            try {
                poolRestartMethod.invoke(ds, false);
            } catch (Throwable e) {
                Log.warn("Failed to execute dataSource 'clear' method", e);
            }
        }
    }

    ConnectionPoolMonitorVo getPoolMonitorVo() {
        if (poolMonitorVoMethod != null) {
            try {
                ConnectionPoolMonitorVo vo = (ConnectionPoolMonitorVo) poolMonitorVoMethod.invoke(ds);
                if (notSetBeeDsId) setBeeDsIdToMonitorSingletonVo(vo);
                return vo;
            } catch (Throwable e) {
                Log.warn("Failed to execute dataSource 'getPoolMonitorVo' method", e);
            }
        }
        return null;
    }

    private synchronized void setBeeDsIdToMonitorSingletonVo(ConnectionPoolMonitorVo vo) {
        setValueToField(vo, "dsId", dsId);
        setValueToField(vo, "dsUUID", dsUUID);
        notSetBeeDsId = false;
    }

    private void setValueToField(ConnectionPoolMonitorVo vo, String fieldId, String value) {
        Field field = null;
        try {
            Class monitorVoClass = ConnectionPoolMonitorVo.class;
            field = monitorVoClass.getDeclaredField(fieldId);
            field.setAccessible(true);
            field.set(vo, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //do nothing
        } finally {
            if (field != null) field.setAccessible(false);
        }
    }

    private void readBeeDsMethods() {
        if (isBeeDs) {
            Class dsClass = ds.getClass();
            try {
                poolMonitorVoMethod = dsClass.getMethod("getPoolMonitorVo");
            } catch (NoSuchMethodException e) {
                Log.warn("DataSource method(getPoolMonitorVo) not found", e);
            }
            try {
                poolRestartMethod = dsClass.getMethod("restartPool", Boolean.TYPE);
            } catch (NoSuchMethodException e) {
                Log.warn("DataSource method(clear) not found", e);
            }
        }
    }
}
