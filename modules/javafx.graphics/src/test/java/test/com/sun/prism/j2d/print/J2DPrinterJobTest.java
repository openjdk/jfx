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

package test.com.sun.prism.j2d.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.StreamPrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

import com.sun.javafx.tk.Toolkit;
import com.sun.prism.j2d.print.J2DPrinter;
import com.sun.prism.j2d.print.J2DPrinterJob;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javafx.print.PageLayoutShim;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.print.PrinterShim;
import javafx.scene.ParentShim;
import test.com.sun.javafx.pgstub.StubToolkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class J2DPrinterJobTest {

    private J2DPrinterJob job;
    private PrinterJobMock printerJobMock;

    @Before
    public void setUp() {
        // Otherwise, the printer job will try to create a nestedLoop which is not supported by the StubToolkit.
        ((StubToolkit) Toolkit.getToolkit()).setFxUserThread(false);

        PrinterJob printerJob = PrinterJob.createPrinterJob(PrinterShim.createPrinter(new J2DPrinter(new PrintServiceMock())));

        printerJobMock = new PrinterJobMock();
        job = new J2DPrinterJob(printerJob) {

            @Override
            protected java.awt.print.PrinterJob createPrinterJob() {
                return printerJobMock;
            }

            @Override
            protected J2DPrinterJob.J2DPageable createJ2dPageable() {
                return new J2DPageable();
            }

            class J2DPageable extends J2DPrinterJob.J2DPageable {

                @Override
                public void setPageDone(boolean pageDone) {
                    super.setPageDone(pageDone);

                    String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
                    if (methodName.equals("waitForNextPage")) {
                        // Stop the function 'waitForNextPage()' for a while.
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (!methodName.equals("implPrintPage")) {
                        fail("The test needs to be revised, " +
                             "because the setPageDone() method is only expected in the 'waitForNextPage()' and 'implPrintPage()' methods, " +
                             "but it was called from '" + methodName + "()'");
                    }
                }

                @Override
                protected void printNode(Graphics g, int w, int h) {
                    // do nothing, as we don't really print in the test
                }

                @Override
                protected void clearScene(J2DPrinterJob.PageInfo pageInfo) {
                    // not needed in test
                }
            }
        };
    }

    @After
    public void tearDown() {
        ((StubToolkit) Toolkit.getToolkit()).setFxUserThread(true);
    }

    @Test
    public void testJobEnd() {
        assertTrue(job.print(PageLayoutShim.createPageLayout(Paper.A4, PageOrientation.PORTRAIT), new ParentShim()));
        assertTrue(job.endJob());
        printerJobMock.waitUntilPrinted(10);
        assertTrue("It seems that an error is occurred during printing.", job.endJob()); // check the jobError is not set
        assertEquals("The submitted page was not printed.", List.of(0), printerJobMock.getPrintedPages());
    }

    @Test
    public void testJobCanceled() {
        assertTrue(job.print(PageLayoutShim.createPageLayout(Paper.A4, PageOrientation.PORTRAIT), new ParentShim()));
        job.cancelJob();
        printerJobMock.waitUntilPrinted(10);
        assertEquals("The page was printed even though the job was canceled.", List.of(), printerJobMock.getPrintedPages());
    }

    private static class PrintServiceMock extends StreamPrintService {
        public PrintServiceMock() {
            super(new ByteArrayOutputStream());
        }

        @Override
        public String getOutputFormat() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public DocPrintJob createPrintJob() {
            return null;
        }

        @Override
        public void addPrintServiceAttributeListener(PrintServiceAttributeListener listener) {

        }

        @Override
        public void removePrintServiceAttributeListener(PrintServiceAttributeListener listener) {

        }

        @Override
        public PrintServiceAttributeSet getAttributes() {
            return null;
        }

        @Override
        public <T extends PrintServiceAttribute> T getAttribute(Class<T> category) {
            return null;
        }

        @Override
        public DocFlavor[] getSupportedDocFlavors() {
            return new DocFlavor[0];
        }

        @Override
        public boolean isDocFlavorSupported(DocFlavor flavor) {
            return false;
        }

        @Override
        public Class<?>[] getSupportedAttributeCategories() {
            return new Class[0];
        }

        @Override
        public boolean isAttributeCategorySupported(Class<? extends Attribute> category) {
            return false;
        }

        @Override
        public Object getDefaultAttributeValue(Class<? extends Attribute> category) {
            return null;
        }

        @Override
        public Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor, AttributeSet attributes) {
            return null;
        }

        @Override
        public boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor, AttributeSet attributes) {
            return false;
        }

        @Override
        public AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes) {
            return null;
        }

        @Override
        public ServiceUIFactory getServiceUIFactory() {
            return null;
        }
    }

    private static class PrinterJobMock extends java.awt.print.PrinterJob {
        private PrintService service;
        private Pageable pageable;
        private volatile boolean printed;
        private List<Integer> printedPages = Collections.synchronizedList(new ArrayList<>());

        public void waitUntilPrinted(int timeoutInSeconds) {
            for (int i = 0; !printed && i < timeoutInSeconds; i++) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!printed) {
                fail("Timeout: after " + timeoutInSeconds + " seconds the print job is still running");
            }
        }

        public List<Integer> getPrintedPages() {
            return printedPages;
        }

        @Override
        public void setPrintService(PrintService service) {
            this.service = service;
        }

        public PrintService getPrintService() {
            return service;
        }

        @Override
        public void setPageable(Pageable pageable) throws NullPointerException {
            this.pageable = pageable;
        }

        @Override
        public void print(PrintRequestAttributeSet attributes) throws PrinterException {
            try {
                int numberOfPages = pageable.getNumberOfPages();
                if (numberOfPages == Pageable.UNKNOWN_NUMBER_OF_PAGES) {
                    numberOfPages = 10; // it should be enough for test purposes
                }
                for (int i = 0; i < numberOfPages; i++) {
                    PageFormat pageFormat = pageable.getPageFormat(i);
                    int printResult = printPage(pageFormat, i);
                    if (printResult == Printable.NO_SUCH_PAGE) {
                        break;
                    }
                    printedPages.add(i);
                }
            } finally {
                printed = true;
            }
        }

        protected int printPage(PageFormat pageFormat, int pageIndex) throws PrinterException {
            Graphics2D graphics = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB).createGraphics();
            Printable printable = pageable.getPrintable(pageIndex);
            return printable.print(graphics, pageFormat, pageIndex);
        }

        @Override
        public void print() throws PrinterException {
        }

        @Override
        public void setPrintable(Printable painter) {

        }

        @Override
        public void setPrintable(Printable painter, PageFormat format) {

        }

        @Override
        public boolean printDialog() throws HeadlessException {
            return false;
        }

        @Override
        public PageFormat pageDialog(PageFormat page) throws HeadlessException {
            return null;
        }

        @Override
        public PageFormat defaultPage(PageFormat page) {
            return null;
        }

        @Override
        public PageFormat validatePage(PageFormat page) {
            return null;
        }

        @Override
        public void setCopies(int copies) {

        }

        @Override
        public int getCopies() {
            return 0;
        }

        @Override
        public String getUserName() {
            return null;
        }

        @Override
        public void setJobName(String jobName) {

        }

        @Override
        public String getJobName() {
            return null;
        }

        @Override
        public void cancel() {

        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
