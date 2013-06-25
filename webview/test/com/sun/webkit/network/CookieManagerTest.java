/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.util.TreeSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * A test for the {@link CookieManager} class.
 */
public class CookieManagerTest {

    private final CookieManager cookieManager = new CookieManager();


    /**
     * Tests retrieval of a single cookie.
     */
    @Test
    public void testGetSingleCookie() {
        put("http://example.org/", "foo=bar");
        assertEquals("foo=bar", get("http://example.org/"));
    }

    /**
     * Tests retrieval of multiple cookies.
     */
    @Test
    public void testGetMultipleCookies() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Domain=example.org",
                "quux=corge; Path=/grault");
        assertEquals("quux=corge; foo=bar; baz=qux",
                get("http://example.org/grault"));
    }

    /**
     * Tests what happens if get() is called on a CookieManager that
     * does not have any cookies.
     */
    @Test
    public void testGetNoCookies() {
        assertEquals("", get("http://example.org/"));
    }

    /**
     * Tests what happens if there are no cookies matching the get() request.
     */
    @Test
    public void testGetNoMatchingCookies() {
        put("http://subdomain.example.org/",
                "foo=bar",
                "baz=qux; Domain=example.org; Path=/quux");
        assertEquals("", get("http://example.org/"));
    }

    /**
     * Tests what happens if get() requests expired cookies.
     */
    @Test
    public void testGetExpiredCookies() {
        put("http://example.org/",
                "foo=bar; Max-Age=1",
                "baz=qux; Max-Age=0",
                "quux=courge; Expires=30 Sep 2011 00:00:00");
        assertEquals("foo=bar", get("http://example.org/"));
        sleep(1200);
        assertEquals("", get("http://example.org/"));
    }

    /**
     * Tests what happens if get() encounters domain mismatch.
     */
    @Test
    public void testGetDomainMismatch() {
        put("http://subdomain.example.org/",
                "foo=bar",
                "baz=qux; Domain=example.org");
        assertEquals("foo=bar; baz=qux", get("http://subdomain.example.org/"));
        assertEquals("baz=qux", get("http://example.org/"));
        assertEquals("", get("http://axample.org/"));
    }

    /**
     * Tests case-insensitiveness of the host component of the URI
     * for the get() method.
     */
    @Test
    public void testGetHostCaseInsensitiveness() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Domain=example.org",
                "quux=corge; Path=/grault");
        assertEquals("quux=corge; foo=bar; baz=qux",
                get("http://Example.org/grault"));
        assertEquals("quux=corge; foo=bar; baz=qux",
                get("http://EXAMPLE.ORG/grault"));
    }

    /**
     * Tests what happens if get() encounters path mismatch.
     */
    @Test
    public void testGetPathMismatch() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Path=/",
                "quux=courge; Path=/lvl1",
                "grault=garply; Path=/lvl1/lvl2");
        assertEquals("grault=garply; quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1/lvl2/lvl3/lvl4"));
        assertEquals("grault=garply; quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1/lvl2/lvl3"));
        assertEquals("grault=garply; quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1/lvl2"));
        assertEquals("quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1/lvl2A"));
        assertEquals("quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1/lvlA"));
        assertEquals("quux=courge; foo=bar; baz=qux",
                get("http://example.org/lvl1"));
        assertEquals("foo=bar; baz=qux", get("http://example.org/lvl1A"));
        assertEquals("foo=bar; baz=qux", get("http://example.org/lvlA"));
        assertEquals("foo=bar; baz=qux", get("http://example.org/"));
        assertEquals("", get("http://example.org"));
    }

    /**
     * Tests the get() method's handling of the secureOnly and httpOnly flags.
     */
    @Test
    public void testGetSecureOnlyAndHttpOnly() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Secure",
                "quux=courge; HttpOnly",
                "grault=garply; Secure; HttpOnly");
        assertEquals("foo=bar; quux=courge", get("http://example.org/"));
        assertEquals("foo=bar; baz=qux; quux=courge; grault=garply",
                get("https://example.org/"));
        assertEquals("foo=bar", get("javascript://example.org/"));
        assertEquals("foo=bar; baz=qux", get("javascripts://example.org/"));
    }

    /**
     * Tests the sorting performed by the get() method.
     */
    @Test
    public void testPathSorting() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Path=/lvl1",
                "quux=courge; Path=/lvl1/lvl2");
        assertEquals("quux=courge; baz=qux; foo=bar",
                get("http://example.org/lvl1/lvl2"));
    }

    /**
     * Tests the sorting performed by the get() method.
     */
    @Test
    public void testCreationTimeSorting() {
        put("http://example.org/", "foo=bar", "baz=qux", "quux=courge");
        assertEquals("foo=bar; baz=qux; quux=courge",
                get("http://example.org/"));

        sleep(10);

        put("http://example.org/", "foo=discard; Max-Age=0");
        put("http://example.org/", "foo=bar");
        assertEquals("baz=qux; quux=courge; foo=bar",
                get("http://example.org/"));
    }

    /**
     * Tests the sorting performed by the get() method.
     */
    @Test
    public void testPathAndCreationTimeSorting() {
        put("http://example.org/",
                "foo=bar",
                "baz=qux; Path=/lvl1",
                "quux=courge",
                "grault=garply; Path=/lvl1/lvl2");
        assertEquals("grault=garply; baz=qux; foo=bar; quux=courge",
                get("http://example.org/lvl1/lvl2"));
    }

    /**
     * Tests the get() method's argument validation.
     */
    @Test
    public void testGetArgumentValidation() {
        try {
            cookieManager.get(null, new HashMap<String,List<String>>());
            fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException expected) {}

        try {
            cookieManager.get(uri("http://hostname"), null);
            fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException expected) {}
    }

    /**
     * Tests the get() method's handling of null scheme.
     */
    @Test
    public void testGetNullScheme() {
        put("http://example.org/", "foo=bar", "baz=qux; HttpOnly");
        assertEquals("foo=bar", get("//example.org/"));
    }

    /**
     * Tests the get() method's handling of null host.
     */
    @Test
    public void testGetNullHost() {
        put("http://example.org/", "foo=bar");
        assertEquals("", get("file:///baz"));
    }

    /**
     * Tests case-insensitiveness of the host component of the URI
     * for the put() method.
     */
    @Test
    public void testPutHostCaseInsensitiveness() {
        put("http://Example.org/",
                "foo=bar",
                "baz=qux; Domain=example.org",
                "quux=corge; Path=/grault");
        assertEquals("quux=corge; foo=bar; baz=qux",
                get("http://example.org/grault"));

        put("http://EXAMPLE.ORG/",
                "foo=bar2",
                "baz=qux2; Domain=example.org",
                "quux=corge2; Path=/grault");
        assertEquals("quux=corge2; foo=bar2; baz=qux2",
                get("http://example.org/grault"));
    }

    /**
     * Tests the put() method's handling of public suffixes.
     */
    @Test
    public void testPutPublicSuffix() {
        put("http://example.org/", "foo=bar", "baz=qux; Domain=org");
        assertEquals("foo=bar", get("http://example.org/"));
        assertEquals("", get("http://org/"));
    }

    /**
     * Tests the put() method's handling of public suffixes.
     */
    @Test
    public void testPutPublicSuffixSameAsHost() {
        put("http://org/", "foo=bar", "baz=qux; Domain=org");
        assertEquals("", get("http://example.org/"));
        assertEquals("foo=bar; baz=qux", get("http://org/"));
    }

    /**
     * Tests the put() method's handling of the domain attribute.
     */
    @Test
    public void testPutDomain() {
        put("http://lvl1.example.org/", "foo=bar");
        assertEquals("", get("http://lvl2.lvl1.example.org/"));
        assertEquals("foo=bar", get("http://lvl1.example.org/"));
        assertEquals("", get("http://example.org/"));
        assertEquals("", get("http://org/"));

        put("http://lvl1.example.org/", "foo=bar; Domain=lvl1.example.org");
        assertEquals("foo=bar", get("http://lvl2.lvl1.example.org/"));
        assertEquals("foo=bar", get("http://lvl1.example.org/"));
        assertEquals("", get("http://example.org/"));
        assertEquals("", get("http://org/"));

        put("http://lvl1.example.org/", "foo=discard; Max-Age=0");
        put("http://lvl1.example.org/", "foo=bar; Domain=example.org");
        assertEquals("foo=bar", get("http://lvl2.lvl1.example.org/"));
        assertEquals("foo=bar", get("http://lvl1.example.org/"));
        assertEquals("foo=bar", get("http://example.org/"));
        assertEquals("", get("http://org/"));

        put("http://example.org/", "foo=discard; Max-Age=0");
        put("http://lvl1.example.org/", "foo=bar; Domain=org");
        assertEquals("", get("http://lvl2.lvl1.example.org/"));
        assertEquals("", get("http://lvl1.example.org/"));
        assertEquals("", get("http://example.org/"));
        assertEquals("", get("http://org/"));

        put("http://lvl1.example.org/", "foo=bar; Domain=lvlA.example.org");
        assertEquals("", get("http://lvl2.lvl1.example.org/"));
        assertEquals("", get("http://lvl1.example.org/"));
        assertEquals("", get("http://example.org/"));
        assertEquals("", get("http://org/"));

        put("http://lvl1.example.org/",
                "foo=bar; Domain=lvl2.lvl1.example.org");
        assertEquals("", get("http://lvl2.lvl1.example.org/"));
        assertEquals("", get("http://lvl1.example.org/"));
        assertEquals("", get("http://example.org/"));
        assertEquals("", get("http://org/"));
    }

    /**
     * Tests the put() method's handling of the default path.
     */
    @Test
    public void testPutDefaultPath() {
        put("http://example.org/", "foo=bar");
        assertEquals("foo=bar", get("http://example.org/"));
        assertEquals("foo=bar", get("http://example.org/dirA"));
        assertEquals("foo=bar", get("http://example.org/dirB"));

        put("http://example.org/", "foo=discard; Max-Age=0");
        put("http://example.org/dirA", "foo=bar");
        assertEquals("foo=bar", get("http://example.org/"));
        assertEquals("foo=bar", get("http://example.org/dirA"));
        assertEquals("foo=bar", get("http://example.org/dirA/dirB"));
        assertEquals("foo=bar", get("http://example.org/dirB"));

        put("http://example.org/dirA", "foo=discard; Max-Age=0");
        put("http://example.org/dirA/dirB", "foo=bar");
        assertEquals("", get("http://example.org/"));
        assertEquals("foo=bar", get("http://example.org/dirA"));
        assertEquals("foo=bar", get("http://example.org/dirA/dirB"));
        assertEquals("", get("http://example.org/dirB"));
    }

    /**
     * Tests the put() method's handling of the HttpOnly attribute.
     */
    @Test
    public void testPutHttpOnly() {
        put("javascript://example.org/", "foo=bar; HttpOnly");
        assertEquals("", get("http://example.org/"));

        put("http://example.org/", "foo=bar; HttpOnly");
        assertEquals("foo=bar", get("http://example.org/"));

        put("javascript://example.org/", "foo=qux;");
        assertEquals("foo=bar", get("http://example.org/"));

        put("http://example.org/", "foo=qux;");
        assertEquals("foo=qux", get("http://example.org/"));
    }

    /**
     * Tests if put() correctly handles overwrites.
     */
    @Test
    public void testPutOverwrite() {
        put("http://example.org/", "foo=bar", "baz=qux");
        assertEquals("foo=bar; baz=qux", get("http://example.org/"));

        sleep(10);

        put("http://example.org/", "foo=bar");
        assertEquals("foo=bar; baz=qux", get("http://example.org/"));

        put("http://example.org/", "foo=discard; Max-Age=0");
        put("http://example.org/", "foo=bar");
        assertEquals("baz=qux; foo=bar", get("http://example.org/"));
    }

    /**
     * Tests if put() correctly overwrites expired cookie.
     * This test is disabled because it takes considerable amount of time
     * to run.
     */
    @Ignore
    @Test
    public void testPutOverwriteExpired() {
        put("http://example.org/", "foo=bar; Max-Age=1; HttpOnly");
        assertEquals("", get("javascript://example.org/"));

        put("javascript://example.org/", "foo=bar");
        assertEquals("", get("javascript://example.org/"));

        sleep(1200);

        put("javascript://example.org/", "foo=bar");
        assertEquals("foo=bar", get("javascript://example.org/"));
    }

    /**
     * Tests if put() correctly handles duplicates within a single request.
     */
    @Test
    public void testPutDuplicates() {
        put("http://example.org/", "foo=bar", "foo=baz");
        assertEquals("foo=baz", get("http://example.org/"));

        put("http://example.org/", "foo=bar", "foo=baz", "foo=qux");
        assertEquals("foo=qux", get("http://example.org/"));

        put("http://example.org/", "foo=bar", "foo=discard; Max-Age=0");
        assertEquals("", get("http://example.org/"));
    }

    /**
     * Tests if put() correctly purges individual domains.
     */
    @Test
    public void testPutPurgeDomain1() {
        for (int i = 0; i < 1; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(0, 1), toSet(get("http://example.org/")));

        sleep(10);

        for (int i = 1; i < 51; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(1, 51), toSet(get("http://example.org/")));
    }

    /**
     * Tests if put() correctly purges individual domains.
     */
    @Test
    public void testPutPurgeDomain2() {
        for (int i = 0; i < 25; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(0, 25), toSet(get("http://example.org/")));

        sleep(10);

        for (int i = 25; i < 75; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(25, 75), toSet(get("http://example.org/")));
    }

    /**
     * Tests if put() correctly purges individual domains.
     */
    @Test
    public void testPutPurgeDomain3() {
        for (int i = 0; i < 50; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(0, 50), toSet(get("http://example.org/")));

        sleep(10);

        for (int i = 50; i < 100; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        assertEquals(gen(50, 100), toSet(get("http://example.org/")));
    }

    /**
     * Tests if put() correctly purges individual domains
     * and takes into account cookie expiry.
     * This test is disabled because it takes considerable amount of time
     * to run.
     */
    @Ignore
    @Test
    public void testPutPurgeDomainAfterExpiry() {
        for (int i = 0; i < 25; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 25; i < 50; i++) {
            put("http://example.org/", fmt("foo%1$d=bar%1$d; Max-Age=1", i));
        }
        assertEquals(gen(0, 50), toSet(get("http://example.org/")));

        sleep(1200);

        put("http://example.org/", "foo50=bar50");
        Set<String> expected = gen(0, 25);
        expected.add("foo50=bar50");
        assertEquals(expected, toSet(get("http://example.org/")));
    }

    private static Set<String> gen(int from, int to) {
        Set<String> set = new LinkedHashSet<String>(to - from);
        for (int i = from; i < to; i++) {
            set.add(fmt("foo%1$d=bar%1$d", i));
        }
        return set;
    }

    /**
     * Tests if put() correctly purges cookies globally.
     */
    @Test
    public void testPutPurgeCookiesGlobally1() {
        String urip = "http://example%d.org/";
        for (int i = 0; i < 10000; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        int count = 0;
        for (int i = 0; i < 10000; i++) {
            if (get(fmt(urip, i)).length() > 0) {
                count++;
            }
        }
        assertEquals(3994, count);
    }

    /**
     * Tests if put() correctly purges cookies globally.
     * This test is disabled because it takes considerable amount of time
     * to run.
     */
    @Ignore
    @Test
    public void testPutPurgeCookiesGlobally2() {
        String urip = "http://example%d.org/";
        for (int i = 0; i < 1001; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 0; i < 1001; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
        for (int i = 1001; i < 5001; i++) {
            assertEquals("", get(fmt(urip, i)));
        }

        sleep(10);

        for (int i = 1001; i < 5001; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 0; i < 1001; i++) {
            assertEquals("", get(fmt(urip, i)));
        }
        for (int i = 1001; i < 5001; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
    }

    /**
     * Tests if put() correctly purges cookies globally.
     * This test is disabled because it takes considerable amount of time
     * to run.
     */
    @Ignore
    @Test
    public void testPutPurgeCookiesGlobally3() {
        String urip = "http://example%d.org/";
        for (int i = 0; i < 2002; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 0; i < 2002; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
        for (int i = 2002; i < 6002; i++) {
            assertEquals("", get(fmt(urip, i)));
        }

        sleep(10);

        for (int i = 2002; i < 6002; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 0; i < 2002; i++) {
            assertEquals("", get(fmt(urip, i)));
        }
        for (int i = 2002; i < 6002; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
    }

    /**
     * Tests if put() correctly purges cookies globally and takes
     * into account cookie expiry.
     * This test is disabled because it takes considerable amount of time
     * to run.
     */
    @Ignore
    @Test
    public void testPutPurgeCookiesGloballyAfterExpiry() {
        String urip = "http://example%d.org/";
        for (int i = 0; i < 2000; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 2000; i < 4000; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d; Max-Age=1", i));
        }
        for (int i = 0; i < 4000; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }

        sleep(1200);

        for (int i = 4000; i < 4001; i++) {
            put(fmt(urip, i), fmt("foo%1$d=bar%1$d", i));
        }
        for (int i = 0; i < 2000; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
        for (int i = 2000; i < 4000; i++) {
            assertEquals("", get(fmt(urip, i)));
        }
        for (int i = 4000; i < 4001; i++) {
            assertEquals(fmt("foo%1$d=bar%1$d", i), get(fmt(urip, i)));
        }
    }

    /**
     * Tests the put() method's argument validation.
     */
    @Test
    public void testPutArgumentValidation() {
        try {
            cookieManager.put(null, new HashMap<String,List<String>>());
            fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException expected) {}

        try {
            cookieManager.put(uri("http://hostname"), null);
            fail("IllegalArgumentException expected but not thrown");
        } catch (IllegalArgumentException expected) {}
    }

    /**
     * Tests the put() method's handling of null scheme.
     */
    @Test
    public void testPutNullScheme() {
        put("//example.org/", "foo=bar", "baz=qux; HttpOnly");
        assertEquals("foo=bar", get("http://example.org/"));

        put("//example.org/", "foo=discard; Max-Age=0");
        assertEquals("", get("http://example.org/"));

        put("//example.org/", "foo=bar; HttpOnly");
        assertEquals("", get("http://example.org/"));

        put("http://example.org/", "foo=bar; HttpOnly");
        assertEquals("foo=bar", get("http://example.org/"));

        put("//example.org/", "foo=baz");
        assertEquals("foo=bar", get("http://example.org/"));

        put("http://example.org/", "foo=baz");
        assertEquals("foo=baz", get("http://example.org/"));
    }

    /**
     * Tests the put() method's handling of null host.
     */
    @Test
    public void testPutNullHost() {
        put("file:///baz", "foo=bar");
        assertEquals("", get("http://example.org/baz"));
    }


    private static URI uri(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    private void put(String uri, String... values) {
        Map<String,List<String>> map = new HashMap<String,List<String>>(1);
        List<String> list = new ArrayList<String>(values.length);
        for (int i = values.length - 1; i >= 0; i--) {
            list.add(values[i]);
        }
        String header;
        double d = Math.random();
        if (d < 0.33) {
            header = "Set-Cookie";
        } else if (d > 0.67) {
            header = "set-cookie";
        } else {
            header = "SET-cookie";
        }
        map.put(header, list);
        cookieManager.put(uri(uri), map);
    }

    private String get(String uri) {
        Map<String,List<String>> map = cookieManager.get(uri(uri),
                Collections.<String,List<String>>emptyMap());
        List<String> list = map.get("Cookie");
        assertEquals(list == null ? 0 : 1, map.size());
        if (list != null) {
            assertEquals(1, list.size());
            return list.get(0);
        } else {
            return "";
        }
    }

    private static void sleep(long millis) {
        long endTime = System.currentTimeMillis() + millis;
        while (true) {
            long time = System.currentTimeMillis();
            if (time >= endTime) {
                break;
            }
            try {
                Thread.sleep(endTime - time);
            } catch (InterruptedException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    private static Set<String> toSet(String s) {
        return new TreeSet<String>(Arrays.asList(s.split("; ")));
    }

    private static String fmt(String format, Object... args) {
        return String.format(format, args);
    }
}
