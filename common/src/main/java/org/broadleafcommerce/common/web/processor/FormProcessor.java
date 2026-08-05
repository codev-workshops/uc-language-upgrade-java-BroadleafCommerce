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
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
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
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public FormProcessor() {
        // Precedence 1: this replacement must execute as early as possible so that subsequent processors act
        // on this element as if it were a normal form instead of a blc:form
        super(TemplateMode.HTML, "blc", "form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model,
                             IElementModelStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IProcessableElementTag tag = (IProcessableElementTag) model.get(0);
        Map<String, String> formAttributes = new HashMap<String, String>();
        for (IAttribute attribute : tag.getAllAttributes()) {
            formAttributes.put(attribute.getAttributeCompleteName(), attribute.getValue());
        }
        IModel hiddenInputs = modelFactory.createModel();

        // If the form will be not be submitted with a GET, we must add the CSRF token
        // We do this instead of checking for a POST because post is default if nothing is specified
        if (!"GET".equalsIgnoreCase(tag.getAttributeValue("method"))) {
            try {
                String csrfToken = eps.getCSRFToken();
                String stateVersionToken = null;
                if (spps.isEnabled()) {
                    stateVersionToken = spps.getStateVersionToken();
                }

                //detect multipart form
                if ("multipart/form-data".equalsIgnoreCase(tag.getAttributeValue("enctype"))) {
                    IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                            .parseExpression(context, tag.getAttributeValue("th:action"));
                    String action = (String) expression.execute(context);
                    String csrfQueryParameter = "?" + eps.getCsrfTokenParameter() + "=" + csrfToken;
                    if (stateVersionToken != null) {
                        csrfQueryParameter += "&" + spps.getStateVersionTokenParameter() + "=" + stateVersionToken;
                    }
                    formAttributes.remove("th:action");
                    formAttributes.put("action", action + csrfQueryParameter);
                } else {
                    hiddenInputs.add(createHiddenInput(modelFactory, eps.getCsrfTokenParameter(), csrfToken));
                    if (stateVersionToken != null) {
                        hiddenInputs.add(createHiddenInput(modelFactory, spps.getStateVersionTokenParameter(),
                                stateVersionToken));
                    }
                }

            } catch (ServiceException e) {
                throw new RuntimeException("Could not get a CSRF token for this session", e);
            }
        }
        
        // Convert the <blc:form> node to a normal <form> node, preserving its body
        IModel body = modelFactory.createModel();
        for (int i = 1; i < model.size() - 1; i++) {
            body.add(model.get(i));
        }

        model.reset();
        model.add(modelFactory.createOpenElementTag("form", formAttributes, null, false));
        model.addModel(hiddenInputs);
        model.addModel(body);
        model.add(modelFactory.createCloseElementTag("form"));
    }

    protected IProcessableElementTag createHiddenInput(IModelFactory modelFactory, String name, String value) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("type", "hidden");
        attributes.put("name", name);
        attributes.put("value", value);
        return modelFactory.createStandaloneElementTag("input", attributes, null, false, true);
    }

}
