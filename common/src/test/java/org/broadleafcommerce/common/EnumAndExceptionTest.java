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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.broadleafcommerce.common.testsupport.BeanExerciser;
import org.broadleafcommerce.common.testsupport.ClassScanner;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoke coverage for the two families of tiny types in this module: the java enums (their constants, their
 * accessors and their {@code valueOf} contract) and the exceptions (every public constructor plus the message
 * and cause they expose).
 */
public class EnumAndExceptionTest {

    private static List<Class<?>> enums() {
        List<Class<?>> enums = new ArrayList<Class<?>>();
        for (Class<?> candidate : ClassScanner.classesUnder("org.broadleafcommerce")) {
            if (candidate.isEnum()) {
                enums.add(candidate);
            }
        }
        return enums;
    }

    private static List<Class<?>> exceptions() {
        List<Class<?>> exceptions = new ArrayList<Class<?>>();
        for (Class<?> candidate : ClassScanner.classesUnder("org.broadleafcommerce")) {
            if (Throwable.class.isAssignableFrom(candidate) && !Modifier.isAbstract(candidate.getModifiers())) {
                exceptions.add(candidate);
            }
        }
        return exceptions;
    }

    @Test
    public void enumsAndExceptionsWereFound() {
        assertFalse(enums().isEmpty(), "expected the common module to declare enums");
        assertFalse(exceptions().isEmpty(), "expected the common module to declare exceptions");
    }

    @TestFactory
    public List<DynamicTest> enumConstantsResolveByName() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Class<?> type : enums()) {
            tests.add(DynamicTest.dynamicTest(type.getName(), () -> verifyEnum(type)));
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> exceptionsExposeTheirMessageAndCause() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Class<?> type : exceptions()) {
            tests.add(DynamicTest.dynamicTest(type.getName(), () -> verifyException(type)));
        }
        return tests;
    }

    private void verifyEnum(Class<?> type) throws Exception {
        Object[] constants = type.getEnumConstants();
        Method valueOf = type.getMethod("valueOf", String.class);
        for (Object constant : constants) {
            Enum<?> value = (Enum<?>) constant;
            assertSame(constant, valueOf.invoke(null, value.name()));
            assertNotNull(value.toString());
            assertEquals(value.hashCode(), value.hashCode());

            for (Method reader : type.getDeclaredMethods()) {
                if (reader.getParameterTypes().length == 0 && !Modifier.isStatic(reader.getModifiers())
                        && reader.getReturnType() != void.class && Modifier.isPublic(reader.getModifiers())) {
                    reader.invoke(constant);
                }
            }
        }
    }

    private void verifyException(Class<?> type) throws Exception {
        boolean instantiated = false;
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                arguments[i] = parameterTypes[i] == String.class ? "boom"
                        : Throwable.class.isAssignableFrom(parameterTypes[i]) ? new IllegalStateException("cause")
                        : BeanExerciser.sampleValueFor(parameterTypes[i]);
            }
            Throwable thrown;
            try {
                thrown = (Throwable) constructor.newInstance(arguments);
            } catch (Exception e) {
                // constructors requiring collaborators we cannot build are covered by their own tests
                continue;
            }
            instantiated = true;
            thrown.getMessage();
            thrown.getCause();
            assertNotNull(thrown.toString());

            final Throwable toThrow = thrown;
            assertThrows(Throwable.class, () -> {
                throw toThrow;
            });
        }
        assertNotNull(type.getName());
        assertFalse(type.getConstructors().length > 0 && !instantiated,
                "no public constructor of " + type.getName() + " could be exercised");
    }
}
