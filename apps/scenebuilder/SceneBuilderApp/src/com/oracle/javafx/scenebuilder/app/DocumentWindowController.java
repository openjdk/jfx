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
package com.oracle.javafx.scenebuilder.app;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.info.InfoPanelController;
import com.oracle.javafx.scenebuilder.app.menubar.MenuBarController;
import com.oracle.javafx.scenebuilder.app.message.MessageBarController;
import com.oracle.javafx.scenebuilder.app.preview.BackgroundColorDialogController;
import com.oracle.javafx.scenebuilder.app.preview.PreviewWindowController;
import com.oracle.javafx.scenebuilder.app.selectionbar.SelectionBarController;
import com.oracle.javafx.scenebuilder.app.template.FxmlTemplates;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController.SectionId;
import com.oracle.javafx.scenebuilder.kit.editor.panel.library.LibraryPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog.ButtonID;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AlertDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.editor.search.SearchController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 */
public class DocumentWindowController extends AbstractFxmlWindowController {
    
    
    public enum DocumentControlAction {
        SAVE_FILE,
        SAVE_AS_FILE,
        REVERT_FILE,
        PRINT_FILE,
        CLOSE_FILE,
        REVEAL_FILE,
        GOTO_CONTENT,
        GOTO_PROPERTIES,
        GOTO_LAYOUT,
        GOTO_CODE,
        TOGGLE_LIBRARY_PANEL,
        TOGGLE_HIERARCHY_PANEL,
        TOGGLE_CSS_PANEL,
        TOGGLE_LEFT_PANEL,
        TOGGLE_RIGHT_PANEL,
        TOGGLE_GUIDES_VISIBILITY,
        SHOW_PREVIEW_WINDOW,
        CHOOSE_BACKGROUND_COLOR,
        EDIT_INCLUDED_FILE,
        REVEAL_INCLUDED_FILE,
        ADD_SCENE_STYLE_SHEET,
        SET_RESOURCE,
        REMOVE_RESOURCE,
        REVEAL_RESOURCE,
        HELP
    }
    
    public enum ActionStatus {
        CANCELLED,
        DONE
    }
    
    private final EditorController editorController = new EditorController();
    private final MenuBarController menuBarController = new MenuBarController(this);
    private final ContentPanelController contentPanelController = new ContentPanelController(editorController);
    private final AbstractHierarchyPanelController hierarchyPanelController = new HierarchyPanelController(editorController);
    private final InfoPanelController infoPanelController = new InfoPanelController(editorController);
    private final InspectorPanelController inspectorPanelController = new InspectorPanelController(editorController);
    private final CssPanelDelegate cssPanelDelegate = new CssPanelDelegate(inspectorPanelController, this);
    private final CssPanelController cssPanelController = new CssPanelController(editorController, cssPanelDelegate);
    private final LibraryPanelController libraryPanelController = new LibraryPanelController(editorController);
    private final SelectionBarController selectionBarController = new SelectionBarController(editorController);
    private final MessageBarController messageBarController = new MessageBarController(editorController);
    // The PreviewWindowController is created lazily because it needs an owner
    // but computing it here would be too costly (impact on start-up time).
    private PreviewWindowController previewWindowController = null;
    private final SearchController librarySearchController = new SearchController(editorController);
    private final SearchController inspectorSearchController = new SearchController(editorController);
    private final SearchController cssPanelSearchController = new SearchController(editorController);;
    private final SceneStyleSheetMenuController sceneStyleSheetMenuController = new SceneStyleSheetMenuController(this);
    private final CssPanelMenuController cssPanelMenuController = new CssPanelMenuController(cssPanelController);
    private final ResourceController resourceController = new ResourceController((this));

    @FXML private StackPane libraryPanelHost;
    @FXML private StackPane librarySearchPanelHost;
    @FXML private StackPane hierarchyPanelHost;
    @FXML private StackPane infoPanelHost;
    @FXML private StackPane contentPanelHost;
    @FXML private StackPane inspectorPanelHost;
    @FXML private StackPane inspectorSearchPanelHost;
    @FXML private StackPane cssPanelHost;
    @FXML private StackPane cssPanelSearchPanelHost;
    @FXML private StackPane messageBarHost;
    @FXML private Accordion documentAccordion;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane leftRightSplitPane;
    @FXML private SplitPane libraryDocumentSplitPane;
    
    @FXML private CheckMenuItem libraryViewAsList;
    @FXML private CheckMenuItem libraryViewAsSections;
    
    @FXML private MenuItem cssPanelShowStyledOnlyMi;
    @FXML private MenuItem cssPanelSplitDefaultsMi;

    private SplitController bottomSplitController;
    private SplitController leftSplitController;
    private SplitController rightSplitController;
    private SplitController librarySplitController;
    private SplitController documentSplitController;
    
    private FileTime loadFileTime;
    
    /*
     * DocumentWindowController
     */
    
    public DocumentWindowController() {
        super(DocumentWindowController.class.getResource("DocumentWindow.fxml"), //NOI18N
                I18N.getBundle());
        editorController.setLibrary(SceneBuilderApp.getSingleton().getUserLibrary());
    }
    
    public EditorController getEditorController() {
        return editorController;
    }
    
    public MenuBarController getMenuBarController() {
        return menuBarController;
    }
    
    public ContentPanelController getContentPanelController() {
        return contentPanelController;
    }
    
    public InspectorPanelController getInspectorPanelController() {
        return inspectorPanelController;
    }
    
    public CssPanelController getCssPanelController() {
        return cssPanelController;
    }
    
    public AbstractHierarchyPanelController getHierarchyPanelController() {
        return hierarchyPanelController;
    }
    
    public InfoPanelController getInfoPanelController() {
        return infoPanelController;
    }
    
    public PreviewWindowController getPreviewWindowController() {
        return previewWindowController;
    }
    
    public SceneStyleSheetMenuController getSceneStyleSheetMenuController() {
        return sceneStyleSheetMenuController;
    }
    
    public ResourceController getResourceController() {
        return resourceController;
    }
    
    public void loadFromFile(File fxmlFile) throws IOException {
        final URL fxmlURL = fxmlFile.toURI().toURL();
        final String fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
        editorController.setFxmlTextAndLocation(fxmlText, fxmlURL);
        updateLoadFileTime();
    }
    
    public void loadFromURL(URL fxmlURL) {
        assert fxmlURL != null;
        try {
            final String fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
            editorController.setFxmlTextAndLocation(fxmlText, null);
            updateLoadFileTime();
        } catch(IOException x) {
            throw new IllegalStateException(x);
        }
    }

    public void loadWithDefaultContent() {
        final URL fxmlURL = FxmlTemplates.getDefaultContentURL();
        loadFromURL(fxmlURL);
    }
    
    public String getFxmlText() {
        return editorController.getFxmlText();
    }
    
    public static final String makeTitle(FXOMDocument fxomDocument) {
        final String title;
        
        if (fxomDocument == null) {
            title = I18N.getString("label.no.document");
        } else if (fxomDocument.getLocation() == null) {
            title = I18N.getString("label.untitled");
        } else {
            final File toto = new File(fxomDocument.getLocation().getPath());
            title = toto.getName();
        }
        
        return title;
    }
    
    public boolean canPerformControlAction(DocumentControlAction controlAction) {
        final boolean result;
        
        switch(controlAction) {
            case PRINT_FILE:
                result = editorController.getFxomDocument() != null;
                break;
                
            case TOGGLE_LIBRARY_PANEL:
            case TOGGLE_HIERARCHY_PANEL:
            case TOGGLE_CSS_PANEL:
            case TOGGLE_LEFT_PANEL:
            case TOGGLE_RIGHT_PANEL:
            case TOGGLE_GUIDES_VISIBILITY:
            case SHOW_PREVIEW_WINDOW:
                result = true;
                break;
                
            case CHOOSE_BACKGROUND_COLOR:
                result = false;
                break;
                
            case SAVE_FILE:
                result = editorController.canUndo();
                break;
                
            case SAVE_AS_FILE:
            case CLOSE_FILE:
                result = true;
                break;
                
            case REVERT_FILE:
                result = editorController.canUndo() 
                        && editorController.getFxomDocument().getLocation() != null;
                break;
                
            case REVEAL_FILE:
                result = (editorController.getFxomDocument() != null) 
                        && (editorController.getFxomDocument().getLocation() != null);
                break;
                
//            case PRINT_FILE:
            case GOTO_CONTENT:
            case GOTO_PROPERTIES:
            case GOTO_LAYOUT:
            case GOTO_CODE:
                result = true;
                break;
                
            case EDIT_INCLUDED_FILE:
            case REVEAL_INCLUDED_FILE:
                // TODO(elp) : to be implemented
                result = false;
                break;
                
            case ADD_SCENE_STYLE_SHEET:
                result = true;
                break;
                
            case SET_RESOURCE:
            case REMOVE_RESOURCE:
            case REVEAL_RESOURCE:
                result = true;
                break;
                
            case HELP:
                result = true;
                break;
                
            default:
                result = false;
                assert false;
                break;
        }
       
        return result;
    }
    
    public void performControlAction(DocumentControlAction controlAction) {
        assert canPerformControlAction(controlAction);
        
        switch(controlAction) {
            case SHOW_PREVIEW_WINDOW:
                if (previewWindowController == null) {
                    previewWindowController = new PreviewWindowController(editorController, getStage());
                }
                previewWindowController.openWindow();
                break;
                
            case CHOOSE_BACKGROUND_COLOR:
                performChooseBackgroundColor(getStage());
                break;
                
            case SAVE_FILE:
                performSaveOrSaveAsAction();
                break;
                
            case SAVE_AS_FILE:
                performSaveAsAction();
                break;
                
            case REVERT_FILE:
                performRevertAction();
                break;
                
            case CLOSE_FILE:
                performCloseAction();
                break;
                
            case REVEAL_FILE:
                performRevealAction();
                break;
                
            case GOTO_CONTENT:
                contentPanelController.getGlassLayer().requestFocus();
                break;

            case GOTO_PROPERTIES:
                performGoToSection(SectionId.PROPERTIES);
                break;
                
            case GOTO_LAYOUT:
                performGoToSection(SectionId.LAYOUT);
                break;
                
            case GOTO_CODE:
                performGoToSection(SectionId.CODE);
                break;
                
            case TOGGLE_LEFT_PANEL: 
                leftSplitController.toggleTarget();
                break;
                
            case TOGGLE_RIGHT_PANEL:
                rightSplitController.toggleTarget();
                break;
                
            case TOGGLE_CSS_PANEL:
                assert cssPanelHost != null;
                assert cssPanelSearchPanelHost != null;
                if (cssPanelHost.getChildren().isEmpty()) {
                    cssPanelHost.getChildren().add(cssPanelController.getPanelRoot());
                }
                if (cssPanelSearchPanelHost.getChildren().isEmpty()) {
                    cssPanelSearchPanelHost.getChildren().add(cssPanelSearchController.getPanelRoot());
                    addCssPanelSearchListener();
                }

                bottomSplitController.toggleTarget();
                break;
                
            case TOGGLE_LIBRARY_PANEL:
                librarySplitController.toggleTarget();
                if (librarySplitController.isTargetVisible() == false) {
                    // Make sure Hierarchy is visible
                    documentSplitController.showTarget();
                }
                break;
                
            case TOGGLE_HIERARCHY_PANEL:
                documentSplitController.toggleTarget();
                if (documentSplitController.isTargetVisible() == false) {
                    // Make sure Library is visible
                    librarySplitController.showTarget();
                }
                break;
                
            case TOGGLE_GUIDES_VISIBILITY:
                contentPanelController.setGuidesVisible(
                        ! contentPanelController.isGuidesVisible());
                break;
                
            case EDIT_INCLUDED_FILE:
            case REVEAL_INCLUDED_FILE:
                // TODO(elp) : to be implemented
                break;
                
            case ADD_SCENE_STYLE_SHEET:
                sceneStyleSheetMenuController.performAddSceneStyleSheet();
                break;
                
            case SET_RESOURCE:
                resourceController.performSetResource();
                break;
                
            case REMOVE_RESOURCE:
                resourceController.performRemoveResource();
                break;
                
            case REVEAL_RESOURCE:
                resourceController.performRevealResource();
                break;
                
            case HELP:
                performHelp();
                break;
                
            default:
                assert false;
                break;
        }
    }
    
    
    public boolean isLeftPanelVisible() {
        return leftSplitController.isTargetVisible();
    }
    
    
    public boolean isRightPanelVisible() {
        return rightSplitController.isTargetVisible();
    }
    
    
    public boolean isBottomPanelVisible() {
        return bottomSplitController.isTargetVisible();
    }
    
    
    public boolean isHierarchyPanelVisible() {
        return documentSplitController.isTargetVisible();
    }
    
    
    public boolean isLibraryPanelVisible() {
        return librarySplitController.isTargetVisible();
    }
    
    
    public static class TitleComparator implements Comparator<DocumentWindowController> {

        @Override
        public int compare(DocumentWindowController d1, DocumentWindowController d2) {
            final int result;
            
            assert d1 != null;
            assert d2 != null;
            
            if (d1 == d2) {
                result = 0;
            } else {
                final String t1 = d1.getStage().getTitle();
                final String t2 = d2.getStage().getTitle();
                assert t1 != null;
                assert t2 != null;
                result = t1.compareTo(t2);
            }
            
            return result;
        }
        
    }
    
    /*
     * AbstractFxmlWindowController
     */
    
    @Override
    protected void controllerDidLoadFxml() {
        
        assert libraryPanelHost != null;
        assert librarySearchPanelHost != null;
        assert hierarchyPanelHost != null;
        assert infoPanelHost != null;
        assert contentPanelHost != null;
        assert inspectorPanelHost != null;
        assert inspectorSearchPanelHost != null;
        assert messageBarHost != null;
        assert mainSplitPane != null;
        assert mainSplitPane.getItems().size() == 2;
        assert leftRightSplitPane != null;
        assert leftRightSplitPane.getItems().size() == 3;
        assert libraryDocumentSplitPane != null;
        assert libraryDocumentSplitPane.getItems().size() == 2;
        assert documentAccordion != null;
        assert documentAccordion.getPanes().isEmpty() == false;
        
        // Insert the menu bar
        assert getRoot() instanceof VBox;
        final VBox rootVBox = (VBox) getRoot();
        rootVBox.getChildren().add(0, menuBarController.getMenuBar());
        
        // Additional split pane setup (SplitPane.resizableWithParent is no in SB)
        SplitPane.setResizableWithParent(mainSplitPane.getItems().get(1), false);
        SplitPane.setResizableWithParent(leftRightSplitPane.getItems().get(0), false);
        SplitPane.setResizableWithParent(leftRightSplitPane.getItems().get(2), false);
        SplitPane.setResizableWithParent(libraryDocumentSplitPane.getItems().get(0), false);
        
        libraryPanelHost.getChildren().add(libraryPanelController.getPanelRoot());
        librarySearchPanelHost.getChildren().add(librarySearchController.getPanelRoot());
        hierarchyPanelHost.getChildren().add(hierarchyPanelController.getPanelRoot());
        infoPanelHost.getChildren().add(infoPanelController.getPanelRoot());
        contentPanelHost.getChildren().add(contentPanelController.getPanelRoot());
        inspectorPanelHost.getChildren().add(inspectorPanelController.getPanelRoot());
        inspectorSearchPanelHost.getChildren().add(inspectorSearchController.getPanelRoot());
        messageBarHost.getChildren().add(messageBarController.getPanelRoot());
        
        messageBarController.getSelectionBarHost().getChildren().add(
                selectionBarController.getPanelRoot());
        
        inspectorSearchController.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> ov, String oldStr, String newStr) {
                inspectorPanelController.setSearchPattern(newStr);
            }
        });
        
        librarySearchController.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> ov, String oldStr, String newStr) {
                libraryPanelController.setSearchPattern(newStr);
            }
        });
        
        bottomSplitController = new SplitController(mainSplitPane, SplitController.Target.LAST);
        leftSplitController = new SplitController(leftRightSplitPane, SplitController.Target.FIRST);
        rightSplitController = new SplitController(leftRightSplitPane, SplitController.Target.LAST);
        librarySplitController = new SplitController(libraryDocumentSplitPane, SplitController.Target.FIRST);
        documentSplitController = new SplitController(libraryDocumentSplitPane, SplitController.Target.LAST);
        bottomSplitController.hideTarget();
        
        messageBarHost.heightProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                final double h = messageBarHost.getHeight();
                contentPanelHost.setPadding(new Insets(h, 0.0, 0.0, 0.0));
            }
        });
        
        documentAccordion.setExpandedPane(documentAccordion.getPanes().get(0));
    }
    
    @Override
    protected void controllerDidCreateStage() {
        updateStageTitle();
    }
    
    @Override 
    public void onCloseRequest(WindowEvent event) {
        performCloseAction();
    }

    public boolean isFrontDocumentWindow() {
        // Should add code skeleton window when available
        return getStage().isFocused()
                || (previewWindowController != null
                && previewWindowController.getStage().isFocused());
    }

    public void performCloseFrontDocumentWindow() {
        // Should add code skeleton window when available
        if (getStage().isFocused()) {
            performCloseAction();
        } else if (previewWindowController != null
                && previewWindowController.getStage().isFocused()) {
            previewWindowController.closeWindow();
        }
    }

    //
    // Inspector menu
    //
    @FXML
    void onInspectorShowAllAction(ActionEvent event) {
        inspectorPanelController.setShowMode(InspectorPanelController.ShowMode.ALL);
        
    }
    
    @FXML
    void onInspectorShowEditedAction(ActionEvent event) {
        inspectorPanelController.setShowMode(InspectorPanelController.ShowMode.EDITED);
    }
    
    @FXML
    void onInspectorViewSectionsAction(ActionEvent event) {
        inspectorPanelController.setViewMode(InspectorPanelController.ViewMode.SECTION);
    }
    
    @FXML
    void onInspectorViewByPropertyNameAction(ActionEvent event) {
        inspectorPanelController.setViewMode(InspectorPanelController.ViewMode.PROPERTY_NAME);
    }
    
    @FXML
    void onInspectorViewByPropertyTypeAction(ActionEvent event) {
        inspectorPanelController.setViewMode(InspectorPanelController.ViewMode.PROPERTY_TYPE);
    }
    
    //
    // CSS menu
    //
    
    @FXML
    void onCssPanelViewRulesAction(ActionEvent event) {
        cssPanelMenuController.viewRules();
    }

    @FXML
    void onCssPanelViewTableAction(ActionEvent event) {
        cssPanelMenuController.viewTable();
    }

    @FXML
    void onCssPanelViewTextAction(ActionEvent event) {
        cssPanelMenuController.viewText();
    }

    @FXML
    void onCssPanelCopyStyleablePathAction(ActionEvent event) {
        cssPanelMenuController.copyStyleablePath();
    }

    @FXML
    void onCssPanelSplitDefaultsAction(ActionEvent event) {
        cssPanelMenuController.splitDefaultsAction(cssPanelSplitDefaultsMi);
    }

    @FXML
    void onCssPanelShowStyledOnlyAction(ActionEvent event) {
        cssPanelMenuController.showStyledOnly(cssPanelShowStyledOnlyMi);
    }
    
    //
    // Hierarchy menu
    //
    @FXML
    void onHierarchyShowInfo(ActionEvent event) {
        hierarchyPanelController.setDisplayOption(AbstractHierarchyPanelController.DisplayOption.INFO);
        
    }
    
    @FXML
    void onHierarchyShowFxId(ActionEvent event) {
        hierarchyPanelController.setDisplayOption(AbstractHierarchyPanelController.DisplayOption.FXID);
    }
    
    @FXML
    void onHierarchyShowNodeId(ActionEvent event) {
        hierarchyPanelController.setDisplayOption(AbstractHierarchyPanelController.DisplayOption.NODEID);
    }
    
    //
    // Library menu
    //
    @FXML
    void onLibraryImportJarFxml(ActionEvent event) {
        libraryPanelController.performImportJarFxml();
    }
    
    @FXML
    void onLibraryViewAsList(ActionEvent event) {
        libraryViewAsList.setSelected(true);
        libraryViewAsSections.setSelected(false);
        libraryPanelController.setDisplayMode(LibraryPanelController.DISPLAY_MODE.LIST);
    }
    
    @FXML
    void onLibraryViewAsSections(ActionEvent event) {
        libraryViewAsList.setSelected(false);
        libraryViewAsSections.setSelected(true);
        libraryPanelController.setDisplayMode(LibraryPanelController.DISPLAY_MODE.SECTIONS);
    }

    @FXML
    void onLibraryImportSelection(ActionEvent event) {
        System.out.println("[DocumentWindowController::onLibraryImportSelection] Not yet available"); //NOI18N
    }
    
    /*
     * Private
     */
    
    private void updateStageTitle() {
        getStage().setTitle(makeTitle(editorController.getFxomDocument()));
    }
    
    private void performChooseBackgroundColor(Window owner) {
        final BackgroundColorDialogController bcdc 
                = new BackgroundColorDialogController(owner);
        bcdc.openWindow();
    }
    
    ActionStatus performSaveOrSaveAsAction() {
        final ActionStatus result;
        
        if (editorController.getFxomDocument().getLocation() == null) {
            result = performSaveAsAction();
        } else {
            result = performSaveAction();
        }
        
        return result;
    }
    
    private void addCssPanelSearchListener() {
        cssPanelSearchController.textProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> ov, String oldStr, String newStr) {
                cssPanelController.setSearchPattern(newStr);
            }
        });
    }
    
    private void performGoToSection(SectionId sectionId) {
        inspectorPanelController.setExpandedSection(sectionId);
    }
    
    private ActionStatus performSaveAction() {
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        assert fxomDocument != null;
        assert fxomDocument.getLocation() != null;
        
        ActionStatus result;
        if (editorController.canGetFxmlText()) {
            final Path fxmlPath;
            try {
                fxmlPath = Paths.get(fxomDocument.getLocation().toURI());
            } catch(URISyntaxException x) {
                // Should not happen
                throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
            }
            final String fileName = fxmlPath.getFileName().toString();
            
            try {
                final boolean saveConfirmed;
                if (checkLoadFileTime()) {
                    saveConfirmed = true;
                } else {
                    final AlertDialog d = new AlertDialog(getStage());
                    d.setMessage(I18N.getString("alert.overwrite.message", fileName));
                    d.setDetails(I18N.getString("alert.overwrite.details"));
                    d.setOKButtonVisible(true);
                    d.setOKButtonTitle(I18N.getString("label.overwrite"));
                    d.setDefaultButtonID(ButtonID.CANCEL);
                    d.setShowDefaultButton(true);
                    saveConfirmed = (d.showAndWait() == ButtonID.OK);
                }
            
                if (saveConfirmed) {
                    try {
                        final byte[] fxmlBytes = fxomDocument.getFxmlText().getBytes("UTF-8"); //NOI18N
                        Files.write(fxmlPath, fxmlBytes);
                        updateLoadFileTime();
                        editorController.clearUndoRedo();

                        editorController.getMessageLog().logInfoMessage(
                                "log.info.save.confirmation", I18N.getBundle(), fileName);
                        result = ActionStatus.DONE;
                    } catch(UnsupportedEncodingException x) {
                        // Should not happen
                        throw new RuntimeException("Bug", x); //NOI18N
                    }
                } else {
                    result = ActionStatus.CANCELLED;
                }
            } catch(IOException x) {
                final ErrorDialog d = new ErrorDialog(getStage());
                d.setMessage(I18N.getString("alert.save.failure.message", fileName));
                d.setDetails(I18N.getString("alert.save.failure.details"));
                d.setDebugInfoWithThrowable(x);
                d.showAndWait();
                result = ActionStatus.CANCELLED;
            }
        } else {
            result = ActionStatus.CANCELLED;
        }
        
        return result;
    }
    
    
    private ActionStatus performSaveAsAction() {
        
        final ActionStatus result;
        if (editorController.canGetFxmlText()) {
            final FileChooser fileChooser = new FileChooser();
            final FileChooser.ExtensionFilter f 
                    = new FileChooser.ExtensionFilter(I18N.getString("file.filter.label.fxml"),
                            "*.fxml"); //NOI18N
            fileChooser.getExtensionFilters().add(f);
            
            final File nextInitialDirectory 
                    = SceneBuilderApp.getSingleton().getNextInitialDirectory();
            if (nextInitialDirectory != null) {
                fileChooser.setInitialDirectory(nextInitialDirectory);
            }

            File fxmlFile = fileChooser.showSaveDialog(getStage());
            if (fxmlFile == null) {
                result = ActionStatus.CANCELLED;
                
            } else {
                // See DTL-5948: on Linux we anticipate an extension less path.
                final String path = fxmlFile.getPath();
                if (! path.endsWith(".fxml")) { //NOI18N
                    fxmlFile = new File(path + ".fxml"); //NOI18N
                }
                
                // Transform File into URL
                final URL newLocation;
                try {
                    newLocation = fxmlFile.toURI().toURL();
                } catch(MalformedURLException x) {
                    // Should not happen
                    throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
                }
                
                // Checks if fxmlFile is the name of an already opened document
                final DocumentWindowController dwc
                        = SceneBuilderApp.getSingleton().lookupDocumentWindowControllers(newLocation);
                if (dwc != null) {
                    final Path fxmlPath = Paths.get(fxmlFile.toString());
                    final String fileName = fxmlPath.getFileName().toString();
                    final ErrorDialog d = new ErrorDialog(getStage());
                    d.setMessage(I18N.getString("alert.save.conflict.message", fileName));
                    d.setDetails(I18N.getString("alert.save.conflict.details"));
                    d.showAndWait();
                    result = ActionStatus.CANCELLED;
                } else {
                    // Recalculates references if needed
                    // TODO(elp)

                    // First change the location of the fxom document
                    editorController.setFxmlLocation(newLocation);
                    updateLoadFileTime();
                    updateStageTitle();

                    // Now performs a regular save action
                    result = performSaveAction();
                    
                    // Keep track of the user choice for next time
                    SceneBuilderApp.getSingleton().updateNextInitialDirectory(fxmlFile);
                }
            }
        } else {
            result = ActionStatus.CANCELLED;
        }
        
        return result;
    }
    
    
    private void performRevertAction() {
        assert editorController.getFxomDocument() != null;
        assert editorController.getFxomDocument().getLocation() != null;
        
        final AlertDialog d = new AlertDialog(getStage());
        d.setMessage(I18N.getString("alert.revert.question.message", getStage().getTitle()));
        d.setDetails(I18N.getString("alert.revert.question.details"));
        d.setOKButtonTitle(I18N.getString("label.revert"));

        if (d.showAndWait() == AlertDialog.ButtonID.OK) {
            final URL location = editorController.getFxomDocument().getLocation();
            final File fxmlFile = new File(location.getPath());
            try {
                loadFromFile(fxmlFile);
            } catch(IOException x) {
                final ErrorDialog errorDialog = new ErrorDialog(null);
                errorDialog.setMessage(I18N.getString("alert.open.failure1.message", getStage().getTitle()));
                errorDialog.setDetails(I18N.getString("alert.open.failure1.details"));
                errorDialog.setDebugInfoWithThrowable(x);
                errorDialog.setTitle(I18N.getString("alert.title.open"));
                errorDialog.showAndWait();
                SceneBuilderApp.getSingleton().documentWindowRequestClose(this);
            }
        }
    }
    
    
    ActionStatus performCloseAction() {
        
        // Makes sure that our window is front 
        getStage().toFront();
        
        // Checks if there are some pending changes
        final boolean closeConfirmed;
        if (editorController.getJobManager().canUndo()) {
            
            final AlertDialog d = new AlertDialog(getStage());
            d.setMessage(I18N.getString("alert.save.question.message", getStage().getTitle()));
            d.setDetails(I18N.getString("alert.save.question.details"));
            d.setOKButtonTitle(I18N.getString("label.save"));
            d.setActionButtonTitle(I18N.getString("label.do.not.save"));
            d.setActionButtonVisible(true);
            
            switch(d.showAndWait()) {
                default:
                case OK:
                    if (editorController.getFxomDocument().getLocation() == null) {
                        closeConfirmed = (performSaveAsAction() == ActionStatus.DONE);
                    } else {
                        closeConfirmed = (performSaveAction() == ActionStatus.DONE);
                    }
                    break;
                case CANCEL:
                    closeConfirmed = false;
                    break;
                case ACTION: // Do not save
                    closeConfirmed = true;
                    break;
            }
            
        } else {
            // No pending changes
            closeConfirmed = true;
        }
        
        // Closes if confirmed
        if (closeConfirmed) {
            SceneBuilderApp.getSingleton().documentWindowRequestClose(this);
        }
        
        return closeConfirmed ? ActionStatus.DONE : ActionStatus.CANCELLED;
    }
    
    
    private void performRevealAction() {
        assert editorController.getFxomDocument() != null;
        assert editorController.getFxomDocument().getLocation() != null;
        
        final URL location = editorController.getFxomDocument().getLocation();
        final File fxmlFile = new File(location.getPath());
        
        try {
            EditorPlatform.revealInFileBrowser(fxmlFile);
        } catch(IOException x) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setMessage(I18N.getString("alert.reveal.failure.message", getStage().getTitle()));
            errorDialog.setDetails(I18N.getString("alert.reveal.failure.details"));
            errorDialog.setDebugInfoWithThrowable(x);
            errorDialog.showAndWait();
        }
    }
    
    
    private void updateLoadFileTime() {
        
        final URL fxmlURL = editorController.getFxmlLocation();
        if (fxmlURL == null) {
            loadFileTime = null;
        } else {
            try {
                final Path fxmlPath = Paths.get(fxmlURL.toURI());
                if (Files.exists(fxmlPath)) {
                    loadFileTime = Files.getLastModifiedTime(fxmlPath);
                } else {
                    loadFileTime = null;
                }
            } catch(URISyntaxException x) {
                throw new RuntimeException("Bug", x); //NOI18N
            } catch(IOException x) {
                loadFileTime = null;
            }
        }
    }
    
    
    private boolean checkLoadFileTime() throws IOException {
        assert editorController.getFxmlLocation() != null;
        
        /*
         *  loadFileTime == null
         *          => fxml file does not exist
         *          => TRUE
         *
         *  loadFileTime != null
         *          => fxml file does/did exist
         *
         *          currentFileTime == null
         *              => fxml file no longer exists
         *              => TRUE
         *
         *          currentFileTime != null
         *              => fxml file still exists
         *              => loadFileTime.compare(currentFileTime) == 0
         */
        
        boolean result;
        if (loadFileTime == null) {
            // editorController.getFxmlLocation() does not exist yet
            result = true;
        } else {
            try {
                // editorController.getFxmlLocation() still exists
                // Check if its file time matches loadFileTime
                Path fxmlPath = Paths.get(editorController.getFxmlLocation().toURI());
                FileTime currentFileTime = Files.getLastModifiedTime(fxmlPath);
                result = loadFileTime.compareTo(currentFileTime) == 0;
            } catch(NoSuchFileException x) {
                // editorController.getFxmlLocation() no longer exists
                result = true;
            } catch(URISyntaxException x) {
                throw new RuntimeException("Bug", x); //NOI18N
            }
        }
        
        return result;
    }
    
        
    private void performHelp() {
        try {
            EditorPlatform.open(EditorPlatform.DOCUMENTATION_URL);
        } catch (IOException ioe) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setMessage(I18N.getString("alert.help.failure.message", EditorPlatform.DOCUMENTATION_URL));
            errorDialog.setDetails(I18N.getString("alert.messagebox.failure.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
    }
}
