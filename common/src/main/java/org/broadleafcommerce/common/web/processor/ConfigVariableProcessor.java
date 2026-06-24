/*
 * #%L
 * BroadleafCommerce Common Libraries
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
package org.broadleafcommerce.common.web.processor;

import org.broadleafcommerce.common.util.BLCSystemProperty;
import org.broadleafcommerce.common.web.dialect.AbstractModelVariableModifierProcessor;
import org.broadleafcommerce.common.web.expression.PropertiesVariableExpression;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;


/**
 * <p>
 * Looks up the value of a configuration variable and adds the value to the model.
 * 
 * <p>
 * While this adds the configuration value onto the model, you might want to use the value of this in larger expression. In
 * that instance you may want to use {@link PropertiesVariableExpression} instead with {@code #props.get('property')}.
 * 
 * @parameter name (required) the name of the system property to look up
 * @parameter resultVar (optional) what model variable the system property value is added to, defaults to <b>value</b>
 * 
 * @author bpolster
 * @see {@link PropertiesVariableExpression}
 */
public class ConfigVariableProcessor extends AbstractModelVariableModifierProcessor {

    public ConfigVariableProcessor() {
        super("config", 10000);
    }

    @Override
    protected void modifyModelAttributes(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        String resultVar = tag.getAttributeValue("resultVar");
        if (resultVar == null) {
            resultVar = "value";
        }
        
        String attributeName = tag.getAttributeValue("name");
        String attributeValue = BLCSystemProperty.resolveSystemProperty(attributeName);
        
        addToModel(structureHandler, resultVar, attributeValue);
    }
}
