/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.common.cache.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.extensibility.cache.ehcache.MergeEhCacheManagerFactoryBean;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author jfischer
 *
 */
public class BigMemoryHydratedCacheManagerImpl extends AbstractHydratedCacheManager {

    private static final Log LOG = LogFactory.getLog(BigMemoryHydratedCacheManagerImpl.class);
    private static final BigMemoryHydratedCacheManagerImpl MANAGER = new BigMemoryHydratedCacheManagerImpl();

    public static BigMemoryHydratedCacheManagerImpl getInstance() {
        return MANAGER;
    }

    private Map<String, List<String>> cacheMemberNamesByEntity = Collections.synchronizedMap(new HashMap<String, List<String>>(100));
    private List<String> removeKeys = Collections.synchronizedList(new ArrayList<String>(100));
    private Cache<Object, Object> offHeap = null;

    private BigMemoryHydratedCacheManagerImpl()  {
        //the cache manager cannot be resolved in this constructor because it may not be initialized yet
    }

    private CacheManager getCacheManager() {
        CacheManager manager = MergeEhCacheManagerFactoryBean.getConfiguredManager();
        if (manager == null) {
            manager = Caching.getCachingProvider().getCacheManager();
        }
        return manager;
    }

    @SuppressWarnings("unchecked")
    private synchronized Cache<Object, Object> getHeap() {
        if (offHeap == null) {
            CacheManager manager = getCacheManager();
            offHeap = manager.getCache("hydrated-offheap-cache");
            if (offHeap == null) {
                offHeap = manager.createCache("hydrated-offheap-cache", new MutableConfiguration<Object, Object>());
            }
        }
        return offHeap;
    }

    @Override
    public Object getHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName) {
        String myKey = cacheRegion + '_' + cacheName + '_' + elementItemName + '_' + elementKey;
        if (removeKeys.contains(myKey)) {
            return null;
        }
        return getHeap().get(myKey);
    }

    @Override
    public void addHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName, Object elementValue) {
        String heapKey = cacheRegion + '_' + cacheName + '_' + elementItemName + '_' + elementKey;
        String nameKey = cacheRegion + '_' + cacheName + '_' + elementKey;
        removeKeys.remove(nameKey);
        if (!cacheMemberNamesByEntity.containsKey(nameKey)) {
            List<String> myMembers = new ArrayList<String>(50);
            myMembers.add(elementItemName);
            cacheMemberNamesByEntity.put(nameKey, myMembers);
        } else {
            List<String> myMembers = cacheMemberNamesByEntity.get(nameKey);
            myMembers.add(elementItemName);
        }
        getHeap().put(heapKey, elementValue);
    }

    protected void removeCache(String cacheRegion, Serializable key) {
        String cacheName = cacheRegion;
        String nameKey = cacheRegion + '_' + cacheName + '_' + key;
        if (cacheMemberNamesByEntity.containsKey(nameKey)) {
            String[] members = new String[cacheMemberNamesByEntity.get(nameKey).size()];
            members = cacheMemberNamesByEntity.get(nameKey).toArray(members);
            for (String myMember : members) {
                String itemKey = cacheRegion + '_' + myMember + '_' + key;
                removeKeys.add(itemKey);
            }
            cacheMemberNamesByEntity.remove(nameKey);
        }
    }
    
    protected void removeAll(String cacheName) {
        //do nothing
    }

}
