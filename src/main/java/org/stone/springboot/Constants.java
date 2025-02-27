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

/**
 * Constants definition
 *
 * @author Chris Liao
 */
public class Constants {

    //***************************************************************************************************************//
    //                                     1: Data Source                                                            //
    //***************************************************************************************************************//
    //prefix of configuration of spring data source
    public static final String Config_DS_Prefix = "spring.datasource";
    //Configuration name of a data source id
    public static final String Config_DS_Id = "dsId";
    //Configuration name of id of dynamic data source
    public static final String Config_Dyn_DS_Id = "dynDsId";
    //Configuration name of primary id of dynamic data source
    public static final String Config_Dyn_DS_PrimaryId = "dynDsPrimaryId";
    //Configuration name of primary value
    public static final String Config_DS_Primary = "primary";
    //Configuration name of datasource class type
    public static final String Config_DS_Type = "type";
    //Configuration name of jndi name of datasource
    public static final String Config_DS_Jndi = "jndiName";

    //***************************************************************************************************************//
    //                                     2: Object Source                                                          //
    //***************************************************************************************************************//
}
