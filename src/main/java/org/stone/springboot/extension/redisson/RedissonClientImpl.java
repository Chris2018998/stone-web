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
package org.stone.springboot.extension.redisson;

import org.redisson.api.RedissonClient;
import org.stone.springboot.extension.CacheClient;

/**
 * Cache client interface.
 *
 * @author Chris Liao
 */
public class RedissonClientImpl implements CacheClient {

    private final RedissonClient client;

    public RedissonClientImpl(RedissonClient client) {
        this.client = client;
    }

    public String get(String key) {
        return (String) client.getBucket(key).get();
    }

    public void set(String key, String value) {
        client.getBucket(key).set(value);
    }
}
