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

import org.springframework.core.env.Environment;
import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeDataSource;
import org.stone.springboot.factory.SpringBeeDataSourceFactory;
import org.stone.springboot.factory.SpringDataSourceException;
import org.stone.springboot.factory.SpringDataSourceFactory;
import org.stone.springboot.sql.SqlExecution;
import org.stone.springboot.sql.SqlExecutionWorkshop;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * A manager to maintain a set of data sources registered in spring container.
 *
 * @author Chris Liao
 */
public final class DataSourceBeanManager extends SpringConfigurationLoader {
    private static final DataSourceBeanManager single = new DataSourceBeanManager();
    //data sources map
    private final Map<String, DataSourceBean> dataSourceMap;
    //data source factories map
    private final Map<Class<? extends DataSource>, SpringDataSourceFactory> factoryMap;
    //sql execution workshop
    private SqlExecutionWorkshop sqlExecutionWorkshop;

    private DataSourceBeanManager() {
        this.factoryMap = new HashMap<>(1);
        this.dataSourceMap = new ConcurrentHashMap<>(1);
        this.factoryMap.put(BeeDataSource.class, new SpringBeeDataSourceFactory());
    }

    public static DataSourceBeanManager getInstance() {
        return single;
    }

    //***************************************************************************************************************//
    //                                     1: ds maintenance(4)                                                      //
    //***************************************************************************************************************//
    public DataSourceBean getDataSource(String dsId) {
        return dataSourceMap.get(dsId);
    }

    public void addDataSource(DataSourceBean ds) {
        dataSourceMap.put(ds.getDsId(), ds);
        ds.setWorkshop(sqlExecutionWorkshop);
    }

    public void clearDataSourcePool(String dsId, boolean force) throws SQLException {
        DataSourceBean ds = dataSourceMap.get(dsId);
        if (ds != null) ds.clear(force);
    }

    public List<BeeConnectionPoolMonitorVo> getDataSourceMonitoringVoList() throws SQLException {
        List<BeeConnectionPoolMonitorVo> poolMonitorVoList = new ArrayList<>(dataSourceMap.size());
        Iterator<DataSourceBean> iterator = dataSourceMap.values().iterator();

        while (iterator.hasNext()) {
            DataSourceBean ds = iterator.next();
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    //***************************************************************************************************************//
    //                                     2: sql-trace (1)                                                          //
    //***************************************************************************************************************//
    public Collection<SqlExecution> getSqlExecutionList() {
        return sqlExecutionWorkshop != null ? sqlExecutionWorkshop.getSqlTraceQueue() : null;
    }

    public void setSqlExecutionWorkshop(SqlExecutionWorkshop statementPool) {
        this.sqlExecutionWorkshop = statementPool;
        for (DataSourceBean ds : dataSourceMap.values())
            ds.setWorkshop(sqlExecutionWorkshop);
    }

    //***************************************************************************************************************//
    //                                     3: Create Data source Bean                                                //
    //***************************************************************************************************************//
    public DataSourceBean createDataSourceBean(String prefix, String dsId, Environment environment) {
        String jndiNameTex = getConfigValue(prefix, Config_DS_Jndi, environment);
        String primaryText = getConfigValue(prefix, Config_DS_Primary, environment);
        boolean isPrimary = isNotBlank(primaryText) && Boolean.valueOf(primaryText).booleanValue();

        if (isNotBlank(jndiNameTex)) {//jndi dataSource
            return createJndiDataSourceBean(dsId, jndiNameTex, isPrimary);
        } else {//independent type
            return createDataSourceBeanByDsType(prefix, dsId, environment, isPrimary);
        }
    }

    private DataSourceBean createJndiDataSourceBean(String dsId, String jndiName, boolean isPrimary) {
        try {
            Object namingObj = new InitialContext().lookup(jndiName);
            if (namingObj instanceof DataSource) {
                return new DataSourceBean(dsId, true, isPrimary, (DataSource) namingObj);
            } else {
                throw new SpringDataSourceException("DataSource(" + dsId + ")-Jndi Name(" + jndiName + ") is not a data source object");
            }
        } catch (NamingException e) {
            throw new SpringDataSourceException("DataSource(" + dsId + ")-Failed to lookup data source by jndi-name:" + jndiName);
        }
    }

    private DataSourceBean createDataSourceBeanByDsType(String prefix, String dsId, Environment environment, boolean isPrimary) {
        //1:load dataSource class
        String dsClassName = getConfigValue(prefix, Config_DS_Type, environment);
        dsClassName = isBlank(dsClassName) ? BeeDataSource.class.getName() : dsClassName.trim();

        //2:create dataSource class
        Class<?> dsClass;
        try {
            dsClass = Class.forName(dsClassName);
        } catch (ClassNotFoundException e) {
            throw new SpringDataSourceException("DataSource(" + dsId + ")-Not found class:" + dsClassName);
        }

        //3:create dataSource
        DataSource ds;
        SpringDataSourceFactory dsFactory = factoryMap.get(dsClass);
        if (dsFactory == null && SpringDataSourceFactory.class.isAssignableFrom(dsClass))
            dsFactory = (SpringDataSourceFactory) createInstanceByClassName(dsId, dsClass);
        if (dsFactory != null) {//create by factory
            try {
                ds = dsFactory.createDataSource(prefix, dsId, environment);
            } catch (SpringDataSourceException e) {
                throw e;
            } catch (Exception e) {
                throw new SpringDataSourceException("DataSource(" + dsId + ")-Failed to get instance from dataSource factory", e);
            }
        } else if (DataSource.class.isAssignableFrom(dsClass)) {
            ds = (DataSource) createInstanceByClassName(dsId, dsClass);
            setConfigPropertiesValue(ds, prefix, dsId, environment);
        } else {
            throw new SpringDataSourceException("DataSource(" + dsId + ")-target type is not a valid data source type");
        }

        return new DataSourceBean(dsId, false, isPrimary, ds);
    }
}
