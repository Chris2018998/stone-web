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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stone.springboot.SpringStoneMonitorConfig;
import org.stone.springboot.SpringStoneObjectsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static org.stone.springboot.SpringDsRegisterUtil.stringEquals;
import static org.stone.util.CommonUtil.isBlank;

/**
 * console Controller
 *
 * @author Chris Liao
 */
@Controller
public class ConsoleController {
    private final static String consolePage = "/stone/console.html";
    private final SpringStoneMonitorConfig monitorConfig;
    private final SpringStoneObjectsManager stoneMonitorManager;

    public ConsoleController(SpringStoneMonitorConfig monitorConfig, SpringStoneObjectsManager stoneMonitorManager) {
        this.monitorConfig = monitorConfig;
        this.stoneMonitorManager = stoneMonitorManager;
    }

    //***************************************************************************************************************//
    //                                             1: console(2)                                                     //
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
    public RestfulResponse login(@RequestBody Map<String, String> paramMap, HttpServletRequest req) {
        HttpSession session = req.getSession();
        if ("Y".equals(session.getAttribute(monitorConfig.getLoggedInSuccessTagName())))//has login
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, null, "Login Success");
        if (isBlank(monitorConfig.getConsoleUserId()))
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, null, "Login Success");

        try {
            String userId = paramMap.get("userId");
            String password = paramMap.get("password");
            if (stringEquals(monitorConfig.getConsoleUserId(), userId) && stringEquals(monitorConfig.getConsolePassword(), password)) {//checked pass
                session.setAttribute(monitorConfig.getLoggedInSuccessTagName(), "Y");
                return new RestfulResponse(RestfulResponse.CODE_SUCCESS, null, "Login Success");
            } else
                return new RestfulResponse(RestfulResponse.CODE_FAILED, null, "Login Failed");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Login Failed");
        }
    }

    //***************************************************************************************************************//
    //                                             3: beecp(3)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping("/stone/getDsPoolList")
    public RestfulResponse getDataSourceList() {
        try {
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, stoneMonitorManager.getDsPoolMonitorVoList(), "OK");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Failed to get datasource pool info");
        }
    }

    @ResponseBody
    @PostMapping("/stone/getSqlTraceList")
    public RestfulResponse getSqTraceList() {
        try {
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, stoneMonitorManager.getSqlExecutionList(), "OK");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Failed to get traced sql list");
        }
    }

    @ResponseBody
    @PostMapping("/stone/restartDsPool")
    public RestfulResponse restartDataSourcePool(@RequestBody Map<String, String> parameterMap) {
        try {
            String dsId = parameterMap != null ? parameterMap.get("dsId") : null;
            stoneMonitorManager.restartDataSourcePool(dsId);
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Failed to restart datasource pool");
        }
    }

    //***************************************************************************************************************//
    //                                             4: beeop(2)                                                       //
    //***************************************************************************************************************//
    @ResponseBody
    @PostMapping("/stone/getOsPoolList")
    public RestfulResponse getObjectSourceList() {
        try {
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, stoneMonitorManager.getOsPoolMonitorVoList(), "OK");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Failed to get object source pool info");
        }
    }

    @ResponseBody
    @PostMapping("/stone/restartOsPool")
    public RestfulResponse restartObjectSourcePool(@RequestBody Map<String, String> parameterMap) {
        try {
            String osId = parameterMap != null ? parameterMap.get("osId") : null;
            stoneMonitorManager.restartObjectSourcePool(osId);
            return new RestfulResponse(RestfulResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestfulResponse(RestfulResponse.CODE_FAILED, e, "Failed to restart objectsource pool");
        }
    }
}
