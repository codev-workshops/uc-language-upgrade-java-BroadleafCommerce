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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.easymock.EasyMock;
import org.junit.Test;
import org.thymeleaf.model.IProcessableElementTag;

import java.util.Map;

/**
 * Unit tests for {@link HrefUrlRewriteProcessor} after migrating from the removed Thymeleaf 2 DOM API to the
 * Thymeleaf 3 model API. The non-CDN / non-link branch now reads the element name through
 * {@code IProcessableElementTag#getElementCompleteName()} and the matched attribute value is passed in directly
 * rather than re-read from a Thymeleaf 2 {@code Element}; this branch must leave the href untouched and never emit
 * a {@code src} attribute.
 */
public class HrefUrlRewriteProcessorTest {

    @Test
    public void testPrecedenceInheritedFromUrlRewriteProcessor() {
        assertEquals(1000, new HrefUrlRewriteProcessor().getPrecedence());
    }

    @Test
    public void testNonLinkNonCdnReturnsHrefUnchanged() {
        IProcessableElementTag tag = EasyMock.createMock(IProcessableElementTag.class);
        EasyMock.expect(tag.getElementCompleteName()).andReturn("a");
        EasyMock.expect(tag.getAttributeValue("useCDN")).andReturn(null);
        EasyMock.replay(tag);

        Map<String, String> attrs = new HrefUrlRewriteProcessor().getModifiedAttributeValues(null, tag, "/some/path.css");

        assertEquals(1, attrs.size());
        assertEquals("/some/path.css", attrs.get("href"));
        assertFalse(attrs.containsKey("src"));
        EasyMock.verify(tag);
    }

    @Test
    public void testUseCdnFalseTreatedAsNonCdn() {
        IProcessableElementTag tag = EasyMock.createMock(IProcessableElementTag.class);
        EasyMock.expect(tag.getElementCompleteName()).andReturn("script");
        EasyMock.expect(tag.getAttributeValue("useCDN")).andReturn("false");
        EasyMock.replay(tag);

        Map<String, String> attrs = new HrefUrlRewriteProcessor().getModifiedAttributeValues(null, tag, "/js/app.js");

        assertEquals("/js/app.js", attrs.get("href"));
        EasyMock.verify(tag);
    }
}
