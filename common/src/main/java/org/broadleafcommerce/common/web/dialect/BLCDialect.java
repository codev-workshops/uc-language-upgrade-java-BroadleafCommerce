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
package org.broadleafcommerce.common.web.dialect;

import org.broadleafcommerce.common.web.expression.BroadleafVariableExpressionEvaluator;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Resource;

public class BLCDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

    public static final String NAME = "Broadleaf Commerce Dialect";
    public static final String PREFIX = "blc";

    private Set<IProcessor> processors = new HashSet<IProcessor>();

    @Resource(name = "blVariableExpressionEvaluator")
    private BroadleafVariableExpressionEvaluator expressionEvaluator;

    public BLCDialect() {
        super(NAME, PREFIX, 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return processors;
    }

    public void setProcessors(Set<IProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return expressionEvaluator;
    }

}
