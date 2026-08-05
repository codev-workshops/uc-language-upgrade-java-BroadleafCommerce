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

import org.apache.commons.io.IOUtils;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Resolves templates whose contents are stored in the database through a {@link DatabaseResourceResolver}.
 *
 * <p>Thymeleaf 3 resolves templates to an {@link ITemplateResource} rather than through a pluggable resource
 * resolver, so the database lookup happens here.
 *
 * @author Andre Azzolini (apazzolini)
 */
public class DatabaseTemplateResolver extends AbstractConfigurableTemplateResolver {

    protected DatabaseResourceResolver resourceResolver;

    @Override
    protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
                                                        String template, String resourceName,
                                                        String characterEncoding,
                                                        Map<String, Object> templateResolutionAttributes) {
        InputStream is = resourceResolver.getResourceAsStream(resourceName);
        if (is == null) {
            return null;
        }

        Charset charset = characterEncoding == null ? Charset.defaultCharset() : Charset.forName(characterEncoding);
        try {
            return new StringTemplateResource(IOUtils.toString(is, charset));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the database template " + resourceName, e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public DatabaseResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(DatabaseResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

}
