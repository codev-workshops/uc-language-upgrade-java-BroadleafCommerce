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
package org.broadleafcommerce.common.web.processor;

import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.security.handler.CsrfFilter;
import org.broadleafcommerce.common.security.service.ExploitProtectionService;
import org.broadleafcommerce.common.security.service.StaleStateProtectionService;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Resource;

/**
 * Used as a replacement to the HTML {@code <form>} element which adds a CSRF token input field to forms that are submitted
 * via anything but GET. This is required to properly bypass the {@link CsrfFilter}.
 * 
 * @author apazzolini
 * @see {@link CsrfFilter}
 */
@Component("blFormProcessor")
public class FormProcessor extends AbstractElementModelProcessor {
    
    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService eps;

    @Resource(name = "blStaleStateProtectionService")
    protected StaleStateProtectionService spps;
    
    /**
     * Sets the name of this processor to be used in Thymeleaf template. The low precedence allows subsequent
     * processors to act on the resulting element as if it were a normal form instead of a blc:form.
     */
    public FormProcessor() {
        super(TemplateMode.HTML, "blc", "form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IProcessableElementTag formTag = (IProcessableElementTag) model.get(0);

        Map<String, String> attributes = new LinkedHashMap<String, String>(formTag.getAttributeMap());

        // If the form will be not be submitted with a GET, we must add the CSRF token
        // We do this instead of checking for a POST because post is default if nothing is specified
        boolean csrfRequired = !"GET".equalsIgnoreCase(attributes.get("method"));
        String csrfToken = null;
        String stateVersionToken = null;
        boolean multipart = false;

        if (csrfRequired) {
            try {
                csrfToken = eps.getCSRFToken();
                if (spps.isEnabled()) {
                    stateVersionToken = spps.getStateVersionToken();
                }

                //detect multipart form
                multipart = "multipart/form-data".equalsIgnoreCase(attributes.get("enctype"));
                if (multipart) {
                    IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
                    IStandardExpression expression = parser.parseExpression(context, attributes.get("th:action"));
                    String action = (String) expression.execute(context);
                    String csrfQueryParameter = "?" + eps.getCsrfTokenParameter() + "=" + csrfToken;
                    if (stateVersionToken != null) {
                        csrfQueryParameter += "&" + spps.getStateVersionTokenParameter() + "=" + stateVersionToken;
                    }
                    attributes.remove("th:action");
                    attributes.put("action", action + csrfQueryParameter);
                }
            } catch (ServiceException e) {
                throw new RuntimeException("Could not get a CSRF token for this session", e);
            }
        }

        // Convert the <blc:form> node to a normal <form> node, preserving all attributes
        IOpenElementTag newOpenTag = modelFactory.createOpenElementTag("form", attributes, AttributeValueQuotes.DOUBLE, false);
        ICloseElementTag newCloseTag = modelFactory.createCloseElementTag("form");
        model.replace(0, newOpenTag);
        model.replace(model.size() - 1, newCloseTag);

        // For non-multipart forms, inject hidden CSRF (and state version) inputs at the end of the form body
        if (csrfRequired && !multipart) {
            model.insert(model.size() - 1, createHiddenInput(modelFactory, eps.getCsrfTokenParameter(), csrfToken));
            if (stateVersionToken != null) {
                model.insert(model.size() - 1, createHiddenInput(modelFactory, spps.getStateVersionTokenParameter(), stateVersionToken));
            }
        }
    }

    protected IStandaloneElementTag createHiddenInput(IModelFactory modelFactory, String name, String value) {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("type", "hidden");
        attrs.put("name", name);
        attrs.put("value", value);
        return modelFactory.createStandaloneElementTag("input", attrs, AttributeValueQuotes.DOUBLE, false, true);
    }
    
}
