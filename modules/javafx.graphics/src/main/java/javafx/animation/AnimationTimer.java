/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.shared.TimerReceiver;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * The class {@code AnimationTimer} allows to create a timer, that is called in
 * each frame while it is active.
 *
 * An extending class has to override the method {@link #handle(long)} which
 * will be called in every frame.
 *
 * The methods {@link AnimationTimer#start()} and {@link #stop()} allow to start
 * and stop the timer.
 *
 *
 * @since JavaFX 2.0
 */
public abstract class AnimationTimer {

    private class AnimationTimerReceiver implements TimerReceiver {
        @Override public void handle(final long now) {
            if (accessCtrlCtx == null) {
                throw new IllegalStateException("Error: AccessControlContext not captured");
            }

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                AnimationTimer.this.handle(now);
                return null;
            }, accessCtrlCtx);
        }
    }

    private final AbstractPrimaryTimer timer;
    private final AnimationTimerReceiver timerReceiver = new AnimationTimerReceiver();
    private boolean active;

    // Access control context, captured in start()
    private AccessControlContext accessCtrlCtx = null;

    /**
     * Creates a new timer.
     */
    public AnimationTimer() {
        timer = Toolkit.getToolkit().getPrimaryTimer();
    }

    // For testing only
    AnimationTimer(AbstractPrimaryTimer timer) {
        this.timer = timer;
    }

    /**
     * This method needs to be overridden by extending classes. It is going to
     * be called in every frame while the {@code AnimationTimer} is active.
     *
     * @param now
     *            The timestamp of the current frame given in nanoseconds. This
     *            value will be the same for all {@code AnimationTimers} called
     *            during one frame.
     */
    public abstract void handle(long now);

    /**
     * Starts the {@code AnimationTimer}. Once it is started, the
     * {@link #handle(long)} method of this {@code AnimationTimer} will be
     * called in every frame.
     *
     * The {@code AnimationTimer} can be stopped by calling {@link #stop()}.
     */
    public void start() {
        if (!active) {
            // Capture the Access Control Context to be used during the animation pulse
            accessCtrlCtx = AccessController.getContext();
            timer.addAnimationTimer(timerReceiver);
            active = true;
        }
    }

    /**
     * Stops the {@code AnimationTimer}. It can be activated again by calling
     * {@link #start()}.
     */
    public void stop() {
        if (active) {
            timer.removeAnimationTimer(timerReceiver);
            active = false;
        }
    }
}
