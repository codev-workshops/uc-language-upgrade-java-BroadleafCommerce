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
 * Resolves the {@link HydratedCacheManager} used by {@link org.broadleafcommerce.common.cache.HydratedSetup}.
 *
 * <p>Historically this extended ehcache-2's {@code CacheEventListenerFactory} so the hydrated-cache manager could be
 * wired in as an ehcache-2 cache event listener. ehcache-2's listener factory SPI no longer exists on the Hibernate 6 /
 * JCache stack, so this is now a plain factory that simply resolves the configured manager. Re-binding hydrated-cache
 * invalidation to the Hibernate 6 / JCache event model is handled in a later phase.
 *
 * @author jfischer
 */
public class HydratedCacheEventListenerFactory {

    private static HydratedCacheManager manager = null;

    public HydratedCacheManager createCacheEventListener(Properties props) {
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
            throw new RuntimeException("Unable to create a HydratedCacheManager instance", e);
        }
        return manager;
    }

    public static HydratedCacheManager getConfiguredManager() {
        if (manager == null) {
            manager = EhcacheHydratedCacheManagerImpl.getInstance();
        }
        return manager;
    }
}
