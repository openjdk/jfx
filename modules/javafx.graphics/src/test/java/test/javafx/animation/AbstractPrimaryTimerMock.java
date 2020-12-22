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

package test.javafx.animation;

import java.util.HashSet;
import java.util.Set;
import com.sun.javafx.animation.TickCalculation;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractPrimaryTimer;
import com.sun.scenario.animation.shared.PulseReceiver;

public class AbstractPrimaryTimerMock extends AbstractPrimaryTimer {

    private final Set<PulseReceiver> targets = new HashSet<PulseReceiver>();

    private long nanos;

    public void setNanos(long nanos) {
        this.nanos = nanos;
    }

    @Override
    public long nanos() {
        return nanos;
    }

    @Override
    protected void postUpdateAnimationRunnable(DelayedRunnable animationRunnable) {
    }

    @Override
    protected int getPulseDuration(int precision) {
        return precision / 60;
    }

    @Override
    public void addPulseReceiver(PulseReceiver target) {
        super.addPulseReceiver(target);
        targets.add(target);
    }

    @Override
    public void removePulseReceiver(PulseReceiver target) {
        super.addPulseReceiver(target);
        targets.remove(target);
    }

    public boolean containsPulseReceiver(PulseReceiver target) {
        return targets.contains(target);
    }

    public void pulse() {
        nanos += TickCalculation.toMillis(100) * 1000000L;
        for (PulseReceiver pr : targets) {
            pr.timePulse(TickCalculation.fromNano(nanos));
        }
    }

}
