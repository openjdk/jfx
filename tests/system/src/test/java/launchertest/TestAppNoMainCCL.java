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

package launchertest;

import java.net.URL;
import java.net.URLClassLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import static launchertest.Constants.*;

/**
 * Test application with no main method. This is launched by MainLauncherTest.
 */
public class TestAppNoMainCCL extends Application {

    private static volatile ClassLoader savedCcl;

    public TestAppNoMainCCL() {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl != savedCcl) {
            System.err.println("Unexpected CCL in constructor: " + ccl);
            System.exit(ERROR_CONSTRUCTOR_WRONG_CCL);
        }
    }

    @Override public void start(Stage stage) {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (ccl != savedCcl) {
            System.err.println("Unexpected CCL in start: " + ccl);
            System.exit(ERROR_START_WRONG_CCL);
        }

        Platform.runLater(() -> Platform.exit());
    }

    @Override public void stop() {
        System.exit(ERROR_NONE);
    }

    static {
        try {
            Platform.runLater(() -> {
                // do nothing
            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            System.exit(ERROR_TOOLKIT_NOT_RUNNING);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }

        try {
            savedCcl = new URLClassLoader(new URL[] { new URL("file:.") });
            Thread.currentThread().setContextClassLoader(savedCcl);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

}
