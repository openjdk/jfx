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

package test.javafx.print;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javafx.beans.property.ObjectProperty;

import javafx.print.Printer;
import javafx.print.PrinterJob;

public class PrinterJobTest {

  @Test public void dummyTest() {
  }


  private PrinterJob job;

  @Before
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

  @Test public void testPrinter() {
     if (job == null) {
         return;
     }
     Printer printer = job.getPrinter();
     System.out.println("printer="+printer);
     assertNotNull(printer);
     job.setPrinter(printer);
     assertEquals(printer, job.getPrinter());
   }

  @Test public void testPrinterProperty() {
     if (job == null) {
         return;
     }
     ObjectProperty<Printer> printerProp = job.printerProperty();
     System.out.println("printerProp="+printerProp);
     assertNotNull(printerProp);
     Printer printer = printerProp.get();
     assertNotNull(printer);
     printerProp.set(printer);
     assertEquals(printer, printerProp.get());
  }

}
