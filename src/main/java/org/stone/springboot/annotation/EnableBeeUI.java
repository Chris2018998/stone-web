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
package org.stone.springboot.annotation;

import org.springframework.context.annotation.Import;
import org.stone.springboot.monitor.WebUiControllerRegister;

import java.lang.annotation.*;

/*
 * Annotation to enable web UI to show monitoring info of pools
 *
 *  @author Chris Liao
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Import(value = {WebUiControllerRegister.class})
public @interface EnableBeeUI {
}

