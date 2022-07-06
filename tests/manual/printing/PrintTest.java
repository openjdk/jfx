/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class PrintTest extends Application {

    private static final long TEN_SECONDS = 10 * 1000;
    private final int WIDTH = 400;
    private final int HEIGHT = 400;

    private final int startPage = 1;
    private final int endPage = 2;

    private ComboBox<Printer> printerBox;
    private volatile boolean passed = false;
    private Scene scene;
    private VBox root;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setTitle("Printing test for 8150076");
        Rectangle2D bds = Screen.getPrimary().getVisualBounds();
        stage.setX((bds.getWidth() - WIDTH) / 2);
        stage.setY((bds.getHeight() - HEIGHT) / 2);
        stage.setScene(createScene());
        stage.show();
    }

    static final String instructions =
            "This is regression test for 8150076 (see https://bugs.openjdk.org/browse/JDK-8150076 ).\n" +
                    "Press print button, after this 2 pages must be printed. When all pages are printed you will " +
                    "see \"PASSED!\" message below, otherwise test is failed.";

    static final String noprinter =
            "There are no printers installed. This test cannot run";

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

        printerBox = new ComboBox<>(FXCollections.observableArrayList(Printer.getAllPrinters()));
        printerBox.setValue(Printer.getDefaultPrinter());
        root.getChildren().add(printerBox);

        Button print = new Button("Print");
        print.setLayoutX(80);
        print.setLayoutY(200);
        print.setOnAction(e -> runTest());
        root.getChildren().add(print);

        return scene;
    }


    public void runTest() {
        new Thread(() -> {
            System.out.println("START OF PRINT JOB");
            PrinterJob job = PrinterJob.createPrinterJob(printerBox.getValue());
            job.getJobSettings().setPageRanges(new PageRange(startPage, endPage));
            PageLayout layout = job.getJobSettings().getPageLayout();
            double printableWidth = layout.getPrintableWidth();
            double printableHeight = layout.getPrintableHeight();
            Text printNode = new Text("TEST");
            double scaleX = printableWidth / printNode.prefWidth(1.);
            double scaleY = printableHeight / printNode.prefHeight(1.);
            printNode.getTransforms().add(Transform.scale(scaleX, scaleY));
            printNode.setLayoutY(printableHeight / 2);
            for (int i = startPage; i <= endPage; i++) {
                System.out.println("PRINTING PAGE #" + i);
                job.printPage(printNode);
            }
            job.endJob();
            passed = true;
            System.out.println("END OF PRINT JOB");
        }).start();
        new Thread(() -> {
            while (!passed) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            Platform.runLater(() -> finish());
        }).start();
    }

    private void finish() {
        Text t = new Text();
        t.setText("PASSED!");
        root.getChildren().add(t);
    }
}
