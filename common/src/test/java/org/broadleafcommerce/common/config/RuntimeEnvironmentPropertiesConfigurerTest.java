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
package org.broadleafcommerce.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Exercises the configurer against temporary property files only; no application context is started.
 */
public class RuntimeEnvironmentPropertiesConfigurerTest {

    /** Exposes the merged properties, which are otherwise only visible to Spring. */
    private static class TestableConfigurer extends RuntimeEnvironmentPropertiesConfigurer {

        Properties merged() throws IOException {
            return mergeProperties();
        }
    }

    @TempDir
    File propertyDirectory;

    @AfterEach
    public void clearSystemProperties() {
        System.clearProperty("runtime.environment");
        System.clearProperty("property-override");
        System.clearProperty("property-shared-override");
    }

    private void writeProperties(File directory, String fileName, String contents) throws IOException {
        FileWriter writer = new FileWriter(new File(directory, fileName));
        try {
            writer.write(contents);
        } finally {
            writer.close();
        }
    }

    private Set<Resource> locations(File directory) {
        Set<Resource> locations = new HashSet<Resource>();
        locations.add(new FileSystemResource(directory.getAbsolutePath() + File.separator));
        return locations;
    }

    @Test
    public void theEnvironmentComesFromTheKeyResolver() {
        RuntimeEnvironmentPropertiesConfigurer configurer = new RuntimeEnvironmentPropertiesConfigurer();
        configurer.setKeyResolver(new RuntimeEnvironmentKeyResolver() {

            @Override
            public String resolveRuntimeEnvironmentKey() {
                return "STAGING";
            }
        });

        assertEquals("staging", configurer.determineEnvironment());
        // the cached value is returned verbatim on subsequent calls, without the lower casing
        assertEquals("STAGING", configurer.determineEnvironment());
    }

    @Test
    public void anUnresolvableEnvironmentFallsBackToTheDefault() {
        RuntimeEnvironmentPropertiesConfigurer configurer = new RuntimeEnvironmentPropertiesConfigurer();
        configurer.setDefaultEnvironment("integrationqa");
        configurer.setKeyResolver(new SystemPropertyRuntimeEnvironmentKeyResolver());

        assertEquals("integrationqa", configurer.getDefaultEnvironment());
        assertEquals("integrationqa", configurer.determineEnvironment());
    }

    @Test
    public void theSystemPropertyKeyResolverReadsTheConfiguredKey() {
        SystemPropertyRuntimeEnvironmentKeyResolver resolver = new SystemPropertyRuntimeEnvironmentKeyResolver();
        assertNull(resolver.resolveRuntimeEnvironmentKey());

        System.setProperty("runtime.environment", "production");
        assertEquals("production", resolver.resolveRuntimeEnvironmentKey());

        resolver.setEnvironmentKey("some.other.key");
        assertNull(resolver.resolveRuntimeEnvironmentKey());
    }

    @Test
    public void aDefaultEnvironmentOutsideTheAllowedListIsRejected() {
        RuntimeEnvironmentPropertiesConfigurer configurer = new RuntimeEnvironmentPropertiesConfigurer();
        configurer.setEnvironments(Collections.singleton("production"));
        configurer.setDefaultEnvironment("development");

        assertThrows(AssertionError.class, () -> configurer.afterPropertiesSet());
    }

    @Test
    public void environmentPropertiesOverrideCommonProperties() throws IOException {
        writeProperties(propertyDirectory, "common.properties", "shared.key=common\nonly.common=yes\n");
        writeProperties(propertyDirectory, "development.properties", "shared.key=development\n");

        TestableConfigurer configurer = new TestableConfigurer();
        configurer.setPropertyLocations(locations(propertyDirectory));
        configurer.afterPropertiesSet();

        Properties merged = configurer.merged();
        assertEquals("development", merged.getProperty("shared.key"));
        assertEquals("yes", merged.getProperty("only.common"));
    }

    @Test
    public void theOverridableLocationsAreReadBeforeTheMainLocations() throws IOException {
        File overridable = new File(propertyDirectory, "overridable");
        File main = new File(propertyDirectory, "main");
        assertNotNull(overridable.mkdir() ? overridable : null);
        assertNotNull(main.mkdir() ? main : null);
        writeProperties(overridable, "common.properties", "shared.key=overridable\n");
        writeProperties(main, "common.properties", "shared.key=main\n");

        TestableConfigurer configurer = new TestableConfigurer();
        configurer.setOverridableProperyLocations(locations(overridable));
        configurer.setPropertyLocations(locations(main));
        configurer.afterPropertiesSet();

        assertEquals("main", configurer.merged().getProperty("shared.key"));
    }

    @Test
    public void systemPropertyOverridesWinOverTheEnvironmentFiles() throws IOException {
        writeProperties(propertyDirectory, "common.properties", "shared.key=common\n");
        File overrideFile = new File(propertyDirectory, "override.properties");
        writeProperties(propertyDirectory, "override.properties", "shared.key=override\n");
        File sharedOverrideFile = new File(propertyDirectory, "shared-override.properties");
        writeProperties(propertyDirectory, "shared-override.properties", "other.key=shared-override\n");

        System.setProperty("property-override", overrideFile.getAbsolutePath());
        System.setProperty("property-shared-override", sharedOverrideFile.getAbsolutePath());

        TestableConfigurer configurer = new TestableConfigurer();
        configurer.setPropertyLocations(locations(propertyDirectory));
        configurer.afterPropertiesSet();

        Properties merged = configurer.merged();
        assertEquals("override", merged.getProperty("shared.key"));
        assertEquals("shared-override", merged.getProperty("other.key"));
    }

    @Test
    public void processingPropertiesInstallsAPlaceholderResolver() throws IOException {
        writeProperties(propertyDirectory, "common.properties", "shared.key=common\n");

        TestableConfigurer configurer = new TestableConfigurer();
        configurer.setPropertyLocations(locations(propertyDirectory));
        configurer.afterPropertiesSet();
        assertNull(configurer.getStringValueResolver());

        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[0]);
        when(beanFactory.getAliases("")).thenReturn(new String[0]);
        configurer.postProcessBeanFactory(beanFactory);

        assertEquals("common", configurer.getStringValueResolver().resolveStringValue("${shared.key}"));
        assertNull(configurer.getStringValueResolver().resolveStringValue("${missing.key:}"));
    }
}
