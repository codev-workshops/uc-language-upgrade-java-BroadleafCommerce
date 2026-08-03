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
package org.broadleafcommerce.common.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BLCCollectionUtilsTest {

    private static final TypedTransformer<String> UPPER_CASE = new TypedTransformer<String>() {
        @Override
        public String transform(Object input) {
            return String.valueOf(input).toUpperCase();
        }
    };

    private static final TypedPredicate<String> STARTS_WITH_A = new TypedPredicate<String>() {
        @Override
        public boolean eval(String value) {
            return value.startsWith("a");
        }
    };

    private static final TypedClosure<Integer, String> BY_LENGTH = new TypedClosure<Integer, String>() {
        @Override
        public Integer getKey(String value) {
            return value.length();
        }
    };

    @Test
    public void collectionsAreTransformed() {
        List<String> input = Arrays.asList("a", "b");
        assertEquals(Arrays.asList("A", "B"), new ArrayList<String>(BLCCollectionUtils.collect(input, UPPER_CASE)));
        assertEquals(Arrays.asList("A", "B"), BLCCollectionUtils.collectList(input, UPPER_CASE));
        assertArrayEquals(new String[] {"A", "B"},
                BLCCollectionUtils.collectArray(input, UPPER_CASE, String.class));
    }

    @Test
    public void collectionsAreFiltered() {
        assertEquals(Arrays.asList("apple"),
                BLCCollectionUtils.selectList(Arrays.asList("apple", "banana"), STARTS_WITH_A));
    }

    @Test
    public void aNullListBecomesAnEmptyList() {
        List<String> existing = new ArrayList<String>();
        assertSame(existing, BLCCollectionUtils.createIfNull(existing));
        assertTrue(BLCCollectionUtils.createIfNull(null).isEmpty());
    }

    @Test
    public void arraysAreSearchedTransformedAndConverted() {
        String[] values = new String[] {"apple", "banana"};

        assertTrue(BLCArrayUtils.contains(values, STARTS_WITH_A));
        assertFalse(BLCArrayUtils.contains(new String[] {"banana"}, STARTS_WITH_A));
        assertEquals(Arrays.asList("apple", "banana"), BLCArrayUtils.asList(values));
        assertNull(BLCArrayUtils.asList(null));
        assertEquals(Arrays.asList("APPLE", "BANANA"), BLCArrayUtils.collect(values, UPPER_CASE));
        assertEquals(new HashSet<String>(Arrays.asList("APPLE", "BANANA")),
                BLCArrayUtils.collectSet(values, UPPER_CASE));
    }

    @Test
    public void valuesAreKeyedByAClosure() {
        List<String> values = Arrays.asList("aa", "bb", "ccc");

        Map<Integer, String> keyed = BLCMapUtils.keyedMap(values, BY_LENGTH);
        assertEquals("bb", keyed.get(2));
        assertEquals("ccc", keyed.get(3));

        Map<Integer, String> fromArray = BLCMapUtils.keyedMap(new String[] {"aa"}, BY_LENGTH);
        assertEquals("aa", fromArray.get(2));
        assertTrue(BLCMapUtils.keyedMap((String[]) null, BY_LENGTH).isEmpty());

        Map<Integer, List<String>> grouped = BLCMapUtils.keyedListMap(values, BY_LENGTH);
        assertEquals(Arrays.asList("aa", "bb"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));
    }

    @Test
    public void mapsAreSortedByTheirValues() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("b", 2);
        map.put("a", 1);

        Map<String, Integer> sorted = BLCMapUtils.valueSortedMap(map,
                (left, right) -> left.getValue().compareTo(right.getValue()));

        assertEquals(Arrays.asList("a", "b"), new ArrayList<String>(sorted.keySet()));
    }

    @Test
    public void validationMessagesAreAssembledFromTheMessageSource() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        MessageSource messageSource = mock(MessageSource.class);
        when(applicationContext.getBean("messageSource")).thenReturn(messageSource);
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> "translated:" + invocation.getArguments()[0]);
        new BLCMessageUtils().setApplicationContext(applicationContext);
        BroadleafRequestContext.setBroadleafRequestContext(new BroadleafRequestContext());
        try {
            Map<String, List<String>> propertyErrors = new LinkedHashMap<String, List<String>>();
            propertyErrors.put("name", Arrays.asList("is required", "is too short"));

            String message = ValidationUtil.buildErrorMessage(propertyErrors, Arrays.asList("global failure"));

            assertEquals("The entity has failed validation - name : translated:is required / "
                    + "name : translated:is too short; translated:global failure; ", message);
            assertEquals("The entity has failed validation - ",
                    ValidationUtil.buildErrorMessage(new HashMap<String, List<String>>(), new ArrayList<String>()));
        } finally {
            BroadleafRequestContext.setBroadleafRequestContext(null);
        }
    }

    @Test
    public void collectionsCanBeEmpty() {
        Collection<String> empty = BLCCollectionUtils.collect(new ArrayList<String>(), UPPER_CASE);
        assertTrue(empty.isEmpty());
    }
}
