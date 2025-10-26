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

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.beeop.BeeObjectPoolMonitorVo;

import java.io.Serializable;
import java.util.Collection;

/**
 * 1: push monitoring data of application to redis by timer task
 * 2: Web monitor UI provide a list to display all monitoring data in cache
 * 3: User can choose a record of list to view its runtime monitoring info
 *
 * @author Chris Liao
 */

public final class PoolSnapshot implements Serializable {
    private final String appContextUrl;
    private String cacheTime;
    private Collection<BeeConnectionPoolMonitorVo> dsPoolList;
    private Collection<BeeMethodExecutionLog> dsSqlList;
    private Collection<BeeObjectPoolMonitorVo> osPoolList;

    public PoolSnapshot(String appContextUrl) {
        this.appContextUrl = appContextUrl;
    }

    public String getAppContextUrl() {
        return appContextUrl;
    }

    public String getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(String cacheTime) {
        this.cacheTime = cacheTime;
    }

    public Collection<BeeConnectionPoolMonitorVo> getDsPoolList() {
        return dsPoolList;
    }

    public void setDsPoolList(Collection<BeeConnectionPoolMonitorVo> dsPoolList) {
        this.dsPoolList = dsPoolList;
    }

    public Collection<BeeMethodExecutionLog> getDsSqlList() {
        return dsSqlList;
    }

    public void setDsSqlList(Collection<BeeMethodExecutionLog> dsSqlList) {
        this.dsSqlList = dsSqlList;
    }

    public Collection<BeeObjectPoolMonitorVo> getOsPoolList() {
        return osPoolList;
    }

    public void setOsPoolList(Collection<BeeObjectPoolMonitorVo> osPoolList) {
        this.osPoolList = osPoolList;
    }
}
