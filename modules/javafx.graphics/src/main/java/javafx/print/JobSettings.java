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

package javafx.print;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * The JobSettings class encapsulates most of the configuration of a
 * print job. Applications do not - and cannot - directly create or
 * set a JobSettings instance. One is already installed on the print job
 * when it is created.
 * <p>
 * As documented on PrinterJob, the JobSettings installed on that job will
 * initially reflect the current default settings for the initially
 * associated printer for that job.
 * <p>
 * The JobSettings delegate then remains the same for the life of the job,
 * and will have it's member properties updated to be compatible with
 * a change in Printer on the job. For example as a
 * result of a user interaction via a platform's dialog.
 * An incompatible setting will usually cause the setting to revert to
 * the default for the new printer.
 * <p>
 * Any implicit or explicit updates to settings resulting from
 * the user interaction with dialog will be propagated and visible to
 * the application once the user approves the settings by
 * dismissing the dialog using its "accept" option.
 * <p>
 * For most printing applications it is likely sufficient to let the user
 * set the desired options and have these propagated to the job.
 * For applications which need them,
 * there are setter and getter methods for the individual options,
 * which are also available as properties, and change in values of
 * settings may be monitored and updated via these properties.
 * <p>
 * Not all values of settings are available on all printers. For example
 * a printer may not support two-sided printing.
 * See the {@link javafx.print.Printer Printer} class for how to
 * to determine supported settings.
 *
 * @since JavaFX 8.0
 */
public final class JobSettings {

    private PrinterJob job;
    private Printer printer;
    private PrinterAttributes printerCaps;

    /*
     * There's no need for client code to create a JobSettings
     * as there is already one set on a PrinterJob and it must
     * be updated, not replaced. So we do not expose the constructor.
     *
     */
    JobSettings(Printer printer) {
        this.printer = printer;
        printerCaps = printer.getPrinterAttributes();
    }

    void setPrinterJob(PrinterJob job) {
        this.job = job;
    }

    private boolean isJobNew() {
        // If we haven't yet set the job its equivalent to a new job.
        return job == null || job.isJobNew();
    }


    /*
     * We need to be able to distinguish settings which are
     * the printer defaults, versus explicitly set ones. so
     * the settings object remembers for each setting if it
     * was an explicit setting or a default.
     * Settings such as JobName which are supportable across printers
     * don't need this treatment.
     */

    private boolean defaultCopies = true;
    private boolean hasOldCopies = false;
    private int oldCopies;

    private boolean defaultSides = true;
    private boolean hasOldSides = false;
    private PrintSides oldSides;

    private boolean defaultCollation = true;
    private boolean hasOldCollation = false;
    private Collation oldCollation;

    private boolean defaultPrintColor = true;
    private boolean hasOldPrintColor = false;
    private PrintColor oldPrintColor;

    private boolean defaultPrintQuality = true;
    private boolean hasOldPrintQuality = false;
    private PrintQuality oldPrintQuality;

    private boolean defaultPrintResolution = true;
    private boolean hasOldPrintResolution = false;
    private PrintResolution oldPrintResolution;

    private boolean defaultPaperSource = true;
    private boolean hasOldPaperSource = false;
    private PaperSource oldPaperSource;

    private boolean defaultPageLayout = true;
    private boolean hasOldPageLayout = false;
    private PageLayout oldPageLayout;

    /**
     * If any settings are incompatible with the specified printer,
     * they are updated to be compatible.
     * This method could be useful as a public one.
     */
    void updateForPrinter(Printer printer) {

        this.printer = printer;
        this.printerCaps = printer.getPrinterAttributes();

        ////////////////////////////////////////////////

        /*
         * The algorithm for how we update is tricky in order to get
         * the desired behaviour, which needs to be explained first.
         * - If neither user nor code ever set a value for a property,
         * we always update to the default for the new printer.
         * - If user or code has ever explicitly set a value for a
         * property then when we navigate to a different printer we
         * remember that last set value.
         * - If the new printer can support that value, it is used
         * - If it can't, then we use the default value for that printer,
         * or in a few cases some 'close' value. Such as substituting
         * LANDSCAPE for REVERSE_LANDSCAPE.
         * - We still remember the value and if we move to another printer
         * that supports it, it gets resurrected.
         */

        //////////// COPIES ////////////

        if (defaultCopies) {
            if (getCopies() != printerCaps.getDefaultCopies()) {
                setCopies(printerCaps.getDefaultCopies());
                defaultCopies = true; // restore that this is default.
            }
        } else {
            int copies = getCopies();
            if (hasOldCopies && oldCopies > copies) {
                copies = oldCopies;
            }
            int maxCopies = printerCaps.getMaxCopies();
            if (!hasOldCopies && getCopies() > maxCopies) {
                hasOldCopies = true;
                oldCopies = getCopies();
            }
            if (copies > maxCopies) copies = maxCopies;
            setCopies(copies);
        }


        ////////////////////////////////////////////////

        PrintSides currSides = getPrintSides();
        PrintSides defSides = printerCaps.getDefaultPrintSides();
        Set<PrintSides> suppSides = printerCaps.getSupportedPrintSides();

        if (defaultSides) {
            if (currSides != defSides) {
                setPrintSides(defSides);
                defaultSides = true;  // restore that this is default.
            }
        } else {
            // The "current" sides may not be the one we really wanted,
            // but one that was forced by virtue of the current printer not
            // supporting the one specified for a previous printer.
            // Check to see if the old supported value can now
            // be restored on this new printer.
            if (hasOldSides) {
                if (suppSides.contains(oldSides)) {
                    setPrintSides(oldSides);
                    hasOldSides = false; // is current again.
                } else {
                    setPrintSides(defSides);
                }
            } else if (!suppSides.contains(currSides)) {
                    hasOldSides = true;
                    oldSides = currSides;
                    setPrintSides(defSides);
            }
        }

        ////////////////////////////////////////////////

        Collation currColl = getCollation();
        Collation defColl = printerCaps.getDefaultCollation();
        Set<Collation> suppColl = printerCaps.getSupportedCollations();

        if (defaultCollation) {
            if (currColl != defColl) {
                setCollation(defColl);
                defaultCollation = true;  // restore that this is default.
            }
        } else {
            if (hasOldCollation) {
                if (suppColl.contains(oldCollation)) {
                    setCollation(oldCollation);
                    hasOldCollation = false; // is current again.
                } else {
                    setCollation(defColl);
                }
            } else if (!suppColl.contains(currColl)) {
                    hasOldCollation = true;
                    oldCollation = currColl;
                    setCollation(defColl);
            }
        }


        ////////////////////////////////////////////////

        PrintColor currColor = getPrintColor();
        PrintColor defColor = printerCaps.getDefaultPrintColor();
        Set<PrintColor> suppColors = printerCaps.getSupportedPrintColors();

        if (defaultPrintColor) {
            if (currColor != defColor) {
                setPrintColor(defColor);
                defaultPrintColor = true; // restore that this is default.
            }
        } else {
            if (hasOldPrintColor) {
                if (suppColors.contains(oldPrintColor)) {
                    setPrintColor(oldPrintColor);
                    hasOldPrintColor = false; // is current again.
                } else {
                    setPrintColor(defColor);
                }
            } else if (!suppColors.contains(currColor)) {
                hasOldPrintColor = true;
                oldPrintColor = currColor;
                setPrintColor(defColor);
            }
        }

        ////////////////////////////////////////////////

        PrintQuality currQuality = getPrintQuality();
        PrintQuality defQuality = printerCaps.getDefaultPrintQuality();
        Set<PrintQuality> suppQuality = printerCaps.getSupportedPrintQuality();

        if (defaultPrintQuality) {
            if (currQuality != defQuality) {
                setPrintQuality(defQuality);
                defaultPrintQuality = true; // restore that this is default.
            }
        } else {
            if (hasOldPrintQuality) {
                if (suppQuality.contains(oldPrintQuality)) {
                    setPrintQuality(oldPrintQuality);
                    hasOldPrintQuality = false; // is current again.
                } else {
                    setPrintQuality(defQuality);
                }
            } else if (!suppQuality.contains(currQuality)) {
                hasOldPrintQuality = true;
                oldPrintQuality = currQuality;
                setPrintQuality(defQuality);
            }
        }

        ////////////////////////////////////////////////

       PrintResolution currRes = getPrintResolution();
       PrintResolution defResolution = printerCaps.getDefaultPrintResolution();
       Set<PrintResolution> suppRes =
           printerCaps.getSupportedPrintResolutions();

       if (defaultPrintResolution) {
           if (currRes != defResolution) {
               setPrintResolution(defResolution);
               defaultPrintResolution = true; // restore that this is default.
            }
        } else {
            if (hasOldPrintResolution) {
                if (suppRes.contains(oldPrintResolution)) {
                    setPrintResolution(oldPrintResolution);
                    hasOldPrintResolution = false; // is current again.
                } else {
                    setPrintResolution(defResolution);
                }
            } else if (!suppRes.contains(currRes)) {
                hasOldPrintResolution = true;
                oldPrintResolution = currRes;
                setPrintResolution(defResolution);
            }
        }

       ////////////////////////////////////////////////


       PaperSource currSource = getPaperSource();
       PaperSource defSource = printerCaps.getDefaultPaperSource();
       Set<PaperSource> suppSources = printerCaps.getSupportedPaperSources();

       if (defaultPaperSource) {
           if (currSource != defSource) {
               setPaperSource(defSource);
               defaultPaperSource = true; // restore that this is default.
            }
        } else {
            if (hasOldPaperSource) {
                if (suppSources.contains(oldPaperSource)) {
                    setPaperSource(oldPaperSource);
                    hasOldPaperSource = false; // is current again.
                } else {
                    setPaperSource(defSource);
                }
            } else if (!suppSources.contains(currSource)) {
                hasOldPaperSource = true;
                oldPaperSource = currSource;
                setPaperSource(defSource);
            }
        }

       ///////////////////////////////////////////////////

       // Paper size is an important component of PageLayout
       // and the selected paper input tray may affect the paper sizes
       // that are supported. For example non-standard sizes may
       // require a manual or secondary input tray to be used.
       // The implementation below is not accounting for that and
       // it likely should, which means that when updating to a new
       // printer we need to consider tray and layout together, not
       // consecutively.

       PageLayout currPageLayout = getPageLayout();
       PageLayout defPageLayout = printer.getDefaultPageLayout();

       if (defaultPageLayout) {
           // It might appear cleaner to always set the default page layout
           // instance for this printer, but it if they are equal, then
           // either set(..) will skip it anyway, or it won't and a
           // listener will see a somewhat spurious change in the property.
           if (!currPageLayout.equals(defPageLayout)) {
               setPageLayout(defPageLayout);
               defaultPageLayout = true; // restore that this is default.
            }
        } else {
            if (hasOldPageLayout) {
                PageLayout valPageLayout =
                    job.validatePageLayout(oldPageLayout);
                if (valPageLayout.equals(oldPageLayout)) {
                    setPageLayout(oldPageLayout);
                    hasOldPageLayout = false; // is current again.
                } else {
                    setPageLayout(defPageLayout);
                }
            } else {
                PageLayout valPageLayout =
                    job.validatePageLayout(currPageLayout);
                if (!valPageLayout.equals(currPageLayout)) {
                    hasOldPageLayout = true;
                    oldPageLayout = currPageLayout;
                    // Should I use the validated layout instead ?
                    setPageLayout(defPageLayout);
                }
            }
        }
    }

    ///////////////////////  START JOBNAME /////////////////////

    private static final String DEFAULT_JOBNAME = "JavaFX Print Job";
    private SimpleStringProperty jobName;

    /**
     * <code>StringProperty</code> representing the name of a job.
     * @return the name of a job
     */
    public final StringProperty jobNameProperty() {
        if (jobName == null) {
            jobName = new SimpleStringProperty(JobSettings.this, "jobName",
                                               DEFAULT_JOBNAME) {

                @Override
                public void set(String value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        value = DEFAULT_JOBNAME;
                    }
                    super.set(value);
                }

                @Override
                public void bind(ObservableValue<? extends String>
                                 rawObservable) {
                    throw new
                        RuntimeException("Jobname property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<String> other) {
                    throw new
                        RuntimeException("Jobname property cannot be bound");
                }
            };
        }
        return jobName;
    }

    /**
     * Get the name of a job.
     * @return a string representing the name of a job
     */
    public String getJobName() {
        return jobNameProperty().get();
    }


    /**
     * Set the name of a job.
     * @param name string representing the name of a job
     */
    public void setJobName(String name) {
        jobNameProperty().set(name);
    }
    ///////////////////////  END JOBNAME /////////////////////

    ///////////////////////  START OUTPUTFILE /////////////////////

    private SimpleStringProperty outputFile;

    /**
     * A {@code StringProperty} representing the
     * name of a filesystem file, to which the platform printer
     * driver should spool the rendered print data.
     * <p>
     * Applications can use this to programmatically request print-to-file
     * behavior where the native print system is capable of spooling the
     * output to a filesystem file, rather than the printer device.
     * <p>
     * This is often useful where the printer driver generates a format
     * such as Postscript or PDF, and the application intends to distribute
     * the result instead of printing it, or for some other reason the
     * application does not want physical media (paper) emitted by the printer.
     * <p>
     * The default value is an empty string, which is interpreted as unset,
     * equivalent to null, which means output is sent to the printer.
     * So in order to reset to print to the printer, clear the value of
     * this property by setting it to null or an empty string.
     * <p>
     * Additionally if the application displays a printer dialog which allows
     * the user to specify a file destination, including altering an application
     * specified file destination, the value of this property will reflect that
     * user-specified choice, including clearing it to reset to print to
     * the printer, if the user does so.
     * <p>
     * If the print system does not support print-to-file, then this
     * setting will be ignored.
     * <p>
     * If the specified name specifies a non-existent path, or does not specify
     * a user writable file, when printing the results are platform-dependent.
     * Possible behaviours might include replacement with a default output file location,
     * printing to the printer instead, or a platform printing error.
     * If a {@code SecurityManager} is installed and it denies access to the
     * specified file a {@code SecurityException} may be thrown.
     *
     * @defaultValue an empty string
     *
     * @return the name of a printer spool file
     * @since 17
     */
    public final StringProperty outputFileProperty() {
        if (outputFile == null) {
            outputFile =
                new SimpleStringProperty(JobSettings.this, "outputFile", "") {

                @Override
                public void set(String value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        value = "";
                    }
                    super.set(value);
                }

                @Override
                public void bind(ObservableValue<? extends String>
                                 rawObservable) {
                    throw new
                        RuntimeException("OutputFile property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<String> other) {
                    throw new
                        RuntimeException("OutputFile property cannot be bound");
                }

                @Override
                public String toString() {
                     return get();
                }
            };
        }
        return outputFile;
    }

    public String getOutputFile() {
        return outputFileProperty().get();
    }


    public void setOutputFile(String filePath) {
        outputFileProperty().set(filePath);
    }
    ///////////////////////  END OUTPUTFILE /////////////////////

    //////////////////////// START COPIES ////////////////////////

    private IntegerProperty copies;

    /**
     * <code>IntegerProperty</code> representing the number of
     * copies of the job to print.
     * @return the number of copies of the job to print
     */
    public final IntegerProperty copiesProperty() {
        if (copies == null) {
            copies =
                new SimpleIntegerProperty(JobSettings.this, "copies",
                                          printerCaps.getDefaultCopies()) {

                @Override
                public void set(int value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value <= 0) {
                        if (defaultCopies) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultCopies());
                            defaultCopies = true;
                            return;
                        }
                    }
                    super.set(value);
                    defaultCopies = false;
                }

                @Override
                public void bind(ObservableValue<? extends Number>
                                 rawObservable) {
                    throw new
                        RuntimeException("Copies property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<Number> other) {
                    throw new
                        RuntimeException("Copies property cannot be bound");
                }
            };
        }
        return copies;
    }

    /**
     * Get the number of copies to print.
     * @return number of copies to print
     */
    public int getCopies() {
         return copiesProperty().get();
    }

    /**
     * Set the number of copies to print.
     * @param nCopies number of copies to print
     */
    public final void setCopies(int nCopies) {
        copiesProperty().set(nCopies);
    }

    //////////////////////// END COPIES ////////////////////////


    ///////////////////////  START PAGE RANGES /////////////////////
    private ObjectProperty<PageRange[]> pageRanges = null;

    /**
     * An <code>ObjectProperty</code> whose value represents the job pages
     * to print as an array of PageRange.
     * A null values mean print all pages.
     * Otherwise it must be a non-overlapping array of PageRange
     * instances ordered in increasing page number.
     * Page numbers start from 1 (one).
     * An empty array is considered equivalent to a null array.
     * <p>
     * An illegal or unsupported (by the printer) set of page ranges
     * will be ignored.
     * <p>
     * Ranges which exceed beyond the number of pages imaged by the job
     * during printing do not cause any error.
     *
     * @return the value presents the job pages to print as an array of PageRange
     */
    public final ObjectProperty pageRangesProperty() {
         if (pageRanges == null) {
            pageRanges = new SimpleObjectProperty(JobSettings.this,
                                                  "pageRanges", null) {

                @Override
                public void set(Object o) {
                    try {
                        set((PageRange[])o);
                    } catch (ClassCastException e) {
                        return;
                    }
                }

                public void set(PageRange[] value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null || value.length == 0 ||
                        value[0] == null) {
                        value = null;
                    } else { // validate
                        int len = value.length;
                        PageRange[] arr = new PageRange[len];
                        int curr = 0;
                        for (int i=0; i<len; i++) {
                            PageRange r = value[i];
                            if (r == null || curr >= r.getStartPage()) {
                                return; // bad range
                            }
                            curr = r.getEndPage();
                            arr[i] = r;
                        }
                        value = arr;
                    }
                    // passed validation so its either null,
                    // or an array of ranges in increasing order.
                    super.set(value);
                }

                @Override
                public void bind(ObservableValue rawObservable) {
                    throw new RuntimeException
                        ("PageRanges property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property other) {
                    throw new RuntimeException
                        ("PageRanges property cannot be bound");
                }
            };
        }
        return pageRanges;
    }

    /**
     * The range of pages to print. null always means all pages.
     * See {@link pageRangesProperty} for more details.
     * @return null or an array as specified above
     */
    public PageRange[] getPageRanges() {
        return (PageRange[])(pageRangesProperty().get());
    }

    /**
     * The range of pages to print as an array of PageRange.
     * The use of varargs means the common case of a single range
     * can be auto-boxed.
     * <code>((PageRange[])null)</code> always means all pages however
     * since this is the default it is less likely to be used.
     * See {@link pageRangesProperty} for more details.
     * @param pages null or a varargs array as specified above
     */
    public void setPageRanges(PageRange... pages) {
        pageRangesProperty().set((PageRange[])pages);
    }

    ///////////////////////  END PAGE RANGES /////////////////////


    ///////////////////////  START SIDES  /////////////////////

    private ObjectProperty<PrintSides> sides = null;

    /**
     * Property representing an instance of <code>PrintSides</code>.
     * @return an instance of <code>PrintSides</code>
     */
    public final ObjectProperty<PrintSides> printSidesProperty() {
         if (sides == null) {
             sides = new SimpleObjectProperty<PrintSides>
                 (JobSettings.this, "printSides",
                  printerCaps.getDefaultPrintSides()) {

                @Override
                public void set(PrintSides value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultSides) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultPrintSides());
                            defaultSides = true;
                        }
                    }
                    if (printerCaps.getSupportedPrintSides().contains(value)) {
                        super.set(value);
                        defaultSides = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends PrintSides>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PrintSides property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PrintSides> other) {
                    throw new RuntimeException
                        ("PrintSides property cannot be bound");
                }
            };
        }
        return sides;
    }

    /**
     * If a printer supports it, then a job may be printed on
     * both sides of the media (paper), ie duplex printing.
     * This method returns the selected setting.
     * @return the duplex (side) setting.
     */
    public PrintSides getPrintSides() {
        return printSidesProperty().get();
    }

    /**
     * Set the <code>PrintSides</code> property which controls
     * duplex printing.
     * A null value is ignored.
     * @param sides new setting for number of sides.
     */
    public void setPrintSides(PrintSides sides) {
        if (sides == getPrintSides()) {
            return;
        }
        printSidesProperty().set(sides);
    }
    ///////////////////////  END SIDES  /////////////////////


    ///////////////////////  START COLLATION /////////////////////

    private ObjectProperty<Collation> collation = null;

    /**
     * Property representing an instance of <code>Collation</code>.
     * @return an instance of <code>Collation</code>
     */
    public final ObjectProperty<Collation> collationProperty() {
         if (collation == null) {
             Collation coll = printerCaps.getDefaultCollation();
             collation = new SimpleObjectProperty<Collation>
                (JobSettings.this, "collation", coll) {

                @Override
                public void set(Collation value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultCollation) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultCollation());
                            defaultCollation = true;
                            return;
                        }
                    }
                    if (printerCaps.getSupportedCollations().contains(value)) {
                        super.set(value);
                        defaultCollation = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends Collation>
                                 rawObservable) {
                    throw new RuntimeException
                        ("Collation property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<Collation> other) {
                    throw new RuntimeException
                        ("Collation property cannot be bound");
                }
            };
        }
        return collation;
    }

    /**
     * Collation determines how sheets are sorted when
     * multiple copies of a document are printed.
     * As such it is only relevant if 2 or more copies of
     * a document with 2 more sheets are printed.
     * A sheet is the physical media, so documents with 2 pages
     * that are printed N-up, or double-sided may still have only
     * one sheet.
     * A collated print job produces documents with sheets
     * of a document sorted in sequence.
     * An uncollated job collects together the multiple copies
     * of the same sheet.
     * Uncollated (<code>false</code>) is the typical default value.
     *
     * @return the collation
     */
    public Collation getCollation() {
        return collationProperty().get();
    }

    /**
     * Set the <code>Collation</code> property.
     * A null value is ignored.
     * @param collation new setting for collation
     */
    public void setCollation(Collation collation) {
        if (collation == getCollation()) {
            return;
        }
        collationProperty().set(collation);
    }

    ///////////////////////  END COLLATION /////////////////////

    ///////////////////////  START COLOUR /////////////////////

    private ObjectProperty<PrintColor> color = null;

    /**
     * Property representing an instance of <code>PrintColor</code>.
     * @return an instance of <code>PrintColor</code>
     */
    public final ObjectProperty<PrintColor> printColorProperty() {
         if (color == null) {
            color = new SimpleObjectProperty<PrintColor>
                (JobSettings.this, "printColor",
                 printerCaps.getDefaultPrintColor()) {

                @Override
                public void set(PrintColor value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultPrintColor) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultPrintColor());
                            defaultPrintColor = true;
                        }
                    }
                    if (printerCaps.
                        getSupportedPrintColors().contains(value)) {
                        super.set(value);
                        defaultPrintColor = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends PrintColor>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PrintColor property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PrintColor> other) {
                    throw new RuntimeException
                        ("PrintColor property cannot be bound");
                }
            };
        }
        return color;
    }

    public PrintColor getPrintColor() {
        return printColorProperty().get();
    }

    /**
     * Set the <code>PrintColor</code> property.
     * A null value is ignored.
     *
     * @param color new setting for print color.
     */
    public void setPrintColor(PrintColor color) {
        if (color == getPrintColor()) {
            return;
        }
        printColorProperty().set(color);
    }

    ///////////////////////  END COLOUR /////////////////////

    ///////////////////////  START QUALITY /////////////////////

    private ObjectProperty<PrintQuality> quality = null;

    /**
     * Property representing an instance of <code>PrintQuality</code>.
     * @return an instance of <code>PrintQuality</code>
     */
    public final ObjectProperty<PrintQuality> printQualityProperty() {
         if (quality == null) {
            quality = new SimpleObjectProperty<PrintQuality>
                (JobSettings.this, "printQuality",
                 printerCaps.getDefaultPrintQuality()) {

                @Override
                public void set(PrintQuality value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultPrintQuality) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultPrintQuality());
                            defaultPrintQuality = true;
                        }
                    }
                    if (printerCaps.
                        getSupportedPrintQuality().contains(value)) {
                        super.set(value);
                        defaultPrintQuality = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends PrintQuality>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PrintQuality property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PrintQuality> other) {
                    throw new RuntimeException
                        ("PrintQuality property cannot be bound");
                }
            };
        }
        return quality;
    }

    public PrintQuality getPrintQuality() {
        return  printQualityProperty().get();
    }

    /**
     * Set the <code>PrintQuality</code> property.
     * A null value is ignored.
     * <p>
     * Note that quality and resolution overlapping concepts.
     * Therefore a printer may support setting one, or the other but
     * not both. Applications setting these programmatically should
     * query both properties and select appropriately from the supported
     * values. If a printer supports non-standard values, code likely
     * cannot distinguish the printer's interpretation of these values
     * and is safest to stick to selecting from the standard value that
     * matches the requirement.
     * @param quality new setting for print quality.
     */
    public void setPrintQuality(PrintQuality quality) {
        if (quality == getPrintQuality()) {
            return;
        }
        printQualityProperty().set(quality);
    }

    ///////////////////////  END QUALITY /////////////////////

    ///////////////////////  START RESOLUTION /////////////////////


    private ObjectProperty<PrintResolution> resolution = null;

    /**
     * Property representing an instance of <code>PrintResolution</code>.
     * @return an instance of <code>PrintResolution</code>
     */
    public final ObjectProperty<PrintResolution> printResolutionProperty() {
         if (resolution == null) {
            resolution = new SimpleObjectProperty<PrintResolution>
                (JobSettings.this, "printResolution",
                 printerCaps.getDefaultPrintResolution()) {

                @Override
                public void set(PrintResolution value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultPrintResolution) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultPrintResolution());
                            defaultPrintResolution = true;
                        }
                    }
                    if (printerCaps.getSupportedPrintResolutions().
                        contains(value))
                    {
                        super.set(value);
                        defaultPrintResolution = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends PrintResolution>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PrintResolution property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PrintResolution> other)
                {
                    throw new RuntimeException
                        ("PrintResolution property cannot be bound");
                }
            };
        }
        return resolution;
    }

    /**
     *
     * @return the print resolution
     */
    public PrintResolution getPrintResolution() {
        return printResolutionProperty().get();
    }

    /**
     * Set the <code>PrintResolution</code> property.
     * A null value is ignored.
     * <p>
     * Note that quality and resolution overlapping concepts.
     * Therefore a printer may support setting one, or the other but
     * not both. Applications setting these programmatically should
     * query both properties and select appropriately from the supported
     * values. If a printer supports non-standard values, code likely
     * cannot distinguish the printer's interpretation of these values
     * and is safest to stick to selecting from the standard value that
     * matches the requirement.
     * @param resolution new setting for print resolution.
     */
    public void setPrintResolution(PrintResolution resolution) {
        if (resolution == null || resolution == getPrintResolution()) {
            return;
        }
        printResolutionProperty().set(resolution);
    }

    ///////////////////////  END RESOLUTION /////////////////////

    //////////////// START PAPERSOURCE /////////////////

    private ObjectProperty<PaperSource> paperSource = null;


    /**
     * Property representing an instance of <code>PaperSource</code>.
     * @return an instance of <code>PaperSource</code>
     */
    public final ObjectProperty<PaperSource> paperSourceProperty() {
         if (paperSource == null) {
            paperSource = new SimpleObjectProperty<PaperSource>
                (JobSettings.this, "paperSource",
                 printerCaps.getDefaultPaperSource()) {

                @Override
                public void set(PaperSource value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        if (defaultPaperSource) {
                            return;
                        } else {
                            super.set(printerCaps.getDefaultPaperSource());
                            defaultPaperSource = true;
                        }
                    }
                    if (printerCaps.
                        getSupportedPaperSources().contains(value)) {
                        super.set(value);
                        defaultPaperSource = false;
                    }
                }

                @Override
                public void bind(ObservableValue<? extends PaperSource>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PaperSource property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PaperSource> other) {
                    throw new RuntimeException
                        ("PaperSource property cannot be bound");
                }
            };
        }
        return paperSource;
    }


    public PaperSource getPaperSource() {
        return paperSourceProperty().get();
    }

    public void setPaperSource(PaperSource value) {
        paperSourceProperty().set(value);
    }

    //////////////// END PAPERSOURCE /////////////////

    //////////////// START PAGELAYOUT /////////////////

    /*
     * Default page needs to originate from the printer, be the
     * default for the initial settings on the job.

     * having "page" be a property might make sense and perhaps
     * for the elts of a paper like orientation.
     * But what is the point in listening in on the paper of a Page
     * or orientation if a whole new Page is added instead ..
     */

    private ObjectProperty<PageLayout> layout = null;

    /**
     * Property representing an instance of <code>PageLayout</code>.
     * @return an instance of <code>PageLayout</code>
     */
    public final ObjectProperty<PageLayout> pageLayoutProperty() {
         if (layout == null) {
             layout = new SimpleObjectProperty<PageLayout>
                 (JobSettings.this, "pageLayout",
                  printer.getDefaultPageLayout()) {

                @Override
                public void set(PageLayout value) {
                    if (!isJobNew()) {
                        return;
                    }
                    if (value == null) {
                        return;
                    }
                    defaultPageLayout = false;
                    super.set(value);
                }

                @Override
                public void bind(ObservableValue<? extends PageLayout>
                                 rawObservable) {
                    throw new RuntimeException
                        ("PageLayout property cannot be bound");
                }

                @Override
                public void bindBidirectional(Property<PageLayout> other) {
                    throw new RuntimeException
                        ("PageLayout property cannot be bound");
                }
            };
        }
        return layout;
    }

    /**
     * Get the current page layout for this job.
     * @return page layout to use for the job.
     */
    public PageLayout getPageLayout() {
        return pageLayoutProperty().get();
    }

    /**
     * Set the PageLayout to use.
     * @param pageLayout The page layout to use.
     */
    public void setPageLayout(PageLayout pageLayout) {
        pageLayoutProperty().set(pageLayout);
    }

    //////////////// END PAGELAYOUT /////////////////

    @Override
    public String toString() {
        String nl = System.lineSeparator();
        return
            " Collation = " + getCollation() + nl +
            " Copies = " + getCopies() + nl +
            " Sides = " + getPrintSides() + nl +
            " JobName = " + getJobName() + nl +
            " Page ranges = " + getPageRanges() + nl +
            " Print color = " + getPrintColor() + nl +
            " Print quality = " + getPrintQuality() + nl +
            " Print resolution = " + getPrintResolution() + nl +
            " Paper source = " + getPaperSource() + nl +
            " Page layout = " + getPageLayout();
    }
}
