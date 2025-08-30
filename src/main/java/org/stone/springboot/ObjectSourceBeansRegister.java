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
import org.stone.springboot.annotation.EnableBeeOs;
import org.stone.springboot.controller.MonitorControllerRegister;
import org.stone.springboot.dynamic.DynamicAspect;
import org.stone.springboot.dynamic.DynamicObjectSource;
import org.stone.springboot.exception.ConfigurationException;
import org.stone.springboot.exception.ObjectSourceException;

import java.util.*;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * #ids of objectSource
 * spring.objectSource.osId=os1,os2
 *
 * #os1
 * spring.objectSource.os1.fairMode=true
 * spring.objectSource.os1.initialSize=10
 * spring.objectSource.os1.maxActive=10
 * ......
 *
 * #os2
 * spring.objectSource.os2.fairMode=false
 * spring.objectSource.os2.initialSize=20
 * spring.objectSource.os2.maxActive=50
 * ......
 *
 * @author Chris Liao
 */
public class ObjectSourceBeansRegister<K, V> implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ObjectSourceBeanManager<K, V> osBeanManager;
    private Environment environment;

    public ObjectSourceBeansRegister() {
        this.osBeanManager = ObjectSourceBeanManager.getInstance();
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
        //1: read configured id list of source
        List<String> osIdList = getOsIdList(environment, registry);

        //2: load configuration for dynamic object source
        Properties dynamicSourceProperties = getDynamicOsInfo(osIdList, environment, registry);

        //3: create object sources with configuration
        Map<String, ObjectSourceBean<K, V>> osMap = createObjectSourceBean(osIdList, environment);

        //4: register object sources to spring container
        this.registerObjectSourceBean(osMap, dynamicSourceProperties, registry);

        //5: register ObjectSource to spring container
        this.registerObjectSourceBean(osMap, dynamicSourceProperties, registry);

        //6: attempt to register monitor
        Map<String, Object> attributes = classMetadata.getAnnotationAttributes(EnableBeeOs.class.getName(), false);
        if ((boolean) attributes.get(Annotation_Monitor_Attribute_Name)) {
            new MonitorControllerRegister().registerBeanDefinitions(classMetadata, registry, environment);
        }
    }

    /**
     * 1: get object source config id list
     *
     * @param environment spring boot environment
     * @param registry    spring boot registry
     * @return ObjectSource name list
     */
    private List<String> getOsIdList(Environment environment, BeanDefinitionRegistry registry) {
        String osIdsText = osBeanManager.getConfigValue(Config_OS_Prefix, Config_OS_Id, environment);
        if (isBlank(osIdsText))
            throw new ConfigurationException("Missed or not found config item '" + Config_OS_Prefix + "." + Config_OS_Id + "'");

        String[] osIds = osIdsText.trim().split(",");
        List<String> osIdList = new ArrayList<>(osIds.length);
        for (String id : osIds) {
            if (isBlank(id) || osIdList.contains(id)) continue;
            id = id.trim();
            if (SpringConfigurationLoader.existsBeanDefinition(id, registry))
                throw new ConfigurationException("Existed a registered bean with id '" + id + "'");

            //ObjectSource id(" + id + ")has been registered by another bean

            osIdList.add(id);
        }
        if (osIdList.isEmpty())
            throw new ConfigurationException("Missed or not found config item '" + Config_OS_Prefix + "." + Config_OS_Id + "'");

        return osIdList;
    }

    /**
     * 2: get dynamic config info
     *
     * @param osIdList    object source name list
     * @param environment spring boot environment
     * @return ObjectSource name list
     */
    private Properties getDynamicOsInfo(List<String> osIdList, Environment environment, BeanDefinitionRegistry registry) {
        String dynId = osBeanManager.getConfigValue(Config_OS_Prefix, Config_Dyn_OS_Id, environment);
        String primaryDs = osBeanManager.getConfigValue(Config_OS_Prefix, Config_Dyn_OS_PrimaryId, environment);

        dynId = (dynId == null) ? "" : dynId;
        primaryDs = (primaryDs == null) ? "" : primaryDs;

        if (isNotBlank(dynId)) {
            if (osIdList.contains(dynId))
                throw new ConfigurationException("Dynamic object source id '" + dynId + "' can't be in os-id list");
            if (SpringConfigurationLoader.existsBeanDefinition(dynId, registry))
                throw new ConfigurationException("Dynamic object source id '" + dynId + "' has been registered by another bean");

            if (isBlank(primaryDs))
                throw new ConfigurationException("Missed or not found config item '" + Config_OS_Prefix + "." + Config_Dyn_OS_PrimaryId + "'");
            if (!osIdList.contains(primaryDs.trim()))
                throw new ConfigurationException("Dynamic primaryOs '" + primaryDs + "' not found in os-id list");
        }

        Properties dynProperties = new Properties();
        dynProperties.put(Config_Dyn_OS_Id, dynId);
        dynProperties.put(Config_Dyn_OS_PrimaryId, primaryDs);
        return dynProperties;
    }

    /**
     * 3: create object source by config
     *
     * @param osIdList    ObjectSource name list
     * @param environment spring boot environment
     * @return ObjectSource holder map
     */
    private Map<String, ObjectSourceBean<K, V>> createObjectSourceBean(List<String> osIdList, Environment environment) {
        Map<String, ObjectSourceBean<K, V>> osMap = new LinkedHashMap<>(osIdList.size());
        try {
            for (String osId : osIdList) {
                String osPrefix = Config_OS_Prefix + "." + osId;
                osMap.put(osId, osBeanManager.createObjectSourceBean(osPrefix, osId, environment));
            }
            return osMap;
        } catch (Throwable e) {//failed then close all created object source
            for (ObjectSourceBean<K, V> ds : osMap.values()) {
                try {
                    ds.close();
                } catch (Exception ee) {
                    //do nothing
                }
            }
            throw new ObjectSourceException("multi object source created failed", e);
        }
    }

    /**
     * 4: register ObjectSource to springBoot
     *
     * @param osMap ObjectSource list
     */
    private void registerObjectSourceBean(Map<String, ObjectSourceBean<K, V>> osMap, Properties dynProperties, BeanDefinitionRegistry registry) {
        String dynOsId = dynProperties.getProperty(Config_Dyn_OS_Id);
        String primaryOsId = dynProperties.getProperty(Config_Dyn_OS_PrimaryId);

        for (ObjectSourceBean<K, V> os : osMap.values())
            registerObjectSourceBean(os, registry);

        //register dynamic ObjectSource
        if (isNotBlank(dynOsId) && isNotBlank(primaryOsId)) {
            ThreadLocal<ObjectSourceBean<K, V>> osThreadLocal = new ThreadLocal<>();

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(DynamicObjectSource.class);
            define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(new DynamicObjectSource<>(dynOsId, osThreadLocal)));
            registry.registerBeanDefinition(dynOsId, define);
            log.info("Registered a dynamic object source(type:{})with bean Id '{}'", define.getBeanClassName(), dynOsId);

            String aspectBeanId = "beeOs_" + DynamicAspect.class.getName();
            GenericBeanDefinition osIdSetDefine = new GenericBeanDefinition();
            osIdSetDefine.setBeanClass(DynamicAspect.class);

            DynamicAspect<K, V> dynamicAspect = new DynamicAspect<>();
            dynamicAspect.setDynOsThreadLocal(primaryOsId, osThreadLocal);
            osIdSetDefine.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(dynamicAspect));
            registry.registerBeanDefinition(aspectBeanId, osIdSetDefine);
            log.info("Registered an aspect component(type:{})for dynamic object source with bean Id '{}'", define.getBeanClassName(), aspectBeanId);
        }
    }

    //4.1:register ObjectSource to Spring bean container
    private void registerObjectSourceBean(ObjectSourceBean<K, V> springOs, BeanDefinitionRegistry registry) {
        GenericBeanDefinition define = new GenericBeanDefinition();
        define.setPrimary(springOs.isPrimary());
        define.setBeanClass(springOs.getClass());
        define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(springOs));
        registry.registerBeanDefinition(springOs.getOsId(), define);
        log.info("Registered a object source(type:{})with bean Id '{}'", define.getBeanClassName(), springOs.getOsId());
        osBeanManager.addObjectSource(springOs);
    }
}
