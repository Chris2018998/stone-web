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

import org.stone.springboot.datasource.SpringBootDataSourceUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.stone.util.CommonUtil.isBlank;

/**
 * request filter
 *
 * @author Chris Liao
 */
public class LoginedCheckFilter implements Filter {
    private final String userId;
    private final String loggedInTagName;
    private final String[] excludeUrlSuffix = {".js", ".css", ".gif"};
    private final String[] excludeUrls = {"/stone/login", "/stone/login.html"};
    private final String[] restUrls = {"/stone/getDsPoolList", "/stone/getSqlTraceList", "/stone/restartDsPool", "/stone/getOsPoolList", "/stone/restartOsPool"};

    LoginedCheckFilter(String userId, String loggedInTagName) {
        this.userId = userId;
        this.loggedInTagName = loggedInTagName;
    }

    public void destroy() {
        //do nothing
    }

    public void init(FilterConfig var1) {
        //do nothing
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (isBlank(userId)) {
            chain.doFilter(req, res);
        } else {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            String requestPath = httpReq.getServletPath();

            if ("Y".equals(httpReq.getSession().getAttribute(loggedInTagName)) || isExcludeUrl(requestPath)) {
                chain.doFilter(req, res);
            } else if (isRestRequestUrl(requestPath)) {//is rest request url
                res.setContentType("application/json");
                OutputStream ps = res.getOutputStream();
                RestfulResponse restResponse = new RestfulResponse(RestfulResponse.CODE_SECURITY, null, "unauthorized");
                ps.write(SpringBootDataSourceUtil.object2String(restResponse).getBytes(StandardCharsets.UTF_8));
            } else {
                req.getRequestDispatcher("/stone/login.html").forward(req, res);
            }
        }
    }

    private boolean isRestRequestUrl(String requestPath) {
        for (String str : restUrls)
            if (requestPath.endsWith(str)) return true;
        return false;
    }

    private boolean isExcludeUrl(String requestPath) {
        for (String str : excludeUrls)
            if (requestPath.equals(str)) return true;
        for (String str : excludeUrlSuffix)
            if (requestPath.endsWith(str)) return true;
        return false;
    }
}
