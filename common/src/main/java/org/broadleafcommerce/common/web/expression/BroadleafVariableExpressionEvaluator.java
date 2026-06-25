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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

/**
 * Provides a skeleton to register multiple {@link BroadleafVariableExpression} implementors as Thymeleaf expression
 * objects (e.g. {@code ${#props}}, {@code ${#brc}}).
 *
 * <p>Migrated from the removed Thymeleaf 2 {@code SpelVariableExpressionEvaluator#computeAdditionalExpressionObjects}.
 * Thymeleaf 3 contributes expression objects through {@link IExpressionObjectFactory} (wired into the dialect via
 * {@code org.thymeleaf.dialect.IExpressionObjectDialect}).
 * 
 * @author Andre Azzolini (apazzolini)
 */
public class BroadleafVariableExpressionEvaluator implements IExpressionObjectFactory {
    
    @Resource(name = "blVariableExpressions")
    protected List<BroadleafVariableExpression> expressions = new ArrayList<BroadleafVariableExpression>();

    @Override
    public Set<String> getAllExpressionObjectNames() {
        Set<String> names = new LinkedHashSet<String>();
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
