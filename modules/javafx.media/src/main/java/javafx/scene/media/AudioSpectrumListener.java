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

package javafx.scene.media;

/**
 * Audio spectrum callback interface. The observer receives periodic updates of
 * the audio spectrum. A single observer may be attached to a given {@link MediaPlayer}
 * using {@link MediaPlayer#setAudioSpectrumListener}.
 *
 * @see MediaPlayer
 * @since JavaFX 2.0
 */
public interface AudioSpectrumListener {
    /**
     * Notifies the observer about new data available for the audio spectrum.
     *
     * <p>The notification period for updates may be set using
     * {@link MediaPlayer#setAudioSpectrumInterval}, and the number of bands and
     * sensitivity threshold via {@link MediaPlayer#setAudioSpectrumNumBands} and
     * {@link MediaPlayer#setAudioSpectrumThreshold}, respectively. The default
     * values of the settings are an interval of <code>0.1</code> seconds,
     * <code>128</code> bands, and sensitivity threshold <code>-60.0</code> dB.
     *
     * <p>The arrays
     * passed to the listener should be considered to be read-only as the
     * underlying implementation may re-use them.</p>
     *
     * @param timestamp timestamp of the event in seconds.
     * @param duration duration for which the spectrum was computed in seconds.
     * @param magnitudes array containing the non-positive spectrum magnitude in decibels
     * (dB) for each band.
     * The size of the array equals the number of bands and should be considered
     * to be read-only.
     * @param phases array containing the phase in the range
     * [<code>Math.PI</code>,&nbsp;<code>Math.PI</code>] for each band.
     * The size of the array equals the number of bands and should be considered
     * to be read-only.
     */
    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases);
}
