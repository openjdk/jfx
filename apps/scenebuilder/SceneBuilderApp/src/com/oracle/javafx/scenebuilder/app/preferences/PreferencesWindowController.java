/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.app.preferences;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.BackgroundImage;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.CSSAnalyzerColumnsOrder;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.recentItemsSizes;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import java.util.Arrays;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Preferences window controller.
 */
public class PreferencesWindowController extends AbstractFxmlWindowController {

    @FXML
    private TextField documentHeight;
    @FXML
    private TextField documentWidth;
    @FXML
    private ChoiceBox<BackgroundImage> backgroundImage;
    @FXML
    private ChoiceBox<DisplayOption> hierarchyDisplayOption;
    @FXML
    private ChoiceBox<CSSAnalyzerColumnsOrder> cssAnalyzerColumnsOrder;
    @FXML
    private VBox alignmentGuides;
    @FXML
    private VBox dropTargetRing;
    @FXML
    private ChoiceBox<Integer> recentItemsSize;

    public PreferencesWindowController() {
        super(PreferencesWindowController.class.getResource("Preferences.fxml"), //NOI18N
                I18N.getBundle());
    }

    /*
     * AbstractModalDialog
     */
    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();

        final PreferencesController preferencesController
                = PreferencesController.getSingleton();
        final PreferencesRecordGlobal recordGlobal
                = preferencesController.getRecordGlobal();

        // Document size
        documentHeight.setText(String.valueOf(recordGlobal.getDocumentHeight()));
        documentHeight.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                final String value = documentHeight.getText();
                recordGlobal.setDocumentHeight(Double.valueOf(value));
                documentHeight.selectAll();
            }
        });
        documentWidth.setText(String.valueOf(recordGlobal.getDocumentWidth()));
        documentWidth.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                final String value = documentWidth.getText();
                recordGlobal.setDocumentWidth(Double.valueOf(value));
                documentWidth.selectAll();
            }
        });

        // Background image
        backgroundImage.getItems().setAll(Arrays.asList(BackgroundImage.class.getEnumConstants()));
        backgroundImage.setValue(recordGlobal.getBackgroundImage());
        backgroundImage.getSelectionModel().selectedItemProperty().addListener(new BackgroundImageListener());

        // Hierarchy display option
        hierarchyDisplayOption.getItems().setAll(Arrays.asList(DisplayOption.class.getEnumConstants()));
        hierarchyDisplayOption.setValue(recordGlobal.getHierarchyDisplayOption());
        hierarchyDisplayOption.getSelectionModel().selectedItemProperty().addListener(new DisplayOptionListener());

        // CSS analyzer column order
        cssAnalyzerColumnsOrder.getItems().setAll(Arrays.asList(CSSAnalyzerColumnsOrder.class.getEnumConstants()));
        cssAnalyzerColumnsOrder.setValue(recordGlobal.getCSSAnalyzerColumnsOrder());
        cssAnalyzerColumnsOrder.getSelectionModel().selectedItemProperty().addListener(new ColumnOrderListener());

        // Number of open recent items
        recentItemsSize.getItems().setAll(recentItemsSizes);
        recentItemsSize.setValue(recordGlobal.getRecentItemsSize());
        recentItemsSize.getSelectionModel().selectedItemProperty().addListener(new RecentItemsSizeListener());
    }

    /*
     * AbstractWindowController
     */
    @Override
    protected void controllerDidCreateStage() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
        assert getRoot().getScene().getWindow() != null;

        getStage().setTitle(I18N.getString("prefs.title"));
        getStage().setResizable(false);
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
        super.closeWindow();
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class BackgroundImageListener implements ChangeListener<BackgroundImage> {

        @Override
        public void changed(ObservableValue<? extends BackgroundImage> observable,
                BackgroundImage oldValue, BackgroundImage newValue) {
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            recordGlobal.setBackgroundImage(newValue);
        }
    }

    private static class DisplayOptionListener implements ChangeListener<DisplayOption> {

        @Override
        public void changed(ObservableValue<? extends DisplayOption> observable,
                DisplayOption oldValue, DisplayOption newValue) {
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            recordGlobal.setHierarchyDisplayOption(newValue);
        }
    }

    private static class ColumnOrderListener implements ChangeListener<CSSAnalyzerColumnsOrder> {

        @Override
        public void changed(ObservableValue<? extends CSSAnalyzerColumnsOrder> observable,
                CSSAnalyzerColumnsOrder oldValue, CSSAnalyzerColumnsOrder newValue) {
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            recordGlobal.setCSSAnalyzerColumnsOrder(newValue);
        }
    }

    private static class RecentItemsSizeListener implements ChangeListener<Integer> {

        @Override
        public void changed(ObservableValue<? extends Integer> observable,
                Integer oldValue, Integer newValue) {
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            recordGlobal.setRecentItemsSize(newValue);
        }
    }
}
