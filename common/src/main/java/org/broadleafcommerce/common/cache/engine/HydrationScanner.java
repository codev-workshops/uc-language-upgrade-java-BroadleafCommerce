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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import jakarta.persistence.Id;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author jfischer
 *
 */
public class HydrationScanner extends ClassVisitor {

    private static final int API = Opcodes.ASM9;

    @SuppressWarnings("unchecked")
    public HydrationScanner(Class topEntityClass, Class entityClass) {
        super(API);
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
    
    @SuppressWarnings("unchecked")
    private Class clazz;
    
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

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        try {
            clazz = Class.forName(name.replaceAll("/", "."));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        Type annotationType = Type.getType(descriptor);
        if (annotationType.getClassName().equals(Cache.class.getName())) {
            return new AnnotationVisitor(API) {
                @Override
                public void visit(String name, Object value) {
                    if ("region".equals(name)) {
                        cacheRegion = (String) value;
                    }
                }
            };
        }
        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        final String currentFieldName = name;
        final Class currentFieldClass = resolveFieldClass(descriptor);
        return new FieldVisitor(API) {
            @Override
            public AnnotationVisitor visitAnnotation(String fieldAnnotationDescriptor, boolean visible) {
                Type annotationType = Type.getType(fieldAnnotationDescriptor);
                if (annotationType.getClassName().equals(Id.class.getName())) {
                    idMutators.put(currentFieldName, retrieveMutators(currentFieldName, currentFieldClass));
                    return null;
                }
                if (annotationType.getClassName().equals(Hydrated.class.getName())) {
                    return new AnnotationVisitor(API) {
                        @Override
                        public void visit(String annotationMember, Object annotationValue) {
                            if ("factoryMethod".equals(annotationMember)) {
                                HydrationItemDescriptor itemDescriptor = new HydrationItemDescriptor();
                                itemDescriptor.setFactoryMethod((String) annotationValue);
                                itemDescriptor.setMutators(retrieveMutators(currentFieldName, currentFieldClass));
                                cacheMutators.put(currentFieldName, itemDescriptor);
                            }
                        }
                    };
                }
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Class resolveFieldClass(String descriptor) {
        Type fieldType = Type.getType(descriptor);
        switch (fieldType.getSort()) {
        case Type.BOOLEAN:
            return boolean.class;
        case Type.BYTE:
            return byte.class;
        case Type.CHAR:
            return char.class;
        case Type.DOUBLE:
            return double.class;
        case Type.FLOAT:
            return float.class;
        case Type.INT:
            return int.class;
        case Type.LONG:
            return long.class;
        case Type.SHORT:
            return short.class;
        case Type.OBJECT:
            try {
                return Class.forName(fieldType.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        default:
            return null;
        }
    }

    private Method[] retrieveMutators(String fieldName, Class fieldClass) {
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

}
