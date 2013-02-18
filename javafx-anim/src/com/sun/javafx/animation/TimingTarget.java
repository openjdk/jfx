/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.animation;

/**
 * This interface provides the methods which are called by Clip during the
 * course of a timing observableArrayList. Applications that wish to receive
 * timing events will either create a subclass of TimingTargetAdapter and
 * override or they can create or use an implementation of TimingTarget. A
 * TimingTarget can be passed into the constructor of Clip or set later with the
 * {@link TimelineClip#addTarget(TimingTarget)} method. Any Clip may have
 * multiple TimingTargets.
 */
public interface TimingTarget {
    /**
     * This method will receive all of the timing events from a Clip during an
     * animation.
     * 
     * @param totalElapsed
     *            the amount of time that has elapsed relative to the start time
     *            of a Clip, in milliseconds.
     */
    public void timingEvent(long totalElapsed);

    /**
     * Called when the Clip's animation begins. This provides a chance for
     * targets to perform any setup required at animation start time.
     */
    public void begin();

    /**
     * Called when the Clip's animation ends.
     */
    public void end();

    /**
     * Called when the direction of a clip changes.
     */
    public void toggle();

    /**
     * Called when the Clip's animation is paused.
     */
    public void pause();

    /**
     * Called when the Clip's animation resumes from a paused state.
     */
    public void resume();
}
