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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 * @author jfischer
 */
public abstract class AbstractHydratedCacheManager implements HydratedCacheManager, HydratedAnnotationManager,
        CacheEntryRemovedListener<Object, Object>, CacheEntryExpiredListener<Object, Object>,
        CacheEntryUpdatedListener<Object, Object>, Serializable {

    private static final Log LOG = LogFactory.getLog(AbstractHydratedCacheManager.class);

    private Map<String, HydrationDescriptor> hydrationDescriptors = Collections.synchronizedMap(new HashMap(100));

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
            throw new RuntimeException("Broadleaf Commerce Hydrated Cache currently only supports entities with a single @Id annotation.");
        }
        Method[] singleMutators = mutators.values().iterator().next();
        descriptor.setIdMutators(singleMutators);
        String cacheRegion = scanner.getCacheRegion();
        if (cacheRegion == null || "".equals(cacheRegion)) {
            cacheRegion = topEntityClass.getName();
        }
        descriptor.setCacheRegion(cacheRegion);
        hydrationDescriptors.put(entity.getClass().getName(), descriptor);
        return descriptor;
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

    public void dispose() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Disposing of all hydrated cache members");
        }
        hydrationDescriptors.clear();
    }

    /**
     * Invalidate the hydrated cache entries for the given region/key. Implementations supply the
     * concrete storage clean-up.
     */
    protected abstract void removeCache(String cacheRegion, Object key);

    protected abstract void removeAll(String cacheName);

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events) throws CacheEntryListenerException {
        handleEvents(events);
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events) throws CacheEntryListenerException {
        handleEvents(events);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events) throws CacheEntryListenerException {
        handleEvents(events);
    }

    private void handleEvents(Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events) {
        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            removeCache(event.getSource().getName(), event.getKey());
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return this;
    }

}
