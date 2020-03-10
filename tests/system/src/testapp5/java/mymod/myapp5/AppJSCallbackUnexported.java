/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package myapp5;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import myapp5.pkg1.MyCallback;

import static myapp5.Constants.*;

/**
 * Modular test application for testing Javascript callback.
 * This is launched by ModuleLauncherTest.
 */
public class AppJSCallbackUnexported extends Application {

    private static int callbackCount = -1;
    private static final CountDownLatch launchLatch = new CountDownLatch(1);
    private static final CountDownLatch contentLatch = new CountDownLatch(1);

    private final MyCallback callback = new MyCallback();
    private WebEngine webEngine;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Thread thr = new Thread(() -> {
            try {
                Application.launch(args);
            } catch (Throwable t) {
                System.err.println("ERROR: caught unexpected exception: " + t);
                t.printStackTrace(System.err);
                System.exit(ERROR_UNEXPECTED_EXCEPTION);
            }
        });
        thr.start();

        // Wait for JavaFX runtime to startup and launch the application
        waitForLatch(launchLatch, 10, "waiting for FX startup");

        // Wait for the web content to be loaded
        waitForLatch(contentLatch, 5, "loading web content");

        // Test that the callback is as expected
        try {
            Util.assertEquals(0, callbackCount);
            System.exit(ERROR_NONE);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_ASSERTION_FAILURE);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            launchLatch.countDown();
            webEngine = new WebView().getEngine();
            webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    try {
                        final JSObject window = (JSObject) webEngine.executeScript("window");
                        Util.assertNotNull(window);
                        window.setMember("javaCallback", callback);
                        webEngine.executeScript("document.getElementById(\"mybtn1\").click()");
                        callbackCount = callback.getCount();
                        contentLatch.countDown();
                    } catch (Throwable t) {
                        t.printStackTrace(System.err);
                        System.exit(ERROR_UNEXPECTED_EXCEPTION);
                    }
                }
            });
            webEngine.loadContent(Util.content);
        } catch (Error | Exception ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    public static void waitForLatch(CountDownLatch latch, int seconds, String msg) {
        try {
            if (!latch.await(seconds, TimeUnit.SECONDS)) {
                System.err.println("Timeout: " + msg);
                System.exit(ERROR_UNEXPECTED_EXCEPTION);
            }
        } catch (InterruptedException ex) {
            System.err.println("ERROR: caught unexpected exception: " + ex);
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

}
