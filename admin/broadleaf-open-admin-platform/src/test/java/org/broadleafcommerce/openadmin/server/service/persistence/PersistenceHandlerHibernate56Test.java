/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.server.service.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.broadleafcommerce.openadmin.server.service.persistence.module.BasicPersistenceModule;
import org.broadleafcommerce.openadmin.server.service.persistence.module.criteria.FieldPathBuilder;
import org.broadleafcommerce.openadmin.server.service.persistence.module.criteria.predicate.BetweenDatePredicateProvider;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;
import org.junit.Test;

/**
 * Verifies persistence handlers compile and are compatible with Hibernate 5.6 API.
 */
public class PersistenceHandlerHibernate56Test {

    @Test
    public void testBasicPersistenceModuleInstantiable() {
        BasicPersistenceModule module = new BasicPersistenceModule();
        assertNotNull(module);
    }

    @Test
    public void testFieldPathBuilderInstantiable() {
        FieldPathBuilder builder = new FieldPathBuilder();
        assertNotNull(builder);
    }

    @Test
    public void testBetweenDatePredicateProviderInstantiable() {
        BetweenDatePredicateProvider provider = new BetweenDatePredicateProvider();
        assertNotNull(provider);
    }

    @Test
    public void testHibernate56CriteriaClassesAccessible() {
        assertNotNull(CriteriaBuilderImpl.class);
        assertNotNull(PluralAttributePath.class);
        assertNotNull(SingularAttributePath.class);
    }

    @Test
    public void testHibernate56JpaImportsResolved() {
        try {
            Class.forName("org.hibernate.jpa.QueryHints");
            assertTrue("org.hibernate.jpa.QueryHints is accessible", true);
        } catch (ClassNotFoundException e) {
            assertTrue("org.hibernate.jpa.QueryHints should be available in Hibernate 5.6", false);
        }
    }

    @Test
    public void testHibernateFlushModeAccessible() {
        assertNotNull(org.hibernate.FlushMode.MANUAL);
        assertNotNull(org.hibernate.FlushMode.AUTO);
        assertNotNull(org.hibernate.FlushMode.COMMIT);
        assertFalse(org.hibernate.FlushMode.MANUAL.equals(org.hibernate.FlushMode.AUTO));
    }

    @Test
    public void testHibernateMetadataApiAccessible() {
        try {
            Class<?> metadataClass = Class.forName("org.hibernate.boot.Metadata");
            assertNotNull(metadataClass);
            assertNotNull(metadataClass.getMethod("getEntityBinding", String.class));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            assertTrue("Hibernate 5.6 Metadata API should be available: " + e.getMessage(), false);
        }
    }

    @Test
    public void testTypeHelperAvailable() {
        try {
            Class<?> typeHelperClass = Class.forName("org.hibernate.TypeHelper");
            assertNotNull(typeHelperClass);
            assertNotNull(typeHelperClass.getMethod("basic", Class.class));
            assertNotNull(typeHelperClass.getMethod("entity", Class.class));
            assertNotNull(typeHelperClass.getMethod("entity", String.class));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            assertTrue("Hibernate 5.6 TypeHelper API should be available: " + e.getMessage(), false);
        }
    }
}
