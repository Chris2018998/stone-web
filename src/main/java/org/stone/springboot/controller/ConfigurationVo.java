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
import org.springframework.core.env.Environment;
import org.stone.springboot.SpringConfigurationLoader;
import org.stone.springboot.datacache.CacheClient;
import org.stone.springboot.extension.LocalJsonUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.stone.springboot.Constants.Config_Bee_Prefix;
import static org.stone.tools.CommonUtil.isBlank;

/**
 * #1: configuration for UI
 * spring.bee.ui.username=admin
 * spring.bee.ui.password=admin
 * spring.bee.ui.logged-flag=bee-logged-success
 * <p>
 * #2: configuration for cache
 * spring.bee.cache.key-prefix=spring-bee-
 * spring.bee.cache.write-period=18000
 * spring.bee.cache.client-factory=org.stone.springboot.datacache.redisson.RedissonClientFactory
 * spring.bee.json.tool=org.stone.springboot.extension.JackSonImpl
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
public final class ConfigurationVo extends SpringConfigurationLoader {
    private static final ConfigurationVo single = new ConfigurationVo();
    private final Logger log = LoggerFactory.getLogger(ConfigurationVo.class);

    //1: user/password for web ui login
    private String userId = "admin";
    private String password = "admin";
    private String loggedFlag = MonitorController.class.getName();

    //2: cache monitoring data
    private long writePeriod;
    private String cacheKeyPrefix;
    private CacheClient cacheClientFactory;

    //3:Json Util
    private LocalJsonUtil jsonTool;
    private String AppWebContext;

    public static ConfigurationVo getInstance() {
        return single;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
        if (!isBlank(loggedFlag)) this.loggedFlag = loggedFlag;
    }

    public long getWritePeriod() {
        return writePeriod;
    }

    public void setWritePeriod(long writePeriod) {
        this.writePeriod = writePeriod;
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        this.cacheKeyPrefix = cacheKeyPrefix;
    }

    public CacheClient getCacheClientFactory() {
        return cacheClientFactory;
    }

    public void setCacheClientFactory(CacheClient cacheClientFactory) {
        this.cacheClientFactory = cacheClientFactory;
    }

    public LocalJsonUtil getJsonTool() {
        return jsonTool;
    }

    public void setJsonTool(LocalJsonUtil jsonTool) {
        this.jsonTool = jsonTool;
    }

    public String getAppWebContext() {
        return AppWebContext;
    }

    //***************************************************************************************************************//
    //                                     Monitoring info loading                                                   //
    //***************************************************************************************************************//
    public void load(Environment environment) {
        //1: load Monitoring configuration
        setConfigPropertiesValue(this, Config_Bee_Prefix + ".ui", null, environment);
        setConfigPropertiesValue(this, Config_Bee_Prefix + ".cache", null, environment);
        setConfigPropertiesValue(this, Config_Bee_Prefix + ".json", null, environment);

        //2: create provider
        this.AppWebContext = buildAppContextInfo(environment);
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
