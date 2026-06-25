/*
 * #%L
 * BroadleafCommerce Admin Module
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
package org.broadleafcommerce.admin.server.service.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.broadleafcommerce.admin.server.service.persistence.module.provider.CategoryParentCategoryFieldPersistenceProvider;
import org.broadleafcommerce.admin.server.service.persistence.module.provider.ForeignSkuFieldPersistenceProvider;
import org.broadleafcommerce.admin.server.service.persistence.module.provider.ProductParentCategoryFieldPersistenceProvider;
import org.broadleafcommerce.admin.server.service.persistence.module.provider.SkuFieldsPersistenceProvider;
import org.broadleafcommerce.admin.server.service.persistence.module.provider.SkuPricingPersistenceProvider;
import org.broadleafcommerce.openadmin.server.service.handler.CustomPersistenceHandlerAdapter;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.junit.Test;

/**
 * Verifies admin persistence handlers compile and are compatible with Hibernate 5.6 API.
 */
public class AdminPersistenceHandlerHibernate56Test {

    @Test
    public void testSkuCustomPersistenceHandlerInstantiable() {
        SkuCustomPersistenceHandler handler = new SkuCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testProductCustomPersistenceHandlerInstantiable() {
        ProductCustomPersistenceHandler handler = new ProductCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testCategoryCustomPersistenceHandlerInstantiable() {
        CategoryCustomPersistenceHandler handler = new CategoryCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testCustomerCustomPersistenceHandlerInstantiable() {
        CustomerCustomPersistenceHandler handler = new CustomerCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testChildCategoriesCustomPersistenceHandlerInstantiable() {
        ChildCategoriesCustomPersistenceHandler handler = new ChildCategoriesCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testSkuBundleItemCustomPersistenceHandlerInstantiable() {
        SkuBundleItemCustomPersistenceHandler handler = new SkuBundleItemCustomPersistenceHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof CustomPersistenceHandlerAdapter);
    }

    @Test
    public void testAdminPersistenceProvidersInstantiable() {
        assertNotNull(new CategoryParentCategoryFieldPersistenceProvider());
        assertNotNull(new ForeignSkuFieldPersistenceProvider());
        assertNotNull(new ProductParentCategoryFieldPersistenceProvider());
        assertNotNull(new SkuFieldsPersistenceProvider());
        assertNotNull(new SkuPricingPersistenceProvider());
    }

    @Test
    public void testHibernateJpaImportResolved() {
        assertNotNull(HibernateEntityManager.class);
    }

    @Test
    public void testHibernate56CriteriaBuilderAccessible() {
        assertNotNull(CriteriaBuilderImpl.class);
    }

    @Test
    public void testHibernate56JpaQueryHintsAccessible() {
        try {
            Class.forName("org.hibernate.jpa.QueryHints");
            assertTrue("org.hibernate.jpa.QueryHints is accessible", true);
        } catch (ClassNotFoundException e) {
            assertTrue("org.hibernate.jpa.QueryHints should be available in Hibernate 5.6", false);
        }
    }
}
