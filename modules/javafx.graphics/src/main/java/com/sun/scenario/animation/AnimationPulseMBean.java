/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation;

public interface AnimationPulseMBean {
    public boolean getEnabled();

    public void setEnabled(boolean enabled);

    // we are using millis as time units here

    public long getPULSE_DURATION();

    public long getSkippedPulses();

    public long getSkippedPulsesIn1Sec();

    // from the scheduled time
    public long getStartMax();

    public long getStartMaxIn1Sec();

    public long getStartAv();

    public long getStartAvIn100Millis();

    public long getEndMax();

    public long getEndMaxIn1Sec();

    public long getEndAv();

    public long getEndAvIn100Millis();

    public long getAnimationDurationMax();

    public long getAnimationMaxIn1Sec();

    public long getAnimationDurationAv();

    public long getAnimationDurationAvIn100Millis();

    public long getPaintingDurationMax();

    public long getPaintingDurationMaxIn1Sec();

    public long getPaintingDurationAv();

    public long getPaintingDurationAvIn100Millis();

    public long getScenePaintingDurationMaxIn1Sec();

    public long getPaintingPreparationDurationMaxIn1Sec();

    public long getPaintingFinalizationDurationMaxIn1Sec();

    public long getPulseDurationMax();

    public long getPulseDurationMaxIn1Sec();

    public long getPulseDurationAv();

    public long getPulseDurationAvIn100Millis();
}
