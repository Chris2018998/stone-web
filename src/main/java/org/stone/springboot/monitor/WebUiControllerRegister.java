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
package org.stone.springboot.monitor;

import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.LocalScheduleService;
import org.stone.springboot.SpringConfigurationLoader;
import org.stone.springboot.extension.CacheClientProvider;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.springboot.monitor.RequestFilter.URL_Pattern;

/**
 * Controller importer
 *
 * @author Chris Liao
 */
public class WebUiControllerRegister implements ApplicationContextAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext applicationContext;

    //spring boot env
    public void setApplicationContext(ApplicationContext SpringBeanContext) throws BeansException {
        this.applicationContext = SpringBeanContext;
    }

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        //1: check bean name
        String resetControllerRegName = WebUiController.class.getName();
        String resetControllerFilterRegName = RequestFilter.class.getName();
        if (SpringConfigurationLoader.existsBeanDefinition(resetControllerRegName, registry)) {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerRegName);
            throw new BeanDefinitionStoreException("BeanDefinition id '" + resetControllerRegName + "'already exists in spring context");
        }
        if (SpringConfigurationLoader.existsBeanDefinition(resetControllerFilterRegName, registry)) {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerFilterRegName);
            throw new BeanDefinitionStoreException("BeanDefinition id '" + resetControllerFilterRegName + "'already exists in spring context");
        }

        //2: load configuration
        MonitorConfig loader = new MonitorConfig();
        loader.load(applicationContext.getEnvironment());

        //3: Register controller and filter
        GenericBeanDefinition define = new GenericBeanDefinition();
        define.setBeanClass(WebUiController.class);
        define.setPrimary(true);
        define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(new WebUiController()));
        registry.registerBeanDefinition(resetControllerRegName, define);
        log.info("Register bee monitor controller({}) with id:{}", define.getBeanClassName(), resetControllerRegName);

        RequestFilter dsFilter = new RequestFilter(loader.getJsonTool());
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
        registration.setName(resetControllerFilterRegName);
        registration.addUrlPatterns(URL_Pattern);
        define = new GenericBeanDefinition();
        define.setBeanClass(FilterRegistrationBean.class);
        define.setPrimary(true);
        define.setInstanceSupplier(SpringConfigurationLoader.createSpringSupplier(registration));
        registry.registerBeanDefinition(resetControllerFilterRegName, define);
        log.info("Register bee security filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);

        //4: schedule a task to write pool snapshots to cache
        CacheClientProvider provider = loader.getCacheClientProvider();
        if (provider != null) {
            CacheTask task = new CacheTask(
                    loader.getCacheKeyPrefix(),
                    new PoolsSnapshot(loader.getHostWebUrl()),
                    loader.getJsonTool(),
                    provider);

            LocalScheduleService.getInstance().scheduleAtFixedRate(task, 0L, loader.getCacheInterval(), MILLISECONDS);
        }
    }
}
