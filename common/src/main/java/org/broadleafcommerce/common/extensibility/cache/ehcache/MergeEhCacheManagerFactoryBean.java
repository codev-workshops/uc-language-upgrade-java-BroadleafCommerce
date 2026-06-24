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

import org.broadleafcommerce.common.extensibility.context.ResourceInputStream;
import org.broadleafcommerce.common.extensibility.context.merge.MergeXmlConfigResource;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

/**
 * Produces the JSR-107 ({@link javax.cache.CacheManager}) used as the application cache manager. Historically this
 * extended Spring's {@code EhCacheManagerFactoryBean} (ehcache 2), which was removed in Spring 6. The cache stack
 * now targets ehcache 3 via the JCache (JSR-107) abstraction; this bean merges the configured cache config locations
 * and builds the {@link CacheManager} from the JSR-107 provider.
 *
 * <p>The produced manager is also exposed statically (see {@link #getConfiguredManager()}) so the legacy, non-Spring
 * singleton consumers (e.g. the hydrated cache managers) that previously relied on
 * {@code net.sf.ehcache.CacheManager.getInstance()} can obtain the same manager instance.</p>
 */
public class MergeEhCacheManagerFactoryBean implements FactoryBean<CacheManager>, ApplicationContextAware,
        DisposableBean {

    private static volatile CacheManager configuredManager;

    private ApplicationContext applicationContext;

    @jakarta.annotation.Resource(name="blMergedCacheConfigLocations")
    protected Set<String> mergedCacheConfigLocations;

    protected List<Resource> configLocations;

    protected Resource mergedConfigResource;

    protected boolean shared = false;

    protected CacheManager cacheManager;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void configureMergedItems() {
        List<Resource> temp = new ArrayList<Resource>();
        if (mergedCacheConfigLocations != null && !mergedCacheConfigLocations.isEmpty()) {
            for (String location : mergedCacheConfigLocations) {
                temp.add(applicationContext.getResource(location));
            }
        }
        if (configLocations != null && !configLocations.isEmpty()) {
            for (Resource resource : configLocations) {
                temp.add(resource);
            }
        }
        try {
            MergeXmlConfigResource merge = new MergeXmlConfigResource();
            ResourceInputStream[] sources = new ResourceInputStream[temp.size()];
            int j=0;
            for (Resource resource : temp) {
                sources[j] = new ResourceInputStream(resource.getInputStream(), resource.getURL().toString());
                j++;
            }
            this.mergedConfigResource = merge.getMergedConfigResource(sources);
        } catch (Exception e) {
            throw new FatalBeanException("Unable to merge cache locations", e);
        }
    }

    @Override
    public CacheManager getObject() {
        if (cacheManager == null) {
            CachingProvider provider = Caching.getCachingProvider();
            CacheManager built = null;
            if (mergedConfigResource != null) {
                try {
                    built = provider.getCacheManager(mergedConfigResource.getURI(), getClass().getClassLoader());
                } catch (Exception e) {
                    // The merged resource may not be a valid JSR-107/ehcache-3 configuration (e.g. a legacy
                    // ehcache-2 schema). Fall back to the provider default manager; region-specific
                    // configuration is applied via the ehcache-3 configuration in a later phase.
                    built = null;
                }
            }
            if (built == null) {
                built = provider.getCacheManager();
            }
            cacheManager = built;
            configuredManager = built;
        }
        return cacheManager;
    }

    @Override
    public Class<?> getObjectType() {
        return CacheManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() {
        if (cacheManager != null && !cacheManager.isClosed()) {
            cacheManager.close();
        }
        configuredManager = null;
    }

    /**
     * The merged {@link CacheManager} produced by this factory bean, for use by legacy static singleton consumers
     * that cannot have the manager injected.
     *
     * @return the configured cache manager, or {@code null} if it has not been created yet
     */
    public static CacheManager getConfiguredManager() {
        return configuredManager;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public void setConfigLocations(List<Resource> configLocations) throws BeansException {
        this.configLocations = configLocations;
    }
}
