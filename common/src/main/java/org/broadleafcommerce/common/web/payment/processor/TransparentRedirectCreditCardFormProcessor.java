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
import org.broadleafcommerce.common.web.dialect.BLCDialect;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

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
        super(TemplateMode.HTML, BLCDialect.DEFAULT_PREFIX, "transparent_credit_card_form", true, null, false, 1);
    }

    /**
     * Migrated from the removed Thymeleaf 2 DOM-based {@code AbstractElementProcessor}. The custom element is renamed to
     * a standard {@code <form>} (preserving its body) by mutating the Thymeleaf 3 {@link IModel}; the gateway hidden
     * inputs are inserted as standalone tags before the closing tag.
     */
    @Override
    protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
        final IModelFactory modelFactory = context.getModelFactory();
        final IProcessableElementTag openTag = (IProcessableElementTag) model.get(0);
        final Map<String, String> attributes = new LinkedHashMap<String, String>(openTag.getAttributeMap());

        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, attributes.get("paymentRequestDTO"));
        PaymentRequestDTO requestDTO = (PaymentRequestDTO) expression.execute(context);

        attributes.remove("paymentRequestDTO");

        Map<String, Map<String,String>> formParameters = new HashMap<String, Map<String,String>>();
        Map<String, String> configurationSettings = new HashMap<String, String>();

        //Create the configuration settings map to pass into the payment module
        List<String> keysToRemove = new ArrayList<String>();
        for (String key : attributes.keySet()) {
            if (key.startsWith("config-")){
                final int trimLength = "config-".length();
                String configParam = key.substring(trimLength);
                configurationSettings.put(configParam, attributes.get(key));
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
        Map<String,String> actionValue = formParameters.get(formActionKey.toString());
        if (actionValue != null && actionValue.size()>0) {
            String key = (String)actionValue.keySet().toArray()[0];
            actionUrl = actionValue.get(key);
        }
        attributes.put("action", actionUrl);

        // Convert the <blc:transparent_credit_card_form> node to a normal <form> node, preserving the body
        IOpenElementTag newOpenTag = modelFactory.createOpenElementTag("form", attributes, AttributeValueQuotes.DOUBLE, false);
        model.replace(0, newOpenTag);
        if (model.size() > 1 && model.get(model.size() - 1) instanceof ICloseElementTag) {
            ICloseElementTag newCloseTag = modelFactory.createCloseElementTag("form");
            model.replace(model.size() - 1, newCloseTag);
        }

        //Append any hidden fields necessary for the Transparent Redirect
        Map<String, String> hiddenFields = formParameters.get(formHiddenParamsKey.toString());
        if (hiddenFields != null && !hiddenFields.isEmpty()) {
            int insertIndex = model.size() - 1;
            for (String key : hiddenFields.keySet()) {
                Map<String, String> inputAttributes = new LinkedHashMap<String, String>();
                inputAttributes.put("type", "hidden");
                inputAttributes.put("name", key);
                inputAttributes.put("value", hiddenFields.get(key));
                IStandaloneElementTag hiddenNode = modelFactory.createStandaloneElementTag("input", inputAttributes, AttributeValueQuotes.DOUBLE, false, true);
                model.insert(insertIndex, hiddenNode);
                insertIndex++;
            }
        }
    }

    public TRCreditCardExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public void setExtensionManager(TRCreditCardExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }
}
