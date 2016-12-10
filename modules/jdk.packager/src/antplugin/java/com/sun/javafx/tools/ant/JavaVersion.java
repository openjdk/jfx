/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.tools.ant;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.oracle.tools.packager.Log;

public class JavaVersion {
    // for handling full vesions like 9.0.0.0-ea
    private static final Pattern fullVersionMatcher =
            Pattern.compile("((\\d+).(\\d+).(\\d+).(\\d+))(-(.*))?(\\+[^\"]*)?");
    // for handling debug vesions like 9-internal, 9-ea, etc
    private static final Pattern shortVersionMatcher =
            Pattern.compile("(\\d+)(-(.*))?(\\+[^\"]*)?");

    private String release;
    private String major;
    private String minor;
    private String security;
    private String patch;
    private String modifiers;

    private boolean empty = true;

    public JavaVersion(String version) {
        if (version != null && !version.isEmpty()) {
            Matcher matcher = fullVersionMatcher.matcher(version);
            if (matcher.find()) {
                empty = false;
                release = matcher.group(1);
                major = matcher.group(2);
                minor = matcher.group(3);
                security = matcher.group(4);
                patch = matcher.group(5);
                modifiers = matcher.group(7);
            } else {
                matcher = shortVersionMatcher.matcher(version);
                if (matcher.find()) {
                    empty = false;
                    release = matcher.group(1);
                    major = matcher.group(1);
                    modifiers = matcher.group(3);
                } else {
                    Log.info("Error: Unable to recognize java version format.");
                }
            }
        }
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean match(JavaVersion other) {
        if (major != null && other.major != null
                && !major.equals(other.major)) {
            return false;
        }

        if (minor != null && other.minor != null
                && !minor.equals(other.minor)) {
            return false;
        }

        if (security != null && other.security != null
                && !security.equals(other.security)) {
            return false;
        }

        if (patch != null && other.patch != null
                && !patch.equals(other.patch)) {
            return false;
        }

        // omit modifiers, for now..

        return true;
    }

    @Override
    public String toString() {
        return "[Release: " + release +
                "; Major: " + major +
                "; Minor: "+ minor +
                "; Security: " + security +
                "; Patch: " + patch +
                "; Modifiers: "+ modifiers+"]";
    }
}
