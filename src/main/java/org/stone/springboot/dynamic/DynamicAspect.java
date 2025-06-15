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
package org.stone.springboot.dynamic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.stone.springboot.DataSourceBean;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.ObjectSourceBean;
import org.stone.springboot.ObjectSourceBeanManager;
import org.stone.springboot.annotation.BeeDsId;
import org.stone.springboot.annotation.BeeOsId;
import org.stone.springboot.exception.DataSourceException;
import org.stone.springboot.exception.ObjectSourceException;

import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/*
 * dynamic Aspect
 *
 * @author Chris Liao
 */

@Aspect
public final class DynamicAspect<K, V> {
    private String primaryDsId;
    private ThreadLocal<DataSourceBean> dsLocal;

    private String primaryOsId;
    private ThreadLocal<ObjectSourceBean<K, V>> osLocal;

    //***************************************************************************************************************//
    //                                     1: properties set(3)                                                      //
    //***************************************************************************************************************/
    public void setDynDsThreadLocal(String primaryDsId, ThreadLocal<DataSourceBean> dsLocal) {
        this.dsLocal = dsLocal;
        this.primaryDsId = primaryDsId;
    }

    public void setDynOsThreadLocal(String primaryOsId, ThreadLocal<ObjectSourceBean<K, V>> osLocal) {
        this.osLocal = osLocal;
        this.primaryOsId = primaryOsId;
    }

    //***************************************************************************************************************//
    //                                     2: dataSource Aspect                                                      //
    //***************************************************************************************************************/
    @Around("@annotation(org.stone.springboot.annotation.BeeDsId)")
    public Object setDataSourceId(ProceedingJoinPoint joinPoint) throws Throwable {
        if (dsLocal == null) throw new DataSourceException("Dynamic datasource not be enable");
        if (isBlank(primaryDsId)) throw new DataSourceException("Dynamic primary datasource id not set");

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        BeeDsId annotation = methodSignature.getMethod().getAnnotation(BeeDsId.class);
        String dsId = annotation.value();

        try {
            if (isBlank(dsId)) dsId = primaryDsId;
            dsLocal.set(DataSourceBeanManager.getInstance().getDataSource(dsId));
            return joinPoint.proceed();
        } finally {
            if (isNotBlank(dsId)) dsLocal.remove();
        }
    }

    //***************************************************************************************************************//
    //                                     3: objectSource Aspect                                                    //
    //***************************************************************************************************************/
    @Around("@annotation(org.stone.springboot.annotation.BeeOsId)")
    public Object setObjectSourceId(ProceedingJoinPoint joinPoint) throws Throwable {
        if (osLocal == null) throw new ObjectSourceException("Dynamic object-source not be enable");
        if (isBlank(primaryOsId)) throw new ObjectSourceException("Dynamic primary object-source id not set");

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        BeeOsId annotation = methodSignature.getMethod().getAnnotation(BeeOsId.class);
        String osId = annotation.value();

        try {
            if (isBlank(osId)) osId = primaryOsId;
            osLocal.set(ObjectSourceBeanManager.getInstance().getObjectSource(osId));
            return joinPoint.proceed();
        } finally {
            if (isNotBlank(osId)) osLocal.remove();
        }
    }
}
