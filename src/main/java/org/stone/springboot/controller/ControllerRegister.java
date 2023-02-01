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
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.datasource.SpringBootDataSourceUtil;
import org.stone.springboot.datasource.SpringBootGlobalConfig;

import javax.servlet.Filter;

import static org.stone.util.CommonUtil.isBlank;

/**
 * Register Monitor to springboot
 *
 * @author Chris Liao
 */
public class ControllerRegister implements EnvironmentAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //springboot environment
    private Environment environment;

    public final void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //1:read datasource controller
        SpringBootGlobalConfig config = SpringBootDataSourceUtil.readMonitorConfig(environment);

        //2:register controller controller
        String resetControllerRegName = ConsoleController.class.getName();
        if (!SpringBootDataSourceUtil.existsBeanDefinition(resetControllerRegName, registry)) {
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(ConsoleController.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringBootDataSourceUtil.createSpringSupplier(new ConsoleController(
                    config.getMonitorUserId(),
                    config.getMonitorPassword(),
                    config.getMonitorLoggedInTagName())));
            registry.registerBeanDefinition(resetControllerRegName, define);
            log.info("Register DataSource-restController({}) with id:{}", define.getBeanClassName(), resetControllerRegName);
        } else {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerRegName);
        }

        //3: register controller controller filter
        String resetControllerFilterRegName = LoginedCheckFilter.class.getName();
        if (isBlank(config.getMonitorUserId()) && !SpringBootDataSourceUtil.existsBeanDefinition(resetControllerFilterRegName, registry)) {
            LoginedCheckFilter dsFilter = new LoginedCheckFilter(config.getMonitorUserId(), config.getMonitorLoggedInTagName());
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
            registration.setName(resetControllerFilterRegName);
            registration.addUrlPatterns("/stone/*");

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(FilterRegistrationBean.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringBootDataSourceUtil.createSpringSupplier(registration));
            registry.registerBeanDefinition(resetControllerFilterRegName, define);
            log.info("Register stone-login-filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);
        } else {
            log.error("BeanDefinition id {} has been exists in spring context", resetControllerFilterRegName);
        }
    }
}
