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
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
        super("blc", HREF);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        
        String elementName = tag.getElementDefinition().getElementName().getElementName();
        String useCDN = tag.getAttributeValue("useCDN");

        if (LINK.equals(elementName) || (useCDN != null && "true".equals(useCDN))) {
            HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();
            
            boolean secureRequest = true;
            if (request != null) {
                secureRequest = isRequestSecure(request);
            }
            
            String elementValue = attributeValue;
            if (elementValue.startsWith("/")) {
                elementValue = "@{ " + elementValue + " }";
            }
            
            IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(context.getConfiguration());
            IStandardExpression expression = expressionParser.parseExpression(context, elementValue);
            String assetPath = (String) expression.execute(context);
            
            assetPath = staticAssetPathService.convertAssetPath(assetPath, null, secureRequest);
            structureHandler.setAttribute(HREF, assetPath);
        } else {
            structureHandler.setAttribute(HREF, attributeValue);
        }
    }
}
