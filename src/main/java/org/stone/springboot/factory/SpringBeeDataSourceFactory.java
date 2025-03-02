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
package org.stone.springboot.factory;

import org.springframework.core.env.Environment;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.jta.BeeJtaDataSource;
import org.stone.springboot.DataSourceBeanManager;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.tools.CommonUtil.isBlank;

/*
 * BeeDataSource Spring boot Factory
 *
 *  spring.datasource.d1.poolName=BeeCP1
 *  spring.datasource.d1.username=root
 *  spring.datasource.d1.password=root
 *  spring.datasource.d1.jdbcUrl=jdbc:mysql://localhost:3306/test
 *  spring.datasource.d1.driverClassName=com.mysql.cj.jdbc.Driver
 *
 *  @author Chris liao
 */
public class SpringBeeDataSourceFactory implements SpringDataSourceFactory {

    private static void setConnectPropertiesConfig(BeeDataSourceConfig config, String prefix, Environment environment) {
        DataSourceBeanManager manager = DataSourceBeanManager.getInstance();

        config.addConnectProperty(manager.getConfigValue(prefix, CONFIG_CONNECT_PROP, environment));
        String connectPropertiesCount = manager.getConfigValue(prefix, CONFIG_CONNECT_PROP_SIZE, environment);
        if (!isBlank(connectPropertiesCount)) {
            int count = Integer.parseInt(connectPropertiesCount.trim());
            for (int i = 1; i <= count; i++)
                config.addConnectProperty(manager.getConfigValue(prefix, CONFIG_CONNECT_PROP_KEY_PREFIX + i, environment));
        }
    }

    public DataSource createDataSource(String prefix, String dsId, Environment environment) throws SpringDataSourceException {
        DataSourceBeanManager manager = DataSourceBeanManager.getInstance();

        //1:read spring configuration and inject to datasource's config object
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        manager.setConfigPropertiesValue(config, prefix, dsId, environment);
        setConnectPropertiesConfig(config, prefix, environment);

        //2:try to lookup TransactionManager with jndi name
        TransactionManager tm = null;
        String tmJndiName = manager.getConfigValue(prefix, CONFIG_TM_JNDI, environment);
        if (!isBlank(tmJndiName)) {
            try {
                Context nameCtx = new InitialContext();
                tm = (TransactionManager) nameCtx.lookup(tmJndiName);
            } catch (NamingException e) {
                throw new SpringDataSourceException("Failed to look transaction manager with jndi name:" + tmJndiName, e);
            }
        }

        //3:create dataSource instance
        BeeDataSource ds = new BeeDataSource(config);
        return (tm != null) ? new BeeJtaDataSource(ds, tm) : ds;
    }
}