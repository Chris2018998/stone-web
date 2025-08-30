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
package org.stone.springboot.controller;

/**
 * an interface to push runtime info of cp,op to remote server(for example：Redis server,prometheus gateWay server
 *
 * @author Chris Liao
 */

public interface PoolSnapshotPusher {

    /**
     * push snapshot of pools to remote server
     *
     * @param snapshot is runtime monitor object
     * @throws Exception when push fail
     */
    void push(PoolSnapshot snapshot) throws Exception;

}
