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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.stone.springboot.LocalScheduleService;
import org.stone.springboot.extension.CacheClientProvider;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.tools.CommonUtil.isBlank;

/**
 * #1: configuration for UI
 * spring.bee.ui.username=admin
 * spring.bee.ui.password=admin
 * spring.bee.ui.logged-flag=bee-logged-success
 * #2: configuration for cache
 * spring.bee.cache.key-prefix=spring-bee-
 * spring.bee.cache.write-period=18000
 * spring.bee.cache.client-factory=org.stone.springboot.extension.redisson.RedissonClientFactory
 * spring.bee.json-tool=org.stone.springboot.extension.JackSonImpl
 * <p>
 * #3: comments out
 * #spring.bee.redis-host=192.168.1.1
 * #spring.bee.redis-port=6379
 * #spring.bee.redis-password=redis
 * #spring.bee.redis-send-period=18000
 * #spring.bee.redis-read-period=1800
 *
 * @author Chris Liao
 */
public class WebUiControllerRegister implements ApplicationContextAware, ImportBeanDefinitionRegistrar {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext applicationContext;

    //spring boot env
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    //Register controller bean to ioc
    public final void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        MonitorConfig loader = new MonitorConfig();
        loader.load(applicationContext.getEnvironment());

        //2: register Controller
        String resetControllerRegName = WebUiController.class.getName();
        if (!loader.existsBeanDefinition(resetControllerRegName, registry)) {
            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(WebUiController.class);
            define.setPrimary(true);
            define.setInstanceSupplier(loader.createSpringSupplier(
                    new WebUiController(loader.getUsername(), loader.getPassword(), loader.getLoggedFlag())));
            registry.registerBeanDefinition(resetControllerRegName, define);
            log.info("Register stone monitor controller({}) with id:{}", define.getBeanClassName(), resetControllerRegName);
        } else {
            log.error("BeanDefinition id {} already exists in spring context", resetControllerRegName);
        }

        //3: register filter
        String resetControllerFilterRegName = RequestFilter.class.getName();
        if (isBlank(loader.getUsername()) && !loader.existsBeanDefinition(resetControllerFilterRegName, registry)) {
            RequestFilter dsFilter = new RequestFilter(loader.getUsername(), loader.getLoggedFlag(), loader.getJsonTool());
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(dsFilter);
            registration.setName(resetControllerFilterRegName);
            registration.addUrlPatterns("/stone/*");

            GenericBeanDefinition define = new GenericBeanDefinition();
            define.setBeanClass(FilterRegistrationBean.class);
            define.setPrimary(true);
            define.setInstanceSupplier(loader.createSpringSupplier(registration));
            registry.registerBeanDefinition(resetControllerFilterRegName, define);
            log.info("Register stone security filter({}) with id:{}", define.getBeanClassName(), resetControllerFilterRegName);
        } else {
            log.error("BeanDefinition id {} has been exists in spring context", resetControllerFilterRegName);
        }

        //4: run a task
        CacheClientProvider provider = loader.getCacheClientProvider();
        if (provider != null) {
            PoolsSnapshot poolsSnapshot = new PoolsSnapshot(loader.getHostWebUrl());
            CacheTask task = new CacheTask(
                    loader.getCacheKeyPrefix(),
                    poolsSnapshot,
                    loader.getJsonTool(),
                    provider);

            LocalScheduleService.getInstance().scheduleAtFixedRate(task, 0L, loader.getCacheInterval(), MILLISECONDS);
        }
    }
}
