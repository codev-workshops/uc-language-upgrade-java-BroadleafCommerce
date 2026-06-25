/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.server.security.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.broadleafcommerce.openadmin.server.security.domain.AdminUser;
import org.broadleafcommerce.openadmin.server.security.domain.AdminUserImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies the Spring Security 5.x migration of {@link AdminSecurityServiceImpl}: the deprecated externally-salted
 * {@code org.springframework.security.authentication.encoding.PasswordEncoder} / {@code SaltSource} support was removed,
 * so the service now delegates solely to the configured {@link PasswordEncoder} and never produces an external salt.
 *
 * <p>Mirrors {@code CustomerServiceImplPasswordEncoderTest} from {@code core/broadleaf-profile} (Phase 2).
 *
 * @author java17-migration (Phase 3)
 */
public class AdminSecurityServiceImplPasswordEncoderTest {

    /**
     * Deterministic stand-in for a self-salting {@link PasswordEncoder} (e.g. bcrypt) so the test does not depend on a
     * particular hashing implementation.
     */
    private static final PasswordEncoder PREFIX_ENCODER = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    };

    private AdminSecurityServiceImpl securityService;

    @Before
    public void setUp() {
        securityService = new AdminSecurityServiceImpl();
        securityService.setPasswordEncoder(PREFIX_ENCODER);
    }

    @Test
    public void encodePasswordDelegatesToConfiguredEncoder() {
        assertEquals("encoded:secret", securityService.encodePassword("secret"));
    }

    @Test
    public void isPasswordValidDelegatesToConfiguredEncoder() {
        String encoded = securityService.encodePassword("secret");
        assertTrue(securityService.isPasswordValid(encoded, "secret"));
        assertFalse(securityService.isPasswordValid(encoded, "wrong"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void saltOverloadsIgnoreExternalSalt() {
        // The external salt is no longer applied, so the salt-aware overloads match the plain ones.
        String encoded = securityService.encodePassword("secret");
        assertEquals(encoded, securityService.encodePassword("secret", "ignored-salt"));
        assertTrue(securityService.isPasswordValid(encoded, "secret", "ignored-salt"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void externalSaltIsAlwaysNull() {
        AdminUser user = new AdminUserImpl();
        assertNull(securityService.getSalt(user, "secret"));
        assertNull(securityService.getSalt());
    }

    @Test
    public void setupRejectsNonPasswordEncoderBean() {
        try {
            securityService.setPasswordEncoder("not-an-encoder");
            fail("Expected NoSuchBeanDefinitionException when the blAdminPasswordEncoder bean is not a PasswordEncoder");
        } catch (NoSuchBeanDefinitionException expected) {
            // expected
        }
    }
}
