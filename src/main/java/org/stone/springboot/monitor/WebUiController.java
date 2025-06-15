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
package org.stone.springboot.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.ObjectSourceBeanManager;

import java.util.Map;
import java.util.Objects;

import static org.stone.springboot.monitor.RequestFilter.*;
import static org.stone.tools.CommonUtil.isBlank;

/**
 * UI Controller
 *
 * @author Chris Liao
 */
@Controller
public class WebUiController {
    private final MonitorConfig config;
    private final DataSourceBeanManager dsManager;
    private final ObjectSourceBeanManager osManager;

    public WebUiController() {
        this.config = MonitorConfig.getInstance();
        this.dsManager = DataSourceBeanManager.getInstance();
        this.osManager = ObjectSourceBeanManager.getInstance();
    }

    //***************************************************************************************************************//
    //                                             1: welcome(2)                                                     //
    //***************************************************************************************************************//
    @RequestMapping(Welcome_URL)
    public String welcome1() {
        return Monitor_Page;
    }

    @RequestMapping(Welcome_URL2)
    public String welcome2() {
        return Monitor_Page;
    }

    //***************************************************************************************************************//
    //                                             2: login(1)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping(Login_URL)
    public RestResponse login(@RequestBody Map<String, String> paramMap, HttpServletRequest req) {
        HttpSession session = req.getSession();
        if ("Y".equals(session.getAttribute(config.getLoggedFlag())))//has login
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");
        if (isBlank(config.getUsername()))
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");

        try {
            String inputtedUserName = paramMap.get("username");
            String inputtedPassword = paramMap.get("password");
            if (Objects.equals(config.getUsername(), inputtedUserName) && Objects.equals(config.getPassword(), inputtedPassword)) {//checked pass
                session.setAttribute(config.getLoggedFlag(), "Y");
                return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");
            } else
                return new RestResponse(RestResponse.CODE_FAILED, null, "Login Failed");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Login Failed");
        }
    }

    //***************************************************************************************************************//
    //                                             3: beecp(3)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping(Ds_Pool_List_URL)
    public RestResponse getDsPoolList() {
        try {
            return new RestResponse(RestResponse.CODE_SUCCESS, dsManager.getDsPoolMonitorVoList(), "OK");
        } catch (Throwable e) {
            e.printStackTrace();
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to get datasource pool info");
        }
    }

    @ResponseBody
    @PostMapping(Ds_Sql_List_URL)
    public RestResponse getDsSqlList() {
        try {
            return new RestResponse(RestResponse.CODE_SUCCESS, dsManager.getSqlExecutionList(), "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to get traced sql list");
        }
    }

    @ResponseBody
    @PostMapping(Ds_Pool_Clear_URL)
    public RestResponse clearDsPool(@RequestBody Map<String, String> parameterMap) {
        try {
            String dsId = parameterMap != null ? parameterMap.get("dsId") : null;
            dsManager.clearDsPool(dsId, false);
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to clear datasource pool");
        }
    }

    @ResponseBody
    @PostMapping(Ds_Sql_Cancel_URL)
    public RestResponse cancelStatement(@RequestBody Map<String, String> parameterMap) {
        try {
            String statementUUID = parameterMap != null ? parameterMap.get("uuid") : null;
            dsManager.cancelStatementExecution(statementUUID);
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to cancel statement");
        }
    }

    //***************************************************************************************************************//
    //                                             4: beeop(2)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping(Os_Pool_List_URL)
    public RestResponse getOsPoolList() {
        try {
            return new RestResponse(RestResponse.CODE_SUCCESS, osManager.getOsPoolMonitorVoList(), "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to get object source pool info");
        }
    }

    @ResponseBody
    @PostMapping(Os_Pool_Clear_URL)
    public RestResponse clearOsPool(@RequestBody Map<String, String> parameterMap) {
        try {
            String osId = parameterMap != null ? parameterMap.get("osId") : null;
            osManager.clearOsPool(osId, false);
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to restart object source pool");
        }
    }
}
