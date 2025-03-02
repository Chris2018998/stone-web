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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Global Scheduled service to run tasks.(Only use in stone-web project)
 *
 * @author Chris Liao
 */
public class InternalScheduledService {
    private static final InternalScheduledService single = new InternalScheduledService();
    private final ScheduledThreadPoolExecutor timerExecutor;

    private InternalScheduledService() {
        this.timerExecutor = new ScheduledThreadPoolExecutor(2,
                new SpringBootDsThreadFactory());
        timerExecutor.setKeepAliveTime(15, TimeUnit.SECONDS);
        timerExecutor.allowCoreThreadTimeOut(true);
    }

    public static InternalScheduledService getInstance() {
        return single;
    }

    public void scheduleAtFixedRate(Runnable command,
                                    long initialDelay,
                                    long period,
                                    TimeUnit unit) {
        timerExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    private static final class SpringBootDsThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, "StoneThreadFactory");
            th.setDaemon(true);
            return th;
        }
    }
}
