/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import javafx.application.Application;
import javafx.print.PrinterJob;
import javafx.print.Printer;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class PrinterListenerTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    ObservableSet<Printer> printers;
    Printer defaultPrinter;
    Stage window;

    public void start(Stage stage) {
        window = stage;
        printPrinters();
        printers.addListener(new SetChangeListener<Printer>() {
           public void onChanged(SetChangeListener.Change<? extends Printer> change) {
               printChanged(change);
           }
        });

       VBox root = new VBox();
       Scene scene = new Scene(root);
       Button b = new Button("List Printers");
       b.setOnAction(e -> printPrinters());
       root.getChildren().add(b);
       Button p = new Button("Show Print Dialog");
       p.setOnAction(e -> showPrintDialog());
       root.getChildren().add(p);
       Text t = new Text();
       t.setWrappingWidth(400);
       t.setText(
         "This is a very manual test which to be useful " +
         "requires you to be adding and removing printers and changing " +
         "the default from System Settings or whatever is the norm for " +
         "the platform being tested and then pressing 'List Printers'. \n" +
         "Updates happen only when you call the API - no background thread. " +
         "The Added or Removed printers will be reported by the change listener " +
         "demonstrating that the ObservableList works.\n" +
         "The Print Dialog can be used to verify what is listed matches the dialog.");

       root.getChildren().add(t);
       stage.setScene(scene);
       stage.show();
    }

    public void showPrintDialog() {
        PrinterJob job = PrinterJob.createPrinterJob();
        job.showPrintDialog(window);
    }

    public void printPrinters() {
        if (printers != null) {
            System.out.println("Current default printer="+defaultPrinter);
            System.out.println("Current Printers :");
            for (Printer p : printers) System.out.println(p);
            System.out.println();
        }

        printers = Printer.getAllPrinters();
        defaultPrinter = Printer.getDefaultPrinter();

        System.out.println("New Default Printer ="+defaultPrinter);
        System.out.println("New Printers :");
        for (Printer p : printers) System.out.println(p);
        System.out.println();
    }

    static void printChanged(SetChangeListener.Change<? extends Printer> c) {
        if (c.wasAdded()) {
            System.out.println("Added : " + c.getElementAdded());
        } else if (c.wasRemoved()) {
            System.out.println("Removed : " + c.getElementRemoved());
        } else {
           System.out.println("Other change");
        }

    }
}
