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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

/**
 * 
 * @author jfischer
 *
 */
public class EhcacheHydratedCacheManagerImpl extends AbstractHydratedCacheManager {

    private static final Log LOG = LogFactory.getLog(EhcacheHydratedCacheManagerImpl.class);
    private static final EhcacheHydratedCacheManagerImpl MANAGER = new EhcacheHydratedCacheManagerImpl();

    public static EhcacheHydratedCacheManagerImpl getInstance() {
        return MANAGER;
    }

    private Map<String, List<String>> cacheMembersByEntity = Collections.synchronizedMap(new HashMap<String, List<String>>(100));
    private Cache<String, Object> heap = null;

    private EhcacheHydratedCacheManagerImpl()  {
        //the JCache CacheManager is resolved lazily so that the JCache provider is fully started first
    }
    
    private synchronized Cache<String, Object> getHeap() {
        if (heap == null) {
            CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
            Cache<String, Object> existing = cacheManager.getCache("hydrated-cache", String.class, Object.class);
            if (existing != null) {
                heap = existing;
            } else {
                MutableConfiguration<String, Object> config = new MutableConfiguration<String, Object>()
                        .setTypes(String.class, Object.class)
                        .setStoreByValue(false);
                heap = cacheManager.createCache("hydrated-cache", config);
            }
        }
        return heap;
    }

    @Override
    public Object getHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName) {
        String myKey = cacheRegion + '_' + cacheName + '_' + elementItemName + '_' + elementKey;
        return getHeap().get(myKey);
    }

    @Override
    public void addHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey, String elementItemName, Object elementValue) {
        String heapKey = cacheRegion + '_' + cacheName + '_' + elementItemName + '_' + elementKey;
        String nameKey = cacheRegion + '_' + cacheName + '_' + elementKey;
        if (!cacheMembersByEntity.containsKey(nameKey)) {
            List<String> myMembers = new ArrayList<String>(50);
            myMembers.add(elementItemName);
            cacheMembersByEntity.put(nameKey, myMembers);
        } else {
            List<String> myMembers = cacheMembersByEntity.get(nameKey);
            myMembers.add(elementItemName);
        }
        getHeap().put(heapKey, elementValue);
    }

    @Override
    protected void removeCache(String cacheRegion, Object key) {
        String cacheName = cacheRegion;
        String nameKey = cacheRegion + '_' + cacheName + '_' + key;
        if (cacheMembersByEntity.containsKey(nameKey)) {
            String[] members = new String[cacheMembersByEntity.get(nameKey).size()];
            members = cacheMembersByEntity.get(nameKey).toArray(members);
            for (String myMember : members) {
                String itemKey = cacheRegion + '_' + cacheName + '_' + myMember + '_' + key;
                getHeap().remove(itemKey);
            }
            cacheMembersByEntity.remove(nameKey);
        }
    }
    
    @Override
    protected void removeAll(String cacheName) {
        //do nothing
    }

}
