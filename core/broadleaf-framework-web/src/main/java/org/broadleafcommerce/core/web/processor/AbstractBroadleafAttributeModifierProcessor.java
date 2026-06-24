/*
 * #%L
 * BroadleafCommerce Framework Web
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
package org.broadleafcommerce.core.web.processor;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

/**
 * Thymeleaf 3 replacement for the Thymeleaf 2 {@code AbstractAttributeModifierAttrProcessor}. Subclasses compute a map
 * of attribute names to values that should be applied to the host element; an empty/blank value removes the attribute.
 * The processed (custom) attribute is removed automatically.
 */
public abstract class AbstractBroadleafAttributeModifierProcessor extends AbstractAttributeTagProcessor {

    public static final String DIALECT_PREFIX = "blc";

    public AbstractBroadleafAttributeModifierProcessor(String attributeName, int precedence) {
        super(TemplateMode.HTML, DIALECT_PREFIX, null, false, attributeName, true, precedence, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        Map<String, String> modifiedAttributes = getModifiedAttributeValues(context, tag, attributeValue);
        if (modifiedAttributes != null) {
            for (Map.Entry<String, String> entry : modifiedAttributes.entrySet()) {
                if (entry.getValue() == null || entry.getValue().length() == 0) {
                    structureHandler.removeAttribute(entry.getKey());
                } else {
                    structureHandler.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected abstract Map<String, String> getModifiedAttributeValues(ITemplateContext context, IProcessableElementTag tag, String attributeValue);
}
