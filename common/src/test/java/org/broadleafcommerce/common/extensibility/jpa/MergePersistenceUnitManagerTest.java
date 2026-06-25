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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Focused tests for the architecturally-significant {@link MergePersistenceUnitManager}, which is the documented
 * exception to the "avoid reflection" rule: it reflects into Spring's {@link DefaultPersistenceUnitManager} private
 * fields/methods. These tests pin that reflection contract against the Spring version on the classpath (Spring 5.3.x for
 * the Java 17 migration) so a future Spring bump that renames an internal member fails loudly here, and verify that the
 * reflective {@code configureMergedItems()} path actually executes end-to-end on Spring 5.3.
 */
public class MergePersistenceUnitManagerTest {

    /**
     * Guards the four private fields {@link MergePersistenceUnitManager} reflects on in {@code configureMergedItems()}
     * and {@code preparePersistenceUnitInfos()}.
     */
    @Test
    public void testReflectedSuperclassFieldsExistWithExpectedTypes() throws Exception {
        Field persistenceXmlLocations = DefaultPersistenceUnitManager.class.getDeclaredField("persistenceXmlLocations");
        assertEquals(String[].class, persistenceXmlLocations.getType());

        Field persistenceUnitInfoNames = DefaultPersistenceUnitManager.class.getDeclaredField("persistenceUnitInfoNames");
        assertTrue("persistenceUnitInfoNames should be assignable to Set",
                Set.class.isAssignableFrom(persistenceUnitInfoNames.getType()));

        Field persistenceUnitInfos = DefaultPersistenceUnitManager.class.getDeclaredField("persistenceUnitInfos");
        assertTrue("persistenceUnitInfos should be assignable to Map",
                Map.class.isAssignableFrom(persistenceUnitInfos.getType()));

        Field resourcePatternResolver = DefaultPersistenceUnitManager.class.getDeclaredField("resourcePatternResolver");
        assertEquals(ResourcePatternResolver.class, resourcePatternResolver.getType());
    }

    /**
     * Guards the two private no-arg methods {@link MergePersistenceUnitManager} reflectively invokes.
     */
    @Test
    public void testReflectedSuperclassMethodsExist() throws Exception {
        Method readPersistenceUnitInfos = DefaultPersistenceUnitManager.class.getDeclaredMethod("readPersistenceUnitInfos");
        assertNotNull(readPersistenceUnitInfos);

        Method determineDefaultPersistenceUnitRootUrl =
                DefaultPersistenceUnitManager.class.getDeclaredMethod("determineDefaultPersistenceUnitRootUrl");
        assertNotNull(determineDefaultPersistenceUnitRootUrl);
    }

    /**
     * Guards the {@code init(...)} overloads reflectively invoked on each persistence unit info. {@code
     * readPersistenceUnitInfos()} returns package-private {@code SpringPersistenceUnitInfo} instances (a subclass of
     * {@link MutablePersistenceUnitInfo}), which is where the {@code init} overloads live - hence the by-name lookup.
     */
    @Test
    public void testPersistenceUnitInfoInitMethodsExist() throws Exception {
        Class<?> springPuiClass =
                Class.forName("org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo");
        assertNotNull(springPuiClass.getDeclaredMethod("init", LoadTimeWeaver.class));
        assertNotNull(springPuiClass.getDeclaredMethod("init", ClassLoader.class));
        // sanity: the persistence unit info hierarchy is a PersistenceUnitInfo / MutablePersistenceUnitInfo
        assertTrue(MutablePersistenceUnitInfo.class.isAssignableFrom(springPuiClass));
        assertTrue(PersistenceUnitInfo.class.isAssignableFrom(MutablePersistenceUnitInfo.class));
    }

    /**
     * Exercises the reflective read of the private {@code persistenceXmlLocations} field end-to-end: non-default
     * locations are merged in while the default {@code .../persistence.xml} location is filtered out.
     */
    @Test
    public void testConfigureMergedItemsReadsAndMergesPersistenceXmlLocationsReflectively() throws Exception {
        MergePersistenceUnitManager manager = new MergePersistenceUnitManager();
        manager.mergedPersistenceXmlLocations = new HashSet<String>();
        manager.mergedDataSources = new HashMap<String, DataSource>();

        String customLocation = "classpath*:/blMergedPersistenceXml-test.xml";
        manager.setPersistenceXmlLocations(customLocation, "classpath*:META-INF/persistence.xml");

        manager.configureMergedItems();

        Field persistenceXmlLocations = DefaultPersistenceUnitManager.class.getDeclaredField("persistenceXmlLocations");
        persistenceXmlLocations.setAccessible(true);
        String[] resolved = (String[]) persistenceXmlLocations.get(manager);

        assertArrayEquals("Only the non-default location should survive the merge",
                new String[] { customLocation }, resolved);
    }
}
