/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.runtime;

import com.sun.javafx.runtime.VersionInfo;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class VersionInfoTest {

    // Increment this feature-release counter for every major release.
    private static final String FEATURE = "24";

    // The working directory at runtime is 'modules/javafx.base'.
    private static final String PROPERTIES_FILE = "build/module-lib/javafx.properties";
    private static final String VERSION_KEY = "javafx.version";
    private static final String RUNTIME_VERSION_KEY = "javafx.runtime.version";

    // See 'java.lang.Runtime.Version' for the format of a short version string.
    private static final String VNUM = "[1-9][0-9]*((\\.0)*\\.[1-9][0-9]*)*";
    private static final String PRE = "([a-zA-Z0-9]+)";
    private static final String SVSTR = String.format("%s(-%s)?", VNUM, PRE);

    private static class Version {
        private String vnum = "";
        private String suffix = "";
        private String build = "";
        private String opt = "";

        // Version number is in the following form:
        // $VNUM[-$SUFFIX][+$BUILD[-$OPT]]
        private Version(String version) {
            int plusIdx = version.indexOf("+");
            int firstDashIdx = version.indexOf("-");
            if (plusIdx < 0) {
                if (firstDashIdx >= 0) {
                    vnum = version.substring(0, firstDashIdx);
                    suffix = version.substring(firstDashIdx+1);
                } else {
                    vnum = version;
                }
            } else {
                if (firstDashIdx < 0) {
                    vnum = version.substring(0, plusIdx);
                    build = version.substring(plusIdx+1);
                } else {
                    if (firstDashIdx < plusIdx) {
                        vnum = version.substring(0, firstDashIdx);
                        suffix = version.substring(firstDashIdx+1, plusIdx);
                        String rest = version.substring(plusIdx+1);
                        int nextDashIndex = rest.indexOf("-");
                        if (nextDashIndex < 0) {
                            build = rest;
                        } else {
                            build = rest.substring(0, nextDashIndex);
                            opt = rest.substring(nextDashIndex+1);
                        }
                    } else {
                        vnum = version.substring(0, plusIdx);
                        build = version.substring(plusIdx+1, firstDashIdx);
                        opt = version.substring(firstDashIdx+1);
                    }
                }
            }

//            System.err.println("version = " + version);
//            System.err.println("    vnum = " + vnum);
//            System.err.println("    suffix = " + suffix);
//            System.err.println("    build = " + build);
//            System.err.println("    opt = " + opt);
//            System.err.println();
        }
    }

    private final Properties properties;

    public VersionInfoTest() {
        properties = new Properties();
    }

    @Before
    public void setup() throws IOException {
        try (var reader = new FileReader(PROPERTIES_FILE)) {
            properties.load(reader);
        }
    }

    @Test
    public void testMajorVersion() {
        String version = VersionInfo.getVersion();
        assertTrue(version.startsWith(FEATURE));
        String runtimeVersion = VersionInfo.getRuntimeVersion();
        assertTrue(runtimeVersion.startsWith(version));
    }

    @Test
    public void testBuildNumber() {
        String version = VersionInfo.getVersion();
        assertFalse(version.contains("+"));
        Version v = new Version(version);
        assertEquals("", v.build);
        assertEquals("", v.opt);

        String runtimeVersion = VersionInfo.getRuntimeVersion();
        assertTrue(runtimeVersion.contains("+"));
        v = new Version(runtimeVersion);
        assertTrue(v.build.length() > 0);
        int buildNum = Integer.parseInt(v.build);
        assertTrue(buildNum >= 0);
    }

    @Test
    public void testNoFcs() {
        String version = VersionInfo.getVersion();
        assertFalse(version.contains("fcs"));
        String runtimeVersion = VersionInfo.getRuntimeVersion();
        assertFalse(runtimeVersion.contains("fcs"));
    }

    @Test
    public void testSuffixOpt() {
        String runtimeVersion = VersionInfo.getRuntimeVersion();
        int internalIndex = runtimeVersion.indexOf("-internal");
        boolean isInternal = internalIndex > 0;
        Version v = new Version(runtimeVersion);
        if (isInternal) {
            assertEquals("internal", v.suffix);
            assertTrue(v.opt.length() > 0);
        } else {
            assertFalse("internal".equals(v.suffix));
        }
    }

    @Test
    public void testNonPublic() {
        String runtimeVersion = VersionInfo.getRuntimeVersion();
        Version v = new Version(runtimeVersion);
        String milestone = VersionInfo.getReleaseMilestone();
        String timestamp = VersionInfo.getBuildTimestamp();
        String hudsonJob = VersionInfo.getHudsonJobName();
        assertEquals(milestone, v.suffix);
        if (hudsonJob.length() == 0) {
            assertEquals(timestamp, v.opt);
            assertEquals("internal", v.suffix);
        } else {
            assertFalse("internal".equals(v.suffix));
        }
    }

    @Test
    public void testVersionFormat() {
        String version = VersionInfo.getVersion();
        String message = String.format("Wrong short version string: '%s'", version);
        assertTrue(message, version.matches(SVSTR));
    }

    @Test
    public void testRuntimeVersionFormat() {
        String runtimeVersion = VersionInfo.getRuntimeVersion();
        try {
            Runtime.Version.parse(runtimeVersion);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testVersionInFile() {
        String versionLive = VersionInfo.getVersion();
        String versionFile = properties.getProperty(VERSION_KEY);
        assertEquals(versionLive, versionFile);
    }

    @Test
    public void testRuntimeVersionInFile() {
        String runtimeVersionLive = VersionInfo.getRuntimeVersion();
        String runtimeVersionFile = properties.getProperty(RUNTIME_VERSION_KEY);
        assertEquals(runtimeVersionLive, runtimeVersionFile);
    }
}
