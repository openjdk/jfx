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
package com.oracle.javafx.scenebuilder.kit.editor;

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.job.BringForwardJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.BringToFrontJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.CutSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.DeleteSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.DuplicateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.FitToParentSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.InsertAsSubComponentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.PasteIntoJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.PasteJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.SendBackwardJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.SendToBackJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.TrimSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.UseComputedSizesSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.AddColumnJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.AddRowJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.GridPaneJobUtils.Position;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.MoveColumnJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.MoveRowJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.AbstractWrapInJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.UnwrapJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInAnchorPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInGridPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInGroupJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInHBoxJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInScrollPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInSplitPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInStackPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInTabPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInTitledPaneJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInToolBarJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.wrap.WrapInVBoxJob;
import com.oracle.javafx.scenebuilder.kit.editor.messagelog.MessageLog;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReport;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.editor.util.ContextMenuController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.glossary.Glossary;
import com.oracle.javafx.scenebuilder.kit.glossary.BuiltinGlossary;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ClipboardEncoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableListValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.effect.Effect;
import javafx.scene.input.Clipboard;
import javafx.util.Callback;

/**
 * An editor controller is the central object which coordinates the editing
 * of an FXML document across the different panels (hierarchy, content,
 * inspector...). 
 * <p>
 * An editor controller is associated to an FXML document. It can perform
 * editing and control actions on this document. It also maintains the list of
 * objects selected by the user.
 * <p>
 * Some panel controllers can be attached to an editor controller. They listen
 * to the editor and update their content accordingly.
 */
public class EditorController {
    
    /**
     * An 'edit' action is an action which modifies the document associated
     * to this editor. It makes the document dirty and pushes a
     * new item on the undo/redo stack.
     */
    public enum EditAction {
        // Candidates for Edit menu
        CUT,
        PASTE,
        PASTE_INTO,
        DUPLICATE,
        DELETE,
        TRIM,
        TOGGLE_FX_ROOT,
        // Candidates for Modify menu
        FIT_TO_PARENT,
        USE_COMPUTED_SIZES,
        // Candidates for Modify/GridPane menu
        MOVE_ROW_ABOVE,
        MOVE_ROW_BELOW,
        MOVE_COLUMN_BEFORE,
        MOVE_COLUMN_AFTER,
        ADD_ROW_ABOVE,
        ADD_ROW_BELOW,
        ADD_COLUMN_BEFORE,
        ADD_COLUMN_AFTER,
        INCREASE_ROW_SPAN,
        DECREASE_ROW_SPAN,
        INCREASE_COLUMN_SPAN,
        DECREASE_COLUMN_SPAN,
        // Candidates for Arrange menu
        BRING_TO_FRONT,
        SEND_TO_BACK,
        BRING_FORWARD,
        SEND_BACKWARD,
        UNWRAP,
        WRAP_IN_ANCHOR_PANE,
        WRAP_IN_GRID_PANE,
        WRAP_IN_GROUP,
        WRAP_IN_HBOX,
        WRAP_IN_PANE,
        WRAP_IN_SCROLL_PANE,
        WRAP_IN_SPLIT_PANE,
        WRAP_IN_STACK_PANE,
        WRAP_IN_TAB_PANE,
        WRAP_IN_TITLED_PANE,
        WRAP_IN_TOOL_BAR,
        WRAP_IN_VBOX
    }
    
    /**
     * A 'control' action does not modify the document. It only changes a 
     * state or a mode in this editor.
     */
    public enum ControlAction {
        // Candidates for Edit menu
        COPY,
        SELECT_ALL,
        SELECT_NONE,
        SELECT_PARENT,
        SELECT_NEXT,
        SELECT_PREVIOUS,
        TOGGLE_CSS_SELECTION,
        TOGGLE_SAMPLE_DATA
    }
    
    private final Selection selection = new Selection();
    private final JobManager jobManager = new JobManager(this);
    private final MessageLog messageLog = new MessageLog();
    private final ErrorReport errorReport = new ErrorReport();
    private final DragController dragController = new DragController(this);
    private final InlineEditController inlineEditController = new InlineEditController(this);
    private final ContextMenuController contextMenuController = new ContextMenuController(this);
    
    private final ObjectProperty<FXOMDocument> fxomDocumentProperty 
            = new SimpleObjectProperty<>();
    private final ObjectProperty<URL> fxmlLocationProperty 
            = new SimpleObjectProperty<>();
    private final ObjectProperty<Library> libraryProperty 
            = new SimpleObjectProperty<>(BuiltinLibrary.getLibrary());
    private final ObjectProperty<Glossary> glossaryProperty 
            = new SimpleObjectProperty<>(new BuiltinGlossary());
    private final ObjectProperty<ResourceBundle> resourcesProperty
            = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Theme> themeProperty
            = new SimpleObjectProperty<>(Theme.MODENA);
    private final ListProperty<File> sceneStyleSheetProperty
            = new SimpleListProperty<>();
    private final ObjectProperty<File> resourceProperty
            = new SimpleObjectProperty<>(null);
    private final BooleanProperty pickModeEnabledProperty
            = new SimpleBooleanProperty(true);
    private final BooleanProperty sampleDataEnabledProperty
            = new SimpleBooleanProperty(false);
    
    private Callback<Void, Boolean> requestTextEditingSessionEnd;
    
    /**
     * Creates an empty editor controller (ie it has no associated fxom document).
     */
    public EditorController() {
        jobManager.revisionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                jobManagerRevisionDidChange();
            }
        });
    }
    
    
    /**
     * Sets the fxml content to be edited by this editor.
     * A null value makes this editor empty.
     * 
     * @param fxmlText null or the fxml text to be edited
     * @throws IOException if fxml text cannot be parsed and loaded correctly.
     */
    public void setFxmlText(String fxmlText) throws IOException {
        setFxmlTextAndLocation(fxmlText, getFxmlLocation());
    }
    
    /**
     * Returns null or the fxml content being edited by this editor.
     * 
     * @return null or the fxml content being edited by this editor.
     */
    public String getFxmlText() {
        final String result;
        
        if (getFxomDocument() == null) {
            result = null;
        } else {
            result = getFxomDocument().getFxmlText();
        }
        
        return result;
    }
    
    /**
     * Returns true if fxml content being edited can be returned safely.
     * This method will return false if there is a text editing session on-going.
     * 
     * @return true if fxml content being edited can be returned safely.
     */
    public boolean canGetFxmlText() {
        final boolean result;
        
        if (requestTextEditingSessionEnd == null) {
            result = true;
        } else {
            result = requestTextEditingSessionEnd.call(null);
            // If the callback returns true, then it should have call
            // textEditingSessionDidEnd() 
            // => requestTextEditingSessionEnd should be null
            assert (requestTextEditingSessionEnd == null) || (result == false);
        }
        
        return result;
    }
    
    /**
     * Tells this editor that a text editing session has started.
     * The editor controller may invoke the requestSessionEnd() callback 
     * if it needs the text editing session to stop. The callback should;
     *   - either stop the text editing session, invoke textEditingSessionDidEnd()
     *     and return true
     *   - either keep the text editing session on-going and return false
     * 
     * @param requestSessionEnd Callback that should end the text editing session or return false  
     */
    public void textEditingSessionDidBegin(Callback<Void, Boolean> requestSessionEnd) {
        assert requestTextEditingSessionEnd == null;
        requestTextEditingSessionEnd = requestSessionEnd;
    }
    
    
    /**
     * Tells this editor that the text editing session has ended.
     */
    public void textEditingSessionDidEnd() {
        assert requestTextEditingSessionEnd != null;
        requestTextEditingSessionEnd = null;
    }
    
    /*
     * Returns true if a text editing session is currently on going.
     */
    public boolean isTextEditingSessionOnGoing() {
        return requestTextEditingSessionEnd != null;
    }
    
    /**
     * Sets the location of the fxml being edited.
     * If null value is passed, fxml text is being interpreted with any location
     * (ie some references may be broken).
     * 
     * @param fxmlLocation null or the location of the fxml being edited.
     */
    public void setFxmlLocation(URL fxmlLocation) {
        fxmlLocationProperty.setValue(fxmlLocation);
        if (getFxomDocument() != null) {
            getFxomDocument().setLocation(fxmlLocation);
        }
    }
    
    /**
     * Returns the library used by this editor.
     * 
     * @return the library used by this editor (never null).
     */
    public Library getLibrary() {
        return libraryProperty.getValue();
    }
    
    /**
     * Sets the library used by this editor.
     * When this method is called, user scene graph is fully rebuilt using 
     * the new library and all panel refresh their contents.
     * 
     * @param library the library to be used by this editor (never null).
     */
    public void setLibrary(Library library) {
        assert library != null;
        libraryProperty.getValue().classLoaderProperty().removeListener(libraryClassLoaderListener);
        libraryProperty.setValue(library);
        libraryProperty.getValue().classLoaderProperty().addListener(libraryClassLoaderListener);
        libraryClassLoaderDidChange();
    }
    
    /**
     * The property holding the library used by this editor.
     * 
     * @return the property holding the library used by this editor (never null).
     */
    public ObservableValue<Library> libraryProperty() {
        return libraryProperty;
    }
    
    /**
     * Returns the glossary used by this editor.
     * 
     * @return the glossary used by this editor (never null).
     */
    public Glossary getGlossary() {
        return glossaryProperty.getValue();
    }
    
    /**
     * Sets the glossary used by this editor.
     * The Inspector panel(s) connected to this editor will update
     * their suggested lists in Code section.
     * 
     * @param glossary the glossary to be used by this editor (never null).
     */
    public void setLibrary(Glossary glossary) {
        assert glossary != null;
        glossaryProperty.setValue(glossary);
    }
    
    /**
     * The property holding the glossary used by this editor.
     * 
     * @return the property holding the glossary used by this editor (never null).
     */
    public ObservableValue<Glossary> glossaryProperty() {
        return glossaryProperty;
    }
    
    /**
     * Returns the resource bundle used by this editor.
     * 
     * @return  the resource bundle used by this editor.
     */
    public ResourceBundle getResources() {
        return resourcesProperty.getValue();
    }
    
    /**
     * Sets the resource bundle used by this editor.
     * Content and Preview panels sharing this editor will update
     * their content to use this new theme.
     * 
     * @param resources null of the resource bundle to be used by this editor.
     */
    public void setResources(ResourceBundle resources) {
        resourcesProperty.setValue(resources);
        resourcesDidChange();
    }
    
    /**
     * The property holding the resource bundle used by this editor.
     * 
     * @return the property holding the resource bundle used by this editor (never null).
     */
    public ObservableValue<ResourceBundle> resourcesProperty() {
        return resourcesProperty;
    }
    
    /**
     * Returns the theme used by this editor.
     * 
     * @return the theme used by this editor.
     */
    public Theme getTheme() {
        return themeProperty.getValue();
    }
    
    /**
     * Sets the theme used by this editor.
     * Content and Preview panels sharing this editor will update
     * their content to use this new theme.
     * 
     * @param theme the theme to be used by this editor
     */
    public void setTheme(Theme theme) {
        themeProperty.setValue(theme);
    }
    
    /**
     * The property holding the theme used by this editor.
     * 
     * @return the property holding the theme associated to the editor (never null).
     */
    public ObservableValue<Theme> themeProperty() {
        return themeProperty;
    }
    
    /**
     * 
     * @return the list of scene style sheet used by this editor
     */
    public ObservableList<File> getSceneStyleSheets() {
        return sceneStyleSheetProperty.getValue();
    }
    
    /**
     * 
     * @param styleSheets the list of scene style sheet to be used by this editor
     */
    public void setSceneStyleSheets(ObservableList<File> styleSheets) {
        sceneStyleSheetProperty.setValue(styleSheets);
    }
    
    /**
     * The property holding the list of scene style sheet used by this editor.
     * 
     * @return the property holding the set of scene style sheet used by the editor,
     * or null if has not been set.
     */
    public ObservableListValue<File> sceneStyleSheetProperty() {
        return sceneStyleSheetProperty;
    }
    
    /**
     * 
     * @return the resource used by this editor.
     */
    public File getResource() {
        return resourceProperty.getValue();
    }
    
    /**
     * The resource bundle is provided as a property file.
     * 
     * @param resource the resource bundle to be used by this editor.
     */
    public void setResource(File resource) {
        resourceProperty.setValue(resource);
    }
    
    /**
     * The property holding the resource used by this editor.
     * 
     * @return the property holding the resource used by the editor,
     * or null if has not been set.
     */
    public ObservableValue<File> resourceProperty() {
        return resourceProperty;
    }
    
    /**
     * Returns true if 'pick mode' is enabled for this editor.
     * @return true if 'pick mode' is enabled for this editor.
     */
    public boolean isPickModeEnabled() {
        return pickModeEnabledProperty.getValue();
    }
    
    /**
     * Enables or disables 'pick mode' on this editor.
     * 
     * @param pickModeEnabled true if 'pick mode' should be enabled.
     */
    public void setPickModeEnabled(boolean pickModeEnabled) {
        pickModeEnabledProperty.setValue(pickModeEnabled);
    }
    
    /**
     * The property indicating if 'pick mode' is enabled or not.
     * 
     * @return the property indicating if 'pick mode' is enabled or not.
     */
    public ObservableValue<Boolean> pickModeEnabledProperty() {
        return pickModeEnabledProperty;
    }
    
    /**
     * Returns true if content and preview panels attached to this editor
     * should display sample data.
     * 
     * @return true if content and preview panels should display sample data.
     */
    public boolean isSampleDataEnabled() {
        return sampleDataEnabledProperty.getValue();
    }
    
    /**
     * Enables or disables display of sample data in content and preview panels
     * attached to this editor.
     * 
     * @param sampleDataEnabled true if sample data should be displayed
     */
    public void setSampleDataEnabled(boolean sampleDataEnabled) {
        sampleDataEnabledProperty.setValue(sampleDataEnabled);
        if (getFxomDocument() != null) {
            getFxomDocument().setSampleDataEnabled(isSampleDataEnabled());
        }
    }
    
    /**
     * The property indicating if sample data should be displayed or not.
     * 
     * @return the property indicating if sample data should be displayed or not.
     */
    public ObservableValue<Boolean> sampleDataEnabledProperty() {
        return sampleDataEnabledProperty;
    }
    
    
    /**
     * Returns null or the location of the fxml being edited.
     * 
     * @return null or the location of the fxml being edited.
     */
    public URL getFxmlLocation() {
        return fxmlLocationProperty.getValue();
    }
    
    /**
     * Sets both fxml text and location to be edited by this editor.
     * Performs setFxmlText() and setFxmlLocation() but in a optimized manner
     * (it avoids an extra scene graph refresh).
     * 
     * @param fxmlText null or the fxml text to be edited
     * @param fxmlLocation null or the location of the fxml text being edited
     * @throws IOException if fxml text cannot be parsed and loaded correctly.
     */
    public void setFxmlTextAndLocation(String fxmlText, URL fxmlLocation) throws IOException {
        updateFxomDocument(fxmlText, fxmlLocation, getResources());
        this.fxmlLocationProperty.setValue(fxmlLocation);
    }
    
    /**
     * Sets fxml text, location and resources to be edited by this editor.
     * Performs setFxmlText(), setFxmlLocation() and setResources() but in an
     * optimized manner (it avoids extra scene graph refresh).
     * 
     * @param fxmlText null or the fxml text to be edited
     * @param fxmlLocation null or the location of the fxml text being edited
     * @param resources null or the resource bundle used to load the fxml text
     * @throws IOException if fxml text cannot be parsed and loaded correctly.
     */
    public void setFxmlTextLocationAndResources(String fxmlText, URL fxmlLocation,
            ResourceBundle resources) throws IOException {
        updateFxomDocument(fxmlText, fxmlLocation, resources);
        this.fxmlLocationProperty.setValue(fxmlLocation);
    }
    
    /**
     * The property holding the document associated to this editor.
     * @return the property holding the document associated to this editor.
     */
    public ObservableValue<FXOMDocument> fxomDocumentProperty() {
        return fxomDocumentProperty;
    }
    
    /**
     * Returns the document associated to this editor.
     * 
     * @return the document associated to this editor.
     */
    public FXOMDocument getFxomDocument() {
        return fxomDocumentProperty.getValue();
    }

    /**
     * @treatAsPrivate Returns the selection associated to this editor.
     * 
     * @return  the selection associated to this editor.
     */
    public Selection getSelection() {
        return selection;
    }
    
    
    /**
     * @treatAsPrivate Returns the job manager associated to this editor.
     * 
     * @return  the job manager associated to this editor.
     */
    public JobManager getJobManager() {
        return jobManager;
    }
    
    /**
     * @treatAsPrivate Returns the message log associated to this editor.
     * 
     * @return  the message log associated to this editor.
     */
    public MessageLog getMessageLog() {
        return messageLog;
    }
    
    /**
     * @treatAsPrivate Returns the error report associated to this editor.
     * 
     * @return  the error report associated to this editor.
     */
    public ErrorReport getErrorReport() {
        return errorReport;
    }

    /**
     * @treatAsPrivate Returns the drag controller associated to this editor.
     * 
     * @return the drag controller associated to this editor.
     */
    public DragController getDragController() {
        return dragController;
    }
    
    /**
     * @treatAsPrivate Returns the inline edit controller associated to this editor.
     * 
     * @return the inline edit controller associated to this editor.
     */
    public InlineEditController getInlineEditController() {
        return inlineEditController;
    }
    
    /**
     * @treatAsPrivate Returns the context menu controller associated to this editor.
     * 
     * @return the context menu controller associated to this editor.
     */
    public ContextMenuController getContextMenuController() {
        return contextMenuController;
    }

    /**
     * Returns true if the undo action is permitted (ie there is something
     * to be undone).
     * 
     * @return true if the undo action is permitted.
     */
    public boolean canUndo() {
        return jobManager.canUndo();
    }
    
    /**
     * Returns null or the description of the action to be undone.
     * 
     * @return null or the description of the action to be undone.
     */
    public String getUndoDescription() {
        return jobManager.getUndoDescription();
    }
    
    /**
     * Performs the undo action.
     */
    public void undo() {
        jobManager.undo();
        assert getFxomDocument().isUpdateOnGoing() == false;
    }
    
    /**
     * Returns true if the redo action is permitted (ie there is something
     * to be redone).
     * 
     * @return true if the redo action is permitted.
     */
    public boolean canRedo() {
        return jobManager.canRedo();
    }
    
    /**
     * Returns null or the description of the action to be redone.
     * 
     * @return null or the description of the action to be redone.
     */
    public String getRedoDescription() {
        return jobManager.getRedoDescription();
    }
    
    /**
     * Performs the redo action.
     */
    public void redo() {
        jobManager.redo();
        assert getFxomDocument().isUpdateOnGoing() == false;
    }
    
    /**
     * Clears the undo/redo stack of this editor controller.
     */
    public void clearUndoRedo() {
        jobManager.clear();
    }
    
    /**
     * Performs an edit action.
     * 
     * @param editAction the edit action to be performed.
     */
    public void performEditAction(EditAction editAction) {
        switch(editAction) {
            case ADD_COLUMN_BEFORE: {
                final AddColumnJob job = new AddColumnJob(this, Position.BEFORE);
                jobManager.push(job);
                break;
            }
            case ADD_COLUMN_AFTER: {
                final AddColumnJob job = new AddColumnJob(this, Position.AFTER);
                jobManager.push(job);
                break;
            }
            case ADD_ROW_ABOVE: {
                final AddRowJob job = new AddRowJob(this, Position.ABOVE);
                jobManager.push(job);
                break;
            }
            case ADD_ROW_BELOW: {
                final AddRowJob job = new AddRowJob(this, Position.BELOW);
                jobManager.push(job);
                break;
            }
            case BRING_FORWARD: {
                final BringForwardJob job = new BringForwardJob(this);
                jobManager.push(job);
                break;
            }
            case BRING_TO_FRONT: {
                final BringToFrontJob job = new BringToFrontJob(this);
                jobManager.push(job);
                break;
            }
            case CUT: {
                final CutSelectionJob job = new CutSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case DELETE: {
                final DeleteSelectionJob job = new DeleteSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case DUPLICATE: {
                final DuplicateSelectionJob job = new DuplicateSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case FIT_TO_PARENT: {
                final FitToParentSelectionJob job
                        = new FitToParentSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case MOVE_COLUMN_BEFORE: {
                final MoveColumnJob job = new MoveColumnJob(this, Position.BEFORE);
                jobManager.push(job);
                break;
            }
            case MOVE_COLUMN_AFTER: {
                final MoveColumnJob job = new MoveColumnJob(this, Position.AFTER);
                jobManager.push(job);
                break;
            }
            case MOVE_ROW_ABOVE: {
                final MoveRowJob job = new MoveRowJob(this, Position.ABOVE);
                jobManager.push(job);
                break;
            }
            case MOVE_ROW_BELOW: {
                final MoveRowJob job = new MoveRowJob(this, Position.BELOW);
                jobManager.push(job);
                break;
            }
            case PASTE: {
                final PasteJob job = new PasteJob(this);
                jobManager.push(job);
                break;
            }
            case PASTE_INTO: {
                final PasteIntoJob job = new PasteIntoJob(this);
                jobManager.push(job);
                break;
            }
            case SEND_BACKWARD: {
                final SendBackwardJob job = new SendBackwardJob(this);
                jobManager.push(job);
                break;
            }
            case SEND_TO_BACK: {
                final SendToBackJob job = new SendToBackJob(this);
                jobManager.push(job);
                break;
            }
            case UNWRAP: {
                final UnwrapJob job = new UnwrapJob(this);
                jobManager.push(job);
                break;
            }
            case USE_COMPUTED_SIZES: {
                final UseComputedSizesSelectionJob job
                        = new UseComputedSizesSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case TRIM: {
                final TrimSelectionJob job = new TrimSelectionJob(this);
                jobManager.push(job);
                break;
            }
            case WRAP_IN_ANCHOR_PANE: {
                performWrap(javafx.scene.layout.AnchorPane.class);
                break;
            }
            case WRAP_IN_GRID_PANE: {
                performWrap(javafx.scene.layout.GridPane.class);
                break;
            }
            case WRAP_IN_GROUP: {
                performWrap(javafx.scene.Group.class);
                break;
            }
            case WRAP_IN_HBOX: {
                performWrap(javafx.scene.layout.HBox.class);
                break;
            }
            case WRAP_IN_PANE: {
                performWrap(javafx.scene.layout.Pane.class);
                break;
            }
            case WRAP_IN_SCROLL_PANE: {
                performWrap(javafx.scene.control.ScrollPane.class);
                break;
            }
            case WRAP_IN_SPLIT_PANE: {
                performWrap(javafx.scene.control.SplitPane.class);
                break;
            }
            case WRAP_IN_STACK_PANE: {
                performWrap(javafx.scene.layout.StackPane.class);
                break;
            }
            case WRAP_IN_TAB_PANE: {
                performWrap(javafx.scene.control.TabPane.class);
                break;
            }
            case WRAP_IN_TITLED_PANE: {
                performWrap(javafx.scene.control.TitledPane.class);
                break;
            }
            case WRAP_IN_TOOL_BAR: {
                performWrap(javafx.scene.control.ToolBar.class);
                break;
            }
            case WRAP_IN_VBOX: {
                performWrap(javafx.scene.layout.VBox.class);
                break;
            }
            default:
                throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
        }
        assert getFxomDocument().isUpdateOnGoing() == false;
    }
    
    /**
     * Returns true if the specified edit action is permitted.
     * 
     * @param editAction the edit action to be tested.
     * @return true if the specified edit action is permitted.
     */
    public boolean canPerformEditAction(EditAction editAction) {
        final boolean result;
        switch(editAction) {
            case ADD_COLUMN_BEFORE: {
                final AddColumnJob job = new AddColumnJob(this, Position.BEFORE);
                result = job.isExecutable();
                break;
            }
            case ADD_COLUMN_AFTER: {
                final AddColumnJob job = new AddColumnJob(this, Position.AFTER);
                result = job.isExecutable();
                break;
            }
            case ADD_ROW_ABOVE: {
                final AddRowJob job = new AddRowJob(this, Position.ABOVE);
                result = job.isExecutable();
                break;
            }
            case ADD_ROW_BELOW: {
                final AddRowJob job = new AddRowJob(this, Position.BELOW);
                result = job.isExecutable();
                break;
            }
            case BRING_FORWARD: {
                final BringForwardJob job = new BringForwardJob(this);
                result = job.isExecutable();
                break;
            }
            case BRING_TO_FRONT: {
                final BringToFrontJob job = new BringToFrontJob(this);
                result = job.isExecutable();
                break;
            }
            case CUT: {
                final CutSelectionJob job = new CutSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case DELETE: {
                final DeleteSelectionJob job = new DeleteSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case DUPLICATE: {
                final DuplicateSelectionJob job = new DuplicateSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case FIT_TO_PARENT: {
                final FitToParentSelectionJob job
                        = new FitToParentSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case MOVE_COLUMN_BEFORE: {
                final MoveColumnJob job = new MoveColumnJob(this, Position.BEFORE);
                result = job.isExecutable();
                break;
            }
            case MOVE_COLUMN_AFTER: {
                final MoveColumnJob job = new MoveColumnJob(this, Position.AFTER);
                result = job.isExecutable();
                break;
            }
            case MOVE_ROW_ABOVE: {
                final MoveRowJob job = new MoveRowJob(this, Position.ABOVE);
                result = job.isExecutable();
                break;
            }
            case MOVE_ROW_BELOW: {
                final MoveRowJob job = new MoveRowJob(this, Position.BELOW);
                result = job.isExecutable();
                break;
            }
            case PASTE: {
                final PasteJob job = new PasteJob(this);
                result = job.isExecutable();
                break;
            }
            case PASTE_INTO: {
                final PasteIntoJob job = new PasteIntoJob(this);
                result = job.isExecutable();
                break;
            }
            case SEND_BACKWARD: {
                final SendBackwardJob job = new SendBackwardJob(this);
                result = job.isExecutable();
                break;
            }
            case SEND_TO_BACK: {
                final SendToBackJob job = new SendToBackJob(this);
                result = job.isExecutable();
                break;
            }
            case UNWRAP: {
                final UnwrapJob job = new UnwrapJob(this);
                result = job.isExecutable();
                break;
            }
            case USE_COMPUTED_SIZES: {
                final UseComputedSizesSelectionJob job 
                        = new UseComputedSizesSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case TRIM: {
                final TrimSelectionJob job = new TrimSelectionJob(this);
                result = job.isExecutable();
                break;
            }
            case WRAP_IN_ANCHOR_PANE: {
                result = canPerformWrap(javafx.scene.layout.AnchorPane.class);
                break;
            }
            case WRAP_IN_GRID_PANE: {
                result = canPerformWrap(javafx.scene.layout.GridPane.class);
                break;
            }
            case WRAP_IN_GROUP: {
                result = canPerformWrap(javafx.scene.Group.class);
                break;
            }
            case WRAP_IN_HBOX: {
                result = canPerformWrap(javafx.scene.layout.HBox.class);
                break;
            }
            case WRAP_IN_PANE: {
                result = canPerformWrap(javafx.scene.layout.Pane.class);
                break;
            }
            case WRAP_IN_SCROLL_PANE: {
                result = canPerformWrap(javafx.scene.control.ScrollPane.class);
                break;
            }
            case WRAP_IN_SPLIT_PANE: {
                result = canPerformWrap(javafx.scene.control.SplitPane.class);
                break;
            }
            case WRAP_IN_STACK_PANE: {
                result = canPerformWrap(javafx.scene.layout.StackPane.class);
                break;
            }
            case WRAP_IN_TAB_PANE: {
                result = canPerformWrap(javafx.scene.control.TabPane.class);
                break;
            }
            case WRAP_IN_TITLED_PANE: {
                result = canPerformWrap(javafx.scene.control.TitledPane.class);
                break;
            }
            case WRAP_IN_TOOL_BAR: {
                result = canPerformWrap(javafx.scene.control.ToolBar.class);
                break;
            }
            case WRAP_IN_VBOX: {
                result = canPerformWrap(javafx.scene.layout.VBox.class);
                break;
            }
            default:
                result = false;
                break;
        }
        
        return result;
    }
    
    /**
     * Performs the specified control action.
     * 
     * @param controlAction the control action to be performed.
     */
    public void performControlAction(ControlAction controlAction) {
        switch(controlAction) {
            case COPY: {
                performCopy();
                break;
            }
            case SELECT_ALL: {
                performSelectAll();
                break;
            }
            case SELECT_NONE: {
                performSelectNone();
                break;
            }
            case SELECT_PARENT: {
                performSelectParent();
                break;
            }
            case SELECT_NEXT: {
                performSelectNext();
                break;
            }
            case SELECT_PREVIOUS: {
                performSelectPrevious();
                break;
            }
            case TOGGLE_SAMPLE_DATA: {
                setSampleDataEnabled( ! isSampleDataEnabled());
                break;
            }
            default:
                throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
        }
    }
    
    /**
     * Returns true if the specified control action is permitted.
     * 
     * @param controlAction the control action to be tested.
     * @return true if the specified control action is permitted.
     */
    public boolean canPerformControlAction(ControlAction controlAction) {
        final boolean result;

        // If there is no document loaded, we cannot perform control actions
        if (getFxomDocument() == null || getFxomDocument().getFxomRoot() == null) {
            return false;
        }
        switch(controlAction) {
            case COPY: {
                result = canPerformCopy();
                break;
            }
            case SELECT_ALL: {
                result = canPerformSelectAll();
                break;
            }
            case SELECT_NONE: {
                result = canPerformSelectNone();
                break;
            }
            case SELECT_PARENT: {
                result = canPerformSelectParent();
                break;
            }
            case SELECT_NEXT: {
                result = canPerformSelectNext();
                break;
            }
            case SELECT_PREVIOUS: {
                result = canPerformSelectPrevious();
                break;
            }
            case TOGGLE_SAMPLE_DATA: {
                result = true;
                break;
            }
            default:
                result = false;
                break;
        }
        
        return result;
    }
    
    /**
     * Performs the 'insert' edit action. This action creates an object
     * matching the specified library item and insert it in the document
     * (according the selection state).
     * 
     * @param libraryItem the library item describing the object to be inserted.
     */
    public void performInsert(LibraryItem libraryItem) {
        final FXOMObject target;
        
        assert canPerformInsert(libraryItem); // (1)
        
        final FXOMObject rootObject = getFxomDocument().getFxomRoot();
        if (selection.isEmpty() || selection.isSelected(rootObject)) {
            target = rootObject;
        } else {
            target = selection.getAncestor();
            assert target != null; // Because (1)
        }
        
        final FXOMDocument newItemDocument = libraryItem.instantiate();
        assert newItemDocument != null; // Because (1)
        final FXOMObject newObject = newItemDocument.getFxomRoot();
        assert newObject != null;
        newObject.moveToFxomDocument(getFxomDocument());
        final InsertAsSubComponentJob job = new InsertAsSubComponentJob(
                newObject, target, -1, this);
        jobManager.push(job);

        // Select the new added object
        getSelection().select(newObject);
    }

    /**
     * Returns true if the 'insert' action is permitted with the specified
     * library item.
     * 
     * @param libraryItem the library item describing the object to be inserted.
     * @return true if the 'insert' action is permitted.
     */
    public boolean canPerformInsert(LibraryItem libraryItem) {
        final FXOMObject targetCandidate;
        final boolean result;
        
        if (getFxomDocument() == null) {
            targetCandidate = null;
        } else {
            assert (libraryItem.getLibrary().getClassLoader() == null)
                || (libraryItem.getLibrary().getClassLoader() == getFxomDocument().getClassLoader());
            final FXOMObject rootObject = getFxomDocument().getFxomRoot();
            if (selection.isEmpty() || selection.isSelected(rootObject)) {
                // No selection or root is selected -> we insert below root
                targetCandidate = rootObject;
            } else {
                // Let's use the common parent of the selected objects.
                // It might be null if selection holds some non FXOMObject entries
                targetCandidate = selection.getAncestor();
            }
        }
        
        if (targetCandidate == null) {
            result = false;
        } else {
            final FXOMDocument newItemDocument = libraryItem.instantiate();
            if (newItemDocument == null) {
                // For some reason, library is unable to instantiate this item
                result = false;
            } else {
                newItemDocument.getFxomRoot().moveToFxomDocument(getFxomDocument());
                final InsertAsSubComponentJob job = new InsertAsSubComponentJob(
                        newItemDocument.getFxomRoot(), targetCandidate, -1, this);
                result = job.isExecutable();
            }
        }
        
        return result;
    }

    /**
     * Return the list of library items that can be passed to 
     * {@link EditorController#performInsert(com.oracle.javafx.scenebuilder.kit.library.LibraryItem)}.
     * 
     * @return the list of library items.
     */
    public static List<Object> getLibraryItemsSupportingInsertion() {
        throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
    }
    
    /**
     * Performs the 'wrap' edit action. This action creates an object
     * matching the specified class and reparent all the selected objects
     * below this new object.
     * 
     * @param wrappingClass the wrapping class
     */
    public void performWrap(Class<? extends Parent> wrappingClass) {
        assert canPerformWrap(wrappingClass);
        final AbstractWrapInJob job = AbstractWrapInJob.getWrapInJob(this, wrappingClass);
        jobManager.push(job);
    }
    
    /**
     * Returns true if the 'wrap' action is permitted with the specified class.
     *
     * @param wrappingClass the wrapping class.
     * @return true if the 'wrap' action is permitted.
     */
    public boolean canPerformWrap(Class<? extends Parent> wrappingClass) {
        if (getClassesSupportingWrapping().contains(wrappingClass) == false) {
            return false;
        }
        final AbstractWrapInJob job = AbstractWrapInJob.getWrapInJob(this, wrappingClass);
        return job.isExecutable();
    }

    private static List<Class<? extends Parent>> classesSupportingWrapping;

    /**
     * Return the list of classes that can be passed to 
     * {@link EditorController#performWrap(java.lang.Class)}.
     * 
     * @return the list of classes.
     */
    public synchronized static Collection<Class<? extends Parent>> getClassesSupportingWrapping() {
        if (classesSupportingWrapping == null) {
            classesSupportingWrapping = new ArrayList<>();
            classesSupportingWrapping.add(javafx.scene.layout.AnchorPane.class);
            classesSupportingWrapping.add(javafx.scene.layout.GridPane.class);
            classesSupportingWrapping.add(javafx.scene.Group.class);
            classesSupportingWrapping.add(javafx.scene.layout.HBox.class);
            classesSupportingWrapping.add(javafx.scene.layout.Pane.class);
            classesSupportingWrapping.add(javafx.scene.control.ScrollPane.class);
            classesSupportingWrapping.add(javafx.scene.control.SplitPane.class);
            classesSupportingWrapping.add(javafx.scene.layout.StackPane.class);
            classesSupportingWrapping.add(javafx.scene.control.TabPane.class);
            classesSupportingWrapping.add(javafx.scene.control.TitledPane.class);
            classesSupportingWrapping.add(javafx.scene.control.ToolBar.class);
            classesSupportingWrapping.add(javafx.scene.layout.VBox.class);
            classesSupportingWrapping = Collections.unmodifiableList(classesSupportingWrapping);
        }
        
        return classesSupportingWrapping;
    }

    /**
     * Performs the copy control action.
     */
    private void performCopy() {
        assert canPerformCopy(); // (1)
        assert selection.getGroup() instanceof ObjectSelectionGroup; // Because of (1)
        final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
        
        final ClipboardEncoder encoder = new ClipboardEncoder(osg.getSortedItems());
        assert encoder.isEncodable();
        Clipboard.getSystemClipboard().setContent(encoder.makeEncoding());
    }

    /**
     * Returns true if the selection is not empty.
     *
     * @return if the selection is not empty.
     */
    private boolean canPerformCopy() {
        return selection.getGroup() instanceof ObjectSelectionGroup;
    }
    
    /**
     * Performs the select all control action.
     * Select all sub components of the selection common ancestor.
     */
    private void performSelectAll() {
        assert canPerformSelectAll(); // (1)
        final FXOMObject rootObject = getFxomDocument().getFxomRoot();
        if (selection.isEmpty()) { // (1)
            // If the current selection is empty, we select the root object
            selection.select(rootObject);
        } else if (selection.getGroup() instanceof ObjectSelectionGroup) {
            // Otherwise, select all sub components of the common ancestor ??
            final FXOMObject ancestor = selection.getAncestor();
            assert ancestor != null; // Because of (1)
            final DesignHierarchyMask mask = new DesignHierarchyMask(ancestor);
            final FXOMPropertyC subComponentProperty = mask.getSubComponentProperty();
            final List<FXOMObject> subComponentObjects = subComponentProperty.getValues();
            selection.select(subComponentObjects);
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            // Select ALL rows / columns
            final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
            final FXOMObject gridPane = gsg.getParentObject();
            assert gridPane instanceof FXOMInstance;
            final DesignHierarchyMask gridPaneMask = new DesignHierarchyMask(gridPane);
            int size = 0;
            switch (gsg.getType()) {
                case ROW:
                    size = gridPaneMask.getRowsSize();
                    break;
                case COLUMN:
                    size = gridPaneMask.getColumnsSize();
                    break;
                default:
                    assert false;
                    break;
            }
            // Select first index
            selection.select((FXOMInstance) gridPane, gsg.getType(), 0);
            for (int index = 1; index < size; index++) {
                selection.toggleSelection((FXOMInstance) gridPane, gsg.getType(), index);
            }
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup(); //NOI18N

        }
    }

    /**
     * Returns true if the root object is not selected and if the sub components
     * of the selection common ancestor are not all already selected.
     * 
     * @return if the root object is not selected and if the sub components of
     * the selection common ancestor are not all already selected.
     */
    private boolean canPerformSelectAll() {
        assert getFxomDocument() != null && getFxomDocument().getFxomRoot() != null;
        if (selection.isEmpty()) { // (1)
            return true;
        } else if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final FXOMObject rootObject = getFxomDocument().getFxomRoot();
            // Cannot select all if root is selected
            if (selection.isSelected(rootObject)) { // (1)
                return false;
            } else {
                // Cannot select all if all sub components are already selected
                final FXOMObject ancestor = selection.getAncestor();
                assert ancestor != null; // Because of (1)
                final DesignHierarchyMask mask = new DesignHierarchyMask(ancestor);
                if (mask.isAcceptingSubComponent()) {
                    final FXOMPropertyC subComponentProperty = mask.getSubComponentProperty();
                    final List<FXOMObject> subComponentObjects = subComponentProperty.getValues();
                    for (FXOMObject subComponentObject : subComponentObjects) {
                        if (selection.isSelected(subComponentObject) == false) {
                            return true;
                        }
                    }
                }
            }
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
            // GridSelectionGroup => at least 1 row/column is selected
            assert gsg.getIndexes().isEmpty() == false;
            return true;
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup(); //NOI18N
        }
        return false;
    }
        
    /**
     * Performs the select parent control action.
     * If the selection is multiple, we select the common ancestor.
     */
    private void performSelectParent() {
        assert canPerformSelectParent(); // (1)
        final FXOMObject ancestor = selection.getAncestor();
        assert ancestor != null; // Because of (1)
        selection.select(ancestor);
    }

    /**
     * Returns true if the selection is not empty and the root object is not
     * selected.
     *
     * @return if the selection is not empty and the root object is not
     * selected.
     */
    private boolean canPerformSelectParent() {
        assert getFxomDocument() != null && getFxomDocument().getFxomRoot() != null;
        final FXOMObject rootObject = getFxomDocument().getFxomRoot();
        return !selection.isEmpty() && !selection.isSelected(rootObject);
    }
    
    /**
     * Performs the select next control action.
     */
    private void performSelectNext() {
        assert canPerformSelectNext(); // (1)
        
        final AbstractSelectionGroup asg = selection.getGroup();
        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            final Set<FXOMObject> items = osg.getItems();
            assert items.size() == 1; // Because of (1)
            final FXOMObject selectedObject = items.iterator().next();
            final FXOMObject nextSibling = selectedObject.getNextSlibing();
            assert nextSibling != null; // Because of (1)
            selection.select(nextSibling);
        } else {
            assert asg instanceof GridSelectionGroup; // Because of (1)
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;
            final FXOMObject gridPane = gsg.getParentObject();
            final DesignHierarchyMask mask = new DesignHierarchyMask(gridPane);
            assert gridPane instanceof FXOMInstance;
            final Set<Integer> indexes = gsg.getIndexes();
            assert indexes.size() == 1; // Because of (1)
            int selectedIndex = indexes.iterator().next();
            int nextIndex = selectedIndex + 1;
            int size = 0;
            switch (gsg.getType()) {
                case ROW:
                    size = mask.getRowsSize();
                    break;
                case COLUMN:
                    size = mask.getColumnsSize();
                    break;
                default:
                    assert false;
                    break;
            }
            assert nextIndex < size; // Because of (1)
            selection.select((FXOMInstance) gridPane, gsg.getType(), nextIndex);
        }
    }

    /**
     * Returns true if the selection is single and the container of the selected
     * object container contains a child next to the selected one.
     *
     * @return if the selection is single and the container of the selected
     * object container contains a child next to the selected one.
     */
    private boolean canPerformSelectNext() {
        assert getFxomDocument() != null && getFxomDocument().getFxomRoot() != null;
        if (selection.isEmpty()) {
            return false;
        }
        final AbstractSelectionGroup asg = selection.getGroup();
        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            final Set<FXOMObject> items = osg.getItems();
            if (items.size() != 1) {
                return false;
            }
            final FXOMObject selectedObject = items.iterator().next();
            return selectedObject.getNextSlibing() != null;
        } else if (asg instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;
            final Set<Integer> indexes = gsg.getIndexes();
            if (indexes.size() != 1) {
                return false;
            }
            final FXOMObject gridPane = gsg.getParentObject();
            final DesignHierarchyMask mask = new DesignHierarchyMask(gridPane);
            int size = 0;
            switch (gsg.getType()) {
                case ROW:
                    size = mask.getRowsSize();
                    break;
                case COLUMN:
                    size = mask.getColumnsSize();
                    break;
                default:
                    assert false;
                    break;
            }
            final int index = indexes.iterator().next();
            return index < size - 1;
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup(); //NOI18N
        }
        return false;
    }
        
    /**
     * Performs the select previous control action.
     */
    private void performSelectPrevious() {
        assert canPerformSelectPrevious(); // (1)
        
        final AbstractSelectionGroup asg = selection.getGroup();
        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            final Set<FXOMObject> items = osg.getItems();
            assert items.size() == 1; // Because of (1)
            final FXOMObject selectedObject = items.iterator().next();
            final FXOMObject previousSibling = selectedObject.getPreviousSlibing();
            assert previousSibling != null; // Because of (1)
            selection.select(previousSibling);
        } else {
            assert asg instanceof GridSelectionGroup; // Because of (1)
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;
            final FXOMObject gridPane = gsg.getParentObject();
            assert gridPane instanceof FXOMInstance;
            final Set<Integer> indexes = gsg.getIndexes();
            assert indexes.size() == 1; // Because of (1)
            int selectedIndex = indexes.iterator().next();
            int previousIndex = selectedIndex - 1;
            assert previousIndex >= 0; // Because of (1)
            selection.select((FXOMInstance) gridPane, gsg.getType(), previousIndex);
        }
    }
    
    /**
     * Returns true if the selection is single and the container of the selected
     * object container contains a child previous to the selected one.
     *
     * @return if the selection is single and the container of the selected
     * object container contains a child previous to the selected one.
     */
    private boolean canPerformSelectPrevious() {
        assert getFxomDocument() != null && getFxomDocument().getFxomRoot() != null;
        if (selection.isEmpty()) {
            return false;
        }
        final AbstractSelectionGroup asg = selection.getGroup();
        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            final Set<FXOMObject> items = osg.getItems();
            if (items.size() != 1) {
                return false;
            }
            final FXOMObject selectedObject = items.iterator().next();
            return selectedObject.getPreviousSlibing() != null;
        } else if (asg instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) asg;
            final Set<Integer> indexes = gsg.getIndexes();
            if (indexes.size() != 1) {
                return false;
            }
            final int index = indexes.iterator().next();
            return index > 0;
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup(); //NOI18N
        }
        return false;
    }
        
    /**
     * Performs the select none control action.
     */
    private void performSelectNone() {
        assert canPerformSelectNone();
        selection.clear();
    }

    /**
     * Returns true if the selection is not empty.
     * 
     * @return if the selection is not empty.
     */
    private boolean canPerformSelectNone() {
        return getSelection().isEmpty() == false;
    }
    
    private static List<Class<? extends Effect>> effectsSupportingAddition;
    
    /**
     * Return the list of effect classes that can be passed to 
     * {@link EditorController#performAddEffect(java.lang.Class)}.
     * 
     * @return the list of library items.
     */
    public synchronized static Collection<Class<? extends Effect>> getEffectsSupportingAddition() {
        if (effectsSupportingAddition == null) {
            effectsSupportingAddition = new ArrayList<>();
            effectsSupportingAddition.add(javafx.scene.effect.Bloom.class);
            effectsSupportingAddition.add(javafx.scene.effect.BoxBlur.class);
            effectsSupportingAddition.add(javafx.scene.effect.ColorAdjust.class);
            effectsSupportingAddition.add(javafx.scene.effect.DisplacementMap.class);
            effectsSupportingAddition.add(javafx.scene.effect.DropShadow.class);
            effectsSupportingAddition.add(javafx.scene.effect.GaussianBlur.class);
            effectsSupportingAddition.add(javafx.scene.effect.Glow.class);
            effectsSupportingAddition.add(javafx.scene.effect.InnerShadow.class);
            effectsSupportingAddition.add(javafx.scene.effect.Lighting.class);
            effectsSupportingAddition.add(javafx.scene.effect.MotionBlur.class);
            effectsSupportingAddition.add(javafx.scene.effect.PerspectiveTransform.class);
            effectsSupportingAddition.add(javafx.scene.effect.Reflection.class);
            effectsSupportingAddition.add(javafx.scene.effect.SepiaTone.class);
            effectsSupportingAddition.add(javafx.scene.effect.Shadow.class);
            effectsSupportingAddition = Collections.unmodifiableList(effectsSupportingAddition);
        }
        
        return effectsSupportingAddition;
    }
    
    /**
     * Return true if the 'add effect' action is permitted with the specified
     * class of effect.
     * 
     * @param effectClass an effect class
     * @return true is the 'add effect' action is permitted
     */
    public boolean canPerformAddEffect(Class<? extends Effect> effectClass) {
        // TODO(elp) : check that the selected object accepts the "effect" 
        // property (or more simply that they are Control instances).
        return false;
    }
    
    /**
     * Performs the 'add effect' action. This method creates an instance of
     * the specified effect class and sets it in the effect property of the
     * selected objects.
     * 
     * @param effectClass class of the effect to be added (never null)
     */
    public void performAddEffect(Class<? extends Effect> effectClass) {
        throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
    }
    
    
    /**
     * Returns the list of library items that can be passed to
     * {@link EditorController#performWrap(java.lang.Object)}.
     * 
     * @return the list of library items usable for wrapping.
     */
    public static List<Object> getLibraryItemsSupportingWrapping() {
        throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
    }
    
    /**
     * If selection contains one object and this an fx:include instance, then
     * returns the path of the included file. Else returns null.
     * 
     * @return the path of the included file associated the selected object or null.
     */
    public String getIncludedFile() {
        throw new UnsupportedOperationException("Not yet implemented"); //NOI18N
    }
    
    /**
     * Returns the URL of the CSS style associated to EditorController class.
     * This stylesheet contains rules shareable by all other components of
     * SB kit.
     * 
     * @return URL of EditorController class style sheet (never null). 
     */
    private static URL stylesheet = null;
    public synchronized static URL getStylesheet() {
        if (stylesheet == null) {
            stylesheet = EditorController.class.getResource("EditorController.css"); //NOI18N
            assert stylesheet != null;
        }
        return stylesheet;
    }
    
    
    /*
     * Private
     */
    
    private void updateFxomDocument(String fxmlText, URL fxmlLocation, ResourceBundle resources) throws IOException {
        final FXOMDocument newFxomDocument;
        
        if (fxmlText != null) {
            newFxomDocument = new FXOMDocument(fxmlText, fxmlLocation, getLibrary().getClassLoader(), resources);
        } else {
            newFxomDocument = null;
        }
        jobManager.clear();
        selection.clear();
        messageLog.clear();
        errorReport.setFxomDocument(newFxomDocument);
        fxomDocumentProperty.setValue(newFxomDocument);
    }
    
    private final ChangeListener<ClassLoader> libraryClassLoaderListener
            = new ChangeListener<ClassLoader>() {
                @Override
                public void changed(ObservableValue<? extends ClassLoader> ov, ClassLoader t, ClassLoader t1) {
                    libraryClassLoaderDidChange();
                }
            };
    
    private void libraryClassLoaderDidChange() {
        if (getFxomDocument() != null) {
            getFxomDocument().setClassLoader(libraryProperty.get().getClassLoader());
            errorReport.requestUpdate();
        }
    }
    
    private void resourcesDidChange() {
        if (getFxomDocument() != null) {
            getFxomDocument().setResources(getResources());
            errorReport.requestUpdate();
        }
    }
    
    private void jobManagerRevisionDidChange() {
        errorReport.requestUpdate();
//        setPickModeEnabled(false);
    }
}
