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
package org.broadleafcommerce.common.extensibility.jpa.copy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Verifies that {@link DirectCopyClassTransformer} weaves fields, methods and interfaces from a
 * template class into a target class on the Java 17 / javassist 3.30 stack. This exercises the
 * core load-time-weaving behaviour of the Broadleaf class-transformer pipeline that the Hibernate
 * 6 migration depends on.
 *
 * @author Devin (java17 Phase 1)
 */
public class DirectCopyClassTransformerTest {

    private byte[] readClassBytes(Class<?> clazz) throws Exception {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("Unable to locate compiled bytecode for " + clazz.getName(), is);
            return is.readAllBytes();
        }
    }

    /**
     * A child-first style loader that defines a single class from a supplied byte array, delegating
     * everything else (template class, interface, java.* types) to the parent loader.
     */
    private static final class ByteArrayClassLoader extends ClassLoader {

        private ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    @Test
    public void testFieldMethodAndInterfaceAreWoven() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("test");
        Map<String, String> xformTemplates = new HashMap<String, String>();
        xformTemplates.put(DirectCopyTransformTestTarget.class.getName(),
                DirectCopyTransformTestTemplate.class.getName());
        transformer.setXformTemplates(xformTemplates);

        String internalName = DirectCopyTransformTestTarget.class.getName().replace('.', '/');
        byte[] original = readClassBytes(DirectCopyTransformTestTarget.class);
        byte[] transformed = transformer.transform(getClass().getClassLoader(), internalName, null, null, original);

        assertNotNull("Transformer should return modified bytecode for a targeted class", transformed);

        ByteArrayClassLoader loader = new ByteArrayClassLoader(getClass().getClassLoader());
        Class<?> woven = loader.define(DirectCopyTransformTestTarget.class.getName(), transformed);
        Object instance = woven.getDeclaredConstructor().newInstance();

        // Field copied from the template
        Field wovenField = woven.getDeclaredField("wovenField");
        assertEquals(Long.class, wovenField.getType());

        // Method copied from the template
        Method wovenMethod = woven.getDeclaredMethod("wovenMethod");
        assertEquals("woven", wovenMethod.invoke(instance));

        // Interface copied from the template
        assertTrue("Target should now implement the template's interface",
                DirectCopyTransformTestMarker.class.isAssignableFrom(woven));
        Method interfaceMethod = woven.getDeclaredMethod("wovenInterfaceMethod");
        assertEquals("interface", interfaceMethod.invoke(instance));

        // Existing members preserved
        assertEquals("existing", woven.getDeclaredMethod("getExistingField").invoke(instance));
    }

    @Test
    public void testNullClassNameIsIgnored() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("test");
        assertNull("A null class name (e.g. a lambda) must be skipped",
                transformer.transform(getClass().getClassLoader(), null, null, null, new byte[0]));
    }

    @Test
    public void testClassNotInTemplatesIsNotTransformed() throws Exception {
        DirectCopyClassTransformer transformer = new DirectCopyClassTransformer("test");
        Map<String, String> xformTemplates = new HashMap<String, String>();
        xformTemplates.put("com.example.NotTheTargetClass", DirectCopyTransformTestTemplate.class.getName());
        transformer.setXformTemplates(xformTemplates);

        String internalName = DirectCopyTransformTestTarget.class.getName().replace('.', '/');
        byte[] original = readClassBytes(DirectCopyTransformTestTarget.class);
        byte[] result = transformer.transform(getClass().getClassLoader(), internalName, null, null, original);

        assertNull("A class absent from xformTemplates should be left untouched", result);
    }
}
