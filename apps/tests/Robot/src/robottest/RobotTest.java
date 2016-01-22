/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package robottest;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;

/**
 *  The application should be used by QA in order to test main
 *  FX functionality
 */
public class RobotTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    //@Override
    public void start(final Stage primaryStage) {

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setX(0);
        primaryStage.setY(0);
        final VBox mainBox = new VBox(30);
        mainBox.setAlignment(Pos.CENTER);
        final Scene globalScene = new Scene(new Group(),bounds.getWidth(), bounds.getHeight());
        final RobotBuilder builder = RobotBuilder.getInstance();

        Label welcome = new Label("Welcome to Robot Test");

        Button bRobot = new Button("Robot");
        bRobot.setOnAction(e -> builder.robotTest(globalScene, mainBox, primaryStage));

        Button bquit = new Button("Quit");
        bquit.setOnAction(e -> primaryStage.close());

        mainBox.getChildren().addAll(welcome, bRobot, bquit);
        globalScene.setRoot(mainBox);
        globalScene.getStylesheets().add("robottest/RobotTestStyles.css");
        primaryStage.setScene(globalScene);
        primaryStage.show();
    }

}


