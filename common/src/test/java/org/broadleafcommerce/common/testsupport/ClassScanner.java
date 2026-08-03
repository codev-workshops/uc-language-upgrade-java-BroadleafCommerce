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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the compiled production classes of this module from {@code target/classes} so that broad
 * smoke tests can be written against every implementation of a given contract without having to
 * enumerate them by hand.
 */
public final class ClassScanner {

    private static final File CLASSES_DIR = new File("target/classes");

    private ClassScanner() {
        // utility
    }

    /**
     * @return every loadable production class of this module whose binary name starts with the given prefix
     */
    public static List<Class<?>> classesUnder(String packagePrefix) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        collect(CLASSES_DIR, "", packagePrefix, classes);
        Collections.sort(classes, (left, right) -> left.getName().compareTo(right.getName()));
        return classes;
    }

    private static void collect(File dir, String prefix, String packagePrefix, List<Class<?>> classes) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, prefix + child.getName() + ".", packagePrefix, classes);
            } else if (child.getName().endsWith(".class") && !child.getName().contains("$")) {
                String name = prefix + child.getName().substring(0, child.getName().length() - ".class".length());
                if (!name.startsWith(packagePrefix)) {
                    continue;
                }
                try {
                    classes.add(Class.forName(name, false, ClassScanner.class.getClassLoader()));
                } catch (Throwable e) {
                    // classes whose optional dependencies are absent from the test classpath are simply skipped
                }
            }
        }
    }
}
