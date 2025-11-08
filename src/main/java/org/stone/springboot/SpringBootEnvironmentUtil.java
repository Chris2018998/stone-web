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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.stone.springboot.exception.DataSourceException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static org.stone.tools.BeanUtil.*;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * configuration loader
 *
 * @author Chris Liao
 */
public class SpringBootEnvironmentUtil {
    //Logger
    private static final Logger log = LoggerFactory.getLogger(SpringBootEnvironmentUtil.class);

    public static Supplier<?> createSpringSupplier(Object bean) {
        return new SpringRegSupplier(bean);
    }

    public static boolean existsBeanDefinition(String beanName, BeanDefinitionRegistry registry) {
        return registry.containsBeanDefinition(beanName);
    }

    public static String getConfigValue(String prefix, final String propertyName, Environment environment) {
        String value = readConfig(environment, prefix + "." + propertyName);
        if (value != null) return value;

        String newPropertyName = propertyName.substring(0, 1).toLowerCase(Locale.US) + propertyName.substring(1);
        value = readConfig(environment, prefix + "." + newPropertyName);
        if (value != null) return value;

        value = readConfig(environment, prefix + "." + propertyNameToFieldId(newPropertyName, Separator_MiddleLine));
        if (value != null) return value;

        return readConfig(environment, prefix + "." + propertyNameToFieldId(newPropertyName, Separator_UnderLine));
    }

    private static String readConfig(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (isNotBlank(value)) {
            value = value.trim();
            log.info("{}={}", key, value);
        }
        return value;
    }

    public static Object createDataSourceByClassName(String dsId, Class<?> dsClass) {
        try {
            return dsClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new DataSourceException("DataSource(" + dsId + ")-Failed to instantiated the class:" + dsClass.getName(), e);
        }
    }

    public static void setConfigPropertiesValue(Object bean, String prefix, String id, Environment environment) throws DataSourceException {
        try {
            //1:get all set methods
            Map<String, Method> setMethodMap = getClassSetMethodMap(bean.getClass());
            //2:create map to collect config value
            Map<String, Object> setValueMap = new HashMap<>(setMethodMap.size());
            //3:loop to find out properties config value by set methods
            for (String propertyName : setMethodMap.keySet()) {
                String configVal = getConfigValue(prefix, propertyName, environment);
                if (isBlank(configVal)) continue;
                setValueMap.put(propertyName, configVal.trim());
            }

            //4:inject found config value to ds config object
            setPropertiesValue(bean, setMethodMap, setValueMap);
        } catch (Throwable e) {
            throw new DataSourceException("DataSource(" + id + ")-Failed to set properties", e);
        }
    }

    private record SpringRegSupplier(Object ds) implements Supplier<Object> {
        public Object get() {
            return ds;
        }
    }
}
