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

package com.oracle.tools.fx.monkey.tools;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class StageTesterWindow extends Stage {

    public StageTesterWindow(Stage owner) {
        var pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);

        pane.add(new Label("Title"), 0, 0);
        var titleTextField = new TextField("My Stage");
        pane.add(titleTextField, 1, 0);

        pane.add(new Label("Modality"), 0, 1);
        var modalities = Arrays.stream(Modality.values()).map(Enum::name).toList();
        var modalityComboBox = new ComboBox<>(FXCollections.observableArrayList(modalities));
        modalityComboBox.getSelectionModel().select(0);
        pane.add(modalityComboBox, 1, 1);

        pane.add(new Label("StageStyle"), 0, 2);
        var stageStyles = Arrays.stream(StageStyle.values()).map(Enum::name).toList();
        var stageStyleComboBox = new ComboBox<>(FXCollections.observableArrayList(stageStyles));
        stageStyleComboBox.getSelectionModel().select(0);
        pane.add(stageStyleComboBox, 1, 2);

        pane.add(new Label("NodeOrientation"), 0, 3);
        var nodeOrientations = Arrays.stream(NodeOrientation.values()).map(Enum::name).toList();
        var nodeOrientationComboBox = new ComboBox<>(FXCollections.observableArrayList(nodeOrientations));
        nodeOrientationComboBox.getSelectionModel().select(2);
        pane.add(nodeOrientationComboBox, 1, 3);

        pane.add(new Label("HeaderBar"), 0, 4);
        var headerBarComboBox = new ComboBox<>(FXCollections.observableArrayList(
            "None", "Simple", "Simple / custom buttons", "Split", "Split / custom buttons"));
        headerBarComboBox.getSelectionModel().select(0);
        pane.add(headerBarComboBox, 1, 4);

        pane.add(new Label("AlwaysOnTop"), 0, 5);
        var alwaysOnTopCheckBox = new CheckBox();
        pane.add(alwaysOnTopCheckBox, 1, 5);

        pane.add(new Label("Resizable"), 0, 6);
        var resizableCheckBox = new CheckBox();
        resizableCheckBox.setSelected(true);
        pane.add(resizableCheckBox, 1, 6);

        pane.add(new Label("Iconified"), 0, 7);
        var iconifiedCheckBox = new CheckBox();
        pane.add(iconifiedCheckBox, 1, 7);

        pane.add(new Label("Maximized"), 0, 8);
        var maximizedCheckBox = new CheckBox();
        pane.add(maximizedCheckBox, 1, 8);

        pane.add(new Label("FullScreen"), 0, 9);
        var fullScreenCheckBox = new CheckBox();
        pane.add(fullScreenCheckBox, 1, 9);

        pane.add(new Label("FullScreenExitHint"), 0, 10);
        var fullScreenExitHintTextField = new TextField();
        pane.add(fullScreenExitHintTextField, 1, 10);

        var showStageButton = new Button("Show Stage");
        showStageButton.setOnAction(event -> {
            var newStage = new Stage();
            newStage.initStyle(StageStyle.valueOf(stageStyleComboBox.getValue()));
            newStage.initModality(Modality.valueOf(modalityComboBox.getValue()));
            newStage.setTitle(titleTextField.getText());
            newStage.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected());
            newStage.setResizable(resizableCheckBox.isSelected());
            newStage.setIconified(iconifiedCheckBox.isSelected());
            newStage.setMaximized(maximizedCheckBox.isSelected());
            newStage.setFullScreen(fullScreenCheckBox.isSelected());
            newStage.setFullScreenExitHint(fullScreenExitHintTextField.getText().isEmpty()
                                           ? null : fullScreenExitHintTextField.getText());

            if (newStage.getModality() != Modality.NONE) {
                newStage.initOwner(StageTesterWindow.this);
            }

            var nodeOrientation = NodeOrientation.valueOf(nodeOrientationComboBox.getValue());

            Parent root = switch (headerBarComboBox.getValue().toLowerCase(Locale.ROOT)) {
                case "simple" -> createSimpleHeaderBarRoot(newStage, nodeOrientation, false);
                case "simple / custom buttons" -> createSimpleHeaderBarRoot(newStage, nodeOrientation, true);
                case "split" -> createSplitHeaderBarRoot(newStage, nodeOrientation, false);
                case "split / custom buttons" -> createSplitHeaderBarRoot(newStage, nodeOrientation, true);
                default -> new BorderPane(createWindowActions(newStage, nodeOrientation));
            };

            var scene = new Scene(root);
            scene.setNodeOrientation(nodeOrientation);

            newStage.setWidth(800);
            newStage.setHeight(500);
            newStage.setScene(scene);
            newStage.show();
        });

        var root = new BorderPane(pane);
        root.setPadding(new Insets(20));
        root.setBottom(showStageButton);
        BorderPane.setAlignment(showStageButton, Pos.CENTER);
        BorderPane.setMargin(showStageButton, new Insets(30, 0, 0, 0));

        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        setScene(new Scene(root));
        setTitle("Stage Tester");
    }

    private Parent createSimpleHeaderBarRoot(Stage stage, NodeOrientation nodeOrientation, boolean customWindowButtons) {
        var headerBar = new HeaderBar();
        headerBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        headerBar.setCenter(new TextField() {{ setPromptText("Search..."); setMaxWidth(300); }});

        var sizeComboBox = new ComboBox<>(FXCollections.observableArrayList("Small", "Medium", "Large"));
        sizeComboBox.getSelectionModel().select(0);

        Runnable updateMinHeight = () -> headerBar.setMinHeight(
            switch (sizeComboBox.getValue().toLowerCase(Locale.ROOT)) {
                case "large" -> 80;
                case "medium" -> 50;
                default -> headerBar.getMinSystemHeight();
            });

        sizeComboBox.valueProperty().subscribe(event -> updateMinHeight.run());
        headerBar.minSystemHeightProperty().subscribe(event -> updateMinHeight.run());

        var menuBar = new MenuBar(
            new Menu("File", null,
                new MenuItem("New"),
                new MenuItem("Open"),
                new MenuItem("Save"),
                new MenuItem("Close")),
            new Menu("Edit", null,
                new MenuItem("Undo"),
                new MenuItem("Redo"),
                new MenuItem("Copy"),
                new MenuItem("Paste")));

        var leadingContent = new HBox(menuBar);
        HeaderBar.setDragType(leadingContent, HeaderDragType.DRAGGABLE);
        headerBar.setLeading(leadingContent);

        if (customWindowButtons) {
            HeaderBar.setPrefButtonHeight(stage, 0);
        } else {
            var adaptiveButtonHeight = new CheckBox("Adaptive button height");

            headerBar.heightProperty().subscribe(h -> {
                if (adaptiveButtonHeight.isSelected()) {
                    HeaderBar.setPrefButtonHeight(stage, h.doubleValue());
                }
            });

            adaptiveButtonHeight.selectedProperty().subscribe(value -> {
                if (value) {
                    HeaderBar.setPrefButtonHeight(stage, headerBar.getHeight());
                } else {
                    HeaderBar.setPrefButtonHeight(stage, HeaderBar.USE_DEFAULT_SIZE);
                }
            });

            HBox.setMargin(adaptiveButtonHeight, new Insets(4));
            leadingContent.getChildren().add(adaptiveButtonHeight);
        }

        var trailingNodes = new HBox(sizeComboBox);
        trailingNodes.setAlignment(Pos.CENTER);
        trailingNodes.setSpacing(5);
        headerBar.setTrailing(trailingNodes);

        if (customWindowButtons) {
            trailingNodes.getChildren().addAll(createCustomWindowButtons());
        }

        var borderPane = new BorderPane();
        borderPane.setTop(headerBar);
        borderPane.setCenter(createWindowActions(stage, nodeOrientation));

        return borderPane;
    }

    private Parent createSplitHeaderBarRoot(Stage stage, NodeOrientation nodeOrientation, boolean customWindowButtons) {
        var leftHeaderBar = new HeaderBar();
        leftHeaderBar.setBackground(Background.fill(Color.VIOLET));
        leftHeaderBar.setLeading(new Button("\u2728"));
        leftHeaderBar.setCenter(new TextField() {{ setPromptText("Search..."); setMaxWidth(200); }});
        leftHeaderBar.setTrailingSystemPadding(false);

        var rightHeaderBar = new HeaderBar();
        rightHeaderBar.setBackground(Background.fill(Color.LIGHTSKYBLUE));
        rightHeaderBar.setLeadingSystemPadding(false);

        var sizeComboBox = new ComboBox<>(FXCollections.observableArrayList("Small", "Medium", "Large"));
        sizeComboBox.getSelectionModel().select(0);

        Runnable updateMinHeight = () -> rightHeaderBar.setMinHeight(
            switch (sizeComboBox.getValue().toLowerCase(Locale.ROOT)) {
                case "large" -> 80;
                case "medium" -> 50;
                default -> rightHeaderBar.getMinSystemHeight();
            });

        sizeComboBox.valueProperty().subscribe(event -> updateMinHeight.run());
        rightHeaderBar.minSystemHeightProperty().subscribe(event -> updateMinHeight.run());

        var trailingNodes = new HBox(sizeComboBox);
        trailingNodes.setAlignment(Pos.CENTER);
        trailingNodes.setSpacing(5);
        rightHeaderBar.setTrailing(trailingNodes);

        if (customWindowButtons) {
            trailingNodes.getChildren().addAll(createCustomWindowButtons());
            HeaderBar.setPrefButtonHeight(stage, 0);
        }

        rightHeaderBar.setTrailing(trailingNodes);

        var left = new BorderPane();
        left.setTop(leftHeaderBar);
        left.setCenter(createWindowActions(stage, nodeOrientation));

        var right = new BorderPane();
        right.setTop(rightHeaderBar);

        return new SplitPane(left, right);
    }

    private List<Parent> createCustomWindowButtons() {
        var closeButton = new Button("Close");
        var iconifyButton = new Button("Iconify");
        var maximizeButton = new Button("Maximize");

        maximizeButton.getPseudoClassStates().subscribe(() -> {
            if (maximizeButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("maximized"))) {
                maximizeButton.setText("Restore");
            } else {
                maximizeButton.setText("Maximize");
            }
        });

        HeaderBar.setButtonType(iconifyButton, HeaderButtonType.ICONIFY);
        HeaderBar.setButtonType(maximizeButton, HeaderButtonType.MAXIMIZE);
        HeaderBar.setButtonType(closeButton, HeaderButtonType.CLOSE);
        return List.of(iconifyButton, maximizeButton, closeButton);
    }

    private Parent createWindowActions(Stage stage, NodeOrientation nodeOrientation) {
        var nodeOrientations = Arrays.stream(NodeOrientation.values()).map(Enum::name).toList();
        var nodeOrientationComboBox = new ComboBox<>(FXCollections.observableArrayList(nodeOrientations));
        nodeOrientationComboBox.getSelectionModel().select(nodeOrientation.ordinal());
        nodeOrientationComboBox.getSelectionModel().selectedItemProperty().addListener((_, _, orientation) -> {
            stage.getScene().setNodeOrientation(NodeOrientation.valueOf(orientation));
        });

        var toggleFullScreenButton = new Button("Enter/Exit Full Screen");
        toggleFullScreenButton.setOnAction(event -> stage.setFullScreen(!stage.isFullScreen()));

        var toggleMaximizedButton = new Button("Maximize/Restore");
        toggleMaximizedButton.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

        var toggleIconifiedButton = new Button("Iconify");
        toggleIconifiedButton.setOnAction(event -> stage.setIconified(true));

        var closeButton = new Button("Close");
        closeButton.setOnAction(event -> stage.close());

        var root = new VBox(nodeOrientationComboBox, toggleFullScreenButton, toggleMaximizedButton,
                            toggleIconifiedButton, closeButton);
        root.setSpacing(10);
        root.setAlignment(Pos.CENTER);
        return root;
    }
}
