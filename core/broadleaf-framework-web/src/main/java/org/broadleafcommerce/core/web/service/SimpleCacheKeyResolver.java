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

package org.broadleafcommerce.core.web.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

/**
 * Default implementation of {@link TemplateCacheKeyResolverService} that returns a concatenation of a 
 * templateName and cacheKey.   If the cacheKey is set to none, null is returned resulting in no cache.
 * 
 * @author Brian Polster (bpolster)
 */
@Service("blTemplateCacheKeyResolver")
public class SimpleCacheKeyResolver implements TemplateCacheKeyResolverService {
    
    /**
     * Returns a concatenation of the templateName and cacheKey separated by an "_".    
     * If cacheKey is null, only the templateName is returned.
     * 
     * If cacheKey is "none" then null will be returned causing the template not to be cached.
     * 
     * @return
     */
    @Override
    public String resolveCacheKey(ITemplateContext context, IProcessableElementTag element) {
        StringBuilder sb = new StringBuilder();
        sb.append(getStringValue(context, element, "cacheKey"));
        sb.append(resolveTemplateName(context, element));
        sb.append(resolveLineNumber(context, element));
        return sb.toString();
    }
    

    protected String resolveTemplateName(ITemplateContext context, IProcessableElementTag element) {
        String templateName = getStringValue(context, element, "templateName");

        if (StringUtils.isEmpty(templateName)) {
            templateName = element.getTemplateName();
        }

        if (StringUtils.isEmpty(templateName) && context.getTemplateData() != null) {
            templateName = context.getTemplateData().getTemplate();
        }
        
        return templateName;
    }
    
    protected Integer resolveLineNumber(ITemplateContext context, IProcessableElementTag element) {
        return element.hasLocation() ? element.getLine() : 0;
    }

    protected String getStringValue(ITemplateContext context, IProcessableElementTag element, String attrName) {
        if (element.hasAttribute(attrName)) {
            String cacheKeyParam = element.getAttributeValue(attrName);
            IStandardExpressionParser expressionParser = StandardExpressions.getExpressionParser(context.getConfiguration());
            IStandardExpression expression = expressionParser.parseExpression(context, cacheKeyParam);
            return expression.execute(context).toString();

        }
        return "";
    }
}
