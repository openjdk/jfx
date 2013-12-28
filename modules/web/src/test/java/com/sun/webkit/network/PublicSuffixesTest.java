/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

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
        test("ar", true);
        test("foo.ar", true);
        test("bar.foo.ar", false);
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
                expectedResult, PublicSuffixes.isPublicSuffix(domain));
    }
}
