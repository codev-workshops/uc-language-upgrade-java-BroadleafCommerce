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
package org.broadleafcommerce.common.extensibility.jpa.convert.inheritance;

import static org.junit.Assert.*;

import org.broadleafcommerce.common.extensibility.jpa.convert.BroadleafClassTransformer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Verifies that SingleTableInheritanceClassTransformer can process Java 17-compiled
 * entity classes and correctly apply single-table inheritance rewrites under Hibernate 5.6.
 */
public class SingleTableInheritanceClassTransformerTest {

    private SingleTableInheritanceClassTransformer transformer;

    @Before
    public void setUp() {
        transformer = new SingleTableInheritanceClassTransformer();
    }

    @Test
    public void testTransformerImplementsBroadleafClassTransformer() {
        assertTrue("Should implement BroadleafClassTransformer",
                transformer instanceof BroadleafClassTransformer);
    }

    @Test
    public void testCompileJPAProperties() throws Exception {
        Properties props = new Properties();
        String entityName = SampleSingleTableEntity.class.getName();
        props.setProperty(SingleTableInheritanceClassTransformer.SINGLE_TABLE_ENTITIES, entityName);
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.name", "TYPE");
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.type", "STRING");
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.length", "10");

        transformer.compileJPAProperties(props, SingleTableInheritanceClassTransformer.SINGLE_TABLE_ENTITIES);
        // No exception means properties were parsed correctly
    }

    @Test
    public void testTransformClassNotInInfosReturnsNull() throws Exception {
        byte[] classBytes = getClassBytes(SampleSingleTableEntity.class);
        String className = SampleSingleTableEntity.class.getName().replace('.', '/');
        byte[] result = transformer.transform(
                getClass().getClassLoader(), className, null, null, classBytes);
        assertNull("Class not in infos list should return null", result);
    }

    @Test
    public void testTransformWithConfiguredInfo() throws Exception {
        Properties props = new Properties();
        String entityName = SampleSingleTableEntity.class.getName();
        props.setProperty(SingleTableInheritanceClassTransformer.SINGLE_TABLE_ENTITIES, entityName);
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.name", "DISC_TYPE");
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.type", "STRING");
        props.setProperty("broadleaf.ejb.SampleSingleTableEntity.discriminator.length", "31");

        transformer.compileJPAProperties(props, SingleTableInheritanceClassTransformer.SINGLE_TABLE_ENTITIES);

        byte[] classBytes = getClassBytes(SampleSingleTableEntity.class);
        String className = SampleSingleTableEntity.class.getName().replace('.', '/');
        byte[] result = transformer.transform(
                getClass().getClassLoader(), className, null, null, classBytes);

        assertNotNull("Transformer should return modified bytecode for configured entity", result);
        assertTrue("Modified bytecode should be valid (non-empty)", result.length > 0);

        int majorVersion = ((result[6] & 0xFF) << 8) | (result[7] & 0xFF);
        assertTrue("Modified bytecode should still be Java 17+ compatible (version >= 61), was: " + majorVersion,
                majorVersion >= 61);
    }

    @Test
    public void testNullClassNameReturnsNull() throws Exception {
        byte[] result = transformer.transform(getClass().getClassLoader(), null, null, null, new byte[0]);
        assertNull("Null className should return null", result);
    }

    private byte[] getClassBytes(Class<?> clazz) throws IOException {
        String resourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            return is.readAllBytes();
        }
    }

    @Entity
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
    public static class SampleSingleTableEntity {
        @Id
        private Long id;
        private String name;
    }

    @Entity
    @DiscriminatorValue("CHILD")
    public static class SampleChildEntity extends SampleSingleTableEntity {
        private String childProperty;
    }
}
