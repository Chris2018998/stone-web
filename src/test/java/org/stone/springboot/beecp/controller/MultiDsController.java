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
import org.stone.springboot.annotation.BeeDsId;
import org.stone.springboot.annotation.EnableBeeDs;
import org.stone.springboot.beecp.util.ServerSideUtil;
import org.stone.springboot.exception.DataSourceException;
import org.stone.springboot.monitor.RestResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.stone.springboot.monitor.RestResponse.CODE_FAILED;
import static org.stone.springboot.monitor.RestResponse.CODE_SUCCESS;
import static org.stone.tools.CommonUtil.isBlank;

/*
 *  DataSource Tester Controller
 *
 *  @author Chris Liao
 */
@RestController
@SpringBootApplication
@EnableBeeDs(runMonitor = true)
public class MultiDsController {

    @Autowired
    @Qualifier("ds1")
    private DataSource ds1;
    @Autowired
    @Qualifier("ds2")
    private DataSource ds2;

    @Autowired
    @Qualifier("combineDs")
    private DataSource combineDs;

    public static void hello() {

    }

    @PostConstruct
    public void init() throws Exception {
        StringBuilder sqlBuilder1 = new StringBuilder();
        StringBuilder sqlBuilder2 = new StringBuilder();
        StringBuilder sqlBuilder3 = new StringBuilder();
        sqlBuilder1.append("CREATE TABLE TEST_USER(")
                .append("USER_ID VARCHAR(10),")
                .append("USER_NAME VARCHAR(10))");
        sqlBuilder2.append("CREATE TABLE TEST_USER2(")
                .append("USER_ID VARCHAR(10),")
                .append("USER_NAME VARCHAR(10))");

        sqlBuilder3
                .append("CREATE PROCEDURE BEECP_HELLO()")
                .append("PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME")
                .append("'org.stone.springboot.beecp.controller.MultiDsController.hello'");

        runTableCreationSQL("ds1", ds1, sqlBuilder1.toString(), sqlBuilder2.toString(), sqlBuilder3.toString());
        runTableCreationSQL("ds2", ds2, sqlBuilder1.toString(), sqlBuilder2.toString(), sqlBuilder3.toString());
    }

    private void runTableCreationSQL(String dsName, DataSource ds, String sql1, String sql2, String sql3) throws Exception {
        Connection con = null;
        Statement st = null;

        //drop
        try {
            con = ds.getConnection();
            st = con.createStatement();

            try {
                st.execute("drop table TEST_USER");
            } catch (Exception e) {
            }

            try {
                st.execute("drop table TEST_USER2");
            } catch (Exception e) {
            }

            try {
                st.execute("drop PROCEDURE BEECP_HELLO");
            } catch (Exception e) {
            }

        } catch (Exception e) {
        } finally {
            if (con != null) con.close();
        }

        //create
        try {
            con = ds.getConnection();
            st = con.createStatement();
            st.execute(sql1);
            st.execute(sql2);
            st.execute(sql3);
        } catch (Exception e) {
        } finally {
            if (con != null) con.close();
        }
    }

    @PostMapping("/testGetConnection")
    public RestResponse testGetConnection(@RequestBody Map<String, String> map) {
        try {
            String dsId = map != null ? map.get("dsId") : null;
            if (isBlank(dsId))
                throw new DataSourceException("DataSource Id can't be null or empty");
            if (!"ds1".equals(dsId) && !"ds2".equals(dsId))
                throw new DataSourceException("DataSource Id must be one of list(ds1,ds2)");

            DataSource ds = "ds1".equals(dsId) ? ds1 : ds2;
            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testGetConnection(ds), "OK");
        } catch (Throwable e) {
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/testSQL")
    public RestResponse testSQL(@RequestBody Map<String, String> map) {
        try {
            String dsId = map.get("dsId");
            String sql = map.get("sql");
            String type = map.get("type");
            String slowInd = map.get("slowInd");

            if (isBlank(dsId))
                throw new DataSourceException("DataSource Id can't be null or empty");
            if (!"ds1".equals(dsId) && !"ds2".equals(dsId))
                throw new DataSourceException("DataSource Id must be one of list(ds1,ds2)");
            if (isBlank(sql))
                throw new DataSourceException("Execute SQL can't be null or empty");
            if (isBlank(type))
                throw new DataSourceException("Execute type't be null or empty");
            if (!"Statement".equalsIgnoreCase(type) && !"PreparedStatement".equalsIgnoreCase(type) && !"CallableStatement".equalsIgnoreCase(type))
                throw new DataSourceException("Execute type must be one of list(Statement,PreparedStatement,CallableStatement)");

            DataSource ds = "ds1".equals(dsId) ? ds1 : ds2;
            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testSQL(ds, sql, type, slowInd), "Ok");
        } catch (Throwable e) {
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/testGetConnection1")
    @BeeDsId("ds1")
    public RestResponse testCombineDs1() {
        try {
            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testGetConnection(combineDs), "OK");
        } catch (Throwable e) {
            e.printStackTrace();
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/testGetConnection2")
    @BeeDsId("ds2")
    public RestResponse testCombineDs2() {
        try {
            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testGetConnection(combineDs), "OK");
        } catch (Throwable e) {
            e.printStackTrace();
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/testExecSQL1")
    @BeeDsId("ds1")
    public RestResponse testExecSQL1(@RequestBody Map<String, String> map) {
        try {
            String sql = map.get("sql");
            String type = map.get("type");
            String slowInd = map.get("slowInd");

            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testSQL(combineDs, sql, type, slowInd), "OK");
        } catch (Throwable e) {
            e.printStackTrace();
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }

    @PostMapping("/testExecSQL2")
    @BeeDsId("ds2")
    public RestResponse testExecSQL2(@RequestBody Map<String, String> map) {
        try {
            String sql = map.get("sql");
            String type = map.get("type");
            String slowInd = map.get("slowInd");
            return new RestResponse(CODE_SUCCESS, ServerSideUtil.testSQL(combineDs, sql, type, slowInd), "OK");
        } catch (Throwable e) {
            e.printStackTrace();
            return new RestResponse(CODE_FAILED, e.getMessage(), "Failed");
        }
    }
}