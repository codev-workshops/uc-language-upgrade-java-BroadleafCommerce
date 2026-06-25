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
package org.broadleafcommerce.core.web.service;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Test;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;

/**
 * Exercises the Thymeleaf 3 migration of {@link SimpleCacheKeyResolver}. The resolver now reads from an
 * {@link ITemplateContext} / {@link IProcessableElementTag} instead of the Thymeleaf 2 {@code Arguments}/{@code Element}
 * pair, derives the template name from {@code element.getTemplateName()} and the line from
 * {@code element.hasLocation()/getLine()}.
 *
 * @author Devin (java17 Phase 7)
 */
public class SimpleCacheKeyResolverTest {

    @Test
    public void testResolveCacheKeyUsesTemplateNameAndLineNumber() {
        ITemplateContext context = EasyMock.createMock(ITemplateContext.class);
        IProcessableElementTag element = EasyMock.createMock(IProcessableElementTag.class);

        // No blc cache attributes are present, so no expression parsing happens.
        expect(element.hasAttribute("cacheKey")).andReturn(false);
        expect(element.hasAttribute("templateName")).andReturn(false);
        expect(element.getTemplateName()).andReturn("myTemplate");
        expect(element.hasLocation()).andReturn(true);
        expect(element.getLine()).andReturn(42);

        replay(context, element);

        SimpleCacheKeyResolver resolver = new SimpleCacheKeyResolver();
        String key = resolver.resolveCacheKey(context, element);

        assertEquals("myTemplate42", key);
        verify(context, element);
    }

    @Test
    public void testResolveCacheKeyResolvesZeroWhenNoSourceLocation() {
        ITemplateContext context = EasyMock.createMock(ITemplateContext.class);
        IProcessableElementTag element = EasyMock.createMock(IProcessableElementTag.class);

        expect(element.hasAttribute("cacheKey")).andReturn(false);
        expect(element.hasAttribute("templateName")).andReturn(false);
        expect(element.getTemplateName()).andReturn("anotherTemplate");
        // No source location available -> line number resolves to 0 (Thymeleaf 3 hasLocation()/getLine()).
        expect(element.hasLocation()).andReturn(false);

        replay(context, element);

        SimpleCacheKeyResolver resolver = new SimpleCacheKeyResolver();
        String key = resolver.resolveCacheKey(context, element);

        assertEquals("anotherTemplate0", key);
        verify(context, element);
    }

}
