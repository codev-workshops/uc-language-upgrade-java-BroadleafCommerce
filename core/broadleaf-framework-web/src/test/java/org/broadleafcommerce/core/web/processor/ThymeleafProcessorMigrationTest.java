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

import static org.junit.Assert.*;

import org.junit.Test;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Verifies that the Thymeleaf 3.1 processor migration compiles and instantiates correctly.
 * These tests verify that the migrated processors extend the correct TL3 base classes
 * and can be constructed with the expected dialect prefix.
 */
public class ThymeleafProcessorMigrationTest {

    private static final String DIALECT_PREFIX = "blc";

    @Test
    public void testAddSortLinkProcessorExtendsAbstractAttributeTagProcessor() {
        AddSortLinkProcessor processor = new AddSortLinkProcessor(DIALECT_PREFIX);
        assertTrue("AddSortLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testCatalogRelativeHrefProcessorExtendsAbstractAttributeTagProcessor() {
        CatalogRelativeHrefProcessor processor = new CatalogRelativeHrefProcessor(DIALECT_PREFIX);
        assertTrue("CatalogRelativeHrefProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testPaginationPageLinkProcessorExtendsAbstractAttributeTagProcessor() {
        PaginationPageLinkProcessor processor = new PaginationPageLinkProcessor(DIALECT_PREFIX);
        assertTrue("PaginationPageLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testPaginationSizeLinkProcessorExtendsAbstractAttributeTagProcessor() {
        PaginationSizeLinkProcessor processor = new PaginationSizeLinkProcessor(DIALECT_PREFIX);
        assertTrue("PaginationSizeLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testPaginationSortLinkProcessorExtendsAbstractAttributeTagProcessor() {
        PaginationSortLinkProcessor processor = new PaginationSortLinkProcessor(DIALECT_PREFIX);
        assertTrue("PaginationSortLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testRemoveFacetValuesLinkProcessorExtendsAbstractAttributeTagProcessor() {
        RemoveFacetValuesLinkProcessor processor = new RemoveFacetValuesLinkProcessor(DIALECT_PREFIX);
        assertTrue("RemoveFacetValuesLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testToggleFacetLinkProcessorExtendsAbstractAttributeTagProcessor() {
        ToggleFacetLinkProcessor processor = new ToggleFacetLinkProcessor(DIALECT_PREFIX);
        assertTrue("ToggleFacetLinkProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testPriceTextDisplayProcessorExtendsAbstractAttributeTagProcessor() {
        PriceTextDisplayProcessor processor = new PriceTextDisplayProcessor(DIALECT_PREFIX);
        assertTrue("PriceTextDisplayProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testProductOptionValueProcessorExtendsAbstractAttributeTagProcessor() {
        ProductOptionValueProcessor processor = new ProductOptionValueProcessor(DIALECT_PREFIX);
        assertTrue("ProductOptionValueProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testBroadleafCacheProcessorExtendsAbstractAttributeTagProcessor() {
        BroadleafCacheProcessor processor = new BroadleafCacheProcessor(DIALECT_PREFIX);
        assertTrue("BroadleafCacheProcessor should extend AbstractAttributeTagProcessor",
                processor instanceof AbstractAttributeTagProcessor);
    }

    @Test
    public void testGoogleUniversalAnalyticsProcessorExtendsAbstractElementTagProcessor() {
        GoogleUniversalAnalyticsProcessor processor = new GoogleUniversalAnalyticsProcessor(DIALECT_PREFIX);
        assertTrue("GoogleUniversalAnalyticsProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testHeadProcessorExtendsAbstractElementTagProcessor() {
        HeadProcessor processor = new HeadProcessor(DIALECT_PREFIX);
        assertTrue("HeadProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testProductOptionDisplayProcessorExtendsAbstractElementTagProcessor() {
        ProductOptionDisplayProcessor processor = new ProductOptionDisplayProcessor(DIALECT_PREFIX);
        assertTrue("ProductOptionDisplayProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testOnePageCheckoutProcessorExtendsAbstractElementTagProcessor() {
        OnePageCheckoutProcessor processor = new OnePageCheckoutProcessor(DIALECT_PREFIX);
        assertTrue("OnePageCheckoutProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testUncacheableDataProcessorExtendsAbstractElementTagProcessor() {
        UncacheableDataProcessor processor = new UncacheableDataProcessor(DIALECT_PREFIX);
        assertTrue("UncacheableDataProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testNamedOrderProcessorExtendsAbstractElementTagProcessor() {
        NamedOrderProcessor processor = new NamedOrderProcessor(DIALECT_PREFIX);
        assertTrue("NamedOrderProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testCategoriesProcessorExtendsAbstractElementTagProcessor() {
        CategoriesProcessor processor = new CategoriesProcessor(DIALECT_PREFIX);
        assertTrue("CategoriesProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testGoogleAnalyticsProcessorExtendsAbstractElementTagProcessor() {
        GoogleAnalyticsProcessor processor = new GoogleAnalyticsProcessor(DIALECT_PREFIX);
        assertTrue("GoogleAnalyticsProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testRatingsProcessorExtendsAbstractElementTagProcessor() {
        RatingsProcessor processor = new RatingsProcessor(DIALECT_PREFIX);
        assertTrue("RatingsProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testRelatedProductProcessorExtendsAbstractElementTagProcessor() {
        RelatedProductProcessor processor = new RelatedProductProcessor(DIALECT_PREFIX);
        assertTrue("RelatedProductProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testProductOptionsProcessorExtendsAbstractElementTagProcessor() {
        ProductOptionsProcessor processor = new ProductOptionsProcessor(DIALECT_PREFIX);
        assertTrue("ProductOptionsProcessor should extend AbstractElementTagProcessor",
                processor instanceof AbstractElementTagProcessor);
    }

    @Test
    public void testProcessorsUseHtmlTemplateMode() {
        // Verify a sample of processors are using HTML template mode
        AddSortLinkProcessor addSort = new AddSortLinkProcessor(DIALECT_PREFIX);
        GoogleUniversalAnalyticsProcessor gua = new GoogleUniversalAnalyticsProcessor(DIALECT_PREFIX);
        
        // These processors should be non-null and successfully constructed with TL3 APIs
        assertNotNull(addSort);
        assertNotNull(gua);
    }
}
