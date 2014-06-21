/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.application.PlatformImpl;
import java.net.URL;
import java.net.URLClassLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 *
 * @author kcr
 */
public class TestApp2 extends Application {

    private static ClassLoader origCcl = null;
    private static ClassLoader myCcl = null;

    @Override public void init() throws Exception {
        origCcl = Thread.currentThread().getContextClassLoader();
        if (origCcl == null) {
            throw new NullPointerException("TestApp2.init: origCcl is null");
        }

        // Create a new class loader and set it as the CCL
        URL[] urls = {
            new URL("file:.")
        };
        myCcl = new URLClassLoader(urls, null);
        if (myCcl == null) {
            throw new NullPointerException("TestApp2.init: myCcl is null");
        }

        PlatformImpl.runAndWait(() -> Thread.currentThread().setContextClassLoader(myCcl));
    }

    @Override public void start(final Stage stage) throws Exception {
        if (origCcl == null) {
            throw new NullPointerException("TestApp2.start: origCcl is null");
        }
        if (myCcl == null) {
            throw new NullPointerException("TestApp2.start: myCcl is null");
        }
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl != myCcl) {
            throw new RuntimeException("TestApp2.start: expected: " + myCcl + ",  actual: " + ccl);
        }

        Platform.exit();
    }

}
