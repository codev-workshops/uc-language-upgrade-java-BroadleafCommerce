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

import org.broadleafcommerce.common.cache.JCacheUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;

/**
 * Hydrated cache manager backed by a JSR-107 (JCache) region named {@code hydrated-cache}.
 *
 * @author jfischer
 */
public class EhcacheHydratedCacheManagerImpl extends AbstractHydratedCacheManager {

  private static final String HYDRATED_CACHE_NAME = "hydrated-cache";
  private static final EhcacheHydratedCacheManagerImpl MANAGER = new EhcacheHydratedCacheManagerImpl();

  public static EhcacheHydratedCacheManagerImpl getInstance() {
    return MANAGER;
  }

  private final Map<String, List<String>> cacheMembersByEntity =
      Collections.synchronizedMap(new HashMap<String, List<String>>(100));

  private Cache<Object, Object> heap;

  private EhcacheHydratedCacheManagerImpl() {
  }

  private synchronized Cache<Object, Object> getHeap() {
    if (heap == null) {
      heap = JCacheUtil.getCache(HYDRATED_CACHE_NAME);
    }
    return heap;
  }

  @Override
  public Object getHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey,
      String elementItemName) {
    return getHeap().get(buildItemKey(cacheRegion, cacheName, elementItemName, elementKey));
  }

  @Override
  public void addHydratedCacheElementItem(String cacheRegion, String cacheName, Serializable elementKey,
      String elementItemName, Object elementValue) {
    String nameKey = buildEntityKey(cacheRegion, cacheName, elementKey);
    List<String> myMembers = cacheMembersByEntity.get(nameKey);
    if (myMembers == null) {
      myMembers = Collections.synchronizedList(new ArrayList<String>(50));
      cacheMembersByEntity.put(nameKey, myMembers);
    }
    myMembers.add(elementItemName);
    getHeap().put(buildItemKey(cacheRegion, cacheName, elementItemName, elementKey), elementValue);
  }

  @Override
  protected void removeCache(String cacheRegion, Object key) {
    String nameKey = buildEntityKey(cacheRegion, cacheRegion, key);
    List<String> members = cacheMembersByEntity.remove(nameKey);
    if (members != null) {
      for (String member : new ArrayList<String>(members)) {
        getHeap().remove(buildItemKey(cacheRegion, cacheRegion, member, key));
      }
    }
  }

  private String buildEntityKey(String cacheRegion, String cacheName, Object key) {
    return cacheRegion + '_' + cacheName + '_' + key;
  }

  private String buildItemKey(String cacheRegion, String cacheName, String elementItemName, Object key) {
    return cacheRegion + '_' + cacheName + '_' + elementItemName + '_' + key;
  }
}
