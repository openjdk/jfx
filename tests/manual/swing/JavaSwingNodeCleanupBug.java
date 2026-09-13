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

import javafx.application.Platform;
import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

import java.awt.EventQueue;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class JavaSwingNodeCleanupBug extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        passButton.setOnAction(e -> this.quit());
        failButton.setOnAction(e -> {
            this.quit();
            System.out.println("Test failed as cleaning up SwingNode caused NPE");
        });

        BorderPane pane = new BorderPane();
        HBox hbox = new HBox(10, passButton, failButton);
        hbox.setAlignment(Pos.CENTER);

        VBox rootNode = new VBox(5,
                new Label("1. This is a test for SwingNode cleanup."),
                new Label("2. A JavaFX stage will be shown"),
                new Label("3. Close the stage window by clicking on Close icon"),
                new Label("   If NPE is thrown, click on Fail or else click on Pass"),
                new Label(""),
                hbox, pane);

        stage.setTitle("JavaFX SwingNode");

        rootNode.setPadding(new Insets(5));
        Scene scene = new Scene(rootNode, 300, 200);
        stage.setScene(scene);
        stage.show();
        testNPE();
    }

    private void quit() {
        Platform.exit();
    }

    private static void testNPE() {
        Stage st = new Stage();
        st.setTitle("Second Stage");
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> {
            swingNode.setContent(new JLabel("Swing"));
        });
        st.setScene(new Scene(new BorderPane(swingNode), 200, 100));
        st.addEventHandler(WindowEvent.WINDOW_HIDDEN, evt -> {
            st.getScene().setRoot(new Group());
        });
        st.show();
    }
}
