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

import org.stone.beecp.BeeConnectionPoolMonitorVo;
import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.springboot.exception.DataSourceException;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Management tool to maintain registered datasource beans.
 *
 * @author Chris Liao
 */
public final class DataSourceBeanManager {
    private static final DataSourceBeanManager single = new DataSourceBeanManager();
    private final Map<String, DataSourceBean> dataSourceMap = new ConcurrentHashMap<>(1);

    public static DataSourceBeanManager getInstance() {
        return single;
    }

    //***************************************************************************************************************//
    //                                     1: ds maintenance(4)                                                      //
    //***************************************************************************************************************//
    public DataSourceBean getDataSource(String dsId) {
        return dataSourceMap.get(dsId);
    }

    void addDataSource(DataSourceBean ds) {
        dataSourceMap.put(ds.getDsId(), ds);
    }


    public void restart(String dsId, boolean force) throws SQLException {
        DataSourceBean ds = dataSourceMap.get(dsId);
        if (ds == null) throw new DataSourceException("Data source not found with id:" + dsId);
        ds.restart(force);
    }

    public boolean cancelStatement(String dsId, String logID) throws SQLException {
        DataSourceBean ds = dataSourceMap.get(dsId);
        if (ds == null) throw new DataSourceException("Data source not found with id:" + dsId);
        return ds.cancelStatement(logID);
    }

    public List<BeeConnectionPoolMonitorVo> getAllDsPoolMonitorVos() throws SQLException {
        List<BeeConnectionPoolMonitorVo> poolMonitorVoList = new ArrayList<>(dataSourceMap.size());
        Iterator<DataSourceBean> iterator = dataSourceMap.values().iterator();

        while (iterator.hasNext()) {
            DataSourceBean ds = iterator.next();
            BeeConnectionPoolMonitorVo vo = ds.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.isClosed()) {
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }

    public Collection<BeeMethodExecutionLog> getAllDsSqlExecutionLogs() throws SQLException {
        List<BeeMethodExecutionLog> logList = new ArrayList<>(dataSourceMap.size());
        for (DataSourceBean ds : dataSourceMap.values()) {
            logList.addAll(ds.getMethodExecutionLog(BeeMethodExecutionLog.Type_SQL_Execution));
        }
        return logList;
    }
}
