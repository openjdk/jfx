/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.print;

import javafx.beans.property.*;

import javafx.print.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JobSettingsTest {

  @Test
  public void dummyTest() {
  }


  private PrinterJob job;

  @BeforeEach
  public void setUp() {
     try {
         job = PrinterJob.createPrinterJob();
         if (job == null) {
             System.out.println("No printers installed. Tests cannot run.");
         } else {
             System.out.println("job="+job);
         }
     } catch (SecurityException e) {
         System.out.println("Security exception creating job");
     }
  }

  @Test
  public void testCopiesSettings() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     IntegerProperty copiesProp = js.copiesProperty();
     System.out.println("CopiesProp="+copiesProp);
     assertNotNull(copiesProp);
     int copies = copiesProp.get();
     assertNotNull(copies);
     js.setCopies(copies);
     assertEquals(copies, copiesProp.get());
     assertEquals(copies, js.getCopies());
  }

  @Test
  public void testPageRangeSettings() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PageRange[] pageranges = js.getPageRanges();
     System.out.println("PageRanges="+pageranges);
        Printer printer = job.getPrinter();
        assertNotNull(printer);
        PrinterAttributes pa = printer.getPrinterAttributes();
        assertNotNull(pa);
        if (pa.supportsPageRanges()) {
            PageRange[] newpr = {new PageRange (1, 1)};
            System.out.println("newpr= "+newpr);
            js.setPageRanges(newpr);
            assertEquals(newpr[0], js.getPageRanges()[0]);
        }
  }

  @Test
  public void testPrintColorSettings() {
     PrinterJob job = PrinterJob.createPrinterJob();

     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PrintColor printcolor = js.getPrintColor();
     System.out.println("PrintColor="+printcolor);
     assertNotNull(printcolor);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     assertNotNull(pa);
     Set<PrintColor> s = pa.getSupportedPrintColors();
     if (s != null) {
        for (PrintColor newColor : s) {
            System.out.println("new color= "+newColor);
            js.setPrintColor(newColor);
            assertEquals(newColor, js.getPrintColor());
        }
     }
  }

  @Test
  public void testPrintResolutions() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PrintResolution printresolution = js.getPrintResolution();
     System.out.println("PrintResolution="+printresolution);
     assertNotNull(printresolution);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     assertNotNull(pa);
     Set<PrintResolution> s = pa.getSupportedPrintResolutions();
     if (s != null) {
        for (PrintResolution newRes : s) {
            System.out.println("newRes= "+newRes);
            js.setPrintResolution(newRes);
            assertEquals(newRes, js.getPrintResolution());
        }
     }
  }


   @Test
   public void testPrintSides() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PrintSides printsides = js.getPrintSides();
     System.out.println("PrintSides="+printsides);
     System.out.println("After println PrintSides");
     assertNotNull(printsides);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     System.out.println("After getPrinteAttr");
     assertNotNull(pa);
     System.out.println("Before getSupportedPrintSides");
     Set<PrintSides> s = pa.getSupportedPrintSides();
     System.out.println("After getSupportedPrintSides");
     System.out.println("# of printsides="+s.size());
     if (s != null) {
        for (PrintSides newSides : s) {
            System.out.println("newprintsides= "+newSides);
            js.setPrintSides(newSides);
            assertEquals(newSides, js.getPrintSides());
        }
     }
  }


  @Test
  public void testPageOrientation() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PageLayout pagelayout = js.getPageLayout();
     PageOrientation pageorientation = pagelayout.getPageOrientation();
     System.out.println("PageOrientation="+pageorientation);
     assertNotNull(pageorientation);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     assertNotNull(pa);
     PageOrientation newpageorientation = pa.getDefaultPageOrientation();
     PageLayout newPL = printer.createPageLayout(pagelayout.getPaper(), newpageorientation,   pagelayout.getTopMargin(), pagelayout.getBottomMargin(), pagelayout.getLeftMargin(), pagelayout.getRightMargin());
     js.setPageLayout(newPL);
     pagelayout = js.getPageLayout();
     assertEquals(newpageorientation, pagelayout.getPageOrientation());
  }

    @Test
    public void testPaper() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PageLayout pagelayout = js.getPageLayout();
     Paper paper = pagelayout.getPaper();
     System.out.println("Paper="+paper);
     assertNotNull(paper);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     assertNotNull(pa);
     Set<Paper> s = pa.getSupportedPapers();
     System.out.println("# of papers="+s.size());
     if (s != null) {
        for (Paper newPaper : s) {
            System.out.println("newPaper= "+newPaper);

            double lm = pagelayout.getLeftMargin();
            double rm = pagelayout.getRightMargin();
            double tm = pagelayout.getTopMargin();
            double bm = pagelayout.getBottomMargin();

            if ((lm + rm > newPaper.getWidth()) ||
                (tm + bm > newPaper.getHeight())) {
                // margins exceed this paper, so make them smaller, say 10% of dimension
                lm = rm = newPaper.getWidth() * 0.10;
                tm = bm = newPaper.getHeight() * 0.10;
            }

            PageLayout newPL = printer.createPageLayout(newPaper,
                pagelayout.getPageOrientation(),
                tm, bm, lm, rm);
            System.out.println("newPaper= "+newPaper);
            js.setPageLayout(newPL);
            pagelayout = js.getPageLayout();
            assertEquals(newPaper, pagelayout.getPaper());
        }
     }
  }

  @Test
  public void testPaperSource() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     PaperSource papersource = js.getPaperSource();
     System.out.println("PaperSource="+papersource);
     assertNotNull(papersource);
     Printer printer = job.getPrinter();
     assertNotNull(printer);
     PrinterAttributes pa = printer.getPrinterAttributes();
     assertNotNull(pa);
     Set<PaperSource> s = pa.getSupportedPaperSources();
     System.out.println("# of papersources="+s.size());
     if (s != null) {
        for (PaperSource newPaperSource : s) {
            System.out.println("newpapersource= "+newPaperSource);
            js.setPaperSource(newPaperSource);
            assertEquals(newPaperSource, js.getPaperSource());
        }
     }
  }

  @Test
  public void testCollationSettings() {
     if (job == null) {
         return;
     }
     JobSettings js = job.getJobSettings();
     assertNotNull(js);
     ObjectProperty<Collation> collationProp = js.collationProperty();
     System.out.println("collationProp="+collationProp);
     assertNotNull(collationProp);
     Collation collation = collationProp.get();
     assertNotNull(collation);
     js.setCollation(collation);
     assertEquals(collation, collationProp.get());
     assertEquals(collation, js.getCollation());

  }

}
