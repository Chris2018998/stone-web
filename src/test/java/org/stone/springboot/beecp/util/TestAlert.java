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
package org.stone.springboot.beecp.util;

import org.stone.beecp.BeeMethodExecutionListener;
import org.stone.beecp.BeeMethodExecutionLog;

import java.sql.SQLException;
import java.util.List;

public class TestAlert implements BeeMethodExecutionListener {

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     * @throws SQLException when failure during onMethodStart call
     */
    public void onMethodStart(BeeMethodExecutionLog log) throws SQLException {

    }

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     * @throws SQLException when failure during onMethodEnd call
     */
    public void onMethodEnd(BeeMethodExecutionLog log) throws SQLException {

    }

    /**
     * Handle a list of long-running logs
     *
     * @param logList to be handled
     */
    public void onLongRunningDetected(List<BeeMethodExecutionLog> logList) {

    }
}
