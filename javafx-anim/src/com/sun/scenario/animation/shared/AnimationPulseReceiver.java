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

package com.sun.scenario.animation.shared;

import com.sun.javafx.animation.TickCalculation;
import javafx.animation.Animation;
import com.sun.scenario.animation.AbstractMasterTimer;

public class AnimationPulseReceiver implements PulseReceiver {

    private final Animation animation;
    private final AbstractMasterTimer timer;

    private long startTime;
    private long pauseTime;
    private boolean paused = false;

    public AnimationPulseReceiver(Animation animation, AbstractMasterTimer timer) {
        this.animation = animation;
        this.timer = timer;
    }

    protected void addPulseReceiver() {
        timer.addPulseReceiver(this);
    }

    protected void removePulseReceiver() {
        timer.removePulseReceiver(this);
    }

    private long now() {
        return TickCalculation.fromNano(timer.nanos());
    }

    public void start(long delay) {
        paused = false;
        startTime = now() + delay;
        addPulseReceiver();
    }

    public void stop() {
        if (!paused) {
            removePulseReceiver();
        }
    }

    public void pause() {
        if (!paused) {
            pauseTime = now();
            paused = true;
            removePulseReceiver();
        }
    }

    public void resume() {
        if (paused) {
            final long deltaTime = now() - pauseTime;
            startTime += deltaTime;
            paused = false;
            addPulseReceiver();
        }
    }

    @Override
    public void timePulse(long now) {
        final long elapsedTime = now - startTime;
        if (elapsedTime < 0) {
            return;
        }

        AnimationAccessor.getDefault().timePulse(animation, elapsedTime);
    }
}
