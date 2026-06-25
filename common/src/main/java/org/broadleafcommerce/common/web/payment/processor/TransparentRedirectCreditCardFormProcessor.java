/*
 * #%L
 * BroadleafCommerce Common Libraries
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

package org.broadleafcommerce.common.web.payment.processor;

import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

/**
 * The following processor will modify the declared Credit Card Form
 * and call the Transparent Redirect Service of the configured payment gateway.
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Component("blTransparentRedirectCreditCardFormProcessor")
public class TransparentRedirectCreditCardFormProcessor extends AbstractElementTagProcessor {

    @Resource(name = "blTRCreditCardExtensionManager")
    protected TRCreditCardExtensionManager extensionManager;

    public TransparentRedirectCreditCardFormProcessor() {
        super(TemplateMode.HTML, "blc", "transparent_credit_card_form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, tag.getAttributeValue("paymentRequestDTO"));
        PaymentRequestDTO requestDTO = (PaymentRequestDTO) expression.execute(context);

        Map<String, Map<String,String>> formParameters = new HashMap<>();
        Map<String, String> configurationSettings = new HashMap<>();

        // Collect attributes to pass through to form and config settings
        Map<String, String> formAttrs = new HashMap<>();
        IAttribute[] allAttributes = tag.getAllAttributes();
        for (IAttribute attr : allAttributes) {
            String attrName = attr.getAttributeCompleteName();
            if (attrName.startsWith("config-")) {
                String configParam = attrName.substring("config-".length());
                configurationSettings.put(configParam, attr.getValue());
            } else if (!"paymentRequestDTO".equals(attrName) && !attrName.startsWith("blc:")) {
                formAttrs.put(attrName, attr.getValue());
            }
        }

        try {
            extensionManager.getProxy().createTransparentRedirectForm(formParameters,
                    requestDTO, configurationSettings);
        } catch (PaymentException e) {
            throw new RuntimeException("Unable to Create the Transparent Redirect Form", e);
        }

        StringBuilder formActionKey = new StringBuilder("formActionKey");
        StringBuilder formHiddenParamsKey = new StringBuilder("formHiddenParamsKey");
        extensionManager.getProxy().setFormActionKey(formActionKey);
        extensionManager.getProxy().setFormHiddenParamsKey(formHiddenParamsKey);

        String actionUrl = "";
        Map<String,String> actionValue = formParameters.get(formActionKey.toString());
        if (actionValue != null && !actionValue.isEmpty()) {
            String key = (String) actionValue.keySet().toArray()[0];
            actionUrl = actionValue.get(key);
        }
        formAttrs.put("action", actionUrl);

        IModelFactory modelFactory = context.getModelFactory();
        IModel model = modelFactory.createModel();
        model.add(modelFactory.createOpenElementTag("form", formAttrs, null, false));

        Map<String, String> hiddenFields = formParameters.get(formHiddenParamsKey.toString());
        if (hiddenFields != null && !hiddenFields.isEmpty()) {
            StringBuilder hiddenFieldsHtml = new StringBuilder();
            for (String key : hiddenFields.keySet()) {
                hiddenFieldsHtml.append("<input type=\"hidden\" name=\"")
                                .append(key)
                                .append("\" value=\"")
                                .append(hiddenFields.get(key))
                                .append("\" />");
            }
            model.add(modelFactory.createText(hiddenFieldsHtml.toString()));
        }

        model.add(modelFactory.createCloseElementTag("form"));
        structureHandler.replaceWith(model, true);
    }

    public TRCreditCardExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public void setExtensionManager(TRCreditCardExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }
}
