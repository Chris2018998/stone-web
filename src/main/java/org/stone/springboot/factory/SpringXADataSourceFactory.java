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
import org.stone.springboot.exception.XADataSourceException;

import javax.sql.XADataSource;

/*
 * XAData source factory interface.
 *
 *  @author Chris Liao
 */
public interface SpringXADataSourceFactory {

    /**
     * Create a Xa datasource with configuration in spring boot.
     *
     * @param environment SpringBoot environment
     * @param dsId        configured data source id
     * @param prefix      configured prefix name
     * @return data source instance
     * @throws XADataSourceException when fail to set
     */
    XADataSource createXADataSource(String prefix, String dsId, Environment environment) throws XADataSourceException;

}
