/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NotResizableWindowTest extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception {
        Button openDialogButton = new Button("Press this button");
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        openDialogButton.setOnAction((e)->{
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(primaryStage);
            dialog.setContentText("Press Close button in dialog");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.show();
        });
        passButton.setOnAction((e)->{
            quit();
        });
        failButton.setOnAction((e)->{
            quit();
            throw new AssertionError("The window buttons are not same");
        });


        VBox root = new VBox(8,
                new Label("Check window button state before and after clicking dialog button"),
                new Label("If the state is the same as before, Press Pass otherwise Fail"),
                openDialogButton,passButton,failButton);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void quit() {
        Platform.exit();
    }
    public static void main(String[] args) {
        Application.launch(args);

    }

}
