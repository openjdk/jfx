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

import java.awt.EventQueue;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple SwingNode test program that displays an FX stage with an FX Label
 * and Button, and a Swing JLabel and JButton.
 */
public class SimpleSwingNodeBlurryTextTest extends Application {

    // Set to true to put the Swing JLabel and JButton into a JPanel, using
    // the JPanel as the content of the SwingNode. Set to false to use the
    // Swing JLabel and JButton, respectively, as the content of two separate
    // SwingNodes.
    private static final boolean USE_JPANEL = true;
    private static VBox rootNode = null;
    private static SwingNode swingPanel = null;
    private static SwingNode swingLabel = null;
    private static SwingNode swingButton = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        passButton.setOnAction(e -> this.quit());
        failButton.setOnAction(e -> {
            this.quit();
            throw new AssertionError("SwingNode text rendering is blurry in HIDPI scale");
        });

        BorderPane pane = new BorderPane();
        HBox hbox = new HBox(10, passButton, failButton);
        hbox.setAlignment(Pos.CENTER);

        rootNode = new VBox(5,
                new Label("1. This is a test for text rendering being not blurry on SwingNode."),
                new Label("2. A JavaFX Label and Button and Swing JLabel and JButton is shown one below the other"),
                new Label("3. If Swing JLabel/JButton text is blurry compared to JavaFX Label/Button,"),
                new Label("              click on Fail or else click on Pass"),
                new Label("4. You may need to move the window a bit "),
                new Label("       or maximize/restore the window to render the Swing contents"),
                new Label(""),
                hbox, pane);

        stage.setTitle("JavaFX SwingNode: "
                + "USE_JPANEL:" + USE_JPANEL
                + ", jdk:" + System.getProperty("java.runtime.version")
                + ", javafx:" + System.getProperty("javafx.runtime.version"));

        rootNode.setPadding(new Insets(5));
        Scene scene = new Scene(rootNode, 500, 300);
        stage.setScene(scene);
        stage.show();
        EventQueue.invokeLater(SimpleSwingNodeBlurryTextTest::initSwing);
    }

    private void quit() {
        Platform.exit();
    }

    private static void initSwing() {
        Label label = new Label("JavaFX: label");

        Button button = new Button("JavaFX: Button");
        button.setOnAction(e -> {
            System.out.print("" + Thread.currentThread() + ": ");
            System.out.println("JavaFX: Button");
        });

        JLabel jLabel = new JLabel("Swing: JLabel ");
        if (!USE_JPANEL) {
            swingLabel = new SwingNode();
            swingLabel.setContent(jLabel);
        }

        JButton jButton = new JButton("Swing: JButton");
        jButton.addActionListener(e -> {
            System.out.print("" + Thread.currentThread() + ": ");
            System.out.println("Swing: JButton in SwingNode");
        });

        if (!USE_JPANEL) {
            swingButton = new SwingNode();
            swingButton.setContent(jButton);
        }

        if (USE_JPANEL) {
            JPanel jPanel = new JPanel() {
                {
                    this.setLayout(new FlowLayout(FlowLayout.LEFT));
                    this.add(jLabel);
                    this.add(jButton);
                }
            };
            swingPanel = new SwingNode();
            swingPanel.setContent(jPanel);
        }

        Platform.runLater(() -> {
            if (USE_JPANEL) {
                VBox vbox = new VBox(new Label(""));
                HBox hbox = new HBox(10, label, button);
                rootNode.getChildren().addAll(vbox, hbox, swingPanel);
            } else {
                rootNode.getChildren().addAll(label, button, swingLabel, swingButton);
            }
        });
    }
}
