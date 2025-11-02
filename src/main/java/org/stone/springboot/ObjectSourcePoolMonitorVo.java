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

import org.stone.beeop.BeeObjectPoolMonitorVo;

import java.util.UUID;

/*
 * monitor vo
 *
 * @author Chris Liao
 */
public class ObjectSourcePoolMonitorVo implements BeeObjectPoolMonitorVo {
    private final String osId;
    private final String osUUID;
    private BeeObjectPoolMonitorVo vo;

    ObjectSourcePoolMonitorVo(String osId) {
        this.osId = osId;
        this.osUUID = UUID.randomUUID().toString();
    }

    public void setVo(BeeObjectPoolMonitorVo vo) {
        this.vo = vo;
    }

    public String getOsId() {
        return osId;
    }

    public String getOsUUID() {
        return osUUID;
    }

    public int getTransferWaitingSize() {
        return vo.getTransferWaitingSize();
    }

    public int getSemaphoreWaitingSize() {
        return vo.getSemaphoreWaitingSize();
    }

    public int getCreatingTimeoutSize() {
        return vo.getCreatingTimeoutSize();
    }

    public int getCreatingSize() {
        return vo.getCreatingSize();
    }

    public int getBorrowedSize() {
        return vo.getBorrowedSize();
    }

    public int getPoolMaxSize() {
        return vo.getPoolMaxSize();
    }

    public int getIdleSize() {
        return vo.getIdleSize();
    }

    public int getKeySize() {
        return vo.getKeySize();
    }

    public int getPoolState() {
        return vo.getPoolState();
    }

    public String getPoolMode() {
        return vo.getPoolMode();
    }

    public String getPoolName() {
        return vo.getPoolName();
    }

}
