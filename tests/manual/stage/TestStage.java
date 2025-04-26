/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TestStage extends Application {
    private Stage testStage = new Stage();
    private Label lblWidth = new Label();
    private Label lblHeight = new Label();
    private Label lblMinWidth = new Label();
    private Label lblMinHeight = new Label();
    private Label lblMaxWidth = new Label();
    private Label lblMaxHeight = new Label();
    private Label lblX = new Label();
    private Label lblY = new Label();
    private Label lblSceneWidth = new Label();
    private Label lblSceneHeight = new Label();
    private Label lblSceneX = new Label();
    private Label lblSceneY = new Label();
    private ComboBox<StageStyle> cbStageStyle = new ComboBox<>(FXCollections.observableArrayList(StageStyle.values()));
    private CheckBox cbIsFullScreen = new CheckBox("Is FullScreen");
    private CheckBox cbIsMaximized = new CheckBox("Is Maximized");
    private CheckBox cbIsIconified = new CheckBox("Is Iconified");
    private CheckBox cbIsResizable = new CheckBox("Is Resizable");

    @Override
    public void start(Stage stage) {
        cbStageStyle.valueProperty().addListener((observable, oldValue, newValue) -> {
            testStage.initStyle(StageStyle.valueOf(newValue.name()));
        });

        cbStageStyle.getSelectionModel().select(StageStyle.DECORATED);

        Button btnMaxminize = new Button("Toggle Maximize");
        btnMaxminize.setOnAction(e -> testStage.setMaximized(!testStage.isMaximized()));

        Button btnFullScreen = new Button("Toggle FullScreen");
        btnFullScreen.setOnAction(e -> testStage.setFullScreen(!testStage.isFullScreen()));

        Button btnIconify = new Button("Toggle Iconified");
        btnIconify.setOnAction(e -> testStage.setIconified(!testStage.isIconified()));

        Button btnResizable = new Button("Toggle Resizable");
        btnResizable.setOnAction(e -> testStage.setResizable(!testStage.isResizable()));

        Button btnShow = new Button("Show");
        btnShow.setOnAction(e -> {
            testStage.show();
        });

        Button btnClose = new Button("Close");
        btnClose.setOnAction(e -> {
            testStage.close();
            createTestStage();
        });

        Button btnSizeToScene = new Button("Size to scene");
        btnSizeToScene.setOnAction(e -> {
            testStage.sizeToScene();
        });

        Button btnCenterOnScreen = new Button("Center on screen");
        btnCenterOnScreen.setOnAction(e -> {
            testStage.centerOnScreen();
        });

        Button btnResize = new Button("Resize");
        btnResize.setOnAction(e -> {
            testStage.setWidth(300);
            testStage.setHeight(300);
        });

        Button btnMaxSize = new Button("Set Max Size");
        btnMaxSize.setOnAction(e -> {
            testStage.setMaxWidth(250);
            testStage.setMaxHeight(250);
        });

        Button btnUnsetMaxSize = new Button("Unset Max Size");
        btnUnsetMaxSize.setOnAction(e -> {
            testStage.setMaxWidth(Double.MAX_VALUE);
            testStage.setMaxHeight(Double.MAX_VALUE);
        });

        Button btnMove = new Button("Move");
        btnMove.setOnAction(e -> {
            testStage.setX(100);
            testStage.setY(100);
        });

        cbIsMaximized.setDisable(true);
        cbIsFullScreen.setDisable(true);
        cbIsIconified.setDisable(true);
        cbIsResizable.setDisable(true);

        FlowPane commandPane = new FlowPane(cbStageStyle, btnShow, btnClose, btnSizeToScene, btnCenterOnScreen,
                btnResize, btnMaxSize, btnUnsetMaxSize, btnMove, btnIconify, btnMaxminize, btnFullScreen, btnResizable);
        commandPane.setHgap(5);
        commandPane.setVgap(5);


        VBox root = new VBox(commandPane,
                new Separator(Orientation.HORIZONTAL),
                new Label("Stage Properties:"),
                cbIsIconified, cbIsMaximized,
                cbIsFullScreen, cbIsResizable,
                lblMinWidth, lblMinHeight, lblMaxWidth, lblMaxHeight,
                lblWidth, lblHeight, lblX, lblY,
                new Separator(Orientation.HORIZONTAL),
                new Label("Scene Properties:"),
                lblSceneWidth, lblSceneHeight, lblSceneX, lblSceneY);
        root.setSpacing(5);
        root.setFillWidth(true);

        createTestStage();

        Scene scene = new Scene(root, 500, 600);
        stage.setTitle("Command Stage");
        stage.setScene(scene);
        stage.show();
    }

    private void createTestStage() {
        testStage = new Stage();

        StackPane stackPane = new StackPane();
        stackPane.setBackground(Background.fill(Color.TRANSPARENT));
        testStage = new Stage();
        Scene testScene = new Scene(stackPane, 300, 300, Color.HOTPINK);
        testStage.setScene(testScene);
        testStage.initStyle(cbStageStyle.getValue());
        testStage.setTitle("Test Stage");
        testStage.setWidth(800);
        testStage.setHeight(600);

        cbIsMaximized.selectedProperty().bind(testStage.maximizedProperty());
        cbIsFullScreen.selectedProperty().bind(testStage.fullScreenProperty());
        cbIsIconified.selectedProperty().bind(testStage.iconifiedProperty());
        cbIsResizable.selectedProperty().bind(testStage.resizableProperty());
        lblWidth.textProperty().bind(Bindings.format("Width: %.2f", testStage.widthProperty()));
        lblHeight.textProperty().bind(Bindings.format("Height: %.2f", testStage.heightProperty()));
        lblMinWidth.textProperty().bind(Bindings.format("Min Width: %.2f", testStage.minWidthProperty()));
        lblMinHeight.textProperty().bind(Bindings.format("Min Height: %.2f", testStage.minHeightProperty()));
        lblMaxWidth.textProperty().bind(Bindings.format("Max Width: %.2f", testStage.maxWidthProperty()));
        lblMaxHeight.textProperty().bind(Bindings.format("Max Height: %.2f", testStage.maxHeightProperty()));
        lblX.textProperty().bind(Bindings.format("X: %.2f", testStage.xProperty()));
        lblY.textProperty().bind(Bindings.format("Y: %.2f", testStage.yProperty()));
        lblSceneWidth.textProperty().bind(Bindings.format("Width: %.2f", testScene.widthProperty()));
        lblSceneHeight.textProperty().bind(Bindings.format("Height: %.2f", testScene.heightProperty()));
        lblSceneX.textProperty().bind(Bindings.format("X: %.2f", testScene.xProperty()));
        lblSceneY.textProperty().bind(Bindings.format("Y: %.2f", testScene.yProperty()));
    }

    public static void main(String[] args) {
        launch(TestStage.class, args);
    }
}
