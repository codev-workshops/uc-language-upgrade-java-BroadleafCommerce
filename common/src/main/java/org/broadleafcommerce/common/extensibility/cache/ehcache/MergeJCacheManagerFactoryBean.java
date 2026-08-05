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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;

/**
 * Creates the JSR-107 {@code CacheManager} from the merged set of Ehcache 3 configuration files contributed by the
 * Broadleaf modules and the implementing application.
 */
public class MergeJCacheManagerFactoryBean extends JCacheManagerFactoryBean implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  @jakarta.annotation.Resource(name = "blMergedCacheConfigLocations")
  protected Set<String> mergedCacheConfigLocations;

  protected List<Resource> configLocations;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Merges all of the configured cache configuration documents into a single configuration document and points the
   * JCache provider at it.
   */
  @PostConstruct
  public void configureMergedItems() {
    List<Resource> temp = new ArrayList<Resource>();
    if (mergedCacheConfigLocations != null && !mergedCacheConfigLocations.isEmpty()) {
      for (String location : mergedCacheConfigLocations) {
        temp.add(applicationContext.getResource(location));
      }
    }
    if (configLocations != null && !configLocations.isEmpty()) {
      temp.addAll(configLocations);
    }
    try {
      MergeXmlConfigResource merge = new MergeXmlConfigResource();
      ResourceInputStream[] sources = new ResourceInputStream[temp.size()];
      int j = 0;
      for (Resource resource : temp) {
        sources[j] = new ResourceInputStream(resource.getInputStream(), resource.getURL().toString());
        j++;
      }
      Resource merged = merge.getMergedConfigResource(sources);
      setCacheManagerUri(writeToTempFile(merged).toURI());
    } catch (Exception e) {
      throw new FatalBeanException("Unable to merge cache locations", e);
    }
  }

  private File writeToTempFile(Resource merged) throws Exception {
    File file = File.createTempFile("blc-merged-jcache", ".xml");
    file.deleteOnExit();
    InputStream is = merged.getInputStream();
    try {
      OutputStream os = new FileOutputStream(file);
      try {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
          os.write(buffer, 0, read);
        }
      } finally {
        os.close();
      }
    } finally {
      is.close();
    }
    return file;
  }

  public void setConfigLocations(List<Resource> configLocations) throws BeansException {
    this.configLocations = configLocations;
  }
}
