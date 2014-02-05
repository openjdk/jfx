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
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
        libraryFolderMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                handleRevealPath(applicationDataFolder);
            }
        });
        
        final Menu libraryFolderMenu = new Menu("Application Data Folder"); //NOI18N
        libraryFolderMenu.getItems().add(libraryFolderMenuItem);
        
        /*
         * Layout
         */
        final MenuItem layoutMenuItem 
                = new MenuItem();
        layoutMenuItem.setText("Check \"localToSceneTransform Properties\" in Content Panel"); //NOI18N
        layoutMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                System.out.println("CHECK LOCAL TO SCENE TRANSFORM BEGINS"); //NOI18N
                final ContentPanelController cpc 
                        = DebugMenuController.this.documentWindowController.getContentPanelController();
                checkLocalToSceneTransform(cpc.getPanelRoot());
                System.out.println("CHECK LOCAL TO SCENE TRANSFORM ENDS"); //NOI18N
            }
        });
                
        menu.getItems().add(libraryFolderMenu);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(layoutMenuItem);
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
}
