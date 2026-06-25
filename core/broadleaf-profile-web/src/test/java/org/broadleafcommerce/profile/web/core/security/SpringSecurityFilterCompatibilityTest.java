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

import junit.framework.TestCase;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates that web-tier security filters compile and key Spring Security 5.8
 * APIs are accessible after the migration to Spring 5.3 / Spring Security 5.8.
 */
public class SpringSecurityFilterCompatibilityTest extends TestCase {

    public void testCustomerStateFilterExtendsOncePerRequestFilter() {
        assertTrue(OncePerRequestFilter.class.isAssignableFrom(CustomerStateFilter.class));
    }

    public void testRestApiCustomerStateFilterExtendsGenericFilterBean() {
        assertTrue(GenericFilterBean.class.isAssignableFrom(RestApiCustomerStateFilter.class));
    }

    public void testSessionFixationProtectionFilterExtendsGenericFilterBean() {
        assertTrue(GenericFilterBean.class.isAssignableFrom(SessionFixationProtectionFilter.class));
    }

    public void testSecurityContextHolderAccessible() {
        assertNotNull(SecurityContextHolder.getContext());
    }

    public void testAuthenticationTokenClassesLoadable() {
        assertNotNull(AnonymousAuthenticationToken.class);
        assertNotNull(RememberMeAuthenticationToken.class);
        assertNotNull(UsernamePasswordAuthenticationToken.class);
    }

    public void testScopeSessionConstant() {
        assertEquals(1, WebRequest.SCOPE_SESSION);
    }
}
