/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.animation.shared.ClipEnvelope;
import com.sun.scenario.animation.shared.PulseReceiver;
import javafx.util.Duration;

public abstract class AnimationShim extends Animation {

    public AnimationShim() {
        super();
    }

    public AnimationShim(AbstractMasterTimer timer) {
        super(timer);
    }

    public AnimationShim(AbstractMasterTimer timer, ClipEnvelope clipEnvelope, int resolution) {
        super(timer, clipEnvelope, resolution);
    }

    public ClipEnvelope get_clipEnvelope() {
        return clipEnvelope;
    }

    @Override
    public void impl_pause() {
        super.impl_pause();
    }

    @Override
    public void impl_start(boolean forceSync) {
        super.impl_start(forceSync);
    }

    @Override
    public void impl_setCurrentRate(double currentRate) {
        super.impl_setCurrentRate(currentRate);
    }

    @Override
    public void impl_setCurrentTicks(long ticks) {
        super.impl_setCurrentTicks(ticks);
    }

    @Override
    public boolean impl_startable(boolean forceSync) {
        return super.impl_startable(forceSync);
    }

    @Override
    public void impl_stop() {
        super.impl_stop();
    }

    @Override
    public void impl_sync(boolean forceSync) {
        super.impl_sync(forceSync);
    }

    @Override
    public void impl_timePulse(long elapsedTime) {
        super.impl_timePulse(elapsedTime);
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

    public void shim_impl_finished() {
        impl_finished();
    }

    @Override
    abstract public void impl_playTo(long currentTicks, long cycleTicks);

    @Override
    abstract public void impl_jumpTo(long currentTicks, long cycleTicks, boolean forceJump);

    //-------------------------------

    public static void impl_finished(Animation a) {
        a.impl_finished();
    }

    public static void impl_start(Animation a, boolean forceSync) {
        a.impl_start(forceSync);
    }

    public static boolean impl_startable(Animation a, boolean forceSync) {
        return a.impl_startable(forceSync);
    }

}
