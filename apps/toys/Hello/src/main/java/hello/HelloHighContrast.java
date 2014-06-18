/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloHighContrast extends Application {

    private static final String MODENA_PATH = "com/sun/javafx/scene/control/skin/modena/";
    private String lastStyleUsed = null;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        final ToggleGroup group = new ToggleGroup();
        group.selectedToggleProperty().addListener(ov -> {
            // remove old style
            if (lastStyleUsed != null) {
                StyleManager.getInstance().removeUserAgentStylesheet(MODENA_PATH + lastStyleUsed);
                lastStyleUsed = null;
            }

            // install new style
            String userData = (String) group.getSelectedToggle().getUserData();
            if (userData != null) {
                lastStyleUsed = userData;
                StyleManager.getInstance().addUserAgentStylesheet(MODENA_PATH + userData);
            }
        });
        
        ToggleButton disableHighContrast = new ToggleButton("Disable High Contrast");
        disableHighContrast.setMaxWidth(Double.MAX_VALUE);
        disableHighContrast.setUserData(null);
        disableHighContrast.setToggleGroup(group);
        disableHighContrast.setSelected(true);

        ToggleButton whiteOnBlackBtn = new ToggleButton("White on black");
        whiteOnBlackBtn.setMaxWidth(Double.MAX_VALUE);
        whiteOnBlackBtn.setUserData("whiteOnBlack.css");
        whiteOnBlackBtn.setToggleGroup(group);

        ToggleButton blackOnWhiteBtn = new ToggleButton("Black on white");
        blackOnWhiteBtn.setMaxWidth(Double.MAX_VALUE);
        blackOnWhiteBtn.setUserData("blackOnWhite.css");
        blackOnWhiteBtn.setToggleGroup(group);

        ToggleButton yellowOnBlackBtn = new ToggleButton("Yellow on black");
        yellowOnBlackBtn.setMaxWidth(Double.MAX_VALUE);
        yellowOnBlackBtn.setUserData("yellowOnBlack.css");
        yellowOnBlackBtn.setToggleGroup(group);

        VBox vbox = new VBox(10, disableHighContrast, whiteOnBlackBtn, blackOnWhiteBtn, yellowOnBlackBtn);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox);

        stage.setScene(scene);
        stage.setWidth(200);
        stage.setHeight(200);
        stage.show();
    }
}