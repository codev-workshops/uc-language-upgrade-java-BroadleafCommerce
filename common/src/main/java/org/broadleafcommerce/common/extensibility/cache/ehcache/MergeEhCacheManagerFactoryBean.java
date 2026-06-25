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
package org.broadleafcommerce.common.extensibility.cache.ehcache;

import org.springframework.beans.BeansException;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Set;

/**
 * Produces the JCache {@link javax.cache.CacheManager} used by Broadleaf. Under Ehcache 2 this extended Spring's
 * {@code org.springframework.cache.ehcache.EhCacheManagerFactoryBean} and merged a set of Ehcache 2 XML config files
 * into a single {@code net.sf.ehcache.CacheManager}. Both that factory bean and the Ehcache 2 config schema were
 * removed when migrating to Ehcache 3 / JCache, and the legacy {@code bl-*-ehcache.xml} files are not valid Ehcache 3
 * configuration. This now extends {@link JCacheManagerFactoryBean} and supplies the JCache provider's default
 * {@code CacheManager}; individual cache regions are created on demand (see {@code AbstractCacheMissAware},
 * {@code AbstractHydratedCacheManager} and the auto-creating {@code blSpringCacheManager}), mirroring the Ehcache 2
 * {@code defaultCache} dynamic-creation behaviour.
 */
public class MergeEhCacheManagerFactoryBean extends JCacheManagerFactoryBean implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @jakarta.annotation.Resource(name="blMergedCacheConfigLocations")
    protected Set<String> mergedCacheConfigLocations;

    protected List<Resource> configLocations;

    public void setConfigLocations(List<Resource> configLocations) throws BeansException {
        this.configLocations = configLocations;
    }
}
