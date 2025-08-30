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
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.LocalScheduleService;
import org.stone.springboot.SpringConfigurationLoader;
import org.stone.springboot.extension.CacheClientProvider;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.springboot.controller.MonitorControllerRequestFilter.URL_Pattern;

/**
 * Controller importer
 *
 * @author Chris Liao
 */
public class MonitorControllerRegister {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, Environment environment) {

        //1: load configuration
        MonitorConfig loader = new MonitorConfig();
        loader.load(environment);

        //2: Register controller
        String resetControllerRegName = MonitorController.class.getName();
        if (!SpringConfigurationLoader.existsBeanDefinition(resetControllerRegName, registry)) {
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(MonitorController.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(new MonitorController()));
            registry.registerBeanDefinition(resetControllerRegName, define);
            log.info("Register bee monitor controller({}) with id:{}", define.getBeanClassName(), resetControllerRegName);
        } else {
            log.warn("Bee monitor controller has existed with id:{}", resetControllerRegName);
        }

        //3: Register filter
        String resetControllerFilterRegName = MonitorControllerRequestFilter.class.getName();
        if (!SpringConfigurationLoader.existsBeanDefinition(resetControllerFilterRegName, registry)) {
            MonitorControllerRequestFilter dsFilter = new MonitorControllerRequestFilter();
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
            registration.setName(resetControllerFilterRegName);
            registration.addUrlPatterns(URL_Pattern);
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(FilterRegistrationBean.class);
            define.setPrimary(true);
            define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(registration));
            registry.registerBeanDefinition(resetControllerFilterRegName, define);
            log.info("Register bee security filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);
        } else {
            log.warn("Bee monitor filter has existed with id:{}", resetControllerFilterRegName);
        }

        //4: schedule a task to write pool snapshots to cache
        CacheClientProvider provider = loader.getCacheClientProvider();
        LocalScheduleService scheduleService = LocalScheduleService.getInstance();
        if (provider != null && !scheduleService.isFull()) {
            PoolSnapshotPushTask task = new PoolSnapshotPushTask(
                    loader.getCacheKeyPrefix(),
                    new PoolSnapshot(loader.getHostWebUrl()),
                    provider);

            scheduleService.scheduleAtFixedRate(task, 0L, loader.getCacheInterval(), MILLISECONDS);
        }
    }
}
