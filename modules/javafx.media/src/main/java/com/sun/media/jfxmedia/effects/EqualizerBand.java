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

package com.sun.media.jfxmedia.effects;

/**
 * EqualizerBand represents a single band in AudioEqualizer.  EqualizerBand can specify the center
 * frequency, bandwidth, and gain on the band.
 *
 * @see AudioEqualizer
 */
public interface EqualizerBand
{
    /**
     * Minimum possible gain.
     */
    public static final double MIN_GAIN = -24.0;

    /**
     * Maximum possible gain.
     */
    public static final double MAX_GAIN = 12.0;

    /**
     * Gets the center frequency of this band.  Half of the bandwidth is to the left of this frequncy
     * and half is to the right.
     *
     * @return float value
     */
    public double getCenterFrequency();

    /**
     * Sets the center frequency of this band.  Half of the bandwidth is to the left of this frequncy
     * and half is to the right.
     *
     * @param centerFrequency float value
     */
    public void setCenterFrequency(double centerFrequency);

    /**
     * Gets the bandwith (of frequencies) of this band.
     *
     * @return float value
     */
    public double getBandwidth();

    /**
     * Sets the bandwidth of this band.
     *
     * @param bandwidth float value
     */
    public void setBandwidth(double bandwidth);

    /**
     * Gets the gain value for this bandwidth.  Gains are in decibels.
     *
     * @return float value for dB
     */
    public double getGain();

    /**
     * Sets the gain value for this bandwidth.  Gains are in decibels.
     *
     * @param gain float value in dB
     * @throws IllegalArgumentException if <code>gain</code> is outside of the
     * platform possible range. For example GStreamer based equalizer gain values
     * must be in [-24.0; 12.0] interval.
     */
    public void setGain(double gain);
}
