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
package org.stone.springboot.controller;

import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.MonitoringConfigManager;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * Register controller to spring boot
 *
 * @author Chris Liao
 */
public class ControllerRegister implements ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private MonitoringConfigManager monitorConfig;
    private DataSourceBeanManager monitorManager;

    public void setMonitorConfig(MonitoringConfigManager monitorConfig) {
        this.monitorConfig = monitorConfig;
    }

    public final void setMonitorManager(DataSourceBeanManager monitorManager) {
        this.monitorManager = monitorManager;
    }

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //2:assembly Controller
        String resetControllerRegName = MonitorController.class.getName();
        if (!DataSourceBeanManager.getInstance().existsBeanDefinition(resetControllerRegName, registry)) {
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(MonitorController.class);
            define.setPrimary(true);
            define.setInstanceSupplier(DataSourceBeanManager.getInstance().createSpringSupplier(
                    new MonitorController(monitorConfig, monitorManager)));
            registry.registerBeanDefinition(resetControllerRegName, define);
            log.info("Register DataSource-restController({}) with id:{}", define.getBeanClassName(), resetControllerRegName);
        } else {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerRegName);
        }

        //3: assembly controller controller filter
        String resetControllerFilterRegName = SecurityFilter.class.getName();
        if (isBlank(monitorConfig.getConsoleUserId()) && !DataSourceBeanManager.getInstance().existsBeanDefinition(resetControllerFilterRegName, registry)) {
            SecurityFilter dsFilter = new SecurityFilter(monitorConfig.getConsoleUserId(), monitorConfig.getLoggedInSuccessTagName());
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
            registration.setName(resetControllerFilterRegName);
            registration.addUrlPatterns("/stone/*");

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(FilterRegistrationBean.class);
            define.setPrimary(true);
            define.setInstanceSupplier(DataSourceBeanManager.getInstance().createSpringSupplier(registration));
            registry.registerBeanDefinition(resetControllerFilterRegName, define);
            log.info("Register stone-login-filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);
        } else {
            log.error("BeanDefinition id {} has been exists in spring context", resetControllerFilterRegName);
        }
    }
}
