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
package org.stone.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.stone.beecp.BeeDataSource;
import org.stone.beeop.BeeObjectSource;

import static org.stone.util.CommonUtil.isBlank;

/*
 * config example
 *
 * spring.datasource.dsId=beeDs
 * spring.datasource.type=org.stone.beecp.BeeDataSource
 * spring.datasource.username=root
 * spring.datasource.password=
 * spring.datasource.jdbcUrl=jdbc:mysql://localhost:3306/test
 * spring.datasource.driverClassName=com.mysql.jdbc.Driver
 * spring.datasource.fairMode=true
 * spring.datasource.initialSize=10
 * spring.datasource.maxActive=10
 *
 * @author Chris Liao
 */
@ConditionalOnClass({BeeDataSource.class})
@ConditionalOnProperty(name = "spring.objectsource.type", havingValue = "org.stone.beeop.BeeObjectSource")
public class SpringSingleOsRegister {
    @Bean
    public SpringObjectSource beeObjectSource(Environment environment) throws Exception {
        //1:read ds Id
        String osId = SpringDsRegisterUtil.getConfigValue(SpringDsRegisterUtil.Config_DS_Prefix, SpringDsRegisterUtil.Config_DS_Id, environment);
        if (isBlank(osId)) osId = "beeObjectSource";//default os Id

        //2:read datasource controller config
        SpringSourceMonitorConfig monitorConfig = SpringDsRegisterUtil.readMonitorConfig(environment);

        //3:setup controller config
        //SpringSourceMonitorManager.getInstance().setupSqlTrace(monitorConfig);

        //4:create BeeDataSource
        BeeObjectSource os = null; //SpringDsRegisterUtil.createSpringBootDataSource(SpringDsRegisterUtil.Config_DS_Prefix, osId, environment);
        SpringObjectSource springOs = new SpringObjectSource(osId, os);
        SpringSourceMonitorManager.getInstance().addObjectSource(springOs);
        return springOs;
    }
}
