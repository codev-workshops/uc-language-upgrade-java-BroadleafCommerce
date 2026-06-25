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
import org.broadleafcommerce.core.search.domain.SearchCriteria;
import org.broadleafcommerce.core.web.util.ProcessorUtils;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Thymeleaf Processor that replaces the "href" attribute on an element, maintaining the current search criteria
 * of the request and adding (or replacing, if it exists) the page number parameter on the request.
 *
 * @author Joseph Fridye (jfridye)
 */
public class PaginationPageLinkProcessor extends AbstractAttributeTagProcessor {

    public PaginationPageLinkProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, "paginationpagelink", true, 10000, true);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();

        String baseUrl = request.getRequestURL().toString();

        Map<String, String[]> params = new HashMap<String, String[]>(request.getParameterMap());

        Integer page = Integer.parseInt(attributeValue);

        if (page != null && page > 1) {
            params.put(SearchCriteria.PAGE_NUMBER, new String[]{page.toString()});
        } else {
            params.remove(SearchCriteria.PAGE_NUMBER);
        }

        String url = ProcessorUtils.getUrl(baseUrl, params);

        structureHandler.setAttribute("href", url);
    }
}
