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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
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
        primaryStage.setTitle("Iconified Window Test");
        primaryStage.setWidth(600);
        primaryStage.setHeight(150);
        primaryStage.setIconified(true);

        Text text = new Text("""
                1. The stage must initially appear on the OS taskbar (iconified), but not on the Screen
                2. Observe if the stage pops and then iconifies (wrong)""");

        Scene scene = new Scene(new StackPane(text));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(StartIconified.class, args);
    }
}
