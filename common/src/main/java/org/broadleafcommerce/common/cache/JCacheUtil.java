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
package org.broadleafcommerce.common.cache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

/**
 * Utility for obtaining JSR-107 (JCache) caches. Replaces the direct usage of the Ehcache 2
 * {@code net.sf.ehcache.CacheManager} singleton, which no longer exists in Ehcache 3.
 *
 * <p>Caches that are not declared in the JCache provider's configuration are created on demand with a default
 * configuration so that lookups behave like the previous Ehcache 2 based implementation.
 */
public class JCacheUtil {

    private JCacheUtil() {
    }

    /**
     * @return the default JCache {@link CacheManager} for the current classloader
     */
    public static CacheManager getCacheManager() {
        return Caching.getCachingProvider().getCacheManager();
    }

    /**
     * Looks up the named cache, creating it with a default configuration if the provider does not already know it.
     */
    public static Cache<Object, Object> getCache(String cacheName) {
        CacheManager cacheManager = getCacheManager();
        Cache<Object, Object> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
        if (cache == null) {
            MutableConfiguration<Object, Object> configuration = new MutableConfiguration<Object, Object>()
                    .setTypes(Object.class, Object.class)
                    .setStoreByValue(false);
            try {
                cache = cacheManager.createCache(cacheName, configuration);
            } catch (javax.cache.CacheException e) {
                // another thread may have created the cache concurrently
                cache = cacheManager.getCache(cacheName, Object.class, Object.class);
                if (cache == null) {
                    throw e;
                }
            }
        }
        return cache;
    }
}
