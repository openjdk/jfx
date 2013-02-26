/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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
package ensemble.samplepage;

import ensemble.SampleInfo;
import ensemble.sampleproject.SampleProjectBuilder;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPaneBuilder;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 *
 */
public class Sources extends VBox {
    
    private Button copy;
    private Button saveAsProject;
    private HBox buttons;
    private TabPane tabPane;

    public Sources(final SamplePage samplePage) {
        super(SamplePage.INDENT);
        saveAsProject = ButtonBuilder.create().text("Save As Project").build();
        saveAsProject.setOnAction(new EventHandler<ActionEvent>() {
             @Override public void handle(ActionEvent actionEvent) {
                 File initialDir = FileSystemView.getFileSystemView().getDefaultDirectory();
                 FileChooser fileChooser = new FileChooser();
                 fileChooser.setTitle("Save Netbeans Project As:");
                 fileChooser.setInitialDirectory(initialDir);
                 File result = fileChooser.showSaveDialog(saveAsProject.getScene().getWindow());
                 if (result != null) {
                     SampleProjectBuilder.createSampleProject(result, samplePage.sample);
                 }
             }
         });        
        buttons = HBoxBuilder.create().spacing(SamplePage.INDENT).alignment(Pos.BOTTOM_RIGHT).children(saveAsProject).build();
        tabPane = TabPaneBuilder.create().minWidth(50).minHeight(50).styleClass("floating").build();
        for (SampleInfo.URL sourceURL : samplePage.sample.getSources()) {
            tabPane.getTabs().add(new SourceTab(sourceURL, samplePage));
        }
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        getChildren().setAll(buttons, tabPane);
    }
}
