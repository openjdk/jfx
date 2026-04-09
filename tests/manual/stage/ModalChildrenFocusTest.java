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

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Manual test for closing multiple APPLICATION_MODAL alerts of the same owner.
 *
 * Steps:
 * 1. The owner stage opens automatically.
 * 2. Three modal alerts open on top of it: Alert 1, Alert 2, Alert 3.
 * 3. Move Alert 3 aside and try clicking the owner stage.
 *    -> The owner should NOT receive focus or react to input.
 *    -> Alert 3 (the topmost modal) should remain focused.
 * 4. Close Alert 3 using its OK button -> Alert 2 should receive focus.
 * 5. Close Alert 2 using its OK button -> Alert 1 should receive focus.
 * 6. Close Alert 1 using its OK button -> Owner should receive focus.
 *
 * Expected: After each close, the previously opened modal alert (or the owner) receives focus.
 * Clicking on the owner while modals are open should NOT activate the owner.
 * The focus indicator label at the bottom-right of the owner stage shows "Focused" or "Unfocused".
 */
public class ModalChildrenFocusTest extends Application {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    @Override
    public void start(Stage primaryStage) {
        Label focusLabel = new Label("Unfocused");
        primaryStage.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                focusLabel.setText(isFocused ? "Focused" : "Unfocused"));

        Text instructions = new Text("""
                This is the owner stage.
                While modal alerts are open, clicking here
                should NOT activate this window.
                After all modal alerts are closed,
                this stage should receive focus.""");

        VBox content = new VBox(10, instructions);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(content);
        root.setBottom(focusLabel);
        BorderPane.setAlignment(focusLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(focusLabel, new Insets(5));

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Owner");
        primaryStage.setX(100);
        primaryStage.setY(100);
        primaryStage.show();

        Alert alert1 = createAlert("Alert 1", primaryStage, 150, 150);
        Alert alert2 = createAlert("Alert 2", primaryStage, 200, 200);
        Alert alert3 = createAlert("Alert 3", primaryStage, 250, 250);

        alert1.show();
        alert2.show();
        alert3.show();
    }

    private Alert createAlert(String title, Stage owner, int x, int y) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText("This is a modal alert.\n"
                + "Close this alert and verify that the\n"
                + "correct window receives focus.");
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setX(x);
        alert.setY(y);
        return alert;
    }

    public static void main(String[] args) {
        launch(ModalChildrenFocusTest.class, args);
    }
}
