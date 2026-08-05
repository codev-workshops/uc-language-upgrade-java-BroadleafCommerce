/*
 * #%L
 * broadleaf-theme
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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
package org.broadleafcommerce.core.web.resolver;

import org.broadleafcommerce.common.extension.ExtensionResultHolder;
import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.springframework.stereotype.Service;

import java.io.InputStream;

import jakarta.annotation.Resource;


/**
 * Resolves template contents from the database through an extension point.
 *
 * <p>Thymeleaf 3 removed the pluggable IResourceResolver abstraction, so this is now a plain service
 * consumed by {@link DatabaseTemplateResolver}.
 *
 * @author Andre Azzolini (apazzolini)
 */
@Service("blDatabaseResourceResolver")
public class DatabaseResourceResolver {

    @Resource(name = "blDatabaseResourceResolverExtensionManager")
    protected DatabaseResourceResolverExtensionManager extensionManager;

    /**
     * @return the contents of the named resource, or null when no handler resolved it
     */
    public InputStream getResourceAsStream(String resourceName) {
        ExtensionResultHolder erh = new ExtensionResultHolder();
        ExtensionResultStatusType result = extensionManager.getProxy().resolveResource(erh, resourceName);
        if (result == ExtensionResultStatusType.HANDLED) {
            return (InputStream) erh.getContextMap().get(DatabaseResourceResolverExtensionHandler.IS_KEY);
        }
        return null;
    }

}
