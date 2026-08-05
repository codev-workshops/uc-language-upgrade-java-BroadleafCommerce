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
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes the display values of the product options on an order item as a local variable to the body of the
 * element this processor is attached to.
 *
 * @author Priyesh Patel
 */
public class ProductOptionDisplayProcessor extends AbstractElementTagProcessor {

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public ProductOptionDisplayProcessor() {
        super(TemplateMode.HTML, "blc", "product_option_display", true, null, false, 100);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
                             IElementTagStructureHandler structureHandler) {
        for (Map.Entry<String, Object> variable : getNewLocalVariables(context, tag).entrySet()) {
            structureHandler.setLocalVariable(variable.getKey(), variable.getValue());
        }
    }

    protected Map<String, Object> getNewLocalVariables(ITemplateContext context, IProcessableElementTag tag) {
        HashMap<String, String> productOptionDisplayValues = new HashMap<String, String>();
        Map<String, Object> newVars = new HashMap<String, Object>();
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        IStandardExpression expression = parser.parseExpression(context, tag.getAttributeValue("orderItem"));
        Object item = expression.execute(context);
        if (item instanceof DiscreteOrderItem) {
            DiscreteOrderItem orderItem = (DiscreteOrderItem) item;

            for (String i : orderItem.getOrderItemAttributes().keySet()) {
                for (ProductOption option : orderItem.getProduct().getProductOptions()) {
                    if (option.getAttributeName().equals(i)
                            && !StringUtils.isEmpty(orderItem.getOrderItemAttributes().get(i).toString())) {
                        productOptionDisplayValues.put(option.getLabel(),
                                orderItem.getOrderItemAttributes().get(i).toString());
                    }
                }
            }
        }
        newVars.put("productOptionDisplayValues", productOptionDisplayValues);

        return newVars;
    }
}
