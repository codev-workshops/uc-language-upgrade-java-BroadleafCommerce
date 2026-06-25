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

import junit.framework.TestCase;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Validates that the CustomerServiceImpl correctly delegates to
 * the new Spring Security {@link PasswordEncoder} after the migration
 * from the deprecated authentication.encoding.PasswordEncoder.
 */
public class PasswordEncoderMigrationTest extends TestCase {

    private CustomerServiceImpl customerService;
    private RecordingPasswordEncoder encoder;

    @Override
    protected void setUp() throws Exception {
        customerService = new CustomerServiceImpl();
        encoder = new RecordingPasswordEncoder();
        customerService.passwordEncoderNew = encoder;
    }

    public void testEncodePasswordDelegatesToNewEncoder() {
        String raw = "testPassword123";
        String encoded = customerService.encodePassword(raw);
        assertEquals("encoded:" + raw, encoded);
        assertEquals(raw, encoder.lastEncoded);
    }

    public void testIsPasswordValidDelegatesToNewEncoder() {
        String raw = "testPassword123";
        String stored = "encoded:" + raw;
        assertTrue(customerService.isPasswordValid(raw, stored));
        assertFalse(customerService.isPasswordValid("wrong", stored));
    }

    public void testDeprecatedEncodePassDelegatesToNewEncoder() {
        String raw = "testPassword123";
        String encoded = customerService.encodePass(raw, "ignoredSalt");
        assertEquals("encoded:" + raw, encoded);
    }

    public void testDeprecatedIsPassValidDelegatesToNewEncoder() {
        String raw = "testPassword123";
        String stored = "encoded:" + raw;
        assertTrue(customerService.isPassValid(raw, stored, "ignoredSalt"));
    }

    public void testUsingDeprecatedPasswordEncoderReturnsFalse() {
        assertFalse(customerService.usingDeprecatedPasswordEncoder());
    }

    public void testGetSaltReturnsNull() {
        assertNull(customerService.getSalt(null, "password"));
    }

    /**
     * Simple PasswordEncoder that records calls for test verification.
     */
    private static class RecordingPasswordEncoder implements PasswordEncoder {
        String lastEncoded;

        @Override
        public String encode(CharSequence rawPassword) {
            lastEncoded = rawPassword.toString();
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return ("encoded:" + rawPassword).equals(encodedPassword);
        }
    }
}
