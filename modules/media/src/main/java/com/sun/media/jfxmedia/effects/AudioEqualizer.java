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
 * Provides a master audio equalizer with up to 15 bands.  Each band can have the center frequency,
 * bandwidth, and gain set.
 */
public interface AudioEqualizer
{
    /**
     * The maximum number of bands the equalizer can control.
     */
    public static final int MAX_NUM_BANDS = 64;

    /**
     * Returns whether equalization was enabled or not.
     *
     * @return boolean value
     */
    public boolean getEnabled();

    /**
     * Turns on or off audio equalization.
     *
     * @param bEnable boolean value
     */
    public void setEnabled(boolean bEnable);

    /**
     * Adds a band to the equalizer.
     *
     * @param centerFrequency
     * @param bandwidth
     * @param gain
     * @return instance of EqualizerBand if the band was added, null if a band with the
     * <code>centerFrequency</code> already exists.
     * @throws IllegalArgumentException if <code>centerFrequency</code> or <code>bandwidth</code> are < 0.0.
     */
    public EqualizerBand addBand(double centerFrequency, double bandwidth, double gain);

    /**
     * Removes an equalizer band with the specified center frequency.
     *
     * @param centerFrequency
     * @return true if the band was found and removed.  false otherwise
     * @throws IllegalArgumentException if <code>centerFrequency</code> is
     * negative.
     */
    public boolean removeBand(double centerFrequency);
}
