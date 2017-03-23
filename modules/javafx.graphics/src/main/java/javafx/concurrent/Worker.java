/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 * <p>
 *     A Worker is an object which performs some work in one or more background
 *     threads, and whose state is observable and available to JavaFX applications
 *     and is usable from the main JavaFX Application thread. This interface is
 *     primarily implemented by both {@link Task} and {@link Service}, providing
 *     a common API among both classes which makes it easier for libraries and
 *     frameworks to provide workers which work well when developing user interfaces.
 * </p>
 * <p>
 *     A Worker may or may not be reusable depending on the implementation. A
 *     {@link Task}, for example, is not reusable while a {@link Service} is.
 * </p>
 * <p>
 *     A Worker has a well defined life cycle. Every Worker begins in the
 *     {@link State#READY} state. When the Worker has been scheduled for work
 *     (for example, when a Service's {@link javafx.concurrent.Service#start()}
 *     method is called), it is transitioned to {@link State#SCHEDULED}. Even
 *     Workers which are not technically scheduled, but started immediately
 *     (such as with {@link javafx.concurrent.Task#run()}) will transition through
 *     the {@link State#SCHEDULED} on its way to the {@link State#RUNNING} state.
 * </p>
 * <p>
 *     When the Worker is actually performing its work, the state will have been
 *     transitioned to {@link State#RUNNING}. If the Worker completes normally,
 *     it will end in the {@link State#SUCCEEDED} state, and the result of the
 *     Worker will be set as the <code>value</code> property. If however an Exception
 *     occurs during the execution of the Worker, then the state will be set to
 *     {@link State#FAILED} and the <code>exception</code> property will be set
 *     to the Exception which occurred.
 * </p>
 * <p>
 *     At any time prior to the conclusion of the Worker (that is, if the state
 *     is not already {@link State#SUCCEEDED} or {@link State#FAILED}) the developer
 *     may invoke the {@link javafx.concurrent.Worker#cancel()} method. If called, the
 *     Worker will cease execution (if possible, including use of Thread.interrupt)
 *     and the state changed to {@link State#CANCELLED}.
 * </p>
 * <p>
 *     The only valid beginning state for a Worker is {@link State#READY}, and the
 *     valid ending states are {@link State#CANCELLED}, {@link State#SUCCEEDED},
 *     and {@link State#FAILED}. The <code>running</code> property is set to
 *     true when the state is either {@link State#SCHEDULED} or {@link State#RUNNING}.
 * </p>
 * <p>
 *     The Worker's progress can be monitored via three different properties,
 *     <code>totalWork</code>, <code>workDone</code>, and <code>progress</code>.
 *     These properties are set by the actual implementation of the Worker
 *     interface, but can be observed by anybody. The <code>workDone</code> is
 *     a number between -1 (meaning indeterminate progress) and
 *     <code>totalWork</code>, inclusive. When <code>workDone == totalWork</code>
 *     the <code>progress</code> will be 100% (or 1). <code>totalWork</code>
 *     will be a number between -1 and Long.MAX_VALUE, inclusive. The progress
 *     will be either -1 (meaning indeterminate), or a value between 0 and 1, inclusive,
 *     representing 0% through 100%.
 * </p>
 * <p>
 *     A Worker which is in the {@link State#READY} or {@link State#SCHEDULED} states
 *     will always have <code>workDone</code> and <code>progress</code> set to -1.
 *     A Worker which is in the {@link State#SUCCEEDED} state will always have
 *     <code>workDone == totalWork</code> and <code>progress == 1</code>. In any
 *     other state, the values for these properties may be any value in their
 *     respective valid ranges.
 * </p>
 * @since JavaFX 2.0
 */
public interface Worker<V> {
    /**
     * <p>
     *     The state of a Worker. The state transitions in a Worker are very well defined.
     *     All Workers begin in the READY state. In some circumstances, a Worker might
     *     be scheduled for execution before it is actually executed. In such cases,
     *     it is sometimes useful to know when the Worker has been SCHEDULED separately
     *     from when it is RUNNING. However even in cases where the Worker is executed
     *     immediately, the Worker will temporarily enter the SCHEDULED state before
     *     entering the RUNNING state. That is, the transition is always from
     *     READY to SCHEDULED to RUNNING (unless of course the Worker in cancelled).
     * </p>
     * <p>
     *     A Worker which runs but is never cancelled can only end up in one of two
     *     states, either SUCCEEDED or FAILED. It only enters FAILED if an exception
     *     was thrown during the execution of the Worker. A Worker may be cancelled when
     *     READY, SCHEDULED, or RUNNING, in which case the final status will be CANCELLED.
     *     When a Worker is cancelled in one of these circumstances it will transition
     *     immediately to the CANCELLED state.
     * </p>
     * <p>
     *     A reusable Worker will transition from CANCELLED, SUCCEEDED or FAILED back to
     *     READY. From that point the normal state transitions are again followed.
     * </p>
     * @since JavaFX 2.0
     */
    public enum State {
        /**
         * Indicates that the Worker has not yet been executed and is ready
         * to be executed, or that it has been reinitialized. This is the
         * default initial state of the Worker.
         */
        READY,
        /**
         * Indicates that the Worker has been scheduled for execution, but
         * that it is not currently running. This might be because the
         * Worker is waiting for a thread in a thread pool to become
         * available before it can start running.
         */
        SCHEDULED,
        /**
         * Indicates that this Worker is running. This is set just immediately
         * prior to the Worker actually doing its first bit of work.
         */
        RUNNING,
        /**
         * Indicates that this Worker has completed successfully, and that there
         * is a valid result ready to be read from the <code>value</code> property.
         */
        SUCCEEDED,
        /**
         * Indicates that this Worker has been cancelled via the {@link #cancel()}
         * method.
         */
        CANCELLED,
        /**
         * Indicates that this Worker has failed, usually due to some unexpected
         * condition having occurred. The exception can be retrieved from the
         * <code>exception</code> property.
         */
        FAILED
    }

    /**
     * Specifies the current state of this Worker. The initial value is State.READY.
     * A Task may be restarted, in which case it will progress from one of these
     * end states (SUCCEEDED, CANCELLED, or FAILED) back to READY and then
     * immediately to SCHEDULED and RUNNING. These state transitions may occur
     * immediately one after the other, but will always occur in the prescribed order.
     *
     * @return The current state of this Worker
     */
    public State getState();

    /**
     * Gets the ReadOnlyObjectProperty representing the current state.
     *
     * @return The property representing the state
     */
    public ReadOnlyObjectProperty<State> stateProperty();

    /**
     * Specifies the value, or result, of this Worker. This is set upon entering
     * the SUCCEEDED state, and cleared (set to null) if the Worker is reinitialized
     * (that is, if the Worker is a reusable Worker and is reset or restarted).
     *
     * @return the current value of this Worker
     */
    public V getValue();

    /**
     * Gets the ReadOnlyObjectProperty representing the value.
     *
     * @return The property representing the current value
     */
    public ReadOnlyObjectProperty<V> valueProperty();

    /**
     * Indicates the exception which occurred while the Worker was running, if any.
     * If this property value is {@code null}, there is no known exception, even if
     * the status is FAILED. If this property is not {@code null}, it will most
     * likely contain an exception that describes the cause of failure.
     *
     * @return the exception, if one occurred
     */
    public Throwable getException();

    /**
     * Gets the ReadOnlyObjectProperty representing any exception which occurred.
     *
     * @return the property representing the exception
     */
    public ReadOnlyObjectProperty<Throwable> exceptionProperty();

    /**
     * Indicates the current amount of work that has been completed. Zero or a
     * positive value indicate progress toward completion. This variables value
     * may or may not change from its default value depending on the specific
     * Worker implementation. A value of -1 means that the current amount of work
     * done cannot be determined (ie: it is indeterminate). The value of
     * this property is always less than or equal to totalWork.
     *
     * @see #totalWorkProperty
     * @see #progressProperty
     * @return the amount of work done
     */
    public double getWorkDone();

    /**
     * Gets the ReadOnlyDoubleProperty representing the current progress.
     *
     * @return The property representing the amount of work done
     */
    public ReadOnlyDoubleProperty workDoneProperty();

    /**
     * Indicates a maximum value for the {@link #workDoneProperty} property. The
     * totalWork will either be -1 (indicating that the amount of work
     * to do is indeterminate), or it will be a non-zero value less than or
     * equal to Double.MAX_VALUE.
     *
     * @see #workDoneProperty
     * @see #progressProperty
     * @return the total work to be done
     */
    public double getTotalWork();

    /**
     * Gets the ReadOnlyDoubleProperty representing the maximum amount of work
     * that needs to be done. These "work units" have meaning to the Worker
     * implementation, such as the number of bytes that need to be downloaded
     * or the number of images to process or some other such metric.
     *
     * @return the property representing the total work to be done
     */
    public ReadOnlyDoubleProperty totalWorkProperty();

    /**
     * Indicates the current progress of this Worker in terms of percent complete.
     * A value between zero and one indicates progress toward completion. A value
     * of -1 means that the current progress cannot be determined (that is, it is
     * indeterminate). This property may or may not change from its default value
     * of -1 depending on the specific Worker implementation.
     *
     * @see #workDoneProperty
     * @see #totalWorkProperty
     * @return the current progress
     */
    public double getProgress();

    /**
     * Gets the ReadOnlyDoubleProperty representing the progress.
     *
     * @return the property representing the progress
     */
    public ReadOnlyDoubleProperty progressProperty();

    /**
     * True if the state is either SCHEDULED or RUNNING. When binding a Worker to a
     * {@link javafx.scene.control.ProgressIndicator}, you will typically bind the visibility
     * of the ProgressIndicator to the Worker's running property, and the progress of the
     * ProgressIndicator to the Worker's progress property.
     *
     * @return true if this Worker is running
     */
    public boolean isRunning();

    /**
     * Gets the ReadOnlyBooleanProperty representing whether the Worker is running.
     *
     * @return the property representing whether the worker is running
     */
    public ReadOnlyBooleanProperty runningProperty();

    /**
     * Gets a message associated with the current state of this Worker. This may
     * be something such as "Processing image 1 of 3", for example.
     *
     * @return the current message
     */
    public String getMessage();

    /**
     * Gets the ReadOnlyStringProperty representing the message.
     *
     * @return a property representing the current message
     */
    public ReadOnlyStringProperty messageProperty();

    /**
     * An optional title that should be associated with this Worker.
     * This may be something such as "Modifying Images".
     *
     * @return the current title
     */
    public String getTitle();

    /**
     * Gets the ReadOnlyStringProperty representing the title.
     *
     * @return the property representing the current title
     */
    public ReadOnlyStringProperty titleProperty();

    /**
     * Terminates execution of this Worker. Calling this method will either
     * remove this Worker from the execution queue or stop execution.
     *
     * @return returns true if the cancel was successful
     */
    public boolean cancel();
}
