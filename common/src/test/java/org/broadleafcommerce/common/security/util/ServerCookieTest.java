/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
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
package org.broadleafcommerce.common.security.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ServerCookieTest {

    private String cookie(int version, String name, String value, String path, String domain, String comment,
            int maxAge, boolean secure, boolean httpOnly) {
        StringBuffer header = new StringBuffer();
        ServerCookie.appendCookieValue(header, version, name, value, path, domain, comment, maxAge, secure, httpOnly);
        return header.toString();
    }

    @Test
    public void aSimpleVersionZeroCookieIsRendered() {
        assertEquals("JSESSIONID=abc123", cookie(0, "JSESSIONID", "abc123", null, null, null, -1, false, false));
    }

    @Test
    public void pathDomainSecureAndHttpOnlyAreAppended() {
        String header = cookie(0, "name", "value", "/store", "broadleaf.org", null, -1, true, true);

        assertTrue(header.contains("; Domain=broadleaf.org"), header);
        assertTrue(header.contains("; Path=/store"), header);
        assertTrue(header.endsWith("; Secure; HttpOnly"), header);
    }

    @Test
    public void versionOneCookiesCarryTheirVersionAndComment() {
        String header = cookie(1, "name", "value", "/store", null, "a comment", 60, false, false);

        assertTrue(header.contains("; Version=1"), header);
        assertTrue(header.contains("; Comment=\"a comment\""), header);
        assertTrue(header.contains("; Max-Age=60"), header);
        assertTrue(header.contains("; Expires="), header);
    }

    @Test
    public void anImmediatelyExpiringCookieUsesTheAncientDate() {
        String header = cookie(0, "name", "value", null, null, null, 0, false, false);
        assertTrue(header.contains("; Expires=Thu, 01-Jan-1970"), header);
    }

    @Test
    public void valuesNeedingQuotesSwitchTheCookieToVersionOne() {
        StringBuffer buf = new StringBuffer();
        assertEquals(1, ServerCookie.maybeQuote2(0, buf, "needs quoting", true));
        assertEquals("\"needs quoting\"", buf.toString());
    }

    @Test
    public void emptyAndAlreadyQuotedValuesAreHandled() {
        StringBuffer empty = new StringBuffer();
        ServerCookie.maybeQuote2(0, empty, "");
        assertEquals("\"\"", empty.toString());

        StringBuffer quoted = new StringBuffer();
        ServerCookie.maybeQuote2(0, quoted, "\"already\"");
        assertEquals("\"already\"", quoted.toString());

        StringBuffer versionOne = new StringBuffer();
        ServerCookie.maybeQuote2(1, versionOne, "needs quoting");
        assertEquals("\"needs quoting\"", versionOne.toString());
    }

    @Test
    public void controlCharactersAreRejected() {
        final StringBuffer buf = new StringBuffer();
        assertThrows(IllegalArgumentException.class, () -> ServerCookie.maybeQuote2(0, buf, "bad\u0000value"));
        assertTrue(ServerCookie.containsCTL("bad\u0000value", 0));
        assertFalse(ServerCookie.containsCTL("tab\tseparated", 0));
        assertFalse(ServerCookie.containsCTL(null, 0));
    }

    @Test
    public void quotingHelpersFollowTheTokenRules() {
        assertTrue(ServerCookie.alreadyQuoted("\"value\""));
        assertFalse(ServerCookie.alreadyQuoted("value"));
        assertFalse(ServerCookie.alreadyQuoted(""));
        assertFalse(ServerCookie.alreadyQuoted(null));

        assertTrue(ServerCookie.isToken("value"));
        assertTrue(ServerCookie.isToken(null));
        assertFalse(ServerCookie.isToken("has space"));
        assertFalse(ServerCookie.isToken("a/b", "/"));

        assertTrue(ServerCookie.isToken2("value"));
        assertTrue(ServerCookie.isToken2(null));
        assertFalse(ServerCookie.isToken2("has space"));
        assertFalse(ServerCookie.isToken2("a/b", "/"));
    }

    @Test
    public void embeddedQuotesAreEscaped() {
        StringBuffer buf = new StringBuffer();
        ServerCookie.maybeQuote2(1, buf, "say \"hi\" now");
        assertEquals("\"say \\\"hi\\\" now\"", buf.toString());
    }
}
