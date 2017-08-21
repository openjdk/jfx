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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import static org.junit.Assert.*;
import static test.util.Util.TIMEOUT;

/**
 * Unit tests for HTMLEditor SceneGraph.
 */
public class SceneGraphHTMLEditorTest {

    private static final CountDownLatch launchLatch = new CountDownLatch(1);
    private Stage stage;

    @BeforeClass
    public static void setupOnce() throws Exception {
        Platform.setImplicitExit(false);
        PlatformImpl.startup(() -> {
            launchLatch.countDown();
        });

        if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Platform to start");
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    @After
    public void cleanup() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        if (stage != null) {
            Platform.runLater(stage::hide);
            stage = null;
        }
    }

    @Test
    public void testHTMLEditorSceneGraph() {
        final AtomicReference<Throwable> uce = new AtomicReference<>(null);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> uce.set(e));

        Util.runAndWait(() -> {
            stage = new Stage();
            Label label = new Label("Pane 1");
            StackPane pane1 = new StackPane(label);
            pane1.setPadding(new Insets(10));
            Scene scene = new Scene(pane1, 600, 400);
            stage.setScene(scene);
            stage.show();

            HTMLEditor editor = new HTMLEditor();
            StackPane pane2 = new StackPane(editor);
            pane2.setPadding(new Insets(10));

            scene.setRoot(pane2);
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
}
