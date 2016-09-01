/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javafx.application.Application;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.web.HTMLEditor;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class PrintHTML extends Application {

    String s = "1) Press Print Button. 2) Select Page Ranges.(3) Print\n" +
        "4) Take note of messages to System.out";

    HTMLEditor he;

    @Override
    public void start(Stage primaryStage) throws Exception {
        he = new HTMLEditor();
        he.setHtmlText(s);
        Button b = new Button("Print");
        b.setOnAction(e -> doPrint());
        VBox vbox = new VBox();
        vbox.getChildren().addAll(he, b);
        primaryStage.setScene(new Scene(vbox));
        primaryStage.show();
    }

    public void doPrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job.showPrintDialog(null)) {
            he.print(job);
            System.out.println("status after print="+job.getJobStatus());
            boolean rv = job.endJob();
            System.out.println("success value from endJob = " + rv);
            System.out.println("status after end="+job.getJobStatus());
        }
    }
}
