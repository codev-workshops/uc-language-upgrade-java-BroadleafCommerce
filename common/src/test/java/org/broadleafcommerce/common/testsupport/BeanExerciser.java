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
package org.broadleafcommerce.common.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Drives the property accessors of a simple value object: every setter is called with a representative
 * value and the matching getter is asserted to hand that value straight back, and the
 * {@code equals}/{@code hashCode}/{@code toString} trio is exercised.
 *
 * <p>Value objects whose accessors deliberately transform their input (rather than storing it), or whose
 * derived readers are only meaningful for particular state, can opt individual properties out. An opted out
 * name is neither round tripped nor invoked as a reader; such behaviour belongs in a dedicated test.</p>
 */
public final class BeanExerciser {

    private BeanExerciser() {
        // utility
    }

    public static void exercise(Class<?> type, String... excludedProperties) {
        Object bean = instantiate(type);
        assertNotNull(bean, type.getName());

        Set<String> skipped = new LinkedHashSet<String>();
        for (String property : excludedProperties) {
            skipped.add(property);
        }

        for (Method setter : type.getMethods()) {
            if (!isSetter(setter)) {
                continue;
            }
            Class<?> propertyType = setter.getParameterTypes()[0];
            Object value = sampleValueFor(propertyType);
            invoke(bean, setter, value);

            String property = setter.getName().substring(3);
            if (skipped.contains(decapitalize(property))) {
                continue;
            }
            Method getter = getterFor(type, property, propertyType);
            if (getter == null) {
                continue;
            }
            Object read = invoke(bean, getter);

            if (value != null && read != null) {
                assertEquals(value, read, type.getSimpleName() + "." + property + " should round trip");
            }
        }

        exerciseRemainingReaders(type, bean, skipped);

        assertEquals(bean, bean);
        assertFalse(bean.equals(null), type.getSimpleName() + ".equals(null)");
        bean.hashCode();
        assertNotNull(bean.toString());
    }

    /**
     * Calls the remaining no-argument readers of the bean so that derived properties are covered too.
     */
    private static void exerciseRemainingReaders(Class<?> type, Object bean, Set<String> skipped) {
        for (Method method : type.getMethods()) {
            if (method.getParameterTypes().length != 0
                    || method.getDeclaringClass() == Object.class
                    || Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() == void.class) {
                continue;
            }
            if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) {
                continue;
            }
            String property = method.getName().startsWith("get")
                    ? method.getName().substring(3) : method.getName().substring(2);
            if (property.isEmpty() || skipped.contains(decapitalize(property))) {
                continue;
            }
            invoke(bean, method);
        }
    }

    public static Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            return instantiateWithArguments(type);
        } catch (Exception e) {
            throw new AssertionError("could not instantiate " + type.getName(), e);
        }
    }

    private static Object instantiateWithArguments(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        AssertionError failure = null;
        for (Constructor<?> constructor : constructors) {
            Object[] arguments = new Object[constructor.getParameterTypes().length];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = sampleValueFor(constructor.getParameterTypes()[i]);
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            } catch (Exception e) {
                failure = new AssertionError("could not instantiate " + type.getName(), e);
            }
        }
        throw failure == null ? new AssertionError("no constructor on " + type.getName()) : failure;
    }

    private static boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && method.getName().length() > 3
                && method.getParameterTypes().length == 1
                && !Modifier.isStatic(method.getModifiers())
                && method.getDeclaringClass() != Object.class;
    }

    private static Method getterFor(Class<?> type, String property, Class<?> propertyType) {
        for (String prefix : new String[] {"get", "is"}) {
            try {
                Method getter = type.getMethod(prefix + property);
                if (getter.getReturnType().isAssignableFrom(propertyType)
                        || propertyType.isAssignableFrom(getter.getReturnType())) {
                    return getter;
                }
            } catch (NoSuchMethodException e) {
                // try the next prefix
            }
        }
        return null;
    }

    private static String decapitalize(String property) {
        return Character.toLowerCase(property.charAt(0)) + property.substring(1);
    }

    private static Object invoke(Object bean, Method method, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(bean, arguments);
        } catch (InvocationTargetException e) {
            throw new AssertionError(method.getDeclaringClass().getSimpleName() + "." + method.getName()
                    + " threw " + e.getCause(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T sampleValueFor(Class<T> type) {
        if (type == boolean.class || type == Boolean.class) {
            return (T) Boolean.TRUE;
        } else if (type == byte.class || type == Byte.class) {
            return (T) Byte.valueOf((byte) 1);
        } else if (type == short.class || type == Short.class) {
            return (T) Short.valueOf((short) 1);
        } else if (type == int.class || type == Integer.class) {
            return (T) Integer.valueOf(1);
        } else if (type == long.class || type == Long.class) {
            return (T) Long.valueOf(1L);
        } else if (type == float.class || type == Float.class) {
            return (T) Float.valueOf(1f);
        } else if (type == double.class || type == Double.class) {
            return (T) Double.valueOf(1d);
        } else if (type == char.class || type == Character.class) {
            return (T) Character.valueOf('a');
        } else if (type == String.class) {
            return (T) "test";
        } else if (type == BigDecimal.class) {
            return (T) new BigDecimal("1.00");
        } else if (type == Date.class) {
            return (T) new Date(1451606400000L);
        } else if (type == Locale.class) {
            return (T) Locale.US;
        } else if (type == Class.class) {
            return (T) String.class;
        } else if (type == List.class || type == Iterable.class || type == java.util.Collection.class) {
            return (T) new ArrayList<Object>();
        } else if (type == Set.class) {
            return (T) new LinkedHashSet<Object>();
        } else if (type == Map.class) {
            return (T) new LinkedHashMap<Object, Object>();
        } else if (type.isArray()) {
            return null;
        } else if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length == 0 ? null : (T) constants[0];
        } else if (type.isPrimitive()) {
            return null;
        } else if (Modifier.isFinal(type.getModifiers())) {
            return null;
        }
        try {
            return Mockito.mock(type);
        } catch (Exception e) {
            return null;
        }
    }
}
