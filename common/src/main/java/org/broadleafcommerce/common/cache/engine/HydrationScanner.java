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
package org.broadleafcommerce.common.cache.engine;

import org.broadleafcommerce.common.cache.Hydrated;
import org.hibernate.annotations.Cache;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.persistence.Id;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Scans entity bytecode (via ASM) to discover the Hibernate {@link Cache} region as well as the
 * {@link Id} and {@link Hydrated} field mutators.
 *
 * <p>As of ASM 5 the {@code ClassVisitor}/{@code FieldVisitor}/{@code AnnotationVisitor} types are abstract
 * classes (rather than interfaces) and {@code org.objectweb.asm.commons.EmptyVisitor} was removed. This class
 * therefore extends {@link ClassVisitor} and delegates field/annotation visitation to dedicated inner visitors.
 *
 * @author jfischer
 */
public class HydrationScanner extends ClassVisitor {

    private static final int CLASSSTAGE = 0;
    private static final int FIELDSTAGE = 1;

    @SuppressWarnings("unchecked")
    public HydrationScanner(Class topEntityClass, Class entityClass) {
        super(Opcodes.ASM9);
        this.topEntityClass = topEntityClass;
        this.entityClass = entityClass;
    }

    private String cacheRegion;
    private Map<String, Method[]> idMutators = new HashMap<String, Method[]>();
    private Map<String, HydrationItemDescriptor> cacheMutators = new HashMap<String, HydrationItemDescriptor>();
    @SuppressWarnings("unchecked")
    private final Class entityClass;
    @SuppressWarnings("unchecked")
    private final Class topEntityClass;

    private int stage = CLASSSTAGE;
    @SuppressWarnings("unchecked")
    private Class clazz;
    private String annotation;
    private String fieldName;
    @SuppressWarnings("unchecked")
    private Class fieldClass;

    public void init() {
        try {
            InputStream in = HydrationScanner.class.getClassLoader().getResourceAsStream(topEntityClass.getName().replace('.', '/') + ".class");
            new ClassReader(in).accept(this, ClassReader.SKIP_DEBUG);
            in = HydrationScanner.class.getClassLoader().getResourceAsStream(entityClass.getName().replace('.', '/') + ".class");
            new ClassReader(in).accept(this, ClassReader.SKIP_DEBUG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCacheRegion() {
        return cacheRegion;
    }

    public Map<String, Method[]> getIdMutators() {
        return idMutators;
    }

    public Map<String, HydrationItemDescriptor> getCacheMutators() {
        return cacheMutators;
    }

    private Method[] retrieveMutators() {
        String mutatorName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1, fieldName.length());
        Method getMethod = null;
        try {
            getMethod = clazz.getMethod("get"+mutatorName, new Class[]{});
        } catch (Exception e) {
            //do nothing
        }
        if (getMethod == null) {
            try {
                getMethod = clazz.getMethod("is"+mutatorName, new Class[]{});
            } catch (Exception e) {
                //do nothing
            }
        }
        if (getMethod == null) {
            try {
                getMethod = clazz.getMethod(fieldName, new Class[]{});
            } catch (Exception e) {
                //do nothing
            }
        }
        Method setMethod = null;
        try {
            setMethod = clazz.getMethod("set"+mutatorName, new Class[]{fieldClass});
        } catch (Exception e) {
            //do nothing
        }
        if (getMethod == null || setMethod == null) {
            throw new RuntimeException("Unable to find a getter and setter method for the AdminPresentation field: " + fieldName + ". Make sure you have a getter method entitled: get" + mutatorName + "(), or is" + mutatorName + "(), or " + fieldName + "(). Make sure you have a setter method entitled: set" + mutatorName + "(..).");
        }
        return new Method[]{getMethod, setMethod};
    }

    //ClassVisitor
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        try {
            clazz = Class.forName(name.replaceAll("/", "."));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        stage = CLASSSTAGE;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        Type annotationType = Type.getType(desc);
        if (annotationType.getClassName().equals(Cache.class.getName())) {
            annotation = Cache.class.getName();
        }
        return new HydrationAnnotationVisitor();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        stage = FIELDSTAGE;
        fieldName = name;
        Type fieldType = Type.getType(desc);
        switch(fieldType.getSort()){
        case Type.BOOLEAN:
            fieldClass = boolean.class;
            break;
        case Type.BYTE:
            fieldClass = byte.class;
            break;
        case Type.CHAR:
            fieldClass = char.class;
            break;
        case Type.DOUBLE:
            fieldClass = double.class;
            break;
        case Type.FLOAT:
            fieldClass = float.class;
            break;
        case Type.INT:
            fieldClass = int.class;
            break;
        case Type.LONG:
            fieldClass = long.class;
            break;
        case Type.SHORT:
            fieldClass = short.class;
            break;
        case Type.OBJECT:
            try {
                fieldClass = Class.forName(fieldType.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            break;
        }
        return new HydrationFieldVisitor();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return null;
    }

    /**
     * Visits the annotations declared on a field, capturing {@link Id} and {@link Hydrated} metadata.
     */
    private class HydrationFieldVisitor extends FieldVisitor {

        HydrationFieldVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            Type annotationType = Type.getType(desc);
            if (annotationType.getClassName().equals(Id.class.getName())) {
                idMutators.put(fieldName, retrieveMutators());
            }
            if (annotationType.getClassName().equals(Hydrated.class.getName())) {
                annotation = Hydrated.class.getName();
            }
            return new HydrationAnnotationVisitor();
        }
    }

    /**
     * Reads the attributes of the {@link Cache} and {@link Hydrated} annotations.
     */
    private class HydrationAnnotationVisitor extends AnnotationVisitor {

        HydrationAnnotationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(String name, Object value) {
            if (Cache.class.getName().equals(annotation) && "region".equals(name)) {
                cacheRegion = (String) value;
            }
            if (Hydrated.class.getName().equals(annotation) && "factoryMethod".equals(name)) {
                HydrationItemDescriptor itemDescriptor = new HydrationItemDescriptor();
                itemDescriptor.setFactoryMethod((String) value);
                itemDescriptor.setMutators(retrieveMutators());
                cacheMutators.put(fieldName, itemDescriptor);
            }
        }
    }

}
