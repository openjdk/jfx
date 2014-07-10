/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

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

import java.io.File;

/**
 *  The application should be used by QA in order to test main
 *  FX functionality
 */
public class HelloSanity extends Application {

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
        final TestBuilder builder = TestBuilder.getInstance();

        Label welcome = new Label("Welcome to Hello Sanity");

        Button bControls = new Button("Controls");
        bControls.setOnAction(e -> builder.controlTest(globalScene, mainBox));

        Button bTabs = new Button("Tabs and Menus");
        bTabs.setOnAction(e -> builder.menusTest(globalScene, mainBox, primaryStage));

        Button bWins = new Button("Windows");
        bWins.setOnAction(e -> builder.windowsTest(globalScene, mainBox, primaryStage));

        Button bAnim = new Button("Animation");
        bAnim.setOnAction(e -> builder.animationTest(globalScene, mainBox));

        Button bEffs = new Button("Effects");
        bEffs.setOnAction(e -> builder.effectsTest(globalScene, mainBox));

        Button bRobot = new Button("Robot");
        bRobot.setOnAction(e -> builder.robotTest(globalScene, mainBox, primaryStage));

        Button bgestures = new Button("Gesture Actions");
        bgestures.setOnAction(e -> builder.GestureTest(globalScene, mainBox));
        
        Button bquit = new Button("Quit");
        bquit.setOnAction(e -> primaryStage.close());
        
        mainBox.getChildren().addAll(welcome, bControls, bTabs, bWins, bRobot,
                                     bAnim, bEffs, bgestures, bquit);
        globalScene.setRoot(mainBox);
        globalScene.getStylesheets().add("hello/HelloSanityStyles.css");
        primaryStage.setScene(globalScene);
        primaryStage.show();
    }   
}


