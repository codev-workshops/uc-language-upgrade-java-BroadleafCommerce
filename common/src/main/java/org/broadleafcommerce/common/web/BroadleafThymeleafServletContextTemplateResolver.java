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
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import java.util.Map;

/**
 * Overrides the Thymeleaf template resolver and appends the {@link org.broadleafcommerce.common.web.Theme} path to the
 * url if it exists.
 *
 * <p>Thymeleaf 3.1 removed the servlet-coupled {@code ServletContextTemplateResolver} and the
 * {@code computeResourceName(TemplateProcessingParameters)} hook. This resolver now extends the Spring-aware
 * {@link SpringResourceTemplateResolver} and customizes the Thymeleaf 3 {@code computeResourceName(...)} method by
 * injecting the theme path (and template folder) into the configured prefix before delegating to the superclass.
 */
public class BroadleafThymeleafServletContextTemplateResolver extends SpringResourceTemplateResolver {

    protected String templateFolder = "";

    @Override
    protected String computeResourceName(IEngineConfiguration configuration, String ownerTemplate, String template,
            String prefix, String suffix, boolean forceSuffix, Map<String, String> templateAliases,
            Map<String, Object> templateResolutionAttributes) {

        String themePath = null;

        Theme theme = BroadleafRequestContext.getBroadleafRequestContext().getTheme();
        if (theme != null && theme.getPath() != null) {
            themePath = theme.getPath();
        }

        if (themePath != null && prefix != null && !prefix.trim().equals("")) {
            String themedPrefix = prefix + themePath + '/' + templateFolder;
            return super.computeResourceName(configuration, ownerTemplate, template, themedPrefix, suffix, forceSuffix,
                    templateAliases, templateResolutionAttributes);
        }

        return super.computeResourceName(configuration, ownerTemplate, template, prefix, suffix, forceSuffix,
                templateAliases, templateResolutionAttributes);
    }

    public String getTemplateFolder() {
        return templateFolder;
    }

    public void setTemplateFolder(String templateFolder) {
        this.templateFolder = templateFolder;
    }

}
