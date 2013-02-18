/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.ToolkitAccessor;
import com.sun.scenario.animation.AbstractMasterTimer;

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
 */
public abstract class AnimationTimer {
    
    private final AbstractMasterTimer timer;
    private boolean active;
    
    public AnimationTimer() {
        timer = ToolkitAccessor.getMasterTimer();
    }
    
    // For testing only
    AnimationTimer(AbstractMasterTimer timer) {
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
     * Starts the {@code AnimationTimers}. Once it is started, the
     * {@link #handle(long)} method of this {@code AnimationTimers} will be
     * called in every frame.
     * 
     * The {@code AnimationTimers} can be stopped by calling {@link #stop()}.
     */
    public void start() {
        if (!active) {
            timer.addAnimationTimer(this);
            active = true;
        }
    }

    /**
     * Stops the {@code AnimationTimers}. It can be activated again by calling
     * {@link #start()}.
     */
    public void stop() {
        if (active) {
            timer.removeAnimationTimer(this);
            active = false;
        }
    }
}
