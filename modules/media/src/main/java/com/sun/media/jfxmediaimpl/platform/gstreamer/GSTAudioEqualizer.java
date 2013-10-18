/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform.gstreamer;

import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.EqualizerBand;

final class GSTAudioEqualizer implements AudioEqualizer {
    /**
     * Handle to the native media player.
     */
    private long refMedia;

    //**************************************************************************
    //***** Constructors
    //**************************************************************************

    /**
     * Constructor.
     * @param refNativePlayer A reference to the native player.
     */
    GSTAudioEqualizer(long refMedia) {
        if (refMedia == 0) {
            throw new IllegalArgumentException("Invalid native media reference");
        }

        this.refMedia = refMedia;
    }

    //**************************************************************************
    //***** Public functions
    //**************************************************************************

    public boolean getEnabled() {
        return gstGetEnabled(refMedia);
    }

    public void setEnabled(boolean enable) {
        gstSetEnabled(refMedia, enable);
    }

    public EqualizerBand addBand(double centerFrequency, double bandwidth, double gain) {
        return (gstGetNumBands(refMedia) >= MAX_NUM_BANDS &&
                gain >= EqualizerBand.MIN_GAIN && gain <= EqualizerBand.MAX_GAIN) ?
                null : gstAddBand(refMedia, centerFrequency, bandwidth, gain);
    }

    public boolean removeBand(double centerFrequency) {
        return (centerFrequency > 0) ? gstRemoveBand(refMedia, centerFrequency) : false;
    }

    //**************************************************************************
    //***** JNI functions
    //**************************************************************************
    private native boolean gstGetEnabled(long refMedia);
    private native void gstSetEnabled(long refMedia, boolean enable);
    private native int gstGetNumBands(long refMedia);
    private native EqualizerBand gstAddBand(long refMedia,
                                               double centerFrequency, double bandwidth,
                                               double gain);
    private native boolean gstRemoveBand(long refMedia,
                                            double centerFrequency);
}
