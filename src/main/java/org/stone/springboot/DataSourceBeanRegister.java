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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.beecp.BeeDataSource;
import org.stone.springboot.annotation.EnableBeeDs;
import org.stone.springboot.builder.SpringDataSourceBuilder;
import org.stone.springboot.builder.SpringXADataSourceBuilder;
import org.stone.springboot.exception.ConfigurationException;
import org.stone.springboot.exception.DataSourceException;
import org.stone.springboot.monitor.MonitorBeansRegister;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * #ids of datasource
 * spring.datasource.dsId=ds1,ds2,ds3
 *
 * #ds1
 * spring.datasource.ds1.primary=true
 * spring.datasource.ds1.type=org.stone.beecp.BeeDataSource
 * spring.datasource.ds1.username=root
 * spring.datasource.ds1.password=root
 * spring.datasource.ds1.jdbcUrl=jdbc:mysql://localhost:3306/test
 * spring.datasource.ds1.driverClassName=com.mysql.jdbc.Driver
 * spring.datasource.ds1.fairMode=true
 * spring.datasource.ds1.initialSize=10
 * spring.datasource.ds1.maxActive=10
 * spring.datasource.ds1.enableMethodExecutionLogCache=true
 * ......
 *
 * #ds2
 * spring.datasource.ds2.primary=false
 * spring.datasource.ds2.type=org.stone.beecp.BeeDataSource
 * spring.datasource.ds2.username=root
 * spring.datasource.ds2.password=root
 * spring.datasource.ds2.jdbcUrl=jdbc:mysql://localhost:3306/test
 * spring.datasource.ds2.driverClassName=com.mysql.jdbc.Driver
 * spring.datasource.ds2.fairMode=true
 * spring.datasource.ds2.initialSize=10
 * spring.datasource.ds2.maxActive=10
 * spring.datasource.ds2.enableMethodExecutionLogCache=true
 * ......
 *
 * #ds3
 * spring.datasource.ds3.primary=false
 * spring.datasource.ds3.jndiName=DsJndi
 *
 *
 * @author Chris Liao
 */
public class DataSourceBeanRegister implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DataSourceBeanManager dsBeanManager;
    private Environment environment;

    public DataSourceBeanRegister() {
        this.dsBeanManager = DataSourceBeanManager.getInstance();
    }

    public final void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Loads Data Source configuration from spring environment and Creates Data source
     *
     * @param classMetadata Annotation use class meta
     * @param registry      spring boot bean definition registry factory
     */
    public final void registerBeanDefinitions(AnnotationMetadata classMetadata, BeanDefinitionRegistry registry) {
        //1: read out data source id list
        List<String> dsIdList = getDsIdList(environment, registry);

        //2: Creates Data source beans
        Map<String, DataSourceBean> dsMap = this.createDataSourceBean(dsIdList, environment);

        //3: Registers Created Data source beans to springboot
        this.registerDataSourceBean(dsMap, registry);

        //4: Registers monitor controller to springboot
        Map<String, Object> attributes = classMetadata.getAnnotationAttributes(EnableBeeDs.class.getName(), false);
        if (((Boolean) attributes.get(Annotation_Console_Attribute_Name)).booleanValue()) {//enable web console
            new MonitorBeansRegister().registerBeanDefinitions(classMetadata, registry, environment);
        }
    }


    //***************************************************************************************************************//
    //                                    2: Private methods(3)                                                      //
    //***************************************************************************************************************//

    /**
     * 1: get configured datasource id list
     *
     * @param environment spring boot environment
     * @param registry    spring boot registry
     * @return datasource name list
     */
    private List<String> getDsIdList(Environment environment, BeanDefinitionRegistry registry) {
        String dsIdsText = SpringBootEnvironmentUtil.getConfigValue(Config_DS_Prefix, Config_DS_Id, environment);
        if (isNotBlank(dsIdsText)) {
            List<String> dsIdList = new ArrayList<>(1);
            for (String id : dsIdsText.trim().split(",")) {
                if (isBlank(id))
                    throw new ConfigurationException("Blank value is not allowed on key:" + Config_DS_Prefix + "." + Config_DS_Id);

                if (dsIdList.contains(id)) continue;//skip
                dsIdList.add(id);
            }

            return dsIdList;
        } else {
            throw new ConfigurationException("Not found configuration key:" + Config_DS_Prefix + "." + Config_DS_Id);
        }
    }

    /**
     * 2: Loads Data source configuration and build Data source beans.
     *
     * @param dsIdList    datasource name list
     * @param environment spring boot environment
     * @return dataSource holder map
     */
    private Map<String, DataSourceBean> createDataSourceBean(List<String> dsIdList, Environment environment) {
        Map<String, DataSourceBean> dsMap = new LinkedHashMap<>(dsIdList.size());
        try {
            for (String dsId : dsIdList) {
                String dsPrefix = Config_DS_Prefix + "." + dsId;
                dsMap.put(dsId, createDataSourceBean(dsPrefix, dsId, environment));//create datasource instance
            }
            return dsMap;
        } catch (Throwable e) {//failed then close all created dataSource
            for (DataSourceBean ds : dsMap.values()) {
                try {
                    ds.close();
                } catch (SQLException ee) {
                    //do nothing
                }
            }
            throw new DataSourceException("Data source build failed", e);
        }
    }

    /**
     * 3: Registers Data source to Spring container
     *
     * @param dsMap datasource list
     */
    private void registerDataSourceBean(Map<String, DataSourceBean> dsMap, BeanDefinitionRegistry registry) {
        for (DataSourceBean ds : dsMap.values()) {
            if (SpringBootEnvironmentUtil.existsBeanDefinition(ds.getDsId(), registry)) {
                log.warn("A bean definition existed in spring container with id:{}", ds.getDsId());
            } else {
                GenericBeanDefinition define = new GenericBeanDefinition();//new a spring bean definition object to be registered
                define.setPrimary(ds.isPrimary());
                define.setBeanClass(ds.getClass());
                define.setInstanceSupplier(SpringBootEnvironmentUtil.createSpringSupplier(ds));
                registry.registerBeanDefinition(ds.getDsId(), define);
                log.info("Data source has been registered success with id:{}", ds.getDsId());
                dsBeanManager.addDataSource(ds);
            }
        }
    }

    //***************************************************************************************************************//
    //                                    3: Data source Creation                                                        //
    //***************************************************************************************************************//
    DataSourceBean createDataSourceBean(String prefix, String dsId, Environment environment) {
        String jndiName = SpringBootEnvironmentUtil.getConfigValue(prefix, Config_DS_Jndi, environment);
        String primaryText = SpringBootEnvironmentUtil.getConfigValue(prefix, Config_DS_Primary, environment);
        boolean isPrimary = isNotBlank(primaryText) && Boolean.valueOf(primaryText).booleanValue();

        if (isNotBlank(jndiName)) {//jndi dataSource
            return createJndiDataSourceBean(dsId, jndiName, isPrimary);
        } else {//independent type
            return createDataSourceBeanByDsType(prefix, dsId, environment, isPrimary);
        }
    }

    private DataSourceBean createJndiDataSourceBean(String dsId, String jndiName, boolean isPrimary) {
        try {
            Object namingObj = new InitialContext().lookup(jndiName);
            if (namingObj instanceof DataSource) {
                return new DataSourceBean(dsId, true, isPrimary, namingObj);
            } else {
                throw new DataSourceException("The jndi object is not a data source with name:" + jndiName);
            }
        } catch (NamingException e) {
            throw new DataSourceException("Failed to lookup jndi object  with name:" + jndiName, e);
        }
    }

    private DataSourceBean createDataSourceBeanByDsType(String prefix, String dsId, Environment environment, boolean isPrimary) {
        //1: get data source class name and class name of factory from spring boot configuration
        String dsClassName = SpringBootEnvironmentUtil.getConfigValue(prefix, Config_DS_Type, environment);
        String factoryClassName = SpringBootEnvironmentUtil.getConfigValue(prefix, Config_DS_Factory, environment);
        if (isBlank(dsClassName)) {
            dsClassName = BeeDataSource.class.getName();
            if (isBlank(factoryClassName)) factoryClassName = SpringDataSourceBuilder.class.getName();
        }

        //2: load class of Datasource/XaDatasource by class name
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

            if (!SpringDataSourceBuilder.class.isAssignableFrom(dsFactoryClass) && !SpringXADataSourceBuilder.class.isAssignableFrom(dsFactoryClass))
                throw new DataSourceException("Invalid datasource factory class,must implement interface " + SpringDataSourceBuilder.class.getName()
                        + " or interface " + SpringXADataSourceBuilder.class.getName());
            try {
                dsFactory = dsFactoryClass.getDeclaredConstructor(new Class[0]).newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new DataSourceException("Failed to create data source factory", e);
            }
        }

        //4: Create data source bean
        Object ds;
        if (dsFactory instanceof SpringXADataSourceBuilder) {
            ds = ((SpringXADataSourceBuilder) dsFactory).create(prefix, dsId, environment);
        } else if (dsFactory instanceof SpringDataSourceBuilder) {
            ds = ((SpringDataSourceBuilder) dsFactory).create(prefix, dsId, environment);
        } else {
            ds = SpringBootEnvironmentUtil.createDataSourceByClassName(dsId, dsClass);
            SpringBootEnvironmentUtil.setConfigPropertiesValue(ds, prefix, dsId, environment);
        }

        //5: create a data source bean
        return new DataSourceBean(dsId, false, isPrimary, ds);
    }
}

