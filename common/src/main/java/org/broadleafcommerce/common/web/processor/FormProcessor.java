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
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

/**
 * Used as a replacement to the HTML {@code <form>} element which adds a CSRF token input field to forms that are submitted
 * via anything but GET. This is required to properly bypass the {@link CsrfFilter}.
 * 
 * @author apazzolini
 * @see {@link CsrfFilter}
 */
@Component("blFormProcessor")
public class FormProcessor extends AbstractElementTagProcessor {
    
    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService eps;

    @Resource(name = "blStaleStateProtectionService")
    protected StaleStateProtectionService spps;
    
    public FormProcessor() {
        super(TemplateMode.HTML, "blc", "form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        String method = tag.getAttributeValue("method");
        IModelFactory modelFactory = context.getModelFactory();
        IModel model = modelFactory.createModel();

        // Build a map of attributes for the output <form> tag, preserving original attributes
        Map<String, String> attrs = new HashMap<>();
        for (IAttribute attr : tag.getAllAttributes()) {
            String completeName = attr.getAttributeCompleteName();
            // Skip the matched element name attributes (handled by TL3)
            if (!completeName.startsWith("blc:")) {
                attrs.put(completeName, attr.getValue());
            }
        }

        model.add(modelFactory.createOpenElementTag("form", attrs, null, false));

        if (!"GET".equalsIgnoreCase(method)) {
            try {
                String csrfToken = eps.getCSRFToken();
                String stateVersionToken = null;
                if (spps.isEnabled()) {
                    stateVersionToken = spps.getStateVersionToken();
                }

                String enctype = tag.getAttributeValue("enctype");
                if ("multipart/form-data".equalsIgnoreCase(enctype)) {
                    String thAction = tag.getAttributeValue("th:action");
                    IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
                    IStandardExpression expression = parser.parseExpression(context, thAction);
                    String action = (String) expression.execute(context);
                    String csrfQueryParameter = "?" + eps.getCsrfTokenParameter() + "=" + csrfToken;
                    if (stateVersionToken != null) {
                        csrfQueryParameter += "&" + spps.getStateVersionTokenParameter() + "=" + stateVersionToken;
                    }
                    // The action attribute will be set in the output form tag
                    attrs.put("action", action + csrfQueryParameter);
                    attrs.remove("th:action");
                    // Rebuild the open tag with updated attributes
                    model = modelFactory.createModel();
                    model.add(modelFactory.createOpenElementTag("form", attrs, null, false));
                } else {
                    // Add hidden CSRF fields
                    StringBuilder csrfHidden = new StringBuilder();
                    csrfHidden.append("<input type=\"hidden\" name=\"")
                              .append(eps.getCsrfTokenParameter())
                              .append("\" value=\"")
                              .append(csrfToken)
                              .append("\" />");
                    if (stateVersionToken != null) {
                        csrfHidden.append("<input type=\"hidden\" name=\"")
                                  .append(spps.getStateVersionTokenParameter())
                                  .append("\" value=\"")
                                  .append(stateVersionToken)
                                  .append("\" />");
                    }
                    model.add(modelFactory.createText(csrfHidden.toString()));
                }
            } catch (ServiceException e) {
                throw new RuntimeException("Could not get a CSRF token for this session", e);
            }
        }

        model.add(modelFactory.createCloseElementTag("form"));
        structureHandler.replaceWith(model, true);
    }
}
