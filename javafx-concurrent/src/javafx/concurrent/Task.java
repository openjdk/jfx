/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.*;
import static javafx.concurrent.WorkerStateEvent.*;

/**
 * <p>
 *     A fully observable implementation of a {@link FutureTask}. Tasks exposes
 *     additional state and observable properties useful for programming asynchronous
 *     tasks in JavaFX, as defined in the {@link Worker} interface. An implementation
 *     of Task must override the {@link javafx.concurrent.Task#call()} method. This method
 *     is invoked on the background thread. Any state which is used in this method
 *     must be safe to read and write from a background thread. For example, manipulating
 *     a live scene graph from this method is unsafe and will result in runtime
 *     exceptions.
 * </p>
 * <p>
 *     Tasks are flexible and extremely useful for the encapsulation of "work". Because
 *     {@link Service} is designed to execute a Task, any Tasks defined by the application
 *     or library code can easily be used with a Service. Likewise, since Task extends
 *     from FutureTask, it is very easy and natural to use a Task with the java concurrency
 *     {@link java.util.concurrent.Executor} API. Since a Task is Runnable, you
 *     can also call it directly (by invoking the {@link javafx.concurrent.Task#run()} method)
 *     from another background thread. This allows for composition of work, or pass it to
 *     a new Thread constructed and executed manually. Finally, since you can
 *     manually create a new Thread, passing it a Runnable, it is possible to use
 *     the following idiom:
 *     <pre><code>
 *         Thread th = new Thread(task);
 *         th.setDaemon(true);
 *         th.start();
 *     </code></pre>
 *     Note that this code sets the daemon flag of the Thread to true. If you
 *     want a background thread to prevent the VM from existing after the last
 *     stage is closed, then you would want daemon to be false. However, if
 *     you want the background threads to simply terminate after all the
 *     stages are closed, then you must set daemon to true.
 * </p>
 * <p>
 *     Although {@link java.util.concurrent.ExecutorService} defines several methods which
 *     take a Runnable, you should generally limit yourself to using the <code>execute</code>
 *     method inherited from {@link java.util.concurrent.Executor}.
 * </p>
 * <p>
 *     As with FutureTask, a Task is a one-shot class and cannot be reused. See {@link Service}
 *     for a reusable {@link Worker}.
 * </p>
 * <p>
 *     Because the Task is designed for use with JavaFX GUI applications, it ensures
 *     that every change to its public properties, as well as change notifications
 *     for state, errors, and for event handlers, all occur on the main JavaFX application
 *     thread. Accessing these properties from a background thread (including the
 *     {@link #call()} method) will result in runtime exceptions being raised.
 * </p>
 * <p>
 *     It is <strong>strongly encouraged</strong> that all Tasks be initialized with
 *     immutable state upon which the Task will operate. This should be done by providing
 *     a Task constructor which takes the parameters necessary for execution of the Task.
 *     Immutable state makes it easy and safe to use from any thread and ensures
 *     correctness in the presence of multiple threads.
 * </p>
 * <p>
 *     In Java there is no reliable way to "kill" a thread in process. However,
 *     when <code>cancel</code> is called on a Task, it is important that
 *     the Task stop processing. A "run-away" Task might continue processing
 *     and updating the message, text, and progress properties even after the
 *     Task has been cancelled! In Java, cancelling a Task is a cooperative
 *     endeavor. The user of the Task will request that it be cancelled, and
 *     the author of the Task must check whether is has been cancelled within
 *     the body of the <code>call</code> method. There are two ways this can
 *     be done. First, the Task author may check the isCancelled method,
 *     inherited from <code>FutureTask</code>, to see whether the Task has
 *     been cancelled. Second, if the Task implementation makes use of any
 *     blocking calls (such as NIO InterruptibleChannels or Thread.sleep) and
 *     the task is cancelled while in such a blocking call, an
 *     InterruptedException is thrown. Task implementations which have blocking
 *     calls should recognize that an interrupted thread may be the signal for
 *     a cancelled task and should double check the isCancelled method to ensure
 *     that the InterruptedException was thrown due to the cancellation of the
 *     Task.
 * </p>
 * <h2>Examples</h2>
 * <p>
 *     The following set of examples demonstrate some of the most common uses of
 *     Tasks.
 * </p>
 *
 * <h3>A Simple Loop</h3>
 *
 * <p>
 *     The first example is a simple loop that does nothing particularly useful,
 *     but demonstrates the fundamental aspects of writing a Task correctly. This
 *     example will simply loop and print to standard out on each loop iteration.
 *     When it completes, it returns the number of times it iterated.
 * </p>
 *
 * <pre><code>
 *     Task&lt;Integer&gt; task = new Task&lt;Integer&gt;() {
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations;
 *             for (iterations = 0; iterations &lt; 100000; iterations++) {
 *                 if (isCancelled()) {
 *                     break;
 *                 }
 *                 System.out.println("Iteration " + iterations);
 *             }
 *             return iterations;
 *         }
 *     };
 * </code></pre>
 *
 * <p>
 *     First, we define what type of value is returned from this Task. In this
 *     case, we want to return the number of times we iterated, so we will
 *     specify the Task to be of type Integer by using generics. Then, within
 *     the implementation of the <code>call</code> method, we iterate from
 *     0 to 100000. On each iteration, we check to see whether this Task has
 *     been cancelled. If it has been, then we break out of the loop and return
 *     the number of times we iterated. Otherwise a message is printed to
 *     the console and the iteration count increased and we continue looping.
 * </p>
 *
 * <p>
 *     Checking for isCancelled() in the loop body is critical, otherwise the
 *     developer may cancel the task, but the task will continue running
 *     and updating both the progress and returning the incorrect result
 *     from the end of the <code>call</code> method. A correct implementation
 *     of a Task will always check for cancellation.
 * </p>
 *
 * <h3>A Simple Loop With Progress Notification</h3>
 *
 * <p>
 *     Similar to the previous example, except this time we will modify the
 *     progress of the Task in each iteration. Note that we have a choice
 *     to make in the case of cancellation. Do we want to set the progress back
 *     to -1 (indeterminate) when the Task is cancelled, or do we want to leave
 *     the progress where it was at? In this case, lets leave the progress alone
 *     and only update the message on cancellation, though updating the
 *     progress after cancellation is a perfectly valid choice.
 * </p>
 *
 * <pre><code>
 *     Task&lt;Integer&gt; task = new Task&lt;Integer&gt;() {
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations;
 *             for (iterations = 0; iterations &lt; 10000000; iterations++) {
 *                 if (isCancelled()) {
 *                     updateMessage("Cancelled");
 *                     break;
 *                 }
 *                 updateMessage("Iteration " + iterations);
 *                 updateProgress(iterations, 10000000);
 *             }
 *             return iterations;
 *         }
 *     };
 * </code></pre>
 *
 * <p>
 *     As before, within the for loop we check whether the Task has been
 *     cancelled. If it has been cancelled, we will update the Task's
 *     message to indicate that it has been cancelled, and then break as
 *     before. If the Task has not been cancelled, then we will update its
 *     message to indicate the current iteration and then update the
 *     progress to indicate the current progress.
 * </p>
 *
 * <h3>A Simple Loop With Progress Notification And Blocking Calls</h3>
 *
 * <p>
 *     This example adds to the previous examples a blocking call. Because a
 *     blocking call may thrown an InterruptedException, and because an
 *     InterruptedException may occur as a result of the Task being cancelled,
 *     we need to be sure to handle the InterruptedException and check on the
 *     cancel state.
 * </p>
 *
 * <pre><code>
 *     Task&lt;Integer&gt; task = new Task&lt;Integer&gt;() {
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations;
 *             for (iterations = 0; iterations &lt; 1000; iterations++) {
 *                 if (isCancelled()) {
 *                     updateMessage("Cancelled");
 *                     break;
 *                 }
 *                 updateMessage("Iteration " + iterations);
 *                 updateProgress(iterations, 1000);
 *
 *                 // Now block the thread for a short time, but be sure
 *                 // to check the interrupted exception for cancellation!
 *                 try {
 *                     Thread.sleep(100);
 *                 } catch (InterruptedException interrupted) {
 *                     if (isCancelled()) {
 *                         updateMessage("Cancelled");
 *                         break;
 *                     }
 *                 }
 *             }
 *             return iterations;
 *         }
 *     };
 * </code></pre>
 *
 * <p>
 *     Here we have added to the body of the loop a <code>Thread.sleep</code>
 *     call. Since this is a blocking call, I have to handle the potential
 *     InterruptedException. Within the catch block, I will check whether
 *     the Task has been cancelled, and if so, update the message accordingly
 *     and break out of the loop.
 * </p>
 *
 * <h3>A Task Which Takes Parameters</h3>
 *
 * <p>
 *     Most Tasks require some parameters in order to do useful work. For
 *     example, a DeleteRecordTask needs the object or primary key to delete
 *     from the database. A ReadFileTask needs the URI of the file to be read.
 *     Because Tasks operate on a background thread, care must be taken to
 *     make sure the body of the <code>call</code> method does not read or
 *     modify any shared state. There are two techniques most useful for
 *     doing this: using final variables, and passing variables to a Task
 *     during construction.
 * </p>
 *
 * <p>
 *     When using a Task as an anonymous class, the most natural way to pass
 *     parameters to the Task is by using final variables. In this example,
 *     we pass to the Task the total number of times the Task should iterate.
 * </p>
 *
 * <pre><code>
 *     final int totalIterations = 9000000;
 *     Task&lt;Integer&gt; task = new Task&lt;Integer&gt;() {
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations;
 *             for (iterations = 0; iterations &lt; totalIterations; iterations++) {
 *                 if (isCancelled()) {
 *                     updateMessage("Cancelled");
 *                     break;
 *                 }
 *                 updateMessage("Iteration " + iterations);
 *                 updateProgress(iterations, totalIterations);
 *             }
 *             return iterations;
 *         }
 *     };
 * </code></pre>
 *
 * <p>
 *     Since <code>totalIterations</code> is final, the <code>call</code>
 *     method can safely read it and refer to it from a background thread.
 * </p>
 *
 * <p>
 *     When writing Task libraries (as opposed to specific-use implementations),
 *     we need to use a different technique. In this case, I will create an
 *     IteratingTask which performs the same work as above. This time, since
 *     the IteratingTask is defined in its own file, it will need to have
 *     parameters passed to it in its constructor. These parameters are
 *     assigned to final variables.
 * </p>
 *
 * <pre><code>
 *     public class IteratingTask extends Task&lt;Integer&gt; {
 *         private final int totalIterations;
 *
 *         public IteratingTask(int totalIterations) {
 *             this.totalIterations = totalIterations;
 *         }
 *
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations = 0;
 *             for (iterations = 0; iterations &lt; totalIterations; iterations++) {
 *                 if (isCancelled()) {
 *                     updateMessage("Cancelled");
 *                     break;
 *                 }
 *                 updateMessage("Iteration " + iterations);
 *                 updateProgress(iterations, totalIterations);
 *             }
 *             return iterations;
 *         }
 *     }
 * </code></pre>
 *
 * <p>And then when used:</p>
 *
 * <pre><code>
 *     IteratingTask task = new IteratingTask(8000000);
 * </code></pre>
 *
 * <p>In this way, parameters are passed to the IteratingTask in a safe
 * manner, and again, are final. Thus, the <code>call</code> method can
 * safely read this state from a background thread.</p>
 *
 * <p>WARNING: Do not pass mutable state to a Task and then operate on it
 * from a background thread. Doing so may introduce race conditions. In
 * particular, suppose you had a SaveCustomerTask which took a Customer
 * in its constructor. Although the SaveCustomerTask may have a final
 * reference to the Customer, if the Customer object is mutable, then it
 * is possible that both the SaveCustomerTask and some other application code
 * will be reading or modifying the state of the Customer from different
 * threads. Be very careful in such cases, that while a mutable object such
 * as this Customer is being used from a background thread, that it is
 * not being used also from another thread. In particular, if the background
 * thread is reading data from the database and updating the Customer object,
 * and the Customer object is bound to scene graph nodes (such as UI
 * controls), then there could be a violation of threading rules! For such
 * cases, modify the Customer object from the FX Application Thread rather
 * than from the background thread.</p>
 *
 * <pre><code>
 *     public class UpdateCustomerTask extends Task&lt;Customer&gt; {
 *         private final Customer customer;
 *
 *         public UpdateCustomerTask(Customer customer) {
 *             this.customer = customer;
 *         }
 *
 *         &#64;Override protected Customer call() throws Exception {
 *             // pseudo-code:
 *             //   query the database
 *             //   read the values
 *
 *             // Now update the customer
 *             Platform.runLater(new Runnable() {
 *                 &#64;Override public void run() {
 *                     customer.setF setFirstName(rs.getString("FirstName"));
 *                     // etc
 *                 }
 *             });
 *
 *             return customer;
 *         }
 *     }
 * </code></pre>
 *
 * <h3>A Task Which Returns No Value</h3>
 *
 * <p>
 *     Many, if not most, Tasks should return a value upon completion. For
 *     CRUD Tasks, one would expect that a "Create" Task would return the newly
 *     created object or primary key, a "Read" Task would return the read
 *     object, an "Update" task would return the number of records updated,
 *     and a "Delete" task would return the number of records deleted.
 * </p>
 *
 * <p>
 *     However sometimes there just isn't anything truly useful to return.
 *     For example, I might have a Task which writes to a file. Task has built
 *     into it a mechanism for indicating whether it has succeeded or failed
 *     along with the number of bytes written (the progress), and thus there is
 *     nothing really for me to return. In such a case, you can use the Void
 *     type. This is a special type in the Java language which can only be
 *     assigned the value of <code>null</code>. You would use it as follows:
 * </p>
 *
 * <pre><code>
 *     final String filePath = "/foo.txt";
 *     final String contents = "Some contents";
 *     Task&lt;Void&gt; task = new Task&lt;Void&gt;() {
 *         &#64;Override protected Void call() throws Exception {
 *             File file = new File(filePath);
 *             FileOutputStream out = new FileOutputStream(file);
 *             // ... and other code to write the contents ...
 *
 *             // Return null at the end of a Task of type Void
 *             return null;
 *         }
 *     };
 * </code></pre>
 *
 * <h3>A Task Which Returns An ObservableList</h3>
 *
 * <p>Because the ListView, TableView, and other UI controls and scene graph
 * nodes make use of ObservableList, it is common to want to create and return
 * an ObservableList from a Task. When you do not care to display intermediate
 * values, the easiest way to correctly write such a Task is simply to
 * construct an ObservableList within the <code>call</code> method, and then
 * return it at the conclusion of the Task.</p>
 *
 * <pre><code>
 *     Task&lt;ObservableList&lt;Rectangle&gt;&gt; task = new Task&lt;ObservableList&lt;Rectangle&gt;&gt;() {
 *         &#64;Override protected ObservableList&lt;Rectangle&gt; call() throws Exception {
 *             updateMessage("Creating Rectangles");
 *             ObservableList&lt;Rectangle&gt; results = FXCollections.observableArrayList();
 *             for (int i=0; i<100; i++) {
 *                 if (isCancelled()) break;
 *                 Rectangle r = new Rectangle(10, 10);
 *                 r.setX(10 * i);
 *                 results.add(r);
 *                 updateProgress(i, 100);
 *             }
 *             return results;
 *         }
 *     };
 * </code></pre>
 *
 * <p>In the above example, we are going to create 100 rectangles and return
 * them from this task. An ObservableList is created within the
 * <code>call</code> method, populated, and then returned.</p>
 *
 * <h3>A Task Which Returns Partial Results</h3>
 *
 * <p>Sometimes you want to create a Task which will return partial results.
 * Perhaps you are building a complex scene graph and want to show the
 * scene graph as it is being constructed. Or perhaps you are reading a large
 * amount of data over the network and want to display the entries in a
 * TableView as the data is arriving. In such cases, there is some shared state
 * available both to the FX Application Thread and the background thread.
 * Great care must be taken to <strong>never update shared state from any
 * thread other than the FX Application Thread</strong>.</p>
 *
 * <p>The easiest way to do this is to expose a new property on the Task
 * which will represent the partial result. Then make sure to use
 * <code>Platform.runLater</code> when adding new items to the partial
 * result.</p>
 *
 * <pre><code>
 *     public class PartialResultsTask extends Task&lt;ObservableList&lt;Rectangle&gt;&gt; {
 *         // Uses Java 7 diamond operator
 *         private ReadOnlyObjectWrapper<ObservableList<Rectangle>> partialResults =
 *                 new ReadOnlyObjectWrapper<>(this, "partialResults",
 *                         FXCollections.observableArrayList(new ArrayList<Rectangle>()));
 *
 *         public final ObservableList<Rectangle> getPartialResults() { return partialResults.get(); }
 *         public final ReadOnlyObjectProperty<ObservableList<Rectangle>> partialResultsProperty() {
 *             return partialResults.getReadOnlyProperty();
 *         }
 *
 *         &#64;Override protected ObservableList<Rectangle> call() throws Exception {
 *             updateMessage("Creating Rectangles...");
 *             for (int i=0; i<100; i++) {
 *                 if (isCancelled()) break;
 *                 final Rectangle r = new Rectangle(10, 10);
 *                 r.setX(10 * i);
 *                 Platform.runLater(new Runnable() {
 *                     &#64;Override public void run() {
 *                         partialResults.get().add(r);
 *                     }
 *                 });
 *                 updateProgress(i, 100);
 *             }
 *             return partialResults.get();
 *         }
 *     }
 * </code></pre>
 *
 * <!-- TODO: Update to use the PartialResultsTask. This needs to be a new
 *      task written such that there is an updateResults method which does
 *      all the normal event queue coalescing and so forth. Creating a
 *      PartialResultsTask is trivial, however creating a PartialResultsService
 *      is a bit more intimidating. The problem with PartialResultsService is
 *      that you might also want a PartialResultsScheduledService, so there is
 *      some explosion of API possible. It might be that we simply teach
 *      Service itself how to deal with PartialResultsTasks -- that is probably
 *      my preferred approach. -->
 *
 * <h3>A Task Which Modifies The Scene Graph</h3>
 *
 * <p>Generally, Tasks should not interact directly with the UI. Doing so
 * creates a tight coupling between a specific Task implementation and a
 * specific part of your UI. However, when you do want to create such a
 * coupling, you must ensure that you use <code>Platform.runLater</code>
 * so that any modifications of the scene graph occur on the
 * FX Application Thread.</p>
 *
 * <pre><code>
 *     final Group group = new Group();
 *     Task&lt;Void&gt; task = new Task&lt;Void&gt;() {
 *         &#64;Override protected Void call() throws Exception {
 *             for (int i=0; i<100; i++) {
 *                 if (isCancelled()) break;
 *                 final Rectangle r = new Rectangle(10, 10);
 *                 r.setX(10 * i);
 *                 Platform.runLater(new Runnable() {
 *                     &#64;Override public void run() {
 *                         group.getChildren().add(r);
 *                     }
 *                 });
 *             }
 *             return null;
 *         }
 *     };
 * </code></pre>
 *
 * <h3>Reacting To State Changes Generically</h3>
 *
 * <p>Sometimes you may want to write a Task which updates its progress,
 * message, text, or in some other way reacts whenever a state change
 * happens on the Task. For example, you may want to change the status
 * message on the Task on Failure, Success, Running, or Cancelled state changes.
 * </p>
 * <pre><code>
 *     Task&lt;Integer&gt; task = new Task&lt;Integer&gt;() {
 *         &#64;Override protected Integer call() throws Exception {
 *             int iterations = 0;
 *             for (iterations = 0; iterations &lt; 100000; iterations++) {
 *                 if (isCancelled()) {
 *                     break;
 *                 }
 *                 System.out.println("Iteration " + iterations);
 *             }
 *             return iterations;
 *         }
 *
 *         &#64;Override protected void succeeded() {
 *             super.succeeded();
 *             updateMessage("Done!");
 *         }
 *
 *         &#64;Override protected void cancelled() {
 *             super.cancelled();
 *             updateMessage("Cancelled!");
 *         }
 *
 *         &#64;Override protected void failed() {
 *             super.failed();
 *             updateMessage("Failed!");
 *         }
 *     };
 * </code></pre>
 */
public abstract class Task<V> extends FutureTask<V> implements Worker<V>, EventTarget {
    /**
     * Used to send workDone updates in a thread-safe manner from the subclass
     * to the FX application thread and workDone related properties. AtomicReference
     * is used so as to coalesce updates such that we don't flood the event queue.
     */
    private AtomicReference<ProgressUpdate> progressUpdate = new AtomicReference<ProgressUpdate>();

    /**
     * Used to send message updates in a thread-safe manner from the subclass
     * to the FX application thread. AtomicReference is used so as to coalesce
     * updates such that we don't flood the event queue.
     */
    private AtomicReference<String> messageUpdate = new AtomicReference<String>();

    /**
     * Used to send title updates in a thread-safe manner from the subclass
     * to the FX application thread. AtomicReference is used so as to coalesce
     * updates such that we don't flood the event queue.
     */
    private AtomicReference<String> titleUpdate = new AtomicReference<String>();

    /**
     * Creates a new Task.
     */
    public Task() {
        this(new TaskCallable<V>());
    }

    /**
     * This bit of construction trickery is necessary because otherwise there is
     * no way for the main constructor to both create the callable and maintain
     * a reference to it, which is necessary because an anonymous callable construction
     * cannot reference the implicit "this". We leverage an internal Callable
     * so that all the pre-built semantics around cancel and so forth are
     * handled correctly.
     *
     * @param callableAdapter non-null implementation of the
     *                        TaskCallable adapter
     */
    private Task(final TaskCallable<V> callableAdapter) {
        super(callableAdapter);
        callableAdapter.task = this;
    }

    /**
     * Invoked when the Task is executed, the call method must be overridden and
     * implemented by subclasses. The call method actually performs the
     * background thread logic. Only the updateProgress, updateMessage, and
     * updateTitle methods of Task may be called from code within this method.
     * Any other interaction with the Task from the background thread will result
     * in runtime exceptions.
     *
     * @return The result of the background work, if any.
     * @throws Exception an unhandled exception which occurred during the
     *         background operation
     */
    protected abstract V call() throws Exception;

    private ObjectProperty<State> state = new SimpleObjectProperty<State>(this, "state", State.READY);
    final void setState(State value) { // package access for the Service
        checkThread();
        final State s = getState();
        if (s != State.CANCELLED) {
            this.state.set(value);
            // Make sure the running flag is set
            setRunning(value == State.SCHEDULED || value == State.RUNNING);

            // Invoke the event handlers, and then call the protected methods.
            switch (state.get()) {
                case CANCELLED:
                    fireEvent(new WorkerStateEvent(this, WORKER_STATE_CANCELLED));
                    cancelled();
                    break;
                case FAILED:
                    fireEvent(new WorkerStateEvent(this, WORKER_STATE_FAILED));
                    failed();
                    break;
                case READY:
                    // This even can never meaningfully occur, because the
                    // Task begins life as ready and can never go back to it!
                    break;
                case RUNNING:
                    fireEvent(new WorkerStateEvent(this, WORKER_STATE_RUNNING));
                    running();
                    break;
                case SCHEDULED:
                    fireEvent(new WorkerStateEvent(this, WORKER_STATE_SCHEDULED));
                    scheduled();
                    break;
                case SUCCEEDED:
                    fireEvent(new WorkerStateEvent(this, WORKER_STATE_SUCCEEDED));
                    succeeded();
                    break;
                default: throw new AssertionError("Should be unreachable");
            }
        }
    }
    @Override public final State getState() { checkThread(); return state.get(); }
    @Override public final ReadOnlyObjectProperty<State> stateProperty() { checkThread(); return state; }

    /**
     * The onSchedule event handler is called whenever the Task state
     * transitions to the SCHEDULED state.
     *
     * @return the onScheduled event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onScheduledProperty() {
        return getEventHelper().onScheduledProperty();
    }

    /**
     * The onSchedule event handler is called whenever the Task state
     * transitions to the SCHEDULED state.
     *
     * @return the onScheduled event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnScheduled() {
        return eventHelper == null ? null : eventHelper.getOnScheduled();
    }

    /**
     * The onSchedule event handler is called whenever the Task state
     * transitions to the SCHEDULED state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnScheduled(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnScheduled(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the SCHEDULED state.
     * This method is invoked on the FX Application Thread after any listeners
     * of the state property and after the Task has been fully transitioned to
     * the new state.
     */
    protected void scheduled() { }

    /**
     * The onRunning event handler is called whenever the Task state
     * transitions to the RUNNING state.
     *
     * @return the onRunning event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onRunningProperty() {
        return getEventHelper().onRunningProperty();
    }

    /**
     * The onRunning event handler is called whenever the Task state
     * transitions to the RUNNING state.
     *
     * @return the onRunning event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnRunning() {
        return eventHelper == null ? null : eventHelper.getOnRunning();
    }

    /**
     * The onRunning event handler is called whenever the Task state
     * transitions to the RUNNING state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnRunning(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnRunning(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the RUNNING state.
     * This method is invoked on the FX Application Thread after any listeners
     * of the state property and after the Task has been fully transitioned to
     * the new state.
     */
    protected void running() { }

    /**
     * The onSucceeded event handler is called whenever the Task state
     * transitions to the SUCCEEDED state.
     *
     * @return the onSucceeded event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onSucceededProperty() {
        return getEventHelper().onSucceededProperty();
    }

    /**
     * The onSucceeded event handler is called whenever the Task state
     * transitions to the SUCCEEDED state.
     *
     * @return the onSucceeded event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnSucceeded() {
        return eventHelper == null ? null : eventHelper.getOnSucceeded();
    }

    /**
     * The onSucceeded event handler is called whenever the Task state
     * transitions to the SUCCEEDED state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnSucceeded(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnSucceeded(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the SUCCEEDED state.
     * This method is invoked on the FX Application Thread after any listeners
     * of the state property and after the Task has been fully transitioned to
     * the new state.
     */
    protected void succeeded() { }

    /**
     * The onCancelled event handler is called whenever the Task state
     * transitions to the CANCELLED state.
     *
     * @return the onCancelled event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onCancelledProperty() {
        return getEventHelper().onCancelledProperty();
    }

    /**
     * The onCancelled event handler is called whenever the Task state
     * transitions to the CANCELLED state.
     *
     * @return the onCancelled event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnCancelled() {
        return eventHelper == null ? null : eventHelper.getOnCancelled();
    }

    /**
     * The onCancelled event handler is called whenever the Task state
     * transitions to the CANCELLED state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnCancelled(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnCancelled(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the CANCELLED state.
     * This method is invoked on the FX Application Thread after any listeners
     * of the state property and after the Task has been fully transitioned to
     * the new state.
     */
    protected void cancelled() { }

    /**
     * The onFailed event handler is called whenever the Task state
     * transitions to the FAILED state.
     *
     * @return the onFailed event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onFailedProperty() {
        return getEventHelper().onFailedProperty();
    }

    /**
     * The onFailed event handler is called whenever the Task state
     * transitions to the FAILED state.
     *
     * @return the onFailed event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnFailed() {
        return eventHelper == null ? null : eventHelper.getOnFailed();
    }

    /**
     * The onFailed event handler is called whenever the Task state
     * transitions to the FAILED state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnFailed(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnFailed(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the FAILED state.
     * This method is invoked on the FX Application Thread after any listeners
     * of the state property and after the Task has been fully transitioned to
     * the new state.
     */
    protected void failed() { }

    private ObjectProperty<V> value = new SimpleObjectProperty<V>(this, "value");
    private void setValue(V v) { checkThread(); value.set(v); }
    @Override public final V getValue() { checkThread(); return value.get(); }
    @Override public final ReadOnlyObjectProperty<V> valueProperty() { checkThread(); return value; }

    private ObjectProperty<Throwable> exception = new SimpleObjectProperty<Throwable>(this, "exception");
    private void _setException(Throwable value) { checkThread(); exception.set(value); }
    @Override public final Throwable getException() { checkThread(); return exception.get(); }
    @Override public final ReadOnlyObjectProperty<Throwable> exceptionProperty() { checkThread(); return exception; }

    private DoubleProperty workDone = new SimpleDoubleProperty(this, "workDone", -1);
    private void setWorkDone(double value) { checkThread(); workDone.set(value); }
    @Override public final double getWorkDone() { checkThread(); return workDone.get(); }
    @Override public final ReadOnlyDoubleProperty workDoneProperty() { checkThread(); return workDone; }

    private DoubleProperty totalWork = new SimpleDoubleProperty(this, "totalWork", -1);
    private void setTotalWork(double value) { checkThread(); totalWork.set(value); }
    @Override public final double getTotalWork() { checkThread(); return totalWork.get(); }
    @Override public final ReadOnlyDoubleProperty totalWorkProperty() { checkThread(); return totalWork; }

    private DoubleProperty progress = new SimpleDoubleProperty(this, "progress", -1);
    private void setProgress(double value) { checkThread(); progress.set(value); }
    @Override public final double getProgress() { checkThread(); return progress.get(); }
    @Override public final ReadOnlyDoubleProperty progressProperty() { checkThread(); return progress; }

    private BooleanProperty running = new SimpleBooleanProperty(this, "running", false);
    private void setRunning(boolean value) { checkThread(); running.set(value); }
    @Override public final boolean isRunning() { checkThread(); return running.get(); }
    @Override public final ReadOnlyBooleanProperty runningProperty() { checkThread(); return running; }

    private StringProperty message = new SimpleStringProperty(this, "message", "");
    @Override public final String getMessage() { return message.get(); }
    @Override public final ReadOnlyStringProperty messageProperty() { return message; }

    private StringProperty title = new SimpleStringProperty(this, "title", "");
    @Override public final String getTitle() { return title.get(); }
    @Override public final ReadOnlyStringProperty titleProperty() { return title; }

    @Override public final boolean cancel() {
        return cancel(true);
    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        // Delegate to the super implementation to actually attempt to cancel this thing
        boolean flag = super.cancel(mayInterruptIfRunning);
        // If cancel succeeded (according to the semantics of the Future cancel method),
        // then we need to make sure the State flag is set appropriately
        if (flag) {
            // If this method was called on the FX application thread, then we can
            // just update the state directly and this will make sure that after
            // the cancel method was called, the state will be set correctly
            // (otherwise it would be indeterminate. However if the cancel method was
            // called off the FX app thread, then we must use runLater, and the
            // state flag will not be readable immediately after this call. However,
            // that would be the case anyway since these properties are not thread-safe.
            if (isFxApplicationThread()) {
                setState(State.CANCELLED);
            } else {
                runLater(new Runnable() {
                    @Override public void run() {
                        setState(State.CANCELLED);
                    }
                });
            }
        }
        // return the flag
        return flag;
    }

    /**
     * Updates the <code>workDone</code>, <code>totalWork</code>,
     * and <code>progress</code> properties. Calls to updateProgress
     * are coalesced and run later on the FX application thread, and calls
     * to updateProgress, even from the FX Application thread, may not
     * necessarily result in immediate updates to these properties, and
     * intermediate workDone values may be coalesced to save on event
     * notifications. <code>max</code> becomes the new value for
     * <code>totalWork</code>.
     * <p>
     *     <em>This method is safe to be called from any thread.</em>
     * </p>
     *
     * @param workDone A value from -1 up to max. If the value is greater
     *                 than max, an illegal argument exception is thrown.
     *                 If the value passed is -1, then the resulting percent
     *                 done will be -1 (thus, indeterminate).
     * @param max A value from -1 to Long.MAX_VALUE. Any value outside this
     *            range results in an IllegalArgumentException.
     */
    protected void updateProgress(long workDone, long max) {
        // Perform the argument sanity check that workDone is < max
        if (workDone > max) {
            throw new IllegalArgumentException("The workDone must be <= the max");
        }

        // Make sure neither workDone nor max is < -1
        if (workDone < -1 || max < -1) {
            throw new IllegalArgumentException("The workDone and max cannot be less than -1");
        }

        if (isFxApplicationThread()) {
            _updateProgress(workDone, max);
        } else if (progressUpdate.getAndSet(new ProgressUpdate(workDone, max)) == null) {
            runLater(new Runnable() {
                @Override public void run() {
                    final ProgressUpdate update = progressUpdate.getAndSet(null);
                    _updateProgress(update.workDone, update.totalWork);
                }
            });
        }
    }

    private void _updateProgress(double workDone, double max) {
        setTotalWork(max);
        setWorkDone(workDone);
        if (workDone == -1) {
            setProgress(-1);
        } else {
            setProgress(workDone / max);
        }
    }

    /**
     * Updates the <code>message</code> property. Calls to updateMessage
     * are coalesced and run later on the FX application thread, so calls
     * to updateMessage, even from the FX Application thread, may not
     * necessarily result in immediate updates to this property, and
     * intermediate message values may be coalesced to save on event
     * notifications.
     * <p>
     *     <em>This method is safe to be called from any thread.</em>
     * </p>
     *
     * @param message the new message
     */
    protected void updateMessage(String message) {
        if (isFxApplicationThread()) {
            this.message.set(message);
        } else {
            // As with the workDone, it might be that the background thread
            // will update this message quite frequently, and we need
            // to throttle the updates so as not to completely clobber
            // the event dispatching system.
            if (messageUpdate.getAndSet(message) == null) {
                runLater(new Runnable() {
                    @Override public void run() {
                        final String message = messageUpdate.getAndSet(null);
                        Task.this.message.set(message);
                    }
                });
            }
        }
    }

    /**
     * Updates the <code>title</code> property. Calls to updateTitle
     * are coalesced and run later on the FX application thread, so calls
     * to updateTitle, even from the FX Application thread, may not
     * necessarily result in immediate updates to this property, and
     * intermediate title values may be coalesced to save on event
     * notifications.
     * <p>
     *     <em>This method is safe to be called from any thread.</em>
     * </p>
     *
     * @param title the new title
     */
    protected void updateTitle(String title) {
        if (isFxApplicationThread()) {
            this.title.set(title);
        } else {
            // As with the workDone, it might be that the background thread
            // will update this title quite frequently, and we need
            // to throttle the updates so as not to completely clobber
            // the event dispatching system.
            if (titleUpdate.getAndSet(title) == null) {
                runLater(new Runnable() {
                    @Override public void run() {
                        final String title = titleUpdate.getAndSet(null);
                        Task.this.title.set(title);
                    }
                });
            }
        }
    }

    /*
     * IMPLEMENTATION
     */

    private void checkThread() {
        if (!isFxApplicationThread()) {
            throw new IllegalStateException("Task must only be used from the FX Application Thread");
        }
    }

    // This method exists for the sake of testing, so I can subclass and override
    // this method in the test and not actually use Platform.runLater.
    void runLater(Runnable r) {
        Platform.runLater(r);
    }

    // This method exists for the sake of testing, so I can subclass and override
    // this method in the test and not actually use Platform.isFxApplicationThread.
    boolean isFxApplicationThread() {
        return Platform.isFxApplicationThread();
    }

    /***************************************************************************
     *                                                                         *
     *                         Event Dispatch                                  *
     *                                                                         *
     **************************************************************************/

    private EventHelper eventHelper = null;
    private EventHelper getEventHelper() {
        if (eventHelper == null) {
            eventHelper = new EventHelper(this);
        }
        return eventHelper;
    }

    /**
     * Registers an event handler to this task. Any event filters are first
     * processed, then the specified onFoo event handlers, and finally any
     * event handlers registered by this method. As with other events
     * in the scene graph, if an event is consumed, it will not continue
     * dispatching.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getEventHelper().addEventHandler(eventType, eventHandler);
    }

    /**
     * Unregisters a previously registered event handler from this task. One
     * handler might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the handler.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the handler to unregister
     * @throws NullPointerException if the event type or handler is null
     */
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getEventHelper().removeEventHandler(eventType, eventHandler);
    }

    /**
     * Registers an event filter to this task. Registered event filters get
     * an event before any associated event handlers.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the type of the events to receive by the filter
     * @param eventFilter the filter to register
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getEventHelper().addEventFilter(eventType, eventFilter);
    }

    /**
     * Unregisters a previously registered event filter from this task. One
     * filter might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the filter.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the event type from which to unregister
     * @param eventFilter the filter to unregister
     * @throws NullPointerException if the event type or filter is null
     */
    public final <T extends Event> void removeEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getEventHelper().removeEventFilter(eventType, eventFilter);
    }

    /**
     * Sets the handler to use for this event type. There can only be one such
     * handler specified at a time. This handler is guaranteed to be called
     * first. This is used for registering the user-defined onFoo event
     * handlers.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type to associate with the given eventHandler
     * @param eventHandler the handler to register, or null to unregister
     * @throws NullPointerException if the event type is null
     */
    protected final <T extends Event> void setEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getEventHelper().setEventHandler(eventType, eventHandler);
    }

    /**
     * Fires the specified event. Any event filter encountered will
     * be notified and can consume the event. If not consumed by the filters,
     * the event handlers on this task are notified. If these don't consume the
     * event either, then all event handlers are called and can consume the
     * event.
     * <p>
     * This method must be called on the FX user thread.
     *
     * @param event the event to fire
     */
    public final void fireEvent(Event event) {
        checkThread();
        getEventHelper().fireEvent(event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return getEventHelper().buildEventDispatchChain(tail);
    }

    /**
     * A struct like class that contains the last workDone update information.
     * What we do when updateProgress is called, is we create a new ProgressUpdate
     * object and store it. If it was null, then we fire off a new Runnable
     * using RunLater, which will eventually read the latest and set it to null
     * atomically. If it was not null, then we simply update it.
     */
    private static final class ProgressUpdate {
        private double workDone;
        private double totalWork;

        private ProgressUpdate(double p, double m) {
            this.workDone = p;
            this.totalWork = m;
        }
    }

    /**
     *  TaskCallable actually implements the Callable contract as defined for
     *  the FutureTask class, and is necessary so as to allow us to intercept
     *  the call() operation to update state on the Task as appropriate.
     * @param <V>
     */
    private static final class TaskCallable<V> implements Callable<V> {
        /**
         * The Task that is going to use this TaskCallable
         */
        private Task<V> task;

        /**
         * Create a TaskCallable. The concurrent and other fields MUST be set
         * immediately after creation.
         */
        private TaskCallable() { }

        /**
         * Invoked by the system when it is time to run the client code. This
         * implementation is where we modify the state and other properties
         * and from which we invoke the events.
         *
         * @return The result of the Task call method
         * @throws Exception any exception which occurred
         */
        @Override public V call() throws Exception {
            // If the Task is sent to an ExecutorService for execution, then we
            // will need to make sure that we transition first to the SCHEDULED
            // state before then transitioning to the RUNNING state. If the
            // Task was executed by a Service, then it will have already been
            // in the SCHEDULED state and setting it again here has no negative
            // effect. But we must ensure that SCHEDULED is visited before RUNNING
            // in all cases so that developer code can be consistent.
            task.runLater(new Runnable() {
                @Override public void run() {
                    task.setState(State.SCHEDULED);
                    task.setState(State.RUNNING);
                }
            });
            // Go ahead and delegate to the wrapped callable
            try {
                final V result = task.call();
                if (!task.isCancelled()) {
                    // If it was not cancelled, then we take the return
                    // value and set it as the result.
                    task.runLater(new Runnable() {
                        @Override public void run() {
                            // The result must be set first, so that when the
                            // SUCCEEDED flag is set, the value will be available
                            // The alternative is not the case, because you
                            // can assume if the result is set, it has
                            // succeeded.
                            task.setValue(result);
                            task.setState(State.SUCCEEDED);
                        }
                    });
                    return result;
                } else {
                    // There may have been some intermediate result in the
                    // task set from the background thread, so I want to be
                    // sure to return the most recent intermediate value
                    return task.getValue();
                }
            } catch (final Throwable th) {
                // Be sure to set the state after setting the cause of failure
                // so that developers handling the state change events have a
                // throwable to inspect when they get the FAILED state. Note
                // that the other way around is not important -- when a developer
                // observes the causeOfFailure is set to a non-null value, even
                // though the state has not yet been updated, he can infer that
                // it will be FAILED because it can be nothing other than FAILED
                // in that circumstance.
                task.runLater(new Runnable() {
                    @Override public void run() {
                        task._setException(th);
                        task.setState(State.FAILED);
                    }
                });
                // Some error occurred during the call (it might be
                // an exception (either runtime or checked), or it might
                // be an error. In any case, we capture the throwable,
                // record it as the causeOfFailure, and then rethrow. However
                // since the Callable interface requires that we throw an
                // Exception (not Throwable), we have to wrap the exception
                // if it is not already one.
                if (th instanceof Exception) {
                    throw (Exception) th;
                } else {
                    throw new Exception(th);
                }
            }
        }
    }
}
