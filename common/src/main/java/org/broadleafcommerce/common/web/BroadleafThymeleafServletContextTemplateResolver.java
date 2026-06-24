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

import org.broadleafcommerce.common.site.domain.Theme;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.util.Validate;

import java.util.Map;

/**
 * Overrides the Thymeleaf template resolver and appends the org.broadleafcommerce.common.web.Theme path to the url
 * if it exists.
 *
 * <p>Migrated to the Thymeleaf 3 + Spring 6 stack. Thymeleaf 3 removed {@code ServletContextTemplateResolver} and the
 * {@code computeResourceName(TemplateProcessingParameters)} hook; this now extends the Spring-aware
 * {@link SpringResourceTemplateResolver} (which resolves templates through the Spring {@code ApplicationContext}'s
 * resource loading) and overrides the new {@code computeResourceName(...)} signature to inject the theme path.
 */
public class BroadleafThymeleafServletContextTemplateResolver extends SpringResourceTemplateResolver {
    
    protected String templateFolder = "";

    @Override
    protected String computeResourceName(final IEngineConfiguration configuration, final String ownerTemplate,
            final String template, final String prefix, final String suffix, final boolean forceSuffix,
            final Map<String, String> templateAliases, final Map<String, Object> templateResolutionAttributes) {
        String themePath = null;
    
        Theme theme = BroadleafRequestContext.getBroadleafRequestContext().getTheme();
        if (theme != null && theme.getPath() != null) {
            themePath = theme.getPath();
        }             

        Validate.notNull(template, "Template name cannot be null");

        String unaliasedName = templateAliases.get(template);
        if (unaliasedName == null) {
            unaliasedName = template;
        }

        final StringBuilder resourceName = new StringBuilder();
        if (prefix != null && ! prefix.trim().equals("")) {
           
            if (themePath != null) {        
                resourceName.append(prefix).append(themePath).append('/').append(templateFolder);
            }
        }
        resourceName.append(unaliasedName);
        if (suffix != null && ! suffix.trim().equals("")) {
            resourceName.append(suffix);
        }

        return resourceName.toString();
    }
    
    public String getTemplateFolder() {
        return templateFolder;
    }

    public void setTemplateFolder(String templateFolder) {
        this.templateFolder = templateFolder;
    }
    
}


