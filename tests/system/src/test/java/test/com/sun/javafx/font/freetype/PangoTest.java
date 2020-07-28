/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.font.freetype;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import junit.framework.AssertionFailedError;
import static test.util.Util.TIMEOUT;

import static org.junit.Assert.*;

/**
 * Test program for UTF16 to UTF8 conversion and Pango
 */
public class PangoTest {

    static CountDownLatch launchLatch = new CountDownLatch(1);

    static MyApp myApp;
    static Pane pane;

    public static class MyApp extends Application {

        Stage stage = null;

        public MyApp() {
            super();
        }

        @Override
        public void init() {
            myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;
            pane = new VBox(10);
            Scene scene = new Scene(pane, 400, 200);
            stage.setScene(scene);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(launchLatch::countDown));
            stage.show();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[]) null)).start();
        assertTrue("Timeout waiting for Application to launch",
                launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    private void addTextToPane(Text text) throws Exception {
        final CountDownLatch rDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            text.layoutYProperty().addListener(inv -> {
                rDone.countDown();
            });
            pane.getChildren().add(text);
        });
        assertTrue("Timeout waiting for runLater", rDone.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testZeroChar() throws Exception {
        String FULL_UNICODE_SET;
        StringBuilder builder = new StringBuilder();
        for (int character = 0; character < 10000; character++) {
             char[] chars = Character.toChars(character);
             builder.append(chars);
        }
        FULL_UNICODE_SET = builder.toString();
        Text text = new Text(FULL_UNICODE_SET);
        addTextToPane(text);
    }

    @Test
    public void testSurrogatePair() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toChars(55358));
        builder.append(Character.toChars(56605));
        builder.append(Character.toChars(8205));

        Text text = new Text(builder.toString());
        addTextToPane(text);
    }
}
