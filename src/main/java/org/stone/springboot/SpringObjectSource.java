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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.pool.ObjectPoolMonitorVo;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.stone.springboot.SpringDsRegisterUtil.tryToCloseDataSource;

/**
 * object source(only support beeop)
 *
 * @author Chris Liao
 */
public class SpringObjectSource {
    private static final Logger Log = LoggerFactory.getLogger(SpringObjectSource.class);
    private static final Field osIdField;
    private static final Field osUUIDField;

    static {//read monitor field
        try {
            osIdField = ObjectPoolMonitorVo.class.getDeclaredField("osId");
            if (!osIdField.isAccessible()) osIdField.setAccessible(true);

            osUUIDField = ObjectPoolMonitorVo.class.getDeclaredField("osUUID");
            if (!osUUIDField.isAccessible()) osUUIDField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    private final String osId;
    private final String osUUID;
    private final BeeObjectSource os;
    private boolean primary;

    public SpringObjectSource(String osId, BeeObjectSource os) {
        this.osId = osId;
        this.os = os;
        this.osUUID = "SpringOs_" + UUID.randomUUID().toString();
    }

    //***************************************************************************************************************//
    //                                     1: base properties (3)                                                    //
    //***************************************************************************************************************//
    public String getOsId() {
        return osId;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    //***************************************************************************************************************//
    //                                     2: Pool Monitor (4)                                                       //
    //***************************************************************************************************************//
    public void close() {
        tryToCloseDataSource(os);
    }

    public BeeObjectHandle getObjectHandle() throws Exception {
        return os.getObjectHandle();
    }

    void restartPool() {
        try {
            os.restartPool(false);
        } catch (Throwable e) {
            Log.warn("Failed to restart object pool", e);
        }
    }

    public ObjectPoolMonitorVo getPoolMonitorVo() {
        try {
            ObjectPoolMonitorVo vo = os.getPoolMonitorVo();
            if (osIdField != null) osIdField.set(vo, osId);
            if (osUUIDField != null) osUUIDField.set(vo, osUUID);
            return vo;
        } catch (Throwable e) {
            Log.warn("Failed to get pool monitor Vo", e);
            return null;
        }
    }
}
