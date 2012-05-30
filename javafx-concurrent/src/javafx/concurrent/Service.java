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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.*;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.*;
import static javafx.concurrent.WorkerStateEvent.*;

/**
 * <p>
 *     A Service is a non-visual component encapsulating the information required
 *     to perform some work on one or more background threads. As part of the
 *     JavaFX UI library, the Service knows about the JavaFX Application thread
 *     and is designed to relieve the application developer from the burden
 *     of manging multithreaded code that interacts with the user interface. As
 *     such, all of the methods and state on the Service are intended to be
 *     invoked exclusively from the JavaFX Application thread.
 * </p>
 * <p>
 *     Service implements {@link Worker}. As such, you can observe the state of
 *     the background operation and optionally cancel it. Service is a reusable
 *     Worker, meaning that it can be reset and restarted. Due to this, a Service
 *     can be constructed declaratively and restarted on demand.
 * </p>
 * <p>
 *     If an {@link java.util.concurrent.Executor} is specified on the Service,
 *     then it will be used to actually execute the service. Otherwise,
 *     a daemon thread will be created and executed. If you wish to create
 *     non-daemon threads, then specify a custom Executor (for example,
 *     you could use a {@link ThreadPoolExecutor} with a custom
 *     {@link java.util.concurrent.ThreadFactory}).
 * </p>
 * <p>
 *     Because a Service is intended to simplify declarative use cases, subclasses
 *     should expose as properties the input parameters to the work to be done.
 *     For example, suppose I wanted to write a Service which read the first line
 *     from any URL and returned it as a String. Such a Service might be defined,
 *     such that it had a single property, <code>url</code>. It might be implemented
 *     as:
 *     <pre><code>
 *     public static class FirstLineService extends Service&lt;String&gt; {
 *         private StringProperty url = new SimpleStringProperty(this, "url");
 *         public final void setUrl(String value) { url.set(value); }
 *         public final String getUrl() { return url.get(); }
 *         public final StringProperty urlProperty() { return url; }
 *
 *         protected Task createTask() {
 *             final String _url = getUrl();
 *             return new Task&ltString&gt;() {
 *                 protected String call() throws Exception {
 *                     URL u = new URL(_url);
 *                     BufferedReader in = new BufferedReader(
 *                             new InputStreamReader(u.openStream()));
 *                     String result = in.readLine();
 *                     in.close();
 *                     return result;
 *                 }
 *             };
 *         }
 *     }
 *     </code></pre>
 * </p>
 * <p>
 *     The Service by default uses a thread pool Executor with some unspecified
 *     default or maximum thread pool size. This is done so that naive code
 *     will not completely swamp the system by creating thousands of Threads.
 * </p>
 * @param <V>
 */
public abstract class Service<V> implements Worker<V>, EventTarget {
    /**
     * Logger used in the case of some uncaught exceptions
     */
    private static final PlatformLogger LOG = Toolkit.getToolkit().getLogger(Service.class.getName());

    /*
        The follow chunk of static state is for defining the default Executor used
        with the Service. This is based on pre-existing JavaFX Script code and
        experience with JavaFX Script. It was necessary to have a thread pool by default
        because we found naive code could totally overwhelm the system otherwise
        by spawning thousands of threads for fetching resources, for example.
        We also set the priority and daemon status of the thread in its thread
        factory.
     */
    private static final int THREAD_POOL_SIZE = 32;
    private static final long THREAD_TIME_OUT = 1000;

    private static final BlockingQueue<Runnable> IO_QUEUE = new LinkedBlockingQueue<Runnable>();
    // Addition of doPrivileged added due to RT-19580
    private static final ThreadGroup THREAD_GROUP = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
        @Override public ThreadGroup run() {
            return new ThreadGroup("javafx concurrent thread pool");
        }
    });
    private static final Thread.UncaughtExceptionHandler UNCAUGHT_HANDLER = new Thread.UncaughtExceptionHandler() {
        @Override public void uncaughtException(Thread thread, Throwable throwable) {
            // Ignore IllegalMonitorStateException, these are thrown from the ThreadPoolExecutor
            // when a browser navigates away from a page hosting an applet that uses
            // asynchronous tasks. These exceptions generally do not cause loss of functionality.
            if (!(throwable instanceof IllegalMonitorStateException)) {
                LOG.warning("Uncaught throwable in " + THREAD_GROUP.getName(), throwable);
            }
        }
    };
    
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override public Thread newThread(final Runnable run) {
            // Addition of doPrivileged added due to RT-19580
            return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                @Override public Thread run() {
                    final Thread th = new Thread(THREAD_GROUP, run);
                    th.setUncaughtExceptionHandler(UNCAUGHT_HANDLER);
                    th.setPriority(Thread.MIN_PRIORITY);
                    th.setDaemon(true);
                    return th;
                }
            });
        }
    };

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            2, THREAD_POOL_SIZE,
            THREAD_TIME_OUT, TimeUnit.MILLISECONDS,
            IO_QUEUE, THREAD_FACTORY, new ThreadPoolExecutor.AbortPolicy());

    static {
        EXECUTOR.allowCoreThreadTimeOut(true);
    }

    private ObjectProperty<State> state = new SimpleObjectProperty<State>(this, "state", State.READY);
    @Override public final State getState() { checkThread(); return state.get(); }
    @Override public final ReadOnlyObjectProperty<State> stateProperty() { checkThread(); return state; }

    private ObjectProperty<V> value = new SimpleObjectProperty<V>(this, "value");
    @Override public final V getValue() { checkThread(); return value.get(); }
    @Override public final ReadOnlyObjectProperty<V> valueProperty() { checkThread(); return value; }

    private ObjectProperty<Throwable> exception = new SimpleObjectProperty<Throwable>(this, "exception");
    @Override public final Throwable getException() { checkThread(); return exception.get(); }
    @Override public final ReadOnlyObjectProperty<Throwable> exceptionProperty() { checkThread(); return exception; }

    private DoubleProperty workDone = new SimpleDoubleProperty(this, "workDone", -1);
    @Override public final double getWorkDone() { checkThread(); return workDone.get(); }
    @Override public final ReadOnlyDoubleProperty workDoneProperty() { checkThread(); return workDone; }

    private DoubleProperty totalWorkToBeDone = new SimpleDoubleProperty(this, "totalWork", -1);
    @Override public final double getTotalWork() { checkThread(); return totalWorkToBeDone.get(); }
    @Override public final ReadOnlyDoubleProperty totalWorkProperty() { checkThread(); return totalWorkToBeDone; }

    private DoubleProperty progress = new SimpleDoubleProperty(this, "progress", -1);
    @Override public final double getProgress() { checkThread(); return progress.get(); }
    @Override public final ReadOnlyDoubleProperty progressProperty() { checkThread(); return progress; }

    private BooleanProperty running = new SimpleBooleanProperty(this, "running", false);
    @Override public final boolean isRunning() { checkThread(); return running.get(); }
    @Override public final ReadOnlyBooleanProperty runningProperty() { checkThread(); return running; }

    private StringProperty message = new SimpleStringProperty(this, "message", "");
    @Override public final String getMessage() { return message.get(); }
    @Override public final ReadOnlyStringProperty messageProperty() { return message; }

    private StringProperty title = new SimpleStringProperty(this, "title", "");
    @Override public final String getTitle() { return title.get(); }
    @Override public final ReadOnlyStringProperty titleProperty() { return title; }

    /**
     * The executor to use for running this Service. If no executor is specified, then
     * a new daemon thread will be created and used for running the Service using some
     * default executor.
     */
    private ObjectProperty<Executor> executor = new SimpleObjectProperty<Executor>(this, "executor");
    public final void setExecutor(Executor value) { checkThread(); executor.set(value); }
    public final Executor getExecutor() { checkThread(); return executor.get(); }
    public final ObjectProperty<Executor> executorProperty() { return executor; }

    /**
     * The onReady event handler is called whenever the Task state transitions
     * to the READY state.
     *
     * @return the onReady event handler property
     */
    public final ObjectProperty<EventHandler<WorkerStateEvent>> onReadyProperty() {
        return getEventHelper().onReadyProperty();
    }

    /**
     * The onReady event handler is called whenever the Task state transitions
     * to the READY state.
     *
     * @return the onReady event handler, if any
     */
    public final EventHandler<WorkerStateEvent> getOnReady() {
        return eventHelper == null ? null : eventHelper.getOnReady();
    }

    /**
     * The onReady event handler is called whenever the Task state transitions
     * to the READY state.
     *
     * @param value the event handler, can be null to clear it
     */
    public final void setOnReady(EventHandler<WorkerStateEvent> value) {
        getEventHelper().setOnReady(value);
    }

    /**
     * A protected convenience method for subclasses, called whenever the
     * state of the Task has transitioned to the READY state.
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
     */
    protected void ready() { }

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
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
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
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
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
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
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
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
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
     * This method is invoked after any listeners of the state property
     * and after the Task has been fully transitioned to the new state.
     */
    protected void failed() { }

    /**
     * A reference to the last task that was executed. I need this reference so that in the
     * restart method I can cancel the currently running task, and so the cancel method
     * can cancel the currently running task.
     */
    private Task<V> task;

    /**
     * Create a new Service.
     */
    protected Service() {
        // Add a listener to the state, such that we can fire the correct event
        // notifications whenever the state of the Service has changed.
        state.addListener(new ChangeListener<State>() {
            @Override public void changed(ObservableValue<? extends State> observableValue,
                                          State oldState, State newState) {
                // Invoke the event handlers, and then call the protected methods.
                switch (state.get()) {
                    case CANCELLED:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_CANCELLED));
                        cancelled();
                        break;
                    case FAILED:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_FAILED));
                        failed();
                        break;
                    case READY:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_READY));
                        ready();
                        break;
                    case RUNNING:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_RUNNING));
                        running();
                        break;
                    case SCHEDULED:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_SCHEDULED));
                        scheduled();
                        break;
                    case SUCCEEDED:
                        fireEvent(new WorkerStateEvent(Service.this, WORKER_STATE_SUCCEEDED));
                        succeeded();
                        break;
                    default: throw new AssertionError("Should be unreachable");
                }
            }
        });
    }

    @Override public final boolean cancel() {
        checkThread();
        if (task == null) {
            if (state.get() == State.CANCELLED || state.get() == State.SUCCEEDED) {
                return false;
            }
            state.set(State.CANCELLED);
            return true;
        } else {
            return task.cancel(true);
        }
    }

    /**
     * Cancels any currently running Task, if any, and restarts this Service. The state
     * will be reset to READY prior to execution. This method should only be called on
     * the FX application thread.
     */
    public void restart() {
        checkThread();

        // Cancel the current task, if there is one
        if (task != null) {
            task.cancel();
            task = null;

            // RT-20880: IllegalStateException thrown from Service#restart()
            // The problem is that the reset method explodes if the state
            // is SCHEDULED or RUNNING. Although we have cancelled the
            // task above, it is possible that cancelling does not change
            // state to the CANCELLED state. However we still need to
            // succeed in resetting. I believe that although the old task is
            // still running away, everything is about to be unbound so
            // we really can just let the old task run and create a new
            // task and the Service will be none the wiser.
            state.unbind();
            state.setValue(State.CANCELLED);
        }

        // Reset
        reset();

        // Start the thing up again.
        start();
    }

    /**
     * Resets the Service. May only be called while in one of the finish states,
     * that is, SUCCEEDED, FAILED, or CANCELLED, or when READY. This method should
     * only be called on the FX application thread.
     */
    public void reset() {
        checkThread();
        final State s = getState();
        if (s == State.SCHEDULED || s == State.RUNNING) {
            throw new IllegalStateException();
        }

        task = null;
        state.unbind();
        state.set(State.READY);
        value.unbind();
        value.set(null);
        exception.unbind();
        exception.set(null);
        workDone.unbind();
        workDone.set(-1);
        totalWorkToBeDone.unbind();
        totalWorkToBeDone.set(-1);
        progress.unbind();
        progress.set(-1);
        running.unbind();
        running.set(false);
        message.unbind();
        message.set("");
        title.unbind();
        title.set("");
    }

    /**
     * Starts this Service. The Service must be in the READY state to succeed in this call.
     * This method should only be called on the FX application thread.
     */
    public void start() {
        checkThread();

        if (getState() != State.READY) {
            throw new IllegalStateException(
                    "Can only start a Service in the READY state. Was in state " + getState());
        }

        // Create the task
        task = createTask();

        // Wire up all the properties so they use this task
        state.bind(task.stateProperty());
        value.bind(task.valueProperty());
        exception.bind(task.exceptionProperty());
        workDone.bind(task.workDoneProperty());
        totalWorkToBeDone.bind(task.totalWorkProperty());
        progress.bind(task.progressProperty());
        running.bind(task.runningProperty());
        message.bind(task.messageProperty());
        title.bind(task.titleProperty());

        // Advance the task to the "SCHEDULED" state
        task.setState(State.SCHEDULED);

        // Start the task
        executeTask(task);
    }

    /**
     * <p>
     *     Uses the <code>executor</code> defined on this Service to execute the
     *     given task. If the <code>executor</code> is null, then a default
     *     executor is used which will create a new daemon thread on which to
     *     execute this task.
     * </p>
     * <p>
     *     This method is intended only to be called by the Service
     *     implementation.
     * </p>
     * @param task a non-null task to execute
     */
    protected void executeTask(Task<V> task) {
        Executor e = getExecutor();
        if (e != null) {
            e.execute(task);
        } else {
            EXECUTOR.execute(task);
        }
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
    protected final void fireEvent(Event event) {
        checkThread();
        getEventHelper().fireEvent(event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return getEventHelper().buildEventDispatchChain(tail);
    }

    /**
     * Invoked after the Service is started on the JavaFX Application Thread.
     * Implementations should save off any state into final variables prior to
     * creating the Task, since accessing properties defined on the Service
     * within the background thread code of the Task will result in exceptions.
     *
     * For example:
     * <pre><code>
     *     protected Task createTask() {
     *         final String url = myService.getUrl();
     *         return new Task&lt;String&gt;() {
     *             protected String call() {
     *                 URL u = new URL("http://www.oracle.com");
     *                 BufferedReader in = new BufferedReader(
     *                         new InputStreamReader(u.openStream()));
     *                 String result = in.readLine();
     *                 in.close();
     *                 return result;
     *             }
     *         }
     *     }
     * </code></pre>
     *
     * <p>
     *     If the Task is a pre-defined class (as opposed to being an
     *     anonymous class), and if it followed the recommended best-practice,
     *     then there is no need to save off state prior to constructing
     *     the Task since its state is completely provided in its constructor.
     * </p>
     *
     * <pre><code>
     *     protected Task createTask() {
     *         // This is safe because getUrl is called on the FX Application
     *         // Thread and the FirstLineReaderTasks stores it as an
     *         // immutable property
     *         return new FirstLineReaderTask(myService.getUrl());
     *     }
     * </code></pre>
     * @return the Task to execute
     */
    protected abstract Task<V> createTask();

    void checkThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Service must only be used from the FX Application Thread");
        }
    }
}
