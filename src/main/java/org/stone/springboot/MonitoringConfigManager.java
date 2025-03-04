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
import org.springframework.core.env.Environment;
import org.stone.springboot.controller.MonitorController;
import org.stone.springboot.datacache.CacheClient;
import org.stone.springboot.datacache.CacheClientProvider;
import org.stone.springboot.datacache.MonitoringVo;
import org.stone.springboot.datacache.MonitoringVoTimerTask;
import org.stone.springboot.extension.JackSonImpl;
import org.stone.springboot.extension.LocalJsonUtil;
import org.stone.springboot.jdbc.StatementExecutionCollector;
import org.stone.springboot.jdbc.StatementSqlAlert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.springboot.Constants.Config_DS_Prefix;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * configuration of spring monitoring
 *
 * spring.datasource.consoleUserId=admin
 * spring.datasource.consolePassword=admin
 *
 * spring.datasource.sql-trace=true
 * spring.datasource.sql-show=true
 * spring.datasource.sql-queue-size=100
 * spring.datasource.sql-slow-time=5000
 * spring.datasource.sql-timeout-in-queue=60000
 * spring.datasource.sql-slow-action=xxxxx
 * spring.datasource.sql-queue-scan-period=18000
 *
 * spring.datasource.redis-host=192.168.1.1
 * spring.datasource.redis-port=6379
 * spring.datasource.redis-password=redis
 * spring.datasource.redis-send-period=18000
 * spring.datasource.redis-read-period=18000
 *
 * spring.datasource.jsonToolClassName=JackSonTool
 *
 * @author Chris Liao
 */
public final class MonitoringConfigManager extends SpringConfigurationLoader {
    private static final MonitoringConfigManager single = new MonitoringConfigManager();
    private final Logger log = LoggerFactory.getLogger(MonitoringConfigManager.class);

    //***************************************************************************************************************//
    //                                             2: User/Password for web login                                    //
    //***************************************************************************************************************//
    private String consoleUserId = "admin";
    private String consolePassword = "admin";
    private String loggedInSuccessTagName = MonitorController.class.getName();
    //***************************************************************************************************************//
    //                                             3: Control on sql trace                                           //
    //***************************************************************************************************************//
    private boolean sqlTrace = true;
    private boolean sqlShow = true;
    private int sqlQueueSize = 100;
    private long sqlSlowTime = TimeUnit.SECONDS.toMillis(6);
    private long sqlTimeoutInQueue = TimeUnit.MINUTES.toMillis(3);
    private long sqlQueueScanPeriod = TimeUnit.MINUTES.toMillis(3);
    private StatementSqlAlert sqlSlowAction;
    //***************************************************************************************************************//
    //                                             5: other config                                                   //
    //***************************************************************************************************************//
    private LocalJsonUtil jsonUtil;
    private String jsonUtilClassName;

    private String cacheKeyPrefix;
    private CacheClient cacheClient;
    private String cacheClientProviderClassName;

    public static MonitoringConfigManager getInstance() {
        return single;
    }

    public boolean isSqlShow() {
        return sqlShow;
    }

    public void setSqlShow(boolean sqlShow) {
        this.sqlShow = sqlShow;
    }

    public boolean isSqlTrace() {
        return sqlTrace;
    }

    public void setSqlTrace(boolean sqlTrace) {
        this.sqlTrace = sqlTrace;
    }

    public int getSqlQueueSize() {
        return sqlQueueSize;
    }

    public void setSqlQueueSize(int sqlQueueSize) {
        this.sqlQueueSize = sqlQueueSize;
    }

    public long getSqlSlowTime() {
        return sqlSlowTime;
    }

    public void setSqlSlowTime(long sqlSlowTime) {
        this.sqlSlowTime = sqlSlowTime;
    }

    public long getSqlTimeoutInQueue() {
        return sqlTimeoutInQueue;
    }

    public void setSqlTimeoutInQueue(long sqlTimeoutInQueue) {
        this.sqlTimeoutInQueue = sqlTimeoutInQueue;
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        this.cacheKeyPrefix = cacheKeyPrefix;
    }

    public CacheClient getCacheClient() {
        return cacheClient;
    }

    public String getCacheClientProviderClassName() {
        return cacheClientProviderClassName;
    }

    public void setCacheClientProviderClassName(String cacheClientProviderClassName) {
        this.cacheClientProviderClassName = cacheClientProviderClassName;
    }

    public long getSqlQueueScanPeriod() {
        return sqlQueueScanPeriod;
    }

    public void setSqlQueueScanPeriod(long sqlQueueScanPeriod) {
        this.sqlQueueScanPeriod = sqlQueueScanPeriod;
    }

    public StatementSqlAlert getSqlSlowAction() {
        return sqlSlowAction;
    }

    public void setSqlSlowAction(StatementSqlAlert sqlSlowAction) {
        this.sqlSlowAction = sqlSlowAction;
    }

    public String getConsoleUserId() {
        return consoleUserId;
    }

    public void setConsoleUserId(String consoleUserId) {
        this.consoleUserId = consoleUserId;
    }

    public String getConsolePassword() {
        return consolePassword;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public String getLoggedInSuccessTagName() {
        return loggedInSuccessTagName;
    }

    public void setLoggedInSuccessTagName(String loggedInSuccessTagName) {
        if (!isBlank(loggedInSuccessTagName))
            this.loggedInSuccessTagName = loggedInSuccessTagName;
    }

    public String getJsonUtilClassName() {
        return jsonUtilClassName;
    }

    public void setJsonUtilClassName(String jsonUtilClassName) {
        this.jsonUtilClassName = jsonUtilClassName;
    }

    public LocalJsonUtil getJsonUtil() {
        return jsonUtil;
    }

    public void setJsonUtil(LocalJsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    //***************************************************************************************************************//
    //                                     Monitoring info loading                                                   //
    //***************************************************************************************************************//
    public void loadMonitorConfig(Environment environment) {
        //1: load Monitoring configuration
        setConfigPropertiesValue(this, Config_DS_Prefix, null, environment);

        //3: create Json Util
        createJsonTool(this.jsonUtilClassName);

        //4: create provider
        createCacheClientProvider(this.cacheClientProviderClassName, environment);

        //5: set SqlExecutionCache to data source manager
        if (sqlTrace) {
            DataSourceBeanManager.getInstance()
                    .setStatementExecutionCollector(new StatementExecutionCollector(this));
        }
    }

    //create json tool implementation
    private void createJsonTool(String jsonClassName) {
        if (!isNotBlank(jsonClassName)) {
            try {
                Class<?> jsonToolClass = Class.forName(jsonClassName);
                this.jsonUtil = (LocalJsonUtil) jsonToolClass.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("Failed to create local json util with class:{}", jsonClassName);
            }
        }
        if (jsonUtil == null) jsonUtil = new JackSonImpl();
    }

    private void createCacheClientProvider(String providerClassName, Environment environment) {
        if (!isNotBlank(providerClassName)) {
            try {
                Class<?> providerToolClass = Class.forName(providerClassName);
                CacheClientProvider cacheClientProvider = (CacheClientProvider) providerToolClass.getDeclaredConstructor().newInstance();
                this.cacheClient = cacheClientProvider.create(environment);

                //2: build application context base url
                String appContextBaseUrl = buildAppContextInfo(environment);

                //3: schedule task
                InternalScheduledService.getInstance().
                        scheduleAtFixedRate(new MonitoringVoTimerTask(cacheClient, jsonUtil, this.cacheKeyPrefix, new MonitoringVo(appContextBaseUrl)), 0, 15000L, MILLISECONDS);
            } catch (Throwable e) {
                log.warn("Failed to create cache client provider with class:{}", providerClassName);
            }
        }
    }

    //Build app context url
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
