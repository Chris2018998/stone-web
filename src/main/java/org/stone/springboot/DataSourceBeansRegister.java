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
import org.stone.springboot.dynamic.DynamicAspect;
import org.stone.springboot.dynamic.DynamicDataSource;
import org.stone.springboot.factory.SpringDataSourceException;

import java.sql.SQLException;
import java.util.*;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;

/*
 * example of multi-dataSource
 *
 * spring.datasource.dsId=ds1,ds2
 * spring.datasource.ds1.type=org.stone.beecp.BeeDataSource
 * spring.datasource.ds1.primary=true
 *
 * spring.datasource.ds2.primary=false
 * spring.datasource.ds2.jndiName=DsJndi
 *
 * @author Chris Liao
 */
public class DataSourceBeansRegister implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    //spring boot env
    private Environment environment;

    //spring boot env
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

        //2: load configuration of monitor
        MonitoringConfigManager.getInstance().loadMonitorConfig(environment);

        //3: load configuration for dynamic datasource
        Properties dynamicSourceProperties = getDynamicDsInfo(dsIdList, environment, registry);

        //4: create datasource with configuration
        Map<String, DataSourceBean> dsMap = createDataSources(dsIdList, environment);

        //5: register datasource to spring container
        this.registerDataSources(dsMap, dynamicSourceProperties, registry);
    }

    /**
     * 1: get datasource config id list
     *
     * @param environment spring boot environment
     * @param registry    spring boot registry
     * @return datasource name list
     */
    private List<String> getDsIdList(Environment environment, BeanDefinitionRegistry registry) {
        String dsIdsText = DataSourceBeanManager.getInstance().getConfigValue(Config_DS_Prefix, Config_DS_Id, environment);
        if (isBlank(dsIdsText))
            throw new SpringDataSourceException("Missed or not found config item:" + Config_DS_Prefix + "." + Config_DS_Id);

        String[] dsIds = dsIdsText.trim().split(",");
        ArrayList<String> dsIdList = new ArrayList<>(dsIds.length);
        for (String id : dsIds) {
            if (isBlank(id)) continue;

            id = id.trim();
            if (dsIdList.contains(id))
                throw new SpringDataSourceException("Duplicated id(" + id + ")in multi-datasource id list");
            if (DataSourceBeanManager.getInstance().existsBeanDefinition(id, registry))
                throw new SpringDataSourceException("DataSource id(" + id + ")has been registered by another bean");

            dsIdList.add(id);
        }
        if (dsIdList.isEmpty())
            throw new SpringDataSourceException("Missed or not found config item:" + Config_DS_Prefix + "." + Config_DS_Id);

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
        String combineId = DataSourceBeanManager.getInstance().getConfigValue(Config_DS_Prefix, Config_Dyn_DS_Id, environment);
        String primaryDs = DataSourceBeanManager.getInstance().getConfigValue(Config_DS_Prefix, Config_Dyn_DS_PrimaryId, environment);

        combineId = (combineId == null) ? "" : combineId;
        primaryDs = (primaryDs == null) ? "" : primaryDs;

        if (!isBlank(combineId)) {
            if (dsIdList.contains(combineId))
                throw new SpringDataSourceException("Dynamic-dataSource id (" + combineId + ")can't be in ds-id list");
            if (DataSourceBeanManager.getInstance().existsBeanDefinition(combineId, registry))
                throw new SpringDataSourceException("Dynamic-dataSource id(" + combineId + ")has been registered by another bean");

            if (isBlank(primaryDs))
                throw new SpringDataSourceException("Missed or not found config item:" + Config_DS_Prefix + "." + Config_Dyn_DS_PrimaryId);
            if (!dsIdList.contains(primaryDs.trim()))
                throw new SpringDataSourceException("Dynamic-primaryDs(" + primaryDs + "not found in ds-id list");
        }

        Properties combineProperties = new Properties();
        combineProperties.put(Config_Dyn_DS_Id, combineId);
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
    private Map<String, DataSourceBean> createDataSources(List<String> dsIdList, Environment environment) {
        DataSourceBeanManager manager = DataSourceBeanManager.getInstance();
        Map<String, DataSourceBean> dsMap = new LinkedHashMap<>(dsIdList.size());
        try {
            for (String dsId : dsIdList) {
                String dsPrefix = Config_DS_Prefix + "." + dsId;
                dsMap.put(dsId, manager.createDataSourceBean(dsPrefix, dsId, environment));//create datasource instance
            }
            return dsMap;
        } catch (Throwable e) {//failed then close all created dataSource
            for (DataSourceBean ds : dsMap.values()) {
                try {
                    ds.close();
                } catch (SQLException ee) {

                }
            }
            throw new SpringDataSourceException("multi-DataSource created failed", e);
        }
    }

    /**
     * 4: register datasource to springBoot
     *
     * @param dsMap datasource list
     */
    private void registerDataSources(Map<String, DataSourceBean> dsMap, Properties combineProperties, BeanDefinitionRegistry registry) {
        String combineDsId = combineProperties.getProperty(Config_Dyn_DS_Id);
        String primaryDsId = combineProperties.getProperty(Config_Dyn_DS_PrimaryId);

        for (DataSourceBean ds : dsMap.values())
            registerDataSourceBean(ds, registry);


        //register combine DataSource
        if (!isBlank(combineDsId) && !isBlank(primaryDsId)) {
            ThreadLocal<DataSourceBean> dsThreadLocal = new ThreadLocal<>();

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(DynamicDataSource.class);
            define.setInstanceSupplier(DataSourceBeanManager.getInstance().createSpringSupplier(new DynamicDataSource(dsThreadLocal)));
            registry.registerBeanDefinition(combineDsId, define);
            log.info("Registered Combine-DataSource({})with id:{}", define.getBeanClassName(), combineDsId);

            String dsIdSetterId = DynamicAspect.class.getName();
            GenericBeanDefinition dsIdSetDefine = new GenericBeanDefinition();
            dsIdSetDefine.setBeanClass(DynamicAspect.class);
            dsIdSetDefine.setInstanceSupplier(DataSourceBeanManager.getInstance().createSpringSupplier(new DynamicAspect(primaryDsId, dsThreadLocal)));
            registry.registerBeanDefinition(dsIdSetterId, dsIdSetDefine);
            log.info("Registered DsId-setter({})with id:{}", dsIdSetDefine.getBeanClassName(), dsIdSetterId);
        }
    }

    //4.1:register dataSource to Spring bean container
    private void registerDataSourceBean(DataSourceBean springDs, BeanDefinitionRegistry registry) {
        GenericBeanDefinition define = new GenericBeanDefinition();
        define.setPrimary(springDs.isPrimary());
        define.setBeanClass(springDs.getClass());
        define.setInstanceSupplier(DataSourceBeanManager.getInstance().createSpringSupplier(springDs));
        registry.registerBeanDefinition(springDs.getDsId(), define);
        log.info("Registered DataSource({})with id:{}", define.getBeanClassName(), springDs.getDsId());
        DataSourceBeanManager.getInstance().addDataSource(springDs);
    }
}

