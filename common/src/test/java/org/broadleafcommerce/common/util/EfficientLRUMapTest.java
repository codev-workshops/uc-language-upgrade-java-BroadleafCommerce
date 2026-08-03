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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class EfficientLRUMapTest {

    private Map<String, String> map(int maxEntries) {
        return new EfficientLRUMap<String, String>(maxEntries);
    }

    @Test
    public void theConcurrentBackingMapIsUsedBelowTheThreshold() {
        Map<String, String> map = map(5);
        assertTrue(map.isEmpty());

        assertNull(map.put("a", "1"));
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue("2"));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue("missing"));
        assertEquals(1, map.keySet().size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.entrySet().size());

        assertEquals("2", map.remove("a"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void theMapSwitchesToAnLruOnceTheThresholdIsExceeded() {
        Map<String, String> map = map(2);
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        // the oldest entry is evicted by the LRU backing map
        assertEquals(2, map.size());
        assertFalse(map.containsKey("a"));
        assertEquals("3", map.get("c"));
        assertTrue(map.containsValue("3"));
        assertEquals(2, map.keySet().size());
        assertEquals(2, map.values().size());
        assertEquals(2, map.entrySet().size());
        assertFalse(map.isEmpty());

        assertEquals("3", map.remove("c"));
    }

    @Test
    public void putAllAlsoSwitchesTheBackingMap() {
        Map<String, String> source = new HashMap<String, String>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");

        Map<String, String> map = map(2);
        map.putAll(source);
        assertEquals(2, map.size());

        map.putAll(source);
        assertEquals(2, map.size());
    }

    @Test
    public void clearingRevertsToTheConcurrentBackingMap() {
        Map<String, String> map = map(1);
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());

        map.put("c", "3");
        assertEquals("3", map.get("c"));
        map.clear();
        assertTrue(map.isEmpty());
    }
}
