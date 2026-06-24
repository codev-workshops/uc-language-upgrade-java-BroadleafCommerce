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
package org.broadleafcommerce.common.web.expression;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Resource;

/**
 * Provides a skeleton to register multiple {@link BroadleafVariableExpression} implementors.
 *
 * <p>Thymeleaf 2 exposed custom {@code #expression} objects by subclassing the SpEL variable expression evaluator
 * and overriding {@code computeAdditionalExpressionObjects}. Thymeleaf 3 removed that extension point in favour of
 * the {@link IExpressionObjectFactory} contributed by an {@code IExpressionObjectDialect}, so this class now
 * implements that factory and exposes each registered {@link BroadleafVariableExpression} as an expression object
 * keyed by its name.</p>
 *
 * @author Andre Azzolini (apazzolini)
 */
public class BroadleafVariableExpressionEvaluator implements IExpressionObjectFactory {

    @Resource(name = "blVariableExpressions")
    protected List<BroadleafVariableExpression> expressions = new ArrayList<BroadleafVariableExpression>();

    @Override
    public Set<String> getAllExpressionObjectNames() {
        Set<String> names = new HashSet<String>();
        for (BroadleafVariableExpression expression : expressions) {
            if (!(expression instanceof NullBroadleafVariableExpression)) {
                names.add(expression.getName());
            }
        }
        return names;
    }

    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
        for (BroadleafVariableExpression expression : expressions) {
            if (!(expression instanceof NullBroadleafVariableExpression)
                    && expression.getName().equals(expressionObjectName)) {
                return expression;
            }
        }
        return null;
    }

    @Override
    public boolean isCacheable(String expressionObjectName) {
        return true;
    }

}
