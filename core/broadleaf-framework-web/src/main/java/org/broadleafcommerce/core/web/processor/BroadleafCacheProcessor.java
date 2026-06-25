/*
 * #%L
 * BroadleafCommerce Framework Web
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

package org.broadleafcommerce.core.web.processor;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.config.service.SystemPropertiesService;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.web.service.SimpleCacheKeyResolver;
import org.broadleafcommerce.core.web.service.TemplateCacheKeyResolverService;
import org.springframework.web.context.request.WebRequest;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import javax.annotation.Resource;

/**
 * <p>
 * Allows for a customizable cache mechanism that can be used to avoid expensive Thymeleaf processing for 
 * HTML fragments that are static. For high volume sites, even a 30 second cache of pages can have significant overall
 * performance impacts.
 * 
 * <p>
 * The parameters allowed for this processor include a "cacheTimeout" and "cacheKey". This component will rely on
 * an implementation of {@link TemplateCacheKeyResolverService} to build the actual cacheKey used by the underlying
 * caching implementation.
 * 
 * @param cacheTimeout (optional) the maximum length of time that the fragment will be allowed to be cached.
 * @param cacheKey (optional) Thymeleaf expression that should contribute to the final cache key.
 *  
 * @author bpolster
 * @see {@link TemplateCacheKeyResolverService}
 * @see {@link SimpleCacheKeyResolver}
 */
public class BroadleafCacheProcessor extends AbstractAttributeTagProcessor {

    private static final Log LOG = LogFactory.getLog(BroadleafCacheProcessor.class);

    public static final String ATTR_NAME = "cache";

    protected Cache cache;

    @Resource(name = "blSystemPropertiesService")
    protected SystemPropertiesService systemPropertiesService;

    @Resource(name = "blTemplateCacheKeyResolver")
    protected TemplateCacheKeyResolverService cacheKeyResolver;

    public BroadleafCacheProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, Integer.MIN_VALUE, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        if (!shouldCache(context, attributeValue)) {
            return;
        }

        String cacheKey = cacheKeyResolver.resolveCacheKey(context, tag);

        if (!StringUtils.isEmpty(cacheKey)) {
            net.sf.ehcache.Element cacheElement = getCache().get(cacheKey);
            if (cacheElement != null && !checkExpired(tag, cacheElement)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Template Cache Hit with cacheKey " + cacheKey + " found in cache.");
                }
                structureHandler.replaceWith((String) cacheElement.getObjectValue(), false);
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Template Cache Miss with cacheKey " + cacheKey + " not found in cache.");
                }
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Template not cached due to empty cacheKey");
            }
        }
    }

    protected boolean shouldCache(ITemplateContext context, String cacheAttrValue) {
        if (StringUtils.isEmpty(cacheAttrValue)) {
            return false;
        }

        cacheAttrValue = cacheAttrValue.toLowerCase();
        if (!isCachingEnabled() || "false".equals(cacheAttrValue)) {
            return false;
        } else if ("true".equals(cacheAttrValue)) {
            return true;
        }

        IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, cacheAttrValue);
        Object o = expression.execute(context);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            return "true".equals(((String) o).toLowerCase());
        }
        return false;
    }

    protected boolean checkExpired(IProcessableElementTag tag, net.sf.ehcache.Element cacheElement) {
        if (cacheElement.isExpired()) {
            return true;
        } else {
            String cacheTimeout = tag.getAttributeValue("cacheTimeout");
            if (!StringUtils.isEmpty(cacheTimeout) && StringUtils.isNumeric(cacheTimeout)) {
                Long timeout = Long.valueOf(cacheTimeout) * 1000;
                Long expiryTime = cacheElement.getCreationTime() + timeout;
                if (expiryTime < System.currentTimeMillis()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Cache getCache() {
        if (cache == null) {
            cache = CacheManager.getInstance().getCache("blTemplateElements");
        }
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public boolean isCachingEnabled() {
        boolean enabled = !systemPropertiesService.resolveBooleanSystemProperty("disableThymeleafTemplateCaching");
        if (enabled) {
            BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();
            if (brc != null && brc.getWebRequest() != null) {
                WebRequest request = brc.getWebRequest();
                String disableCachingParam = request.getParameter("disableThymeleafTemplateCaching");
                if ("true".equals(disableCachingParam)) {
                    return false;
                }
            }
        }
        return enabled;
    }
}
