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

import java.util.LinkedHashMap;
import java.util.Map;

public class StringUtilTest {

    @Test
    public void checksumIsStableAndValueDependent() {
        assertEquals(StringUtil.getChecksum("broadleaf"), StringUtil.getChecksum("broadleaf"));
        assertTrue(StringUtil.getChecksum("broadleaf") != StringUtil.getChecksum("commerce"));
    }

    @Test
    public void segmentInclusionOnlyMatchesWholeDotSeparatedSegments() {
        assertTrue(StringUtil.segmentInclusion("sku.date.extra", "sku.date"));
        assertTrue(StringUtil.segmentInclusion("sku", "sku"));
        assertTrue(StringUtil.segmentInclusion("sku.date", "sku."));
        assertFalse(StringUtil.segmentInclusion("sku.dateExtra", "sku.date"));
        assertFalse(StringUtil.segmentInclusion("", "sku"));
        assertFalse(StringUtil.segmentInclusion("sku", ""));
        assertFalse(StringUtil.segmentInclusion(null, "sku"));
    }

    @Test
    public void identicalStringsHaveZeroSimilarityDeviation() {
        assertEquals(0d, StringUtil.determineSimilarity("a b c", "abc"), 0.0001d);
        assertTrue(StringUtil.determineSimilarity("abc", "xyz") > 0d);
    }

    @Test
    public void urlsAreDecodedAndStrippedOfResponseSplittingCharacters() {
        assertEquals("/catalog/shirt", StringUtil.cleanseUrlString("/catalog/%0D%0Ashirt"));
        assertEquals("/catalog shirt", StringUtil.decodeUrl("/catalog%20shirt"));
        assertNull(StringUtil.decodeUrl(null));
        assertNull(StringUtil.removeSpecialCharacters(null));
        assertEquals("ab", StringUtil.removeSpecialCharacters("a b"));
    }

    @Test
    public void fieldNamesAreExtractedFromValidationExpressions() {
        assertEquals("someFieldName", StringUtil.extractFieldNameFromExpression("fields[someFieldName].value"));
    }

    @Test
    public void mapsAreRenderedAsJson() {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("name", "shirt");
        values.put("active", Boolean.TRUE);
        values.put("count", 3);
        values.put("missing", null);

        assertEquals("{\"name\":\"shirt\",\"active\":true,\"count\":3,\"missing\":null}",
                StringUtil.getMapAsJson(values));
        assertEquals("{}", StringUtil.getMapAsJson(new LinkedHashMap<String, Object>()));
    }
}
