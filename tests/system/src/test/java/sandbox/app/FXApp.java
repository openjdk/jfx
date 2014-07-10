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

package sandbox.app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import static sandbox.Constants.*;

/**
 * FX application to test running with a security manager installed. Note that
 * the toolkit will be initialized by the Java 8 launcher.
 */
public class FXApp extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Util.setupTimeoutThread();

        try {
            try {
                // Ensure that we are running with a restrictive
                // security manager
                System.getProperty("sun.something");
                System.err.println("*** Did not get expected security exception");
                System.exit(ERROR_NO_SECURITY_EXCEPTION);
            } catch (SecurityException ex) {
                // This is expected
            }
            Application.launch(args);
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SECURITY_EXCEPTION);
        } catch (RuntimeException ex) {
            ex.printStackTrace(System.err);
            Throwable cause = ex.getCause();
            if (cause instanceof ExceptionInInitializerError) {
                cause = cause.getCause();
                if (cause instanceof SecurityException) {
                    System.exit(ERROR_SECURITY_EXCEPTION);
                }
            }
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        } catch (Error | Exception t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override
    public void start(final Stage stage) {
        try {
            Scene scene = Util.createScene();
            stage.setScene(scene);
            stage.setX(0);
            stage.setY(0);
            stage.show();

            // Hide the stage after the specified amount of time
            KeyFrame kf = new KeyFrame(Duration.millis(SHOWTIME), e -> stage.hide());
            Timeline timeline = new Timeline(kf);
            timeline.play();
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SECURITY_EXCEPTION);
        } catch (Error | Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    @Override public void stop() {
        System.exit(ERROR_NONE);
    }

}
