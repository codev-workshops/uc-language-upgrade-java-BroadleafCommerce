/*
 * #%L
 * BroadleafCommerce Profile
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
package org.broadleafcommerce.profile.core.dao;

import junit.framework.TestCase;

import org.hibernate.jpa.QueryHints;

/**
 * Validates that the Hibernate JPA QueryHints class is available
 * after the migration from org.hibernate.ejb to org.hibernate.jpa.
 */
public class HibernateJpaQueryHintsTest extends TestCase {

    public void testQueryHintsCacheableConstant() {
        assertNotNull(QueryHints.HINT_CACHEABLE);
        assertEquals("org.hibernate.cacheable", QueryHints.HINT_CACHEABLE);
    }

    public void testQueryHintsCacheRegionConstant() {
        assertNotNull(QueryHints.HINT_CACHE_REGION);
        assertEquals("org.hibernate.cacheRegion", QueryHints.HINT_CACHE_REGION);
    }
}
