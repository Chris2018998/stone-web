package org.stone.springboot;

import org.stone.beeop.BeeObjectPoolMonitorVo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectSourceBeanManager<K, V> {
    private static final ObjectSourceBeanManager single = new ObjectSourceBeanManager();
    private final Map<String, ObjectSourceBean<K, V>> osMap;

    private ObjectSourceBeanManager() {
        this.osMap = new ConcurrentHashMap<>(1);
    }

    public static ObjectSourceBeanManager getInstance() {
        return single;
    }

    public void addObjectSource(ObjectSourceBean<K, V> os) {
        osMap.put(os.getOsId(), os);
    }

    public ObjectSourceBean<K, V> getObjectSource(String osId) {
        return osMap.get(osId);
    }

    public void restartObjectSourcePool(String osId) throws Exception {
        ObjectSourceBean<K, V> os = osMap.get(osId);
        if (os != null) os.clear();
    }

    public Collection<BeeObjectPoolMonitorVo> getOsPoolMonitorVoList() throws Exception {
        List<BeeObjectPoolMonitorVo> poolMonitorVoList = new ArrayList<>(osMap.size());
        Iterator<ObjectSourceBean<K, V>> iterator = osMap.values().iterator();

        while (iterator.hasNext()) {
            ObjectSourceBean<K, V> os = iterator.next();
            BeeObjectPoolMonitorVo vo = os.getPoolMonitorVo();
            if (vo == null) continue;
            if (vo.getPoolState() == 3) {//POOL_CLOSED
                iterator.remove();
            } else {
                poolMonitorVoList.add(vo);
            }
        }
        return poolMonitorVoList;
    }
}
