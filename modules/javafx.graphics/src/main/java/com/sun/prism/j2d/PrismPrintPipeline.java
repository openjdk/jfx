/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.awt.Graphics;
import java.awt.Graphics2D;
import com.sun.javafx.print.PrintHelper;
import com.sun.javafx.print.PrinterImpl;
import com.sun.javafx.print.PrinterJobImpl;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.PrintPipeline;
import com.sun.prism.j2d.print.J2DPrinter;
import com.sun.prism.j2d.print.J2DPrinterJob;

public final class PrismPrintPipeline extends PrintPipeline {

    public static PrintPipeline getInstance() {
        return new PrismPrintPipeline();
    }

    public boolean printNode(NGNode ngNode, int w, int h, Graphics g) {
        PrismPrintGraphics ppg = new PrismPrintGraphics((Graphics2D)g, w, h);
        ngNode.render(ppg);
        return true;
    }

    public PrinterJobImpl createPrinterJob(PrinterJob job) {
        return new J2DPrinterJob(job);
    }

    private static Printer defaultPrinter = null;
    public synchronized Printer getDefaultPrinter() {
        // Eventually this needs to be updated to reflect when
        // the default has changed.
        if (defaultPrinter == null) {
            PrintService defPrt =
                PrintServiceLookup.lookupDefaultPrintService();
            if (defPrt == null) {
                defaultPrinter = null;
            } else {
                if (printerSet == null) {
                    PrinterImpl impl = new J2DPrinter(defPrt);
                    defaultPrinter = PrintHelper.createPrinter(impl);
                } else {
                    for (Printer p : printerSet) {
                        PrinterImpl impl = PrintHelper.getPrinterImpl(p);
                        J2DPrinter j2dp = (J2DPrinter)impl;
                        if (j2dp.getService().equals(defPrt)) {
                            defaultPrinter = p;
                            break;
                        }
                    }
                }
            }
        }
        return defaultPrinter;
    }

    static class NameComparator implements Comparator<Printer> {
        public int compare(Printer p1, Printer p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

    private static final NameComparator nameComparator = new NameComparator();

    // This is static. Eventually I want it to be dynamic, but first
    // it needs to be enhanced to only create new instances where
    // there really has been a change, which will be rare.
    private static ObservableSet<Printer> printerSet = null;
    public synchronized ObservableSet<Printer> getAllPrinters() {
        if (printerSet == null) {
            Set printers = new TreeSet<Printer>(nameComparator);
            // Trigger getting default first, so we don't recreate that.
            Printer defPrinter = getDefaultPrinter();
            PrintService defService = null;
            if (defPrinter != null) {
                J2DPrinter def2D =
                    (J2DPrinter)PrintHelper.getPrinterImpl(defPrinter);
                defService = def2D.getService();
            }
            PrintService[] allServices =
                PrintServiceLookup.lookupPrintServices(null, null);
            for (int i=0; i<allServices.length;i++) {
                if (defService != null && defService.equals(allServices[i])) {
                    printers.add(defPrinter);
                } else {
                    PrinterImpl impl = new J2DPrinter(allServices[i]);
                    Printer printer = PrintHelper.createPrinter(impl);
                    impl.setPrinter(printer);
                    printers.add(printer);
                }
            }
            printerSet =
                FXCollections.unmodifiableObservableSet
                   (FXCollections.observableSet(printers));
        }
        return printerSet;
    }
}
