/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager.windows;

import com.oracle.tools.packager.AbstractImageBundler;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(value = Parameterized.class)
public class RuntimeFlagsParserTest {

    @Parameterized.Parameters
    public static Collection<String[]> data() {
        return Arrays.asList(new String[][] {
                { 
                    "java version \"1.7.0_51\"\nJava(TM) SE Runtime Environment (build 1.7.0_51-b13)\nJava HotSpot(TM) 64-Bit Server VM (build 24.51-b03, mixed mode)\n",
                    "64", "1.7.0_51", "1.7.0", "51", null, "7", "51", "51", "0"
                },
                { 
                    "java version \"1.8.0_05-ea\"\nJava(TM) SE Runtime Environment (build 1.8.0_05-ea-b03)\nJava HotSpot(TM) Client VM (build 25.5-b01, mixed mode)\n",
                    "32", "1.8.0_05", "1.8.0", "05", "ea", "8", "05", "05", "0"
                },
                { 
                    "java version \"1.8.0_40\"\nJava(TM) SE Runtime Environment (build 1.8.0_40-b25)\nJava HotSpot(TM) 64-Bit Server VM (build 25.40-b25, mixed mode)",
                    "64", "1.8.0_40", "1.8.0", "40", null, "8", "40", "40", "0"
                },
                { 
                    "java version \"9.1.2.3-ea\"\nJava(TM) SE Runtime Environment (build 9.1.2.3-ea-4+2015-03-14-092653.pi.jdk9)\nJava HotSpot(TM) 64-Bit Server VM (build 25.40-b25, mixed mode)",
                    "64", "9.1.2.3", "9.1.2.3", "1", "ea", "9", "1", "2", "3"
                }
        });
    }
    
    String versionText;
    String bitArch;
    String fullVersion;
    String releaseVersion;
    String updateVersion;
    String versionModifiers;
    String versionMajor;
    String versionMinor;
    String versionSecurity;
    String versionPatch;

    public RuntimeFlagsParserTest(String versionText, String bitArch, String fullVersion, String releaseVersion, String updateVersion, String versionModifiers, String versionMajor, String versionMinor, String versionSecurity, String versionPatch) {
        this.versionText = versionText;
        this.bitArch = bitArch;
        this.fullVersion = fullVersion;
        this.releaseVersion = releaseVersion;
        this.updateVersion = updateVersion;
        this.versionModifiers = versionModifiers;
        this.versionMajor = versionMajor;
        this.versionMinor = versionMinor;
        this.versionSecurity = versionSecurity;
        this.versionPatch = versionPatch;
    }

    @Test
    public void validateVersionText() {
        Map<String, Object> params = new HashMap<>();
        AbstractImageBundler.extractFlagsFromVersion(params, versionText);

        Assert.assertEquals(bitArch, params.get(".runtime.bit-arch"));
        Assert.assertEquals(fullVersion, params.get(".runtime.version"));
        Assert.assertEquals(releaseVersion, params.get(".runtime.version.release"));
        Assert.assertEquals(updateVersion, params.get(".runtime.version.update"));
        Assert.assertEquals(versionModifiers, params.get(".runtime.version.modifiers"));
        Assert.assertEquals(versionMajor, params.get(".runtime.version.major"));
        Assert.assertEquals(versionMinor, params.get(".runtime.version.minor"));
        Assert.assertEquals(versionSecurity, params.get(".runtime.version.security"));
        Assert.assertEquals(versionPatch, params.get(".runtime.version.patch"));
    } 
}
