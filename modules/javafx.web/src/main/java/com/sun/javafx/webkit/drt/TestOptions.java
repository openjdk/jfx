/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

// This file must be kept in sync with Tools/DumpRenderTree/TestOptions.cpp
package com.sun.javafx.webkit.drt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

class TestOptions {
    private final Map<String, String> testOptions = new HashMap();
    private static final String BEGIN_STRING = "webkit-test-runner [ ";
    private static final String END_STRING = " ]";

    TestOptions(final String path) {
        if (path.startsWith("https://") || path.startsWith("http://")) {
            return;
        }
        final String testPath = path.replaceFirst("file://", "");
        try (BufferedReader br = new BufferedReader(new FileReader(testPath))) {
            final String options = br.readLine();
            int beginLocation = options.indexOf(BEGIN_STRING);
            if (beginLocation < 0)
                return;
            int endLocation = options.indexOf(END_STRING, beginLocation);
            if (endLocation < 0)
                return;
            final String pairStrings[] = options.substring(beginLocation + BEGIN_STRING.length(), endLocation).split("[ ,]+");
            for (final String pair : pairStrings) {
                final String splited[] = pair.split("=", 2);
                testOptions.put(splited[0], splited[1]);
            }
        } catch(Exception e) {
            System.err.println("Exception received:" + e);
            e.printStackTrace();
        }
    }

    public Map<String, String> getOptions() {
        return testOptions;
    }

}
