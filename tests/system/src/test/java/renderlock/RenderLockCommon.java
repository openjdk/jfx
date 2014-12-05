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

package renderlock;

import com.sun.javafx.PlatformUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.FillTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import util.Util;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static util.Util.TIMEOUT;

/**
 * Common base class for testing snapshot.
 */
public class RenderLockCommon {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        Stage primaryStage;

        @Override public void init() {
            RenderLockCommon.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            assertTrue(Platform.isFxApplicationThread());
            primaryStage.setTitle("Primary stage");
            Rectangle rect = new Rectangle(100, 50);
            FillTransition trans = new FillTransition(Duration.millis(500),
                    rect, Color.BLUE, Color.VIOLET);
            trans.setCycleCount(Timeline.INDEFINITE);
            trans.setAutoReverse(true);
            trans.play();
            Group root = new Group(rect);
            Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(210);
            primaryStage.setHeight(180);
            assertFalse(primaryStage.isShowing());
            primaryStage.show();
            assertTrue(primaryStage.isShowing());

            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void doSetupOnce() throws Exception {
        // These tests are only valid on Windows and Mac.
        // On Linux the closing of the window does not trigger a
        // focusLost event with the lock held
        assumeTrue(PlatformUtil.isMac() || PlatformUtil.isWindows());

        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[])null)).start();

        if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Application to launch");
        }

        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void doTeardownOnce() {
        Platform.exit();
    }

    // ========================== TEST CASES ==========================

    private Stage testStage;

    protected void doWindowCloseTest() throws Exception {
        final CountDownLatch alertDoneLatch = new CountDownLatch(1);
        final AtomicReference<ButtonType> alertResult = new AtomicReference<>();

        Util.runAndWait(() -> {
            Button button1 = new Button("The Button");
            button1.focusedProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue) {
                    //System.err.println("lost focus");
                    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    Thread t = new Thread(() -> {
                        Util.sleep(SLEEP_TIME);
                        //System.err.println("scheduling runLater to hide alert");
                        Platform.runLater(() -> {
                            //System.err.println("Calling alert.hide");
                            alert.hide();
                        });
                    });
                    t.start();
                    ButtonType result = alert.showAndWait().get();
                    alertResult.set(result);
                    alertDoneLatch.countDown();
                    //System.err.println("result = " + result);
                    //System.err.println("focus listener exit");
                }
            });

            Button button2 = new Button("Other Button");

            testStage = new Stage();
            testStage.setScene(new Scene(new VBox(button1, button2), 400, 300));
            button1.requestFocus();
            testStage.requestFocus();
            testStage.show();
        });

        Util.sleep(SLEEP_TIME);
        //System.err.println("scheduling runLater to hide otherStage");
        Platform.runLater(() -> {
            //System.err.println("Calling otherStage.hide");
            testStage.hide();
        });

        // Wait for results
        if (!alertDoneLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for alert to be hidden");
        }

        assertSame(ButtonType.CANCEL, alertResult.get());
    }

}
