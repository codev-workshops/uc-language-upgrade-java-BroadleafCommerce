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
import org.thymeleaf.standard.expression.StandardExpressions;

/**
 * Default implementation of {@link TemplateCacheKeyResolverService} that returns a concatenation of a 
 * templateName and cacheKey.   If the cacheKey is set to none, null is returned resulting in no cache.
 * 
 * @author Brian Polster (bpolster)
 */
@Service("blTemplateCacheKeyResolver")
public class SimpleCacheKeyResolver implements TemplateCacheKeyResolverService {
    
    @Override
    public String resolveCacheKey(ITemplateContext context, IProcessableElementTag tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(getStringValue(context, tag, "cacheKey"));
        sb.append(resolveTemplateName(context, tag));
        return sb.toString();
    }
    

    protected String resolveTemplateName(ITemplateContext context, IProcessableElementTag tag) {
        String templateName = getStringValue(context, tag, "templateName");

        if (StringUtils.isEmpty(templateName)) {
            templateName = context.getTemplateData().getTemplate();
        }
        
        return templateName;
    }

    protected String getStringValue(ITemplateContext context, IProcessableElementTag tag, String attrName) {
        String attrValue = tag.getAttributeValue(attrName);
        if (attrValue != null) {
            IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, attrValue);
            Object result = expression.execute(context);
            return result != null ? result.toString() : "";
        }
        return "";
    }
}
