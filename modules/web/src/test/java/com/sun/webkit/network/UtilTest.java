/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import org.junit.Test;
import static com.sun.webkit.network.Util.adjustUrlForWebKit;
import java.net.MalformedURLException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UtilTest {

    @Test
    public void testAdjustUrlForWebKitZeroSlashes() throws Exception {
        assertEquals("file:///path", adjustUrlForWebKit("file:path"));
    }

    @Test
    public void testAdjustUrlForWebKitOneSlash() throws Exception {
        assertEquals("file:/path", adjustUrlForWebKit("file:/path"));
    }

    @Test
    public void testAdjustUrlForWebKitTwoSlashes() throws Exception {
        assertEquals("file://path", adjustUrlForWebKit("file://path"));
    }

    @Test
    public void testAdjustUrlForWebKitThreeSlashes() throws Exception {
        assertEquals("file:///path", adjustUrlForWebKit("file:///path"));
    }

    @Test
    public void testAdjustUrlForWebKitMultipleSlashes() throws Exception {
        assertEquals("file:////path", adjustUrlForWebKit("file:////path"));
    }

    @Test
    public void testAdjustUrlForWebKitNonFileUrl() throws Exception {
        assertEquals("http:path", adjustUrlForWebKit("http:path"));
    }

    @Test
    public void testAdjustUrlForWebKitNullPointerException() throws Exception {
        try {
            adjustUrlForWebKit(null);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testAdjustUrlForWebKitMalformedURLException() throws Exception {
        try {
            adjustUrlForWebKit("aaa:path");
            fail("MalformedURLException expected but not thrown");
        } catch (MalformedURLException expected) {}
    }
}
