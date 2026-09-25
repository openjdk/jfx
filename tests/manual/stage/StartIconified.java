/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/***
 * Stage must initially only show on the OS taskbar, but not on the Screen.
 * If the stage pops on the Screen and then iconifies, it's wrong.
 *
 * Note: Will not work on MacOS until https://bugs.openjdk.org/browse/JDK-8305675 is fixed
 */
public class StartIconified extends Application {

    @Override
    public void start(Stage primaryStage) {
        Text instructionText = new Text("""
                1. The "Iconified Window Test" must initially appear only on the operating-system taskbar or Dock.
                2. On macOS, the window may appear briefly before becoming iconified.
                On other platforms it must not appear normally on the screen before becoming iconified, even briefly.
                If it does, the test fails.
                3. Restore the iconified window and verify that it displays normally.
                """);
        instructionText.setWrappingWidth(560);

        StackPane instructionPane = new StackPane(instructionText);

        HBox passFailButtons = createPassFailButtons();

        BorderPane instructionRoot = new BorderPane();
        instructionRoot.setCenter(instructionPane);
        instructionRoot.setBottom(passFailButtons);
        instructionRoot.setPadding(new Insets(15));

        Stage instructionStage = new Stage();
        instructionStage.setTitle("Start Iconified Test Instructions");
        instructionStage.setScene(new Scene(instructionRoot, 600, 160));
        instructionStage.show();

        primaryStage.setTitle("Iconified Window Test");
        primaryStage.setWidth(700);
        primaryStage.setHeight(600);
        primaryStage.setIconified(true);

        Text text = new Text("""
                This stage must initially appear on the OS taskbar (iconified), but not on the Screen.
                """);

        Scene scene = new Scene(new StackPane(text));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createPassFailButtons() {
        var passButton = new Button("Pass");
        passButton.setOnAction(e -> {
            System.out.println("TEST PASSED");
            Platform.exit();
        });
        var failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            System.out.println("TEST FAILED");
            Platform.exit();
            throw new AssertionError("Test failed");
        });
        var hbox = new HBox(10, passButton, failButton);
        return hbox;
    }

    public static void main(String[] args) {
        launch(StartIconified.class, args);
    }
}
