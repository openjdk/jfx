/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.collections.ObservableSet;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrintResolution;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.stage.Stage;

/**
 * Without knowing for sure what the actual hardware margins are
 * on a printer, all we can do here is test that we see reasonable
 * results and that everything adds up.
 */
public class TestMargins extends Application {

    @Override
    public void start(Stage primaryStage) {

        ObservableSet<Printer> printers = Printer.getAllPrinters();
        for (Printer printer : printers) {
            PrinterAttributes pa = printer.getPrinterAttributes();
            Paper paper = pa.getDefaultPaper();
            double pwid = paper.getWidth();
            double phgt = paper.getHeight();
            // NB 24 pts - 1/3" may not be possible on a printer
            // so we won't 'fail' if something else is reported.
            double mx = 24;
            double my = 24;
            PageLayout pl = printer.createPageLayout(paper,
                            PageOrientation.PORTRAIT, mx, mx, my, my);
            double pw = pl.getPrintableWidth();
            double ph = pl.getPrintableHeight();
            double lm = pl.getLeftMargin();
            double rm = pl.getRightMargin();
            double tm = pl.getTopMargin();
            double bm = pl.getBottomMargin();
          

            print("Printer: "+printer.getName());
            print("  Default paper = " + paper + "size(pts)="+pwid+"x"+phgt);
            
            print("  PageLayout (pts) : Paper size " + pw + "x"+ph + 
                 ", Margins: "+lm+ ","+tm+ ","+rm+ ","+bm);
            double width = pw + lm + rm;
            double height = ph + tm + bm;
            print("Reconsituted paper size = " + width+"x"+height);
            if ( (Math.abs(pwid-width) > 1) ||
                 (Math.abs(phgt-height) > 1)) {
                print("BAD LAYOUT\n"+ pl);
            }
            print("  ----------------------------");
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void print(String msg) {
        System.out.println(msg);
    }
}
