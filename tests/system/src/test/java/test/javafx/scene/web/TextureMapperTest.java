/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

public class TextureMapperTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    private static TextureMapperTestApp textureMapperTestApp;
    private WebView webView;

    public static class TextureMapperTestApp extends Application {
        private Stage primaryStage;

        @Override
        public void init() {
            TextureMapperTest.textureMapperTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) {
            Platform.setImplicitExit(false);
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeAll
    public static void setupOnce() {
        Util.launch(launchLatch, TextureMapperTestApp.class);
        assertTrue(Util.await(launchLatch), "Timeout waiting for FX runtime to start");
    }

    @AfterAll
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @BeforeEach
    public void setupTestObjects() {
        Util.runAndWait(() -> {
            webView = new WebView();
            textureMapperTestApp.primaryStage.setScene(new Scene(webView, 800, 600));
            textureMapperTestApp.primaryStage.show();
        });
    }

    /**
     * @test
     * @bug 8386859
     * @summary Verify that a view transition does not crash the Java TextureMapper path.
     */
    @Test
    public void testViewTransitionDoesNotCrashTextureMapper() {
        CountDownLatch transitionStarted = new CountDownLatch(1);

        Util.runAndWait(() -> {
            WebEngine engine = webView.getEngine();
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == SUCCEEDED) {
                    engine.executeScript("if (document.startViewTransition) { document.startViewTransition(function() {}); }");
                    transitionStarted.countDown();
                }
            });
            engine.loadContent("<html></html>", "text/html");
        });

        assertTrue(Util.await(transitionStarted), "Timeout waiting for the view transition to start");
        Util.sleep(500);
    }
}
