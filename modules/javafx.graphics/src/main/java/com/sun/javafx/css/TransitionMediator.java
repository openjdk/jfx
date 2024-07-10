/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import javafx.css.StyleableProperty;

/**
 * {@code TransitionMediator} encapsulates the interactions between a {@link TransitionTimer} and its target.
 */
public abstract class TransitionMediator {

    private TransitionTimer timer;

    /**
     * Starts the transition timer with the specified transition definition.
     *
     * @param definition the transition definition
     */
    public final void run(TransitionDefinition definition) {
        // Might return 'null' if the transition duration is zero or the target node is not showing.
        timer = TransitionTimer.run(this, definition);

        // If no timer was started, we complete the transition immediately.
        if (timer == null) {
            onUpdate(1);
            onStop();
        }
    }

    /**
     * Cancels the transition timer.
     *
     * @param forceStop if {@code true}, the transition timer is stopped unconditionally
     * @return {@code true} if the timer was cancelled, {@code false} otherwise
     * @see TransitionTimer#cancel(boolean)
     */
    public final boolean cancel(boolean forceStop) {
        return timer == null || timer.cancel(forceStop);
    }

    /**
     * Gets the running {@code TransitionTimer}.
     *
     * @return the {@code TransitionTimer}, or {@code null} if no timer is running
     */
    public final TransitionTimer getTimer() {
        return timer;
    }

    /**
     * Returns the styleable property targeted by the transition.
     *
     * @return the styleable property
     */
    public abstract StyleableProperty<?> getStyleableProperty();

    /**
     * Derived classes should implement this method to compute a new intermediate value
     * based on the current progress, and update the {@link StyleableProperty} accordingly.
     *
     * @param progress the progress of the transition, ranging from 0 to 1
     */
    public abstract void onUpdate(double progress);

    /**
     * Occurs when the timer has stopped and the mediator should be discarded.
     * Derived classes should implement this method to clear any references to this mediator.
     */
    public abstract void onStop();
}
