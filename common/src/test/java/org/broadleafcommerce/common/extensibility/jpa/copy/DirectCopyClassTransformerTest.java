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
package org.broadleafcommerce.common.extensibility.jpa.copy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Verifies that the {@link DirectCopyClassTransformer} weaving subsystem produces the expected enhanced/copied bytecode
 * on a small fixture when running on JDK 17 against javassist 3.29 / ASM 9.
 *
 * <p>This is one of the architecturally-significant subsystems adapted for the Java 17 / Hibernate 5.6 migration, so it
 * gets a focused unit test (the subsystem was previously largely untested).
 */
public class DirectCopyClassTransformerTest {

    private byte[] readClassBytes(Class<?> clazz) throws Exception {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (InputStream in = clazz.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("Could not locate class bytes for " + clazz.getName(), in);
            return in.readAllBytes();
        }
    }

    /**
     * The transformer copies the template's declared field, declared method and implemented interface into the target
     * class' bytecode, leaving the target's own members intact.
     */
    @Test
    public void testFieldMethodAndInterfaceAreWovenIntoTarget() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("weaveTest");

        Map<String, String> xformTemplates = new HashMap<String, String>();
        xformTemplates.put(WeaveTestTarget.class.getName(), WeaveTestTemplate.class.getName());
        transformer.setXformTemplates(xformTemplates);

        byte[] originalBytes = readClassBytes(WeaveTestTarget.class);
        String internalName = WeaveTestTarget.class.getName().replace('.', '/');

        byte[] transformedBytes = transformer.transform(getClass().getClassLoader(), internalName, null, null, originalBytes);

        assertNotNull("Transformer should have returned non-null bytecode for a mapped class", transformedBytes);
        assertFalse("Transformed bytecode should differ from the original", java.util.Arrays.equals(originalBytes, transformedBytes));

        // Parse the transformed bytecode in an isolated pool to inspect its structure
        ClassPool pool = new ClassPool(true);
        CtClass woven = pool.makeClass(new ByteArrayInputStream(transformedBytes));

        // Field copied from the template
        assertNotNull("weavedField should have been copied from the template", woven.getDeclaredField("weavedField"));
        // Original field preserved
        assertNotNull("existingField should be preserved on the target", woven.getDeclaredField("existingField"));

        // Method copied from the template
        assertNotNull("weavedMethod should have been copied from the template", woven.getDeclaredMethod("weavedMethod"));
        // Original method preserved
        assertNotNull("existingMethod should be preserved on the target", woven.getDeclaredMethod("existingMethod"));

        // Interface copied from the template
        boolean hasMarkerInterface = false;
        for (CtClass iface : woven.getInterfaces()) {
            if (WeaveTestMarker.class.getName().equals(iface.getName())) {
                hasMarkerInterface = true;
                break;
            }
        }
        assertTrue("WeaveTestMarker interface should have been copied from the template", hasMarkerInterface);
    }

    /**
     * A class that is not a key in a (non-empty) xformTemplates map is left untouched - the transformer returns
     * {@code null} so the original bytes are used.
     */
    @Test
    public void testUnmappedClassIsNotTransformed() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("weaveTest");
        // Non-empty map that does NOT contain the class under test, so the template-driven branch is taken but no
        // weaving occurs for this class.
        Map<String, String> xformTemplates = new HashMap<String, String>();
        xformTemplates.put("org.broadleafcommerce.common.extensibility.jpa.copy.SomeOtherClass",
                WeaveTestTemplate.class.getName());
        transformer.setXformTemplates(xformTemplates);

        byte[] originalBytes = readClassBytes(WeaveTestTarget.class);
        String internalName = WeaveTestTarget.class.getName().replace('.', '/');

        byte[] transformedBytes = transformer.transform(getClass().getClassLoader(), internalName, null, null, originalBytes);

        assertEquals("Unmapped class should not be transformed (null signals 'no change')", null, transformedBytes);
    }

    /**
     * A null class name (e.g. lambdas / anonymous synthetic classes) must be safely skipped.
     */
    @Test
    public void testNullClassNameIsSkipped() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("weaveTest");
        Map<String, String> xformTemplates = new HashMap<String, String>();
        xformTemplates.put(WeaveTestTarget.class.getName(), WeaveTestTemplate.class.getName());
        transformer.setXformTemplates(xformTemplates);

        byte[] result = transformer.transform(getClass().getClassLoader(), null, null, null, new byte[0]);
        assertEquals(null, result);
    }
}
