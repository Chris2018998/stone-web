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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.stone.springboot.SpringBootEnvironmentUtil;
import org.stone.springboot.extension.CacheClientProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.springboot.Constants.Config_Console_Prefix;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * #1: Console configuration
 * spring.bee.console.username=admin
 * spring.bee.console.password=admin
 * spring.bee.console.logged-flag=bee-logged-success
 * <p>
 * #2: Distribution monitor center(Support pull mode and push mode)
 * spring.bee.center.url=xxxx
 * spring.bee.center.username=xxxx
 * spring.bee.center.password=xxxx
 * spring.bee.center.pushInterval=18000
 *
 * @author Chris Liao
 */
public final class MonitorConfig {
    private static final MonitorConfig single = new MonitorConfig();
    private final Logger log = LoggerFactory.getLogger(MonitorConfig.class);

    //1: spring.bee.monitor.x
    private String username = "admin";
    private String password = "admin";
    private String loggedFlag = ConsoleController.class.getName();

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
        SpringBootEnvironmentUtil.setConfigPropertiesValue(this, Config_Console_Prefix, null, environment);
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
        String serverPort = SpringBootEnvironmentUtil.getConfigValue("server", "port", environment);
        String contextPath = SpringBootEnvironmentUtil.getConfigValue("server", "servlet.context-path", environment);
        if (isBlank(serverPort)) throw new InternalError("Not configured [server.port] in application file");
        if (isBlank(contextPath))
            throw new InternalError("Not configured [server.servlet.context-path] in application file");
        return "http://" + hostIP + ":" + serverPort + "/" + contextPath;
    }
}
