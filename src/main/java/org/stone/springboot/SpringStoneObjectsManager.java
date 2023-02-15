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

import org.stone.beecp.pool.ConnectionPoolMonitorVo;
import org.stone.beeop.pool.ObjectPoolMonitorVo;
import org.stone.springboot.sqlTrace.StatementTrace;
import org.stone.springboot.sqlTrace.StatementTracePool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stone monitor manager
 *
 * @author Chris Liao
 */
public final class SpringStoneObjectsManager {
    private static final SpringStoneObjectsManager single = new SpringStoneObjectsManager();
    private final Map<String, SpringDataSource> dsMap;
    private final Map<String, SpringObjectSource> osMap;
    private StatementTracePool sqlStatementPool;

    private SpringStoneObjectsManager() {
        this.dsMap = new ConcurrentHashMap<>(1);
        this.osMap = new ConcurrentHashMap<>(1);
    }

    public static SpringStoneObjectsManager getInstance() {
        return single;
    }

    //***************************************************************************************************************//
    //                                     2: ds maintenance(4)                                                      //
    //***************************************************************************************************************//
    public void addDataSource(SpringDataSource ds) {
        dsMap.put(ds.getDsId(), ds);
        if (sqlStatementPool != null) ds.setStatementPool(sqlStatementPool);
    }

    public SpringDataSource getDataSource(String dsId) {
        return dsMap.get(dsId);
    }

    public void restartDataSourcePool(String dsId) {
        SpringDataSource ds = dsMap.get(dsId);
        if (ds != null) ds.restartPool();
    }

    public void setStatementTracePool(StatementTracePool statementPool) {
        this.sqlStatementPool = statementPool;
        for (SpringDataSource ds : dsMap.values())
            ds.setStatementPool(sqlStatementPool);
    }

    //***************************************************************************************************************//
    //                                     2: os maintenance(3)                                                      //
    //***************************************************************************************************************//
    public void addObjectSource(SpringObjectSource os) {
        osMap.put(os.getOsId(), os);
    }

    public SpringObjectSource getObjectSource(String osId) {
        return osMap.get(osId);
    }

    public void restartObjectSourcePool(String osId) {
        SpringObjectSource os = osMap.get(osId);
        if (os != null) os.restartPool();
    }

    //***************************************************************************************************************//
    //                                     3: Pool Monitor (2)                                                       //
    //***************************************************************************************************************//
    public List<ConnectionPoolMonitorVo> getDsPoolMonitorVoList() {
        List<ConnectionPoolMonitorVo> poolMonitorVoList = new ArrayList<>(dsMap.size());
        Iterator<SpringDataSource> iterator = dsMap.values().iterator();

        while (iterator.hasNext()) {
            SpringDataSource ds = iterator.next();
            ConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    public List<ObjectPoolMonitorVo> getOsPoolMonitorVoList() {
        List<ObjectPoolMonitorVo> poolMonitorVoList = new ArrayList<>(osMap.size());
        Iterator<SpringObjectSource> iterator = osMap.values().iterator();

        while (iterator.hasNext()) {
            SpringObjectSource os = iterator.next();
            ObjectPoolMonitorVo vo = os.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    //***************************************************************************************************************//
    //                                     4: sql-trace (1)                                                          //
    //***************************************************************************************************************//
    public Collection<StatementTrace> getSqlExecutionList() {
        return sqlStatementPool != null ? sqlStatementPool.getSqlTraceQueue() : null;
    }
}
