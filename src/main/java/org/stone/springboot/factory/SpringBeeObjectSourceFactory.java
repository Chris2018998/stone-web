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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.springboot.ObjectSourceBeanManager;
import org.stone.springboot.exception.ObjectSourceException;

import static org.stone.beecp.pool.ConnectionPoolStatics.CONFIG_CONFIG_PRINT_EXCLUSION_LIST;
import static org.stone.beeop.pool.ObjectPoolStatics.*;
import static org.stone.springboot.Constants.Config_ThreadLocal_Enable;
import static org.stone.springboot.Constants.Config_Virtual_Thread;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * Bee Object Source factory
 *
 *  @author Chris liao
 */
public class SpringBeeObjectSourceFactory<K, V> {
    private final ObjectSourceBeanManager<K, V> osManager = ObjectSourceBeanManager.getInstance();

    private void setFactoryPropertiesConfig(BeeObjectSourceConfig<K, V> config, String prefix, Environment environment) {
        config.addFactoryProperty(osManager.getConfigValue(prefix, CONFIG_FACTORY_PROP, environment));
        String factoryPropertiesCount = osManager.getConfigValue(prefix, CONFIG_FACTORY_PROP_SIZE, environment);
        if (isNotBlank(factoryPropertiesCount)) {
            int count = Integer.parseInt(factoryPropertiesCount.trim());
            for (int i = 1; i <= count; i++)
                config.addFactoryProperty(osManager.getConfigValue(prefix, CONFIG_FACTORY_PROP_KEY_PREFIX + i, environment));
        }
    }

    private void setConfigPrintExclusionList(BeeObjectSourceConfig<K, V> config, String osPrefix, Environment environment) {
        String exclusionListText = osManager.getConfigValue(osPrefix, CONFIG_CONFIG_PRINT_EXCLUSION_LIST, environment);
        if (isNotBlank(exclusionListText)) {
            config.clearAllConfigPrintExclusion();//remove existed exclusion
            for (String exclusion : exclusionListText.trim().split(",")) {
                config.addConfigPrintExclusion(exclusion);
            }
        }
    }

    public BeeObjectSource<K, V> createDataSource(String osPrefix, String osId, Environment environment) throws ObjectSourceException {
        BeeObjectSourceConfig<K, V> config = new BeeObjectSourceConfig<>();
        this.setFactoryPropertiesConfig(config, osPrefix, environment);
        setFactoryPropertiesConfig(config, osPrefix, environment);
        setConfigPrintExclusionList(config, osPrefix, environment);

        String threadLocalEnable = osManager.getConfigValue(osPrefix, Config_ThreadLocal_Enable, environment);
        if (threadLocalEnable == null) {
            boolean enableVirtualThread = Boolean.parseBoolean(environment.getProperty(Config_Virtual_Thread, "false"));
            config.setEnableThreadLocal(!enableVirtualThread);
        }

        return new BeeObjectSource<>(config);
    }
}
