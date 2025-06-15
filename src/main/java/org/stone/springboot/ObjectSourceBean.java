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
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.PoolNotCreatedException;

/**
 * A wrapper around Object source(only support BeeObjectSource)
 *
 * @author Chris Liao
 */
public class ObjectSourceBean<K, V> extends BeeObjectSource<K, V> {
    private final String osId;
    private final boolean primary;
    private final BeeObjectSource<K, V> os;
    private final ObjectSourcePoolMonitorVo voWrapper;
    private final Logger log = LoggerFactory.getLogger(ObjectSourceBean.class);

    public ObjectSourceBean(String osId, boolean primary, BeeObjectSource<K, V> os) {
        if (os == null) throw new IllegalArgumentException("Object source can't be null");
        this.osId = osId;
        this.os = os;
        this.primary = primary;
        this.voWrapper = new ObjectSourcePoolMonitorVo(osId);
    }

    //***************************************************************************************************************//
    //                                     1: base operation methods(2)                                              //
    //***************************************************************************************************************//
    public String getOsId() {
        return osId;
    }

    public boolean isPrimary() {
        return primary;
    }

    //***************************************************************************************************************//
    //                                     2: Pool Monitor (5)                                                       //
    //***************************************************************************************************************//
    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        return os.getObjectHandle();
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        return os.getObjectHandle(key);
    }

    //***************************************************************************************************************//
    //                                      3: clear and monitoring(3)                                               //
    //***************************************************************************************************************//
    public void clear(boolean forceRecycleBorrowed) throws Exception {
        os.clear(forceRecycleBorrowed);
    }

    public void clear(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception {
        os.clear(forceRecycleBorrowed, config);
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        try {
            voWrapper.setVo(os.getPoolMonitorVo());
        } catch (Throwable e) {
            if (!(e instanceof PoolNotCreatedException))
                log.warn("Failed to execute dataSource 'getPoolMonitorVo' method", e);
            return null;
        }

        return voWrapper;
    }

    //***************************************************************************************************************//
    //                                       4: keys maintenance(9)                                                  //
    //***************************************************************************************************************//
    public boolean exists(K key) throws Exception {
        return os.exists(key);
    }

    public void clear(K key) throws Exception {
        os.clear(key);
    }

    public void clear(K key, boolean forceRecycleBorrowed) throws Exception {
        os.clear(key, forceRecycleBorrowed);
    }

    public void deleteKey(K key) throws Exception {
        os.deleteKey(key);
    }

    public void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception {
        os.deleteKey(key, forceRecycleBorrowed);
    }

    public boolean isPrintRuntimeLog(K key) throws Exception {
        return os.isPrintRuntimeLog(key);
    }

    public void setPrintRuntimeLog(K key, boolean enable) throws Exception {
        os.setPrintRuntimeLog(key, enable);
    }

    public BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception {
        return os.getMonitorVo(key);
    }

    public Thread[] interruptObjectCreating(K key, boolean interruptTimeout) throws Exception {
        return os.interruptObjectCreating(key, interruptTimeout);
    }
}
