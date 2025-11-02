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
package org.stone.springboot.beecp.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.stone.springboot.annotation.EnableBeeDs;
import org.stone.springboot.beecp.util.DataSourceUtil;
import org.stone.springboot.controller.MonitorControllerResponse;
import org.stone.springboot.exception.DataSourceException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.stone.springboot.beecp.util.DataSourceUtil.*;
import static org.stone.springboot.controller.MonitorControllerResponse.CODE_FAILED;
import static org.stone.springboot.controller.MonitorControllerResponse.CODE_SUCCESS;
import static org.stone.tools.CommonUtil.isBlank;

/*
 * DataSource Tester Controller with Derby driver
 *
 *  @author Chris Liao
 */
@RestController
@SpringBootApplication
@EnableBeeDs(runMonitor = true)
public class DataSourceController {
    @Autowired
    @Qualifier("ds1")
    private DataSource ds1;
    @Autowired
    @Qualifier("ds2")
    private DataSource ds2;

    @PostConstruct
    public void init() throws SQLException {
        Map<String, Boolean> sqlMap = DataSourceUtil.getInitSQLMap();
        DataSourceUtil.executeSQLMap(ds1, sqlMap, Type_Statement);
        DataSourceUtil.executeSQLMap(ds2, sqlMap, Type_Statement);
    }

    @PostMapping("/testGetConnection")
    public MonitorControllerResponse testGetConnection(@RequestBody Map<String, String> map) {
        try {
            String dsId = map != null ? map.get("dsId") : null;
            if (isBlank(dsId))
                throw new DataSourceException("Not provided data source id");
            if (!"ds1".equals(dsId) && !"ds2".equals(dsId))
                throw new DataSourceException("DataSource Id must be one of list(ds1,ds2)");

            DataSource ds = "ds1".equals(dsId) ? ds1 : ds2;
            try (Connection ignored = ds.getConnection()) {
                return new MonitorControllerResponse(CODE_SUCCESS, "OK", "OK");
            }
        } catch (Throwable e) {
            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/executeSQL")
    public MonitorControllerResponse testSQL(@RequestBody Map<String, String> map) {
        try {
            String dsId = map.get("dsId");
            String sql = map.get("sql");
            String type = map.get("type");

            if (isBlank(dsId))
                throw new DataSourceException("DataSource Id can't be null or empty");
            if (!"ds1".equals(dsId) && !"ds2".equals(dsId))
                throw new DataSourceException("DataSource Id must be one of list(ds1,ds2)");
            if (isBlank(sql))
                throw new DataSourceException("Execute SQL can't be null or empty");
            if (isBlank(type))
                throw new DataSourceException("Execute type can't be null or empty");

            int sqlExecutionType;
            if ("Statement".equalsIgnoreCase(type)) {
                sqlExecutionType = Type_Statement;
            } else if ("PreparedStatement".equalsIgnoreCase(type)) {
                sqlExecutionType = Type_PreparedStatement;
            } else if ("CallableStatement".equalsIgnoreCase(type)) {
                sqlExecutionType = Type_CallableStatement;
            } else {
                throw new DataSourceException("Execute type must be one of list(Statement,PreparedStatement,CallableStatement)");
            }

            DataSource ds = "ds1".equals(dsId) ? ds1 : ds2;
            Map<String, Boolean> sqlMap = new HashMap<>(1);
            sqlMap.put(sql, Boolean.FALSE);
            DataSourceUtil.executeSQLMap(ds, sqlMap, sqlExecutionType);
            return new MonitorControllerResponse(CODE_SUCCESS, "OK", "OK");
        } catch (Throwable e) {
            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }


//    @PostMapping("/testGetConnection1")
//    @BeeDsId("ds1")
//    public MonitorControllerResponse testCombineDs1() {
//        try {
//            return new MonitorControllerResponse(CODE_SUCCESS, ServerSideUtil.testGetConnection(combineDs), "OK");
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
//        }
//    }
//
//    @PostMapping("/testGetConnection2")
//    @BeeDsId("ds2")
//    public MonitorControllerResponse testCombineDs2() {
//        try {
//            return new MonitorControllerResponse(CODE_SUCCESS, ServerSideUtil.testGetConnection(combineDs), "OK");
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
//        }
//    }

//    @PostMapping("/testExecSQL1")
//    @BeeDsId("ds1")
//    public MonitorControllerResponse testExecSQL1(@RequestBody Map<String, String> map) {
//        try {
//            String sql = map.get("sql");
//            String type = map.get("type");
//            String slowInd = map.get("slowInd");
//
//            return new MonitorControllerResponse(CODE_SUCCESS, ServerSideUtil.testSQL(sql, type, slowInd), "OK");
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
//        }
//    }

//    @PostMapping("/testExecSQL2")
//    @BeeDsId("ds2")
//    public MonitorControllerResponse testExecSQL2(@RequestBody Map<String, String> map) {
//        try {
//            String sql = map.get("sql");
//            String type = map.get("type");
//            String slowInd = map.get("slowInd");
//            return new MonitorControllerResponse(CODE_SUCCESS, ServerSideUtil.testSQL(sql, type, slowInd), "OK");
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return new MonitorControllerResponse(CODE_FAILED, e.getMessage(), "Failed");
//        }
//    }
}