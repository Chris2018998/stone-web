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
import org.stone.beecp.BeeDataSource;
import org.stone.springboot.factory.SpringBeeDataSourceFactory;

import javax.sql.DataSource;

import static org.stone.springboot.Constants.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * A configuration example is below
 *
 * spring.datasource.dsId=beeDs
 * spring.datasource.type=org.stone.beecp.BeeDataSource
 * spring.datasource.username=root
 * spring.datasource.password=root
 * spring.datasource.jdbcUrl=jdbc:mysql://localhost:3306/test
 * spring.datasource.driverClassName=com.mysql.jdbc.Driver
 * spring.datasource.fairMode=true
 * spring.datasource.initialSize=10
 * spring.datasource.maxActive=10
 *
 * @author Chris Liao
 */
@ConditionalOnClass(BeeDataSource.class)
@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.stone.beecp.BeeDataSource")
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class DataSourceBeanRegister {

    @Bean
    public DataSource beeDataSource(Environment environment) throws Exception {
        //1: Get manager
        DataSourceBeanManager dsManager = DataSourceBeanManager.getInstance();

        //2: Get configured Ids of data source
        String dsId = dsManager.getConfigValue(Config_DS_Prefix, Config_DS_Id, environment);
        if (isBlank(dsId)) dsId = "beeDataSource";

        //3: Load monitoring configuration
        MonitoringConfigManager.getInstance().loadMonitorConfig(environment);

        //4: Create datasource with configuration
        DataSource ds = new SpringBeeDataSourceFactory().createDataSource(Config_DS_Prefix, dsId, environment);
        String primaryText = dsManager.getConfigValue(Config_DS_Prefix, Config_DS_Primary, environment);
        boolean isPrimary = isNotBlank(primaryText) && Boolean.valueOf(primaryText).booleanValue();

        DataSourceBean springDs = new DataSourceBean(dsId, false, isPrimary, ds);
        dsManager.addDataSource(springDs);

        //5: return created datasource to be registered in spring contain
        return springDs;
    }
}
