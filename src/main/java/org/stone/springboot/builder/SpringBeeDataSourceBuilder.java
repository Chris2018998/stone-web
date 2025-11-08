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
package org.stone.springboot.builder;

import jakarta.transaction.TransactionManager;
import org.springframework.core.env.Environment;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.jta.BeeJtaDataSource;
import org.stone.springboot.SpringBootEnvironmentUtil;
import org.stone.springboot.exception.DataSourceException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.stone.beecp.pool.ConnectionPoolStatics.*;
import static org.stone.springboot.Constants.Config_ThreadLocal_Enable;
import static org.stone.springboot.Constants.Config_Virtual_Thread;
import static org.stone.tools.CommonUtil.isNotBlank;

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
public class SpringBeeDataSourceBuilder implements SpringDataSourceBuilder {

    private void setConnectPropertiesConfig(BeeDataSourceConfig config, String dsPrefix, Environment environment) {
        config.addConnectionFactoryProperty(SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_FACTORY_PROP, environment));
        String connectPropertiesCount = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_FACTORY_PROP_SIZE, environment);
        if (isNotBlank(connectPropertiesCount)) {
            int count = Integer.parseInt(connectPropertiesCount.trim());
            for (int i = 1; i <= count; i++)
                config.addConnectionFactoryProperty(SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_FACTORY_PROP_KEY_PREFIX + i, environment));
        }
    }

    private void setSqlExceptionFatalConfig(BeeDataSourceConfig config, String dsPrefix, Environment environment) {
        String sqlExceptionCode = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_SQL_EXCEPTION_CODE, environment);
        String sqlExceptionState = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_SQL_EXCEPTION_STATE, environment);

        if (isNotBlank(sqlExceptionCode)) {
            for (String code : sqlExceptionCode.trim().split(",")) {
                try {
                    config.addSqlExceptionCode(Integer.parseInt(code));
                } catch (NumberFormatException e) {
                    throw new BeeDataSourceConfigException(code + " is not a valid SQLException error code");
                }
            }
        }

        if (isNotBlank(sqlExceptionState)) {
            for (String state : sqlExceptionState.trim().split(",")) {
                config.addSqlExceptionState(state);
            }
        }
    }

    private void setConfigPrintExclusionList(BeeDataSourceConfig config, String dsPrefix, Environment environment) {
        String exclusionListText = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_EXCLUSION_LIST_OF_PRINT, environment);
        if (isNotBlank(exclusionListText)) {
            config.clearExclusionListOfPrint();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                config.addExclusionNameOfPrint(exclusion);
            }
        }
    }

    public DataSource create(String dsPrefix, String dsId, Environment environment) throws DataSourceException {
        //1:read spring configuration and inject to datasource's config object
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        SpringBootEnvironmentUtil.setConfigPropertiesValue(config, dsPrefix, dsId, environment);
        setConnectPropertiesConfig(config, dsPrefix, environment);
        setSqlExceptionFatalConfig(config, dsPrefix, environment);
        setConfigPrintExclusionList(config, dsPrefix, environment);

        //2:try to lookup TransactionManager by jndi
        TransactionManager tm = null;
        String tmJndiName = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, CONFIG_TM_JNDI, environment);
        if (isNotBlank(tmJndiName)) {
            try {
                Context nameCtx = new InitialContext();
                tm = (TransactionManager) nameCtx.lookup(tmJndiName);
            } catch (NamingException e) {
                throw new DataSourceException("Failed to look transaction manager with jndi name:" + tmJndiName, e);
            }
        }

        //3:create dataSource instance
        BeeDataSource ds = new BeeDataSource(config);

        //4:disable threadLocal if exists virtual thread config item
        String threadLocalEnable = SpringBootEnvironmentUtil.getConfigValue(dsPrefix, Config_ThreadLocal_Enable, environment);
        if (threadLocalEnable == null) {
            boolean enableVirtualThread = Boolean.parseBoolean(environment.getProperty(Config_Virtual_Thread, "false"));
            ds.setUseThreadLocal(!enableVirtualThread);
        }

        //5:create jta dataSource or not
        return (tm != null) ? new BeeJtaDataSource(ds, tm) : ds;
    }
}