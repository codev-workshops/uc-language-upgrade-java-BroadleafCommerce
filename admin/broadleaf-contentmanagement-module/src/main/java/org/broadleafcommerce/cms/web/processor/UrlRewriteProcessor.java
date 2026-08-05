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

import org.broadleafcommerce.cms.file.service.StaticAssetService;
import org.broadleafcommerce.common.file.service.StaticAssetPathService;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A Thymeleaf processor that processes the given url through the StaticAssetService's
 * {@link StaticAssetService#convertAssetPath(String, String, boolean)} method to determine
 * the appropriate URL for the asset to be served from.
 * 
 * @author apazzolini
 */
public class UrlRewriteProcessor extends AbstractAttributeTagProcessor {
    
    @Resource(name = "blStaticAssetPathService")
    protected StaticAssetPathService staticAssetPathService;

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public UrlRewriteProcessor() {
        this("src");
    }

    protected UrlRewriteProcessor(final String attributeName) {
        super(TemplateMode.HTML, "blc", null, false, attributeName, true, 1000, true);
    }

    /**
     * @return true if the current request.scheme = HTTPS or if the request.isSecure value is true.
     */
    protected boolean isRequestSecure(HttpServletRequest request) {
        return ("HTTPS".equalsIgnoreCase(request.getScheme()) || request.isSecure());
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                             String attributeValue, IElementTagStructureHandler structureHandler) {
        structureHandler.setAttribute(getRewrittenAttributeName(tag),
                rewrite(context, tag, attributeValue));
    }

    /**
     * @return the attribute that the rewritten value is written to
     */
    protected String getRewrittenAttributeName(IProcessableElementTag tag) {
        return "src";
    }

    protected String rewrite(ITemplateContext context, IProcessableElementTag tag, String attributeValue) {
        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();

        boolean secureRequest = true;
        if (request != null) {
            secureRequest = isRequestSecure(request);
        }

        String elementValue = attributeValue;
        if (elementValue.startsWith("/")) {
            elementValue = "@{ " + elementValue + " }";
        }
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, elementValue);
        String assetPath = (String) expression.execute(context);

        // We are forcing an evaluation of @{} from Thymeleaf above which will automatically add a contextPath, no need to
        // add it twice
        return staticAssetPathService.convertAssetPath(assetPath, null, secureRequest);
    }
}
