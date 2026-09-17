/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.test.manual.util;

import java.util.Set;
import java.util.function.Predicate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * Provides the base class for manual tests which displays the test instructions,
 * the UI under test, and the Pass/Fail buttons.
 * <p>
 * Example:
 * <pre>{@code public class ManualTestExample extends ManualTestWindow {
 *    public ManualTestExample() {
 *        super(
 *            "Manual Test Example",
 *            """
 *            Instructions:
 *            1. you will see a button named "Test"
 *            2. press the button
 *            3. verify that the button can be pressed""",
 *            400, 250
 *        );
 *     }
 *
 *     public static void main(String[] args) throws Exception {
 *         launch(args);
 *     }
 *
 *     @Override
 *     protected Node createContent() {
 *         return new Button("Test");
 *     }
 * }
 * }</pre>
 */
public abstract class ManualTestWindow extends Application {
    /**
     * This method creates the {@code Node} containing elements under test,
     * to be shown below the instructions and above the "Pass"/"Fail" buttons.
     * @return the node
     */
    protected abstract Node createContent();

    private final String title;
    private final String instructions;
    private double width = 1000;
    private double height = 800;
    private int exitCode = TestRunner.CANCELLED;
    private Predicate<OS> skipCheck;

    /**
     * Construct the test window with the specified title and instructions.
     * @param title the title
     * @param instructions the instructions
     */
    public ManualTestWindow(String title, String instructions) {
        this.title = title;
        this.instructions = instructions;
    }

    /**
     * Construct the test window with the specified title, instructions, and dimensions.
     * @param title the title
     * @param instructions the instructions
     * @param width the window width
     * @param height the window height
     */
    public ManualTestWindow(String title, String instructions, double width, double height) {
        this(title, instructions);
        this.width = width;
        this.height = height;
    }

    /**
     * Configures the test to be skipped on the specfied platform(s).
     * @param platforms the platforms to skip
     * @throws IllegalArgumentException if the skip logic has already been configured
     * @see #setRunOn
     */
    public void setSkipOn(OS... platforms) {
        if (skipCheck != null) {
            throw new IllegalArgumentException("skip has already been configured");
        }
        Set<OS> oss = Set.of(platforms);
        skipCheck = (os) -> {
            return oss.contains(os);
        };
    }

    /**
     * Configures the test to be allowed on the specfied platform(s), skipped on all the others.
     * @param platforms
     * @throws IllegalArgumentException if the skip logic has already been configured
     * @see #setSkipOn
     */
    public void setRunOn(OS... platforms) {
        if(skipCheck != null) {
            throw new IllegalArgumentException("skip has already been configured");
        }
        Set<OS> oss = Set.of(platforms);
        skipCheck = (os) -> {
            return !oss.contains(os);
        };
    }

    private Parent createContent(Stage stage) {
        stage.setOnHiding((ev) -> {
            exit(exitCode);
        });
        Node content = createContent();

        BlurType blurType = BlurType.GAUSSIAN;
        Color color = Color.gray(0, 0.5);
        double radius = 10;
        double spread = 0;
        double offsetX = 1;
        double offsetY = 1;
        DropShadow shadow = new DropShadow(blurType, color, radius, spread, offsetX, offsetY);

        BorderPane cp = new BorderPane(content);
        cp.setMargin(content, new Insets(10));
        cp.setBackground(Background.fill(Color.gray(1)));
        cp.setEffect(shadow);

        Node instructionField = toTextFlow(instructions);

        Region fill = new Region();

        Button failButton = new Button("Fail");
        setIcon(failButton, "✘", Color.RED);
        failButton.setMinWidth(100);
        failButton.setOnAction((ev) -> {
            exit(TestRunner.FAILED);
            throw new AssertionError("Failed Manual Test: " + stage.getTitle());
        });

        Button passButton = new Button("Pass");
        setIcon(passButton, "✔", Color.GREEN);
        passButton.setMinWidth(100);
        passButton.setOnAction((ev) -> {
            exit(TestRunner.PASSED);
        });

        HBox buttons = new HBox(
            10,
            fill,
            failButton,
            passButton
        );
        HBox.setHgrow(fill, Priority.ALWAYS);

        VBox vb = new VBox(
            10,
            instructionField,
            cp,
            buttons
        );
        vb.setPadding(new Insets(10));
        VBox.setVgrow(cp, Priority.ALWAYS);
        return vb;
    }

    private void exit(int code) {
        exitCode = code;
        Platform.exit();
        String s = TestRunnerApp.getExitCodeString(code);
        IO.println(title + ": " + s);
        System.exit(code);
    }

    /**
     * Prepares the Application primary stage: creates the content {@code Node} to be tested,
     * creates the manual test UI, sets the {@code Scene}.
     * This method is called before the primary stage is shown.
     * @param stage the primary stage
     */
    protected void prepareStage(Stage stage) {
        // determine if this test needs to be skipped on this platform
        OS os = OS.current();
        boolean skip = OS.UNKNOWN.equals(os) || ((skipCheck != null) && (skipCheck.test(os)));
        if (skip) {
            Alert a = new Alert(AlertType.NONE, "This test is not designed for this platform.", new ButtonType("Skip"));
            a.setTitle("Skip");
            a.showAndWait();
            exit(TestRunner.SKIPPED);
        } else {
            Parent content = createContent(stage);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setTitle(title);
            stage.setScene(new Scene(content));
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        prepareStage(stage);
        stage.show();
    }

    private static Node toTextFlow(String text) {
        TextFlow f = new TextFlow();
        Text t = new Text(text);
        f.getChildren().add(t);
        f.setOnContextMenuRequested((ev) -> {
            ContextMenu m = new ContextMenu();
            MenuItem mi = new MenuItem("Copy Instructions");
            mi.setOnAction((e) -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(text);
                Clipboard.getSystemClipboard().setContent(cc);
            });
            m.getItems().setAll(mi);
            m.show(f, ev.getScreenX(), ev.getScreenY());
        });
        return f;
    }

    private static void setIcon(Button b, String text, Color c) {
        Text t = new Text(text);
        t.setFill(c);
        b.setGraphic(t);
    }
}
