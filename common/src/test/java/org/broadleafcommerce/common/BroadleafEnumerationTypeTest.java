/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
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
package org.broadleafcommerce.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadleafcommerce.common.testsupport.ClassScanner;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Exercises the contract shared by every {@link BroadleafEnumerationType} in this module: the constants
 * declared on the type register themselves, {@code getInstance} resolves them back by their type key and
 * equality is driven purely by that key.
 */
public class BroadleafEnumerationTypeTest {

    private static List<Class<?>> enumerationTypes() {
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (Class<?> candidate : ClassScanner.classesUnder("org.broadleafcommerce")) {
            if (BroadleafEnumerationType.class.isAssignableFrom(candidate)
                    && !candidate.isInterface()
                    && !Modifier.isAbstract(candidate.getModifiers())
                    && !candidate.isEnum()) {
                types.add(candidate);
            }
        }
        return types;
    }

    @Test
    public void theModuleDeclaresEnumerationTypes() {
        assertFalse(enumerationTypes().isEmpty(), "expected the common module to declare enumeration types");
    }

    @TestFactory
    public List<DynamicTest> everyEnumerationTypeHonoursItsContract() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Class<?> type : enumerationTypes()) {
            tests.add(DynamicTest.dynamicTest(type.getSimpleName(), () -> verify(type)));
        }
        return tests;
    }

    private void verify(Class<?> type) throws Exception {
        Method getInstance = null;
        try {
            getInstance = type.getMethod("getInstance", String.class);
        } catch (NoSuchMethodException e) {
            // not every enumeration type exposes a lookup
        }

        int constants = 0;
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())
                    || !type.isAssignableFrom(field.getType())) {
                continue;
            }
            constants++;
            BroadleafEnumerationType constant = (BroadleafEnumerationType) field.get(null);
            assertNotNull(constant, type.getName() + "." + field.getName());
            assertNotNull(constant.getType(), type.getName() + "." + field.getName() + ".getType()");
            assertNotNull(constant.getFriendlyType(), type.getName() + "." + field.getName() + ".getFriendlyType()");

            assertEquals(constant, constant);
            assertFalse(constant.equals(null));
            assertFalse(constant.equals("not an enumeration type"));
            assertEquals(constant.hashCode(), constant.hashCode());

            if (getInstance != null) {
                assertSame(constant, getInstance.invoke(null, constant.getType()),
                        type.getName() + ".getInstance(\"" + constant.getType() + "\")");
            }
        }

        if (getInstance != null && constants > 0) {
            assertTrue(getInstance.invoke(null, "__no_such_type__") == null,
                    type.getName() + ".getInstance() should not resolve an unknown key");
        }

        if (constants > 0) {
            // the public no-arg constructor used by the persistence layer must stay available
            type.newInstance();
        }
    }
}
