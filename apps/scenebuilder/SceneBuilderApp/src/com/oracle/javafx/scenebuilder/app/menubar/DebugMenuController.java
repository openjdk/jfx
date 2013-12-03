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
package com.oracle.javafx.scenebuilder.app.menubar;

import com.oracle.javafx.scenebuilder.app.AppPlatform;
import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
        libraryFolderMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                handleRevealPath(applicationDataFolder);
            }
        });
        
        final Menu libraryFolderMenu = new Menu("Application Data Folder"); //NOI18N
        libraryFolderMenu.getItems().add(libraryFolderMenuItem);
        
        /*
         * Css Panel
         */
        final MenuItem toggleCssPanelMenuItem 
                = new MenuItem();
        toggleCssPanelMenuItem.setText("Toggle Css Panel"); //NOI18N
        toggleCssPanelMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                DebugMenuController.this.documentWindowController.performControlAction(
                        DocumentWindowController.DocumentControlAction.TOGGLE_CSS_PANEL);
            }
        });
                
        menu.getItems().add(toggleCssPanelMenuItem);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(libraryFolderMenu);
        menu.getItems().add(new SeparatorMenuItem());
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
}
