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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.theme.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class StyleThemeTest extends Application {

    @Override
    public void start(Stage stage) {
        setUserAgentStyleTheme(new ModenaTheme());

        var passButton = new Button("Pass");
        passButton.setOnAction(e -> Platform.exit());

        var failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            Platform.exit();
            throw new AssertionError("StyleTheme was not correctly applied");
        });

        var toggleGroup = new ToggleGroup();

        var modenaButton = new RadioButton("Modena");
        modenaButton.setOnAction(e -> setUserAgentStyleTheme(new ModenaTheme()));
        modenaButton.setToggleGroup(toggleGroup);
        modenaButton.setSelected(true);

        var caspianButton = new RadioButton("Caspian");
        caspianButton.setOnAction(e -> setUserAgentStyleTheme(new CaspianTheme()));
        caspianButton.setToggleGroup(toggleGroup);

        var box = new VBox();
        box.setSpacing(20);
        box.setPadding(new Insets(20));
        box.getChildren().add(new VBox(10,
            new VBox(5,
                new Label("1. Use the radio buttons below to switch between Modena and Caspian themes."),
                new Label("2. Observe whether the selected theme is applied to the controls in this window."),
                new Label("3. Click \"Pass\" if the selected theme is correctly applied, otherwise click \"Fail\".")),
            new HBox(5, modenaButton, caspianButton),
            new HBox(5, passButton, failButton),
            new Label("Sample controls:"),
            new CheckBox("CheckBox"),
            new Slider(),
            new Spinner(0, 100, 50)
        ));

        stage.setScene(new Scene(box));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
