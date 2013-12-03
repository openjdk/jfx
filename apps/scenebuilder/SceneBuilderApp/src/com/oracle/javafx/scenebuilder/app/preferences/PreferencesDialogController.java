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
import com.oracle.javafx.scenebuilder.app.preferences.SBPreferences.BackgroundStyleClass;
import com.oracle.javafx.scenebuilder.app.preferences.SBPreferences.CSSAnalyzerColumnsOrder;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import java.util.Arrays;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * Preferences dialog.
 *
 */
public class PreferencesDialogController extends AbstractModalDialog {

    @FXML
    private TextField documentHeight;
    @FXML
    private TextField documentWidth;
    @FXML
    private ChoiceBox<DisplayOption> hierarchyDisplayOption;
    @FXML
    private ChoiceBox<CSSAnalyzerColumnsOrder> cssAnalyzerColumnsOrder;
    @FXML
    private ChoiceBox<BackgroundStyleClass> backgroundStyleClass;
    @FXML
    private VBox alignmentGuides;
    @FXML
    private VBox dropTargetRing;

    public PreferencesDialogController() {
        super(PreferencesDialogController.class.getResource("Preferences.fxml"), //NOI18N
                I18N.getBundle(), null);
    }

    public void onWidthChange() {
        // TODO fix DTL-5656 : SB APP: Preferences dialog
        // check value type when TextField will provide an appropriate callback
    }

    public void onHeightChange() {
        // TODO fix DTL-5656 : SB APP: Preferences dialog
        // check value type when TextField will provide an appropriate callback
    }

    /*
     * AbstractModalDialog
     */
    
    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();
        
        // The AbstractModalDialog.controllerDidLoadFxml initialize the 
        // ACTION, CANCEL and OK buttons as follows :
        // By default, the ACTION button is not visible.
        // => update the buttons setup if needed.
        setActionButtonVisible(true);
        setActionButtonTitle(I18N.getString("prefs.revert"));
        setDefaultButtonID(ButtonID.OK);
        setShowDefaultButton(true);
    }
    
    @Override
    protected void controllerDidLoadContentFxml() {
        // Background style class
        // TODO fix DTL-5656 : SB APP: Preferences dialog
        // update the underlying preference
        backgroundStyleClass.getItems().setAll(Arrays.asList(BackgroundStyleClass.class.getEnumConstants()));
        backgroundStyleClass.getSelectionModel().select(BackgroundStyleClass.BACKGROUND_01); // get value stored in preferences
        backgroundStyleClass.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<BackgroundStyleClass>() {

            @Override
            public void changed(ObservableValue<? extends BackgroundStyleClass> observable,
                    BackgroundStyleClass oldValue,
                    BackgroundStyleClass newValue) {
                backgroundStyleClassDidChange();
            }
        });

        // Background style class
        // TODO fix DTL-5656 : SB APP: Preferences dialog
        // update the underlying preference
        cssAnalyzerColumnsOrder.getItems().setAll(Arrays.asList(CSSAnalyzerColumnsOrder.class.getEnumConstants()));
        cssAnalyzerColumnsOrder.getSelectionModel().select(CSSAnalyzerColumnsOrder.DEFAULTS_FIRST); // get value stored in preferences
        cssAnalyzerColumnsOrder.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<CSSAnalyzerColumnsOrder>() {

            @Override
            public void changed(ObservableValue<? extends CSSAnalyzerColumnsOrder> observable,
                    CSSAnalyzerColumnsOrder oldValue,
                    CSSAnalyzerColumnsOrder newValue) {
                cssAnalyzerColumnsOrderDidChange();
            }
        });

        // Hierarchy display option
        // TODO fix DTL-5656 : SB APP: Preferences dialog
        // update the underlying preference
        hierarchyDisplayOption.getItems().setAll(Arrays.asList(DisplayOption.class.getEnumConstants()));
        hierarchyDisplayOption.getSelectionModel().select(DisplayOption.INFO); // get value stored in preferences
        hierarchyDisplayOption.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DisplayOption>() {

            @Override
            public void changed(ObservableValue<? extends DisplayOption> observable,
                    DisplayOption oldValue,
                    DisplayOption newValue) {
                hierarchyDisplayOptionDidChange();
            }
        });
    }

    @Override
    protected void okButtonPressed(ActionEvent e) {
        getStage().close();
    }

    @Override
    protected void cancelButtonPressed(ActionEvent e) {
        getStage().close();
    }

    @Override
    protected void actionButtonPressed(ActionEvent e) {
        getStage().close();
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
        getStage().initModality(Modality.APPLICATION_MODAL);
    }
    
    
    /*
     * Private
     */
    
    private void backgroundStyleClassDidChange() {
        // To be implemented
    }
    
    private void cssAnalyzerColumnsOrderDidChange() {
        // To be implemented
    }
    
    private void hierarchyDisplayOptionDidChange() {
        // To be implemented
    }
}
