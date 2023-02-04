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

import org.stone.springboot.controller.ConsoleController;
import org.stone.springboot.sqlTrace.StatementTraceAlert;

import java.util.concurrent.TimeUnit;

import static org.stone.util.CommonUtil.isBlank;

/*
 * stone springboot config
 *
 * spring.stone.consoleUserId=admin
 * spring.stone.consolePassword=admin
 *
 * spring.stone.sql-trace=true
 * spring.stone.sql-show=true
 * spring.stone.sql-trace-max-size=100
 * spring.stone.sql-exec-slow-time=5000
 * spring.stone.sql-trace-timeout=60000
 * spring.stone.sql-exec-alert-action=xxxxx
 * spring.stone.sql-trace-timeout-scan-period=18000
 *
 * spring.stone.redis-host=192.168.1.1
 * spring.stone.redis-port=6379
 * spring.stone.redis-password=redis
 * spring.stone.redis-send-period=18000
 * spring.stone.redis-read-period=18000
 *
 * spring.stone.jsonToolClassName=JackSonTool
 *
 * @author Chris Liao
 */
public final class StoneSpringbootConfig {

    //***************************************************************************************************************//
    //                                             1: console userId config                                          //
    //***************************************************************************************************************//
    private String consoleUserId = "admin";
    private String consolePassword = "admin";
    private String loggedInSuccessTagName = ConsoleController.class.getName();

    //***************************************************************************************************************//
    //                                             2: sql trace config                                               //
    //***************************************************************************************************************//
    private boolean sqlShow = true;
    private boolean sqlTrace = true;
    private int sqlTraceMaxSize = 100;
    private long sqlExecSlowTime = TimeUnit.SECONDS.toMillis(6);
    private long sqlTraceTimeout = TimeUnit.MINUTES.toMillis(3);
    private long sqlTraceTimeoutScanPeriod = TimeUnit.MINUTES.toMillis(3);
    private StatementTraceAlert sqlExecAlertAction;

    //***************************************************************************************************************//
    //                                             3: redis storage                                                  //
    //***************************************************************************************************************//
    private String redisHost;
    private int redisPort = 6379;
    private int redisTimeoutMs = 2000;
    private String redisUserId;
    private String redisPassword;
    private long redisSendPeriod = TimeUnit.MINUTES.toMillis(3);//node send
    private long redisReadPeriod = TimeUnit.MINUTES.toMillis(3);//center read

    //***************************************************************************************************************//
    //                                             4: other config                                                   //
    //***************************************************************************************************************//
    private String jsonToolClassName;


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

    public int getSqlTraceMaxSize() {
        return sqlTraceMaxSize;
    }

    public void setSqlTraceMaxSize(int sqlTraceMaxSize) {
        if (sqlTraceMaxSize > 0) this.sqlTraceMaxSize = sqlTraceMaxSize;
    }

    public long getSqlTraceTimeout() {
        return sqlTraceTimeout;
    }

    public void setSqlTraceTimeout(long sqlTraceTimeout) {
        if (sqlTraceTimeout > 0)
            this.sqlTraceTimeout = sqlTraceTimeout;
    }

    public long getSqlExecSlowTime() {
        return sqlExecSlowTime;
    }

    public void setSqlExecSlowTime(long sqlExecSlowTime) {
        if (sqlExecSlowTime > 0)
            this.sqlExecSlowTime = sqlExecSlowTime;
    }

    public long getSqlTraceTimeoutScanPeriod() {
        return sqlTraceTimeoutScanPeriod;
    }

    public void setSqlTraceTimeoutScanPeriod(long sqlTraceTimeoutScanPeriod) {
        if (sqlTraceTimeoutScanPeriod > 0)
            this.sqlTraceTimeoutScanPeriod = sqlTraceTimeoutScanPeriod;
    }

    public StatementTraceAlert getSqlExecAlertAction() {
        return sqlExecAlertAction;
    }

    public void setSqlExecAlertAction(StatementTraceAlert sqlExecAlertAction) {
        if (sqlExecAlertAction != null) this.sqlExecAlertAction = sqlExecAlertAction;
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

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(int redisPort) {
        if (redisPort > 0) this.redisPort = redisPort;
    }

    public int getRedisTimeoutMs() {
        return redisTimeoutMs;
    }

    public void setRedisTimeoutMs(int redisTimeoutMs) {
        if (redisTimeoutMs > 0) this.redisTimeoutMs = redisTimeoutMs;
    }

    public String getRedisUserId() {
        return redisUserId;
    }

    public void setRedisUserId(String redisUserId) {
        this.redisUserId = redisUserId;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }

    public long getRedisSendPeriod() {
        return redisSendPeriod;
    }

    public void setRedisSendPeriod(long redisSendPeriod) {
        if (redisSendPeriod > 0)
            this.redisSendPeriod = redisSendPeriod;
    }

    public long getRedisReadPeriod() {
        return redisReadPeriod;
    }

    public void setRedisReadPeriod(long redisReadPeriod) {
        if (redisReadPeriod > 0)
            this.redisReadPeriod = redisReadPeriod;
    }

    public String getJsonToolClassName() {
        return jsonToolClassName;
    }

    public void setJsonToolClassName(String jsonToolClassName) {
        this.jsonToolClassName = jsonToolClassName;
    }
}
