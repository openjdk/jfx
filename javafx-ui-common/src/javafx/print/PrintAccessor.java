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

package javafx.print;

import javafx.print.Paper;
import javafx.print.Paper.Units;
import javafx.print.PrintResolution;

import com.sun.javafx.print.PrintAccess;
import com.sun.javafx.print.PrinterImpl;

/**
 * An internal class which provides a way for implementation code to
 * access to package level protected constructors (etc).
 * All entry points to the printing API need to ensure this class is
 * loaded and initialised.
 */
class PrintAccessor extends PrintAccess {

    private static final PrintAccessor theAccessor = new PrintAccessor();
    /* Called by classes which need to ensure this class is initialised */
    static void init() {
        PrintAccess.setPrintAccess(theAccessor);
    }

    public PrintResolution createPrintResolution(int fr, int cfr) {
        return new PrintResolution(fr, cfr);
    }

    public Paper createPaper(String paperName,
                             double paperWidth,
                             double paperHeight,
                             Units units) {
        return new Paper(paperName, paperWidth, paperHeight, units);
    }

    public PaperSource createPaperSource(String name) {
        return new PaperSource(name);
    }

    public JobSettings createJobSettings(Printer printer) {
        return new JobSettings(printer);
    }

    /**
     * PrintAccess is used so that implementation code outside this package
     * package can construct printer instances using a non-visible API.
     * We need this since its not valid for applications to create
     * Printer instances, and we also need to pass in the delegate
     * impl object which is not intended to be public.
     */
    public Printer createPrinter(PrinterImpl impl) {
        return new Printer(impl);
    }

    public PrinterImpl getPrinterImpl(Printer printer) {
        return printer.getPrinterImpl();
    }
}
