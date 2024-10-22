/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.com.sun.webkit.network;

import com.sun.webkit.network.CookieShim;
import com.sun.webkit.network.ExtendedTimeShim;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test for the {@link Cookie} class.
 */
public class CookieTest {

    /**
     * Tests parsing of a simple cookie.
     */
    @Test
    public void testParseSimple() {
        String testString = "foo=bar";
        CookieModel expected = new CookieModel("foo", "bar");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie without ';'s.
     */
    @Test
    public void testParseNoSemicolon() {
        String testString = "foo=bar,Max-Age=2,Domain=baz.qux";
        CookieModel expected = new CookieModel("foo",
                "bar,Max-Age=2,Domain=baz.qux");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie without '='s.
     */
    @Test
    public void testParseNoAssignment() {
        String testString = "foo-bar,Max-Age-2,Domain-baz.qux";
        CookieShim actual = CookieShim.parse(testString, ExtendedTimeShim.currentTime());
        assertAsExpected(testString, null, actual);
    }

    /**
     * Tests parsing of a cookie without a name.
     */
    @Test
    public void testParseNoName() {
        String testString = "=bar";
        CookieShim actual = CookieShim.parse(testString, ExtendedTimeShim.currentTime());
        assertAsExpected(testString, null, actual);
    }

    /**
     * Tests parsing of a cookie with an empty name.
     */
    @Test
    public void testParseEmptyName() {
        String testString = " =bar";
        CookieShim actual = CookieShim.parse(testString, ExtendedTimeShim.currentTime());
        assertAsExpected(testString, null, actual);

        testString = "\t=bar";
        actual = CookieShim.parse(testString, ExtendedTimeShim.currentTime());
        assertAsExpected(testString, null, actual);
    }

    /**
     * Tests parsing of a cookie without a value.
     */
    @Test
    public void testParseNoValue() {
        String testString = "foo=";
        CookieModel expected = new CookieModel("foo", "");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an empty value.
     */
    @Test
    public void testParseEmptyValue() {
        String testString = "foo= ";
        CookieModel expected = new CookieModel("foo", "");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=\t";
        expected = new CookieModel("foo", "");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an Expires attribute.
     */
    @Test
    public void testParseExpires() {
        String testString = "foo=bar; Expires=1 Jan 2011 00:00:00";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.expiryTime = 1293840000000L;
        expected.persistent = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; expires=1 Jan 1800 00:00:00";
        expected = new CookieModel("foo", "bar");
        expected.expiryTime = 0;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an illegal Expires attribute.
     */
    @Test
    public void testParseIllegalExpires() {
        String testString = "foo=bar; Expires=1 Jac 2011 00:00:00";
        CookieModel expected = new CookieModel("foo", "bar");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with a Max-Age attribute.
     */
    @Test
    public void testParseMaxAge() {
        String testString = "foo=bar; Max-Age=1";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.expiryTime = expected.creationTime.baseTime() + 1000;
        expected.persistent = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; max-age=1000000";
        expected = new CookieModel("foo", "bar");
        expected.expiryTime = expected.creationTime.baseTime() + 1000000000;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Max-Age=0";
        expected = new CookieModel("foo", "bar");
        expected.expiryTime = 0;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Max-Age=-1";
        expected = new CookieModel("foo", "bar");
        expected.expiryTime = 0;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Max-Age=" + Long.MIN_VALUE;
        expected = new CookieModel("foo", "bar");
        expected.expiryTime = 0;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        expected = new CookieModel("foo", "bar");
        long currentTimeInMillis = expected.creationTime.baseTime();
        long maxAge = (Long.MAX_VALUE - currentTimeInMillis) / 1000;
        testString = "foo=bar; Max-Age=" + maxAge;
        expected.expiryTime = currentTimeInMillis + maxAge * 1000;
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        maxAge = (Long.MAX_VALUE - currentTimeMillis()) / 1000 + 1;
        testString = "foo=bar; Max-Age=" + maxAge;
        expected = new CookieModel("foo", "bar");
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Max-Age=" + Long.MAX_VALUE;
        expected = new CookieModel("foo", "bar");
        expected.persistent = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an illegal Max-Age attribute.
     */
    @Test
    public void testParseIllegalMaxAge() {
        String testString = "foo=bar; Max-Age=baz";
        CookieModel expected = new CookieModel("foo", "bar");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Max-Age=" + Long.MAX_VALUE + "0";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with both the Max-Age and Expires attributes.
     */
    @Test
    public void testParseMaxAgeAndExpires() {
        String testString = "foo=bar; Expires=1 Jan 2011 00:00:00; Max-Age=1";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.expiryTime = expected.creationTime.baseTime() + 1000;
        expected.persistent = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with a Domain attribute.
     */
    @Test
    public void testParseDomain() {
        String testString = "foo=bar; Domain=baz";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.domain = "baz";
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; domain=.baz";
        expected = new CookieModel("foo", "bar");
        expected.domain = "baz";
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; domain=Baz";
        expected = new CookieModel("foo", "bar");
        expected.domain = "baz";
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an illegal Domain attribute.
     */
    @Test
    public void testParseIllegalDomain() {
        String testString = "foo=bar; Domain=";
        CookieModel expected = new CookieModel("foo", "bar");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with a Path attribute.
     */
    @Test
    public void testParsePath() {
        String testString = "foo=bar; Path=/";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.path = "/";
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; path=/baz";
        expected = new CookieModel("foo", "bar");
        expected.path = "/baz";
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Path=";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Path=baz";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with a Secure attribute.
     */
    @Test
    public void testParseSecure() {
        String testString = "foo=bar; Secure";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.secureOnly = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; secure";
        expected = new CookieModel("foo", "bar");
        expected.secureOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Secure=";
        expected = new CookieModel("foo", "bar");
        expected.secureOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Secure=false";
        expected = new CookieModel("foo", "bar");
        expected.secureOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Secure=0";
        expected = new CookieModel("foo", "bar");
        expected.secureOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with an HttpOnly attribute.
     */
    @Test
    public void testParseHttpOnly() {
        String testString = "foo=bar; HttpOnly";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.httpOnly = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; httponly";
        expected = new CookieModel("foo", "bar");
        expected.httpOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; HTTPONLY=";
        expected = new CookieModel("foo", "bar");
        expected.httpOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; HttpOnly=false";
        expected = new CookieModel("foo", "bar");
        expected.httpOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; HttpOnly=0";
        expected = new CookieModel("foo", "bar");
        expected.httpOnly = true;
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie without whitespace.
     */
    @Test
    public void testParseNoWhitespace() {
        String testString = "foo=bar;Max-Age=2;Domain=baz.qux";
        CookieModel expected = new CookieModel("foo", "bar");
        expected.domain = "baz.qux";
        expected.expiryTime = expected.creationTime.baseTime() + 2000;
        expected.persistent = true;
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    /**
     * Tests parsing of a cookie with unsupported attributes.
     */
    @Test
    public void testParseUnsupportedAttributes() {
        String testString = "foo=bar; baz";
        CookieModel expected = new CookieModel("foo", "bar");
        CookieShim actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; baz=qux";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; baz=qux; quux";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);

        testString = "foo=bar; Version=1";
        expected = new CookieModel("foo", "bar");
        actual = CookieShim.parse(testString, expected.creationTime);
        assertAsExpected(testString, expected, actual);
    }

    private static class CookieModel {
        private String name;
        private String value;
        private long expiryTime;
        private String domain;
        private String path;
        private ExtendedTimeShim creationTime;
        private boolean persistent;
        private boolean secureOnly;
        private boolean httpOnly;

        public CookieModel(String name, String value) {
            this.name = name;
            this.value = value;
            this.expiryTime = Long.MAX_VALUE;
            this.domain = "";
            this.creationTime = ExtendedTimeShim.currentTime();
        }
    }

    private static void assertAsExpected(String testString,
                                         CookieModel expected, CookieShim actual)
    {
        String s = ", test string: [" + testString + "],";
        if (expected == null) {
            assertEquals(expected, actual,"Unexpected cookie" + s);
            return;
        }

        assertEquals(expected.name, actual.getName(), "Unexpected name" + s);
        assertEquals(expected.value, actual.getValue(), "Unexpected value" + s);
        assertEquals(expected.expiryTime, actual.getExpiryTime(), "Unexpected expiryTime" + s);
        assertEquals(expected.domain, actual.getDomain(), "Unexpected domain" + s);
        assertEquals(expected.path, actual.getPath(), "Unexpected path" + s);
        assertTrue(expected.creationTime.equals(actual.getCreationTime()), "Unexpected creationTime" + s);
        assertEquals(expected.creationTime.baseTime(), actual.getLastAccessTime(), "Unexpected lastAccessTime" + s);
        assertEquals(expected.persistent, actual.getPersistent(), "Unexpected persistent flag" + s);
        assertEquals(false, actual.getHostOnly(), "Unexpected hostOnly flag" + s);
        assertEquals(expected.secureOnly, actual.getSecureOnly(), "Unexpected secureOnly flag" + s);
        assertEquals(expected.httpOnly, actual.getHttpOnly(), "Unexpected httpOnly flag" + s);
    }

    /**
     * Tests the hasExpired() method.
     */
    @Test
    public void testHasExpired() {
        ExtendedTimeShim currentTime = ExtendedTimeShim.currentTime();
        CookieShim cookie = CookieShim.parse("foo=bar; Max-Age=0", currentTime);
        assertTrue(cookie.hasExpired());

        cookie = CookieShim.parse("foo=bar; Max-Age=1", currentTime);
        assertFalse(cookie.hasExpired());

        currentTime = new ExtendedTimeShim(currentTime.baseTime() - 2000, 0);
        cookie = CookieShim.parse("foo=bar; Max-Age=1", currentTime);
        assertTrue(cookie.hasExpired());
    }

    /**
     * Tests the equals() and hashCode() methods.
     */
    @Test
    public void testEqualsAndHashCode() {
        ExtendedTimeShim currentTime = ExtendedTimeShim.currentTime();
        CookieShim cookie1 = CookieShim.parse("foo=bar", currentTime);
        assertTrue(cookie1.equals(cookie1));
        assertTrue(cookie1.hashCode() == cookie1.hashCode());

        CookieShim cookie2 = CookieShim.parse("foo=baz; Max-Age=1; Secure; HttpOnly",
                currentTime);
        assertTrue(cookie1.equals(cookie2));
        assertTrue(cookie2.equals(cookie1));
        assertTrue(cookie1.hashCode() == cookie2.hashCode());

        cookie2 = CookieShim.parse("foo=bar; Domain=baz", currentTime);
        assertFalse(cookie1.equals(cookie2));
        assertFalse(cookie2.equals(cookie1));

        cookie2 = CookieShim.parse("foo=bar; Path=/baz", currentTime);
        assertFalse(cookie1.equals(cookie2));
        assertFalse(cookie2.equals(cookie1));

        cookie2 = CookieShim.parse("foo=bar; Path=/", currentTime);
        assertFalse(cookie1.equals(cookie2));
        assertFalse(cookie2.equals(cookie1));

        cookie1 = CookieShim.parse("foo=bar; Domain=baz; Path=/qux", currentTime);
        assertTrue(cookie1.equals(cookie1));
        assertTrue(cookie1.hashCode() == cookie1.hashCode());

        cookie2 = CookieShim.parse(
                "foo=baz; Domain=baz; Path=/qux; Max-Age=1; Secure; HttpOnly",
                currentTime);
        assertTrue(cookie1.equals(cookie2));
        assertTrue(cookie2.equals(cookie1));
        assertTrue(cookie1.hashCode() == cookie2.hashCode());

        cookie2 = CookieShim.parse("foo=baz; domain=.baz; path=/qux", currentTime);
        assertTrue(cookie1.equals(cookie2));
        assertTrue(cookie2.equals(cookie1));
        assertTrue(cookie1.hashCode() == cookie2.hashCode());

        cookie2 = CookieShim.parse("foo=baz; Domain=baz1; Path=/qux", currentTime);
        assertFalse(cookie1.equals(cookie2));
        assertFalse(cookie2.equals(cookie1));

        cookie2 = CookieShim.parse("foo=baz; Domain=baz; Path=/qux1", currentTime);
        assertFalse(cookie1.equals(cookie2));
        assertFalse(cookie2.equals(cookie1));

        assertFalse(cookie1.equals(null));
    }

    /**
     * Tests the domainMatches() method.
     */
    @Test
    public void testDomainMatches() {
        assertTrue(CookieShim.domainMatches("foo", "foo"));
        assertTrue(CookieShim.domainMatches("foo.bar", "foo.bar"));

        assertFalse(CookieShim.domainMatches("fooa", "foob"));
        assertFalse(CookieShim.domainMatches("foo.bara", "foo.barb"));

        assertTrue(CookieShim.domainMatches("foo.bar", "bar"));
        assertTrue(CookieShim.domainMatches("foo.bar.qux", "bar.qux"));
        assertTrue(CookieShim.domainMatches("foo.bar.qux", "qux"));

        assertFalse(CookieShim.domainMatches("foo.bar", "ar"));
        assertFalse(CookieShim.domainMatches("foo.bar", ".bar"));
        assertFalse(CookieShim.domainMatches("foo.bar", "o.bar"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", "ar.qux"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", ".bar.qux"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", "o.bar.qux"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", "ux"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", ".qux"));
        assertFalse(CookieShim.domainMatches("foo.bar.qux", "r.qux"));

        assertFalse(CookieShim.domainMatches("192.168.2.1", "1"));
        assertFalse(CookieShim.domainMatches("192.168.2.1", "2.1"));
        assertFalse(CookieShim.domainMatches("192.168.2.1", "168.2.1"));
        assertTrue(CookieShim.domainMatches("192.168.2.1", "192.168.2.1"));
        assertTrue(CookieShim.domainMatches("256.168.2.1", "1"));
        assertTrue(CookieShim.domainMatches("256.168.2.1", "2.1"));
        assertTrue(CookieShim.domainMatches("256.168.2.1", "168.2.1"));
    }

    /**
     * Tests the defaultPath() method.
     */
    @Test
    public void testDefaultPath() {
        assertEquals("/foo", CookieShim.defaultPath(uri("http://hostname/foo/bar")));
        assertEquals("/foo", CookieShim.defaultPath(uri("http://hostname/foo/bar?")));
        assertEquals("/foo", CookieShim.defaultPath(uri("http://hostname/foo/bar?query")));
        assertEquals("/foo", CookieShim.defaultPath(uri("http://hostname/foo/bar?query=push")));

        assertEquals("/", CookieShim.defaultPath(uri("http://hostname")));

        assertEquals("/", CookieShim.defaultPath(uri("http://hostname/")));
        assertEquals("/", CookieShim.defaultPath(uri("http://hostname/foo")));
    }

    private static URI uri(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Tests the pathMatches() method.
     */
    @Test
    public void testPathMatches() {
        assertTrue(CookieShim.pathMatches("foo", "foo"));
        assertTrue(CookieShim.pathMatches("/foo", "/foo"));
        assertTrue(CookieShim.pathMatches("/foo/bar", "/foo/bar"));
        assertTrue(CookieShim.pathMatches("/foo/bar", "/foo/"));
        assertTrue(CookieShim.pathMatches("/foo/bar", "/foo"));
        assertTrue(CookieShim.pathMatches("/foo/bar", "/"));

        assertFalse(CookieShim.pathMatches("/foo/bar", "/foo/b"));
        assertFalse(CookieShim.pathMatches("/foo/bar", "/fo"));
        assertFalse(CookieShim.pathMatches("/foo/bar", "/f"));
        assertFalse(CookieShim.pathMatches("/foo/", "/foo/bar"));
        assertFalse(CookieShim.pathMatches("/foo", "/foo/bar"));
    }
}
