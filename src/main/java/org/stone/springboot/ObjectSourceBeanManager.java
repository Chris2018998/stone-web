package org.stone.springboot;

import org.stone.beeop.BeeObjectPoolMonitorVo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectSourceBeanManager {
    private static final ObjectSourceBeanManager single = new ObjectSourceBeanManager();
    private final Map<String, ObjectSourceBean> osMap;

    private ObjectSourceBeanManager() {
        this.osMap = new ConcurrentHashMap<>(1);
    }

    public static ObjectSourceBeanManager getInstance() {
        return single;
    }

    public void addObjectSource(ObjectSourceBean os) {
        osMap.put(os.getOsId(), os);
    }

    public ObjectSourceBean getObjectSource(String osId) {
        return osMap.get(osId);
    }

    public void restartObjectSourcePool(String osId) throws Exception {
        ObjectSourceBean os = osMap.get(osId);
        if (os != null) os.clear();
    }

    public Collection<BeeObjectPoolMonitorVo> getOsPoolMonitorVoList() throws Exception {
        List<BeeObjectPoolMonitorVo> poolMonitorVoList = new ArrayList<>(osMap.size());
        Iterator<ObjectSourceBean> iterator = osMap.values().iterator();

        while (iterator.hasNext()) {
            ObjectSourceBean os = iterator.next();
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
