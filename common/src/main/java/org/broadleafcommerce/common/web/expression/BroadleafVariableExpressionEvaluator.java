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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;

/**
 * Provides a skeleton to register multiple {@link BroadleafVariableExpression} implementors as Thymeleaf
 * {@code #expression} objects.
 *
 * <p>Migrated to the Thymeleaf 3 SPI. Thymeleaf 3 removed the {@code spring4} {@code SpelVariableExpressionEvaluator}
 * and its {@code computeAdditionalExpressionObjects} hook; custom expression utility objects are now contributed via
 * {@link IExpressionObjectFactory} (registered through {@code IExpressionObjectDialect} on the {@code blc} dialect).
 * Each non-null, non-{@link NullBroadleafVariableExpression} expression is exposed under its
 * {@link BroadleafVariableExpression#getName()}.
 *
 * @author Andre Azzolini (apazzolini)
 */
public class BroadleafVariableExpressionEvaluator implements IExpressionObjectFactory {

    @Resource(name = "blVariableExpressions")
    protected List<BroadleafVariableExpression> expressions = new ArrayList<BroadleafVariableExpression>();

    protected Map<String, BroadleafVariableExpression> getExpressionsByName() {
        Map<String, BroadleafVariableExpression> map = new HashMap<String, BroadleafVariableExpression>();
        for (BroadleafVariableExpression expression : expressions) {
            if (!(expression instanceof NullBroadleafVariableExpression)) {
                map.put(expression.getName(), expression);
            }
        }
        return map;
    }

    @Override
    public Set<String> getAllExpressionObjectNames() {
        return new LinkedHashSet<String>(getExpressionsByName().keySet());
    }

    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
        return getExpressionsByName().get(expressionObjectName);
    }

    @Override
    public boolean isCacheable(String expressionObjectName) {
        return true;
    }

}
