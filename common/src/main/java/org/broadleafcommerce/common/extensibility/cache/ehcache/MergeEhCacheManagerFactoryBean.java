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
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Merges multiple cache configuration locations into a single JCache configuration. Under Ehcache 2 this extended
 * Spring's {@code org.springframework.cache.ehcache.EhCacheManagerFactoryBean}; both that factory bean and the
 * Ehcache 2 {@code net.sf.ehcache.CacheManager} were removed when migrating to Ehcache 3 / JCache, so this now
 * extends {@link JCacheManagerFactoryBean} and supplies the merged configuration via its cache manager URI.
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
            setCacheManagerUri(merge.getMergedConfigResource(sources).getURI());
        } catch (Exception e) {
            throw new FatalBeanException("Unable to merge cache locations", e);
        }
    }

    public void setConfigLocations(List<Resource> configLocations) throws BeansException {
        this.configLocations = configLocations;
    }
}
