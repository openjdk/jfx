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
package com.oracle.util.testing;

import java.util.Objects;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
 * Provides a framework for manual tests to display test instructions, test pane, and
 * Pass/Fail buttons.
 * <p>
 * A simple test would look like this:
 * <pre>{@code
 * public class SampleManualTest {
 *     public static void main(String[] args) throws Exception {
 *         ManualTestWindow.builder().
 *             title("Sample Manual Test").
 *             instructions(
 *                 """
 *                 Provide
 *                 multi-line instructions here.
 *                 """
 *             ).
 *             ui(() -> createTestUI()).
 *             buildAndRun();
 *     }
 *
 *     private static Node createTestUI() {
 *         return new Label("Test UI");
 *     }
 * }
 * }</pre>
 * <p>
 * The framework will create the test Application, generate the Node representing the test UI,
 * and show the test Stage, making sure the last two operations happen in the context of the
 * JavaFX application thread.
 * <p>
 * The {@link Builder#ui(Supplier)} method accepts interfaces which create the test Node.
 * For tests that require multiple windows, these windows can be created by the code specified
 * with {@link Builder#runAfter(Runnable)}.
 */
// TODO timeout
// TODO log area
// TODO screenshots on failure?
// TODO initial position?
public class ManualTestWindow {
    private static String title;
    private static String instructions;
    private static int width = 1000;
    private static int height = 800;
    private static Supplier<Node> generator;
    private static Runnable runAfter;

    private ManualTestWindow() {
    }
    
    public static Builder builder() {
        title = "Manual Test: " + extractClassName();
        return new Builder();
    }

    private static String extractClassName() {
        StackTraceElement[] ss = new Throwable().getStackTrace();
        return ss[2].getClassName();
    }

    /**
     * Manual test builder.
     */
    public static class Builder {
        /**
         * Sets the main window title.
         * When not specified, a title derived from the test class name will be used.
         * @param title the title
         * @return this {@code Builder} instance
         */
        public Builder title(String title) {
            ManualTestWindow.title = title;
            return this;
        }

        /**
         * Sets instructions to be displayed above the test main pane.
         * @param text the instruction text
         * @return this {@code Builder} instance
         */
        public Builder instructions(String text) {
            ManualTestWindow.instructions = text;
            return this;
        }

        /**
         * Determines the size of the main window.
         * @param width
         * @param height
         * @return this {@code Builder} instance
         * @defaultValue 1000 x 800
         */
        public Builder size(int width, int height) {
            ManualTestWindow.width = width;
            ManualTestWindow.height = height;
            return this;
        }

        /**
         * Sets the supplier of the test {@code Node}.
         * The test {@code Node} will be shown below the instructions and above
         * "Pass"/"Fail" buttons.
         * @param generator the {@code Supplier} of the test {@code Node}
         * @return this {@code Builder} instance
         */
        public Builder ui(Supplier<Node> generator) {
            ManualTestWindow.generator = generator;
            return this;
        }

        /**
         * Sets the code to be executed after the main window is shown.
         * @param r the code to run after the main window is shown
         * @return this {@code Builder} instance
         */
        public Builder runAfter(Runnable r) {
            ManualTestWindow.runAfter = r;
            return this;
        }

        /**
         * Creates and launches the test application and its main window.
         * This method throws a {@link NullPointerException} if either instructions
         * or test node generator is {@code null}.
         */
        public void buildAndRun() {
            Objects.requireNonNull(instructions, "instructions must not be null");
            Objects.requireNonNull(generator, "generator must not be null");
            Application.launch(TApplication.class);
        }
    }

    /** The Application to run the manual test */
    public static class TApplication extends Application {
        public TApplication() {
        }

        @Override
        public void start(Stage stage) throws Exception {
            Node content = generator.get();

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

            Button screenshotButton = new Button("Screenshot");
            screenshotButton.setMinWidth(100);
            screenshotButton.setOnAction((ev) -> {
                // TODO
            });
            screenshotButton.setDisable(true); // TODO

            Button failButton = new Button("✘ Fail");
            failButton.setMinWidth(100);
            failButton.setOnAction((ev) -> {
                // TODO encoded screenshot to stderr?
                Platform.exit();
                throw new AssertionError("Failed Manual Test: " + title);
            });
            
            Button passButton = new Button("✔ Pass");
            passButton.setMinWidth(100);
            passButton.setOnAction((ev) -> {
                Platform.exit();
            });

            HBox buttons = new HBox(
                10,
                screenshotButton,
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

            Scene scene = new Scene(vb);
            stage.setWidth(width == 0 ? 800 : width);
            stage.setHeight(height == 0 ? 600 : height);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

            if (runAfter != null) {
                Platform.runLater(runAfter);
            }
        }

        // TODO markdown to show: bold, italic, underline, bullet list, numbered list
        private Node toTextFlow(String text) {
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
    }
}
