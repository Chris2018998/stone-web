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
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.stone.springboot.SpringConfigurationLoader;
import org.stone.springboot.extension.CacheClientProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.springboot.Constants.Config_Monitor_Prefix;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * #1: configuration for UI
 * spring.bee.monitor.username=admin
 * spring.bee.monitor.password=admin
 * spring.bee.monitor.logged-flag=bee-logged-success
 * <p>
 * #2: configuration for cache
 * spring.bee.monitor.cacheInterval=18000
 * spring.bee.monitor.cacheKeyPrefix=spring-bee-
 * spring.bee.monitor.cacheClientProvider=org.stone.springboot.extension.redisson.RedissonClientProvider2
 * spring.bee.monitor.jsonTool=org.stone.springboot.extension.JackSonImpl
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
public final class MonitorConfig extends SpringConfigurationLoader {
    private static final MonitorConfig single = new MonitorConfig();
    private final Logger log = LoggerFactory.getLogger(MonitorConfig.class);

    //1: spring.bee.monitor.x
    private String username = "admin";
    private String password = "admin";
    private String loggedFlag = MonitorController.class.getName();

    //2: spring.bee.monitor.x
    private CacheClientProvider cacheClientProvider;
    private String cacheKeyPrefix = "spring:bee:monitor:";
    private long cacheInterval = SECONDS.toMillis(10L);

    //your web app url
    private String hostWebUrl;
    private ApplicationContext springContext;

    public static MonitorConfig getInstance() {
        return single;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoggedFlag() {
        return loggedFlag;
    }

    public void setLoggedFlag(String loggedFlag) {
        if (isNotBlank(loggedFlag)) this.loggedFlag = loggedFlag;
    }


    public long getCacheInterval() {
        return cacheInterval;
    }

    public void setCacheInterval(long cacheInterval) {
        if (cacheInterval > 1000L) this.cacheInterval = cacheInterval;
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        if (isNotBlank(cacheKeyPrefix)) this.cacheKeyPrefix = cacheKeyPrefix;
    }

    public CacheClientProvider getCacheClientProvider() {
        return cacheClientProvider;
    }

    public void setCacheClientProvider(CacheClientProvider cacheClientProvider) {
        this.cacheClientProvider = cacheClientProvider;
    }

    public String getHostWebUrl() {
        return hostWebUrl;
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }

    public void setSpringContext(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    //***************************************************************************************************************//
    //                                     Monitoring info loading                                                   //
    //***************************************************************************************************************//
    public void load(Environment environment) {
        //1: load Monitoring configuration
        setConfigPropertiesValue(this, Config_Monitor_Prefix, null, environment);
        //2: create provider
        this.hostWebUrl = buildAppContextInfo(environment);
    }

    //Build app context url as cache key
    private String buildAppContextInfo(Environment environment) {
        String hostIP;
        try {
            hostIP = (InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            log.error("Failed to resolve app host IP", e);
            throw new InternalError("Failed to resolve app host IP", e);
        }

        if (isBlank(hostIP)) throw new InternalError("Failed to resolve app host IP");
        String serverPort = getConfigValue("server", "port", environment);
        String contextPath = getConfigValue("server", "servlet.context-path", environment);
        if (isBlank(serverPort)) throw new InternalError("Not configured [server.port] in application file");
        if (isBlank(contextPath))
            throw new InternalError("Not configured [server.servlet.context-path] in application file");
        return "http://" + hostIP + ":" + serverPort + "/" + contextPath;
    }
}
