/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import java.io.File;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX 3D Viewer Application
 */
public class Jfx3dViewerApp extends Application {
    public static final String FILE_URL_PROPERTY = "fileUrl";
    private static ContentModel contentModel;
    private SessionManager sessionManager;

    public static ContentModel getContentModel() {
        return contentModel;
    }

    @Override public void start(Stage stage) throws Exception {
        sessionManager = SessionManager.createSessionManager("Jfx3dViewerApp");
        sessionManager.loadSession();

        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            sessionManager.getProperties().setProperty(FILE_URL_PROPERTY,
                    new File(args.get(0)).toURI().toURL().toString());
        }
        contentModel = new ContentModel();
        Scene scene = new Scene(
                FXMLLoader.<Parent>load(Jfx3dViewerApp.class.getResource("main.fxml")),
                1024,600);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> sessionManager.saveSession());

//        org.scenicview.ScenicView.show(contentModel.getSubScene().getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
