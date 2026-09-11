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
            "PRECONDITION: At least one printer must be installed. \n" +
            "Without a printer, the operating system might not display the dialogs, " +
            "and the test is not valid.\n\n" +

            "Test each of the four buttons, closing each print or page-setup dialog " +
            "before continuing to the next one.\n\n" +

            "MODAL CASES:\n" + "While a modal dialog is open, the main test window must not accept input " +
            "or allow another dialog to be opened. The main window must also remain " + "behind the dialog.\n\n" +

            "NON-MODAL CASES:\n" + "While a non-modal dialog is open, the main test window should normally " +
            "accept input and be movable in front of the dialog.\n\n" +

            "PLATFORM-SPECIFIC BEHAVIOR:\n" +
            "A modal dialog may remain above only its parent window, above all windows " +
            "in the application, or above all desktop windows. All of these behaviors " +
            "are acceptable. \n" +
            "On macOS, the non-modal cases may behave like the modal cases. " +
            "This is expected operating-system behavior and must not be reported as a failure.";

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
