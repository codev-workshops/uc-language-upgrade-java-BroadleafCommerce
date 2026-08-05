/*
 * #%L
 * BroadleafCommerce CMS Module
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
package org.broadleafcommerce.cms.web.processor;

import org.broadleafcommerce.common.file.service.StaticAssetPathService;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;

import jakarta.annotation.Resource;

/**
 * Similar to {@link UrlRewriteProcessor} but handles href tags.   
 * Mainly those that have a useCdn=true attribute or those that are inside a script tag.
 * 
 * @author bpolster
 */
public class HrefUrlRewriteProcessor extends UrlRewriteProcessor {
    
    @Resource(name = "blStaticAssetPathService")
    protected StaticAssetPathService staticAssetPathService;

    private static final String LINK = "link";
    private static final String HREF = "href";

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public HrefUrlRewriteProcessor() {
        super(HREF);
    }

    @Override
    protected String getRewrittenAttributeName(IProcessableElementTag tag) {
        return HREF;
    }

    @Override
    protected String rewrite(ITemplateContext context, IProcessableElementTag tag, String attributeValue) {
        String elementName = tag.getElementCompleteName();
        String useCDN = tag.getAttributeValue("useCDN");

        if (LINK.equals(elementName) || "true".equals(useCDN)) {
            return super.rewrite(context, tag, attributeValue);
        }
        return attributeValue;
    }
}
