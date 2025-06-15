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
    //                                     1: Monitor                                                                //
    //***************************************************************************************************************//
    //attribute name define in annotation
    public static final String Annotation_Monitor_Attribute_Name = "runMonitor";
    //configuration prefix of spring bee monitor
    public static final String Config_Monitor_Prefix = "spring.bee.monitor";
    //***************************************************************************************************************//
    //                                     2: virtual Thread                                                         //
    //***************************************************************************************************************//
    //Spring boot virtual threads
    public static final String Config_Virtual_Thread = "spring.threads.virtual.enabled";
    //thread local enable
    public static final String Config_ThreadLocal_Enable = "enableThreadLocal";

    //***************************************************************************************************************//
    //                                     3: Data Source                                                            //
    //***************************************************************************************************************//
    //prefix of spring data source
    public static final String Config_DS_Prefix = "spring.datasource";
    //Data source registered to spring container with this id
    public static final String Config_DS_Id = "dsId";
    //Configuration name of jndi
    public static final String Config_DS_Jndi = "jndiName";
    //data source class name
    public static final String Config_DS_Type = "type";
    //Register to spring Ioc as primary data source
    public static final String Config_DS_Primary = "primary";
    //factory class name to create datasource
    public static final String Config_DS_Factory = "factory";

    //Composited data source registered to spring with this id
    public static final String Config_Dyn_DS_Id = "dynDsId";
    //ID of primary datasource in composited data source
    public static final String Config_Dyn_DS_PrimaryId = "dynDsPrimaryId";

    //***************************************************************************************************************//
    //                                     4: Object Source                                                          //
    //***************************************************************************************************************//
    //prefix of configuration of spring object source
    public static final String Config_OS_Prefix = "spring.objectSource";
    public static final String Config_OS_Id = "osId";
    public static final String Config_OS_Primary = "primary";
    //Configuration name of id of dynamic object source
    public static final String Config_Dyn_OS_Id = "dynOsId";
    //Configuration name of primary id of dynamic object source
    public static final String Config_Dyn_OS_PrimaryId = "dynOsPrimaryId";
}
