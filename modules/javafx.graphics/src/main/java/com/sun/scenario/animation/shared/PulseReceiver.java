/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A PulseReceiver can receive regular pulses from the PrimaryTimer. Removing
 * receivers from the PrimaryTimer needs to be in-sync with the
 * timePulse-iteration. The receiver is removed if timePulse returns true.
 * The reason we do not use Callback or some other pre-existing interface
 * is that we want an interface that takes a primitive long, whereas Callback
 * would require a wrapped Long and would have some impact on performance.
 */
public interface PulseReceiver {
    /**
     * Callback triggered to send regular pulses to the PulseReceiver
     *
     * @param now
     *            Timestamp of the pulse.
     * @return true if PulseReceiver should be removed from the PrimaryTimer.
     */
    void timePulse(long now);
}
