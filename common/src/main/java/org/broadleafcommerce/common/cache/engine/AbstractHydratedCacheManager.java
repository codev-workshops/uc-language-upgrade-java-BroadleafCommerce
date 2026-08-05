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
import org.broadleafcommerce.common.cache.JCacheUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 * Base hydrated cache manager. Listens to JSR-107 (JCache) entry events on the second level cache regions that
 * contain hydratable entities so that the hydrated members of an entity are evicted alongside the entity itself.
 *
 * @author jfischer
 */
public abstract class AbstractHydratedCacheManager
        implements HydratedCacheManager, HydratedAnnotationManager, CacheEntryRemovedListener<Object, Object>,
        CacheEntryExpiredListener<Object, Object>, CacheEntryUpdatedListener<Object, Object> {

  private static final Log LOG = LogFactory.getLog(AbstractHydratedCacheManager.class);

  private final Map<String, HydrationDescriptor> hydrationDescriptors =
      Collections.synchronizedMap(new HashMap<String, HydrationDescriptor>(100));

  private final Set<String> listeningRegions = Collections.synchronizedSet(new HashSet<String>());

  @Override
  public HydrationDescriptor getHydrationDescriptor(Object entity) {
    if (hydrationDescriptors.containsKey(entity.getClass().getName())) {
      return hydrationDescriptors.get(entity.getClass().getName());
    }
    HydrationDescriptor descriptor = new HydrationDescriptor();
    Class<?> topEntityClass = getTopEntityClass(entity);
    HydrationScanner scanner = new HydrationScanner(topEntityClass, entity.getClass());
    scanner.init();
    descriptor.setHydratedMutators(scanner.getCacheMutators());
    Map<String, Method[]> mutators = scanner.getIdMutators();
    if (mutators.size() != 1) {
      throw new RuntimeException(
          "Broadleaf Commerce Hydrated Cache currently only supports entities with a single @Id annotation.");
    }
    Method[] singleMutators = mutators.values().iterator().next();
    descriptor.setIdMutators(singleMutators);
    String cacheRegion = scanner.getCacheRegion();
    if (cacheRegion == null || "".equals(cacheRegion)) {
      cacheRegion = topEntityClass.getName();
    }
    descriptor.setCacheRegion(cacheRegion);
    hydrationDescriptors.put(entity.getClass().getName(), descriptor);
    registerListener(cacheRegion);
    return descriptor;
  }

  /**
   * Registers this manager as an entry listener on the given cache region so that hydrated members are evicted when
   * the backing entity is removed, updated or expired. Registration happens at most once per region.
   */
  protected void registerListener(String cacheRegion) {
    if (!listeningRegions.add(cacheRegion)) {
      return;
    }
    try {
      Cache<Object, Object> cache = JCacheUtil.getCache(cacheRegion);
      CacheEntryListener<Object, Object> listener = this;
      cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<Object, Object>(
          new FactoryBuilder.SingletonFactory<CacheEntryListener<Object, Object>>(listener), null, false, false));
    } catch (RuntimeException e) {
      listeningRegions.remove(cacheRegion);
      LOG.warn("Unable to register the hydrated cache listener on region " + cacheRegion, e);
    }
  }

  protected Class<?> getTopEntityClass(Object entity) {
    Class<?> myClass = entity.getClass();
    Class<?> superClass = entity.getClass().getSuperclass();
    while (superClass != null && superClass.getName().startsWith("org.broadleaf")) {
      myClass = superClass;
      superClass = superClass.getSuperclass();
    }
    return myClass;
  }

  @Override
  public void onRemoved(Iterable<CacheEntryEvent<?, ?>> events) {
    evict(events);
  }

  @Override
  public void onExpired(Iterable<CacheEntryEvent<?, ?>> events) {
    evict(events);
  }

  @Override
  public void onUpdated(Iterable<CacheEntryEvent<?, ?>> events) {
    evict(events);
  }

  private void evict(Iterable<CacheEntryEvent<?, ?>> events) {
    for (CacheEntryEvent<?, ?> event : events) {
      removeCache(event.getSource().getName(), event.getKey());
    }
  }

  /**
   * Removes all hydrated members associated with the given second level cache entry.
   */
  protected abstract void removeCache(String cacheRegion, Object key);

  public void dispose() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Disposing of all hydrated cache members");
    }
    hydrationDescriptors.clear();
  }
}
