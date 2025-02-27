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

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.stone.springboot.MonitoringConfigManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.stone.tools.CommonUtil.isBlank;

/*
 * request filter
 *
 * @author Chris Liao
 */
public class SecurityFilter implements Filter {
    private final String userId;
    private final String loggedInTagName;
    private final String[] excludeUrlSuffix = {".js", ".css", ".gif"};
    private final String[] excludeUrls = {"/stone/login", "/stone/login.html"};
    private final String[] restUrls = {"/stone/getDataSourceList", "/stone/getSqlTraceList",
            "/stone/clearDataSourcePool", "/stone/getObjectSourceList", "/stone/clearObjectSourcePool"};

    SecurityFilter(String userId, String loggedInTagName) {
        this.userId = userId;
        this.loggedInTagName = loggedInTagName;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    public void destroy() {
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
                ControllerResponse restResponse = new ControllerResponse(ControllerResponse.CODE_SECURITY, null, "unauthorized");
                ps.write(MonitoringConfigManager.getInstance().getJsonUtil().object2String(restResponse).getBytes(StandardCharsets.UTF_8));
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
