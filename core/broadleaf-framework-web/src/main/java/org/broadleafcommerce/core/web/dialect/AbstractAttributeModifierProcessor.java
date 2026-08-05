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
package org.broadleafcommerce.core.web.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

/**
 * Replacement for Thymeleaf 2's {@code AbstractAttributeModifierAttrProcessor}, which was removed in Thymeleaf 3.
 * Subclasses compute a map of attribute names to values that is written onto the current tag; the processed
 * attribute itself is always removed.
 */
public abstract class AbstractAttributeModifierProcessor extends AbstractAttributeTagProcessor {

    public static final String DIALECT_PREFIX = "blc";

    public AbstractAttributeModifierProcessor(String attributeName) {
        this(attributeName, 10000);
    }

    public AbstractAttributeModifierProcessor(String attributeName, int precedence) {
        super(TemplateMode.HTML, DIALECT_PREFIX, null, false, attributeName, true, precedence, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                             String attributeValue, IElementTagStructureHandler structureHandler) {
        Map<String, String> attributes = getModifiedAttributes(context, tag, attributeValue);
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            structureHandler.setAttribute(attribute.getKey(), attribute.getValue());
        }
    }

    /**
     * @return the attributes that should replace the processed attribute on the current tag
     */
    protected abstract Map<String, String> getModifiedAttributes(ITemplateContext context, IProcessableElementTag tag,
                                                                 String attributeValue);

    /**
     * Evaluates the given Thymeleaf expression against the current context.
     */
    protected Object evaluate(ITemplateContext context, String expressionValue) {
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, expressionValue);
        return expression.execute(context);
    }
}
