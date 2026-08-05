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
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;

/**
 * <p>The following processor will modify the declared Credit Card Form
 * and call the Transparent Redirect Service of the configured payment gateway. </p>
 *
 * <p>This processor will change the form's action URL and append any hidden input fields
 * that are necessary to make the call. Certain gateway implementations accept configuration
 * settings in order to generate the form. These configuration parameters can be passed into
 * the module, by prefixing any configuration settings name with "config-" + attribute name = attribute value
 * </p>
 * <p>Here is an example:</p>
 *
 * <pre><code>
 *     <blc:transparent_credit_card_form action="#" method="POST"
 *         paymentRequestDTO="${requestDTO}"
 *         config-specificGatewayParam="value1"
 *         config-specificGatewayParam2="value2"
 *         config-specificGatewayParam3="value3">
 *
 *         <input type="text" name="credit_card_num"/>
 *         ...
 *
 *     </blc:transparent_credit_form>
 * </code></pre>
 *
 * <p>NOTE: please see {@link org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldVariableExpression}
 * to modify the input "name" fields for a particular gateway</p>
 *
 * @see {@link org.broadleafcommerce.common.web.payment.expression.PaymentGatewayFieldVariableExpression}
 * @see {@link TRCreditCardExtensionHandler}
 * @see {@link AbstractTRCreditCardExtensionHandler}
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
    protected void doProcess(ITemplateContext context, IModel model,
                             IElementModelStructureHandler structureHandler) {
        IModelFactory modelFactory = context.getModelFactory();
        IProcessableElementTag tag = (IProcessableElementTag) model.get(0);

        IStandardExpression expression = StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, tag.getAttributeValue("paymentRequestDTO"));
        PaymentRequestDTO requestDTO = (PaymentRequestDTO) expression.execute(context);

        Map<String, String> formAttributes = new HashMap<String, String>();
        Map<String, Map<String,String>> formParameters = new HashMap<String, Map<String,String>>();
        Map<String, String> configurationSettings = new HashMap<String, String>();

        //Create the configuration settings map to pass into the payment module
        for (IAttribute attribute : tag.getAllAttributes()) {
            String key = attribute.getAttributeCompleteName();
            if (key.startsWith("config-")) {
                configurationSettings.put(key.substring("config-".length()), attribute.getValue());
            } else if (!"paymentRequestDTO".equals(key)) {
                formAttributes.put(key, attribute.getValue());
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

        //Change the action attribute on the form to the Payment Gateways Endpoint
        String actionUrl = "";
        Map<String,String> actionValue = formParameters.get(formActionKey.toString());
        if (actionValue != null && actionValue.size()>0) {
            String key = (String)actionValue.keySet().toArray()[0];
            actionUrl = actionValue.get(key);
        }
        formAttributes.put("action", actionUrl);

        //Append any hidden fields necessary for the Transparent Redirect
        IModel hiddenInputs = modelFactory.createModel();
        Map<String, String> hiddenFields = formParameters.get(formHiddenParamsKey.toString());
        if (hiddenFields != null && !hiddenFields.isEmpty()) {
            for (Map.Entry<String, String> hiddenField : hiddenFields.entrySet()) {
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put("type", "hidden");
                attributes.put("name", hiddenField.getKey());
                attributes.put("value", hiddenField.getValue());
                hiddenInputs.add(modelFactory.createStandaloneElementTag("input", attributes, null, false, true));
            }
        }

        // Convert the <blc:transparent_credit_card_form> node to a normal <form> node, preserving its body
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

    public TRCreditCardExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public void setExtensionManager(TRCreditCardExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }
}
