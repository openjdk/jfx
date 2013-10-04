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

package com.sun.javafx.print;

import javafx.print.JobSettings;
import javafx.print.Paper;
import javafx.print.PaperSource;
import javafx.print.PrintResolution;
import javafx.print.Printer;

/**
 * An internal class which provides a way for implementation code to
 * access to package level protected constructors (etc).
 * All entry points to the printing API need to ensure this class is
 * loaded and initialized.
 */
public class PrintHelper {
    private static PrintAccessor printAccessor;

    static {
        forceInit(Printer.class);
    }

    private PrintHelper() {
    }

    public static PrintResolution createPrintResolution(int fr, int cfr) {
        return printAccessor.createPrintResolution(fr, cfr);
    }

    public static Paper createPaper(String paperName,
                             double paperWidth,
                             double paperHeight,
                             Units units) {
        return printAccessor.createPaper(paperName, paperWidth, paperHeight, units);
    }

    public static PaperSource createPaperSource(String name) {
        return printAccessor.createPaperSource(name);
    }

    public static JobSettings createJobSettings(Printer printer) {
        return printAccessor.createJobSettings(printer);
    }

    public static Printer createPrinter(PrinterImpl impl) {
        return printAccessor.createPrinter(impl);
    }

    public static PrinterImpl getPrinterImpl(Printer printer) {
        return printAccessor.getPrinterImpl(printer);
    }

    public static void setPrintAccessor(final PrintAccessor newAccessor) {
        if (printAccessor != null) {
            throw new IllegalStateException();
        }

        printAccessor = newAccessor;
    }

    public interface PrintAccessor {
        PrintResolution createPrintResolution(int fr, int cfr);

        Paper createPaper(String paperName,
                                          double paperWidth,
                                          double paperHeight,
                                          Units units);

        PaperSource createPaperSource(String name);

        JobSettings createJobSettings(Printer printer);

        Printer createPrinter(PrinterImpl impl);

        PrinterImpl getPrinterImpl(Printer printer);
    }

    private static void forceInit(final Class<?> classToInit) {
        try {
            Class.forName(classToInit.getName(), true,
                          classToInit.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
    }
}
