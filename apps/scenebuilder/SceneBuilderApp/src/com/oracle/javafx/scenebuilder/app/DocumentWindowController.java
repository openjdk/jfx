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
package com.oracle.javafx.scenebuilder.app;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.info.InfoPanelController;
import com.oracle.javafx.scenebuilder.app.menubar.MenuBarController;
import com.oracle.javafx.scenebuilder.app.message.MessageBarController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordDocument;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal;
import com.oracle.javafx.scenebuilder.app.preview.BackgroundColorDialogController;
import com.oracle.javafx.scenebuilder.app.preview.PreviewWindowController;
import com.oracle.javafx.scenebuilder.app.report.JarAnalysisReportController;
import com.oracle.javafx.scenebuilder.app.selectionbar.SelectionBarController;
import com.oracle.javafx.scenebuilder.app.skeleton.SkeletonWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.ControlAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.EditAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController.SectionId;
import com.oracle.javafx.scenebuilder.kit.editor.panel.library.LibraryPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog.ButtonID;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AlertDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.editor.search.SearchController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.sun.javafx.scene.control.behavior.KeyBinding;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 */
public class DocumentWindowController extends AbstractFxmlWindowController {
    
    
    public enum DocumentControlAction {
        COPY,
        SELECT_ALL,
        SELECT_NONE,
        SAVE_FILE,
        SAVE_AS_FILE,
        REVERT_FILE,
        CLOSE_FILE,
        REVEAL_FILE,
        GOTO_CONTENT,
        GOTO_PROPERTIES,
        GOTO_LAYOUT,
        GOTO_CODE,
        TOGGLE_LIBRARY_PANEL,
        TOGGLE_DOCUMENT_PANEL,
        TOGGLE_CSS_PANEL,
        TOGGLE_LEFT_PANEL,
        TOGGLE_RIGHT_PANEL,
        TOGGLE_OUTLINES_VISIBILITY,
        TOGGLE_GUIDES_VISIBILITY,
        SHOW_PREVIEW_WINDOW,
        CHOOSE_BACKGROUND_COLOR,
        ADD_SCENE_STYLE_SHEET,
        SET_RESOURCE,
        REMOVE_RESOURCE,
        REVEAL_RESOURCE,
        HELP,
        SHOW_SAMPLE_CONTROLLER
    }
    
    public enum DocumentEditAction {
        DELETE,
        CUT,
        PASTE,
        IMPORT_FXML,
        IMPORT_MEDIA
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
    private final SearchController librarySearchController = new SearchController(editorController);
    private final SearchController inspectorSearchController = new SearchController(editorController);
    private final SearchController cssPanelSearchController = new SearchController(editorController);;
    private final SceneStyleSheetMenuController sceneStyleSheetMenuController = new SceneStyleSheetMenuController(this);
    private final CssPanelMenuController cssPanelMenuController = new CssPanelMenuController(cssPanelController);
    private final ResourceController resourceController = new ResourceController((this));    
    private final DocumentWatchingController watchingController = new DocumentWatchingController(this);
    
    // The controller below are created lazily because they need an owner
    // and computing them here would be too costly (impact on start-up time):
    // - PreviewWindowController
    // - SkeletonWindowController
    // - JarAnalysisReportController
    private PreviewWindowController previewWindowController = null;
    private SkeletonWindowController skeletonWindowController = null;
    private JarAnalysisReportController jarAnalysisReportController = null;

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
    
    @FXML private MenuButton libraryMenuButton;
    @FXML private MenuItem libraryImportSelection;
    @FXML private RadioMenuItem libraryViewAsList;
    @FXML private RadioMenuItem libraryViewAsSections;
    @FXML private MenuItem libraryReveal;
    @FXML private Menu customLibraryMenu;
    
    @FXML private MenuItem cssPanelShowStyledOnlyMi;
    @FXML private MenuItem cssPanelSplitDefaultsMi;
    
    @FXML private RadioMenuItem showInfoMenuItem;
    @FXML private RadioMenuItem showFxIdMenuItem;
    @FXML private RadioMenuItem showNodeIdMenuItem;

    private SplitController bottomSplitController;
    private SplitController leftSplitController;
    private SplitController rightSplitController;
    private SplitController librarySplitController;
    private SplitController documentSplitController;
    
    private FileTime loadFileTime;
    private Job saveJob;

    private static List<String> imageExtensions;
    private static List<String> audioExtensions;
    private static List<String> videoExtensions;
    private static List<String> mediaExtensions;

    private final EventHandler<KeyEvent> mainKeyEventFilter = new EventHandler<KeyEvent>() {

        @Override
        public void handle(KeyEvent event) {
            //------------------------------------------------------------------
            // TEXT INPUT CONTROL
            //------------------------------------------------------------------
            // Common editing actions handled natively and defined as application accelerators
            // 
            // The platform support is not mature/stable enough to rely on.
            // Indeed, the behavior may differ :
            // - when using system menu bar vs not using it
            // - when using accelerators vs using menu items
            // - depending on the focused control (TextField vs ComboBox)
            // 
            // On SB side, we decide for now to consume events that may be handled natively
            // so ALL actions are defined in our ApplicationMenu class.
            //
            // This may be revisit when platform implementation will be more reliable.
            //
            final Node focusOwner = getScene().getFocusOwner();
            final KeyCombination accelerator = getAccelerator(event);
            if (isTextInputControlEditing(focusOwner) == true 
                    && accelerator != null) {
                for (KeyBinding binding : SBTextInputControlBindings.getBindings()) {
                    // The event is handled natively
                    if (binding.getSpecificity(null, event) > 0) {
                        // 
                        // When using system menu bar, the event is handled natively 
                        // before the application receives it : we just consume the event 
                        // so the editing action is not performed a second time by the app.
                        if (menuBarController.getMenuBar().isUseSystemMenuBar()) {
                            event.consume();
                        }
                        break;
                    }
                }
            }

            //------------------------------------------------------------------
            // Hierarchy TreeView + select all
            //------------------------------------------------------------------
            // Select all is handled natively by TreeView (= hierarchy panel control).
            boolean modifierDown = (EditorPlatform.IS_MAC ? event.isMetaDown() : event.isControlDown());
            boolean isSelectAll = KeyCode.A.equals(event.getCode()) && modifierDown;
            if (getHierarchyPanelController().getPanelControl().isFocused() && isSelectAll) {
                // Consume the event so the control action is not performed natively.
                event.consume();
                // When using system menu bar, the control action is performed by the app.
                if (menuBarController.getMenuBar().isUseSystemMenuBar() == false) {
                    if (canPerformControlAction(DocumentControlAction.SELECT_ALL)) {
                        performControlAction(DocumentControlAction.SELECT_ALL);
                    }
                }
            }

            // MenuItems define a single accelerator.
            // BACK_SPACE key must be handled same way as DELETE key.
            boolean isBackspace = KeyCode.BACK_SPACE.equals(event.getCode());
            if (isTextInputControlEditing(focusOwner) == false && isBackspace) {
                if (canPerformEditAction(DocumentEditAction.DELETE)) {
                    performEditAction(DocumentEditAction.DELETE);
                }
                event.consume();
            }
        }
    };
    
    /*
     * DocumentWindowController
     */
    
    public DocumentWindowController() {
        super(DocumentWindowController.class.getResource("DocumentWindow.fxml"), //NOI18N
                I18N.getBundle(), false); // sizeToScene = false because sizing is defined in preferences
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
    
    public DocumentWatchingController getWatchingController() {
        return watchingController;
    }
    
    public SplitController getBottomSplitController() {
        return bottomSplitController;
    }
    
    public SplitController getLeftSplitController() {
        return leftSplitController;
    }
    
    public SplitController getRightSplitController() {
        return rightSplitController;
    }
    
    public SplitController getLibrarySplitController() {
        return librarySplitController;
    }
    
    public SplitController getDocumentSplitController() {
        return documentSplitController;
    }
    
    public void loadFromFile(File fxmlFile) throws IOException {
        final URL fxmlURL = fxmlFile.toURI().toURL();
        final String fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
        editorController.setFxmlTextAndLocation(fxmlText, fxmlURL);
        updateLoadFileTime();
        updateStageTitle(); // No-op if fxml has not been loaded yet
        updateFromDocumentPreferences();
        watchingController.update();
    }
    
    public void loadFromURL(URL fxmlURL) {
        assert fxmlURL != null;
        try {
            final String fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
            editorController.setFxmlTextAndLocation(fxmlText, null);
            updateLoadFileTime();
            updateStageTitle(); // No-op if fxml has not been loaded yet
            updateFromDocumentPreferences();
            watchingController.update();
        } catch(IOException x) {
            throw new IllegalStateException(x);
        }
    }

    public void loadWithDefaultContent() {
        try {
            editorController.setFxmlTextAndLocation("", null); //NOI18N
            updateLoadFileTime();
            updateStageTitle(); // No-op if fxml has not been loaded yet
            watchingController.update();
        } catch (IOException x) {
            throw new IllegalStateException(x);
        }
    }
    
    public void reload() throws IOException {
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        assert (fxomDocument != null) && (fxomDocument.getLocation() != null);
        final URL fxmlURL = fxomDocument.getLocation();
        final String fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
        editorController.setFxmlTextAndLocation(fxmlText, fxmlURL);
        updateLoadFileTime();
        // Here we do not invoke updateStageTitleAndPreferences() neither watchingController.update()
    }
    
    public String getFxmlText() {
        return editorController.getFxmlText();
    }
    
    public void refreshLibraryDisplayOption(LibraryPanelController.DISPLAY_MODE option) {
        switch (option) {
            case LIST:
                libraryViewAsList.setSelected(true);
                break;
            case SECTIONS:
                libraryViewAsSections.setSelected(true);
                break;
            default:
                assert false;
                break;
        }
        libraryPanelController.setDisplayMode(option);
    }
    
    public void refreshHierarchyDisplayOption(DisplayOption option) {
        switch(option) {
            case INFO:
                showInfoMenuItem.setSelected(true);
                break;
            case FXID:
                showFxIdMenuItem.setSelected(true);
                break;
            case NODEID:
                showNodeIdMenuItem.setSelected(true);
                break;
            default:
                assert false;
                break;
        }
        hierarchyPanelController.setDisplayOption(option);
    }

    public void refreshCssTableColumnsOrderingReversed(boolean cssTableColumnsOrderingReversed) {
        cssPanelController.setTableColumnsOrderingReversed(cssTableColumnsOrderingReversed);
    }

    public static final String makeTitle(FXOMDocument fxomDocument) {
        final String title;
        
        if (fxomDocument == null) {
            title = I18N.getString("label.no.document");
        } else if (fxomDocument.getLocation() == null) {
            title = I18N.getString("label.untitled");
        } else {
            String name = ""; //NOI18N
            try {
                final File toto = new File(fxomDocument.getLocation().toURI());
                name = toto.getName();
            } catch (URISyntaxException ex) {
                throw new RuntimeException("Bug", ex); //NOI18N
            }
            title = name;
        }
        
        return title;
    }
    
    public boolean canPerformControlAction(DocumentControlAction controlAction) {
        final boolean result;
        
        switch(controlAction) {
            case COPY:
                result = canPerformCopy();
                break;
                
            case SELECT_ALL:
                result = canPerformSelectAll();
                break;
                
            case SELECT_NONE:
                result = canPerformSelectNone();
                break;
                
            case SHOW_SAMPLE_CONTROLLER:
                result = editorController.getFxomDocument() != null;
                break;
                
            case TOGGLE_LIBRARY_PANEL:
            case TOGGLE_DOCUMENT_PANEL:
            case TOGGLE_CSS_PANEL:
            case TOGGLE_LEFT_PANEL:
            case TOGGLE_RIGHT_PANEL:
            case TOGGLE_OUTLINES_VISIBILITY:
            case TOGGLE_GUIDES_VISIBILITY:
            case SHOW_PREVIEW_WINDOW:
                result = true;
                break;
                
            case CHOOSE_BACKGROUND_COLOR:
                result = false;
                break;
                
            case SAVE_FILE:
                result = isDocumentDirty()
                        || editorController.getFxomDocument().getLocation() == null; // Save new empty document
                break;
                
            case SAVE_AS_FILE:
            case CLOSE_FILE:
                result = true;
                break;
                
            case REVERT_FILE:
                result = isDocumentDirty() 
                        && editorController.getFxomDocument().getLocation() != null;
                break;
                
            case REVEAL_FILE:
                result = (editorController.getFxomDocument() != null) 
                        && (editorController.getFxomDocument().getLocation() != null);
                break;
                
            case GOTO_CONTENT:
            case GOTO_PROPERTIES:
            case GOTO_LAYOUT:
            case GOTO_CODE:
                result = true;
                break;
                
            case ADD_SCENE_STYLE_SHEET:
                result = true;
                break;
                
            case SET_RESOURCE:
                result = true;
                break;
                
            case REMOVE_RESOURCE:
            case REVEAL_RESOURCE:
                result = resourceController.getResourceFile() != null;
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
        
        final PreferencesController pc = PreferencesController.getSingleton();
        final PreferencesRecordDocument recordDocument = pc.getRecordDocument(this);
        
        switch(controlAction) {
            case COPY:
                performCopy();
                break;
                
            case SELECT_ALL:
                performSelectAll();
                break;
                
            case SELECT_NONE:
                performSelectNone();
                break;
                
            case SHOW_PREVIEW_WINDOW:
                if (previewWindowController == null) {
                    previewWindowController = new PreviewWindowController(editorController, getStage());
                    previewWindowController.setToolStylesheet(getToolStylesheet());
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
                if (leftSplitController.isTargetVisible()) {
                    assert librarySplitController.isTargetVisible()
                            || documentSplitController.isTargetVisible();
                    // Hide Left => hide both Library + Document
                    librarySplitController.hideTarget();
                    documentSplitController.hideTarget();
                    leftSplitController.hideTarget();
                } else {
                    assert librarySplitController.isTargetVisible() == false
                            && documentSplitController.isTargetVisible() == false;
                    // Show Left => show both Library + Document
                    librarySplitController.showTarget();
                    documentSplitController.showTarget();
                    leftSplitController.showTarget();

                    // This workarounds layout issues when showing Left
                    libraryDocumentSplitPane.layout();
                    libraryDocumentSplitPane.setDividerPositions(0.5);
                }
                // Update preferences
                recordDocument.setLibraryVisible(librarySplitController.isTargetVisible());
                recordDocument.setDocumentVisible(documentSplitController.isTargetVisible());
                recordDocument.setLeftVisible(leftSplitController.isTargetVisible());
                break;

            case TOGGLE_RIGHT_PANEL:
                rightSplitController.toggleTarget();
                // Update preferences
                recordDocument.setRightVisible(rightSplitController.isTargetVisible());
                break;
                
            case TOGGLE_CSS_PANEL:
                // CSS panel is built lazely : initialize the CSS panel first
                initializeCssPanel();
                bottomSplitController.toggleTarget();
                if (bottomSplitController.isTargetVisible()) {
                    // CSS panel is built lazely
                    // Need to update its table column ordering with preference value
                    final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
                    refreshCssTableColumnsOrderingReversed(recordGlobal.isCssTableColumnsOrderingReversed());
                    // Enable pick mode
                    editorController.setPickModeEnabled(true);
                } else {
                    // Disable pick mode
                    editorController.setPickModeEnabled(false);
                }
                // Update preferences
                recordDocument.setBottomVisible(bottomSplitController.isTargetVisible());
                break;
                
            case TOGGLE_LIBRARY_PANEL:
                if (librarySplitController.isTargetVisible()) {
                    assert leftSplitController.isTargetVisible();
                    librarySplitController.hideTarget();
                    if (documentSplitController.isTargetVisible() == false) {
                        leftSplitController.hideTarget();
                    }
                } else {
                    if (leftSplitController.isTargetVisible() == false) {
                        leftSplitController.showTarget();
                    }
                    librarySplitController.showTarget();
                }
                // Update preferences
                recordDocument.setLibraryVisible(librarySplitController.isTargetVisible());
                recordDocument.setLeftVisible(leftSplitController.isTargetVisible());
                break;
                
            case TOGGLE_DOCUMENT_PANEL:
                if (documentSplitController.isTargetVisible()) {
                    assert leftSplitController.isTargetVisible();
                    documentSplitController.hideTarget();
                    if (librarySplitController.isTargetVisible() == false) {
                        leftSplitController.hideTarget();
                    }
                } else {
                    if (leftSplitController.isTargetVisible() == false) {
                        leftSplitController.showTarget();
                    }
                    documentSplitController.showTarget();
                }
                // Update preferences
                recordDocument.setDocumentVisible(documentSplitController.isTargetVisible());
                recordDocument.setLeftVisible(leftSplitController.isTargetVisible());
                break;
                
            case TOGGLE_OUTLINES_VISIBILITY:
                contentPanelController.setOutlinesVisible(
                        ! contentPanelController.isOutlinesVisible());
                break;
                
            case TOGGLE_GUIDES_VISIBILITY:
                contentPanelController.setGuidesVisible(
                        ! contentPanelController.isGuidesVisible());
                break;
                
            case ADD_SCENE_STYLE_SHEET:
                sceneStyleSheetMenuController.performAddSceneStyleSheet();
                break;
                
            case SET_RESOURCE:
                resourceController.performSetResource();
                // Update preferences
                recordDocument.setI18NResourceFile(getResourceFile());
                break;
                
            case REMOVE_RESOURCE:
                resourceController.performRemoveResource();
                // Update preferences
                recordDocument.setI18NResourceFile(getResourceFile());
                break;
                
            case REVEAL_RESOURCE:
                resourceController.performRevealResource();
                break;
                
            case HELP:
                performHelp();
                break;
                
            case SHOW_SAMPLE_CONTROLLER:
                if (skeletonWindowController == null) {
                    skeletonWindowController = new SkeletonWindowController(editorController, getStage());
                    skeletonWindowController.setToolStylesheet(getToolStylesheet());
                }
                skeletonWindowController.openWindow();
                break;
                
            default:
                assert false;
                break;
        }
    }
    
    public boolean canPerformEditAction(DocumentEditAction editAction) {
        final boolean result;
        
        switch(editAction) {
            case DELETE:
                result = editorController.canPerformEditAction(EditAction.DELETE);
                break;
                
            case CUT:
                result = canPerformCut();
                break;
                
            case IMPORT_FXML:
                result = true;
                break;
                
            case IMPORT_MEDIA:
                result = true;
                break;
                
            case PASTE:
                result = canPerformPaste();
                break;
                
            default:
                result = false;
                assert false;
                break;
        }
       
        return result;
    }
    
    public void performEditAction(DocumentEditAction editAction) {
        assert canPerformEditAction(editAction);
        
        switch(editAction) {
            case DELETE:
                performDelete();
                break;
                
            case CUT:
                performCut();
                break;
                
            case IMPORT_FXML:
                performImportFxml();
                break;
                
            case IMPORT_MEDIA:
                performImportMedia();
                break;

            case PASTE:
                performPaste();
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
    
    public File getResourceFile() {
        return resourceController.getResourceFile();
    }
    
    public void setResourceFile(File file) {
        resourceController.setResourceFile(file);
    }
    
    public boolean isDocumentDirty() {
        return getEditorController().getJobManager().getCurrentJob() != saveJob;
    }
    
    public boolean isUnused() {
        /*
         * A document window controller is considered as "unused" if: //NOI18N
         *  1) it has not fxml text
         *  2) it is not dirty
         *  3) it is unamed
         */
        
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        final boolean noFxmlText = (fxomDocument == null) || (fxomDocument.getFxomRoot() == null);
        final boolean clean = isDocumentDirty() == false;
        final boolean noName = (fxomDocument != null) && (fxomDocument.getLocation() == null);
        
        return noFxmlText && clean && noName;
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
    
    public void initializeCssPanel() {
        assert cssPanelHost != null;
        assert cssPanelSearchPanelHost != null;
        if (cssPanelHost.getChildren().isEmpty()) {
            cssPanelHost.getChildren().add(cssPanelController.getPanelRoot());
        }
        if (cssPanelSearchPanelHost.getChildren().isEmpty()) {
            cssPanelSearchPanelHost.getChildren().add(cssPanelSearchController.getPanelRoot());
            addCssPanelSearchListener();
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
        assert libraryViewAsList != null;
        assert libraryViewAsSections != null;
        assert libraryReveal != null;
        assert libraryMenuButton != null;
        assert libraryImportSelection != null;
        assert customLibraryMenu != null;
        
        // Add a border to the Windows app, because of the specific window decoration on Windows.
        if (EditorPlatform.IS_WINDOWS) {
            getRoot().getStyleClass().add("windows-app-border");//NOI18N
        }
        
        mainSplitPane.addEventFilter(KeyEvent.KEY_PRESSED, mainKeyEventFilter);
        
        // Insert the menu bar
        assert getRoot() instanceof VBox;
        final VBox rootVBox = (VBox) getRoot();
        rootVBox.getChildren().add(0, menuBarController.getMenuBar());
        
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
        
        messageBarHost.heightProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                final double h = messageBarHost.getHeight();
                contentPanelHost.setPadding(new Insets(h, 0.0, 0.0, 0.0));
            }
        });
        
        documentAccordion.setExpandedPane(documentAccordion.getPanes().get(0));
        
        // Monitor the status of the document to set status icon accordingly in message bar
        getEditorController().getJobManager().revisionProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                messageBarController.setDocumentDirty(isDocumentDirty());
            }
        });
        
        // Setup title of the Library Reveal menu item according the underlying o/s.
        final String revealMenuKey;
        if (EditorPlatform.IS_MAC) {
            revealMenuKey = "menu.title.reveal.mac";
        } else if (EditorPlatform.IS_WINDOWS) {
            revealMenuKey = "menu.title.reveal.win";
        } else {
            assert EditorPlatform.IS_LINUX;
            revealMenuKey = "menu.title.reveal.linux";
        }
        libraryReveal.setText(I18N.getString(revealMenuKey));
        
        // We need to tune the content of the library menu according if there's
        // or not a selection likely to be dropped onto Library panel.
        libraryMenuButton.showingProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    AbstractSelectionGroup asg = getEditorController().getSelection().getGroup();
                    libraryImportSelection.setDisable(true);

                    if (asg != null && asg instanceof ObjectSelectionGroup) {
                        if (((ObjectSelectionGroup)asg).getItems().size() >= 1) {
                            libraryImportSelection.setDisable(false);
                        }
                    }
                    
                    // DTL-6439. The custom library menu shall be enabled only
                    // in the case there is a user library directory on disk.
                    Library lib = getEditorController().getLibrary();
                    if (lib instanceof UserLibrary) {
                        File userLibDir = new File(((UserLibrary)lib).getPath());
                        if (userLibDir.canRead()) {
                            customLibraryMenu.setDisable(false);
                        } else {
                            customLibraryMenu.setDisable(true);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void controllerDidCreateStage() {
        updateStageTitle();
        updateFromDocumentPreferences();
    }
    
    @Override
    public void openWindow() {
        
        if (getStage().isShowing() == false) {
            // Starts watching document:
            //      - editorController watches files referenced from the FXML text
            //      - watchingController watches the document file, i18n resources, 
            //        preview stylesheets...
            assert editorController.isFileWatchingStarted() == false;
            editorController.startFileWatching();
            watchingController.start();
        }
        
        super.openWindow();
        
        // Give focus to the library search TextField
        assert librarySearchController != null;
        librarySearchController.requestFocus();
    }
    
    @Override
    public void closeWindow() {
        
        super.closeWindow();
        
        // Stops watching
        editorController.stopFileWatching();
        watchingController.stop();
    }
    
    @Override 
    public void onCloseRequest(WindowEvent event) {
        performCloseAction();
    }

    public boolean isFrontDocumentWindow() {
        return getStage().isFocused()
                || (previewWindowController != null && previewWindowController.getStage().isFocused())
                || (skeletonWindowController != null && skeletonWindowController.getStage().isFocused())
                || (jarAnalysisReportController != null && jarAnalysisReportController.getStage().isFocused());
    }

    public void performCloseFrontDocumentWindow() {
        if (getStage().isFocused()) {
            performCloseAction();
        } else if (previewWindowController != null
                && previewWindowController.getStage().isFocused()) {
            previewWindowController.closeWindow();
        } else if (skeletonWindowController != null
                && skeletonWindowController.getStage().isFocused()) {
            skeletonWindowController.closeWindow();
        } else if (jarAnalysisReportController != null
                && jarAnalysisReportController.getStage().isFocused()) {
            jarAnalysisReportController.closeWindow();
        }
    }

    
    @Override
    protected void toolStylesheetDidChange(String oldStylesheet) {
        super.toolStylesheetDidChange(oldStylesheet);
        editorController.setToolStylesheet(getToolStylesheet());
        // previewWindowController should not be affected by tool style sheet
        if (skeletonWindowController != null) {
            skeletonWindowController.setToolStylesheet(getToolStylesheet());
        }
        if (jarAnalysisReportController != null) {
            jarAnalysisReportController.setToolStylesheet(getToolStylesheet());
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
        cssPanelSplitDefaultsMi.setDisable(true);
        cssPanelShowStyledOnlyMi.setDisable(true);
    }

    @FXML
    void onCssPanelViewTableAction(ActionEvent event) {
        cssPanelMenuController.viewTable();
        cssPanelSplitDefaultsMi.setDisable(false);
        cssPanelShowStyledOnlyMi.setDisable(false);
    }

    @FXML
    void onCssPanelViewTextAction(ActionEvent event) {
        cssPanelMenuController.viewText();
        cssPanelSplitDefaultsMi.setDisable(true);
        cssPanelShowStyledOnlyMi.setDisable(true);
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
        documentAccordion.setExpandedPane(documentAccordion.getPanes().get(0));
    }
    
    @FXML
    void onHierarchyShowFxId(ActionEvent event) {
        hierarchyPanelController.setDisplayOption(AbstractHierarchyPanelController.DisplayOption.FXID);
        documentAccordion.setExpandedPane(documentAccordion.getPanes().get(0));
    }
    
    @FXML
    void onHierarchyShowNodeId(ActionEvent event) {
        hierarchyPanelController.setDisplayOption(AbstractHierarchyPanelController.DisplayOption.NODEID);
        documentAccordion.setExpandedPane(documentAccordion.getPanes().get(0));
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
        if (libraryPanelController.getDisplayMode() != LibraryPanelController.DISPLAY_MODE.SEARCH) {
            libraryPanelController.setDisplayMode(LibraryPanelController.DISPLAY_MODE.LIST);
        } else {
            libraryPanelController.setPreviousDisplayMode(LibraryPanelController.DISPLAY_MODE.LIST);
        }
    }
    
    @FXML
    void onLibraryViewAsSections(ActionEvent event) {
        if (libraryPanelController.getDisplayMode() != LibraryPanelController.DISPLAY_MODE.SEARCH) {
            libraryPanelController.setDisplayMode(LibraryPanelController.DISPLAY_MODE.SECTIONS);
        } else {
            libraryPanelController.setPreviousDisplayMode(LibraryPanelController.DISPLAY_MODE.SECTIONS);
        }
    }

    // This method cannot be called if there is not a valid selection, a selection
    // eligible for being dropped onto Library panel.
    @FXML
    void onLibraryImportSelection(ActionEvent event) {
        AbstractSelectionGroup asg = getEditorController().getSelection().getGroup();

        if (asg instanceof ObjectSelectionGroup) {
            ObjectSelectionGroup osg = (ObjectSelectionGroup)asg;
            assert osg.getItems().isEmpty() == false;
            List<FXOMObject> selection = new ArrayList<>(osg.getItems());
            libraryPanelController.performImportSelection(selection);
        }
    }
    
    @FXML
    void onLibraryRevealCustomFolder(ActionEvent event) {
        String userLibraryPath = ((UserLibrary) getEditorController().getLibrary()).getPath();
        try {
            EditorPlatform.revealInFileBrowser(new File(userLibraryPath));
        } catch(IOException x) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setMessage(I18N.getString("alert.reveal.failure.message", getStage().getTitle()));
            errorDialog.setDetails(I18N.getString("alert.reveal.failure.details"));
            errorDialog.setDebugInfoWithThrowable(x);
            errorDialog.showAndWait();
        }
    }
    
    @FXML
    void onLibraryShowJarAnalysisReport(ActionEvent event) {
        if (jarAnalysisReportController == null) {
            jarAnalysisReportController = new JarAnalysisReportController(getEditorController(), getStage());
            jarAnalysisReportController.setToolStylesheet(getToolStylesheet());
        }
        
        jarAnalysisReportController.openWindow();
    }
    
    /*
     * Private
     */

    private boolean canPerformSelectAll() {
        final boolean result;
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isPopupEditing(focusOwner)) {
            return false;
        } else if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            final String text = tic.getText();
            final String selectedText = tic.getSelectedText();
            if (text == null || text.isEmpty()) {
                result = false;
            } else {
                // Check if the TextInputControl is not already ALL selected
                result = selectedText == null
                        || selectedText.length() < tic.getText().length();
            }
        } else {
            result = getEditorController().canPerformControlAction(ControlAction.SELECT_ALL);
        }
        return result;
    }

    private void performSelectAll() {
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            tic.selectAll();
        } else {
            this.getEditorController().performControlAction(ControlAction.SELECT_ALL);
        }
    }

    private boolean canPerformSelectNone() {
        boolean result;
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isPopupEditing(focusOwner)) {
            return false;
        } else if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            result = tic.getSelectedText() != null && tic.getSelectedText().isEmpty() == false;
        } else {
            result = getEditorController().canPerformControlAction(ControlAction.SELECT_NONE);
        }
        return result;
    }

    private void performSelectNone() {
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            tic.deselect();
        } else {
            this.getEditorController().performControlAction(ControlAction.SELECT_NONE);
        }
    }
    
    private boolean canPerformCopy() {
        boolean result;
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isPopupEditing(focusOwner)) {
            return false;
        } else if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            result = tic.getSelectedText() != null && tic.getSelectedText().isEmpty() == false;
        } else if (isCssRulesEditing(focusOwner) || isCssTextEditing(focusOwner)) {
            result = true;
        } else {
            result = getEditorController().canPerformControlAction(ControlAction.COPY);
        }
        return result;
    }

    private void performCopy() {
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            tic.copy();
        } else if (isCssRulesEditing(focusOwner)) {
            cssPanelController.copyRules();
        } else if (isCssTextEditing(focusOwner)) {
            // CSS text pane is a WebView
            // Let the WebView handle the copy action natively
        } else {
            this.getEditorController().performControlAction(ControlAction.COPY);
        }
    }

    private boolean canPerformCut() {
        boolean result;
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isPopupEditing(focusOwner)) {
            return false;
        } else if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            result = tic.getSelectedText() != null && tic.getSelectedText().isEmpty() == false;
        } else {
            result = getEditorController().canPerformEditAction(EditAction.CUT);
        }
        return result;
    }
    
    private void performCut() {
        final Node focusOwner = this.getScene().getFocusOwner();
        if (isTextInputControlEditing(focusOwner)) {
            final TextInputControl tic = getTextInputControl(focusOwner);
            tic.cut();
        } else {
            this.getEditorController().performEditAction(EditAction.CUT);
        }
    }

    private boolean canPerformPaste() {
        boolean result;
        final Node focusOwner = this.getScene().getFocusOwner();
        // If there is FXML in the clipboard, we paste the FXML whatever the focus owner is
        if (getEditorController().canPerformEditAction(EditAction.PASTE)) {
            result = true;
        } else if (isTextInputControlEditing(focusOwner)) {
            result = Clipboard.getSystemClipboard().hasString();
        } else {
            result = false;
        }
        return result;
    }
    
    private void performPaste() {
        final Node focusOwner = this.getScene().getFocusOwner();
        // If there is FXML in the clipboard, we paste the FXML whatever the focus owner is
        if (getEditorController().canPerformEditAction(EditAction.PASTE)) {
            this.getEditorController().performEditAction(EditAction.PASTE);
            // Give focus to content panel
            contentPanelController.getGlassLayer().requestFocus();
        } else {
            assert isTextInputControlEditing(focusOwner);
            final TextInputControl tic = getTextInputControl(focusOwner);
            tic.paste();
        }
    }

    private void performDelete() {
        
        // Collects all the selected objects
        final List<FXOMObject> selectedObjects = new ArrayList<>();
        final Selection selection = editorController.getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            selectedObjects.addAll(osg.getItems());
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
            selectedObjects.addAll(gsg.collectSelectedObjects());
        } else {
            assert false;
        }
        
        // Collects fx:ids in selected objects and their descendants
        final Map<String, FXOMObject> fxIdMap = new HashMap<>();
        for (FXOMObject selectedObject : selectedObjects) {
            fxIdMap.putAll(selectedObject.collectFxIds());
        }
        
        // Checks if deleted objects have some fx:ids and ask for confirmation.
        final boolean deleteConfirmed;
        if (fxIdMap.isEmpty()) {
            deleteConfirmed = true;
        } else {
            final String message;
            
            if (fxIdMap.size() == 1) {
                if (selectedObjects.size() == 1) {
                    message = I18N.getString("alert.delete.fxid1of1.message");
                } else {
                    message = I18N.getString("alert.delete.fxid1ofN.message");
                }
            } else {
                if (selectedObjects.size() == fxIdMap.size()) {
                    message = I18N.getString("alert.delete.fxidNofN.message");
                } else {
                    message = I18N.getString("alert.delete.fxidKofN.message");
                }
            }
            
            final AlertDialog d = new AlertDialog(getStage());
            d.setMessage(message);
            d.setDetails(I18N.getString("alert.delete.fxid.details"));
            d.setOKButtonTitle(I18N.getString("label.delete"));
            
            deleteConfirmed = (d.showAndWait() == AbstractModalDialog.ButtonID.OK);
        }
        
        if (deleteConfirmed) {
            editorController.performEditAction(EditAction.DELETE);
        }
    }

    private void performImportFxml() {

        final FileChooser fileChooser = new FileChooser();
        final ExtensionFilter f
                = new ExtensionFilter(I18N.getString("file.filter.label.fxml"),
                        "*.fxml"); //NOI18N
        fileChooser.getExtensionFilters().add(f);

        File fxmlFile = fileChooser.showOpenDialog(getStage());
        if (fxmlFile != null) {
            // See DTL-5948: on Linux we anticipate an extension less path.
            final String path = fxmlFile.getPath();
            if (!path.endsWith(".fxml")) { //NOI18N
                fxmlFile = new File(path + ".fxml"); //NOI18N
            }

            this.getEditorController().performImportFxml(fxmlFile);
        }
    }

    private void performImportMedia() {

        final FileChooser fileChooser = new FileChooser();
        final ExtensionFilter imageFilter
                = new ExtensionFilter(I18N.getString("file.filter.label.image"),
                        getImageExtensions());
        final ExtensionFilter audioFilter
                = new ExtensionFilter(I18N.getString("file.filter.label.audio"),
                        getAudioExtensions());
        final ExtensionFilter videoFilter
                = new ExtensionFilter(I18N.getString("file.filter.label.video"),
                        getVideoExtensions());
        final ExtensionFilter mediaFilter
                = new ExtensionFilter(I18N.getString("file.filter.label.media"),
                        getMediaExtensions());
        
        fileChooser.getExtensionFilters().add(mediaFilter);
        fileChooser.getExtensionFilters().add(imageFilter);
        fileChooser.getExtensionFilters().add(audioFilter);
        fileChooser.getExtensionFilters().add(videoFilter);

        File mediaFile = fileChooser.showOpenDialog(getStage());
        if (mediaFile != null) {
            this.getEditorController().performImportMedia(mediaFile);
        }
    }
    
    private static synchronized List<String> getImageExtensions() {
        if (imageExtensions == null) {
            imageExtensions = new ArrayList<>();
            imageExtensions.add("*.jpg"); //NOI18N
            imageExtensions.add("*.jpeg"); //NOI18N
            imageExtensions.add("*.png"); //NOI18N
            imageExtensions.add("*.gif"); //NOI18N
            imageExtensions = Collections.unmodifiableList(imageExtensions);
        }
        return imageExtensions;
    }

    private static synchronized List<String> getAudioExtensions() {
        if (audioExtensions == null) {
            audioExtensions = new ArrayList<>();
            audioExtensions.add("*.aif"); //NOI18N
            audioExtensions.add("*.aiff"); //NOI18N
            audioExtensions.add("*.mp3"); //NOI18N
            audioExtensions.add("*.m4a"); //NOI18N
            audioExtensions.add("*.wav"); //NOI18N
            audioExtensions.add("*.m3u"); //NOI18N
            audioExtensions.add("*.m3u8"); //NOI18N
            audioExtensions = Collections.unmodifiableList(audioExtensions);
        }
        return audioExtensions;
    }

    private static synchronized List<String> getVideoExtensions() {
        if (videoExtensions == null) {
            videoExtensions = new ArrayList<>();
            videoExtensions.add("*.flv"); //NOI18N
            videoExtensions.add("*.fxm"); //NOI18N
            videoExtensions.add("*.mp4"); //NOI18N
            videoExtensions.add("*.m4v"); //NOI18N
            videoExtensions = Collections.unmodifiableList(videoExtensions);
        }
        return videoExtensions;
    }

    private static synchronized List<String> getMediaExtensions() {
        if (mediaExtensions == null) {
            mediaExtensions = new ArrayList<>();
            mediaExtensions.addAll(getImageExtensions());
            mediaExtensions.addAll(getAudioExtensions());
            mediaExtensions.addAll(getVideoExtensions());
            mediaExtensions = Collections.unmodifiableList(mediaExtensions);
        }
        return mediaExtensions;
    }
    
    /**
     * Returns true if the specified node is part of the main scene and is
     * either a TextInputControl or a ComboBox.
     * 
     * @param node the focused node of the main scene
     * @return 
     */
    private boolean isTextInputControlEditing(Node node) {
        return (node instanceof TextInputControl
                || node instanceof ComboBox);
    }

    private TextInputControl getTextInputControl(Node node) {
        assert isTextInputControlEditing(node);
        final TextInputControl tic;
        if (node instanceof TextInputControl) {
            tic = (TextInputControl) node;
        } else {
            assert node instanceof ComboBox;
            final ComboBox<?> cb = (ComboBox<?>) node;
            tic = cb.getEditor();
        }
        return tic;
    }
    
    /**
     * Returns true if we are editing within a popup window :
     * either the specified node is showing a popup window
     * or the inline editing popup is showing.
     *
     * @param node the focused node of the main scene
     * @return
     */
    private boolean isPopupEditing(Node node) {
        return (node instanceof MenuButton && ((MenuButton) node).isShowing())
                || editorController.getInlineEditController().isWindowOpened();
    }
    
    private boolean isCssRulesEditing(Node node) {
        final Node cssRules = cssPanelController.getRulesPane();
        if (cssRules != null) {
            return isDescendantOf(cssRules, node);
        }
        return false;
    }

    private boolean isCssTextEditing(Node node) {
        final Node cssText = cssPanelController.getTextPane();
        if (cssText != null) {
            return isDescendantOf(cssText, node);
        }
        return false;
    }

    private boolean isDescendantOf(Node container, Node node) {
        Node child = node;
        while (child != null) {
            if (child == container) {
                return true;
            }
            child = child.getParent();
        }
        return false;
    }

    private KeyCombination getAccelerator(final KeyEvent event) {
        KeyCombination result = null;
        for (KeyCombination kc : menuBarController.getAccelerators()) {
            if (kc.match(event)) {
                result = kc;
                break;
            }
        }
        return result;
    }

    private void updateStageTitle() {
        if (libraryPanelHost != null) {
            getStage().setTitle(makeTitle(editorController.getFxomDocument()));
        } // else controllerDidLoadFxml() will invoke me again
    }
    
    private void updateFromDocumentPreferences() {
        if (libraryPanelHost != null) { // Layout is over
            // Refresh UI with preferences 
            final PreferencesController pc = PreferencesController.getSingleton();
            // Preferences global to the application
            final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
            recordGlobal.refresh(this);
            // Preferences specific to the document
            final PreferencesRecordDocument recordDocument = pc.getRecordDocument(this);
            recordDocument.readFromJavaPreferences();
            // Update UI accordingly
            recordDocument.refresh();
        }
    }
    
    private void resetDocumentPreferences() {
        final PreferencesController pc = PreferencesController.getSingleton();
        final PreferencesRecordDocument recordDocument = pc.getRecordDocument(this);
        recordDocument.resetDocumentPreferences();
    }
    
    private void performChooseBackgroundColor(Window owner) {
        final BackgroundColorDialogController bcdc 
                = new BackgroundColorDialogController(owner);
        bcdc.setToolStylesheet(getToolStylesheet());
        bcdc.openWindow();
    }
    
    ActionStatus performSaveOrSaveAsAction() {
        final ActionStatus result;
        
        if (editorController.getFxomDocument().getLocation() == null) {
            result = performSaveAsAction();
        } else {
            result = performSaveAction();
        }
        
        if (result.equals(ActionStatus.DONE)) {
            messageBarController.setDocumentDirty(false);
            saveJob = getEditorController().getJobManager().getCurrentJob();
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
        // First make the right panel visible if not already the case
        if (isRightPanelVisible() == false) {
            performControlAction(DocumentControlAction.TOGGLE_RIGHT_PANEL);
        }
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
                        watchingController.removeDocumentTarget();
                        final byte[] fxmlBytes = editorController.getFxmlText().getBytes("UTF-8"); //NOI18N
                        Files.write(fxmlPath, fxmlBytes);
                        updateLoadFileTime();
                        watchingController.update();

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
                boolean forgetSave = false;
                // It is only on Linux where you can get the case the path doesn't
                // end with the extension, thanks the behavior of the FX 8 FileChooser
                // on this specific OS (see RT-31956).
                // Below we ask the user if the extension shall be added or not.
                // See DTL-5948.
                final String path = fxmlFile.getPath();
                if (! path.endsWith(".fxml")) { //NOI18N
                    try {
                        URL alternateURL = new URL(fxmlFile.toURI().toURL().toExternalForm() + ".fxml"); //NOI18N
                        File alternateFxmlFile = new File(alternateURL.toURI());
                        final AlertDialog d = new AlertDialog(getStage());
                        d.setMessage(I18N.getString("alert.save.noextension.message", fxmlFile.getName()));
                        String details = I18N.getString("alert.save.noextension.details");

                        if (alternateFxmlFile.exists()) {
                            details += "\n" //NOI18N
                                    + I18N.getString("alert.save.noextension.details.overwrite", alternateFxmlFile.getName());
                        }

                        d.setDetails(details);
                        d.setOKButtonVisible(true);
                        d.setOKButtonTitle(I18N.getString("alert.save.noextension.savewith"));
                        d.setDefaultButtonID(ButtonID.OK);
                        d.setShowDefaultButton(true);
                        d.setActionButtonDisable(false);
                        d.setActionButtonVisible(true);
                        d.setActionButtonTitle(I18N.getString("alert.save.noextension.savewithout"));

                        switch (d.showAndWait()) {
                            case ACTION:
                                // Nothing to do, we save with the no extension name
                                break;
                            case CANCEL:
                                forgetSave = true;
                                break;
                            case OK:
                                fxmlFile = alternateFxmlFile;
                                break;
                        }
                    } catch (MalformedURLException | URISyntaxException ex) {
                        forgetSave = true;
                    }
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
                if (dwc != null && dwc != this) {
                    final Path fxmlPath = Paths.get(fxmlFile.toString());
                    final String fileName = fxmlPath.getFileName().toString();
                    final ErrorDialog d = new ErrorDialog(getStage());
                    d.setMessage(I18N.getString("alert.save.conflict.message", fileName));
                    d.setDetails(I18N.getString("alert.save.conflict.details"));
                    d.showAndWait();
                    result = ActionStatus.CANCELLED;
                } else if (forgetSave) {
                    result = ActionStatus.CANCELLED;
                } else {
                    // Recalculates references if needed
                    // TODO(elp)

                    // First change the location of the fxom document
                    editorController.setFxmlLocation(newLocation);
                    updateLoadFileTime();
                    updateStageTitle();
                    // We use same DocumentWindowController BUT we change its fxml :
                    // => reset document preferences
                    resetDocumentPreferences();
                    
                    watchingController.update();

                    // Now performs a regular save action
                    result = performSaveAction();
                    
                    // Keep track of the user choice for next time
                    SceneBuilderApp.getSingleton().updateNextInitialDirectory(fxmlFile);

                    // Update recent items with just saved file
                    final PreferencesController pc = PreferencesController.getSingleton();
                    final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
                    recordGlobal.addRecentItem(fxmlFile);
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
            try {
                reload();
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
        if (isDocumentDirty()) {
            
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
            
            // Write java preferences at close time
            final PreferencesController pc = PreferencesController.getSingleton();
            final PreferencesRecordDocument recordDocument = pc.getRecordDocument(this);
            recordDocument.writeToJavaPreferences();
        }
                
        return closeConfirmed ? ActionStatus.DONE : ActionStatus.CANCELLED;
    }
    
    
    private void performRevealAction() {
        assert editorController.getFxomDocument() != null;
        assert editorController.getFxomDocument().getLocation() != null;
        
        final URL location = editorController.getFxomDocument().getLocation();
        
        try {
            final File fxmlFile = new File(location.toURI());
            EditorPlatform.revealInFileBrowser(fxmlFile);
        } catch(IOException | URISyntaxException x) {
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

/**
 * This class setup key bindings for the TextInputControl type classes and
 * provide a way to access the key binding list.
 */
class SBTextInputControlBindings extends com.sun.javafx.scene.control.behavior.TextInputControlBindings {

    private SBTextInputControlBindings() {
        assert false;
    }

    public static List<KeyBinding> getBindings() {
        return BINDINGS;
    }
}
