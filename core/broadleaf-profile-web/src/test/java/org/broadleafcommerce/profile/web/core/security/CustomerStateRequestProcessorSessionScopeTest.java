/*
 * #%L
 * BroadleafCommerce Profile Web
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
package org.broadleafcommerce.profile.web.core.security;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Pins the Spring 5.3 API migration in {@link CustomerStateRequestProcessor}: the anonymous-customer
 * lookups moved off the removed {@code WebRequest.SCOPE_GLOBAL_SESSION} (portlet scope, dropped in Spring 5)
 * onto {@code WebRequest.SCOPE_SESSION}. These tests assert that the anonymous customer / customer-id are
 * read and written against the standard HTTP session scope.
 *
 * @author java17-migration (Phase 4)
 */
public class CustomerStateRequestProcessorSessionScopeTest {

    private CustomerService customerService;
    private CustomerStateRequestProcessor processor;
    private MockHttpServletRequest httpRequest;
    private ServletWebRequest webRequest;

    @Before
    public void setUp() {
        customerService = createMock(CustomerService.class);
        processor = new CustomerStateRequestProcessor();
        processor.customerService = customerService;
        httpRequest = new MockHttpServletRequest();
        webRequest = new ServletWebRequest(httpRequest);
    }

    @Test
    public void resolveAnonymousCustomerCreatesAndStoresInSessionScope() {
        Customer created = new CustomerImpl();
        expect(customerService.createNewCustomer()).andReturn(created);
        replay(customerService);

        Customer resolved = processor.resolveAnonymousCustomer(webRequest);

        assertSame(created, resolved);
        // The full customer must be persisted under SESSION scope (not the removed GLOBAL_SESSION scope).
        assertSame(created, webRequest.getAttribute(
                CustomerStateRequestProcessor.getAnonymousCustomerSessionAttributeName(),
                WebRequest.SCOPE_SESSION));
        verify(customerService);
    }

    @Test
    public void getAnonymousCustomerReturnsFullCustomerFromSessionWithoutDbHit() {
        Customer inSession = new CustomerImpl();
        webRequest.setAttribute(CustomerStateRequestProcessor.getAnonymousCustomerSessionAttributeName(),
                inSession, WebRequest.SCOPE_SESSION);
        // No CustomerService interaction expected when a full customer is already in session.
        replay(customerService);

        assertSame(inSession, processor.getAnonymousCustomer(webRequest));

        verify(customerService);
    }

    @Test
    public void getAnonymousCustomerResolvesByIdFromSessionScope() {
        Customer fromDb = new CustomerImpl();
        webRequest.setAttribute(CustomerStateRequestProcessor.getAnonymousCustomerIdSessionAttributeName(),
                123L, WebRequest.SCOPE_SESSION);
        expect(customerService.readCustomerById(123L)).andReturn(fromDb);
        replay(customerService);

        assertSame(fromDb, processor.getAnonymousCustomer(webRequest));

        verify(customerService);
    }

    @Test
    public void getAnonymousCustomerReturnsNullWhenNothingInSession() {
        replay(customerService);

        assertNull(processor.getAnonymousCustomer(webRequest));

        verify(customerService);
    }
}
