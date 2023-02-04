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
import java.lang.reflect.Method;
import java.util.UUID;

import static org.stone.springboot.datasource.SpringBootDataSourceUtil.tryToCloseDataSource;

/**
 * object source(only support beeop)
 *
 * @author Chris Liao
 */
public final class SpringObjectSource {
    private final static Logger Log = LoggerFactory.getLogger(SpringObjectSource.class);
    private final String osId;
    private final String osUUID;
    private final BeeObjectSource os;

    private boolean primary;
    private Method poolRestartMethod;
    private Method poolMonitorVoMethod;
    private boolean notSetBeeDsId = true;

    SpringObjectSource(String osId, BeeObjectSource os) {
        this.osId = osId;
        this.os = os;
        this.osUUID = "SpringOs_" + UUID.randomUUID().toString();
    }

    String getOsId() {
        return osId;
    }

    boolean isPrimary() {
        return primary;
    }

    void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public BeeObjectHandle getObject() throws Exception {
        return os.getObject();
    }

    public void close() {
        tryToCloseDataSource(os);
    }

    public void restartPool() {
        if (poolRestartMethod != null) {
            try {
                poolRestartMethod.invoke(os, false);
            } catch (Throwable e) {
                Log.warn("Failed to execute dataSource 'clear' method", e);
            }
        }
    }

    ObjectPoolMonitorVo getPoolMonitorVo() {
        if (poolMonitorVoMethod != null) {
            try {
                ObjectPoolMonitorVo vo = (ObjectPoolMonitorVo) poolMonitorVoMethod.invoke(os);
                if (notSetBeeDsId) setBeeOsIdToMonitorSingletonVo(vo);
                return vo;
            } catch (Throwable e) {
                Log.warn("Failed to execute dataSource 'getPoolMonitorVo' method", e);
            }
        }
        return null;
    }

    private synchronized void setBeeOsIdToMonitorSingletonVo(ObjectPoolMonitorVo vo) {
        setValueToField(vo, "osId", osId);
        setValueToField(vo, "osUUID", osUUID);
        notSetBeeDsId = false;
    }

    private void setValueToField(ObjectPoolMonitorVo vo, String fieldId, String value) {
        Field field = null;
        try {
            Class monitorVoClass = ObjectPoolMonitorVo.class;
            field = monitorVoClass.getDeclaredField(fieldId);
            field.setAccessible(true);
            field.set(vo, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //do nothing
        } finally {
            if (field != null) field.setAccessible(false);
        }
    }

    private void readBeeDsMethods() {
        Class dsClass = os.getClass();
        try {
            poolMonitorVoMethod = dsClass.getMethod("getPoolMonitorVo");
        } catch (NoSuchMethodException e) {
            Log.warn("ObjectSource method(getPoolMonitorVo) not found", e);
        }
        try {
            poolRestartMethod = dsClass.getMethod("restartPool", Boolean.TYPE);
        } catch (NoSuchMethodException e) {
            Log.warn("ObjectSource method(restartPool) not found", e);
        }
    }
}
