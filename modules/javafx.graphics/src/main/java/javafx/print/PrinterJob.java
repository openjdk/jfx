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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.stage.Window;

import com.sun.javafx.print.PrinterJobImpl;
import com.sun.javafx.tk.PrintPipeline;

/**
 * PrinterJob is the starting place for JavaFX scenegraph printing.
 * <p>
 * It includes
 * <ul>
 * <li>Printer discovery
 * <li>Job creation
 * <li>Job configuration based on supported printer capabilities
 * <li>Page setup
 * <li>Rendering of a node hierachy to a page.
 * </ul>
 * <p>
 * Here ia a very simple example, which prints a single node.
 * <pre>
 * Node node = new Circle(100, 200, 200);
 * PrinterJob job = PrinterJob.createPrinterJob();
 * if (job != null) {
 *    boolean success = job.printPage(node);
 *    if (success) {
 *        job.endJob();
 *    }
 * }
 * </pre>
 * <b>Points to note</b>
 * <p>
 * In the example above the node was not added to a scene.
 * Since most printing scenarios are printing content that's either
 * not displayed at all, or must be prepared and formatted differently,
 * this is perfectly acceptable.
 * <p>
 * If content that is currently part of a Scene and is being displayed,
 * is printed, then because printing a job or even a single page
 * of the job may span over multiple screen "pulses" or frames, it is
 * important for the application to ensure that the node being printed
 * is not updated during the printing process, else partial or smeared
 * rendering is probable.
 * <p>
 * It should be apparent that the same applies even to nodes that are
 * not displayed - updating them concurrent with printing them is not
 * a good idea.
 * <p>
 * There is no requirement to do printing on the FX application thread.
 * A node may be prepared for printing on any thread, the job may
 * be invoked on any thread. However, minimising the amount of work
 * done on the FX application thread is generally desirable,
 * so as not to affect the responsiveness of the application UI.
 * So the recommendation is to perform printing on a new thread
 * and let the implementation internally schedule any tasks that
 * need to be performed on the FX thread to be run on that thread.
 *
 * @since JavaFX 8.0
 */

public final class PrinterJob {

    // Delegating all the work keeps whatever classes
    // are being used out of the API packages.
    private PrinterJobImpl jobImpl;

    private ObjectProperty<Printer> printer;

    private JobSettings settings;

    /**
     * Factory method to create a job.
     * If there are no printers available, this will return null.
     * Some platforms may provide a pseudo printer, which creates
     * a document. These will be enumerated here so long as the
     * platform also enumerates them as if they are printers.
     * @return a new PrinterJob instance, or null.
     * @throws SecurityException if a job does not have permission
     * to initiate a printer job.
     */
    public static final PrinterJob createPrinterJob() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            return null;
        } else {
            return new PrinterJob(printer);
        }
    }

    /**
     * Factory method to create a job for a specified printer.
     * <p>
     * The <code>printer</code> argument determines the initial printer
     * @param printer to use for the job. If the printer is currently
     * unavailable (eg offline) then this may return null.
     * @return a new PrinterJob, or null.
     * @throws SecurityException if a job does not have permission
     * to initiate a printer job.
     */
    public static final PrinterJob createPrinterJob(Printer printer) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }
        return new PrinterJob(printer);
    }

    private PrinterJob(Printer printer) {

        this.printer = createPrinterProperty(printer);
        settings = printer.getDefaultJobSettings();
        settings.setPrinterJob(this);
        createImplJob(printer, settings);
    }

    synchronized private PrinterJobImpl createImplJob(Printer printer,
                                                      JobSettings settings) {
        if (jobImpl == null) {
            jobImpl = PrintPipeline.getPrintPipeline().createPrinterJob(this);
        }
        return jobImpl;
    }

    /**
     * Updating settings or printer is only allowed on a new job,
     * meaning before you start printing or cancel etc.
     * The implementation needs to check this wherever job state
     * updates are received.
     */
    boolean isJobNew() {
        return getJobStatus() == JobStatus.NOT_STARTED;
    }

    private ObjectProperty<Printer> createPrinterProperty(Printer printer) {

        return new SimpleObjectProperty<>(printer) {

            @Override
            public void set(Printer value) {
                if (value == get() || !isJobNew()) {
                    return;
                }
                if (value == null) {
                    value = Printer.getDefaultPrinter();
                }
                super.set(value);
                jobImpl.setPrinterImpl(value.getPrinterImpl());
                settings.updateForPrinter(value);
            }

            @Override
            public void bind(ObservableValue<? extends Printer> rawObservable) {
                throw new RuntimeException("Printer property cannot be bound");
            }

            @Override
            public void bindBidirectional(Property<Printer> other) {
                throw new RuntimeException("Printer property cannot be bound");
            }

            @Override
            public Object getBean() {
                return PrinterJob.this;
            }

            @Override
            public String getName() {
                return "printer";
            }
        };
    }

    /**
     * Property representing the {@code Printer} for this job.
     * When setting a printer which does not support the current job settings,
     * (for example if DUPLEX printing is requested but the new printer
     * does not support this), then the values are reset to the default
     * for the new printer, or in some cases a similar value. For example
     * this might mean REVERSE_LANDSCAPE is updated to LANDSCAPE, however
     * this implementation optimisation is allowed, but not required.
     * <p>
     * The above applies whether the printer is changed by directly calling
     * this method, or as a side-effect of user interaction with a print
     * dialog.
     * <p>
     * Setting a null value for printer will install the default printer.
     * Setting the current printer has no effect.
     * @return the {@code Printer} for this job
     */
    public final ObjectProperty<Printer> printerProperty() {
        /* The PrinterJob constructor always creates this property,
         * so it can be returned directly.
         */
        return printer;
    }

    public synchronized final Printer getPrinter() {
        return printerProperty().get();
    }

    public synchronized final void setPrinter(Printer printer) {
         printerProperty().set(printer);
    }

    /**
     * The <code>JobSettings</code> encapsulates all the API supported job
     * configuration options such as number of copies,
     * collation option, duplex option, etc.
     * The initial values are based on the current settings for
     * the initial printer.
     * @return current job settings.
     */
    public synchronized JobSettings getJobSettings() {
        return settings;
    }

    /**
     * Displays a Print Dialog.
     * Allow the user to update job state such as printer and settings.
     * These changes will be available in the appropriate properties
     * after the print dialog has returned.
     * The print dialog is also typically used to confirm the user
     * wants to proceed with printing. This is not binding on the
     * application but generally should be obeyed.
     * <p>
     * In the case that there is no UI available then this method
     * returns true, with no options changed, as if the user had
     * confirmed to proceed with printing.
     * <p>
     * If the job is not in a state to display the dialog, such
     * as already printing, cancelled or done, then the dialog will
     * not be displayed and the method will return false.
     * <p>
     * The window <code>owner</code> may be null, but
     * if it is a visible Window, it will be used as the parent.
     * <p>
     * This method may be called from any thread. If it is called from the
     * JavaFX application thread, then it must either be called from an input
     * event handler or from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     *
     * @param owner to which to block input, or null.
     * @return false if the user opts to cancel printing, or the job
     * is not in the new state. That is if it has already started,
     * has failed, or has been cancelled, or ended.
     * @throws IllegalStateException if this method is called during
     * animation or layout processing.
     */
    public synchronized boolean showPrintDialog(Window owner) {
        // TBD handle owner
        if (!isJobNew()) {
            return false;
        } else {
            return jobImpl.showPrintDialog(owner);
        }
    }

    /**
     * Displays a Page Setup dialog.
     * A page set up dialog is primarily to allow an end user
     * to configure the layout of a page. Paper size and orientation
     * are the most common and most important components of this.
     * <p>
     * This will display the most appropriate available dialog for
     * this purpose.
     * However there may be still be access to other settings,
     * including changing the current printer.
     * Therefore a side effect of this dialog display method may be to
     * update that and any other current job settings.
     * The method returns true if the user confirmed the dialog whether or
     * not any changes are made.
     * <p>
     * If the job is not in a state to display the dialog, such
     * as already printing, cancelled or done, then the dialog will
     * not be displayed and the method will return false.
     * <p>
     * The window <code>owner</code> may be null, but
     * if it is a visible Window, it will be used as the parent.
     * <p>
     * This method may be called from any thread. If it is called from the FX
     * application thread, then it must either be called from an input event
     * handler or from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     *
     * @param owner to block input, or null.
     * @return false if the user opts to cancel the dialog, or the job
     * is not in the new state. That is if it has already started,
     * has failed, or has been cancelled, or ended.
     * @throws IllegalStateException if this method is called during
     * animation or layout processing.
     */
    public synchronized boolean showPageSetupDialog(Window owner) {
        // TBD handle owner
        if (!isJobNew()) {
            return false;
        } else {
            return jobImpl.showPageDialog(owner);
        }
    }

    /**
     * This method can be used to check if a page configuration
     * is possible in the current job configuration. For example
     * if the specified paper size is supported. If the original
     * PageLayout is supported it will be returned. If not, a new PageLayout
     * will be returned that attempts to honour the supplied
     * PageLayout, but adjusted to match the current job configuration.
     * <p>
     * This method does not update the job configuration.
     * @param pageLayout to be validated
     * @return a <code>PageLayout</code> that is supported in the
     * current job configuration.
     * @throws NullPointerException if the pageLayout parameter is null.
     */
    synchronized PageLayout validatePageLayout(PageLayout pageLayout) {
        if (pageLayout == null) {
            throw new NullPointerException("pageLayout cannot be null");
        }
        return jobImpl.validatePageLayout(pageLayout);
    }

    /**
     * Print the specified node using the specified page layout.
     * The page layout will override the job default for this page only.
     * If the job state is CANCELED, ERROR or DONE, this method will
     * return false.
     * <p>
     * This method may be called from any thread. If it is called from the FX
     * application thread, then it must either be called from an input event
     * handler or from the run method of a Runnable passed to
     * {@link javafx.application.Platform#runLater Platform.runLater}.
     * It must not be called during animation or layout processing.
     *
     * @param pageLayout Layout for this page.
     * @param node The node to print.
     * @return whether rendering was successful.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalStateException if this method is called during
     * animation or layout processing.
     */
    public synchronized boolean printPage(PageLayout pageLayout, Node node) {
        if (jobStatus.get().ordinal() > JobStatus.PRINTING.ordinal()) {
            return false;
        }
        if (jobStatus.get() == JobStatus.NOT_STARTED) {
            jobStatus.set(JobStatus.PRINTING);
        }
        if (pageLayout == null || node == null) {
            jobStatus.set(JobStatus.ERROR);
            throw new NullPointerException("Parameters cannot be null");
        }
        boolean rv = jobImpl.print(pageLayout, node);
        if (!rv) {
            jobStatus.set(JobStatus.ERROR);
        }
        return rv;
    }

     /**
     * Print the specified node. The page layout is the job default.
     * If the job state is CANCELED, ERROR or DONE, this method will
     * return false.
     * @param node The node to print.
     * @return whether rendering was successful.
     * @throws NullPointerException if the node parameter is null.
     */
    public synchronized boolean printPage(Node node) {
        return printPage(settings.getPageLayout(), node);
    }

    /**
     * An enum class used in reporting status of a print job.
     * Applications can listen to the job status via the
     * {@link #jobStatusProperty() jobStatus} property, or may query it directly
     * using {@link PrinterJob#getJobStatus() getJobStatus()}.
     * <p>
     * The typical life cycle of a job is as follows :
     * <ul>
     * <li>job will be created with status <code>NOT_STARTED</code> and
     * will stay there during configuration via dialogs etc.
     * <li>job will enter state <code>PRINTING</code> when the first page
     * is printed.
     * <li>job will enter state <code>DONE</code> once the job is
     * successfully completed without being cancelled or encountering
     * an error. The job is now completed.
     * <li>A job that encounters an <code>ERROR</code> or is
     * <code>CANCELED</code> is also considered completed.
     * </ul>
     * <p>
     * A job may not revert to an earlier status in its life cycle and
     * the current job state affects operations that may be performed.
     * For example a job may not begin printing again if it has previously
     * passed that state and entered any of the termination states.
     *
     * @since JavaFX 8.0
     */
    public static enum JobStatus {

        /**
         * The new job status. May display print dialogs and
         * configure the job and initiate printing.
         */
        NOT_STARTED,

        /**
         * The job has requested to print at least one page,
         * and has not terminated printing. May no longer
         * display print dialogs.
         */
        PRINTING,

        /**
         * The job has been cancelled by the application.
         * May not display dialogs or initiate printing.
         * Job should be discarded. There is no need to
         * call endJob().
         */
        CANCELED,

        /**
         * The job encountered an error.
         * Job should be discarded. There is no need to
         * call endJob().
         */
        ERROR,

        /**
         * The job initiated printing and later called endJob()
         * which reported success. The job can be discarded
         * as it cannot be re-used.
         */
        DONE
    }

    private ReadOnlyObjectWrapper<JobStatus> jobStatus =
        new ReadOnlyObjectWrapper(JobStatus.NOT_STARTED);

    /**
     * A read only object property representing the current
     * <code>JobStatus</code>
     * @return the current <code>JobStatus</code>
     */
    public final ReadOnlyObjectProperty<JobStatus> jobStatusProperty() {
        return jobStatus.getReadOnlyProperty();
    }

    public final JobStatus getJobStatus() {
        return jobStatus.get();
    }

    /**
     * Cancel the underlying print job at the earliest opportunity.
     * It may return immediately without waiting for the job cancellation
     * to be complete in case this would block the FX user thread
     * for any period of time.
     * If printing is in process at that time, then typically
     * this means cancellation is after the current page is rendered.
     * The job status is updated to CANCELED only once that has happened.
     * Thus determining that the job is CANCELED requires monitoring
     * the job status.
     * <p>
     * The call has no effect if the job has already been requested
     * to be CANCELED, or is in the state ERROR or DONE.
     * For example it will not de-queue from the printer a job that
     * has already been spooled for printing.
     * Once a job is cancelled, it is not valid to call any methods
     * which render new content or change job state.
     */
    public void cancelJob() {
        if (jobStatus.get().ordinal() <= JobStatus.PRINTING.ordinal()) {
            jobStatus.set(JobStatus.CANCELED);
            jobImpl.cancelJob();
        }
    }

    /**
     * If the job can be successfully spooled to the printer queue
     * this will return true. Note : this does not mean the job already
     * printed as that could entail waiting for minutes or longer,
     * even where implementable.
     * <p>
     * A return value of false means the job could not be spooled,
     * or was already completed.
     * <p>
     * Successful completion will also update job status to <code>DONE</code>,
     * at which point the job can no longer be used.
     * <p>
     * Calling endJob() on a job for which no pages have been printed
     * is equivalent to calling {code cancelJob()}.
     * @return true if job is spooled, false if its not, or the job
     * was already in a completed state.
     */
    public synchronized boolean endJob() {
        if (jobStatus.get() == JobStatus.NOT_STARTED) {
            cancelJob();
            return false;
        } else if (jobStatus.get() == JobStatus.PRINTING) {
            boolean rv = jobImpl.endJob();
            jobStatus.set(rv ? JobStatus.DONE : JobStatus.ERROR);
            return rv;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "JavaFX PrinterJob " +
            getPrinter() + "\n" +
            getJobSettings() + "\n" +
            "Job Status = " + getJobStatus();
    }

}
