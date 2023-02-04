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

import org.stone.beeop.BeeObjectHandle;

/**
 * Combine object Source for Multi-ObjectSource
 *
 * @author Chris Liao
 */
public final class CombineObjectSource {
    private final ThreadLocal<RegisteredObjectSource> osLocal;
    private boolean isClosed = false;

    CombineObjectSource(ThreadLocal<RegisteredObjectSource> osLocal) {
        this.osLocal = osLocal;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() {
        this.isClosed = true;
    }

    public BeeObjectHandle getObject() throws Exception {
        return getCurrentObjectSource().getObject();
    }

    private RegisteredObjectSource getCurrentObjectSource() throws Exception {
        if (isClosed) throw new Exception("ObjectSource has closed");
        RegisteredObjectSource os = osLocal.get();
        if (os == null) throw new Exception("ObjectSource not exists");
        return os;
    }
}
