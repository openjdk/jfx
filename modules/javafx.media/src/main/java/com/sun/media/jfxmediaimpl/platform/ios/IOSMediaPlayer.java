/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform.ios;

import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.effects.EqualizerBand;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.control.MediaPlayerOverlay;
import com.sun.media.jfxmediaimpl.NativeMediaPlayer;

import java.util.Map;
import java.util.HashMap;

/**
 * iOS MediaPlayer implementation.
 */
public final class IOSMediaPlayer extends NativeMediaPlayer {

    private IOSMedia iosMedia;

    private final NullAudioEQ audioEqualizer;
    private final NullAudioSpectrum audioSpectrum;
    private final MediaPlayerOverlay mediaPlayerOverlay;

    private float mutedVolume = 1.0f;  // last volume before mute
    private boolean muteEnabled; // false by default

    private IOSMediaPlayer(final IOSMedia sourceMedia) {
        super(sourceMedia);
        iosMedia = sourceMedia;

        // run event loop
        init();

        handleError(iosInitPlayer(iosMedia.getNativeMediaRef()));

        audioEqualizer = new NullAudioEQ();
        audioSpectrum = new NullAudioSpectrum();
        mediaPlayerOverlay = new MediaPlayerOverlayImpl();
    }

    IOSMediaPlayer(final Locator source) {
        this(new IOSMedia(source));
    }

    @Override
    public AudioEqualizer getEqualizer() {
        return audioEqualizer;
    }

    @Override
    public AudioSpectrum getAudioSpectrum() {
        return audioSpectrum;
    }

    @Override
    public MediaPlayerOverlay getMediaPlayerOverlay() {
        return mediaPlayerOverlay;
    }

    private void handleError(final int err) throws MediaException {
        if (0 != err) {
            final MediaError me = MediaError.getFromCode(err);
            throw new MediaException("Media error occurred", null, me);
        }
    }

    @Override
    protected long playerGetAudioSyncDelay() throws MediaException {
        final long[] audioSyncDelay = new long[1];
        handleError(iosGetAudioSyncDelay(iosMedia.getNativeMediaRef(), audioSyncDelay));
        return audioSyncDelay[0];
    }

    @Override
    protected void playerSetAudioSyncDelay(final long delay) throws MediaException {
        handleError(iosSetAudioSyncDelay(iosMedia.getNativeMediaRef(), delay));
    }

    @Override
    protected void playerPlay() throws MediaException {
        handleError(iosPlay(iosMedia.getNativeMediaRef()));
    }

    @Override
    protected void playerStop() throws MediaException {
        handleError(iosStop(iosMedia.getNativeMediaRef()));
    }

    @Override
    protected void playerPause() throws MediaException {
        handleError(iosPause(iosMedia.getNativeMediaRef()));
    }

    @Override
    protected float playerGetRate() throws MediaException {
        final float[] rate = new float[1];
        handleError(iosGetRate(iosMedia.getNativeMediaRef(), rate));
        return rate[0];
    }

    @Override
    protected void playerSetRate(final float rate) throws MediaException {
        handleError(iosSetRate(iosMedia.getNativeMediaRef(), rate));
    }

    @Override
    protected double playerGetPresentationTime() throws MediaException {
        double[] presentationTime = new double[1];
        handleError(iosGetPresentationTime(iosMedia.getNativeMediaRef(), presentationTime));
        return presentationTime[0];
    }

    @Override
    protected boolean playerGetMute() throws MediaException {
        return muteEnabled;
    }

    @Override
    protected synchronized void playerSetMute(final boolean enable) throws MediaException {
        if (enable != muteEnabled) {
            if (enable) {
                final float currentVolume = getVolume();
                playerSetVolume(0);
                muteEnabled = true;
                mutedVolume = currentVolume;
            }
            else {
                muteEnabled = false;
                playerSetVolume(mutedVolume);
            }
        }
    }

    @Override
    protected float playerGetVolume() throws MediaException {
        synchronized(this) {
            if (muteEnabled) {
                return mutedVolume;
            }
        }
        final float[] volume = new float[1];
        handleError(iosGetVolume(iosMedia.getNativeMediaRef(), volume));
        return volume[0];
    }

    @Override
    protected synchronized void playerSetVolume(final float volume) throws MediaException {
        if (!muteEnabled) {
            final int err = iosSetVolume(iosMedia.getNativeMediaRef(), volume);
            if (0 != err) {
                handleError(err);
            } else {
                mutedVolume = volume;
            }
        } else {
            mutedVolume = volume;
        }
    }

    @Override
    protected float playerGetBalance() throws MediaException {
        final float[] balance = new float[1];
        handleError(iosGetBalance(iosMedia.getNativeMediaRef(), balance));
        return balance[0];
    }

    @Override
    protected void playerSetBalance(final float balance) throws MediaException {
        handleError(iosSetBalance(iosMedia.getNativeMediaRef(), balance));
    }

    @Override
    protected double playerGetDuration() throws MediaException {
        final double[] durationArr = new double[1];
        handleError(iosGetDuration(iosMedia.getNativeMediaRef(), durationArr));
        double duration;
        if (durationArr[0] == -1.0) {
            duration = Double.POSITIVE_INFINITY;
        } else {
            duration = durationArr[0];
        }
        return duration;
    }

    @Override
    protected void playerSeek(final double streamTime) throws MediaException {
        handleError(iosSeek(iosMedia.getNativeMediaRef(), streamTime));
    }

    @Override
    protected void playerInit() throws MediaException {
    }

    @Override
    protected void playerFinish() throws MediaException {
        handleError(iosFinish(iosMedia.getNativeMediaRef()));
    }

    @Override
    protected void playerDispose() {
        iosDispose(iosMedia.getNativeMediaRef());
        iosMedia = null;
    }

    // Native methods
    private native int iosInitPlayer(long refNativeMedia);
    private native int iosGetAudioSyncDelay(long refNativeMedia, long[] syncDelay);
    private native int iosSetAudioSyncDelay(long refNativeMedia, long delay);
    private native int iosPlay(long refNativeMedia);
    private native int iosPause(long refNativeMedia);
    private native int iosStop(long refNativeMedia);
    private native int iosGetRate(long refNativeMedia, float[] rate);
    private native int iosSetRate(long refNativeMedia, float rate);
    private native int iosGetPresentationTime(long refNativeMedia, double[] time);
    private native int iosGetVolume(long refNativeMedia, float[] volume);
    private native int iosSetVolume(long refNativeMedia, float volume);
    private native int iosGetBalance(long refNativeMedia, float[] balance);
    private native int iosSetBalance(long refNativeMedia, float balance);
    private native int iosGetDuration(long refNativeMedia, double[] duration);
    private native int iosSeek(long refNativeMedia, double streamTime);
    private native void iosDispose(long refNativeMedia);
    private native int iosFinish(long refNativeMedia);

    // Overlay native methods
    private native int iosSetOverlayX(long mediaRef, double x);
    private native int iosSetOverlayY(long mediaRef, double y);
    private native int iosSetOverlayVisible(long mediaRef, boolean visible);
    private native int iosSetOverlayWidth(long mediaRef, double width);
    private native int iosSetOverlayHeight(long mediaRef, double height);
    private native int iosSetOverlayPreserveRatio(long mediaRef, boolean preserveRatio);
    private native int iosSetOverlayOpacity(long mediaRef, double opacity);
    private native int iosSetOverlayTransform(long mediaRef,
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt);

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
            Double key = centerFrequency;
            if (bands.containsKey(key)) {
                removeBand(centerFrequency);
            }

            EqualizerBand newBand = new NullEQBand(centerFrequency, bandwidth, gain);
            bands.put(key, newBand);
            return newBand;
        }

        public boolean removeBand(double centerFrequency) {
            Double key = centerFrequency;
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

    private final class MediaPlayerOverlayImpl implements MediaPlayerOverlay {

        @Override
        public void setOverlayX(final double x) {
            handleError(iosSetOverlayX(iosMedia.getNativeMediaRef(), x));
        }

        @Override
        public void setOverlayY(final double y) {
            handleError(iosSetOverlayY(iosMedia.getNativeMediaRef(), y));
        }

        @Override
        public void setOverlayVisible(final boolean visible) {
            handleError(iosSetOverlayVisible(iosMedia.getNativeMediaRef(), visible));
        }

        @Override
        public void setOverlayWidth(final double width) {
            handleError(iosSetOverlayWidth(iosMedia.getNativeMediaRef(), width));
        }

        @Override
        public void setOverlayHeight(final double height) {
            handleError(iosSetOverlayHeight(iosMedia.getNativeMediaRef(), height));
        }

        @Override
        public void setOverlayPreserveRatio(final boolean preserveRatio) {
            handleError(iosSetOverlayPreserveRatio(iosMedia.getNativeMediaRef(), preserveRatio));
        }

        @Override
        public void setOverlayOpacity(final double opacity) {
            handleError(iosSetOverlayOpacity(iosMedia.getNativeMediaRef(), opacity));
        }

        @Override
        public void setOverlayTransform(
                final double mxx, final double mxy, final double mxz, final double mxt,
                final double myx, final double myy, final double myz, final double myt,
                final double mzx, final double mzy, final double mzz, final double mzt) {
            handleError(iosSetOverlayTransform(
                    iosMedia.getNativeMediaRef(),
                    mxx, mxy, mxz, mxt,
                    myx, myy, myz, myt,
                    mzx, mzy, mzz, mzt));
        }
    }
}
