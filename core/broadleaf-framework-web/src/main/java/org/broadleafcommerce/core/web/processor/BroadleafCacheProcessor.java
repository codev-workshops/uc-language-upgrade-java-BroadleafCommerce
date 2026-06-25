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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.config.service.SystemPropertiesService;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.common.web.dialect.BLCDialect;
import org.broadleafcommerce.core.web.service.SimpleCacheKeyResolver;
import org.broadleafcommerce.core.web.service.TemplateCacheKeyResolverService;
import org.springframework.web.context.request.WebRequest;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import javax.annotation.Resource;

/**
 * <p>
 * Allows for a customizable cache mechanism that can be used to avoid expensive Thymeleaf processing for
 * HTML fragments that are static. For high volume sites, even a 30 second cache of pages can have significant overall
 * performance impacts.
 *
 * <p>
 * <b>Thymeleaf 3 note:</b> In Thymeleaf 2 this processor implemented template-element caching by mutating the
 * (now removed) mutable DOM and by cooperating with the {@code CacheAwareGeneralTemplateWriter} / template-mode-handler
 * architecture, both of which were removed in Thymeleaf 3 (the engine is now immutable/streaming and exposes no
 * equivalent write-back hook). As a result this processor no longer performs element-level caching; it simply removes
 * its {@code blc:cache} attribute so existing templates continue to render. Template-level caching is still provided by
 * the engine's own {@link org.broadleafcommerce.core.web.cache.BLCICacheManager}. The {@link TemplateCacheKeyResolverService}
 * wiring is retained for backwards compatibility and for implementors that build their own caching on top of it.
 *
 * @author bpolster
 * @see {@link TemplateCacheKeyResolverService}
 * @see {@link SimpleCacheKeyResolver}
 */
public class BroadleafCacheProcessor extends AbstractAttributeTagProcessor {

    private static final Log LOG = LogFactory.getLog(BroadleafCacheProcessor.class);

    public static final String ATTR_NAME = "cache";

    protected static final int PRECEDENCE = Integer.MIN_VALUE;

    @Resource(name = "blSystemPropertiesService")
    protected SystemPropertiesService systemPropertiesService;

    @Resource(name = "blTemplateCacheKeyResolver")
    protected TemplateCacheKeyResolverService cacheKeyResolver;

    public BroadleafCacheProcessor() {
        super(TemplateMode.HTML, BLCDialect.DEFAULT_PREFIX, null, false, ATTR_NAME, true, PRECEDENCE, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        // The blc:cache attribute is removed automatically (removeAttribute=true). Element-level caching is no longer
        // possible on the immutable Thymeleaf 3 engine; see the class-level Javadoc.
        if (LOG.isTraceEnabled() && isCachingEnabled()) {
            LOG.trace("blc:cache is a no-op on Thymeleaf 3; relying on engine-level template caching instead.");
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
