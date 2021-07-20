/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.shared.ClipEnvelope;
import com.sun.scenario.animation.shared.PulseReceiver;
import javafx.util.Duration;

public abstract class AnimationShim extends Animation {

    public AnimationShim() {
        super();
    }

    public AnimationShim(AbstractPrimaryTimer timer) {
        super(timer);
    }

    public AnimationShim(AbstractPrimaryTimer timer, ClipEnvelope clipEnvelope, int resolution) {
        super(timer, clipEnvelope, resolution);
    }

    public ClipEnvelope get_clipEnvelope() {
        return clipEnvelope;
    }

    public void setClipEnvelope(ClipEnvelope clipEnvelope) {
        this.clipEnvelope= clipEnvelope;
    }

    @Override
    public void doPause() {
        super.doPause();
    }

    @Override
    public void doStart(boolean forceSync) {
        super.doStart(forceSync);
    }

    @Override
    public void setCurrentRate(double currentRate) {
        super.setCurrentRate(currentRate);
    }

    @Override
    public void setCurrentTicks(long ticks) {
        super.setCurrentTicks(ticks);
    }

    @Override
    public boolean startable(boolean forceSync) {
        return super.startable(forceSync);
    }

    @Override
    public void doStop() {
        super.doStop();
    }

    @Override
    public void sync(boolean forceSync) {
        super.sync(forceSync);
    }

    @Override
    public void doTimePulse(long elapsedTime) {
        super.doTimePulse(elapsedTime);
    }

    @Override
    public void pauseReceiver() {
        super.pauseReceiver();
    }

    @Override
    public void resumeReceiver() {
        super.resumeReceiver();
    }

    public void shim_setCycleDuration(Duration value) {
        setCycleDuration(value);
    }

    @Override
    public void startReceiver(long delay) {
        super.startReceiver(delay);
    }

    public PulseReceiver shim_pulseReceiver() {
        return pulseReceiver;
    }

    public void shim_finished() {
        finished();
    }

    @Override
    abstract public void doPlayTo(long currentTicks, long cycleTicks);

    @Override
    abstract public void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump);

    //-------------------------------

    public static void finished(Animation a) {
        a.finished();
    }

    public static void doStart(Animation a, boolean forceSync) {
        a.doStart(forceSync);
    }

    public static boolean startable(Animation a, boolean forceSync) {
        return a.startable(forceSync);
    }

}
