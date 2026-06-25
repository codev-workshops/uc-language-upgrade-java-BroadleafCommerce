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

import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import javax.servlet.http.HttpServletRequest;

/**
 * Processor that replaces the "href" attribute on an element to add the context path and the application's
 * default uri prefix.
 * 
 * @author bpolster
 */
public class CatalogRelativeHrefProcessor extends AbstractAttributeTagProcessor {

    public CatalogRelativeHrefProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, "catalogRelativeHref", true, 10000, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();
        String baseUrl = request.getContextPath();
        
        structureHandler.setAttribute("href", baseUrl + attributeValue);
    }
}
