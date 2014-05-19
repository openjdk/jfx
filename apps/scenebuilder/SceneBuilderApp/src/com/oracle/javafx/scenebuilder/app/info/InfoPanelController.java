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
package com.oracle.javafx.scenebuilder.app.info;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyFxControllerJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ToggleFxRootJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.ControllerClassEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.glossary.Glossary;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 
 */
public class InfoPanelController extends AbstractFxmlPanelController {

    @FXML private TableColumn<IndexEntry,String> leftTableColumn;
    @FXML private TableColumn<IndexEntry,FXOMObject> rightTableColumn;
    @FXML private Label bottomLabel;
    @FXML private VBox controllerClassVBox;
    @FXML CheckBox fxrootCheckBox;
    @FXML HBox controllerAndCogHBox;
    
    private IndexEntry.Type entryType = IndexEntry.Type.FX_ID;
    private ControllerClassEditor controllerClassEditor;
    private boolean controllerDidLoadFxmlOver = false;
    
    public InfoPanelController(EditorController editorController) {
        super(InfoPanelController.class.getResource("InfoPanel.fxml"), I18N.getBundle(), editorController); //NOI18N
    }

    public IndexEntry.Type getEntryType() {
        return entryType;
    }

    public void setEntryType(IndexEntry.Type entryType) {
        this.entryType = entryType;
        updateEntriesNow();
    }

    /*
     * AbstractPanelController
     */

    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        requestEntriesUpdate();
        updateAsPerRootNodeStatus();
        updateControllerAndControllerClassEditor();
        
        if (fxrootCheckBox != null) {
            fxrootCheckBox.selectedProperty().removeListener(checkBoxListener);
            fxrootCheckBox.setSelected(isFxRoot());
            fxrootCheckBox.selectedProperty().addListener(checkBoxListener);
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
        requestEntriesUpdate();
        updateAsPerRootNodeStatus();
    }

    @Override
    protected void cssRevisionDidChange() {
        // Nothing to do 
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        requestEntriesUpdate();
        updateAsPerRootNodeStatus();
        updateControllerClassEditor();
        fxrootCheckBox.selectedProperty().removeListener(checkBoxListener);
        fxrootCheckBox.setSelected(isFxRoot());
        fxrootCheckBox.selectedProperty().addListener(checkBoxListener);
    }
    
    @Override
    protected void editorSelectionDidChange() {
        final Selection selection = getEditorController().getSelection();
        
        final Set<IndexEntry> selectedEntries = new HashSet<>();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            selectedEntries.addAll(searchIndexEntries(osg.getItems()));
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
            selectedEntries.addAll(searchIndexEntries(Collections.singleton(gsg.getParentObject())));
        }
        
        final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
        stopListeningToTableViewSelection();
        tableView.getSelectionModel().clearSelection();
        for (IndexEntry e : selectedEntries) {
            tableView.getSelectionModel().select(e);
        }
        startListeningToTableViewSelection();
    }
    
    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {
        
        // Sanity checks
        assert leftTableColumn != null;
        assert rightTableColumn != null;
        assert leftTableColumn.getTableView() == rightTableColumn.getTableView();
        assert bottomLabel != null;
        assert controllerClassVBox != null;
        assert fxrootCheckBox != null;
        assert controllerAndCogHBox != null;

        performInitialization();
    }
    
    // This method is a step to a lazy initialization, to reduce startup time.
    // We didn't find a smart way to detect when the TitledPane containing
    // the InfoPanel is opened for the first time, except by putting a listener
    // on the expandedProperty: this approach means DocumentWindowController
    // has to call performInitialization (turned public), a path we do not want
    // to take.
    private void performInitialization() {
        if (controllerClassEditor != null) {
            controllerClassEditor.reset(getSuggestedControllerClasses(null));
        } else {
            controllerClassEditor = new ControllerClassEditor(getSuggestedControllerClasses(null));
        }

        HBox propNameNode = controllerClassEditor.getPropNameNode();

        // Make so that the property name appears left justified in the VBox
        propNameNode.setAlignment(Pos.CENTER_LEFT);
        controllerClassVBox.getChildren().add(0, propNameNode);

        // Initialize field with current value, if defined.
        controllerAndCogHBox.getChildren().add(controllerClassEditor.getValueEditor());
        controllerAndCogHBox.getChildren().add(controllerClassEditor.getMenu());

        // Need to react each time value of fx controller is changed (direct user input)
        controllerClassEditor.valueProperty().addListener(new ChangeListener<Object>() {

            @Override
            public void changed(ObservableValue<? extends Object> ov, Object t, Object t1) {
                InfoPanelController.this.updateControllerAndControllerClassEditor((String)t1);
            }
        });

        // We e.g. an Untitled document is saved we need to trigger a scan for
        // potential controller classes.
        getEditorController().fxmlLocationProperty().addListener(new ChangeListener<URL>() {

            @Override
            public void changed(ObservableValue<? extends URL> ov, URL t, URL t1) {
                if (t1 != null) {
                    resetSuggestedControllerClasses(t1);
                }
            }
        });
        
        // DTL-6626
        controllerClassEditor.getTextField().focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (!t1) {
                    // Focus loss triggers an update. The text field can be empty.
                    updateControllerAndControllerClassEditor(controllerClassEditor.getTextField().getText());
                }
            }
        });

        leftTableColumn.setCellValueFactory(new PropertyValueFactory<>("key")); //NOI18N
        rightTableColumn.setCellValueFactory(new PropertyValueFactory<>("fxomObject")); //NOI18N
        leftTableColumn.setCellFactory(new LeftCell.Factory());
        rightTableColumn.setCellFactory(new RightCell.Factory());

        fxrootCheckBox.setSelected(isFxRoot());
        fxrootCheckBox.selectedProperty().addListener(checkBoxListener);

        controllerDidLoadFxmlOver = true; // Must be called before updateAsPerRootNodeStatus()
        requestEntriesUpdate();
        updateAsPerRootNodeStatus();
        updateControllerAndControllerClassEditor();
    }
    

    /*
     * Private
     */
    private final static String IGNORED = "ignored"; //NOI18N
    
    private synchronized void updateControllerAndControllerClassEditor() {
        updateControllerAndControllerClassEditor(IGNORED);
    }
    
    private synchronized void updateControllerAndControllerClassEditor(String className) {
        if (getEditorController().getFxomDocument() != null) {
            FXOMObject root = getEditorController().getFxomDocument().getFxomRoot();
            if (root != null) {
                String zeClassName = computeProperClassName(className, root);

                final ModifyFxControllerJob job
                        = new ModifyFxControllerJob(root, zeClassName, getEditorController());

                if (job.isExecutable()) {
                    getEditorController().getJobManager().push(job);
                }
                
                updateControllerClassEditor(zeClassName);
            }
        }
    }
    
    private void updateControllerClassEditor() {
        updateControllerClassEditor(IGNORED);
    }
    
    private void updateControllerClassEditor(String className) {
        if (getEditorController().getFxomDocument() != null) {
            FXOMObject root = getEditorController().getFxomDocument().getFxomRoot();
            if (root != null) {
                String zeClassName = computeProperClassName(className, root);
                
                if (controllerClassEditor != null) {
                    controllerClassEditor.setUpdateFromModel(true);
                    controllerClassEditor.setValue(zeClassName);
                    controllerClassEditor.setUpdateFromModel(false);
                }
            }
        }
    }
    
    private String computeProperClassName(String className, FXOMObject root) {
        String res = className;
        
        if (className != null && className.equals(IGNORED)) {
            res = root.getFxController();
        }

        // When the user set an empty string we consider it means
        // no controller value needs to be set.
        if (className != null && className.isEmpty()) {
            res = null;
        }

        return res;
    }
    
    private void requestEntriesUpdate() {
        updateEntriesNow();
    }
    
    
    private void updateEntriesNow() {
        
        if (leftTableColumn != null) {
            final List<IndexEntry> newEntries = FXCollections.observableArrayList();

            final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
            if (fxomDocument != null) {
                switch(entryType) {
                    case FX_ID: {
                        final Map<String, FXOMObject> fxIds
                                = fxomDocument.collectFxIds();
                        for (Map.Entry<String, FXOMObject> e : fxIds.entrySet()) {
                            final String fxId = e.getKey();
                            final FXOMObject fxomObject = e.getValue();
                            newEntries.add(new IndexEntry(fxId, entryType, fxomObject));
                        }
                        break;
                    }

                    case HANDLER: {
                        break;
                    }

                    case RESOURCE_KEY: {
                        break;
                    }
                    
                    case STYLECLASS:
                        break;
                        
                    default:
                        break;
                }
            }


            // Update items in table view
            final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
            stopListeningToTableViewSelection();
            tableView.getItems().clear();
            tableView.getItems().addAll(newEntries);
            startListeningToTableViewSelection();
            
            // Update bottom label
            final int count = newEntries.size();
            final String labelText;
            switch(count) {
                case 0:
                    labelText = ""; //NOI18N
                    break;
                case 1:
                    labelText = "1 " //NOI18N
                            + I18N.getString("info.label.item");
                    break;
                default:
                    labelText = count + " " //NOI18N
                            + I18N.getString("info.label.items");
                    break;
            }
            bottomLabel.setText(labelText);
            
            // Setup selection again
            editorSelectionDidChange();
        }
    }
    
    
    private void startListeningToTableViewSelection() {
        assert leftTableColumn != null;
        final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
        tableView.getSelectionModel().getSelectedItems().addListener(tableViewSelectionListener);
    }
    
    private void stopListeningToTableViewSelection() {
        assert leftTableColumn != null;
        final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
        tableView.getSelectionModel().getSelectedItems().removeListener(tableViewSelectionListener);
    }
    
    private final ListChangeListener<IndexEntry> tableViewSelectionListener
        = new ListChangeListener<IndexEntry>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends IndexEntry> change) {
                tableSelectionDidChange();
            }
        };
    
    private void tableSelectionDidChange() {
        final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
        final List<IndexEntry> selectedItems =
                tableView.getSelectionModel().getSelectedItems();
        Set<FXOMObject> selectedFxomObjects = new HashSet<>();

        for (IndexEntry i : selectedItems) {
            selectedFxomObjects.add(i.getFxomObject());
        }

        stopListeningToEditorSelection();
        getEditorController().getSelection().select(selectedFxomObjects);
        startListeningToEditorSelection();
    }
    
    
    private Set<IndexEntry> searchIndexEntries(Set<FXOMObject> fxomObjects) {
        assert fxomObjects != null;
        
        final TableView<IndexEntry> tableView = leftTableColumn.getTableView();
        final Set<IndexEntry> result = new HashSet<>();
        for (IndexEntry e : tableView.getItems()) {
            if (fxomObjects.contains(e.getFxomObject())) {
                result.add(e);
            }
        }
        
        return result;
    }
    
    
    private List<String> getSuggestedControllerClasses(URL location) {
        Glossary glossary = getEditorController().getGlossary();
        
        if (location == null && getEditorController().getFxomDocument() != null) {
            location = getEditorController().getFxomDocument().getLocation();
        }
        
        return glossary.queryControllerClasses(location);
    }

    // When there is no defined root node we reset and disable the class field
    // and the fx:root check box.
    private void updateAsPerRootNodeStatus() {
        if (controllerDidLoadFxmlOver && getEditorController().getFxomDocument() != null) {

            if (getEditorController().getFxomDocument().getFxomRoot() == null) {
                fxrootCheckBox.setDisable(true);
                controllerClassEditor.setDisable(true);
                controllerClassEditor.setUpdateFromModel(true);
                controllerClassEditor.setValue(null); //NOI18N
                controllerClassEditor.setUpdateFromModel(false);
            } else {
                fxrootCheckBox.setDisable(false);
                String topClassName = getEditorController().getFxomDocument().getGlue().getRootElement().getTagName();
                fxrootCheckBox.setTooltip(new Tooltip(I18N.getString("info.tooltip.controller", topClassName)));
                controllerClassEditor.setDisable(false);
            }
        }
    }
    
    private void toggleFxRoot() {
        if (getEditorController().getFxomDocument() != null) {
            final FXOMObject root = getEditorController().getFxomDocument().getFxomRoot();
            if (root instanceof FXOMInstance) {
                final ToggleFxRootJob job = new ToggleFxRootJob(getEditorController());
                if (job.isExecutable()) {
                    stopListeningToJobManagerRevision();
                    getEditorController().getJobManager().push(job);
                    startListeningToJobManagerRevision();
                }
            }
        }
    }
    
    private boolean isFxRoot() {
        if (getEditorController().getFxomDocument() != null) {
            final FXOMObject root = getEditorController().getFxomDocument().getFxomRoot();
            if (root instanceof FXOMInstance) {
                return ((FXOMInstance)root).isFxRoot();
            }
        }
        
        return false;
    }
    
    private final ChangeListener<Boolean> checkBoxListener = new ChangeListener<Boolean>() {

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
            toggleFxRoot();
        }
    };

    private void resetSuggestedControllerClasses(URL location) {
        if (controllerClassEditor != null) {
            // The listener on fxmlLocationProperty is called before the file
            // denoted by the location is created on disk, hence the runLater.
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    controllerClassEditor.setUpdateFromModel(true);
                    controllerClassEditor.reset(getSuggestedControllerClasses(location));
                    controllerClassEditor.setUpdateFromModel(false);
                }
            });
        }
    }
}
