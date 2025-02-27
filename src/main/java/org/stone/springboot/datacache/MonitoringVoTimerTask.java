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
package org.stone.springboot.datacache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.ObjectSourceBeanManager;
import org.stone.springboot.extension.LocalJsonUtil;
import org.stone.springboot.sql.SqlExecutionJdbcUtil;

import java.util.Date;

/**
 * Timer task to write monitoring data to cache
 *
 * @author Chris Liao
 */
public final class MonitoringVoTimerTask implements Runnable {
    //cache client
    private final CacheClient client;
    //cache client
    private final LocalJsonUtil jsonUtil;
    //monitoring data
    private final MonitoringVo monitoringData;

    private final Logger logger = LoggerFactory.getLogger(MonitoringVoTimerTask.class);

    public MonitoringVoTimerTask(CacheClient client,
                                 LocalJsonUtil jsonUtil,
                                 MonitoringVo monitoringData) {
        this.client = client;
        this.jsonUtil = jsonUtil;
        this.monitoringData = monitoringData;
    }

    public void run() {
        try {
            monitoringData.setDsList(DataSourceBeanManager.getInstance().getDataSourceMonitoringVoList());
            monitoringData.setSqlList(DataSourceBeanManager.getInstance().getSqlExecutionList());
            monitoringData.setOsList(ObjectSourceBeanManager.getInstance().getOsPoolMonitorVoList());
            monitoringData.setCacheTime(SqlExecutionJdbcUtil.formatDate(new Date()));

            client.set(monitoringData.getAppContextUrl(), jsonUtil.object2String(monitoringData));
        } catch (Exception e) {
            logger.error("Failed to write monitoring data to cache", e);
        }
    }
}
