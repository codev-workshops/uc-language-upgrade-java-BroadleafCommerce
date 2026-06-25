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
package org.broadleafcommerce.cms.web.processor;

import org.broadleafcommerce.common.extension.ExtensionResultStatusType;
import org.broadleafcommerce.common.web.deeplink.DeepLink;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests verifying that Thymeleaf 3 content processors compile and initialize correctly
 * after the migration from Thymeleaf 2.
 */
public class ContentProcessorThymeleaf3Test {

    @Test
    public void testContentProcessorInstantiation() {
        ContentProcessor processor = new ContentProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testContentProcessorWithCustomElement() {
        ContentProcessor processor = new ContentProcessor("blc", "customContent");
        assertNotNull(processor);
    }

    @Test
    public void testUrlRewriteProcessorInstantiation() {
        UrlRewriteProcessor processor = new UrlRewriteProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testHrefUrlRewriteProcessorInstantiation() {
        HrefUrlRewriteProcessor processor = new HrefUrlRewriteProcessor();
        assertNotNull(processor);
    }

    @Test
    public void testHrefUrlRewriteExtendsUrlRewrite() {
        HrefUrlRewriteProcessor processor = new HrefUrlRewriteProcessor();
        assertTrue(processor instanceof UrlRewriteProcessor);
    }

    @Test
    public void testContentProcessorExtensionHandlerInterface() {
        AbstractContentProcessorExtensionHandler handler = new AbstractContentProcessorExtensionHandler() {};
        
        ExtensionResultStatusType result = handler.addAdditionalFieldsToModel(null, null);
        assertEquals(ExtensionResultStatusType.NOT_HANDLED, result);
    }

    @Test
    public void testExtensionHandlerAddExtensionFieldDeepLink() {
        AbstractContentProcessorExtensionHandler handler = new AbstractContentProcessorExtensionHandler() {};
        List<DeepLink> links = new ArrayList<>();
        
        ExtensionResultStatusType result = handler.addExtensionFieldDeepLink(links, null, null);
        assertEquals(ExtensionResultStatusType.NOT_HANDLED, result);
    }

    @Test
    public void testExtensionHandlerPostProcessDeepLinks() {
        AbstractContentProcessorExtensionHandler handler = new AbstractContentProcessorExtensionHandler() {};
        List<DeepLink> links = new ArrayList<>();
        
        ExtensionResultStatusType result = handler.postProcessDeepLinks(links);
        assertEquals(ExtensionResultStatusType.NOT_HANDLED, result);
    }

    @Test
    public void testContentProcessorExtensionManager() {
        ContentProcessorExtensionManager manager = new ContentProcessorExtensionManager();
        assertNotNull(manager);
        assertTrue(manager.continueOnHandled());
    }
}
