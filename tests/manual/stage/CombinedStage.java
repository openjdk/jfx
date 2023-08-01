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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CombinedStage extends Application {
    public static void main(String[] args) {
        launch(CombinedStage.class, args);
    }

    private class ClickThroughRegion extends Region {
        @Override public boolean contains(Point2D p) {
            return false;
        }
    }

    private class ClickThroughHBox extends HBox {
        public ClickThroughHBox(Node... children) {
            super(children);
            setBackground(null);
        }
        @Override public boolean contains(Point2D p) {
            return false;
        }
    }

    private class ClickThroughVBox extends VBox {
        public ClickThroughVBox(Node... children) {
            super(children);
            setBackground(null);
        }
        @Override public boolean contains(Point2D p) {
            return false;
        }
    }

    private class ThreeBox extends ClickThroughHBox {
        private final Region leftPadding = new ClickThroughRegion();
        private final Region rightPadding = new ClickThroughRegion();

        public ThreeBox(Region left, Region center, Region right) {
            HBox.setHgrow(leftPadding, Priority.ALWAYS);
            HBox.setHgrow(rightPadding, Priority.ALWAYS);
            getChildren().addAll(left, leftPadding, center, rightPadding, right);
        }
    }

    @Override
    public void start(Stage stage) {

        TextArea logArea = new TextArea();

        Region leftDecorationRegion = new ClickThroughRegion();
        Button leftButton = new Button("Left");
        leftButton.setOnAction(e -> {
            logArea.appendText("Left button pushed\n");
        });

        Region rightDecorationRegion = new ClickThroughRegion();
        Button rightButton = new Button("Right");
        rightButton.setOnAction(e -> {
            logArea.appendText("Right button pushed\n");
        });

        TextField address = new TextField();
        address.setOnAction(e -> {
            logArea.appendText("Typed: " + address.getText() + "\n");
            address.clear();
        });

        HBox leftContainer = new ClickThroughHBox(leftDecorationRegion, leftButton);
        HBox centerContainer = new ClickThroughHBox(address);
        HBox rightContainer = new ClickThroughHBox(rightButton, rightDecorationRegion);

        HBox toolbar = new ThreeBox(
            leftContainer,
            centerContainer,
            rightContainer
        );
        toolbar.setPadding(new Insets(5, 0, 5, 0));

        VBox vBox = new ClickThroughVBox(toolbar, logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        vBox.setAlignment(Pos.TOP_CENTER);

        Scene scene = new Scene(vBox, 640, 480, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initStyle(StageStyle.COMBINED);

        leftDecorationRegion.prefWidthProperty().bind(stage.leftTitleBarInsetProperty());
        rightDecorationRegion.prefWidthProperty().bind(stage.rightTitleBarInsetProperty());
        stage.titleBarHeightProperty().bind(toolbar.heightProperty());

        stage.show();
    }
}
