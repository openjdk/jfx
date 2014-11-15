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

import com.sun.media.jfxmedia.effects.AudioSpectrum;

final class NativeAudioSpectrum implements AudioSpectrum {
    private static final float[] EMPTY_FLOAT_ARRAY  = new float[0];
    public static final int      DEFAULT_THRESHOLD = -60;
    public static final int      DEFAULT_BANDS = 128;
    public static final double   DEFAULT_INTERVAL = 0.1;

    /**
     * Handle to the native spectrum.
     */
    private final long nativeRef;

    private float[] magnitudes = EMPTY_FLOAT_ARRAY;
    private float[] phases = EMPTY_FLOAT_ARRAY;

    //**************************************************************************
    //***** Constructors
    //**************************************************************************

    /**
     * Constructor.
     * @param refNativePlayer A reference to the native player.
     */
    NativeAudioSpectrum(long refMedia) {
        if (refMedia == 0) {
            throw new IllegalArgumentException("Invalid native media reference");
        }

        this.nativeRef = refMedia;
        setBandCount(DEFAULT_BANDS);
    }

    //**************************************************************************
    //***** Public functions
    //**************************************************************************
    @Override
    public boolean getEnabled() {
        return nativeGetEnabled(nativeRef);
    }

    @Override
    public void setEnabled(boolean enabled) {
        nativeSetEnabled(nativeRef, enabled);
    }

    @Override
    public int getBandCount() {
        // just return the current size of one of the band arrays
        return phases.length;
    }

    @Override
    public void setBandCount(int bands) {
        if (bands > 1) {
            magnitudes = new float[bands];
            for (int i = 0; i < magnitudes.length; i++) {
                magnitudes[i] = (float)DEFAULT_THRESHOLD;//Float.NEGATIVE_INFINITY;
            }

            phases = new float[bands];
            nativeSetBands(nativeRef, bands, magnitudes, phases);
        } else {
            magnitudes = EMPTY_FLOAT_ARRAY;
            phases = EMPTY_FLOAT_ARRAY;

            throw new IllegalArgumentException("Number of bands must at least be 2");
        }
    }

    @Override
    public double getInterval() {
        return nativeGetInterval(nativeRef);
    }

    @Override
    public void setInterval(double interval) {
        if (interval * NativeMediaPlayer.ONE_SECOND >= 1) {
            nativeSetInterval(nativeRef, interval);
        } else {
            throw new IllegalArgumentException("Interval can't be less that 1 nanosecond");
        }
    }

    @Override
    public int getSensitivityThreshold() {
        return nativeGetThreshold(nativeRef);
    }

    @Override
    public void setSensitivityThreshold(int threshold) {
        if (threshold <= 0) {
            nativeSetThreshold(nativeRef, threshold);
        } else {
            throw new IllegalArgumentException(String.format("Sensitivity threshold must be less than 0: %d", threshold));
        }
    }

    @Override
    public float[] getMagnitudes(float[] mag) {
        int size = magnitudes.length;
        if(mag == null || mag.length < size) {
            mag = new float[size];
        }
        System.arraycopy(magnitudes, 0, mag, 0, size);
        return mag;
    }

    @Override
    public float[] getPhases(float[] phs) {
        int size = phases.length;
        if(phs == null || phs.length < size) {
            phs = new float[size];
        }
        System.arraycopy(phases, 0, phs, 0, size);
        return phs;
    }

    //**************************************************************************
    //***** JNI methods
    //**************************************************************************
    private native boolean nativeGetEnabled(long nativeRef);
    private native void    nativeSetEnabled(long nativeRef, boolean enable);
    private native void    nativeSetBands(long nativeRef, int bands, float[] magnitudes, float[] phases);
    private native double  nativeGetInterval(long nativeRef);
    private native void    nativeSetInterval(long nativeRef, double interval);
    private native int     nativeGetThreshold(long nativeRef);
    private native void    nativeSetThreshold(long nativeRef, int threshold);
}
