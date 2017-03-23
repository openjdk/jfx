/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import javafx.geometry.Rectangle2D;

import javafx.print.JobSettings;
import javafx.print.Printer;
import javafx.print.Collation;
import javafx.print.PageRange;
import javafx.print.Paper;
import javafx.print.PaperSource;
import javafx.print.PrintColor;
import javafx.print.PageOrientation;
import javafx.print.PrintQuality;
import javafx.print.PrintResolution;
import javafx.print.PrintSides;

/*
 * This is called 'PrinterImpl' not because it is a class implementing
 * features of a superclass, but because its defines the interfaces
 * required of the delegate implementation class for a Printer.
 * The actual implementation class needs to be discovered at runtime.
 */
public interface PrinterImpl {

    public void setPrinter(Printer printer);

    public String getName();

    public JobSettings getDefaultJobSettings();

    public Rectangle2D printableArea(Paper paper);

    public int defaultCopies();
    public int maxCopies();

    public Collation defaultCollation();
    public Set<Collation> supportedCollations();

    public PrintSides defaultSides();
    public Set<PrintSides> supportedSides();

    public PageRange defaultPageRange();
    public boolean supportsPageRanges();

    public PrintResolution defaultPrintResolution();
    public Set<PrintResolution> supportedPrintResolution();

    public PrintColor defaultPrintColor();
    public Set<PrintColor> supportedPrintColor();

    public PrintQuality defaultPrintQuality();
    public Set<PrintQuality> supportedPrintQuality();

    public PageOrientation defaultOrientation();
    public Set<PageOrientation> supportedOrientation();

    public Paper defaultPaper();
    public Set<Paper> supportedPapers();

    public PaperSource defaultPaperSource();
    public Set<PaperSource> supportedPaperSources();

}
