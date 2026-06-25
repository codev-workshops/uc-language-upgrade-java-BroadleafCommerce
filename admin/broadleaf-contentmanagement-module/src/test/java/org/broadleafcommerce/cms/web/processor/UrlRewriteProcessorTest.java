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
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

/**
 * Unit tests covering the behavior of {@link UrlRewriteProcessor} after its migration from the removed Thymeleaf 2
 * {@code AbstractAttributeModifierAttrProcessor} to the Thymeleaf 3 {@code AbstractAttributeTagProcessor}. The
 * precedence and the secure-request detection that drove the legacy processor must be preserved.
 */
public class UrlRewriteProcessorTest {

    @Test
    public void testPrecedenceMatchesLegacyValue() {
        assertEquals(1000, new UrlRewriteProcessor().getPrecedence());
    }

    @Test
    public void testIsRequestSecureForHttpsScheme() {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getScheme()).andReturn("HTTPS");
        EasyMock.replay(request);

        assertTrue(new UrlRewriteProcessor().isRequestSecure(request));
        EasyMock.verify(request);
    }

    @Test
    public void testIsRequestSecureForSecureFlag() {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getScheme()).andReturn("http");
        EasyMock.expect(request.isSecure()).andReturn(true);
        EasyMock.replay(request);

        assertTrue(new UrlRewriteProcessor().isRequestSecure(request));
        EasyMock.verify(request);
    }

    @Test
    public void testIsRequestInsecure() {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getScheme()).andReturn("http");
        EasyMock.expect(request.isSecure()).andReturn(false);
        EasyMock.replay(request);

        assertFalse(new UrlRewriteProcessor().isRequestSecure(request));
        EasyMock.verify(request);
    }
}
