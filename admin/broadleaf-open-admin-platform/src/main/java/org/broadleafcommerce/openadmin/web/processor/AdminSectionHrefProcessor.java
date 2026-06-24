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

import org.broadleafcommerce.openadmin.server.security.domain.AdminSection;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * A Thymeleaf processor that will generate the HREF of a given Admin Section.
 * This is useful in constructing the left navigation menu for the admin console.
 *
 * @author elbertbautista
 */
@Component("blAdminSectionHrefProcessor")
public class AdminSectionHrefProcessor extends AbstractAttributeTagProcessor {

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public AdminSectionHrefProcessor() {
        super(TemplateMode.HTML, "blc_admin", null, false, "admin_section_href", true, 10002, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
            String attributeValue, IElementTagStructureHandler structureHandler) {
        String href = "#";

        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, attributeValue);
        AdminSection section = (AdminSection) expression.execute(context);
        if (section != null) {
            String contextPath = "";
            if (context instanceof IWebContext) {
                contextPath = ((IWebContext) context).getExchange().getRequest().getApplicationPath();
            }
            href = contextPath + section.getUrl();
        }

        structureHandler.setAttribute("href", href);
    }

}
