/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.stage.Stage;

import static launchertest.Constants.*;

/**
 * Test application with no main method. This is launched by MainLauncherTest.
 */
public class TestPreloader extends Preloader {

    public TestPreloader() {
        if (!Platform.isFxApplicationThread()) {
            System.exit(ERROR_PRELOADER_CONSTRUCTOR_WRONG_THREAD);
        }
    }

    @Override public void init() {
        if (Platform.isFxApplicationThread()) {
            System.exit(ERROR_PRELOADER_INIT_WRONG_THREAD);
        }
    }

    @Override public void start(Stage stage) throws Exception {
        if (!Platform.isFxApplicationThread()) {
            System.exit(ERROR_PRELOADER_START_WRONG_THREAD);
        }

        Platform.runLater(new Runnable() {
            @Override public void run() {
                // No-op
            }
        });
    }

    @Override public void stop() {
        if (!Platform.isFxApplicationThread()) {
            System.exit(ERROR_PRELOADER_STOP_WRONG_THREAD);
        }
    }

    static {
        if (!Platform.isFxApplicationThread()) {
            Thread.dumpStack();
            System.exit(ERROR_PRELOADER_CLASS_INIT_WRONG_THREAD);
        }

        try {
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    // do nothing
                }
            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            System.exit(ERROR_TOOLKIT_NOT_RUNNING);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

}
