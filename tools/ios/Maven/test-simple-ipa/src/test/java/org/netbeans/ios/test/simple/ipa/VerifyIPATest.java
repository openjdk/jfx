/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.ios.test.simple.ipa;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Checks that the IPA has been created on Mac.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class VerifyIPATest {
    private static File ipa;

    @BeforeClass public static void findTheIPAFile() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            throw new SkipException("Can run only on Mac OS X!");
        }

        File baseDir = new File(System.getProperty("basedir"));
        assertTrue(baseDir.isDirectory(), "Base dir exists: " + baseDir);
        ipa = new File(new File(baseDir, "target"), "test-simple-ipa.ipa");
        assertTrue(ipa.isFile(), "IPA file has been generated: " + ipa);
    }

    @Test public void verifyContent() throws IOException {
        ZipFile zip = new ZipFile(ipa);
        ZipEntry infoPList = zip.getEntry("Payload/test-simple-ipa.app/Info.plist");
        assertNotNull(infoPList, "Info.pllist property found");
    }
}
