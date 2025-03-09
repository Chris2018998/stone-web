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

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.stone.beeop.BeeObjectSource;

/*
 * Default register to import an object pool to spring boot
 *
 * spring.objectsource.dsId=beeDs
 * spring.objectsource.type=org.stone.beecp.Beeobjectsource
 * spring.objectsource.username=root
 * spring.objectsource.password=
 * spring.objectsource.jdbcUrl=jdbc:mysql://localhost:3306/test
 * spring.objectsource.driverClassName=com.mysql.jdbc.Driver
 * spring.objectsource.fairMode=true
 * spring.objectsource.initialSize=10
 * spring.objectsource.maxActive =10
 *
 * @author Chris Liao
 */
@ConditionalOnClass(BeeObjectSource.class)
@ConditionalOnProperty(name = "spring.objectsource.type", havingValue = "org.stone.beeop.BeeObjectSource")
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class ObjectSourceBeanRegister {
    @Bean
    public BeeObjectSource beeObjectSource(Environment environment) throws Exception {
//        //1:read ds id
//        String dsId = SpringobjectsourceUtil.getConfigValue(SpringobjectsourceUtil.Config_DS_Prefix, SpringobjectsourceUtil.Config_DS_Id, environment);
//        if (isBlank(dsId)) dsId = "beeobjectsource";//default ds Id
//
//        //2:read objectsource monitor config
//        MonitorConfig objectsourceMonitorConfig = SpringobjectsourceUtil.loadDsMonitorConfig(environment);
//
//        //3:setup monitor config
//        SpringobjectsourceManager.getInstance().setupMonitorConfig(objectsourceMonitorConfig);
//
//        //4:create Beeobjectsource
//        objectsource ds = new BeeobjectsourceFactory().createobjectsource(SpringobjectsourceUtil.Config_DS_Prefix, dsId, environment);
//        Springobjectsource springDs = new Springobjectsource(dsId, ds, false);
//        SpringobjectsourceManager.getInstance().addSpringBootobjectsource(springDs);
//
//        return springDs;

        return null;
    }
}
