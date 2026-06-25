/*
 * #%L
 * BroadleafCommerce Profile
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
package org.broadleafcommerce.profile.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies the Spring Security 5.x migration of {@link CustomerServiceImpl}: the deprecated externally-salted
 * {@code org.springframework.security.authentication.encoding.PasswordEncoder} / {@code SaltSource} support was removed,
 * so the service now delegates solely to the configured {@link PasswordEncoder} and never produces an external salt.
 *
 * @author java17-migration (Phase 2)
 */
public class CustomerServiceImplPasswordEncoderTest {

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

    private CustomerServiceImpl customerService;

    @Before
    public void setUp() {
        customerService = new CustomerServiceImpl();
        customerService.setPasswordEncoder(PREFIX_ENCODER);
    }

    @Test
    public void encodePasswordDelegatesToConfiguredEncoder() {
        assertEquals("encoded:secret", customerService.encodePassword("secret"));
    }

    @Test
    public void isPasswordValidDelegatesToConfiguredEncoder() {
        String encoded = customerService.encodePassword("secret");
        assertTrue(customerService.isPasswordValid("secret", encoded));
        assertFalse(customerService.isPasswordValid("wrong", encoded));
    }

    @Test
    public void customerOverloadsIgnoreExternalSalt() {
        Customer customer = new CustomerImpl();
        // The external salt is no longer applied, so the customer-aware overloads match the plain ones.
        assertEquals(customerService.encodePassword("secret"),
                customerService.encodePassword("secret", customer));
        assertTrue(customerService.isPasswordValid("secret",
                customerService.encodePassword("secret"), customer));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void externalSaltIsAlwaysNull() {
        Customer customer = new CustomerImpl();
        assertNull(customerService.getSalt(customer));
        assertNull(customerService.getSalt(customer, "secret"));
    }

    @Test
    public void setupRejectsNonPasswordEncoderBean() {
        try {
            customerService.setPasswordEncoder("not-an-encoder");
            fail("Expected NoSuchBeanDefinitionException when the blPasswordEncoder bean is not a PasswordEncoder");
        } catch (NoSuchBeanDefinitionException expected) {
            // expected
        }
    }
}
