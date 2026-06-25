/*
 * #%L
 * BroadleafCommerce CMS Module
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
package org.broadleafcommerce.cms.structure.service;

import org.broadleafcommerce.cms.structure.dao.StructuredContentDao;
import org.broadleafcommerce.cms.structure.domain.StructuredContent;
import org.broadleafcommerce.cms.structure.domain.StructuredContentImpl;
import org.broadleafcommerce.cms.structure.domain.StructuredContentType;
import org.broadleafcommerce.cms.structure.domain.StructuredContentTypeImpl;
import org.broadleafcommerce.common.structure.dto.StructuredContentDTO;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for StructuredContentService under Hibernate 5.6 compatibility.
 * Verifies that the service layer CRUD operations function correctly
 * after the migration from Hibernate 4.x query patterns.
 */
public class StructuredContentServiceHibernate56Test {

    private StructuredContentServiceImpl service;
    private StructuredContentDao mockDao;

    @Before
    public void setUp() {
        service = new StructuredContentServiceImpl();
        mockDao = EasyMock.createMock(StructuredContentDao.class);
        service.structuredContentDao = mockDao;
    }

    @Test
    public void testFindStructuredContentById() {
        StructuredContentImpl content = new StructuredContentImpl();
        content.setId(1L);

        EasyMock.expect(mockDao.findStructuredContentById(1L)).andReturn(content);
        EasyMock.replay(mockDao);

        StructuredContent result = service.findStructuredContentById(1L);
        assertNotNull(result);
        assertEquals(Long.valueOf(1L), result.getId());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testFindStructuredContentTypeById() {
        StructuredContentTypeImpl type = new StructuredContentTypeImpl();
        type.setId(10L);
        type.setName("TestType");

        EasyMock.expect(mockDao.findStructuredContentTypeById(10L)).andReturn(type);
        EasyMock.replay(mockDao);

        StructuredContentType result = service.findStructuredContentTypeById(10L);
        assertNotNull(result);
        assertEquals("TestType", result.getName());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testFindStructuredContentTypeByName() {
        StructuredContentTypeImpl type = new StructuredContentTypeImpl();
        type.setId(20L);
        type.setName("Homepage Banner");

        EasyMock.expect(mockDao.findStructuredContentTypeByName("Homepage Banner")).andReturn(type);
        EasyMock.replay(mockDao);

        StructuredContentType result = service.findStructuredContentTypeByName("Homepage Banner");
        assertNotNull(result);
        assertEquals("Homepage Banner", result.getName());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testRetrieveAllStructuredContentTypes() {
        List<StructuredContentType> types = new ArrayList<>();
        StructuredContentTypeImpl type1 = new StructuredContentTypeImpl();
        type1.setName("Type1");
        StructuredContentTypeImpl type2 = new StructuredContentTypeImpl();
        type2.setName("Type2");
        types.add(type1);
        types.add(type2);

        EasyMock.expect(mockDao.retrieveAllStructuredContentTypes()).andReturn(types);
        EasyMock.replay(mockDao);

        List<StructuredContentType> result = service.retrieveAllStructuredContentTypes();
        assertNotNull(result);
        assertEquals(2, result.size());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testSaveStructuredContentType() {
        StructuredContentTypeImpl type = new StructuredContentTypeImpl();
        type.setName("NewType");

        EasyMock.expect(mockDao.saveStructuredContentType(type)).andReturn(type);
        EasyMock.replay(mockDao);

        StructuredContentType result = service.saveStructuredContentType(type);
        assertNotNull(result);
        assertEquals("NewType", result.getName());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testFindAllContentItems() {
        List<StructuredContent> items = new ArrayList<>();
        StructuredContentImpl item1 = new StructuredContentImpl();
        item1.setId(1L);
        StructuredContentImpl item2 = new StructuredContentImpl();
        item2.setId(2L);
        items.add(item1);
        items.add(item2);

        EasyMock.expect(mockDao.findAllContentItems()).andReturn(items);
        EasyMock.replay(mockDao);

        List<StructuredContent> result = service.findAllContentItems();
        assertNotNull(result);
        assertEquals(2, result.size());

        EasyMock.verify(mockDao);
    }

    @Test
    public void testBuildStructuredContentDTOListEmpty() {
        List<StructuredContentDTO> result = service.buildStructuredContentDTOList(Collections.emptyList(), false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEvaluateAndPrioritizeContentSingleItem() {
        StructuredContentDTO dto = new StructuredContentDTO();
        dto.setPriority(1);
        List<StructuredContentDTO> list = new ArrayList<>();
        list.add(dto);

        List<StructuredContentDTO> result = service.evaluateAndPriortizeContent(list, 1, Collections.emptyMap());
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
