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

package com.sun.media.jfxmediaimpl;

import java.net.URI;
import com.sun.media.jfxmedia.AudioClip;
import com.sun.media.jfxmedia.logging.Logger;
import java.net.URISyntaxException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Singleton class that manages AudioClip implementations.
 */
public class AudioClipProvider {
    private static AudioClipProvider primaDonna;
    private boolean useNative;

    public static synchronized AudioClipProvider getProvider() {
        if (null == primaDonna) {
            primaDonna = new AudioClipProvider();
        }
        return primaDonna;
    }

    private AudioClipProvider() {
        // Attempt to init the native audio clip stack
        // if that fails, fall back on the NativeMediaAudioClip impl
        useNative = false;
        try {
            useNative = NativeAudioClip.init();
        } catch (UnsatisfiedLinkError ule) {
            Logger.logMsg(Logger.DEBUG, "JavaFX AudioClip native methods not linked, using NativeMedia implementation");
        } catch (Exception t) {
            Logger.logMsg(Logger.ERROR, "Exception while loading native AudioClip library: "+t);
        }
    }

    public AudioClip load(URI source) throws URISyntaxException, FileNotFoundException, IOException {
        if (useNative) {
            return NativeAudioClip.load(source);
        }
        return NativeMediaAudioClip.load(source);
    }

    public AudioClip create(byte[] data, int dataOffset, int sampleCount, int sampleFormat, int channels, int sampleRate)
            throws IllegalArgumentException
    {
        if (useNative) {
            return NativeAudioClip.create(data, dataOffset, sampleCount, sampleFormat, channels, sampleRate);
        }
        return NativeMediaAudioClip.create(data, dataOffset, sampleCount, sampleFormat, channels, sampleRate);
    }

    public void stopAllClips() {
        if (useNative) {
            NativeAudioClip.stopAllClips();
        }
        NativeMediaAudioClip.stopAllClips();
    }
}
