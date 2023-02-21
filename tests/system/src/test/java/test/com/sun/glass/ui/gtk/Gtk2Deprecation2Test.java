/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.gtk;

import com.sun.javafx.PlatformUtil;
import java.io.ByteArrayOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

public class Gtk2Deprecation2Test extends Gtk2DeprecationCommon {

    @BeforeClass
    public static void setup() throws Exception {
        doSetup(false);
    }

    @AfterClass
    public static void teardown() {
        doTeardown();
    }

    @Test
    public void testNoDeprecationMessage() throws Exception {
        assumeTrue(PlatformUtil.isLinux());

        final String output = out.toString();
        System.err.println(output);
        assertFalse("Unexpected warning message", output.contains("WARNING"));
        assertFalse("Unexpected warning message", output.contains("deprecated"));
        assertFalse("Unexpected warning message", output.contains("removed"));
    }

}
