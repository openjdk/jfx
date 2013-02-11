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

/*
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
*/
}
