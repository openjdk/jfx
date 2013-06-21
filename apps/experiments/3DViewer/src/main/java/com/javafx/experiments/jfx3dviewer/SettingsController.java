/*
 * Copyright (c) 2010, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.jfx3dviewer;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import com.javafx.experiments.shape3d.SubDivision;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.util.Callback;

/**
 * Controller class for settings panel
 */
public class SettingsController implements Initializable {
    private final ContentModel contentModel = Jfx3dViewerApp.getContentModel();

    public Accordion settings;
    public ColorPicker ambientColorPicker;
    public CheckBox showAxisCheckBox;
    public CheckBox yUpCheckBox;
    public Slider fovSlider;
    public CheckBox scaleToFitCheckBox;
    public ColorPicker light1ColorPicker;
    public CheckBox ambientEnableCheckbox;
    public CheckBox light1EnabledCheckBox;
    public CheckBox light1followCameraCheckBox;
    public ColorPicker backgroundColorPicker;
    public Slider light1x;
    public Slider light1y;
    public Slider light1z;
    public CheckBox light2EnabledCheckBox;
    public ColorPicker light2ColorPicker;
    public Slider light2x;
    public Slider light2y;
    public Slider light2z;
    public CheckBox light3EnabledCheckBox;
    public ColorPicker light3ColorPicker;
    public Slider light3x;
    public Slider light3y;
    public Slider light3z;
    public CheckBox wireFrameCheckbox;
    public ToggleGroup subdivisionLevelGroup;
    public ToggleGroup subdivisionBoundaryGroup;
    public ToggleGroup subdivisionSmoothGroup;
    public TreeTableView<Node> hierarachyTreeTable;
    public TreeTableColumn<Node, String> nodeColumn;
    public TreeTableColumn<Node, String> idColumn;
    public TreeTableColumn<Node, Boolean> visibilityColumn;
    
    @Override public void initialize(URL location, ResourceBundle resources) {
        // keep one pane open always
        settings.expandedPaneProperty().addListener(new ChangeListener<TitledPane>() {
            @Override public void changed(ObservableValue<? extends TitledPane> observable, TitledPane oldValue, TitledPane newValue) {
                Platform.runLater(
                        new Runnable() {
                            @Override public void run() {
                                if (settings.getExpandedPane() == null)
                                    settings.setExpandedPane(settings.getPanes().get(0));
                            }
                        });
            }
        });
        // wire up settings in OPTIONS
        contentModel.getAutoScalingGroup().enabledProperty().bind(scaleToFitCheckBox.selectedProperty());
        contentModel.showAxisProperty().bind(showAxisCheckBox.selectedProperty());
        contentModel.yUpProperty().bind(yUpCheckBox.selectedProperty());
        backgroundColorPicker.setValue((Color)contentModel.getSubScene().getFill());
        contentModel.getSubScene().fillProperty().bind(backgroundColorPicker.valueProperty());
        wireFrameCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isWireframe) {
                contentModel.setWireFrame(isWireframe);
            }
        });
        subdivisionLevelGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle selectedToggle) {
                if (selectedToggle == null && oldValue != null) {
                    subdivisionLevelGroup.selectToggle(oldValue);
                    selectedToggle = oldValue;
                } else {
                    contentModel.setSubdivisionLevel(Integer.parseInt((String)selectedToggle.getUserData()));
                }
            }
        });
        subdivisionBoundaryGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle selectedToggle) {
                if (selectedToggle == null && oldValue != null) {
                    subdivisionBoundaryGroup.selectToggle(oldValue);
                    selectedToggle = oldValue;
                } else {
                    contentModel.setBoundaryMode((SubDivision.BoundaryMode) selectedToggle.getUserData());
                }
            }
        });
        subdivisionSmoothGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle selectedToggle) {
                if (selectedToggle == null && oldValue != null) {
                    subdivisionSmoothGroup.selectToggle(oldValue);
                    selectedToggle = oldValue;
                } else {
                    contentModel.setMapBorderMode((SubDivision.MapBorderMode) selectedToggle.getUserData());
                }
            }
        });
        // wire up settings in LIGHTS
        ambientEnableCheckbox.setSelected(contentModel.getAmbientLightEnabled());
        contentModel.ambientLightEnabledProperty().bind(ambientEnableCheckbox.selectedProperty());
        ambientColorPicker.setValue(contentModel.getAmbientLight().getColor());
        contentModel.getAmbientLight().colorProperty().bind(ambientColorPicker.valueProperty());

        // LIGHT 1
        light1EnabledCheckBox.setSelected(contentModel.getLight1Enabled());
        contentModel.light1EnabledProperty().bind(light1EnabledCheckBox.selectedProperty());
        light1ColorPicker.setValue(contentModel.getLight1().getColor());
        contentModel.getLight1().colorProperty().bind(light1ColorPicker.valueProperty());
        light1x.disableProperty().bind(light1followCameraCheckBox.selectedProperty());
        light1y.disableProperty().bind(light1followCameraCheckBox.selectedProperty());
        light1z.disableProperty().bind(light1followCameraCheckBox.selectedProperty());
        light1followCameraCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    contentModel.getLight1().translateXProperty().bind(new DoubleBinding() {
                        { bind(contentModel.getCamera().boundsInParentProperty()); }
                        @Override protected double computeValue() {
                            return contentModel.getCamera().getBoundsInParent().getMinX();
                        }
                    });
                    contentModel.getLight1().translateYProperty().bind(new DoubleBinding() {
                        { bind(contentModel.getCamera().boundsInParentProperty()); }
                        @Override protected double computeValue() {
                            return contentModel.getCamera().getBoundsInParent().getMinY();
                        }
                    });
                    contentModel.getLight1().translateZProperty().bind(new DoubleBinding() {
                        { bind(contentModel.getCamera().boundsInParentProperty()); }
                        @Override protected double computeValue() {
                            return contentModel.getCamera().getBoundsInParent().getMinZ();
                        }
                    });
                } else {
                    contentModel.getLight1().translateXProperty().bind(light1x.valueProperty());
                    contentModel.getLight1().translateYProperty().bind(light1y.valueProperty());
                    contentModel.getLight1().translateZProperty().bind(light1z.valueProperty());
                }
            }
        });
        // LIGHT 2
        light2EnabledCheckBox.setSelected(contentModel.getLight2Enabled());
        contentModel.light2EnabledProperty().bind(light2EnabledCheckBox.selectedProperty());
        light2ColorPicker.setValue(contentModel.getLight2().getColor());
        contentModel.getLight2().colorProperty().bind(light2ColorPicker.valueProperty());
        contentModel.getLight2().translateXProperty().bind(light2x.valueProperty());
        contentModel.getLight2().translateYProperty().bind(light2y.valueProperty());
        contentModel.getLight2().translateZProperty().bind(light2z.valueProperty());
        // LIGHT 3
        light3EnabledCheckBox.setSelected(contentModel.getLight3Enabled());
        contentModel.light3EnabledProperty().bind(light3EnabledCheckBox.selectedProperty());
        light3ColorPicker.setValue(contentModel.getLight3().getColor());
        contentModel.getLight3().colorProperty().bind(light3ColorPicker.valueProperty());
        contentModel.getLight3().translateXProperty().bind(light3x.valueProperty());
        contentModel.getLight3().translateYProperty().bind(light3y.valueProperty());
        contentModel.getLight3().translateZProperty().bind(light3z.valueProperty());
        // wire up settings in CAMERA
        fovSlider.setValue(contentModel.getCamera().getFieldOfView());
        contentModel.getCamera().fieldOfViewProperty().bind(fovSlider.valueProperty());

        hierarachyTreeTable.rootProperty().bind(new ObjectBinding<TreeItem<Node>>() {

            {
                bind(contentModel.contentProperty());
            }

            @Override
            protected TreeItem<Node> computeValue() {
                Node content3D = contentModel.getContent();
                if (content3D != null) {
                    return new TreeItemImpl(content3D);
                } else {
                    return null;
                }
            }
        });
        nodeColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Node, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Node, String> p) {
                return p.getValue().valueProperty().asString();
            }
        });
        idColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Node, String>, ObservableValue<String>>() {

            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Node, String> p) {
                return p.getValue().getValue().idProperty();
            }
        });
        visibilityColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Node, Boolean>, ObservableValue<Boolean>>() {

            @Override
            public ObservableValue<Boolean> call(TreeTableColumn.CellDataFeatures<Node, Boolean> p) {
                return p.getValue().getValue().visibleProperty();
            }
        });
        visibilityColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(visibilityColumn));

        SessionManager sessionManager = SessionManager.getSessionManager();

        sessionManager.bind(showAxisCheckBox.selectedProperty(), "showAxis");
        sessionManager.bind(yUpCheckBox.selectedProperty(), "yUp");
        sessionManager.bind(scaleToFitCheckBox.selectedProperty(), "scaleToFit");
        sessionManager.bind(wireFrameCheckbox.selectedProperty(), "wireFrame");
        sessionManager.bind(backgroundColorPicker.valueProperty(), "backgroundColor");
        sessionManager.bind(fovSlider.valueProperty(), "fieldOfView");
        sessionManager.bind(subdivisionLevelGroup, "subdivisionLevel");
        sessionManager.bind(subdivisionBoundaryGroup, "subdivisionBoundary");
        sessionManager.bind(subdivisionSmoothGroup, "subdivisionSmooth");
        sessionManager.bind(light1ColorPicker.valueProperty(), "light1Color");
        sessionManager.bind(light1EnabledCheckBox.selectedProperty(), "light1Enabled");
        sessionManager.bind(light1followCameraCheckBox.selectedProperty(), "light1FollowCamera");
        sessionManager.bind(light1x.valueProperty(), "light1X");
        sessionManager.bind(light1y.valueProperty(), "light1Y");
        sessionManager.bind(light1z.valueProperty(), "light1Z");
        sessionManager.bind(light2ColorPicker.valueProperty(), "light2Color");
        sessionManager.bind(light2EnabledCheckBox.selectedProperty(), "light2Enabled");
        sessionManager.bind(light2x.valueProperty(), "light2X");
        sessionManager.bind(light2y.valueProperty(), "light2Y");
        sessionManager.bind(light2z.valueProperty(), "light2Z");
        sessionManager.bind(light3ColorPicker.valueProperty(), "light3Color");
        sessionManager.bind(light3EnabledCheckBox.selectedProperty(), "light3Enabled");
        sessionManager.bind(light3x.valueProperty(), "light3X");
        sessionManager.bind(light3y.valueProperty(), "light3Y");
        sessionManager.bind(light3z.valueProperty(), "light3Z");
        sessionManager.bind(ambientColorPicker.valueProperty(), "ambient");
        sessionManager.bind(ambientEnableCheckbox.selectedProperty(), "ambientEnable");
        sessionManager.bind(settings, "settingsPane");
    }

    private class TreeItemImpl extends TreeItem<Node> {

        public TreeItemImpl(Node node) {
            super(node);
            if (node instanceof Parent) {
                for (Node n : ((Parent) node).getChildrenUnmodifiable()) {
                    getChildren().add(new TreeItemImpl(n));
                }
            }
        }
    }
}
