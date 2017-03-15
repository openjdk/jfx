/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.text.Text;

public class PrintDialogModalityTest extends Application {

    static final String infoText =
     "NOTE: if there are no printers installed this test is not valid " +
     "since depending on O/S no dialog may be displayed.\n" +
     "This tests that a print dialog can be made modal w.r.t " +
     "a parent window. Cycle through in any order the different " +
     "dialog options via pressing the buttons. For the modal cases " +
     "when the dialog is displayed, the original window should be " +
     "unresponsive to input, for example preventing you launching " +
     "another dialog, and also should stay below the dialog. " +
     "Depending on platform the dialog may stay above just the "+
     "parent, or all application or even all desktop windows.\n" +
     "Non-modal dialogs will generally allow you to click on the "+
     "main window and raise it above the dialog. However " +
     "depending on platform, even the non-modal cases may behave " +
     "as if they are modal. Notably this is the case on MacOS as " +
     "that is the behaviour enforced by the O/S";

    @Override
    public void start(Stage primaryStage) {

        VBox vbox;

        Text info = new Text(infoText);
        info.setWrappingWidth(450);
        final PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {

            Button b1 = new Button("Modal Print");
            Button b2 = new Button("Modal Page Setup");
            Button b3 = new Button("Non-modal Print");
            Button b4 = new Button("Non-modal Page Setup");

            b1.setOnAction((ActionEvent event) -> {
                Window w = b1.getScene().getWindow();
                job.showPrintDialog(w);
            });
            b2.setOnAction((ActionEvent event) -> {
                Window w = b2.getScene().getWindow();
                job.showPageSetupDialog(w);
            });
            b3.setOnAction((ActionEvent event) -> {
                job.showPrintDialog(null);
            });
            b4.setOnAction((ActionEvent event) -> {
                job.showPageSetupDialog(null);
            });
            HBox hbox1 = new HBox(2, b1, b2);
            HBox hbox2 = new HBox(2, b3, b4);
            hbox1.setAlignment(Pos.CENTER);
            hbox2.setAlignment(Pos.CENTER);
            vbox = new VBox(3, info, hbox1, hbox2);
        } else {
            Text noprinters = new Text("No printers found!");
            noprinters.setFill(Color.RED);
            vbox = new VBox(2, info, noprinters);
        }
        vbox.setAlignment(Pos.TOP_CENTER);
        Scene scene = new Scene(vbox, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
