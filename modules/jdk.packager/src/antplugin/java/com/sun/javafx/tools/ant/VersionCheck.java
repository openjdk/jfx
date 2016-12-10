/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


final class VersionCheck {

    private VersionCheck() {};

    private static final String RESOURCES_VERSION_PROPERTIES = "/resources/version.properties";
    private static final String JAVA_VERSION = "java.version";

    private static String getVersion() {
        String result = "";
        InputStream in = VersionCheck.class.getResourceAsStream(RESOURCES_VERSION_PROPERTIES);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            String line = reader.readLine();
            if (!line.isEmpty()) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    result = parts[1];
                }
            }
        } catch (IOException ex) {
        }

        return result;
    }

    public static boolean isSameVersion() {
        boolean result = false;

        JavaVersion jarVersion = new JavaVersion(getVersion());
        JavaVersion runtimeVersion = new JavaVersion(System.getProperty(JAVA_VERSION));

        if (!jarVersion.isEmpty() && !runtimeVersion.isEmpty() &&
            jarVersion.match(runtimeVersion)) {
            result = true;
        }

        return result;
    }
}
