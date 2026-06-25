/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2024 Broadleaf Commerce
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
package org.broadleafcommerce.core.web.processor;

import static org.junit.Assert.*;

import org.broadleafcommerce.core.web.api.BroadleafRestApiMvcConfiguration;
import org.broadleafcommerce.core.web.resolver.DatabaseResourceResolver;
import org.broadleafcommerce.core.web.resolver.DatabaseTemplateResolver;
import org.junit.Test;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Verifies that the Spring 5.3 MVC migration compiles correctly:
 * - WebMvcConfigurerAdapter → WebMvcConfigurer
 * - Template resolver classes use TL3 APIs
 */
public class SpringMvcMigrationTest {

    @Test
    public void testBroadleafRestApiMvcConfigurationImplementsWebMvcConfigurer() {
        BroadleafRestApiMvcConfiguration config = new BroadleafRestApiMvcConfiguration();
        assertTrue("BroadleafRestApiMvcConfiguration should implement WebMvcConfigurer",
                config instanceof WebMvcConfigurer);
    }

    @Test
    public void testDatabaseTemplateResolverExtendsStringTemplateResolver() {
        DatabaseTemplateResolver resolver = new DatabaseTemplateResolver();
        assertTrue("DatabaseTemplateResolver should extend StringTemplateResolver",
                resolver instanceof StringTemplateResolver);
    }

    @Test
    public void testDatabaseResourceResolverCanBeInstantiated() {
        DatabaseResourceResolver resolver = new DatabaseResourceResolver();
        assertNotNull("DatabaseResourceResolver should be instantiable", resolver);
        assertEquals("BL_DATABASE", resolver.getName());
    }
}
