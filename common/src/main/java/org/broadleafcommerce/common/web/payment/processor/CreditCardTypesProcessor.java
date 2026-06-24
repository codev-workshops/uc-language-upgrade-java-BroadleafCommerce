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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The following processor will add any Payment Gateway specific Card Type 'codes' to the model if
 * the gateway requires that a 'Card Type' (e.g. Visa, MasterCard, etc...) be sent along with
 * the credit card number and expiry date.
 * </p>
 *
 * <p>This processor will put the key 'paymentGatewayCardTypes' on the model if there are any types available</p>
 *
 * <p>Here is an example:</p>
 *
 * <pre><code>
 *  <blc:credit_card_types >
 *      <div th:if="${paymentGatewayCardTypes != null}" class="form-group">
 *          <label for="cardNumber">Card Type</label>
 *          <select th:name="${#paymentGatewayField.mapName('creditCard.creditCardType')}">
 *              <option th:each="entry : ${paymentGatewayCardTypes}" th:value="${entry.key}" th:text="${entry.value}"></option>
 *          </select>
 *      </div>
 *  </blc:credit_card_types>
 * </code></pre>
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Component("blCreditCardTypesProcessor")
public class CreditCardTypesProcessor extends AbstractElementTagProcessor {

    protected static final Log LOG = LogFactory.getLog(CreditCardTypesProcessor.class);

    @Resource(name = "blCreditCardTypesExtensionManager")
    protected CreditCardTypesExtensionManager extensionManager;

    public CreditCardTypesProcessor() {
        super(TemplateMode.HTML, "blc", "credit_card_types", true, null, false, 100);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) {
        Map<String, String> creditCardTypes = new HashMap<String, String>();

        try {
            extensionManager.getProxy().populateCreditCardMap(creditCardTypes);
        } catch (Exception e) {
            LOG.warn("Unable to Populate Credit Card Types Map for this Payment Module, or card type is not needed.");
        }

        if (!creditCardTypes.isEmpty()) {
            structureHandler.setLocalVariable("paymentGatewayCardTypes", creditCardTypes);
        }

        // Drop the custom host tag but keep the body, exposing the contributed variable to it
        structureHandler.removeTags();
    }

}
