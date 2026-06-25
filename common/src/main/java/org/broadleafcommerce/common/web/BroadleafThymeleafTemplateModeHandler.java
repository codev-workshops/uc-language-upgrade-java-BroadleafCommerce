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
package org.broadleafcommerce.common.web;

import org.thymeleaf.templatemode.TemplateMode;

/**
 * In Thymeleaf 3, template mode handlers are no longer a separate concept.
 * This class is retained for backward compatibility and wraps a TemplateMode enum value.
 */
public class BroadleafThymeleafTemplateModeHandler {

    private final TemplateMode templateMode;

    public BroadleafThymeleafTemplateModeHandler(TemplateMode templateMode) {
        this.templateMode = templateMode;
    }

    public TemplateMode getTemplateMode() {
        return templateMode;
    }
}
