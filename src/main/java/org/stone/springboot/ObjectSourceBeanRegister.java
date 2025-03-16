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

import static org.stone.springboot.Constants.Config_OS_Id;
import static org.stone.springboot.Constants.Config_OS_Prefix;
import static org.stone.tools.CommonUtil.isBlank;

/*
 * A default object source register to import single data source to spring container
 *
 * spring.objectSource.osId=beeOs
 * spring.objectSource.type=org.stone.beecp.BeeObjectSource
 * spring.objectSource.fairMode=true
 * spring.objectSource.initialSize=10
 * spring.objectSource.maxActive =10
 *
 * @author Chris Liao
 */
@ConditionalOnClass(BeeObjectSource.class)
@ConditionalOnProperty(name = "spring.objectSource.type", havingValue = "org.stone.beeop.BeeObjectSource")
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class ObjectSourceBeanRegister {

    @Bean
    public <K, V> BeeObjectSource<K, V> beeObjectSource(Environment environment) {
        //1:Create bee data source with loading configuration from Spring boot environment
        ObjectSourceBeanManager<K, V> osManager = ObjectSourceBeanManager.getInstance();
        String osId = osManager.getConfigValue(Config_OS_Prefix, Config_OS_Id, environment);
        if (isBlank(osId)) osId = "beeObjectSource";

        ObjectSourceBean<K, V> osBean = osManager.createObjectSourceBean(Config_OS_Prefix, osId, environment);
        osManager.addObjectSource(osBean);
        return osBean;
    }
}
