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

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.stone.springboot.extension.JackSonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.stone.tools.CommonUtil.isBlank;

/*
 * request filter
 *
 * @author Chris Liao
 */
public class RequestFilter implements Filter {
    static final String URL_Pattern = "/bee/*";
    static final String Welcome_URL = "/bee";
    static final String Welcome_URL2 = "/bee/";
    static final String Login_URL = "/bee/login";
    static final String Login_Page = "/bee/login.html";
    static final String Monitor_Page = "/bee/monitor.html";
    static final String Ds_Pool_List_URL = "/bee/dsPoolList";
    static final String Ds_Pool_Clear_URL = "/bee/dsPoolClear";
    static final String Ds_Sql_List_URL = "/bee/dsSqlList";
    static final String Ds_Sql_Cancel_URL = "/bee/dsSqlCancel";
    static final String Os_Pool_List_URL = "/bee/osPoolList";
    static final String Os_Pool_Clear_URL = "/bee/osPoolClear";

    private final String loggedFlag;
    private final boolean securityCheck;
    private final String[] excludeUrlSuffix = {".js", ".css", ".gif"};
    private final String[] excludeUrls = {Login_URL, Login_Page};
    private final String[] restUrls = {
            Ds_Pool_List_URL,
            Ds_Pool_Clear_URL,
            Ds_Sql_List_URL,
            Ds_Sql_Cancel_URL,
            Os_Pool_List_URL,
            Os_Pool_Clear_URL};

    RequestFilter() {
        this.loggedFlag = MonitorConfig.getInstance().getLoggedFlag();
        this.securityCheck = isBlank(MonitorConfig.getInstance().getUsername());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    public void destroy() {
        //do nothing
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (securityCheck) {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            String requestPath = httpReq.getServletPath();

            if ("Y".equals(httpReq.getSession().getAttribute(loggedFlag)) || isExcludeUrl(requestPath)) {
                chain.doFilter(req, res);
            } else if (isRestRequestUrl(requestPath)) {//is rest request url
                res.setContentType("application/json");
                OutputStream ps = res.getOutputStream();
                RestResponse restResponse = new RestResponse(RestResponse.CODE_SECURITY, null, "unauthorized");
                ps.write(JackSonUtil.object2String(restResponse).getBytes(StandardCharsets.UTF_8));
            } else {
                req.getRequestDispatcher(Login_Page).forward(req, res);
            }
        } else {
            chain.doFilter(req, res);
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
