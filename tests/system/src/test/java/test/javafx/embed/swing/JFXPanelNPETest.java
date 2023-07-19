/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class JFXPanelNPETest {
    private static WebView webView;
    private static JFrame jFrame;
    private static JFXPanel contentPane;
    private static AtomicBoolean failure;
    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

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
        if (jFrame != null) {
            SwingUtilities.invokeLater(() -> jFrame.dispose());
        }
    }

    @Test
    public void testSceneNPE() throws Exception {
        failure = new AtomicBoolean(false);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                failure.set(true);
            }
        });
        SwingUtilities.invokeAndWait(JFXPanelNPETest::createUI);
        for (int i = 0; i < 300; i++) {
            SwingUtilities.invokeLater(contentPane::repaint);
            Platform.runLater(() -> contentPane.setScene(null));
            Thread.sleep(100);
            Platform.runLater(() -> contentPane.setScene(webView.getScene()));
            Thread.sleep(100);
        }
        System.out.println("failure = " + failure.get());
        Assert.assertFalse(failure.get());
    }

    protected static void createUI() {
        jFrame = new JFrame();
        contentPane = new JFXPanel();
        Platform.runLater(() -> fx(contentPane));
        jFrame.setContentPane(contentPane);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(400,400);
        jFrame.setVisible(true);
    }

    private static void fx(final JFXPanel contentPane) {
        webView = new WebView();
        final var engine = webView.getEngine();
        engine.loadContent("hello!");
        contentPane.setScene(new Scene(webView));
    }
}

