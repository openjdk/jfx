/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.*;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class PrintToFileTest extends Application {

    private final int WIDTH = 400;
    private final int HEIGHT = 400;

    private volatile boolean passed = false;
    private volatile boolean failed = false;
    private Scene scene;
    private VBox root;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setTitle("Printing to file test");
        Rectangle2D bds = Screen.getPrimary().getVisualBounds();
        stage.setX((bds.getWidth() - WIDTH) / 2);
        stage.setY((bds.getHeight() - HEIGHT) / 2);
        stage.setScene(createScene());
        stage.show();
    }

    static final String instructions =
        "This tests that programmatically specifying print to file works.\n" +
        "Select the Print button to run the test\n";

    static final String noprinter =
            "There are no printers installed. This test cannot be run\n";

    private TextArea createInfo(String msg) {
        TextArea t = new TextArea(msg);
        t.setWrapText(true);
        t.setEditable(false);
        return t;
    }

    private Scene createScene() {

        root = new VBox();
        scene = new Scene(root);

        String msg = instructions;
        if (Printer.getDefaultPrinter() == null) {
            msg = noprinter;
        }
        TextArea info = createInfo(msg);
        root.getChildren().add(info);

        Button print = new Button("Print");
        print.setLayoutX(80);
        print.setLayoutY(200);
        print.setOnAction(e -> runTest());
        root.getChildren().add(print);

        return scene;
    }

    public void runTest() {
        new Thread(() -> {
            passed = false;
            failed = false;
            System.out.println("START OF PRINT JOB");
            PrinterJob job = PrinterJob.createPrinterJob();
            JobSettings settings = job.getJobSettings();
            String fileName = "printtofiletest.prn";
            settings.outputFileProperty().set(fileName);
            String destFileName =  settings.outputFileProperty().get();
            System.out.println("dest="+ destFileName);
            File f = new File(destFileName);
            f.delete();
            Platform.runLater(() -> {
                Text t = new Text("file="+settings.getOutputFile());
                root.getChildren().add(t);
            });
            Text printNode = new Text("\n\nTEST\nabc\ndef");
            job.printPage(printNode);
            job.endJob();
            try {
                 // wait for printer spooler to create the file.
                 Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            if (f.exists()) {
                System.out.println("created file " + f);
                passed = true;
            } else {
                failed = true;
            }
            System.out.println("END OF PRINT JOB");
        }).start();
        new Thread(() -> {
            while (!passed && !failed) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            Platform.runLater(() -> displayMessage());

        }).start();
    }

    private void displayMessage() {
        Text t = new Text();
        if (passed) {
            t.setText("TEST PASSED!");
        } else {
            t.setText("TEST FAILED!");
        }
        root.getChildren().add(t);
    }
}
