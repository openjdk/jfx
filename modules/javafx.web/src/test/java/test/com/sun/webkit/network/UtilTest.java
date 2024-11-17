/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;
import static com.sun.webkit.network.Util.adjustUrlForWebKit;
import java.net.MalformedURLException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
