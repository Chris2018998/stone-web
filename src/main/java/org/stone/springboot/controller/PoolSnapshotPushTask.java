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
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.ObjectSourceBeanManager;
import org.stone.springboot.extension.CacheClient;
import org.stone.springboot.extension.CacheClientProvider;
import org.stone.springboot.extension.JackSonUtil;

/**
 * Timer task to write monitoring data to cache
 *
 * @author Chris Liao
 */
public final class PoolSnapshotPushTask implements Runnable {
    //prefix of cache key
    private final String keyPrefix;
    //monitoring data
    private final PoolSnapshot snapshot;
    //cache client
    private final CacheClientProvider provider;

    private final DataSourceBeanManager dsManager;

    private final ObjectSourceBeanManager osManager;

    private final Logger logger = LoggerFactory.getLogger(PoolSnapshotPushTask.class);

    public PoolSnapshotPushTask(String keyPrefix, PoolSnapshot snapshot, CacheClientProvider provider) {

        this.keyPrefix = keyPrefix;
        this.snapshot = snapshot;
        this.provider = provider;

        this.dsManager = DataSourceBeanManager.getInstance();
        this.osManager = ObjectSourceBeanManager.getInstance();
    }

    public void run() {
        try {
//            snapshot.setDsPoolList(dsManager.getAllDsPoolMonitorVos());
//            snapshot.setDsSqlList(dsManager.getSqlExecutionLogList());
//            snapshot.setOsPoolList(osManager.getOsPoolMonitorVoList());
//            snapshot.setCacheTime(StatementJdbcUtil.formatDate(new Date()));

            CacheClient client = provider.get();
            if (client != null) {
                client.set(keyPrefix + "-" + snapshot.getAppContextUrl(), JackSonUtil.object2String(snapshot));
            } else {
                logger.warn("Redis client is null");
            }
        } catch (Exception e) {
            logger.error("Failed to write monitoring data to cache", e);
        }
    }
}
