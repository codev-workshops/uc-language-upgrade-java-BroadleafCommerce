/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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

package org.thymeleaf.templatewriter;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * In Thymeleaf 3, template writers no longer exist as a separate concept. The rendering is
 * built into the engine. This class is retained for backward compatibility and provides
 * a simple cache wrapper for template element content.
 * 
 * @author Andre Azzolini (apazzolini), Brian Polster (bpolster)
 */
public class CacheAwareGeneralTemplateWriter {

    protected static final Log LOG = LogFactory.getLog(CacheAwareGeneralTemplateWriter.class);

    protected Cache cache;

    public String getCachedContent(String cacheKey) {
        Cache c = getCache();
        if (c != null) {
            Element element = c.get(cacheKey);
            if (element != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Read template from cache - " + cacheKey);
                }
                return (String) element.getObjectValue();
            }
        }
        return null;
    }

    public void putCachedContent(String cacheKey, String content) {
        Cache c = getCache();
        if (c != null) {
            Element element = new Element(cacheKey, content);
            c.put(element);
        }
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Cache getCache() {
        if (cache == null) {
            cache = CacheManager.getInstance().getCache("blTemplateElements");
        }
        return cache;
    }
}
