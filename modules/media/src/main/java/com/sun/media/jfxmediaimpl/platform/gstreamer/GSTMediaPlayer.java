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

import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.NativeMediaPlayer;

/**
 * GStreamer implementation of a MediaPlayer.
 */
final class GSTMediaPlayer extends NativeMediaPlayer {
    private GSTMedia gstMedia = null;
    private float mutedVolume = 1.0f;  // last volume before mute
    private boolean muteEnabled = false;
    private AudioEqualizer audioEqualizer;
    private AudioSpectrum audioSpectrum;

    private GSTMediaPlayer(GSTMedia sourceMedia) {
        super(sourceMedia);
        init();
        gstMedia = sourceMedia;

        int rc = gstInitPlayer(gstMedia.getNativeMediaRef());
        if (0 != rc) {
            dispose();
            throwMediaErrorException(rc, null);
        }

        audioSpectrum = new GSTAudioSpectrum(gstMedia.getNativeMediaRef());
        audioEqualizer = new GSTAudioEqualizer(gstMedia.getNativeMediaRef());
    }

    GSTMediaPlayer(Locator source) {
        this(new GSTMedia(source));
    }

    @Override
    public AudioEqualizer getEqualizer() {
        return audioEqualizer;
    }

    @Override
    public AudioSpectrum getAudioSpectrum() {
        return audioSpectrum;
    }

    // FIXME: this should be pushed down to native instead of returning an int value
    private void throwMediaErrorException(int code, String message)
            throws MediaException
    {
        MediaError me = MediaError.getFromCode(code);
        throw new MediaException(message, null, me);
    }

    protected long playerGetAudioSyncDelay() throws MediaException {
        long[] audioSyncDelay = new long[1];
        int rc = gstGetAudioSyncDelay(gstMedia.getNativeMediaRef(), audioSyncDelay);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        return audioSyncDelay[0];
    }

    protected void playerSetAudioSyncDelay(long delay) throws MediaException {
        int rc = gstSetAudioSyncDelay(gstMedia.getNativeMediaRef(), delay);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected void playerPlay() throws MediaException {
        int rc = gstPlay(gstMedia.getNativeMediaRef());
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected void playerStop() throws MediaException {
        int rc = gstStop(gstMedia.getNativeMediaRef());
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected void playerPause() throws MediaException {
        int rc = gstPause(gstMedia.getNativeMediaRef());
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected void playerFinish() throws MediaException {
        int rc = gstFinish(gstMedia.getNativeMediaRef());
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected float playerGetRate() throws MediaException {
        float[] rate = new float[1];
        int rc = gstGetRate(gstMedia.getNativeMediaRef(), rate);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        return rate[0];
    }

    protected void playerSetRate(float rate) throws MediaException {
        int rc = gstSetRate(gstMedia.getNativeMediaRef(), rate);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected double playerGetPresentationTime() throws MediaException {
        double[] presentationTime = new double[1];
        int rc = gstGetPresentationTime(gstMedia.getNativeMediaRef(), presentationTime);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        return presentationTime[0];
    }

    protected boolean playerGetMute() throws MediaException {
        return muteEnabled;
    }

    protected synchronized void playerSetMute(boolean enable) throws MediaException {
        if (enable != muteEnabled) {
            if (enable) {
                // Cache the current volume.
                float currentVolume = getVolume();

                // Set the volume to zero.
                playerSetVolume(0);

                // Set the mute flag. It is necessary to do this after
                // calling setVolume() as otherwise the volume will not
                // be set to zero.
                muteEnabled = true;

                // Save the pre-mute volume.
                mutedVolume = currentVolume;
            }
            else {
                // Unset the mute flag. It is necessary to do this before
                // calling setVolume() as otherwise the volume will not
                // be set to the cached value.
                muteEnabled = false;

                // Set the volume to the cached value.
                playerSetVolume(mutedVolume);
            }
        }
    }

    protected float playerGetVolume() throws MediaException {
        synchronized(this) {
            if (muteEnabled)
                return mutedVolume;
        }
        float[] volume = new float[1];
        int rc = gstGetVolume(gstMedia.getNativeMediaRef(), volume);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        return volume[0];
    }

    protected synchronized void playerSetVolume(float volume) throws MediaException {
        if (!muteEnabled) {
            int rc = gstSetVolume(gstMedia.getNativeMediaRef(), volume);
            if (0 != rc) {
                throwMediaErrorException(rc, null);
            } else {
                mutedVolume = volume;
            }
        } else {
            mutedVolume = volume;
        }
    }

    protected float playerGetBalance() throws MediaException {
        float[] balance = new float[1];
        int rc = gstGetBalance(gstMedia.getNativeMediaRef(), balance);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        return balance[0];
    }

    protected void playerSetBalance(float balance) throws MediaException {
        int rc = gstSetBalance(gstMedia.getNativeMediaRef(), balance);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected double playerGetDuration() throws MediaException {
        double[] duration = new double[1];
        int rc = gstGetDuration(gstMedia.getNativeMediaRef(), duration);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
        if (duration[0] == -1.0) {
            return Double.POSITIVE_INFINITY;
        } else {
            return duration[0];
        }
    }

    protected void playerSeek(double streamTime) throws MediaException {
        int rc = gstSeek(gstMedia.getNativeMediaRef(), streamTime);
        if (0 != rc) {
            throwMediaErrorException(rc, null);
        }
    }

    protected void playerInit() throws MediaException {
    }

    protected void playerDispose() {
        audioEqualizer = null;
        audioSpectrum = null;
        gstMedia = null;
    }

    // Native methods
    private native int gstInitPlayer(long refNativeMedia);
    private native int gstGetAudioSyncDelay(long refNativeMedia, long[] syncDelay);
    private native int gstSetAudioSyncDelay(long refNativeMedia, long delay);
    private native int gstPlay(long refNativeMedia);
    private native int gstPause(long refNativeMedia);
    private native int gstStop(long refNativeMedia);
    private native int gstFinish(long refNativeMedia);
    private native int gstGetRate(long refNativeMedia, float[] rate);
    private native int gstSetRate(long refNativeMedia, float rate);
    private native int gstGetPresentationTime(long refNativeMedia, double[] time);
    private native int gstGetVolume(long refNativeMedia, float[] volume);
    private native int gstSetVolume(long refNativeMedia, float volume);
    private native int gstGetBalance(long refNativeMedia, float[] balance);
    private native int gstSetBalance(long refNativeMedia, float balance);
    private native int gstGetDuration(long refNativeMedia, double[] duration);
    private native int gstSeek(long refNativeMedia, double streamTime);
}
