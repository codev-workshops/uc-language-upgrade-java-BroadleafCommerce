/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.web.processor;

import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.openadmin.web.form.component.ListGrid;
import org.broadleafcommerce.openadmin.web.form.entity.Field;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * A Thymeleaf processor that will generate the appropriate ID for a given admin component.
 * 
 * @author Andre Azzolini (apazzolini)
 */
@Component("blAdminComponentIdProcessor")
public class AdminComponentIdProcessor extends AbstractAttributeTagProcessor {

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public AdminComponentIdProcessor() {
        super(TemplateMode.HTML, "blc_admin", null, false, "component_id", true, 10002, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, attributeValue);
        Object component = expression.execute(context);

        String fieldName = "";
        String id = "";
        
        if (component instanceof ListGrid) {
            ListGrid lg = (ListGrid) component;
            
            fieldName = "listGrid-";
            if (ListGrid.Type.MAIN.toString().toLowerCase().equals(lg.getListGridType())) {
                fieldName += ListGrid.Type.MAIN.toString().toLowerCase();
            } else {
                fieldName = fieldName + lg.getListGridType() + '-' + lg.getSubCollectionFieldName();
            }
        } else if (component instanceof Field) {
            Field field = (Field) component;
            fieldName = "field-" + field.getName();
        }
        
        if (StringUtils.isNotBlank(fieldName)) {
            id = cleanCssIdString(fieldName);
        }

        if (StringUtils.isNotBlank(id)) {
            structureHandler.setAttribute("id", id);
        }
    }

    protected String cleanCssIdString(String in) {
        in = in.replaceAll("[^a-zA-Z0-9-]", "-");
        return in;
    }

}
