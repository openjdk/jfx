/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An object representing a nested event loop.
 */
public final class EventLoop {

    private static final Deque<EventLoop> stack = new ArrayDeque<>();

    /**
     * Possible states for a nested event loop object.
     */
    public static enum State {
        /**
         * A free EventLoop object that can be used to start a new nested event loop.
         */
        IDLE,

        /**
         * The nested loop represented by this object is currently running.
         */
        ACTIVE,

        /**
         * The nested loop has been requested to leave.
         */
        LEAVING
    }

    private State state = State.IDLE;
    private Object returnValue;

    EventLoop() {
        Application.checkEventThread();
    }

    /**
     * Gets the current {@link State} of this EventLoop instance.
     * This method must only be invoked on the main (event handling)
     * thread.
     */
    public State getState() {
        Application.checkEventThread();
        return state;
    }

    /**
     * Starts a nested event loop.
     *
     * The EventLoop object must be in the {@code IDLE} state when calling this
     * method. Upon entering the nested event loop, the state of the object
     * changes to {@code ACTIVE}. When the method returns, the EventLoop object
     * is back to the {@code IDLE} state.
     *
     * Calling this method temporarily blocks processing of the current event,
     * and starts a nested event loop to handle other native events.  To
     * proceed with the blocked execution path, the application should call the
     * {@link #leave(Object)} method.
     *
     * Note that this method must only be invoked on the main (event handling)
     * thread.
     *
     * An application may enter several nested loops recursively. Each nested
     * event loop must be represented by a separate EventLoop instance. There's
     * no limit of recursion other than that imposed by the native stack size.
     *
     * @return an object passed to the leave() method
     * @throws RuntimeException if the current thread is not the main thread
     * @throws IllegalStateException if the EventLoop object isn't IDLE
     */
    public Object enter() {
        Application.checkEventThread();
        if (!state.equals(State.IDLE)) {
            throw new IllegalStateException("The event loop object isn't idle");
        }

        state = State.ACTIVE;
        stack.push(this);
        try {
            Object ret = Application.enterNestedEventLoop();
            assert ret == this : "Internal inconsistency - wrong EventLoop";
            assert stack.peek() == this : "Internal inconsistency - corrupted event loops stack";
            assert state.equals(State.LEAVING) : "The event loop isn't leaving";

            return returnValue;
        } finally {
            returnValue = null;
            state = State.IDLE;
            stack.pop();

            if (!stack.isEmpty() && stack.peek().state.equals(State.LEAVING)) {
                Application.invokeLater(() -> {
                    EventLoop loop = stack.peek();
                    // we might have already entered another loop, so check again
                    if (loop != null && loop.state.equals(State.LEAVING)) {
                        Application.leaveNestedEventLoop(loop);
                    }
                });
            }
        }
    }

    /**
     * Requests this nested event loop to terminate.
     *
     * The EventLoop object must be in the {@code ACTIVE} state when calling
     * this method. This method switches the state of the object to {@code
     * LEAVING}.
     *
     * After calling this method and returning from the current event handler,
     * the execusion returns to the point where the {@link #enter()} method
     * was called previously. You may specify a return value for the
     * enter() method by passing the argument {@code retValue} to
     * the leave().
     *
     * Calls to enter() and leave() may be interleaved for different event
     * loops.  If the EventLoop object is not innermost (i.e. not most recently
     * started), calling leave() just switches this EventLoop object to the
     * {@code LEAVING} state w/o actually terminating it after returning from
     * the current event handler. The enter() method that started this event
     * loop only returns after all inner event loops are terminated.
     *
     * Note that this method must only be invoked on the main (event handling)
     * thread.
     *
     * @throws RuntimeException if the current thread is not the main thread
     * @throws IllegalStateException if the nested event loop isn't ACTIVE
     */
    public void leave(Object ret) {
        Application.checkEventThread();
        if (!state.equals(State.ACTIVE)) {
            throw new IllegalStateException("The event loop object isn't active");
        }

        state = State.LEAVING;
        returnValue = ret;

        if (stack.peek() == this) {
            Application.leaveNestedEventLoop(this);
        }
    }
}

