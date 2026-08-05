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

import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.search.domain.SearchCriteria;
import org.broadleafcommerce.core.web.util.ProcessorUtils;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.broadleafcommerce.core.web.dialect.AbstractAttributeModifierProcessor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Thymeleaf Processor that replaces the "href" attribute on an <a/> element, maintaining the current search criteria
 * of the request and adding (or replacing, if it exists) the sort parameter on the request.
 *
 * @author Joseph Fridye (jfridye)
 */
public class PaginationSortLinkProcessor extends AbstractAttributeModifierProcessor {

    public PaginationSortLinkProcessor() {
        super("pagination-sort-link", 10000);
    }


    @Override
    protected Map<String, String> getModifiedAttributes(ITemplateContext context, IProcessableElementTag tag,
                                                       String attributeValue) {

        Map<String, String> attributes = new HashMap<String, String>();

        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();

        String baseUrl = request.getRequestURL().toString();

        Map<String, String[]> params = new HashMap<String, String[]>(request.getParameterMap());

        String sort = attributeValue;

        if (StringUtils.isNotBlank(sort)) {
            params.put(SearchCriteria.SORT_STRING, new String[]{sort});
        } else {
            params.remove(SearchCriteria.SORT_STRING);
        }

        // If there is a page number parameter, remove it. This ensures that when the search results refresh the
        // first page of results will be displayed.
        params.remove(SearchCriteria.PAGE_NUMBER);

        String url = ProcessorUtils.getUrl(baseUrl, params);

        attributes.put("href", url);

        return attributes;

    }

}
