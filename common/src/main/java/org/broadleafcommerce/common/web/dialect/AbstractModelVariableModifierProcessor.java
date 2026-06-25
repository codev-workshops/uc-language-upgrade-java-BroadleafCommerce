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

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author apazzolini
 * 
 * Wrapper class around Thymeleaf's AbstractElementTagProcessor that facilitates adding Objects
 * to the current evaluation context (model) for processing in the remainder of the page.
 *
 */
public abstract class AbstractModelVariableModifierProcessor extends AbstractElementTagProcessor {
    
    public AbstractModelVariableModifierProcessor(String dialectPrefix, String elementName) {
        super(TemplateMode.HTML, dialectPrefix, elementName, true, null, false, 1000);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        modifyModelAttributes(context, tag, structureHandler);
        structureHandler.removeElement();
    }
    
    protected void addToModel(IElementTagStructureHandler structureHandler, String key, Object value) {
        structureHandler.setLocalVariable(key, value);
    }
    
    @SuppressWarnings("unchecked")
    protected <T> void addCollectionToExistingSet(ITemplateContext context, IElementTagStructureHandler structureHandler, String key, Collection<T> value) {
        Object existing = context.getVariable(key);
        Set<T> items;
        if (existing instanceof Set) {
            items = (Set<T>) existing;
        } else {
            items = new HashSet<T>();
        }
        items.addAll(value);
        structureHandler.setLocalVariable(key, items);
    }

    @SuppressWarnings("unchecked")
    protected <T> void addItemToExistingSet(ITemplateContext context, IElementTagStructureHandler structureHandler, String key, Object value) {
        Object existing = context.getVariable(key);
        Set<T> items;
        if (existing instanceof Set) {
            items = (Set<T>) existing;
        } else {
            items = new HashSet<T>();
        }
        items.add((T) value);
        structureHandler.setLocalVariable(key, items);
    }
    
    protected abstract void modifyModelAttributes(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler);
}
