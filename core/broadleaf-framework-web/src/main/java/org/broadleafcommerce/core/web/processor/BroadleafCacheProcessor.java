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

import jakarta.annotation.Resource;

/**
 * <p>
 * Allows for a customizable cache mechanism that can be used to avoid expensive Thymeleaf processing for 
 * HTML fragments that are static. For high volume sites, even a 30 second cache of pages can have significant overall
 * performance impacts.
 * 
 * <p>
 * When used as in conjunction within a {@code th:substituteby}, {@code th:replace} or {@code th:include} attribute this
 * will cache the template being included. If used in conjunction with {@link th:remove} this will cache all of the child
 * nodes of the element. If neither of these cases are true, this will cache the current node and all children.
 * 
 * <p>
 * The parameters allowed for this processor include a "cacheTimeout" and "cacheKey". This component will rely on
 * an implementation of {@link TemplateCacheKeyResolverService} to build the actual cacheKey used by the underlying
 * caching implementation.   The parameter named "cacheKey" will be used in the construction of the actual cacheKey
 * which may rely on variables like 
 * 
 * <p>
 * Implementors can create more functional cacheKey mechanisms. For example, Broadleaf Enterprise provides an 
 * additional implementation named {@code EnterpriseCacheKeyResolver} with support for additional caching 
 * features.
 * 
 * @param cacheTimeout (optional) the maximum length of time that the fragment will be allowed to be cached.   This
 * is important for fragments for which a good cacheKey would be difficult to generate.
 * @param cacheKey (optional) Thymeleaf expression that should contribute to the final cache key to reference the cached
 * element. The final key is determined by the {@link TemplateCacheKeyResolverService} but it is not required.
 * Implementations of {@link TemplateCacheKeyResolverService} can rely on variables like the customer, site, theme, etc. to
 * build the final cacheKey.
 *  
 * @author bpolster
 * @see {@link TemplateCacheKeyResolverService}
 * @see {@link SimpleCacheKeyResolver}
 */
public class BroadleafCacheProcessor extends AbstractAttributeTagProcessor {

    private static final Log LOG = LogFactory.getLog(BroadleafCacheProcessor.class);

    public static final String ATTR_NAME = "cache";

    @Resource(name = "blSystemPropertiesService")
    protected SystemPropertiesService systemPropertiesService;

    @Resource(name = "blTemplateCacheKeyResolver")
    protected TemplateCacheKeyResolverService cacheKeyResolver;

    public BroadleafCacheProcessor() {
        super(TemplateMode.HTML, "blc", null, false, ATTR_NAME, true, Integer.MIN_VALUE, true);
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

        // Check for an expression
        IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, cacheAttrValue);
        Object o = expression.execute(context);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof String) {
            cacheAttrValue = (String) o;
            cacheAttrValue = cacheAttrValue.toLowerCase();
            return "true".equals(cacheAttrValue);
        }
        return false;
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        if (shouldCache(context, attributeValue)) {
            String cacheKey = cacheKeyResolver.resolveCacheKey(context, tag);
            if (StringUtils.isEmpty(cacheKey) && LOG.isTraceEnabled()) {
                LOG.trace("Template not cached due to empty cacheKey");
            }
        }
    }

    public boolean isCachingEnabled() {
        boolean enabled = !systemPropertiesService.resolveBooleanSystemProperty("disableThymeleafTemplateCaching");
        if (enabled) {
            // check for a URL param that overrides caching - useful for testing if this processor is incorrectly
            // caching a page (possibly due to an bad cacheKey).

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
