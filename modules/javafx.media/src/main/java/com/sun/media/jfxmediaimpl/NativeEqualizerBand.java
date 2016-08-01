/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl;

import com.sun.media.jfxmedia.effects.EqualizerBand;

final class NativeEqualizerBand implements EqualizerBand {

    private final long bandRef; // Native band backend

    private NativeEqualizerBand(long bandRef) {
        if (bandRef != 0) {
            this.bandRef = bandRef;
        } else {
            throw new IllegalArgumentException("bandRef == 0");
        }
    }

    @Override
    public double getCenterFrequency() {
        return nativeGetCenterFrequency(bandRef);
    }

    @Override
    public void setCenterFrequency(double centerFrequency) {
        nativeSetCenterFrequency(bandRef, centerFrequency);
    }

    @Override
    public double getBandwidth() {
        return nativeGetBandwidth(bandRef);
    }

    @Override
    public void setBandwidth(double bandwidth) {
        nativeSetBandwidth(bandRef, bandwidth);
    }

    @Override
    public double getGain() {
        return nativeGetGain(bandRef);
    }

    @Override
    public void setGain(double gain) {
        if (gain >= MIN_GAIN && gain <= MAX_GAIN) {
            nativeSetGain(bandRef, gain);
        }
    }

    //**************************************************************************
    //***** JNI methods
    //**************************************************************************
    private native double nativeGetCenterFrequency(long bandRef);
    private native void   nativeSetCenterFrequency(long bandRef, double centerFrequency);
    private native double nativeGetBandwidth(long bandRef);
    private native void   nativeSetBandwidth(long bandRef, double bandwidth);
    private native double nativeGetGain(long bandRef);
    private native void   nativeSetGain(long bandRef, double gain);
}
