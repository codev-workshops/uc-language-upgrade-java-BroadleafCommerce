/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2024 Broadleaf Commerce
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
package org.broadleafcommerce.core.web.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.MatchingAttributeName;
import org.thymeleaf.processor.element.MatchingElementName;
import org.thymeleaf.templatemode.TemplateMode;

import org.junit.Test;

/**
 * Verifies the Thymeleaf 2 -&gt; 3 migration contract for this module's processors. Each processor now extends a
 * Thymeleaf 3 {@code AbstractElementTagProcessor}/{@code AbstractAttributeTagProcessor} and must advertise the correct
 * {@code blc}-prefixed element/attribute name, the {@code HTML} template mode and the expected precedence so the engine
 * can match them. This replaces the Thymeleaf 2 {@code getName()}/{@code AttrProcessor} registration contract.
 *
 * @author Devin (java17 Phase 7)
 */
public class Thymeleaf3ProcessorMigrationTest {

    private static final String PREFIX = "blc";

    private void assertElement(AbstractElementTagProcessor processor, String expectedName, int expectedPrecedence) {
        assertEquals(TemplateMode.HTML, processor.getTemplateMode());
        assertEquals(expectedPrecedence, processor.getPrecedence());

        MatchingElementName matchingElementName = processor.getMatchingElementName();
        assertFalse("Processor should match a specific element, not all elements",
                matchingElementName.isMatchingAllElements());
        assertEquals(expectedName, matchingElementName.getMatchingElementName().getElementName());
        assertTrue(matchingElementName.getMatchingElementName().isPrefixed());
        assertEquals(PREFIX, matchingElementName.getMatchingElementName().getPrefix());
    }

    private void assertAttribute(AbstractAttributeTagProcessor processor, String expectedAttr, int expectedPrecedence) {
        assertEquals(TemplateMode.HTML, processor.getTemplateMode());
        assertEquals(expectedPrecedence, processor.getPrecedence());

        MatchingAttributeName matchingAttributeName = processor.getMatchingAttributeName();
        assertFalse("Processor should match a specific attribute, not all attributes",
                matchingAttributeName.isMatchingAllAttributes());
        assertEquals(expectedAttr, matchingAttributeName.getMatchingAttributeName().getAttributeName());
        assertTrue(matchingAttributeName.getMatchingAttributeName().isPrefixed());
        assertEquals(PREFIX, matchingAttributeName.getMatchingAttributeName().getPrefix());
    }

    @Test
    public void testElementProcessorsAdvertiseTl3Metadata() {
        assertElement(new HeadProcessor(), "head", 10000);
        assertElement(new UncacheableDataProcessor(), "uncacheabledata", 100);
        assertElement(new GoogleUniversalAnalyticsProcessor(), "google_universal_analytics", 0);
        assertElement(new OnePageCheckoutProcessor(), "one_page_checkout", 100);
        assertElement(new ProductOptionDisplayProcessor(), "product_option_display", 100);
        assertElement(new CategoriesProcessor(), "categories", 10000);
    }

    @Test
    public void testAttributeProcessorsAdvertiseTl3Metadata() {
        assertAttribute(new AddSortLinkProcessor(), "addsortlink", 10000);
        assertAttribute(new PriceTextDisplayProcessor(), "price", 1500);
    }

    @Test
    public void testCacheProcessorIsLowestPrecedenceNoOpAttribute() {
        // blc:cache became a pass-through attribute processor; element-level caching was removed in Thymeleaf 3.
        assertAttribute(new BroadleafCacheProcessor(), "cache", Integer.MIN_VALUE);
    }

}
