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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;

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
    public ColorPicker cameraColorPicker;
    public CheckBox scaleToFitCheckBox;

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
        // wire up settings
        fovSlider.setValue(contentModel.getCamera().getFieldOfView());
        contentModel.getCamera().fieldOfViewProperty().bind(fovSlider.valueProperty());
        contentModel.getAutoScalingGroup().enabledProperty().bind(scaleToFitCheckBox.selectedProperty());
        contentModel.showAxisProperty().bind(showAxisCheckBox.selectedProperty());
        contentModel.yUpProperty().bind(yUpCheckBox.selectedProperty());
    }
}
