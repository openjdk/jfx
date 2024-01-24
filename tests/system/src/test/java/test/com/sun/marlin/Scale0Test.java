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
package test.manual.marlin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/**
 * @test
 * @bug 8312603
 * @summary Check the crash with MarlinFX renderer
 */
public class Scale0Test {

    private final static int SIZE = 800;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    static final ByteArrayOutputStream out = new ByteArrayOutputStream(2048 * 1024);
    static final PrintStream defaultErrorStream = System.err;

    static {
        Locale.setDefault(Locale.US);

        System.setProperty("prism.verbose", "false");
        // enable Marlin logging & internal checks:
        System.setProperty("prism.marlin.log", "true");
    }

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {

        Stage stage = null;

        public MyApp() {
            super();
        }

        @Override
        public void init() {
            Scale0Test.myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            this.stage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        // Capture stderr:
        System.setErr(new PrintStream(out, true));

        Util.launch(launchLatch, MyApp.class);
        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test(timeout = 15000)
    public void TestBug() {

        Platform.runLater(() -> {
            myApp.stage.setScene(createScene());
            myApp.stage.show();
        });

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ie) {
            Logger.getLogger(Scale0Test.class.getName()).log(Level.SEVERE, "interrupted", ie);
        }

        // Restore stderr:
        System.setErr(defaultErrorStream);

        // Get stderr to check exception:
        final String stdErr = out.toString();

        if (!stdErr.isEmpty()) {
            System.err.println("Captured System.err output (" + stdErr.length() + " chars):");
            System.err.println("---------------------------------------");
            System.err.println(stdErr);
            System.err.println("---------------------------------------");
        }

        if (stdErr.contains("ArrayIndexOutOfBoundsException")) {
            Assert.fail("ArrayIndexOutOfBoundsException thrown !");
        }
    }

    private Group leftPane;
    private Slider slider;

    private Scene createScene() {
        slider = new Slider(0, 2, 0) {
            {
                setBlockIncrement((getMax() - getMin()) / 4);
                setMajorTickUnit((getMax() - getMin()) / 4);
                setMinorTickCount(2);
                setPrefWidth(200);
                setShowTickLabels(true);
                setShowTickMarks(true);
            }
        };
        leftPane = new Group();

        final NodeAndGraphic leftNode = create();
        preparePane(this.leftPane, leftNode.node);

        try {
            String propertyName = "scaleXProperty"; //Works fine for translateXProperty
            Method method = leftNode.graphic.getClass().getMethod(propertyName, (Class[]) null);
            Object bindableObj = method.invoke(leftNode.graphic);
            Method bindMethod = bindableObj.getClass().getMethod("bind", ObservableValue.class);
            bindMethod.invoke(bindableObj, slider.valueProperty());
        } catch (Throwable th) {
            Logger.getLogger(Scale0Test.class.getName()).log(Level.SEVERE, "bind exception", th);
        }

        final Pane leftContainer = new Pane() {
            {
                setStyle("-fx-border-color: rosybrown;");
                getChildren().add(leftPane);
                setPrefSize(300, 300);
                setMaxSize(300, 300);
                setMinSize(300, 300);
            }
        };
        GridPane.setConstraints(leftContainer, 0, 2);

        GridPane field = new GridPane() {
            {
                getChildren().addAll(slider, leftContainer);
            }
        };

        return new Scene(field, SIZE, SIZE, Color.WHITE);
    }

    private static NodeAndGraphic create() {
        Button button = new Button();
        button.setLayoutX(50);
        button.setLayoutY(50);
        button.setPrefSize(100, 50);
        button.setMinSize(100, 50);
        button.setMaxSize(100, 50);
        if (button instanceof Labeled) {
            Labeled l = (Labeled) button;
            Circle circle = new Circle(10);
            circle.setFill(Color.LIGHTGREEN);
            circle.setStroke(Color.DARKGREEN);
            circle.getStrokeDashArray().add(10.);
            circle.getStrokeDashArray().add(8.);
            l.setGraphic(circle);
        }
        return new NodeAndGraphic(button, button.getGraphic());
    }

    private static void preparePane(Group pane, Node node) {
        pane.getChildren().clear();
        final Rectangle bounds = new Rectangle() {
            {
                setWidth(300);
                setHeight(300);
                setFill(Color.TRANSPARENT);
            }
        };

        pane.getChildren().add(bounds);
        pane.setClip(new Rectangle() {
            {
                setWidth(300);
                setHeight(300);
            }
        });
        pane.getChildren().add(node);
    }

    final static class NodeAndGraphic {

        final Node node;
        final Object graphic;

        NodeAndGraphic(Node node, Object graphic) {
            this.node = node;
            this.graphic = graphic;
        }
    }

}
