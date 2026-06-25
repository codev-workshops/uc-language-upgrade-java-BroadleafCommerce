/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.web.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.broadleafcommerce.common.web.dialect.BLCAdminDialect;
import org.junit.Test;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that the admin Thymeleaf dialect registers and processes templates under Thymeleaf 3.
 */
public class AdminThymeleafDialectTest {

    @Test
    public void testDialectRegistersWithThymeleaf3Engine() {
        BLCAdminDialect dialect = new BLCAdminDialect();
        assertNotNull(dialect);
        assertEquals("blc_admin", dialect.getPrefix());
        assertEquals("BLC Admin Dialect", dialect.getName());
    }

    @Test
    public void testDialectAcceptsProcessors() {
        BLCAdminDialect dialect = new BLCAdminDialect();
        Set<IProcessor> processors = new HashSet<>();

        AdminComponentIdProcessor componentIdProcessor = new AdminComponentIdProcessor();
        AdminFieldBuilderProcessor fieldBuilderProcessor = new AdminFieldBuilderProcessor();

        processors.add(componentIdProcessor);
        processors.add(fieldBuilderProcessor);
        dialect.setProcessors(processors);

        Set<IProcessor> registered = dialect.getProcessors("blc_admin");
        assertEquals(2, registered.size());
        assertTrue(registered.contains(componentIdProcessor));
        assertTrue(registered.contains(fieldBuilderProcessor));
    }

    @Test
    public void testProcessorsAreCorrectType() {
        AdminComponentIdProcessor componentIdProcessor = new AdminComponentIdProcessor();
        assertTrue(componentIdProcessor instanceof AbstractAttributeTagProcessor);

        AdminFieldBuilderProcessor fieldBuilderProcessor = new AdminFieldBuilderProcessor();
        assertTrue(fieldBuilderProcessor instanceof AbstractElementTagProcessor);

        AdminModuleProcessor moduleProcessor = new AdminModuleProcessor();
        assertTrue(moduleProcessor instanceof AbstractElementTagProcessor);

        AdminUserProcessor userProcessor = new AdminUserProcessor();
        assertTrue(userProcessor instanceof AbstractElementTagProcessor);

        AdminSectionHrefProcessor sectionHrefProcessor = new AdminSectionHrefProcessor();
        assertTrue(sectionHrefProcessor instanceof AbstractAttributeTagProcessor);

        ErrorsProcessor errorsProcessor = new ErrorsProcessor();
        assertTrue(errorsProcessor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testDialectPrefixAndProcessors() {
        BLCAdminDialect dialect = new BLCAdminDialect();

        Set<IProcessor> processors = new HashSet<>();
        processors.add(new AdminComponentIdProcessor());
        processors.add(new AdminFieldBuilderProcessor());
        processors.add(new AdminModuleProcessor());
        processors.add(new AdminUserProcessor());
        processors.add(new AdminSectionHrefProcessor());
        processors.add(new ErrorsProcessor());
        dialect.setProcessors(processors);

        Set<IProcessor> registered = dialect.getProcessors("blc_admin");
        assertEquals(6, registered.size());
    }
}
