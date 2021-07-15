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

import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.print.JobSettings;
import javafx.print.Collation;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.PageRange;
import javafx.print.Paper;
import javafx.print.PaperSource;
import javafx.print.PrintColor;
import javafx.print.PrintQuality;
import javafx.print.PrintResolution;
import javafx.print.PrintSides;


import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class JobSettingsInfo extends Application {

    private final int WIDTH = 1000;
    private final int HEIGHT = 800;

    private volatile boolean passed = false;
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
        "This displays the JobSettings info as JobSettings.toString() and \n" +
        "again as individual properties. There's no more validation as to \n" +
        "the exact values than is necessary since it is just toString()\n " +
        "being tested. Just exit the test after reading what is displayed";

    static final String noprinter =
            "There are no printers installed. This test cannot be run\n";

    private TextArea createTextArea(String msg, int hgt) {
        TextArea t = new TextArea(msg);
        t.setWrapText(false);
        t.setEditable(false);
        t.prefRowCountProperty().set(hgt);
        return t;
    }

    private Scene createScene() {

        root = new VBox();
        scene = new Scene(root);

        String msg = instructions;
        if (Printer.getDefaultPrinter() == null) {
            msg = noprinter;
        }
        TextArea info = createTextArea(msg,6);
        root.getChildren().add(info);

        String infoText = runTest();
        TextArea jobInfo = createTextArea(infoText,40);
        root.getChildren().add(jobInfo);

        return scene;
    }

    public String runTest() {
        String text = "";
        PrinterJob job = PrinterJob.createPrinterJob();
        JobSettings settings = job.getJobSettings();
        Printer printer = job.getPrinter();
        PageLayout pl = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE,
                Printer.MarginType.EQUAL);
        settings.pageLayoutProperty().set(pl);
        String fileName = "printtofiletest.prn";
        settings.outputFileProperty().set(fileName);
        settings.jobNameProperty().set("Test Job Name");
        settings.setPageRanges(new PageRange(1,2), new PageRange(5,10));
        settings.copiesProperty().set(2);
        settings.paperSourceProperty().set(PaperSource.MANUAL);
        settings.collationProperty().set(Collation.COLLATED);
        settings.printColorProperty().set(PrintColor.COLOR);
        settings.printSidesProperty().set(PrintSides.DUPLEX);
        settings.printQualityProperty().set(PrintQuality.DRAFT);

        text += "Printer=" + printer + "\n";
        text += "\n";
        text += "Settings:\n";
        text += settings;
        text += "\n\n";
        text += "Individually printed settings\n";
        text += "Collation : " + settings.collationProperty() + "\n";
        text += "Copies : " + settings.copiesProperty() + "\n";
        text += "Sides : " + settings.printSidesProperty() + "\n";
        text += "Job name : " + settings.jobNameProperty() + "\n";
        text += "Output file : " + settings.outputFileProperty() + "\n";
        text += "Page Ranges : " + settings.pageRangesProperty() + "\n";
        text += "Color : " + settings.printColorProperty() + "\n";
        text += "Quality : " + settings.printQualityProperty() + "\n";
        text += "Resolution : " + settings.printResolutionProperty() + "\n";
        text += "Source : " + settings.paperSourceProperty() + "\n";
        text += "Page layout : " + settings.pageLayoutProperty() + "\n";

        return text;
    }
}
