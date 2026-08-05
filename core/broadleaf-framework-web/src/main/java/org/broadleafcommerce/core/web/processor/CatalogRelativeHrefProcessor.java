/*
 * #%L
 * BroadleafCommerce CMS Module
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
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogURLService;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.broadleafcommerce.core.web.dialect.AbstractAttributeModifierProcessor;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * For use with category and product entities.   Creates a relative URL using the
 * current URI appended with the url-key (or last fragment of the url).
 * 
 * Takes in a category or product object as a parameter.
 * 
 * Uses the current request for the baseURI.
 * 
 * This implementation will also a categoryId or productId to the end of the URL it generates.
 * 
 * @author bpolster
 */
public class CatalogRelativeHrefProcessor extends AbstractAttributeModifierProcessor {

    private static final String RHREF = "rhref";
    private static final String HREF = "href";

    @Resource(name = "blCatalogURLService")
    protected CatalogURLService catalogURLService;

    public CatalogRelativeHrefProcessor() {
        super(RHREF);
    }

    @Override
    protected Map<String, String> getModifiedAttributes(ITemplateContext context, IProcessableElementTag tag,
                                                       String attributeValue) {
        HttpServletRequest request = BroadleafRequestContext.getBroadleafRequestContext().getRequest();

        String relativeHref = buildRelativeHref(evaluate(context, attributeValue), request);
               
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(HREF, relativeHref);
        return attrs;
    }

    protected String buildRelativeHref(Object result, HttpServletRequest request) {
        String currentUrl = request.getRequestURI();

        if (request.getQueryString() != null) {
            currentUrl = currentUrl + "?" + request.getQueryString();
        }

        if (result instanceof Product) {
            return catalogURLService.buildRelativeProductURL(currentUrl, (Product) result);
        } else if (result instanceof Category) {
            return catalogURLService.buildRelativeCategoryURL(currentUrl, (Category) result);
        }
        return "";
    }

}
