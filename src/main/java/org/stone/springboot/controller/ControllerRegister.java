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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.SpringDsRegisterUtil;
import org.stone.springboot.SpringStoneMonitorConfig;
import org.stone.springboot.SpringStoneObjectsManager;

import javax.servlet.Filter;

import static org.stone.util.CommonUtil.isBlank;

/**
 * Register controller to springboot
 *
 * @author Chris Liao
 */
public class ControllerRegister implements ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private SpringStoneMonitorConfig monitorConfig;
    private SpringStoneObjectsManager monitorManager;

    public void setMonitorConfig(SpringStoneMonitorConfig monitorConfig) {
        this.monitorConfig = monitorConfig;
    }

    public final void setMonitorManager(SpringStoneObjectsManager monitorManager) {
        this.monitorManager = monitorManager;
    }

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        //2:assembly controller controller
        String resetControllerRegName = ConsoleController.class.getName();
        if (!SpringDsRegisterUtil.existsBeanDefinition(resetControllerRegName, registry)) {
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(ConsoleController.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringDsRegisterUtil.createSpringSupplier(
                    new ConsoleController(monitorConfig, monitorManager)));
            registry.registerBeanDefinition(resetControllerRegName, define);
            log.info("Register DataSource-restController({}) with id:{}", define.getBeanClassName(), resetControllerRegName);
        } else {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerRegName);
        }

        //3: assembly controller controller filter
        String resetControllerFilterRegName = LoginedCheckFilter.class.getName();
        if (isBlank(monitorConfig.getConsoleUserId()) && !SpringDsRegisterUtil.existsBeanDefinition(resetControllerFilterRegName, registry)) {
            LoginedCheckFilter dsFilter = new LoginedCheckFilter(monitorConfig.getConsoleUserId(), monitorConfig.getLoggedInSuccessTagName());
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
            registration.setName(resetControllerFilterRegName);
            registration.addUrlPatterns("/stone/*");

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(FilterRegistrationBean.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringDsRegisterUtil.createSpringSupplier(registration));
            registry.registerBeanDefinition(resetControllerFilterRegName, define);
            log.info("Register stone-login-filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);
        } else {
            log.error("BeanDefinition id {} has been exists in spring context", resetControllerFilterRegName);
        }
    }
}
