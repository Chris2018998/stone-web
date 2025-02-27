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
package org.stone.springboot.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.MonitoringConfigManager;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * monitoring Controller
 *
 * @author Chris Liao
 */
@Controller
public class MonitorController {
    private final static String consolePage = "/stone/monitor.html";
    private final MonitoringConfigManager monitorConfig;
    private final DataSourceBeanManager stoneMonitorManager;

    public MonitorController(MonitoringConfigManager monitorConfig, DataSourceBeanManager stoneMonitorManager) {
        this.monitorConfig = monitorConfig;
        this.stoneMonitorManager = stoneMonitorManager;
    }

    //***************************************************************************************************************//
    //                                             1: welcome(2)                                                     //
    //***************************************************************************************************************//
    @RequestMapping("/stone")
    public String welcome1() {
        return consolePage;
    }

    @RequestMapping("/stone/")
    public String welcome2() {
        return consolePage;
    }

    //***************************************************************************************************************//
    //                                             2: login(1)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping("/stone/login")
    public ControllerResponse login(@RequestBody Map<String, String> paramMap, HttpServletRequest req) {
        HttpSession session = req.getSession();
        if ("Y".equals(session.getAttribute(monitorConfig.getLoggedInSuccessTagName())))//has login
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, null, "Login Success");
        if (isBlank(monitorConfig.getConsoleUserId()))
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, null, "Login Success");

        try {
            String userId = paramMap.get("userId");
            String password = paramMap.get("password");
            if (Objects.equals(monitorConfig.getConsoleUserId(), userId) && Objects.equals(monitorConfig.getConsolePassword(), password)) {//checked pass
                session.setAttribute(monitorConfig.getLoggedInSuccessTagName(), "Y");
                return new ControllerResponse(ControllerResponse.CODE_SUCCESS, null, "Login Success");
            } else
                return new ControllerResponse(ControllerResponse.CODE_FAILED, null, "Login Failed");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Login Failed");
        }
    }

    //***************************************************************************************************************//
    //                                             3: beecp(3)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping("/stone/getLocalDataSourceList")
    public ControllerResponse getDataSourceList() {
        try {
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, stoneMonitorManager.getDataSourceMonitoringVoList(), "OK");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Failed to get datasource pool info");
        }
    }

    @ResponseBody
    @PostMapping("/stone/getLocalSqlList")
    public ControllerResponse getSqTraceList() {
        try {
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, stoneMonitorManager.getSqlExecutionList(), "OK");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Failed to get traced sql list");
        }
    }

    @ResponseBody
    @PostMapping("/stone/clearLocalDataSourcePool")
    public ControllerResponse clearDataSourcePool(@RequestBody Map<String, String> parameterMap) throws SQLException {
        try {
            String dsId = parameterMap != null ? parameterMap.get("dsId") : null;
            stoneMonitorManager.clearDataSourcePool(dsId, false);
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Failed to restart datasource pool");
        }
    }

    //***************************************************************************************************************//
    //                                             4: beeop(2)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping("/stone/getLocalObjectSourceList")
    public ControllerResponse getObjectSourceList() {
        try {
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, stoneMonitorManager.getDataSourceMonitoringVoList(), "OK");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Failed to get object source pool info");
        }
    }

    @ResponseBody
    @PostMapping("/stone/clearLocalObjectSourcePool")
    public ControllerResponse clearObjectSourcePool(@RequestBody Map<String, String> parameterMap) {
        try {
            String osId = parameterMap != null ? parameterMap.get("osId") : null;
            stoneMonitorManager.clearDataSourcePool(osId, false);
            return new ControllerResponse(ControllerResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new ControllerResponse(ControllerResponse.CODE_FAILED, e, "Failed to restart objectsource pool");
        }
    }
}
