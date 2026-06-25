/*
 * #%L
 * BroadleafCommerce Integration
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
package org.broadleafcommerce.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fixes bean definitions that reference removed classes from Spring 4 / Thymeleaf 2 / Hibernate 4.
 * This runs after the BLC merge context loads all XML bean definitions but before
 * beans are actually instantiated, allowing us to correct class names and remove
 * beans that reference completely removed APIs.
 */
public class Spring5BeanDefinitionFixPostProcessor implements BeanFactoryPostProcessor {

    private static final Map<String, String> CLASS_REPLACEMENTS = new HashMap<>();
    private static final Set<String> BEANS_TO_REMOVE = new HashSet<>(Arrays.asList(
            "hibernateExporter",
            "blWebDatabaseTemplateResolver",
            "blWebTemplateResolver",
            "blWebClasspathTemplateResolver",
            "blWebCustomTemplateResolver",
            "blWebCommonClasspathTemplateResolver",
            "blWebTemplateResolvers",
            "blWebTemplateEngine",
            "blAdminWebTemplateResolver",
            "blAdminWebClasspathTemplateResolver",
            "blAdminWebCustomTemplateResolver",
            "blAdminWebTemplateResolvers",
            "blAdminWebTemplateEngine",
            "blEmailTemplateResolver",
            "blEmailTemplateEngine"
    ));


    static {
        CLASS_REPLACEMENTS.put("org.springframework.ui.velocity.VelocityEngineFactoryBean",
                "org.apache.velocity.app.VelocityEngine");
        CLASS_REPLACEMENTS.put("org.thymeleaf.spring4.dialect.SpringStandardDialect",
                "org.thymeleaf.spring5.dialect.SpringStandardDialect");
        CLASS_REPLACEMENTS.put("org.thymeleaf.spring4.SpringTemplateEngine",
                "org.thymeleaf.spring5.SpringTemplateEngine");
        CLASS_REPLACEMENTS.put("org.thymeleaf.spring4.messageresolver.SpringMessageResolver",
                "org.thymeleaf.spring5.messageresolver.SpringMessageResolver");
        CLASS_REPLACEMENTS.put("org.thymeleaf.templateresolver.ServletContextTemplateResolver",
                "org.thymeleaf.templateresolver.ClassLoaderTemplateResolver");
        CLASS_REPLACEMENTS.put("nz.net.ultraq.thymeleaf.LayoutDialect",
                "nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry)) {
            return;
        }
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        // Remove beans that reference completely removed APIs
        for (String beanToRemove : BEANS_TO_REMOVE) {
            if (registry.containsBeanDefinition(beanToRemove)) {
                registry.removeBeanDefinition(beanToRemove);
            }
        }

        String[] beanNames = registry.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            String beanClassName = bd.getBeanClassName();
            if (beanClassName != null && CLASS_REPLACEMENTS.containsKey(beanClassName)) {
                String newClass = CLASS_REPLACEMENTS.get(beanClassName);
                bd.setBeanClassName(newClass);

                // Remove properties that don't exist on the new class
                if ("org.apache.velocity.app.VelocityEngine".equals(newClass)) {
                    MutablePropertyValues pvs = bd.getPropertyValues();
                    if (pvs.contains("velocityProperties")) {
                        pvs.removePropertyValue("velocityProperties");
                    }
                }
                if ("org.thymeleaf.spring5.SpringTemplateEngine".equals(newClass)) {
                    MutablePropertyValues pvs = bd.getPropertyValues();
                    if (pvs.contains("templateModeHandlers")) {
                        pvs.removePropertyValue("templateModeHandlers");
                    }
                    if (pvs.contains("cacheManager")) {
                        pvs.removePropertyValue("cacheManager");
                    }
                }
                if ("org.thymeleaf.templateresolver.ClassLoaderTemplateResolver".equals(newClass)) {
                    fixTemplateModeHtml5(bd);
                }
            }
        }

        // Fix HTML5 -> HTML for ALL template resolvers (including ones not class-replaced)
        // Also fix BLC Thymeleaf 3 processors that need a dialect prefix constructor arg
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            String beanClassName = bd.getBeanClassName();
            if (beanClassName != null) {
                if (beanClassName.contains("TemplateResolver")) {
                    fixTemplateModeHtml5(bd);
                }
                // BLC Thymeleaf 3 processors that need "blc" dialectPrefix constructor arg
                if (beanClassName.startsWith("org.broadleafcommerce")
                        && beanClassName.contains("Processor")
                        && bd.getConstructorArgumentValues().getArgumentCount() == 0) {
                    try {
                        Class<?> clazz = Class.forName(beanClassName);
                        clazz.getConstructor(); // no-arg exists, skip
                    } catch (NoSuchMethodException e) {
                        // no zero-arg constructor, add dialectPrefix
                        bd.getConstructorArgumentValues().addIndexedArgumentValue(0, "blc");
                    } catch (ClassNotFoundException e) {
                        // class not found, skip
                    }
                }
            }
        }
    }

    private void fixTemplateModeHtml5(BeanDefinition bd) {
        MutablePropertyValues pvs = bd.getPropertyValues();
        if (pvs.contains("templateMode")) {
            Object val = pvs.getPropertyValue("templateMode").getValue();
            String strVal = null;
            if (val instanceof TypedStringValue) {
                strVal = ((TypedStringValue) val).getValue();
            } else if (val instanceof String) {
                strVal = (String) val;
            }
            if ("HTML5".equals(strVal)) {
                pvs.removePropertyValue("templateMode");
                pvs.addPropertyValue("templateMode", "HTML");
            }
        }
    }
}
