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

import org.springframework.core.env.Environment;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.springboot.factory.SpringBeeObjectSourceFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.stone.beeop.pool.ObjectPoolStatics.POOL_CLOSED;
import static org.stone.beeop.pool.ObjectPoolStatics.POOL_CLOSING;
import static org.stone.springboot.Constants.Config_OS_Primary;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * A manager to maintain a set of bee object sources registered in spring container.
 *
 * @author Chris Liao
 */
public class ObjectSourceBeanManager<K, V> extends SpringConfigurationLoader {
    private static final ObjectSourceBeanManager single = new ObjectSourceBeanManager();
    private final Map<String, ObjectSourceBean<K, V>> osMap;

    private ObjectSourceBeanManager() {
        this.osMap = new ConcurrentHashMap<>(1);
    }

    public static ObjectSourceBeanManager getInstance() {
        return single;
    }

    public void addObjectSource(ObjectSourceBean<K, V> os) {
        osMap.put(os.getOsId(), os);
    }

    public ObjectSourceBean<K, V> getObjectSource(String osId) {
        return osMap.get(osId);
    }

    public void clearOsPool(String osId, boolean forceRecycleBorrowed) throws Exception {
        ObjectSourceBean<K, V> os = osMap.get(osId);
        if (os != null) os.restart(forceRecycleBorrowed);
    }

    public void clearOsPool(String osId, boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        ObjectSourceBean<K, V> os = osMap.get(osId);
        if (os != null) os.restart(forceRecycleBorrowed, config);
    }

    public Collection<BeeObjectPoolMonitorVo> getOsPoolMonitorVoList() throws Exception {
        List<BeeObjectPoolMonitorVo> poolMonitorVoList = new ArrayList<>(osMap.size());
        Iterator<ObjectSourceBean<K, V>> iterator = osMap.values().iterator();

        while (iterator.hasNext()) {
            ObjectSourceBean<K, V> os = iterator.next();
            BeeObjectPoolMonitorVo vo = os.getPoolMonitorVo();

            if (vo == null) continue;
            int poolState = vo.getPoolState();
            if (poolState == POOL_CLOSING || poolState == POOL_CLOSED) {//POOL_CLEARING,POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    //***************************************************************************************************************//
    //                                     2: Create Object source Bean                                              //
    //***************************************************************************************************************//
    public ObjectSourceBean<K, V> createObjectSourceBean(String prefix, String osId, Environment environment) {
        String primaryText = getConfigValue(prefix, Config_OS_Primary, environment);
        boolean isPrimary = isNotBlank(primaryText) && Boolean.valueOf(primaryText).booleanValue();
        BeeObjectSource<K, V> objectSource = new SpringBeeObjectSourceFactory().createDataSource(prefix, osId, environment);
        return new ObjectSourceBean<>(osId, isPrimary, objectSource);
    }
}
