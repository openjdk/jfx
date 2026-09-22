/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DndTest extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Drag And Drop Test");

        final Text source = new Text(50, 100, "DRAG ME");
        source.setScaleX(2.0);
        source.setScaleY(2.0);

        final Text target = new Text(250, 100, "DROP HERE");
        target.setScaleX(2.0);
        target.setScaleY(2.0);

        Group group = new Group();

        Label instructions = new Label("""
                Perform each case separately. Click Reset after each case.

                1. Drag "DRAG ME" onto "DROP HERE".
                Expected: "DROP HERE" changes to "DRAG ME".

                2. Drag "DRAG ME" into a text editor that accepts text drops.
                Expected: The editor shows the text "DRAG ME".

                3. Drag "DRAG ME" onto "DROP HERE", holding Shift (Command on macOS)
                until you drop.
                Expected: "DROP HERE" changes to "DRAG ME" and the original text disappears.
                """);
        instructions.setWrapText(true);

        Button reset = new Button("Reset");
        reset.setOnAction(event -> {
            source.setText("DRAG ME");
            target.setText("DROP HERE");
            source.setFill(Color.BLACK);
            target.setFill(Color.BLACK);
        });

        VBox root = new VBox(10, instructions, group, reset);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 700, 300);

        source.setOnDragDetected(event -> {
            Dragboard db = source.startDragAndDrop(TransferMode.ANY);
            db.setDragView(source.snapshot(null, null), 0, -30);

            ClipboardContent content = new ClipboardContent();
            content.putString(source.getText());
            db.setContent(content);

            event.consume();
        });

        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target &&
                    event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }

            event.consume();
        });

        target.setOnDragEntered(event -> {
            if (event.getGestureSource() != target &&
                    event.getDragboard().hasString()) {
                target.setFill(Color.GREEN);
            }

            event.consume();
        });

        target.setOnDragExited(event -> {
            target.setFill(Color.BLACK);

            event.consume();
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                target.setText(db.getString());
                success = true;
            }
            event.setDropCompleted(success);

            event.consume();
        });

        source.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {
                source.setText("");
            }

            event.consume();
        });

        group.getChildren().add(source);
        group.getChildren().add(target);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
