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
import javafx.scene.Node;

public abstract class TransitionShim extends Transition {

    protected TransitionShim() {
        super();
    }

    protected TransitionShim(double targetFramerate) {
        super(targetFramerate);
    }

    protected TransitionShim(AbstractMasterTimer timer) {
        super(timer);
    }

    @Override
    public void impl_pause() {
        super.impl_pause();
    }

    @Override
    public void impl_sync(boolean forceSync) {
        super.impl_sync(forceSync);
    }

    @Override
    public void impl_jumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
        super.impl_jumpTo(currentTicks, cycleTicks, forceJump);
    }

    public void shim_impl_finished() {
        super.impl_finished();
    }

    @Override
    public Node getParentTargetNode() {
        return super.getParentTargetNode();
    }

    @Override
    public void impl_start(boolean forceSync) {
        super.impl_start(forceSync);
    }

    @Override
    public boolean impl_startable(boolean forceSync) {
        return super.impl_startable(forceSync);
    }

    @Override
    public void impl_playTo(long currentTicks, long cycleTicks) {
        super.impl_playTo(currentTicks, cycleTicks);
    }

    @Override
    public Interpolator getCachedInterpolator() {
        return super.getCachedInterpolator();
    }

    ///// statics

    public static void interpolate(Transition t, double frac) {
        t.interpolate(frac);
    }

}
