/*
 * #%L
 * BroadleafCommerce Admin Module
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
package org.broadleafcommerce.admin.server.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Verifies admin security configuration classes are accessible under Spring Security 5.8.
 */
public class AdminSecuritySpring58Test {

    @Test
    public void testSecurityContextHolderAccessible() {
        assertNotNull(SecurityContextHolder.getContext());
    }

    @Test
    public void testUsernamePasswordAuthenticationTokenLoadable() {
        assertNotNull(UsernamePasswordAuthenticationToken.class);
    }

    @Test
    public void testAuthenticationProviderLoadable() {
        assertNotNull(AuthenticationProvider.class);
    }

    @Test
    public void testAntPathRequestMatcherFromCorrectPackage() {
        AntPathRequestMatcher matcher = new AntPathRequestMatcher("/admin/**");
        assertNotNull(matcher);
        assertTrue(matcher instanceof RequestMatcher);
    }

    @Test
    public void testRequestMatcherPackageLocation() {
        try {
            Class<?> clazz = Class.forName("org.springframework.security.web.util.matcher.RequestMatcher");
            assertNotNull(clazz);
        } catch (ClassNotFoundException e) {
            assertTrue("RequestMatcher should be in org.springframework.security.web.util.matcher package", false);
        }
    }

    @Test
    public void testCsrfTokenRepositoryAccessible() {
        try {
            Class<?> csrfRepo = Class.forName("org.springframework.security.web.csrf.CsrfTokenRepository");
            assertNotNull(csrfRepo);
            Class<?> httpSessionCsrf = Class.forName("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository");
            assertNotNull(httpSessionCsrf);
        } catch (ClassNotFoundException e) {
            assertTrue("Spring Security 5.8 CSRF classes should be accessible: " + e.getMessage(), false);
        }
    }

    @Test
    public void testSecurityContextRepositoryAccessible() {
        try {
            Class<?> clazz = Class.forName("org.springframework.security.web.context.SecurityContextRepository");
            assertNotNull(clazz);
        } catch (ClassNotFoundException e) {
            assertTrue("SecurityContextRepository should be available in Spring Security 5.8", false);
        }
    }
}
