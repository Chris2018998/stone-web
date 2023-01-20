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
package org.stone.springboot.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.stone.springboot.SpringBootDataSourceUtil;
import org.stone.springboot.controller.RestResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static org.stone.util.CommonUtil.isBlank;

/**
 * request filter
 *
 * @author Chris Liao
 */
@Controller
public class LoginController {
    private final String userId;
    private final String password;
    private final String loggedInTagName;
    private HttpSession session;

    LoginController(String userId, String password, String loggedInTagName) {
        this.userId = userId;
        this.password = password;
        this.loggedInTagName = loggedInTagName;
    }

    @ModelAttribute
    public void setReqAndRes(HttpServletRequest req, HttpServletResponse res) {
        this.session = req.getSession();
    }

    @ResponseBody
    @PostMapping("/stone/login")
    public RestResponse login(@RequestBody Map<String, String> paramMap) {
        if ("Y".equals(session.getAttribute(loggedInTagName)))//has login
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");
        if (isBlank(userId))
            return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");

        try {
            String userId = paramMap.get("userId");
            String password = paramMap.get("password");
            if (this.userId.equals(userId) && SpringBootDataSourceUtil.stringEquals(this.password, password)) {//checked pass
                session.setAttribute(loggedInTagName, "Y");
                return new RestResponse(RestResponse.CODE_SUCCESS, null, "Login Success");
            } else
                return new RestResponse(RestResponse.CODE_FAILED, null, "Login Failed");
        } catch (Throwable e) {
            return new RestResponse(RestResponse.CODE_FAILED, e, "Login Failed");
        }
    }
}
