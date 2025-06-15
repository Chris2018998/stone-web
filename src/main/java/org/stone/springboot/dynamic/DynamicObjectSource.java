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

import org.stone.beeop.BeeObjectHandle;
import org.stone.springboot.ObjectSourceBean;

/**
 * Combine object Source(only support beeop)
 *
 * @author Chris Liao
 */
public final class DynamicObjectSource<K, V> extends ObjectSourceBean<K, V> {
    private final ThreadLocal<ObjectSourceBean<K, V>> osLocal;
    private boolean isClosed = false;

    public DynamicObjectSource(String osId, ThreadLocal<ObjectSourceBean<K, V>> osLocal) {
        super(osId, false, null);
        this.osLocal = osLocal;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() {
        this.isClosed = true;
    }

    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        return getCurrentObjectSource().getObjectHandle();
    }

    private ObjectSourceBean<K, V> getCurrentObjectSource() throws Exception {
        if (isClosed) throw new Exception("ObjectSource has closed");
        ObjectSourceBean<K, V> os = osLocal.get();
        if (os == null) throw new Exception("ObjectSource not exists");
        return os;
    }
}
