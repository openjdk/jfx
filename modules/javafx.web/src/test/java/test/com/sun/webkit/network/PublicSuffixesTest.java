/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.network.PublicSuffixesShim;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * A test for the {@link PublicSuffixes} class.
 */
public class PublicSuffixesTest {

    /**
     * Tests a selection of domains.
     */
    @Test
    public void testSelectedDomains() {
        test("oracle.com", false);
        test("google.com", false);
        test("gmail.com", false);
        test("yahoo.com", false);
        test("facebook.com", false);
        test("linkedin.com", false);
        test("com", true);
        test("co.uk", true);
        test("org", true);
        test("gov", true);
        test("info", true);
        test("cn", true);
        test("ru", true);
        test("spb.ru", true);
    }

    /**
     * Tests a simple rule.
     */
    @Test
    public void testSimpleRule() {
        test("us.com", true);
        test("foo.us.com", false);
    }

    /**
     * Tests a wildcard rule.
     */
    @Test
    public void testWildcardRule() {
        test("mm", true);
        test("foo.mm", true);
        test("bar.foo.mm", false);
    }

    /**
     * Tests an exception rule.
     */
    @Test
    public void testExceptionRule() {
        test("metro.tokyo.jp", false);
        test("foo.metro.tokyo.jp", false);
        test("tokyo.jp", true);
        test("jp", true);
    }

    /**
     * Tests an IDN rule.
     */
    @Test
    public void testIdnRule() {
        test("xn--p1ai", true);
        test("xn--80afoajeqg5e.xn--p1ai", false);
    }


    /**
     * Tests a given domain.
     */
    private static void test(String domain, boolean expectedResult) {
        assertEquals("Unexpected result, domain: [" + domain + "],",
                expectedResult, PublicSuffixesShim.isPublicSuffix(domain));
    }
}
