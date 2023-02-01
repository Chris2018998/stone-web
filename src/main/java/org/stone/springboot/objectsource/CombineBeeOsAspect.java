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
package org.stone.springboot.objectsource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.stone.springboot.annotation.BeeDsId;

import static org.stone.util.CommonUtil.isBlank;

/*
 *  combine ObjectSource Aspect
 *
 *  @author Chris Liao
 */

@Aspect
@Order(1)
public class CombineBeeOsAspect {
    private final String primaryDsId;
    private final ThreadLocal<SpringBootBeeOs> osThreadLocal;

    CombineBeeOsAspect(String primaryDsId, ThreadLocal<SpringBootBeeOs> osThreadLocal) {
        this.primaryDsId = primaryDsId;
        this.osThreadLocal = osThreadLocal;
    }

    @Pointcut("@annotation(org.stone.springboot.annotation.BeeOsId)")
    public void pointcut() {
        //do nothing
    }

    @Around("pointcut()")
    public Object setDataSourceId(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        BeeDsId annotation = methodSignature.getMethod().getAnnotation(BeeDsId.class);
        String osId = annotation.value();

        try {
            if (isBlank(osId)) osId = primaryDsId;
            //osThreadLocal.set(SpringBootDataSourceManager.getInstance().getSpringBootDataSource(dsId));
            return joinPoint.proceed();
        } finally {
            if (!isBlank(osId)) osThreadLocal.remove();
        }
    }
}
