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
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.stone.springboot.DataSourceBean;
import org.stone.springboot.DataSourceBeanManager;
import org.stone.springboot.ObjectSourceBean;
import org.stone.springboot.ObjectSourceBeanManager;
import org.stone.springboot.annotation.BeeDsId;
import org.stone.springboot.annotation.BeeOsId;
import org.stone.springboot.factory.SpringDataSourceException;

import static org.stone.tools.CommonUtil.isBlank;

/*
 * combine Aspect
 *
 * @author Chris Liao
 */

@Aspect
@Order(1)
public final class DynamicAspect {
    private final String primaryDsId;
    private final ThreadLocal<DataSourceBean> dsLocal;
    private String primaryOsId;
    private ThreadLocal<ObjectSourceBean<?, ?>> osLocal;

    //***************************************************************************************************************//
    //                                     1: properties set(3)                                                      //
    //***************************************************************************************************************/
    public DynamicAspect(String primaryDsId, ThreadLocal<DataSourceBean> dsLocal) {
        this.dsLocal = dsLocal;
        this.primaryDsId = primaryDsId;
    }

    public void setOsThreadLocal(String primaryOsId, ThreadLocal<ObjectSourceBean<?, ?>> osLocal) {
        this.osLocal = osLocal;
        this.primaryOsId = primaryOsId;
    }

    //***************************************************************************************************************//
    //                                     2: dataSource Aspect                                                      //
    //***************************************************************************************************************/
    @Pointcut("@annotation(org.stone.springboot.annotation.BeeDsId)")
    public void dsPointcut() {
        //do nothing
    }

    @Around("dsPointcut()")
    public Object setDataSourceId(ProceedingJoinPoint joinPoint) throws Throwable {
        if (dsLocal == null) throw new SpringDataSourceException("Combine datasource not be enable");
        if (isBlank(primaryDsId)) throw new SpringDataSourceException("Combine primary datasource id not set");

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        BeeDsId annotation = methodSignature.getMethod().getAnnotation(BeeDsId.class);
        String dsId = annotation.value();

        try {
            if (isBlank(dsId)) dsId = primaryDsId;
            dsLocal.set(DataSourceBeanManager.getInstance().getDataSource(dsId));
            return joinPoint.proceed();
        } finally {
            if (!isBlank(dsId)) dsLocal.remove();
        }
    }

    //***************************************************************************************************************//
    //                                     3: objectSource Aspect                                                    //
    //***************************************************************************************************************/
    @Pointcut("@annotation(org.stone.springboot.annotation.BeeOsId)")
    public void osPointcut() {
        //do nothing
    }

    @Around("osPointcut()")
    public Object setObjectSourceId(ProceedingJoinPoint joinPoint) throws Throwable {
        if (dsLocal == null) throw new SpringDataSourceException("Combine object-source not be enable");
        if (isBlank(primaryDsId)) throw new SpringDataSourceException("Combine primary object-source id not set");

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        BeeOsId annotation = methodSignature.getMethod().getAnnotation(BeeOsId.class);
        String osId = annotation.value();

        try {
            if (isBlank(osId)) osId = primaryOsId;
            osLocal.set(ObjectSourceBeanManager.getInstance().getObjectSource(osId));
            return joinPoint.proceed();
        } finally {
            if (!isBlank(osId)) osLocal.remove();
        }
    }
}
