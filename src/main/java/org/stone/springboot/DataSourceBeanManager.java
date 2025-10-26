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
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.springboot.exception.DataSourceException;
import org.stone.springboot.factory.SpringDataSourceFactory;
import org.stone.springboot.factory.SpringXADataSourceFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * A Management tool to maintain registered datasource bean.
 *
 * @author Chris Liao
 */
public final class DataSourceBeanManager extends SpringConfigurationLoader {
    private static final DataSourceBeanManager single = new DataSourceBeanManager();

    //Data source map
    private final Map<String, DataSourceBean> dataSourceMap = new ConcurrentHashMap<>(1);

    public static DataSourceBeanManager getInstance() {
        return single;
    }

    private static DataSourceBean createJndiDataSourceBean(String dsId, String jndiName, boolean isPrimary) {
        try {
            Object namingObj = new InitialContext().lookup(jndiName);
            if (namingObj instanceof DataSource) {
                return new DataSourceBean(dsId, true, isPrimary, namingObj);
            } else {
                throw new DataSourceException("The object is not a ata source object with jndi name '" + jndiName + "'");
            }
        } catch (NamingException e) {
            throw new DataSourceException("Failed to lookup jndi object with jndi name '" + jndiName + "'", e);
        }
    }

    //***************************************************************************************************************//
    //                                     1: ds maintenance(4)                                                      //
    //***************************************************************************************************************//
    public DataSourceBean getDataSource(String dsId) {
        return dataSourceMap.get(dsId);
    }

    public void addDataSource(DataSourceBean ds) {
        dataSourceMap.put(ds.getDsId(), ds);
    }

    public void clearDsPool(String dsId, boolean force) throws SQLException {
        DataSourceBean ds = dataSourceMap.get(dsId);
        if (ds != null) ds.clear(force);
    }

    public List<BeeConnectionPoolMonitorVo> getDsPoolMonitorVoList() throws SQLException {
        List<BeeConnectionPoolMonitorVo> poolMonitorVoList = new ArrayList<>(dataSourceMap.size());
        Iterator<DataSourceBean> iterator = dataSourceMap.values().iterator();

        while (iterator.hasNext()) {
            DataSourceBean ds = iterator.next();
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.isClosed()) {//POOL_CLEARING,POOL_CLOSED
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
    public Collection<BeeMethodExecutionLog> getSqlExecutionList() {
        return null;
    }

    public void cancelStatementExecution(String statementUUID) throws SQLException {
//        if (statementExecutionCollector != null) {
//            statementExecutionCollector.cancelStatementExecution(statementUUID);
//        }
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

    private DataSourceBean createDataSourceBeanByDsType(String prefix, String dsId, Environment environment, boolean isPrimary) {
        //1: get configuration ds,factory
        String dsClassName = getConfigValue(prefix, Config_DS_Type, environment);
        String factoryClassName = getConfigValue(prefix, Config_DS_Factory, environment);
        if (isBlank(dsClassName)) {
            dsClassName = BeeDataSource.class.getName();
            if (isBlank(factoryClassName)) factoryClassName = SpringDataSourceFactory.class.getName();
        }

        //2: load class of Datasource/XaDatasource
        Class<?> dsClass;
        try {
            dsClass = Class.forName(dsClassName);
        } catch (ClassNotFoundException e) {
            throw new DataSourceException("Not found Datasource/XaDatasource class name:" + dsClassName);
        }
        if (!DataSource.class.isAssignableFrom(dsClass) && !XADataSource.class.isAssignableFrom(dsClass))
            throw new DataSourceException("Invalid class,the configured class must implement interface " + DataSource.class.getName()
                    + " or interface " + XADataSource.class.getName());

        //3: load factory class
        Object dsFactory = null;
        if (isNotBlank(factoryClassName)) {
            Class<?> dsFactoryClass;
            try {
                dsFactoryClass = Class.forName(factoryClassName);
            } catch (ClassNotFoundException e) {
                throw new DataSourceException("Not found datasource factory class name:" + factoryClassName);
            }

            if (!SpringDataSourceFactory.class.isAssignableFrom(dsFactoryClass) && !SpringXADataSourceFactory.class.isAssignableFrom(dsFactoryClass))
                throw new DataSourceException("Invalid datasource factory class,must implement interface " + SpringDataSourceFactory.class.getName()
                        + " or interface " + SpringXADataSourceFactory.class.getName());
            try {
                dsFactory = dsFactoryClass.getDeclaredConstructor(new Class[0]).newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new DataSourceException("Failed to create data source factory", e);
            }
        }

        //4: Create data source bean
        Object ds;
        if (dsFactory instanceof SpringXADataSourceFactory) {
            ds = ((SpringXADataSourceFactory) dsFactory).createXADataSource(prefix, dsId, environment);
        } else if (dsFactory instanceof SpringDataSourceFactory) {
            ds = ((SpringDataSourceFactory) dsFactory).createDataSource(prefix, dsId, environment);
        } else {
            ds = createDataSourceByClassName(dsId, dsClass);
            setConfigPropertiesValue(ds, prefix, dsId, environment);
        }

        return new DataSourceBean(dsId, false, isPrimary, ds);
    }
}
