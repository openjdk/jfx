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

package classloader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Test application for ClassLoaderTest1
 */
public class TestApp1 extends Application {

    @Override public void init() {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl == null) {
            throw new NullPointerException("TestApp1.init: Context ClassLoader is null");
        }
        if (ccl == TestApp1.class.getClassLoader()) {
            throw new RuntimeException("TestApp1.init: Context ClassLoader == this ClassLoader: "
                    + ccl);
        }
    }

    @Override public void start(final Stage stage) throws Exception {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl == null) {
            throw new NullPointerException("TestApp1.start: Context ClassLoader is null");
        }
        if (ccl == TestApp1.class.getClassLoader()) {
            throw new RuntimeException("TestApp1.start: Context ClassLoader == this ClassLoader: "
                    + ccl);
        }
        Platform.exit();
    }

}
