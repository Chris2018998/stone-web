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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stone.springboot.SpringBootDataSourceManager;

import java.util.Map;

/**
 * Controller
 *
 * @author Chris Liao
 */
@Controller
public class DsMonitorController {
    private final static String CN_PAGE = "/stone/beecp/chinese.html";
    private final static String EN_PAGE = "/stone/beecp/english.html";

    private final SpringBootDataSourceManager dsManager = SpringBootDataSourceManager.getInstance();

    @RequestMapping("/stone/beecp")
    public String welcome1() {
        return "redirect:/stone/beecp/";
    }

    @RequestMapping("/stone/beecp/")
    public String welcome2() {
        return CN_PAGE;
    }

    @RequestMapping("/stone/beecp/cn")
    public String openChinesePage1() {
        return CN_PAGE;
    }

    @RequestMapping("/stone/beecp/chinese")
    public String openChinesePage2() {
        return CN_PAGE;
    }

    @RequestMapping("/stone/beecp/en")
    public String openEnglishPage1() {
        return EN_PAGE;
    }

    @RequestMapping("/stone/beecp/english")
    public String openEnglishPage2() {
        return EN_PAGE;
    }


    @ResponseBody
    @PostMapping("/stone/beecp/getDataSourceList")
    public RestResponse getDataSourceList() {
        try {
            return new RestResponse(RestResponse.CODE_SUCCESS, dsManager.getPoolMonitorVoList(), "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to 'getDataSourceList'");
        }
    }

    @ResponseBody
    @PostMapping("/stone/beecp/getSqlTraceList")
    public RestResponse getSqTraceList() {
        try {
            return new RestResponse(RestResponse.CODE_SUCCESS, dsManager.getSqlExecutionList(), "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to 'getSqlTraceList'");
        }
    }

    @ResponseBody
    @PostMapping("/stone/beecp/clearDataSource")
    public RestResponse clearDsConnections(@RequestBody Map<String, String> parameterMap) {
        try {
            String dsId = parameterMap != null ? parameterMap.get("dsId") : null;
            dsManager.clearDsConnections(dsId);
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "OK");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Failed to 'clearDsConnections'");
        }
    }
}
