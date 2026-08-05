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

/**
 * Holder for the {@link HydratedCacheManager} in use by the application. Previously this was an Ehcache 2
 * {@code CacheEventListenerFactory}; with JCache the manager registers itself as an entry listener on the cache
 * regions that hold hydratable entities.
 *
 * @author jfischer
 */
public class HydratedCacheEventListenerFactory {

  private static HydratedCacheManager manager = EhcacheHydratedCacheManagerImpl.getInstance();

  private HydratedCacheEventListenerFactory() {
  }

  /**
   * @return the manager currently configured for hydrated caching
   */
  public static HydratedCacheManager getConfiguredManager() {
    return manager;
  }

  /**
   * Overrides the default {@link EhcacheHydratedCacheManagerImpl} manager.
   */
  public static void setConfiguredManager(HydratedCacheManager hydratedCacheManager) {
    manager = hydratedCacheManager;
  }
}
