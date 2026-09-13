/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import test.javafx.util.OutputRedirect;
import test.util.Util;

@Timeout(value=30000, unit=TimeUnit.MILLISECONDS)
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
            Assertions.assertTrue(Platform.isFxApplicationThread());
            Assertions.assertNotNull(primaryStage);

            launchLatch.countDown();
        }
    }

    @BeforeAll
    public static void doSetupOnce() throws Exception {
        Util.launch(launchLatch, MyApp.class);
        Assertions.assertEquals(0, launchLatch.getCount());
    }

    @AfterAll
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    @AfterEach
    public void doCleanup() {
        if (jFrame != null) {
            SwingUtilities.invokeLater(() -> jFrame.dispose());
        }
    }

    @Test
    public void testSceneNPE() throws Exception {
        OutputRedirect.suppressStderr();
        try {
            SwingUtilities.invokeAndWait(JFXPanelNPETest::createUI);
            for (int i = 0; i < 300; i++) {
                SwingUtilities.invokeLater(contentPane::repaint);
                Platform.runLater(() -> contentPane.setScene(null));
                Thread.sleep(1);
                Platform.runLater(() -> contentPane.setScene(webView.getScene()));
                Thread.sleep(1);
            }
            // Wait for both threads to process the earlier runnables
            SwingUtilities.invokeAndWait(() -> {});
            Util.runAndWait(() -> {});
        } finally {
            OutputRedirect.checkAndRestoreStderr();
        }
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
