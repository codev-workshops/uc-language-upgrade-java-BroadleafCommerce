/*
 * #%L
 * BroadleafCommerce Common Libraries
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
package org.broadleafcommerce.common.extensibility.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

import java.util.Properties;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Unit tests for the merge semantics of {@link MergePersistenceUnitManager}. Multiple
 * persistence.xml files each declare the same persistence-unit name ("blPU") and are merged into a
 * single {@link PersistenceUnitInfo}. The reflective Spring-3 implementation was replaced during
 * the Java 17 / Spring 6 migration by a delegation to the superclass plus the merge accumulation
 * performed in {@code postProcessPersistenceUnitInfo}; these tests pin down that accumulation
 * behaviour.
 *
 * <p>The test lives in the same package as the class under test so that it can drive the
 * {@code protected} merge hooks directly without reflection.</p>
 *
 * @author Devin (java17 Phase 1)
 */
public class MergePersistenceUnitManagerTest {

    private static final String PU_NAME = "blPU";

    private MutablePersistenceUnitInfo newUnit(String name) {
        MutablePersistenceUnitInfo pu = new MutablePersistenceUnitInfo();
        pu.setPersistenceUnitName(name);
        pu.setProperties(new Properties());
        return pu;
    }

    @Test
    public void testManagedClassNamesAreMergedWithoutDuplicates() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();

        MutablePersistenceUnitInfo first = newUnit(PU_NAME);
        first.addManagedClassName("com.example.A");
        first.addManagedClassName("com.example.B");

        MutablePersistenceUnitInfo second = newUnit(PU_NAME);
        second.addManagedClassName("com.example.B"); // duplicate across units
        second.addManagedClassName("com.example.C");

        manager.postProcessPersistenceUnitInfo(first);
        manager.postProcessPersistenceUnitInfo(second);

        PersistenceUnitInfo merged = manager.obtainPersistenceUnitInfo(PU_NAME);
        assertEquals(3, merged.getManagedClassNames().size());
        assertTrue(merged.getManagedClassNames().contains("com.example.A"));
        assertTrue(merged.getManagedClassNames().contains("com.example.B"));
        assertTrue(merged.getManagedClassNames().contains("com.example.C"));
    }

    @Test
    public void testMappingFileNamesAndPropertiesAreMerged() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();

        MutablePersistenceUnitInfo first = newUnit(PU_NAME);
        first.addMappingFileName("mapping-one.xml");
        first.getProperties().setProperty("hibernate.show_sql", "true");

        MutablePersistenceUnitInfo second = newUnit(PU_NAME);
        second.addMappingFileName("mapping-one.xml"); // duplicate
        second.addMappingFileName("mapping-two.xml");
        second.getProperties().setProperty("hibernate.format_sql", "false");

        manager.postProcessPersistenceUnitInfo(first);
        manager.postProcessPersistenceUnitInfo(second);

        PersistenceUnitInfo merged = manager.obtainPersistenceUnitInfo(PU_NAME);
        assertEquals(2, merged.getMappingFileNames().size());
        assertTrue(merged.getMappingFileNames().contains("mapping-one.xml"));
        assertTrue(merged.getMappingFileNames().contains("mapping-two.xml"));

        assertEquals("true", merged.getProperties().getProperty("hibernate.show_sql"));
        assertEquals("false", merged.getProperties().getProperty("hibernate.format_sql"));
    }

    @Test
    public void testGetMergedUnitReturnsSameInstanceForRepeatedName() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();

        MutablePersistenceUnitInfo first = newUnit(PU_NAME);
        MutablePersistenceUnitInfo second = newUnit(PU_NAME);

        MutablePersistenceUnitInfo merged1 = manager.getMergedUnit(PU_NAME, first);
        MutablePersistenceUnitInfo merged2 = manager.getMergedUnit(PU_NAME, second);

        assertSame("The first unit registered for a name is the canonical merged unit",
                first, merged1);
        assertSame("A subsequent unit with the same name resolves to the original merged unit",
                first, merged2);
        assertFalse(second == merged2);
    }

    @Test
    public void testSeparatePersistenceUnitsAreKeptDistinct() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();

        MutablePersistenceUnitInfo blPu = newUnit("blPU");
        blPu.addManagedClassName("com.example.A");
        MutablePersistenceUnitInfo blEventPu = newUnit("blEventPU");
        blEventPu.addManagedClassName("com.example.Event");

        manager.postProcessPersistenceUnitInfo(blPu);
        manager.postProcessPersistenceUnitInfo(blEventPu);

        assertEquals(1, manager.obtainPersistenceUnitInfo("blPU").getManagedClassNames().size());
        assertEquals("com.example.Event",
                manager.obtainPersistenceUnitInfo("blEventPU").getManagedClassNames().get(0));
    }

    @Test
    public void testObtainDefaultPersistenceUnitInfoIsUnsupported() {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        try {
            manager.obtainDefaultPersistenceUnitInfo();
            fail("A default persistence unit must not be supported; the unit name is required");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
