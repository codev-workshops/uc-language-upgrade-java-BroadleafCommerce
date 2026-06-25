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
package org.broadleafcommerce.common.extensibility.jpa;

import static org.junit.Assert.*;

import org.broadleafcommerce.common.extensibility.jpa.convert.BroadleafClassTransformer;
import org.junit.Test;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that MergePersistenceUnitManager is compatible with Spring 5.3's
 * DefaultPersistenceUnitManager and can be instantiated under Java 17.
 */
public class MergePersistenceUnitManagerTest {

    @Test
    public void testInstanceOfDefaultPersistenceUnitManager() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        assertTrue("MergePersistenceUnitManager should extend DefaultPersistenceUnitManager",
                manager instanceof DefaultPersistenceUnitManager);
    }

    @Test
    public void testClassTransformersListInitialized() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        assertNotNull("classTransformers should be initialized",
                manager.getClassTransformers());
    }

    @Test
    public void testSetAndGetClassTransformers() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        List<BroadleafClassTransformer> transformers = new ArrayList<>();
        manager.setClassTransformers(transformers);
        assertSame("Should return the same list that was set", transformers, manager.getClassTransformers());
    }

    @Test
    public void testJava17ClassFileVersion() {
        int javaVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        assertTrue("Tests should be running on Java 17+", javaVersion >= 17);

        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        assertNotNull("Manager should be creatable on Java 17", manager);
    }
}
