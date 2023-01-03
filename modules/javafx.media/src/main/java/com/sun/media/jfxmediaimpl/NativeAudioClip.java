/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import com.sun.media.jfxmedia.AudioClip;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.locator.Locator;
import java.net.URISyntaxException;

final class NativeAudioClip extends AudioClip {
    private final Locator mediaSource;
    private long nativeHandle = 0;

    private static NativeAudioClipDisposer clipDisposer = new NativeAudioClipDisposer();

    private static native boolean nacInit();
    private static native long nacLoad(Locator source);
    private static native long nacCreate(byte[] data, int dataOffset, int sampleCount, int sampleFormat, int channels, int sampleRate);
    private static native void nacUnload(long handle);
    private static native void nacStopAll();

    public static synchronized boolean init() {
        return nacInit();
    }

    public static AudioClip load(URI source) {
        NativeAudioClip newClip = null;
        try {
            Locator locator = new Locator(source);
            locator.init();
            newClip = new NativeAudioClip(locator);
        } catch (URISyntaxException ex) {
            throw new MediaException("Non-compliant URI", ex);
        } catch (IOException ex) {
            throw new MediaException("Cannot connect to media", ex);
        }
        if (null != newClip && 0 != newClip.getNativeHandle()) {
            MediaDisposer.addResourceDisposer(newClip, newClip.getNativeHandle(), clipDisposer);
        } else {
            newClip = null;
            throw new MediaException("Cannot create audio clip");
        }
        return newClip;
    }

    public static AudioClip create(byte[] data, int dataOffset, int sampleCount, int sampleFormat, int channels, int sampleRate) {
        NativeAudioClip newClip = new NativeAudioClip(data, dataOffset, sampleCount, sampleFormat, channels, sampleRate);
        if (null != newClip && 0 != newClip.getNativeHandle()) {
            MediaDisposer.addResourceDisposer(newClip, newClip.getNativeHandle(), clipDisposer);
        } else {
            newClip = null;
            throw new MediaException("Cannot create audio clip");
        }
        return newClip;
    }


    private native NativeAudioClip nacCreateSegment(long handle, double startTime, double stopTime);
    private native NativeAudioClip nacCreateSegment(long handle, int startSample, int endSample);
    private native NativeAudioClip nacResample(long handle, int startSample, int endSample, int sampleRate);
    private native NativeAudioClip nacAppend(long handle, long otherClip);
    private native boolean nacIsPlaying(long handle);
    private native void nacPlay(long handle, double volume, double balance, double pan, double rate, int loopCount, int priority);
    private native void nacStop(long handle);


    private NativeAudioClip(Locator source) {
        mediaSource = source;
        nativeHandle = nacLoad(mediaSource);
    }

    private NativeAudioClip(byte[] data, int dataOffset, int sampleCount, int sampleFormat, int channels, int sampleRate) {
        mediaSource = null;
        nativeHandle = nacCreate(data, dataOffset, sampleCount, sampleFormat, channels, sampleRate);
    }

    long getNativeHandle() {
        return nativeHandle;
    }

    @Override
    public AudioClip createSegment(double startTime, double stopTime) {
        return nacCreateSegment(nativeHandle, startTime, stopTime);
    }

    @Override
    public AudioClip createSegment(int startSample, int endSample) {
        return nacCreateSegment(nativeHandle, startSample, endSample);
    }

    @Override
    public AudioClip resample(int startSample, int endSample, int newSampleRate) {
        return nacResample(nativeHandle, startSample, endSample, newSampleRate);
    }

    @Override
    public AudioClip append(AudioClip clip) {
        // assert clip is NativeAudioClip or bail
        if (!(clip instanceof NativeAudioClip)) {
            throw new IllegalArgumentException("AudioClip type mismatch, cannot append");
        }
        return nacAppend(nativeHandle, ((NativeAudioClip)clip).getNativeHandle());
    }

    @Override
    public AudioClip flatten() {
        // no effect for NativeAudioClip
        return this;
    }

    @Override
    public boolean isPlaying() {
        return nacIsPlaying(nativeHandle);
    }

    @Override
    public void play() {
        nacPlay(nativeHandle, clipVolume, clipBalance, clipPan, clipRate, loopCount, clipPriority);
    }

    @Override
    public void play(double volume) {
        nacPlay(nativeHandle, volume, clipBalance, clipPan, clipRate, loopCount, clipPriority);
    }

    @Override
    public void play(double volume, double balance, double rate, double pan, int loopCount, int priority) {
        nacPlay(nativeHandle, volume, balance, pan, rate, loopCount, priority);
    }

    @Override
    public void stop() {
        nacStop(nativeHandle);
    }

    public static void stopAllClips() {
        nacStopAll();
    }

    private static class NativeAudioClipDisposer implements MediaDisposer.ResourceDisposer {
        @Override
        public void disposeResource(Object resource) {
            // resource is a Long
            long nativeHandle = ((Long)resource).longValue();
            if (0 != nativeHandle) {
                nacUnload(nativeHandle);
            }
        }
    }
}
