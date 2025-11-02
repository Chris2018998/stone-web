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
import org.stone.springboot.annotation.EnableBeeDs;
import org.stone.springboot.controller.MonitorControllerRegister;
import org.stone.springboot.exception.ConfigurationException;
import org.stone.springboot.exception.DataSourceException;

import java.sql.SQLException;
import java.util.*;

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
public class DataSourceBeansRegister implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DataSourceBeanManager dsBeanManager;
    private Environment environment;

    public DataSourceBeansRegister() {
        this.dsBeanManager = DataSourceBeanManager.getInstance();
    }

    public final void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * A override method to register data sources to spring ioc container
     *
     * @param classMetadata Annotation use class meta
     * @param registry      spring boot bean definition registry factory
     */
    public final void registerBeanDefinitions(AnnotationMetadata classMetadata, BeanDefinitionRegistry registry) {
        //1: read configured id list of datasource
        List<String> dsIdList = getDsIdList(environment, registry);

        //2: create datasource with configuration
        Map<String, DataSourceBean> dsMap = this.createDataSourceBean(dsIdList, environment);

        //3: register datasource to spring container
        this.registerDataSourceBean(dsMap, registry);

        //4: attempt to register monitor
        Map<String, Object> attributes = classMetadata.getAnnotationAttributes(EnableBeeDs.class.getName(), false);
        if ((boolean) attributes.get(Annotation_Monitor_Attribute_Name)) {
            new MonitorControllerRegister().registerBeanDefinitions(classMetadata, registry, environment);
        }
    }

    /**
     * 1: get datasource config id list
     *
     * @param environment spring boot environment
     * @param registry    spring boot registry
     * @return datasource name list
     */
    private List<String> getDsIdList(Environment environment, BeanDefinitionRegistry registry) {
        String dsIdsText = dsBeanManager.getConfigValue(Config_DS_Prefix, Config_DS_Id, environment);
        if (isBlank(dsIdsText))
            throw new ConfigurationException("Missed or not found config item '" + Config_DS_Prefix + "." + Config_DS_Id + "'");

        String[] dsIds = dsIdsText.trim().split(",");
        List<String> dsIdList = new ArrayList<>(dsIds.length);
        for (String id : dsIds) {
            if (isBlank(id) || dsIdList.contains(id)) continue;
            if (SpringConfigurationLoader.existsBeanDefinition(id, registry))
                throw new ConfigurationException("Existed a registered bean with id '" + id + "'");

            dsIdList.add(id);
        }

        if (dsIdList.isEmpty())
            throw new ConfigurationException("Missed or not found config item '" + Config_DS_Prefix + "." + Config_DS_Id + "'");

        return dsIdList;
    }

    /**
     * 2: get dynamic config info
     *
     * @param dsIdList    datasource name list
     * @param environment spring boot environment
     * @return datasource name list
     */
    private Properties getDynamicDsInfo(List<String> dsIdList, Environment environment, BeanDefinitionRegistry registry) {
        String dynId = dsBeanManager.getConfigValue(Config_DS_Prefix, Config_Dyn_DS_Id, environment);
        String primaryDs = dsBeanManager.getConfigValue(Config_DS_Prefix, Config_Dyn_DS_PrimaryId, environment);

        dynId = (dynId == null) ? "" : dynId;
        primaryDs = (primaryDs == null) ? "" : primaryDs;

        if (isNotBlank(dynId)) {
            if (dsIdList.contains(dynId))
                throw new ConfigurationException("Dynamic dataSource id '" + dynId + "' can't be in ds-id list");
            if (SpringConfigurationLoader.existsBeanDefinition(dynId, registry))
                throw new ConfigurationException("Dynamic dataSource id '" + dynId + "' has been registered by another bean");

            if (isBlank(primaryDs))
                throw new ConfigurationException("Missed or not found config item '" + Config_DS_Prefix + "." + Config_Dyn_DS_PrimaryId + "'");
            if (!dsIdList.contains(primaryDs.trim()))
                throw new ConfigurationException("Dynamic primaryDs '" + primaryDs + "' not found in ds-id list");
        }

        Properties combineProperties = new Properties();
        combineProperties.put(Config_Dyn_DS_Id, dynId);
        combineProperties.put(Config_Dyn_DS_PrimaryId, primaryDs);
        return combineProperties;
    }

    /**
     * 3: create dataSource by config
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
                dsMap.put(dsId, dsBeanManager.createDataSourceBean(dsPrefix, dsId, environment));//create datasource instance
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
            throw new DataSourceException("Multi data source created failed", e);
        }
    }

    /**
     * 4: register datasource to springBoot
     *
     * @param dsMap datasource list
     */
    private void registerDataSourceBean(Map<String, DataSourceBean> dsMap, BeanDefinitionRegistry registry) {
        for (DataSourceBean ds : dsMap.values())
            registerDataSourceBean(ds, registry);

    }

    //4.1:register dataSource to Spring bean container
    private void registerDataSourceBean(DataSourceBean springDs, BeanDefinitionRegistry registry) {
        GenericBeanDefinition define = new GenericBeanDefinition();
        define.setPrimary(springDs.isPrimary());
        define.setBeanClass(springDs.getClass());
        define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(springDs));
        registry.registerBeanDefinition(springDs.getDsId(), define);
        log.info("Registered a data source(type:{})with bean Id '{}'", define.getBeanClassName(), springDs.getDsId());
        dsBeanManager.addDataSource(springDs);
    }
}

