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
package org.stone.springboot.extension;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * Default implementation of local json util
 *
 * @author Chris Liao
 */
public class JackSonUtil {
    private static final ObjectMapper JacksonObjectMapper;

    static {
        JacksonObjectMapper = new ObjectMapper();
        JacksonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        JacksonObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static String object2String(Object obj) throws IOException {
        return JacksonObjectMapper.writeValueAsString(obj);
    }

    public static <T> T string2Object(String str, Class<T> clazz) throws IOException {
        return JacksonObjectMapper.readerFor(clazz).readValue(str);
    }
}
