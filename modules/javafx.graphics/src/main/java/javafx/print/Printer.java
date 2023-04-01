/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.print.PrintHelper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableSet;
import javafx.geometry.Rectangle2D;

import static javafx.print.PageOrientation.*;

import com.sun.javafx.tk.PrintPipeline;
import com.sun.javafx.print.PrinterImpl;
import com.sun.javafx.print.Units;

/**
 * A Printer instance represents the destination for a print job.
 * <p>
 * Printers may be enumerated and selected for use with a print job.
 * <p>
 * The configuration of the printer default settings are then used to
 * populate the initial settings for a job.
 * <p>
 * Since the availability of printers may change during the
 * execution of a program, due to administrative actions,
 * a long running program which has cached a printer which
 * has since been taken off-line, may create a job using that
 * instance, but printing will fail.
 *
 * @since JavaFX 8.0
 */
public final class Printer {

    /**
     * Retrieve the installed printers.
     * The set of printers may be dynamic.
     * Consequently, there is no guarantee that the result will be
     * the same from call to call, but should change only as
     * a result of the default changing in the environment of the
     * application.
     * <p>Note: since printers may be installed, but offline, then
     * the application may want to query the status of a printer
     * before using it.
     * @return may be null if there are no printers.
     * @throws SecurityException if the application does not
     * have permission to browse printers.
     */
    public static ObservableSet<Printer> getAllPrinters() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }
        return PrintPipeline.getPrintPipeline().getAllPrinters();
    }

    private static ReadOnlyObjectWrapper<Printer> defaultPrinter;

    private static ReadOnlyObjectWrapper<Printer> defaultPrinterImpl() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }
        Printer p = PrintPipeline.getPrintPipeline().getDefaultPrinter();
        if (defaultPrinter == null) {
            defaultPrinter =
                new ReadOnlyObjectWrapper<>(null, "defaultPrinter", p);
        } else {
            defaultPrinter.setValue(p);
        }
        return defaultPrinter;
    }

    /**
     * A read only object property representing the current default printer.
     * If there are no installed printers, the wrapped value will be null.
     * @return the current default printer
     * @throws SecurityException if the application does not
     * have permission to browse printers.
     */
    public static ReadOnlyObjectProperty<Printer> defaultPrinterProperty() {
        return defaultPrinterImpl().getReadOnlyProperty();
    }

    /**
     * Retrieve the default printer.
     * May return null if no printers are installed.
     * <p>
     * The configuration of available printers may be dynamic.
     * Consequently there is no guarantee that the result will be
     * the same from call to call, but should change only as
     * a result of the default changing in the environment of the
     * application.
     * @return default printer or null.
     * @throws SecurityException if the application does not
     * have permission to browse printers.
     */
    public static Printer getDefaultPrinter() {
        return defaultPrinterProperty().get();
    }

    private PrinterImpl impl;

    Printer(PrinterImpl impl) {
        this.impl = impl;
        impl.setPrinter(this);
    }

    PrinterImpl getPrinterImpl() {
        return impl;
    }

    /**
     * Return the name used by the underlying system to identify
     * the printer to users and/or applications.
     * @return printer name.
     */
    public String getName() {
        return impl.getName();
    }

    private PrinterAttributes attributes;
    /**
     * Retrieves the delegate object encapsulating the printer
     * attributes and capabilities.
     * @return printer attributes.
     */
    public PrinterAttributes getPrinterAttributes() {
        if (attributes == null) {
            attributes = new PrinterAttributes(impl);
        }
        return attributes;
    }

    /**
     * Returns the default settings for this printer as would be
     * used in setting up a job for this printer.
     * <p>
     * When a job is first created and associated with a printer,
     * (typically the default printer), it acquires these default
     * settings for that printer.
     * This method always returns a new instance.
     */
    JobSettings getDefaultJobSettings() {
        return impl.getDefaultJobSettings();
    }

    /**
     * The MarginType is used to determine the printable area of a PageLayout.
     * @since JavaFX 8.0
     */
    public static enum MarginType {
        /**
         * This requests a default 0.75 inch margin on all sides.
         * This is considered to be a common default and is supported
         * by all known printers. However this may be adjusted if the paper is
         * too small, to ensure that the margins are not more than 50% of
         * the smaller dimension. Applications that do expect to deal with
         * such small media should likely be specifying the required margins
         * explicitly.
         * In the unlikely event the hardware margin is larger than 0.75"
         * it will be adjusted to that same hardware minimum on all sides.
         */
        DEFAULT,
        /**
         * Request margins are set to be the smallest on each side that
         * the hardware allows. This creates the greatest printable area
         * but the margins may not be aesthetic if they are too small, or
         * there is significant variation on the different sides of the
         * paper.
         * <p>
         * This is is also useful for an application that wants to know
         * this so it can construct a new PageLayout that fits within
         * these margins.
         */
        HARDWARE_MINIMUM,
        /**
         * Choose the largest of the four hardware margins, and use that for
         * all for margins, so that the margins are equal on all sides.
         */
        EQUAL,
        /**
         * Similar to <code>EQUAL</code>, but it chooses the larger of
         * the left/right hardware margins and top/bottom hardware margins
         * separately, so that the top and bottom margins are equal, and
         * the left and right margins are equal.
         */
        EQUAL_OPPOSITES,

    }

    private PageLayout defPageLayout;
    /**
     * Return the default page layout for this printer.
     * @return default page layout.
     */
    public PageLayout getDefaultPageLayout() {
        if (defPageLayout == null) {
            PrinterAttributes printerCaps = getPrinterAttributes();
            defPageLayout =
                createPageLayout(printerCaps.getDefaultPaper(),
                                 printerCaps.getDefaultPageOrientation(),
                                 MarginType.DEFAULT);
        }
        return defPageLayout;
    }

    /**
     * Obtain a new PageLayout instance for this printer using the specified
     * parameters.
     * The paper should be one of the supported papers and
     * the orientation should be a supported orientation.
     * If the printer cannot support the layout as specified, it
     * will adjust the returned layout to a supported configuration
     * @param paper The paper to use
     * @param orient The orientation to use
     * @param mType the margin type to use
     * @return PageLayout based on the specified parameters.
     * @throws NullPointerException if any of the parameters are null.
     */
    public PageLayout createPageLayout(Paper paper, PageOrientation orient,
                        MarginType mType) {

        if (paper == null || orient == null || mType == null) {
            throw new NullPointerException("Parameters cannot be null");
        }

        // TBD: Adjust paper to a supported one first.
        Rectangle2D imgArea = impl.printableArea(paper);
        double width = paper.getWidth() / 72.0;
        double height = paper.getHeight() / 72.0;
        double plm = imgArea.getMinX();
        double ptm = imgArea.getMinY();
        double prm = width - imgArea.getMaxX();
        double pbm = height - imgArea.getMaxY();
        // fix for FP error
        if (plm < 0.01) plm = 0;
        if (prm < 0.01) prm = 0;
        if (ptm < 0.01) ptm = 0;
        if (pbm < 0.01) pbm = 0;

        switch (mType) {
        case DEFAULT:
            plm = (plm <= 0.75) ? 0.75 : plm;
            prm = (prm <= 0.75) ? 0.75 : prm;
            ptm = (ptm <= 0.75) ? 0.75 : ptm;
            pbm = (pbm <= 0.75) ? 0.75 : pbm;
            break;
        case EQUAL: {
            double maxH = Math.max(plm, prm);
            double maxV = Math.max(ptm, pbm);
            double maxM = Math.max(maxH, maxV);
            plm = prm = ptm = pbm = maxM;
            break;
        }
        case EQUAL_OPPOSITES: {
            double maxH = Math.max(plm, prm);
            double maxV = Math.max(ptm, pbm);
            plm = prm = maxH;
            ptm = pbm = maxV;
            break;
        }
        case HARDWARE_MINIMUM:
        default: // Use hardware margins as is.
            break;
        }

        while (plm + prm > width) {
           plm /= 2.0;
           prm /= 2.0;
        }
        while (ptm + pbm > height) {
           ptm /= 2.0;
           pbm /= 2.0;
        }
        double lm, rm, tm, bm;
        // Now we gave the margins, they need to be adjusted into
        // the orientation of the paper. If the orientation is not
        // supported by the printer, then that needs to adjusted first.

        // TBD: Adjust orient to a supported one first.
        switch (orient) {
        case LANDSCAPE: lm = pbm; rm = ptm; tm = plm; bm = prm;
            break;
        case REVERSE_LANDSCAPE: lm = ptm; rm = pbm; tm = prm; bm = plm;
            break;
        case REVERSE_PORTRAIT: lm = prm; rm = plm; tm = pbm; bm = ptm;
            break;
        default: lm = plm; rm = prm; tm = ptm; bm = pbm;
        }
        lm *= 72;
        rm *= 72;
        tm *= 72;
        bm *= 72;
        return new PageLayout(paper, orient, lm, rm, tm, bm);
    }

    /**
     * Obtain a new PageLayout for this printer using the specified
     * parameters.
     * The paper should be one of the supported papers and
     * the orientation should be a supported orientation.
     * <p>
     * Margin values are specified in 1/72 of an inch points.
     * Margins will be validated against the printer supported margins,
     * and adjusted if necessary. This method is generally useful to
     * a client that wants margins that are different (eg wider)
     * than the default margins, such as 1" at top and bottom and
     * 0.5" to the left and right.
     * <p>A client that needs to know what margin values are legal should first
     * obtain a PageLayout using the <code>HARDWARE_MINIMUM</code> margins.
     * <p>
     * If the printer cannot support the layout as specified, it
     * will adjust the returned layout to a supported configuration
     * @param paper The paper to use
     * @param orient The orientation to use
     * @param lMargin the left margin to use in pts.
     * @param rMargin the right margin to use in pts.
     * @param tMargin the top margin to use in pts.
     * @param bMargin the bottom margin to use in pts.
     * @return PageLayout based on the specified parameters.
     * @throws NullPointerException if paper or orient are null.
     * @throws IllegalArgumentException if any of the margins values are
     * less than zero.
     */
    public PageLayout createPageLayout(Paper paper, PageOrientation orient,
                                       double lMargin, double rMargin,
                                       double tMargin, double bMargin) {

        if (paper == null || orient == null) {
            throw new NullPointerException("Parameters cannot be null");
        }
        if (lMargin < 0 || rMargin < 0 || tMargin < 0 || bMargin < 0) {
            throw new IllegalArgumentException("Margins must be >= 0");
        }
        // TBD: Adjust paper to a supported one first.
        Rectangle2D imgArea = impl.printableArea(paper);
        double width = paper.getWidth() / 72.0;
        double height = paper.getHeight() / 72.0;
        double plm = imgArea.getMinX();
        double ptm = imgArea.getMinY();
        double prm = width - imgArea.getMaxX();
        double pbm = height - imgArea.getMaxY();

        lMargin /= 72.0;
        rMargin /= 72.0;
        tMargin /= 72.0;
        bMargin /= 72.0;

        // Check if the requested margins exceed the paper and
        // if they do, ignore them.
        boolean useDefault = false;
        if (orient == PORTRAIT || orient == REVERSE_PORTRAIT) {
            if ((lMargin + rMargin > width) ||
                (tMargin + bMargin > height)) {
                useDefault = true;
            }
        } else {
            if ((lMargin + rMargin > height) ||
                (tMargin + bMargin > width)) {
                useDefault = true;
            }
        }
        if (useDefault) {
            return createPageLayout(paper, orient, MarginType.DEFAULT);
        }

        double lm, rm, tm, bm;
        // TBD: Adjust orient to a supported one first.
        switch (orient) {
        case LANDSCAPE: lm = pbm; rm = ptm; tm = plm; bm = prm;
            break;
        case REVERSE_LANDSCAPE: lm = ptm; rm = pbm; tm = prm; bm = plm;
            break;
        case REVERSE_PORTRAIT: lm = prm; rm = plm; tm = pbm; bm = ptm;
            break;
        default: lm = plm; rm = prm; tm = ptm; bm = pbm;
        }

        lm = (lMargin >= lm) ? lMargin : lm;
        rm = (rMargin >= rm) ? rMargin : rm;
        tm = (tMargin >= tm) ? tMargin : tm;
        bm = (bMargin >= bm) ? bMargin : bm;

        lm *= 72;
        rm *= 72;
        tm *= 72;
        bm *= 72;

        return new PageLayout(paper, orient, lm, rm, tm, bm);
    }

    @Override public String toString() {
        return "Printer " + getName();
    }

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        PrintHelper.setPrintAccessor(new PrintHelper.PrintAccessor() {

            @Override
            public PrintResolution createPrintResolution(int fr, int cfr) {
                return new PrintResolution(fr, cfr);
            }

            @Override
            public Paper createPaper(String paperName,
                                     double paperWidth,
                                     double paperHeight,
                                     Units units) {
                return new Paper(paperName, paperWidth, paperHeight, units);
            }

            @Override
            public PaperSource createPaperSource(String name) {
                return new PaperSource(name);
            }

            @Override
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
            @Override
            public Printer createPrinter(PrinterImpl impl) {
                return new Printer(impl);
            }

            @Override
            public PrinterImpl getPrinterImpl(Printer printer) {
                return printer.getPrinterImpl();
            }
        });
    }
}
