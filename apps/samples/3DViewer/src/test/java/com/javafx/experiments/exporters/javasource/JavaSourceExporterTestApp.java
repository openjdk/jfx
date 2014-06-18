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
package com.javafx.experiments.exporters.javasource;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.javafx.experiments.importers.Optimizer;
import com.javafx.experiments.importers.maya.MayaImporter;

/**
 * Simple Test application to load 3D file and export as Java Class
 */
public class JavaSourceExporterTestApp extends Application {
    @Override public void start(Stage primaryStage) throws Exception {
        String URL = "file:///Users/jpotts/Projects/jfx-bluray-8.0/apps/bluray/BluRay/src/bluray/botmenu/dukeBot.ma";

        MayaImporter importer = new MayaImporter();
        importer.load(URL, true);

        Optimizer optimizer = new Optimizer(importer.getTimeline(),importer.getRoot());
        optimizer.optimize();

        File out = new File("/Users/jpotts/Projects/jfx-bluray-8.0/apps/bluray/BluRay/src/bluray/botmenu/GreenBot.java");
        JavaSourceExporter javaSourceExporter = new JavaSourceExporter(
                URL.substring(0,URL.lastIndexOf('/')),
                importer.getRoot(), importer.getTimeline(),
                "bluray.botmenu",
                out);
        javaSourceExporter.export();

        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
