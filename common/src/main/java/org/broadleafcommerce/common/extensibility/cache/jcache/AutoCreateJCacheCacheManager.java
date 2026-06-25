/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
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
package org.broadleafcommerce.common.extensibility.cache.jcache;

import org.springframework.cache.Cache;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.cache.jcache.JCacheCacheManager;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

/**
 * {@link JCacheCacheManager} that creates cache regions on demand. Spring's stock {@code JCacheCacheManager} only
 * exposes caches already present in the underlying {@link CacheManager} and returns {@code null} for unknown names.
 * Ehcache 2 (the previous implementation) instead created missing caches dynamically from its {@code defaultCache}
 * template. This subclass restores that behaviour for the JCache provider so callers such as the Broadleaf resource
 * caching resolver/transformer can obtain a cache without it being pre-declared.
 */
public class AutoCreateJCacheCacheManager extends JCacheCacheManager {

    public AutoCreateJCacheCacheManager() {
        super();
    }

    public AutoCreateJCacheCacheManager(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Cache getMissingCache(String name) {
        Cache cache = super.getMissingCache(name);
        if (cache == null) {
            CacheManager cacheManager = getCacheManager();
            javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(name);
            if (jcache == null) {
                MutableConfiguration<Object, Object> configuration =
                        new MutableConfiguration<Object, Object>().setStoreByValue(false);
                jcache = cacheManager.createCache(name, configuration);
            }
            cache = new JCacheCache(jcache, isAllowNullValues());
        }
        return cache;
    }
}
