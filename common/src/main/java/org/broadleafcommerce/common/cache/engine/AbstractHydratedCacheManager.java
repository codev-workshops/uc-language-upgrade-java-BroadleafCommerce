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

import net.sf.ehcache.event.CacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jfischer
 */
public abstract class AbstractHydratedCacheManager implements CacheEventListener, HydratedCacheManager, HydratedAnnotationManager {

    private static final Log LOG = LogFactory.getLog(AbstractHydratedCacheManager.class);

    private Map<String, HydrationDescriptor> hydrationDescriptors = Collections.synchronizedMap(new HashMap(100));

    @Override
    public HydrationDescriptor getHydrationDescriptor(Object entity) {
        if (hydrationDescriptors.containsKey(entity.getClass().getName())) {
            return hydrationDescriptors.get(entity.getClass().getName());
        }
        HydrationDescriptor descriptor = new HydrationDescriptor();
        Class<?> topEntityClass = getTopEntityClass(entity);
        HydrationScanner scanner = new HydrationScanner(topEntityClass, entity.getClass());
        scanner.init();
        descriptor.setHydratedMutators(scanner.getCacheMutators());
        Map<String, Method[]> mutators = scanner.getIdMutators();
        if (mutators.size() != 1) {
            throw new RuntimeException("Broadleaf Commerce Hydrated Cache currently only supports entities with a single @Id annotation.");
        }
        Method[] singleMutators = mutators.values().iterator().next();
        descriptor.setIdMutators(singleMutators);
        String cacheRegion = scanner.getCacheRegion();
        if (cacheRegion == null || "".equals(cacheRegion)) {
            cacheRegion = topEntityClass.getName();
        }
        descriptor.setCacheRegion(cacheRegion);
        hydrationDescriptors.put(entity.getClass().getName(), descriptor);
        return descriptor;
    }

    protected Class<?> getTopEntityClass(Object entity) {
        Class<?> myClass = entity.getClass();
        Class<?> superClass = entity.getClass().getSuperclass();
        while (superClass != null && superClass.getName().startsWith("org.broadleaf")) {
            myClass = superClass;
            superClass = superClass.getSuperclass();
        }
        return myClass;
    }

    @Override
    public void dispose() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Disposing of all hydrated cache members");
        }
        hydrationDescriptors.clear();
    }

    /**
     * Resolves the Hibernate second-level cache key that is stored in the underlying EhCache region into the
     * entity (or collection role) name and the entity identifier that the Broadleaf hydrated cache is keyed by.
     * <p>
     * Through Hibernate 4 the second-level cache key was the public {@code org.hibernate.cache.spi.CacheKey},
     * which exposed {@code getEntityOrRoleName()} and {@code getKey()}. As of Hibernate 5 the default key is the
     * package-private, {@code final} {@code org.hibernate.cache.internal.CacheKeyImplementation} which only exposes
     * {@code getId()} publicly. Since there is no public API to obtain the entity/role name, this reads the
     * {@code entityOrRoleName} field reflectively, falling back to the supplied {@code defaultEntityName} and the raw
     * key when the runtime key is not a recognized Hibernate cache key.
     *
     * @return a two element array: {@code [entityOrRoleName, id]}
     */
    protected static Object[] resolveEntityNameAndId(Serializable key, String defaultEntityName) {
        String entityName = defaultEntityName;
        Object id = key;
        if (key != null && "org.hibernate.cache.internal.CacheKeyImplementation".equals(key.getClass().getName())) {
            try {
                Method getId = key.getClass().getMethod("getId");
                id = getId.invoke(key);
            } catch (Exception e) {
                LOG.warn("Unable to read the id from the Hibernate cache key", e);
            }
            try {
                Field nameField = key.getClass().getDeclaredField("entityOrRoleName");
                nameField.setAccessible(true);
                entityName = (String) nameField.get(key);
            } catch (Exception e) {
                LOG.warn("Unable to read the entityOrRoleName from the Hibernate cache key", e);
            }
        }
        return new Object[]{entityName, id};
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return this;
    }

}
