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
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;

/**
 * <p>The following processor will modify the declared Credit Card Form and call the Transparent Redirect Service of the
 * configured payment gateway.   See the original Broadleaf source for the full tag contract and example usage.</p>
 *
 * <p>Migrated to the Thymeleaf 3 {@link AbstractElementModelProcessor} API: the {@code <blc:transparent_credit_card_form>}
 * element is rewritten to a standard {@code <form>} by editing the element {@link IModel} (preserving the body) and
 * inserting the gateway's hidden fields as child standalone tags.</p>
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Component("blTransparentRedirectCreditCardFormProcessor")
public class TransparentRedirectCreditCardFormProcessor extends AbstractElementModelProcessor {

    @Resource(name = "blTRCreditCardExtensionManager")
    protected TRCreditCardExtensionManager extensionManager;

    public TransparentRedirectCreditCardFormProcessor() {
        super(TemplateMode.HTML, "blc", "transparent_credit_card_form", true, null, false, 1);
    }

    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IProcessableElementTag formTag = (IProcessableElementTag) model.get(0);

        Map<String, String> attributes = new LinkedHashMap<String, String>(formTag.getAttributeMap());

        IEngineConfiguration configuration = context.getConfiguration();
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
        IStandardExpression expression = parser.parseExpression(context, attributes.get("paymentRequestDTO"));
        PaymentRequestDTO requestDTO = (PaymentRequestDTO) expression.execute(context);

        attributes.remove("paymentRequestDTO");

        Map<String, Map<String, String>> formParameters = new HashMap<String, Map<String, String>>();
        Map<String, String> configurationSettings = new HashMap<String, String>();

        //Create the configuration settings map to pass into the payment module
        List<String> keysToRemove = new ArrayList<String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("config-")) {
                final int trimLength = "config-".length();
                String configParam = key.substring(trimLength);
                configurationSettings.put(configParam, entry.getValue());
                keysToRemove.add(key);
            }
        }

        for (String keyToRemove : keysToRemove) {
            attributes.remove(keyToRemove);
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

        //Change the action attribute on the form to the Payment Gateways Endpoint
        String actionUrl = "";
        Map<String, String> actionValue = formParameters.get(formActionKey.toString());
        if (actionValue != null && actionValue.size() > 0) {
            String key = (String) actionValue.keySet().toArray()[0];
            actionUrl = actionValue.get(key);
        }
        attributes.put("action", actionUrl);

        //Append any hidden fields necessary for the Transparent Redirect
        int insertIndex = 1;
        Map<String, String> hiddenFields = formParameters.get(formHiddenParamsKey.toString());
        if (hiddenFields != null && !hiddenFields.isEmpty()) {
            for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
                model.insert(insertIndex++, createHiddenInput(modelFactory, entry.getKey(), entry.getValue()));
            }
        }

        // Convert the <blc:transparent_credit_card_form> node to a normal <form> node
        model.replace(0, modelFactory.createOpenElementTag("form", attributes, AttributeValueQuotes.DOUBLE, false));
        model.replace(model.size() - 1, modelFactory.createCloseElementTag("form"));
    }

    protected IStandaloneElementTag createHiddenInput(IModelFactory modelFactory, String name, String value) {
        Map<String, String> inputAttributes = new LinkedHashMap<String, String>();
        inputAttributes.put("type", "hidden");
        inputAttributes.put("name", name);
        inputAttributes.put("value", value);
        return modelFactory.createStandaloneElementTag("input", inputAttributes, AttributeValueQuotes.DOUBLE, false, true);
    }

    public TRCreditCardExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public void setExtensionManager(TRCreditCardExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }
}
