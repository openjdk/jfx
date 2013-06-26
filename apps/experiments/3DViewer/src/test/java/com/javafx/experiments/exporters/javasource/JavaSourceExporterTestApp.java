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
