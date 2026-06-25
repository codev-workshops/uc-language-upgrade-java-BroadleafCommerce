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
package org.broadleafcommerce.common.extensibility.jpa.convert;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Verifies that EntityMarkerClassTransformer can process Java 17-compiled entity classes
 * and correctly identify persistence annotations.
 */
public class EntityMarkerClassTransformerTest {

    private EntityMarkerClassTransformer transformer;

    @Before
    public void setUp() {
        transformer = new EntityMarkerClassTransformer();
    }

    @Test
    public void testTransformEntityClass() throws Exception {
        byte[] classBytes = getClassBytes(SampleEntity.class);
        assertNotNull("Should be able to read sample entity class bytes", classBytes);

        String className = SampleEntity.class.getName().replace('.', '/');
        byte[] result = transformer.transform(
                getClass().getClassLoader(), className, null, null, classBytes);

        assertNull("EntityMarkerClassTransformer returns null (bytecode is unchanged, it only marks)", result);
        assertTrue("Entity class should be tracked in transformedEntityClassNames",
                transformer.getTransformedEntityClassNames().contains(SampleEntity.class.getName()));
    }

    @Test
    public void testNonEntityClassNotMarked() throws Exception {
        byte[] classBytes = getClassBytes(NonEntityClass.class);
        String className = NonEntityClass.class.getName().replace('.', '/');
        byte[] result = transformer.transform(
                getClass().getClassLoader(), className, null, null, classBytes);
        assertNull("Non-entity class should return null", result);
        assertFalse("Non-entity class should not be in transformedEntityClassNames",
                transformer.getTransformedEntityClassNames().contains(NonEntityClass.class.getName()));
    }

    @Test
    public void testNullClassName() throws Exception {
        byte[] result = transformer.transform(getClass().getClassLoader(), null, null, null, new byte[0]);
        assertNull("Null className should return null", result);
    }

    @Test
    public void testJava17BytecodeVersion() throws Exception {
        byte[] classBytes = getClassBytes(SampleEntity.class);
        int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
        assertTrue("Sample entity should be compiled with Java 17+ bytecode (version >= 61), was: " + majorVersion,
                majorVersion >= 61);
    }

    private byte[] getClassBytes(Class<?> clazz) throws IOException {
        String resourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            return is.readAllBytes();
        }
    }

    @javax.persistence.Entity
    public static class SampleEntity {
        @javax.persistence.Id
        private Long id;
        private String name;
    }

    public static class NonEntityClass {
        private String value;
    }
}
