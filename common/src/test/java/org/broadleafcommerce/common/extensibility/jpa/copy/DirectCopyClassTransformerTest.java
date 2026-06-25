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

import static org.junit.Assert.*;

import org.broadleafcommerce.common.extensibility.jpa.convert.BroadleafClassTransformer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Verifies that DirectCopyClassTransformer can handle Java 17-compiled classes
 * (class file version 61) with javassist 3.29.x.
 */
public class DirectCopyClassTransformerTest {

    private DirectCopyClassTransformer transformer;

    @Before
    public void setUp() {
        transformer = new DirectCopyClassTransformer("testModule");
    }

    @Test
    public void testTransformerImplementsBroadleafClassTransformer() {
        assertTrue("DirectCopyClassTransformer should implement BroadleafClassTransformer",
                transformer instanceof BroadleafClassTransformer);
    }

    @Test
    public void testNullClassNameReturnsNull() throws Exception {
        byte[] result = transformer.transform(getClass().getClassLoader(), null, null, null, new byte[0]);
        assertNull("Null className should return null", result);
    }

    @Test
    public void testJava17BytecodeCanBeParsed() throws Exception {
        byte[] classBytes = getClassBytes(SampleTargetEntity.class);
        assertNotNull("Should be able to load class bytes", classBytes);
        int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
        assertTrue("Should be compiled to Java 17+ bytecode (version >= 61), was: " + majorVersion,
                majorVersion >= 61);

        javassist.ClassPool pool = javassist.ClassPool.getDefault();
        javassist.CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(classBytes));
        assertNotNull("Javassist should be able to parse Java 17 class bytes", ctClass);
        assertEquals("Should read the correct class name",
                SampleTargetEntity.class.getName(), ctClass.getName());
        ctClass.detach();
    }

    @Test
    public void testXformTemplatesConfiguration() {
        Map<String, String> xformTemplates = new HashMap<>();
        xformTemplates.put("com.example.Target", "com.example.Template");
        transformer.setXformTemplates(xformTemplates);
        assertEquals("xformTemplates should be set", xformTemplates, transformer.getXformTemplates());
    }

    private byte[] getClassBytes(Class<?> clazz) throws IOException {
        String resourceName = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            return is.readAllBytes();
        }
    }

    @javax.persistence.Entity
    public static class SampleTargetEntity {
        @javax.persistence.Id
        private Long id;
        private String name;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
