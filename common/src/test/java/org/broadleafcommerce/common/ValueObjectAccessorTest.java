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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.broadleafcommerce.common.testsupport.BeanExerciser;
import org.broadleafcommerce.common.testsupport.ClassScanner;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Property level smoke coverage for the entity, DTO and wrapper value objects of the common module: every
 * setter is driven with a representative value and asserted to be handed back by its getter, and the
 * derived read-only accessors are called so that they are exercised at least once.
 */
public class ValueObjectAccessorTest {

    private static final List<String> VALUE_OBJECT_PACKAGES = Arrays.asList(
            "org.broadleafcommerce.common.admin.domain.",
            "org.broadleafcommerce.common.audit.",
            "org.broadleafcommerce.common.value.",
            "org.broadleafcommerce.common.rule.",
            "org.broadleafcommerce.common.notification.service.type.",
            "org.broadleafcommerce.common.persistence.",
            "org.broadleafcommerce.common.presentation.override.",
            "org.broadleafcommerce.common.security.util.",
            "org.broadleafcommerce.common.template.",
            "org.broadleafcommerce.common.breadcrumbs.dto.",
            "org.broadleafcommerce.common.config.domain.",
            "org.broadleafcommerce.common.currency.domain.",
            "org.broadleafcommerce.common.email.domain.",
            "org.broadleafcommerce.common.email.service.info.",
            "org.broadleafcommerce.common.entity.dto.",
            "org.broadleafcommerce.common.enumeration.domain.",
            "org.broadleafcommerce.common.file.domain.",
            "org.broadleafcommerce.common.i18n.domain.",
            "org.broadleafcommerce.common.locale.domain.",
            "org.broadleafcommerce.common.media.domain.",
            "org.broadleafcommerce.common.page.dto.",
            "org.broadleafcommerce.common.payment.dto.",
            "org.broadleafcommerce.common.sandbox.domain.",
            "org.broadleafcommerce.common.site.domain.",
            "org.broadleafcommerce.common.sitemap.domain.",
            "org.broadleafcommerce.common.sitemap.wrapper.",
            "org.broadleafcommerce.common.structure.dto.",
            "org.broadleafcommerce.common.time.domain."
    );

    /**
     * Properties whose accessors deliberately do not round trip; each is covered by a dedicated test instead.
     */
    private static final Map<String, String[]> EXCLUDED_PROPERTIES = new HashMap<String, String[]>();

    static {
        // setFilePathLocation() normalises the path by appending a trailing separator
        EXCLUDED_PROPERTIES.put("org.broadleafcommerce.common.file.domain.FileWorkArea",
                new String[] {"filePathLocation"});
        // setPropertyType() stores the enumeration key rather than the instance
        EXCLUDED_PROPERTIES.put("org.broadleafcommerce.common.config.domain.SystemPropertyImpl",
                new String[] {"propertyType"});
        // getJavaCurrency() resolves the currency code through java.util.Currency
        EXCLUDED_PROPERTIES.put("org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl",
                new String[] {"javaCurrency"});
        // the null object is immutable: its setters are no-ops
        EXCLUDED_PROPERTIES.put("org.broadleafcommerce.common.currency.domain.NullBroadleafCurrency",
                new String[] {"defaultFlag", "currencyCode", "friendlyName", "javaCurrency"});
    }

    /**
     * Every accessor of the wrapper delegates to the wrapped DTO, so it is covered by
     * {@code StructuredContentDTOWrapperTest} instead.
     */
    private static final List<String> EXCLUDED_CLASSES = Arrays.asList(
            "org.broadleafcommerce.common.structure.dto.StructuredContentDTOWrapper",
            // the following are Spring/Hibernate infrastructure beans rather than value objects: their
            // accessors need a running container, so they are driven by their own tests where possible
            "org.broadleafcommerce.common.persistence.EntityConfiguration",
            "org.broadleafcommerce.common.persistence.transaction.LifecycleAwareJDBCServices",
            "org.broadleafcommerce.common.persistence.transaction.LifecycleAwareJpaTransactionManager",
            "org.broadleafcommerce.common.template.TemplateOverrideExtensionManager");

    private static List<Class<?>> valueObjects() {
        List<Class<?>> valueObjects = new ArrayList<Class<?>>();
        for (String packageName : VALUE_OBJECT_PACKAGES) {
            for (Class<?> candidate : ClassScanner.classesUnder(packageName)) {
                if (candidate.isInterface() || candidate.isEnum() || candidate.isAnnotation()
                        || Modifier.isAbstract(candidate.getModifiers())
                        || !Modifier.isPublic(candidate.getModifiers())
                        || EXCLUDED_CLASSES.contains(candidate.getName())) {
                    continue;
                }
                valueObjects.add(candidate);
            }
        }
        return valueObjects;
    }

    @Test
    public void valueObjectsWereFound() {
        assertFalse(valueObjects().isEmpty(), "expected the common module to declare value objects");
    }

    @TestFactory
    public List<DynamicTest> valueObjectPropertiesRoundTrip() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Class<?> valueObject : valueObjects()) {
            final String[] excluded = EXCLUDED_PROPERTIES.containsKey(valueObject.getName())
                    ? EXCLUDED_PROPERTIES.get(valueObject.getName()) : new String[0];
            tests.add(DynamicTest.dynamicTest(valueObject.getName(),
                    () -> BeanExerciser.exercise(valueObject, excluded)));
        }
        return tests;
    }
}
