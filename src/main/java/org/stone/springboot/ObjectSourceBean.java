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

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSource;

/**
 * object source(only support beeop)
 *
 * @author Chris Liao
 */
public class ObjectSourceBean<K, V> {
    private final String osId;
    private final BeeObjectSource<K, V> os;
    private boolean primary;

    public ObjectSourceBean(String osId, BeeObjectSource<K, V> os) {
        this.osId = osId;
        this.os = os;
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


    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        return os.getObjectHandle();
    }

    //***************************************************************************************************************//
    //                                     2: Pool Monitor (4)                                                       //
    //***************************************************************************************************************//
    public void close() throws Exception {
        os.close();
    }

    void clear() throws Exception {
        os.clear(false);
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception {
        return os.getPoolMonitorVo();
    }
}
