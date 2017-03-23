/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application;

import com.sun.javafx.application.PlatformImpl;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.*;
import static test.util.Util.TIMEOUT;

/**
 * Unit tests for Platform runLater.
 */
public class NullCCLTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    @BeforeClass
    public static void setupOnce() {
        // Create a new thread so we can set the CCL to null without affecting JUnit
        new Thread(() -> {
            // Set the CCL to null and start the JavaFX runtime
            Thread.currentThread().setContextClassLoader(null);
            Platform.setImplicitExit(false);
            PlatformImpl.startup(() -> {
                launchLatch.countDown();
            });
        }).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertNull(Thread.currentThread().getContextClassLoader());
        });
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    private Stage stage;

    @After
    public void cleanup() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        if (stage != null) {
            Platform.runLater(stage::hide);
            stage = null;
        }
    }

    private void doTest(Callable<Node> loadContent) {
        final AtomicReference<Throwable> uce = new AtomicReference<>(null);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> uce.set(e));

        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertNull(Thread.currentThread().getContextClassLoader());
            StackPane root = new StackPane();
            Scene scene = new Scene(root);
            Node content = null;
            try {
                content = loadContent.call();
            } catch (RuntimeException ex) {
                throw (RuntimeException) ex;
            } catch (Exception ex) {
                fail("Unexpected exception: " + ex);
            }
            assertNotNull(content);
            root.getChildren().add(content);

            stage = new Stage();
            stage.setScene(scene);
            stage.show();
        });
        Util.sleep(2000);
        Util.runAndWait(() -> {
            stage.hide();
            stage = null;
        });

        // Check for uncaught exception
        final Throwable e = uce.get();
        if (e != null) {
            throw new RuntimeException("UncaughtException", e);
        }
    }

    @Test
    public void testFonts() {
        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertNull(Thread.currentThread().getContextClassLoader());
            List<String> fontFamilies = Font.getFamilies();
            assertNotNull(fontFamilies);
            assertFalse(fontFamilies.isEmpty());
            List<String> fontNames = Font.getFontNames();
            assertNotNull(fontNames);
            assertFalse(fontNames.isEmpty());
        });
    }

    @Test
    public void testLabel() {
        doTest(() -> {
            Label label = new Label("This is a JavaFX label");
            return label;
        });
    }

    @Test
    public void testHTMLEditor() {
        doTest(() -> {
            HTMLEditor htmlEditor = new HTMLEditor();
            htmlEditor.setHtmlText("<html><body>Hello, World!</body></html>");
            return htmlEditor;
        });
    }

    @Test
    public void testWebView() throws Exception {
        final String HTML_FILE_NAME = "test.html";

        URL url = NullCCLTest.class.getResource(HTML_FILE_NAME);
        assertNotNull(url);
        URLConnection conn = url.openConnection();
        InputStream stream = conn.getInputStream();
        stream.close();

        final String webURLString = url.toExternalForm();

        doTest(() -> {
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            webEngine.load(webURLString);
            return webView;
        });
    }

}
