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
package org.stone.springboot.assembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.CombineAspect;
import org.stone.springboot.CombineDataSource;
import org.stone.springboot.SpringDataSource;
import org.stone.springboot.StoneMonitorConfig;
import org.stone.springboot.factory.SpringBootDataSourceException;

import java.util.*;

import static org.stone.util.CommonUtil.isBlank;

/*
 *  SpringBoot dataSource config example
 *
 *  spring.datasource.dsId=ds1,ds2
 *  spring.datasource.ds1.type=cn.stone.BeeDataSoruce
 *  spring.datasource.ds1.primary=true
 *
 *  spring.datasource.ds2.primary=false
 *  spring.datasource.ds2.jndiName=DsJndi
 *
 *   @author Chris Liao
 */
public class MultiDataSourceRegister implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    //logger
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //springboot environment
    private Environment environment;

    /**
     * Read dataSource configuration from environment and create DataSource
     *
     * @param environment SpringBoot Environment
     */
    public final void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Read dataSource configuration from environment and create DataSource
     *
     * @param classMetadata Annotation use class meta
     * @param registry      springboot bean definition registry factory
     */
    public final void registerBeanDefinitions(AnnotationMetadata classMetadata, BeanDefinitionRegistry registry) {
        //1:read multi-dataSource id list
        List<String> dsIdList = getDsIdList(environment, registry);

        //2:read datasource controller config
        StoneMonitorConfig dataSourceMonitorConfig = SpringBootDataSourceUtil.readMonitorConfig(environment);

        //3:read combine-ds config
        Properties combineProperties = getCombineDsInfo(dsIdList, environment, registry);

        //4:create dataSources by id list
        Map<String, SpringDataSource> dsMap = createDataSources(dsIdList, environment);

        //5:read sql sqlTrace config
        SpringBootDataSourceManager.getInstance().setupSqlTrace(dataSourceMonitorConfig);

        //6:assembly datasource to spring container
        this.registerDataSources(dsMap, combineProperties, registry);
    }

    /**
     * 1: get datasource config id list
     *
     * @param environment springboot environment
     * @param registry    springboot registry
     * @return datasource name list
     */
    private List<String> getDsIdList(Environment environment, BeanDefinitionRegistry registry) {
        String dsIdsText = SpringBootDataSourceUtil.getConfigValue(SpringBootDataSourceUtil.Config_DS_Prefix, SpringBootDataSourceUtil.Config_DS_Id, environment);
        if (isBlank(dsIdsText))
            throw new SpringBootDataSourceException("Missed or not found config item:" + SpringBootDataSourceUtil.Config_DS_Prefix + "." + SpringBootDataSourceUtil.Config_DS_Id);

        String[] dsIds = dsIdsText.trim().split(",");
        ArrayList<String> dsIdList = new ArrayList<>(dsIds.length);
        for (String id : dsIds) {
            if (isBlank(id)) continue;

            id = id.trim();
            if (dsIdList.contains(id))
                throw new SpringBootDataSourceException("Duplicated id(" + id + ")in multi-datasource id list");
            if (SpringBootDataSourceUtil.existsBeanDefinition(id, registry))
                throw new SpringBootDataSourceException("DataSource id(" + id + ")has been registered by another bean");

            dsIdList.add(id);
        }
        if (dsIdList.isEmpty())
            throw new SpringBootDataSourceException("Missed or not found config item:" + SpringBootDataSourceUtil.Config_DS_Prefix + "." + SpringBootDataSourceUtil.Config_DS_Id);

        return dsIdList;
    }

    /**
     * 2: get combine config info
     *
     * @param dsIdList    datasource name list
     * @param environment springboot environment
     * @return datasource name list
     */
    private Properties getCombineDsInfo(List<String> dsIdList, Environment environment, BeanDefinitionRegistry registry) {
        String combineId = SpringBootDataSourceUtil.getConfigValue(SpringBootDataSourceUtil.Config_DS_Prefix, SpringBootDataSourceUtil.Config_DS_CombineId, environment);
        String primaryDs = SpringBootDataSourceUtil.getConfigValue(SpringBootDataSourceUtil.Config_DS_Prefix, SpringBootDataSourceUtil.Config_DS_Combine_PrimaryDs, environment);

        combineId = (combineId == null) ? "" : combineId;
        primaryDs = (primaryDs == null) ? "" : primaryDs;

        if (!isBlank(combineId)) {
            if (dsIdList.contains(combineId))
                throw new SpringBootDataSourceException("Combine-dataSource id (" + combineId + ")can't be in ds-id list");
            if (SpringBootDataSourceUtil.existsBeanDefinition(combineId, registry))
                throw new SpringBootDataSourceException("Combine-dataSource id(" + combineId + ")has been registered by another bean");

            if (isBlank(primaryDs))
                throw new SpringBootDataSourceException("Missed or not found config item:" + SpringBootDataSourceUtil.Config_DS_Prefix + "." + SpringBootDataSourceUtil.Config_DS_Combine_PrimaryDs);
            if (!dsIdList.contains(primaryDs.trim()))
                throw new SpringBootDataSourceException("Combine-primaryDs(" + primaryDs + "not found in ds-id list");
        }

        Properties combineProperties = new Properties();
        combineProperties.put(SpringBootDataSourceUtil.Config_DS_CombineId, combineId);
        combineProperties.put(SpringBootDataSourceUtil.Config_DS_Combine_PrimaryDs, primaryDs);
        return combineProperties;
    }

    /**
     * 3: create dataSource by config
     *
     * @param dsIdList    datasource name list
     * @param environment springboot environment
     * @return dataSource holder map
     */
    private Map<String, SpringDataSource> createDataSources(List<String> dsIdList, Environment environment) {
        Map<String, SpringDataSource> dsMap = new LinkedHashMap<>(dsIdList.size());
        try {
            for (String dsId : dsIdList) {
                String dsPrefix = SpringBootDataSourceUtil.Config_DS_Prefix + "." + dsId;
                dsMap.put(dsId, SpringBootDataSourceUtil.createSpringBootDataSource(dsPrefix, dsId, environment));//create datasource instance
            }
            return dsMap;
        } catch (Throwable e) {//failed then close all created dataSource
            for (SpringDataSource ds : dsMap.values())
                ds.close();
            throw new SpringBootDataSourceException("multi-DataSource created failed", e);
        }
    }

    /**
     * 4: assembly datasource to springBoot
     *
     * @param dsMap datasource list
     */
    private void registerDataSources(Map<String, SpringDataSource> dsMap, Properties combineProperties, BeanDefinitionRegistry registry) {
        String combineDsId = combineProperties.getProperty(SpringBootDataSourceUtil.Config_DS_CombineId);
        String primaryDsId = combineProperties.getProperty(SpringBootDataSourceUtil.Config_DS_Combine_PrimaryDs);

        for (SpringDataSource ds : dsMap.values())
            registerDataSourceBean(ds, registry);

        //assembly combine DataSource
        if (!isBlank(combineDsId) && !isBlank(primaryDsId)) {
            ThreadLocal<SpringDataSource> dsThreadLocal = new ThreadLocal<>();

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(CombineDataSource.class);
            define.setInstanceSupplier(SpringBootDataSourceUtil.createSpringSupplier(new CombineDataSource(dsThreadLocal)));
            registry.registerBeanDefinition(combineDsId, define);
            log.info("Registered Combine-DataSource({})with id:{}", define.getBeanClassName(), combineDsId);

            String dsIdSetterId = CombineAspect.class.getName();
            GenericBeanDefinition dsIdSetDefine = new GenericBeanDefinition();
            dsIdSetDefine.setBeanClass(CombineAspect.class);
            dsIdSetDefine.setInstanceSupplier(SpringBootDataSourceUtil.createSpringSupplier(new CombineAspect(primaryDsId, dsThreadLocal)));
            registry.registerBeanDefinition(dsIdSetterId, dsIdSetDefine);
            log.info("Registered DsId-setter({})with id:{}", dsIdSetDefine.getBeanClassName(), dsIdSetterId);
        }
    }

    //4.1:assembly dataSource to Spring bean container
    private void registerDataSourceBean(SpringDataSource springDs, BeanDefinitionRegistry registry) {
        GenericBeanDefinition define = new GenericBeanDefinition();
        define.setPrimary(springDs.isPrimary());
        define.setBeanClass(springDs.getClass());
        define.setInstanceSupplier(SpringBootDataSourceUtil.createSpringSupplier(springDs));
        registry.registerBeanDefinition(springDs.getDsId(), define);
        log.info("Registered DataSource({})with id:{}", define.getBeanClassName(), springDs.getDsId());
        SpringBootDataSourceManager.getInstance().addSpringBootDataSource(springDs);
    }
}

