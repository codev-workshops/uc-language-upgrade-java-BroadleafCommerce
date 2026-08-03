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
package org.broadleafcommerce.common.structure.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructuredContentDTOWrapperTest {

    private StructuredContentDTOWrapper wrapper(StructuredContentDTO wrapped) {
        return new StructuredContentDTOWrapper(wrapped);
    }

    @Test
    public void everyAccessorDelegatesToTheWrappedDto() {
        StructuredContentDTO wrapped = new StructuredContentDTO();
        StructuredContentDTOWrapper wrapper = wrapper(wrapped);

        wrapper.setId(7L);
        wrapper.setContentName("banner");
        wrapper.setContentType("Global");
        wrapper.setLocaleCode("en_US");
        wrapper.setRuleExpression("1 == 1");
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("title", "hello");
        wrapper.setValues(values);
        List<ItemCriteriaDTO> criteria = new ArrayList<ItemCriteriaDTO>();
        wrapper.setItemCriteriaDTOList(criteria);

        assertEquals(Long.valueOf(7L), wrapped.getId());
        assertEquals("banner", wrapper.getContentName());
        assertEquals("Global", wrapper.getContentType());
        assertEquals("en_US", wrapper.getLocaleCode());
        assertEquals("1 == 1", wrapper.getRuleExpression());
        assertSame(values, wrapper.getValues());
        assertSame(criteria, wrapper.getItemCriteriaDTOList());
        assertEquals("hello", wrapper.getPropertyValue("title"));
        assertEquals(wrapped.hashCode(), wrapper.hashCode());
        assertEquals(wrapped.toString(), wrapper.toString());
    }

    @Test
    public void theWrapperCanOverrideOnlyThePriority() {
        StructuredContentDTO wrapped = new StructuredContentDTO();
        wrapped.setPriority(5);
        StructuredContentDTOWrapper wrapper = wrapper(wrapped);

        assertEquals(Integer.valueOf(5), wrapper.getPriority());

        wrapper.setPriority(1);
        assertEquals(Integer.valueOf(1), wrapped.getPriority());
        assertEquals(Integer.valueOf(1), wrapper.getPriority());
    }

    /**
     * Documents current behaviour: {@code equals} forwards to the wrapped DTO, which compares itself against
     * the wrapper, so the wrapper is not equal to itself. This is a pre-existing defect, asserted here rather
     * than fixed.
     */
    @Test
    public void equalsIsNotReflexive() {
        StructuredContentDTOWrapper wrapper = wrapper(new StructuredContentDTO());
        assertFalse(wrapper.equals(wrapper));
    }
}
