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
package com.oracle.tools.fx.monkey;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class ExtendedWindow {

    private ExtendedWindow() {}

    public static void showSimpleHeaderBar(StageStyle style, NodeOrientation orientation) {
        var stage = new Stage();

        var headerBar = new HeaderBar();
        headerBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));

        var resizable = new CheckBox("Resizable");
        resizable.selectedProperty().bindBidirectional(stage.resizableProperty());

        var fullScreen = new Button("Full-screen");
        fullScreen.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));

        var content = new HBox(resizable, fullScreen);
        content.setSpacing(10);
        content.setAlignment(Pos.CENTER);
        headerBar.setCenter(content);
        HeaderBar.setAlignment(content, Pos.CENTER);

        var root = new BorderPane();
        root.setTop(headerBar);

        var scene = new Scene(root);
        scene.setNodeOrientation(orientation);

        stage.initStyle(style);
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(500);
        stage.show();
    }

    public static void showSplitHeaderBar(StageStyle style, NodeOrientation orientation) {
        var stage = new Stage();

        var leftHeaderBar = new HeaderBar();
        leftHeaderBar.setBackground(Background.fill(Color.VIOLET));
        leftHeaderBar.setTrailingSystemPadding(false);

        var rightHeaderBar = new HeaderBar();
        rightHeaderBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        rightHeaderBar.setLeadingSystemPadding(false);

        var resizable = new CheckBox("Resizable");
        resizable.selectedProperty().bindBidirectional(stage.resizableProperty());

        var fullScreen = new Button("Full-screen");
        fullScreen.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));

        var content = new HBox(resizable, fullScreen);
        content.setSpacing(10);
        content.setAlignment(Pos.CENTER);
        rightHeaderBar.setCenter(content);
        HeaderBar.setAlignment(content, Pos.CENTER);

        var left = new BorderPane();
        left.setTop(leftHeaderBar);

        var right = new BorderPane();
        right.setTop(rightHeaderBar);

        var root = new SplitPane(left, right);

        var scene = new Scene(root);
        scene.setNodeOrientation(orientation);

        stage.initStyle(style);
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(500);
        stage.show();
    }

    public static void showCustomHeaderButtons(StageStyle style, NodeOrientation orientation) {
        var stage = new Stage();

        var headerBar = new HeaderBar();
        headerBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        headerBar.setMinHeight(40);

        var resizable = new CheckBox("Resizable");
        resizable.selectedProperty().bindBidirectional(stage.resizableProperty());

        var fullScreen = new Button("Full-screen");
        fullScreen.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));

        var minimize = new Button("Minimize");
        minimize.setOnAction(e -> stage.setIconified(true));
        HeaderBar.setHeaderButtonType(minimize, HeaderButtonType.MINIMIZE);

        var maximize = new Button("Maximize/restore");
        maximize.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        HeaderBar.setHeaderButtonType(maximize, HeaderButtonType.MAXIMIZE);

        var close = new Button("Close");
        close.setOnAction(e -> stage.close());
        HeaderBar.setHeaderButtonType(close, HeaderButtonType.CLOSE);

        var headerButtons = new HBox(minimize, maximize, close);
        headerButtons.setAlignment(Pos.CENTER);
        headerButtons.setSpacing(5);
        headerBar.setTrailing(headerButtons);

        var content = new HBox(resizable, fullScreen);
        content.setSpacing(10);
        content.setAlignment(Pos.CENTER);
        headerBar.setCenter(content);
        HeaderBar.setAlignment(content, Pos.CENTER);

        var root = new BorderPane();
        root.setTop(headerBar);

        var scene = new Scene(root);
        scene.setNodeOrientation(orientation);

        stage.initStyle(style);
        stage.initDefaultHeaderButtons(false);
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(500);
        stage.show();
    }
}
