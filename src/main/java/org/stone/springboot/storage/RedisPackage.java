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
package org.stone.springboot.storage;

import org.stone.beecp.pool.ConnectionPoolMonitorVo;
import org.stone.springboot.sqlTrace.StatementTrace;

import java.util.Collection;
import java.util.List;

/**
 * Redis Package object
 *
 * @author Chris Liao
 */

public class RedisPackage {
    private String packageUUID;
    private List<ConnectionPoolMonitorVo> dsList;
    private Collection<StatementTrace> sqlList;

    RedisPackage() {
    }

    RedisPackage(String packageUUID) {
        this.packageUUID = packageUUID;
    }

    public String getPackageUUID() {
        return packageUUID;
    }

    public void setPackageUUID(String packageUUID) {
        this.packageUUID = packageUUID;
    }

    public List<ConnectionPoolMonitorVo> getDsList() {
        return dsList;
    }

    public void setDsList(List<ConnectionPoolMonitorVo> dsList) {
        this.dsList = dsList;
    }

    public Collection<StatementTrace> getSqlList() {
        return sqlList;
    }

    public void setSqlList(Collection<StatementTrace> sqlList) {
        this.sqlList = sqlList;
    }
}
