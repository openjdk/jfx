/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager.bundlers;

import java.io.File;
import junit.framework.TestCase;

public class BundleParamsTest extends TestCase {

    public BundleParamsTest(String testName) {
        super(testName);
    }

    public void testValidateRuntimeLocation() {
        //File jh = new File(System.getProperty("java.home"));
        File jh = new File("/Library/Java/JavaVirtualMachines/jdk1.7.0_07.jdk/Contents/Home/jre");

        assertNotNull("Expect java.home to be ok.",
                BundleParams.validateRuntimeLocation(jh));

        assertNotNull("Expect JDK home to be ok.",
                BundleParams.validateRuntimeLocation(jh.getParentFile()));

        assertNull("Null input is ok too",
                BundleParams.validateRuntimeLocation(null));

        try {
            //should not be able to find JRE
            BundleParams.validateRuntimeLocation(new File(jh, "lib"));
        } catch (Exception e) {
            //it is expected
        }

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");
        if (isMac) {
            assertNotNull("JDK install folder on Mac is ok.",
                    BundleParams.validateRuntimeLocation(
                    jh.getParentFile().getParentFile().getParentFile()));
        }
    }
}
