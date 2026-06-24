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

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Resolves the {@link HydratedCacheManager} for the hydrated-cache subsystem. Under Ehcache 2 this was an
 * {@code net.sf.ehcache.event.CacheEventListenerFactory} wired through {@code ehcache.xml}; with the move to
 * Ehcache 3 / JCache the manager is itself a {@code javax.cache.event} listener and is configured
 * programmatically (defaulting to {@link EhcacheHydratedCacheManagerImpl}).
 *
 * @author jfischer
 *
 */
public class HydratedCacheEventListenerFactory {

    private static HydratedCacheManager manager = EhcacheHydratedCacheManagerImpl.getInstance();

    public static HydratedCacheManager createCacheEventListener(Properties props) {
        try {
            if (props == null || props.isEmpty()) {
                manager = EhcacheHydratedCacheManagerImpl.getInstance();
            } else {
                String managerClass = props.getProperty("managerClass");
                Class<?> clazz = Class.forName(managerClass);
                Method method = clazz.getDeclaredMethod("getInstance");
                manager = (HydratedCacheManager) method.invoke(null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a CacheEventListener instance", e);
        }
        return manager;
    }

    public static void setConfiguredManager(HydratedCacheManager configuredManager) {
        manager = configuredManager;
    }

    public static HydratedCacheManager getConfiguredManager() {
        return manager;
    }
}
