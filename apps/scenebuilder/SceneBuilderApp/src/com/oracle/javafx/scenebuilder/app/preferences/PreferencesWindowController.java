/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ALIGNMENT_GUIDES_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.BACKGROUND_IMAGE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.CSS_TABLE_COLUMNS_ORDERING_REVERSED;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ROOT_CONTAINER_HEIGHT;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ROOT_CONTAINER_WIDTH;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.HIERARCHY_DISPLAY_OPTION;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LIBRARY_DISPLAY_OPTION;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.PARENT_RING_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS_SIZE;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.BackgroundImage;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.CSSAnalyzerColumnsOrder;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal.recentItemsSizes;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.DoubleField;
import com.oracle.javafx.scenebuilder.kit.editor.panel.library.LibraryPanelController.DISPLAY_MODE;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode;
import java.net.URL;
import java.util.Arrays;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.WindowEvent;

/**
 * Preferences window controller.
 */
public class PreferencesWindowController extends AbstractFxmlWindowController {

    @FXML
    private DoubleField rootContainerHeight;
    @FXML
    private DoubleField rootContainerWidth;
    @FXML
    private ChoiceBox<BackgroundImage> backgroundImage;
    @FXML
    private ChoiceBox<DISPLAY_MODE> libraryDisplayOption;
    @FXML
    private ChoiceBox<DisplayOption> hierarchyDisplayOption;
    @FXML
    private ChoiceBox<CSSAnalyzerColumnsOrder> cssAnalyzerColumnsOrder;
    @FXML
    private MenuButton alignmentGuidesButton;
    @FXML
    private MenuButton parentRingButton;
    @FXML
    private CustomMenuItem alignmentGuidesMenuItem;
    @FXML
    private CustomMenuItem parentRingMenuItem;
    @FXML
    private Rectangle alignmentGuidesGraphic;
    @FXML
    private Rectangle parentRingGraphic;
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

        // CSS setup
        final URL themeURL = EditorController.class.getResource("css/Theme.css"); //NOI18N
        assert themeURL != null;
        getRoot().getStylesheets().add(0, themeURL.toString());

        // Root container size
        rootContainerHeight.setText(String.valueOf(recordGlobal.getRootContainerHeight()));
        rootContainerHeight.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                final String value = rootContainerHeight.getText();
                recordGlobal.setRootContainerHeight(Double.valueOf(value));
                rootContainerHeight.selectAll();
                // Update preferences
                recordGlobal.writeToJavaPreferences(ROOT_CONTAINER_HEIGHT);
                // Update UI
                recordGlobal.refreshRootContainerHeight();
            }
        });
        rootContainerWidth.setText(String.valueOf(recordGlobal.getRootContainerWidth()));
        rootContainerWidth.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                final String value = rootContainerWidth.getText();
                recordGlobal.setRootContainerWidth(Double.valueOf(value));
                rootContainerWidth.selectAll();
                // Update preferences
                recordGlobal.writeToJavaPreferences(ROOT_CONTAINER_WIDTH);
                // Update UI
                recordGlobal.refreshRootContainerWidth();
            }
        });

        // Background image
        backgroundImage.getItems().setAll(Arrays.asList(BackgroundImage.class.getEnumConstants()));
        backgroundImage.setValue(recordGlobal.getBackgroundImage());
        backgroundImage.getSelectionModel().selectedItemProperty().addListener(new BackgroundImageListener());

        final PaintPicker.Delegate delegate = new PaintPicker.Delegate() {
            @Override
            public void handleError(String warningKey, Object... arguments) {
            }
        };

        // Alignment guides color
        final Color alignmentColor = recordGlobal.getAlignmentGuidesColor();
        final PaintPicker alignmentColorPicker = new PaintPicker(delegate, Mode.COLOR);
        alignmentGuidesGraphic.setFill(alignmentColor);
        alignmentGuidesMenuItem.setContent(alignmentColorPicker);
        alignmentColorPicker.setPaintProperty(alignmentColor);
        alignmentColorPicker.paintProperty().addListener(
                new AlignmentGuidesColorListener(alignmentGuidesGraphic));

        // Parent ring color
        final Color parentRingColor = recordGlobal.getParentRingColor();
        final PaintPicker parentRingColorPicker = new PaintPicker(delegate, Mode.COLOR);
        parentRingGraphic.setFill(parentRingColor);
        parentRingMenuItem.setContent(parentRingColorPicker);
        parentRingColorPicker.setPaintProperty(parentRingColor);
        parentRingColorPicker.paintProperty().addListener(
                new ParentRingColorListener(parentRingGraphic));

        // Library view option
        final DISPLAY_MODE availableDisplayMode[] = new DISPLAY_MODE[]{
            DISPLAY_MODE.LIST, DISPLAY_MODE.SECTIONS};
        libraryDisplayOption.getItems().setAll(Arrays.asList(availableDisplayMode));
        libraryDisplayOption.setValue(recordGlobal.getLibraryDisplayOption());
        libraryDisplayOption.getSelectionModel().selectedItemProperty().addListener(new LibraryOptionListener());

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
            // Update preferences
            recordGlobal.setBackgroundImage(newValue);
            recordGlobal.writeToJavaPreferences(BACKGROUND_IMAGE);
            // Update UI
            recordGlobal.refreshBackgroundImage();
        }
    }

    private static class LibraryOptionListener implements ChangeListener<DISPLAY_MODE> {

        @Override
        public void changed(ObservableValue<? extends DISPLAY_MODE> ov, DISPLAY_MODE oldValue, DISPLAY_MODE newValue) {
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            // Update preferences
            recordGlobal.setLibraryDisplayOption(newValue);
            recordGlobal.writeToJavaPreferences(LIBRARY_DISPLAY_OPTION);
            // Update UI
            recordGlobal.refreshLibraryDisplayOption();
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
            // Update preferences
            recordGlobal.setHierarchyDisplayOption(newValue);
            recordGlobal.writeToJavaPreferences(HIERARCHY_DISPLAY_OPTION);
            // Update UI
            recordGlobal.refreshHierarchyDisplayOption();
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
            // Update preferences
            recordGlobal.setCSSAnalyzerColumnsOrder(newValue);
            recordGlobal.writeToJavaPreferences(CSS_TABLE_COLUMNS_ORDERING_REVERSED);
            // Update UI
            recordGlobal.refreshCSSAnalyzerColumnsOrder();
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
            // Update preferences
            recordGlobal.setRecentItemsSize(newValue);
            recordGlobal.writeToJavaPreferences(RECENT_ITEMS_SIZE);
            recordGlobal.writeToJavaPreferences(RECENT_ITEMS);
        }
    }

    private static class AlignmentGuidesColorListener implements ChangeListener<Paint> {

        private final Rectangle graphic;

        public AlignmentGuidesColorListener(Rectangle graphic) {
            this.graphic = graphic;
        }

        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            assert newValue instanceof Color;
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            // Update preferences
            recordGlobal.setAlignmentGuidesColor((Color) newValue);
            recordGlobal.writeToJavaPreferences(ALIGNMENT_GUIDES_COLOR);
            // Update UI
            recordGlobal.refreshAlignmentGuidesColor();
            graphic.setFill(newValue);
        }
    }

    private static class ParentRingColorListener implements ChangeListener<Paint> {

        private final Rectangle graphic;

        public ParentRingColorListener(Rectangle graphic) {
            this.graphic = graphic;
        }

        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            assert newValue instanceof Color;
            final PreferencesController preferencesController
                    = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal
                    = preferencesController.getRecordGlobal();
            // Update preferences
            recordGlobal.setParentRingColor((Color) newValue);
            recordGlobal.writeToJavaPreferences(PARENT_RING_COLOR);
            // Update UI
            recordGlobal.refreshParentRingColor();
            graphic.setFill(newValue);
        }
    }
}
