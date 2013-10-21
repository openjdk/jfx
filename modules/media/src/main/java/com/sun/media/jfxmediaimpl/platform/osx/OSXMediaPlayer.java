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

package com.sun.media.jfxmediaimpl.platform.osx;

import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.effects.EqualizerBand;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.NativeMedia;
import com.sun.media.jfxmediaimpl.NativeMediaPlayer;
import java.util.HashMap;
import java.util.Map;

/**
 * Mac OS X MediaPlayer implementation.
 */
final class OSXMediaPlayer extends NativeMediaPlayer {
    private NullAudioEQ audioEq;
    private NullAudioSpectrum audioSpectrum;
    private final Locator mediaLocator;

    OSXMediaPlayer(NativeMedia sourceMedia) {
        super(sourceMedia);
        init();
        mediaLocator = sourceMedia.getLocator();
        audioEq = new NullAudioEQ();
        audioSpectrum = new NullAudioSpectrum();
    }

    OSXMediaPlayer(Locator source) {
        this(new OSXMedia(source));
    }

    @Override
    public AudioEqualizer getEqualizer() {
        return audioEq;
    }

    @Override
    public AudioSpectrum getAudioSpectrum() {
        return audioSpectrum;
    }

    void initializePlayer() {
        osxCreatePlayer(mediaLocator.getStringLocation());
    }

    @Override
    protected long playerGetAudioSyncDelay() throws MediaException {
        return osxGetAudioSyncDelay();
    }

    @Override
    protected void playerSetAudioSyncDelay(long delay) throws MediaException {
        osxSetAudioSyncDelay(delay);
    }

    @Override
    protected void playerPlay() throws MediaException {
        osxPlay();
    }

    @Override
    protected void playerStop() throws MediaException {
        osxStop();
    }

    @Override
    protected void playerPause() throws MediaException {
        osxPause();
    }

    @Override
    protected void playerFinish() throws MediaException {
        osxFinish();
    }

    @Override
    protected float playerGetRate() throws MediaException {
        return osxGetRate();
    }

    @Override
    protected void playerSetRate(float rate) throws MediaException {
        osxSetRate(rate);
    }

    @Override
    protected double playerGetPresentationTime() throws MediaException {
        return osxGetPresentationTime();
    }

    @Override
    protected boolean playerGetMute() throws MediaException {
        return osxGetMute();
    }

    @Override
    protected void playerSetMute(boolean state) throws MediaException {
        osxSetMute(state);
    }

    @Override
    protected float playerGetVolume() throws MediaException {
        return osxGetVolume();
    }

    @Override
    protected void playerSetVolume(float volume) throws MediaException {
        osxSetVolume(volume);
    }

    @Override
    protected float playerGetBalance() throws MediaException {
        return osxGetBalance();
    }

    @Override
    protected void playerSetBalance(float balance) throws MediaException {
        osxSetBalance(balance);
    }

    @Override
    protected double playerGetDuration() throws MediaException {
        double duration = osxGetDuration();
        if (duration == -1.0) {
            return Double.POSITIVE_INFINITY;
        }

        return duration;
    }

    @Override
    protected void playerSeek(double streamTime) throws MediaException {
        osxSeek(streamTime);
    }

    @Override
    protected void playerDispose() {
        osxDispose();
    }

    private native void osxCreatePlayer(String sourceURI) throws MediaException;
    private native long osxGetAudioSyncDelay() throws MediaException;
    private native void osxSetAudioSyncDelay(long delay) throws MediaException;
    private native void osxPlay() throws MediaException;
    private native void osxStop() throws MediaException;
    private native void osxPause() throws MediaException;
    private native void osxFinish() throws MediaException;
    private native float osxGetRate() throws MediaException;
    private native void osxSetRate(float rate) throws MediaException;
    private native double osxGetPresentationTime() throws MediaException;
    private native boolean osxGetMute() throws MediaException;
    private native void osxSetMute(boolean state) throws MediaException;
    private native float osxGetVolume() throws MediaException;
    private native void osxSetVolume(float volume) throws MediaException;
    private native float osxGetBalance() throws MediaException;
    private native void osxSetBalance(float balance) throws MediaException;
    private native double osxGetDuration() throws MediaException;
    private native void osxSeek(double streamTime) throws MediaException;
    private native void osxDispose();

    @Override
    public void playerInit() throws MediaException {
    }

    private static final class NullAudioEQ implements AudioEqualizer {
        private boolean enabled = false;
        private Map<Double, EqualizerBand> bands
                = new HashMap<Double,EqualizerBand>();

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean bEnable) {
            enabled = bEnable;
        }

        public EqualizerBand addBand(double centerFrequency, double bandwidth, double gain) {
            Double key = new Double(centerFrequency);
            if (bands.containsKey(key)) {
                removeBand(centerFrequency);
            }

            EqualizerBand newBand = new NullEQBand(centerFrequency, bandwidth, gain);
            bands.put(key, newBand);
            return newBand;
        }

        public boolean removeBand(double centerFrequency) {
            Double key = new Double(centerFrequency);
            if (bands.containsKey(key)) {
                bands.remove(key);
                return true;
            }
            return false;
        }
    }

    private static final class NullAudioSpectrum implements AudioSpectrum {
        private boolean enabled = false;
        private int bandCount = 128;
        private double interval = 0.1;
        private int threshold = 60;
        private float[] fakeData;

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBandCount() {
            return bandCount;
        }

        public void setBandCount(int bands) {
            bandCount = bands;
            fakeData = new float[bandCount];
        }

        public double getInterval() {
            return interval;
        }

        public void setInterval(double interval) {
            this.interval = interval;
        }

        public int getSensitivityThreshold() {
            return threshold;
        }

        public void setSensitivityThreshold(int threshold) {
            this.threshold = threshold;
        }

        public float[] getMagnitudes(float[] mag) {
            int size = fakeData.length;
            if (mag == null || mag.length < size) {
                mag = new float[size];
            }
            System.arraycopy(fakeData, 0, mag, 0, size);
            return mag;
        }

        public float[] getPhases(float[] phs) {
            int size = fakeData.length;
            if (phs == null || phs.length < size) {
                phs = new float[size];
            }
            System.arraycopy(fakeData, 0, phs, 0, size);
            return phs;
        }
    }

    private static final class NullEQBand implements EqualizerBand {
        private double center;
        private double bandwidth;
        private double gain;

        NullEQBand(double center, double bandwidth, double gain) {
            this.center = center;
            this.bandwidth = bandwidth;
            this.gain = gain;
        }

        public double getCenterFrequency() {
            return center;
        }

        public void setCenterFrequency(double centerFrequency) {
            center = centerFrequency;
        }

        public double getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(double bandwidth) {
            this.bandwidth = bandwidth;
        }

        public double getGain() {
            return gain;
        }

        public void setGain(double gain) {
            this.gain = gain;
        }
    }
}
