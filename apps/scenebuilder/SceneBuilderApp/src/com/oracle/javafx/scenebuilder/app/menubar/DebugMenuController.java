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
package com.oracle.javafx.scenebuilder.app.menubar;

import com.oracle.javafx.scenebuilder.app.AppPlatform;
import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.JobManager;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.reference.UpdateReferencesJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 *
 */
class DebugMenuController {
    
    private final Menu menu = new Menu("Debug"); //NOI18N
    private final DocumentWindowController documentWindowController;
    
    public DebugMenuController(DocumentWindowController documentWindowController) {
        
        this.documentWindowController = documentWindowController;
        /*
         * User Library Folder
         */
        final String applicationDataFolder 
                = AppPlatform.getApplicationDataFolder();
        final MenuItem libraryFolderMenuItem 
                = new MenuItem();
        libraryFolderMenuItem.setText(applicationDataFolder);
        libraryFolderMenuItem.setOnAction(t -> handleRevealPath(applicationDataFolder));
        
        final Menu libraryFolderMenu = new Menu("Application Data Folder"); //NOI18N
        libraryFolderMenu.getItems().add(libraryFolderMenuItem);
        
        /*
         * Layout
         */
        final MenuItem layoutMenuItem 
                = new MenuItem();
        layoutMenuItem.setText("Check \"localToSceneTransform Properties\" in Content Panel"); //NOI18N
        layoutMenuItem.setOnAction(t -> {
            System.out.println("CHECK LOCAL TO SCENE TRANSFORM BEGINS"); //NOI18N
            final ContentPanelController cpc 
                    = DebugMenuController.this.documentWindowController.getContentPanelController();
            checkLocalToSceneTransform(cpc.getPanelRoot());
            System.out.println("CHECK LOCAL TO SCENE TRANSFORM ENDS"); //NOI18N
        });
                
        /*
         * Tool theme
         */
        final MenuItem useDefaultThemeMenuItem = new MenuItem();
        useDefaultThemeMenuItem.setText("Use Default Theme"); //NOI18N
        useDefaultThemeMenuItem.setOnAction(t -> SceneBuilderApp.getSingleton().performControlAction(SceneBuilderApp.ApplicationControlAction.USE_DEFAULT_THEME, 
                DebugMenuController.this.documentWindowController));
        final MenuItem useDarkThemeMenuItem = new MenuItem();
        useDarkThemeMenuItem.setText("Use Dark Theme"); //NOI18N
        useDarkThemeMenuItem.setOnAction(t -> SceneBuilderApp.getSingleton().performControlAction(SceneBuilderApp.ApplicationControlAction.USE_DARK_THEME, 
                DebugMenuController.this.documentWindowController));
        
        /*
         * Undo/redo stack
         */
        final Menu undoRedoStack = new Menu();
        undoRedoStack.setText("Undo/Redo Stack"); //NOI18N
        undoRedoStack.getItems().add(makeMenuItem("Dummy", true)); //NOI18N
        undoRedoStack.setOnMenuValidation(t -> {
            assert t.getTarget() instanceof Menu;
            undoRedoStackMenuShowing((Menu) t.getTarget());
        });
                
        menu.getItems().add(libraryFolderMenu);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(layoutMenuItem);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(useDefaultThemeMenuItem);
        menu.getItems().add(useDarkThemeMenuItem);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(undoRedoStack);
    }
    
    public Menu getMenu() {
        return menu;
    }
    
    
    /*
     * Private
     */
    
    private void handleRevealPath(String path) {
        try {
            EditorPlatform.revealInFileBrowser(new File(path));
        } catch(IOException x) {
            final ErrorDialog d = new ErrorDialog(null);
            d.setMessage("Failed to reveal folder"); //NOI18N
            d.setDetails(path);
            d.setDebugInfoWithThrowable(x);
            d.showAndWait();
        }
    }
    
    
    private void checkLocalToSceneTransform(Node node) {
        
        final Point2D p1 = node.localToScene(0, 0);
        final Point2D p2 = node.getLocalToSceneTransform().transform(0, 0);
        
        final boolean okX = MathUtils.equals(p1.getX(), p2.getX(), 0.0000001);
        final boolean okY = MathUtils.equals(p1.getY(), p2.getY(), 0.0000001);
        if ((okX == false) || (okY == false)) {
            System.out.println("CHECK FAILED FOR " + node + ", p1=" + p1 + ", p2=" + p2); //NOI18N
        }
        
        if (node instanceof Parent) {
            final Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                checkLocalToSceneTransform(child);
            }
        }
    }
    
    /*
     * Private (undo/redo stack)
     */
    
    private void undoRedoStackMenuShowing(Menu menu) {
        final JobManager jobManager
                = documentWindowController.getEditorController().getJobManager();
        
        final List<Job> redoStack = jobManager.getRedoStack();
        final List<Job> undoStack = jobManager.getUndoStack();
        
        final List<MenuItem> menuItems = menu.getItems();
        
        menuItems.clear();
        if (redoStack.isEmpty()) {
            menuItems.add(makeMenuItem("Redo Stack Empty", true)); //NOI18N
        } else {
            for (Job job : redoStack) {
                menuItems.add(0, makeJobMenuItem(job));
            }
        }
        
        menuItems.add(new SeparatorMenuItem());
        
        if (undoStack.isEmpty()) {
            menuItems.add(makeMenuItem("Undo Stack Empty", true)); //NOI18N
        } else {
            for (Job job : undoStack) {
                menuItems.add(makeJobMenuItem(job));
            }
        }
    }
    
    
    private MenuItem makeMenuItem(String text, boolean disable) {
        final MenuItem result = new MenuItem();
        result.setText(text);
        result.setDisable(disable);
        return result;
    }
    
    
    private MenuItem makeJobMenuItem(Job job) {
        final MenuItem result;
        
        if (job instanceof CompositeJob) {
            final CompositeJob compositeJob = (CompositeJob)job;
            final Menu newMenu = new Menu(compositeJob.getClass().getSimpleName());
            addJobMenuItems(compositeJob.getSubJobs(), newMenu);
            result = newMenu;
        } else if (job instanceof BatchJob) {
            final BatchJob batchJob = (BatchJob)job;
            final Menu newMenu = new Menu(batchJob.getClass().getSimpleName());
            addJobMenuItems(batchJob.getSubJobs(), newMenu);
            result = newMenu;
        } else if (job instanceof UpdateReferencesJob) {
            final UpdateReferencesJob fixReferencesJob = (UpdateReferencesJob)job;
            final Menu newMenu = new Menu(fixReferencesJob.getClass().getSimpleName());
            addJobMenuItems(fixReferencesJob, newMenu);
            result = newMenu;
        } else {
            result = new MenuItem(job.getClass().getSimpleName());
        }
        
        return result;
    }
    
    private void addJobMenuItems(List<Job> jobs, Menu targetMenu) {
        for (Job job : jobs) {
            targetMenu.getItems().add(makeJobMenuItem(job));
        }
        
        if (targetMenu.getItems().isEmpty()) {
            targetMenu.getItems().add(makeMenuItem("Empty", true)); //NOI18N
        }
    }
    
    
    private void addJobMenuItems(UpdateReferencesJob j, Menu targetMenu) {
        targetMenu.getItems().add(makeJobMenuItem(j.getSubJob()));
        final List<Job> fixJobs = j.getFixJobs();
        if (fixJobs.isEmpty() == false) {
            targetMenu.getItems().add(new SeparatorMenuItem());
            addJobMenuItems(fixJobs, targetMenu);
        }
    }
}
