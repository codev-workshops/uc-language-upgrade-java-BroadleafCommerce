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

import org.broadleafcommerce.openadmin.web.rulebuilder.dto.FieldWrapper;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.RuleBuilderFieldService;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.RuleBuilderFieldServiceFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import javax.annotation.Resource;

/**
 * @author Elbert Bautista (elbertbautista)
 */
@Component("blAdminFieldBuilderProcessor")
public class AdminFieldBuilderProcessor extends AbstractElementTagProcessor {

    @Resource(name = "blRuleBuilderFieldServiceFactory")
    protected RuleBuilderFieldServiceFactory ruleBuilderFieldServiceFactory;

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public AdminFieldBuilderProcessor() {
        super(TemplateMode.HTML, "blc_admin", "admin_field_builder", true, null, false, 100);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        FieldWrapper fieldWrapper = new FieldWrapper();
        
        String fieldBuilderValue = tag.getAttributeValue("fieldBuilder");
        IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, fieldBuilderValue);
        String fieldBuilder = (String) expression.execute(context);

        if (fieldBuilder != null) {
            RuleBuilderFieldService ruleBuilderFieldService = ruleBuilderFieldServiceFactory.createInstance(fieldBuilder);
            if (ruleBuilderFieldService != null) {
                fieldWrapper = ruleBuilderFieldService.buildFields();
            }
        }
        
        structureHandler.setLocalVariable("fieldWrapper", fieldWrapper);
        structureHandler.removeElement();
    }
}
