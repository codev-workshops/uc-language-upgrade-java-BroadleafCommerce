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
 * Wrapper class around Thymeleaf's element processor that facilitates adding Objects
 * to the current evaluation context (model) for processing in the remainder of the page.
 *
 * <p>Migrated from the Thymeleaf 2 DOM-based {@code AbstractElementProcessor} to the Thymeleaf 3 event-based
 * {@link AbstractElementTagProcessor}. The custom element is processed by computing model variables and then
 * the wrapping tags are removed (their body, if any, is retained), with the computed variables exposed as
 * local variables to that body.</p>
 */
public abstract class AbstractModelVariableModifierProcessor extends AbstractElementTagProcessor {

    public static final String DIALECT_PREFIX = "blc";

    public AbstractModelVariableModifierProcessor(String elementName) {
        this(elementName, 1000);
    }

    public AbstractModelVariableModifierProcessor(String elementName, int precedence) {
        super(TemplateMode.HTML, DIALECT_PREFIX, elementName, true, null, false, precedence);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
        modifyModelAttributes(context, tag, structureHandler);
        // Remove the custom tag from the output while preserving any body content.
        structureHandler.removeTags();
    }

    /**
     * Helper method to add a value as a local variable available to the body of the processed element.
     * @param structureHandler the structure handler for the processed element
     * @param key the key to add to the model
     * @param value the value represented by the key
     */
    protected void addToModel(IElementTagStructureHandler structureHandler, String key, Object value) {
        structureHandler.setLocalVariable(key, value);
    }

    @SuppressWarnings("unchecked")
    protected <T> void addCollectionToExistingSet(ITemplateContext context, IElementTagStructureHandler structureHandler, String key, Collection<T> value) {
        Set<T> items = (Set<T>) context.getVariable(key);
        if (items == null) {
            items = new HashSet<T>();
        }
        items.addAll(value);
        structureHandler.setLocalVariable(key, items);
    }

    @SuppressWarnings("unchecked")
    protected <T> void addItemToExistingSet(ITemplateContext context, IElementTagStructureHandler structureHandler, String key, Object value) {
        Set<T> items = (Set<T>) context.getVariable(key);
        if (items == null) {
            items = new HashSet<T>();
        }
        items.add((T) value);
        structureHandler.setLocalVariable(key, items);
    }

    /**
     * This method must be overridden by a processor that wishes to modify the model. It will
     * be called by this abstract processor in the correct precedence in the evaluation chain.
     * @param context the template context
     * @param tag the element tag being processed
     * @param structureHandler the structure handler used to expose model variables and modify output
     */
    protected abstract void modifyModelAttributes(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler);
}
