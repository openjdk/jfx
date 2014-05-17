package hello;
/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloStageOnTop extends Application{

    public static final String ENABLE_ON_TOP = "Enable on top";
    public static final String DISABLE_ON_TOP = "Disable on top";

    @Override
    public void start(Stage primaryStage) throws Exception {
        Button button = new Button("Open Root stage with child");
        CheckBox box = new CheckBox("Root is always on top");
        box.setSelected(true);
        VBox root = new VBox(15, box, button);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root);


        button.setOnAction(event -> createNewStage(0, null, box.isSelected()));

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createNewStage(int level, Stage owner, boolean onTop) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle(level == 0 ? "Root" : "Child " + level);
        stage.setAlwaysOnTop(onTop);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root);
        stage.setScene(scene);

        ToggleButton onTopButton = new ToggleButton(onTop ? DISABLE_ON_TOP : ENABLE_ON_TOP);
        onTopButton.setSelected(onTop);

        stage.alwaysOnTopProperty().addListener((observable, oldValue, newValue) -> {
            onTopButton.setSelected(newValue);
            onTopButton.setText(newValue ? DISABLE_ON_TOP : ENABLE_ON_TOP);
        });

        onTopButton.setOnAction(event -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));

        CheckBox box = new CheckBox("Child stage always on top");
        box.setSelected(true);
        Button newStageButton = new Button("Open child stage");

        newStageButton.setOnAction(event -> createNewStage(level + 1, stage, box.isSelected()));

        root.getChildren().addAll(onTopButton, box, newStageButton);

        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
