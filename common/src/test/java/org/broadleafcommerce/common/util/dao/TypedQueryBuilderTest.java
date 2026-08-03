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
package org.broadleafcommerce.common.util.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.broadleafcommerce.common.util.dao.TQRestriction.Mode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

/**
 * The query builder is exercised against a mocked {@link EntityManager}; no persistence unit is started.
 */
public class TypedQueryBuilderTest {

    private TypedQueryBuilder<String> builder() {
        return new TypedQueryBuilder<String>(String.class, "item");
    }

    @Test
    public void aBuilderWithoutRestrictionsSelectsEverything() {
        assertEquals("SELECT item FROM java.lang.String item", builder().toQueryString());
        assertEquals("SELECT COUNT(*) FROM java.lang.String item", builder().toQueryString(true));
    }

    @Test
    public void restrictionsAreAndedAndParametersCollected() {
        TypedQueryBuilder<String> builder = builder()
                .addRestriction("item.name", "=", "shirt")
                .addRestriction("item.id", "in", Arrays.asList(1L, 2L));

        assertEquals("SELECT item FROM java.lang.String item WHERE (item.name = :p0) AND (item.id in (:p1))",
                builder.toQueryString());
        assertEquals("shirt", builder.getParamMap().get("p0"));
        assertEquals(Arrays.asList(1L, 2L), builder.getParamMap().get("p1"));
    }

    @Test
    public void joinsAndOrdersAreRendered() {
        String ql = builder()
                .addJoin(new TQJoin("item.collection", "collection"))
                .addOrder(new TQOrder("item.name", true))
                .addOrder(new TQOrder("item.id", false))
                .toQueryString();

        assertEquals("SELECT item FROM java.lang.String item JOIN item.collection collection"
                + " ORDER BY item.name ASC, item.id DESC", ql);
    }

    @Test
    public void nestedRestrictionsUseTheirJoinMode() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        String ql = new TQRestriction(Mode.OR)
                .addChildRestriction(new TQRestriction("item.name", "=", "a"))
                .addChildRestriction(new TQRestriction("item.name", "=", "b"))
                .toQl("p0", paramMap);

        assertEquals("((item.name = :p0_0) OR (item.name = :p0_1))", ql);
        assertEquals("a", paramMap.get("p0_0"));
        assertEquals("b", paramMap.get("p0_1"));
    }

    @Test
    public void aValuelessRestrictionBindsNoParameter() {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        // the operation is lower cased by the restriction
        assertEquals("(item.name is null)", new TQRestriction("item.name", "IS NULL").toQl("p0", paramMap));
        assertEquals(0, paramMap.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void queriesAreCreatedWithTheirParametersBound() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<String> query = mock(TypedQuery.class);
        TypedQuery<Long> countQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(eq("SELECT item FROM java.lang.String item WHERE (item.name = :p0)"),
                eq(String.class))).thenReturn(query);
        when(entityManager.createQuery(eq("SELECT COUNT(*) FROM java.lang.String item WHERE (item.name = :p0)"),
                eq(Long.class))).thenReturn(countQuery);

        TypedQueryBuilder<String> builder = builder().addRestriction("item.name", "=", "shirt");

        assertSame(query, builder.toQuery(entityManager));
        verify(query).setParameter("p0", "shirt");

        assertSame(countQuery, builder.toCountQuery(entityManager));
        verify(countQuery).setParameter("p0", "shirt");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nullParameterValuesAreNotBound() {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<String> query = mock(TypedQuery.class);
        when(entityManager.createQuery(eq("SELECT item FROM java.lang.String item"), eq(String.class)))
                .thenReturn(query);

        TypedQueryBuilder<String> builder = builder();
        builder.getParamMap().put("p0", null);
        builder.toQuery(entityManager);

        verify(query, never()).setParameter(eq("p0"), eq((Object) null));
    }
}
