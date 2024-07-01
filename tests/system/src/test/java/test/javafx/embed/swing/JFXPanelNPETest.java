/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.embed.swing;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class JFXPanelNPETest implements Thread.UncaughtExceptionHandler {
    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);
    private static CountDownLatch latch = new CountDownLatch(1);
    private static Throwable th;
    private static JFrame frame;

    JFrame jframe;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            Assert.assertTrue(Platform.isFxApplicationThread());
            Assert.assertNotNull(primaryStage);

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void doSetupOnce() throws Exception {
        Util.launch(launchLatch, MyApp.class);
        Assert.assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    @After
    public void doCleanup() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.dispose());
        }
    }

    @Test
    public void testRemoveAddJFXPanel() throws Throwable {

        frame = new JFrame("FX");
        final JFXPanel fxPanel = new JFXPanel();
        // fxPanel added to frame for the first time
        frame.add(fxPanel);
        frame.setSize(200, 200);
        frame.setVisible(true);

        Platform.runLater(() -> {
            Scene scene = new Scene(new Button("Testbutton"));
            fxPanel.setScene(scene);
            Thread.currentThread().setUncaughtExceptionHandler(this);
            SwingUtilities.invokeLater(() -> {
                // fxPanel removed from frame
                frame.remove(fxPanel);
                // fxPanel added to frame again
                frame.add(fxPanel); // <-- leads to NullPointerException
            });
        });
        latch.await(5, TimeUnit.SECONDS);
        System.out.println("throwable " + th);
        if (th != null) {
            throw th;
        }
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        th = throwable;
        latch.countDown();
    }
}
