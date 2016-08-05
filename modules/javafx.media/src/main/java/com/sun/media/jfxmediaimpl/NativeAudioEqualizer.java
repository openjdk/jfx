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

import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.EqualizerBand;

final class NativeAudioEqualizer implements AudioEqualizer {
    /**
     * Handle to the native equalizer.
     */
    private final long nativeRef;

    //**************************************************************************
    //***** Constructors
    //**************************************************************************

    /**
     * Constructor.
     * @param nativeRef A reference to the native component.
     */
    NativeAudioEqualizer(long nativeRef) {
        if (nativeRef == 0) {
            throw new IllegalArgumentException("Invalid native media reference");
        }

        this.nativeRef = nativeRef;
    }

    //**************************************************************************
    //***** Public functions
    //**************************************************************************

    @Override
    public boolean getEnabled() {
        return nativeGetEnabled(nativeRef);
    }

    @Override
    public void setEnabled(boolean enable) {
        nativeSetEnabled(nativeRef, enable);
    }

    @Override
    public EqualizerBand addBand(double centerFrequency, double bandwidth, double gain) {
        return (nativeGetNumBands(nativeRef) >= MAX_NUM_BANDS &&
                gain >= EqualizerBand.MIN_GAIN && gain <= EqualizerBand.MAX_GAIN) ?
                null : nativeAddBand(nativeRef, centerFrequency, bandwidth, gain);
    }

    @Override
    public boolean removeBand(double centerFrequency) {
        return (centerFrequency > 0) ? nativeRemoveBand(nativeRef, centerFrequency) : false;
    }

    //**************************************************************************
    //***** JNI methods
    //**************************************************************************
    private native boolean nativeGetEnabled(long nativeRef);
    private native void nativeSetEnabled(long nativeRef, boolean enable);
    private native int nativeGetNumBands(long nativeRef);
    private native EqualizerBand nativeAddBand(long nativeRef,
                                               double centerFrequency, double bandwidth,
                                               double gain);
    private native boolean nativeRemoveBand(long nativeRef, double centerFrequency);
}
