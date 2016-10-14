/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmedia.track;

import java.lang.annotation.Native;
import java.util.Locale;
import com.sun.media.jfxmedia.Media;

/**
 * A class representing an audio track in a media.
 *
 * @see Media
 */
public class AudioTrack extends Track {

    /**
     * Channel mask bit indicating the absence of any channel
     */
    @Native public static final int UNKNOWN         = 0;

    /**
     * Channel mask bit indicating the presence of a front left channel.
     */
    @Native public static final int FRONT_LEFT      = 0x01;

    /**
     * Channel mask bit indicating the presence of a front right channel.
     */
    @Native public static final int FRONT_RIGHT     = 0x02;

    /**
     * Channel mask bit indicating the presence of a front center channel.
     */
    @Native public static final int FRONT_CENTER    = 0x04;

    /**
     * Channel mask bit indicating the presence of a rear left channel.
     */
    @Native public static final int REAR_LEFT       = 0x08;

    /**
     * Channel mask bit indicating the presence of a rear right channel.
     */
    @Native public static final int REAR_RIGHT      = 0x10;

    /**
     * Channel mask bit indicating the presence of a rear center channel.
     */
    @Native public static final int REAR_CENTER     = 0x20;

    private int    numChannels;
    private int    channelMask;
    private float  encodedSampleRate;

    /**
     * Constructor.
     *
     * @param enabled Whether this track is enabled by default or not (if the container supports it)
     * @param trackID A unique identifier for this track
     * @param name The name of the track.
     * @param encoding The encoding of the track.
     * @param locale The language information for the track.
     * @param numChannels The number of audio channels in the track.
     * @param channelMask The channel mask of the track.
     * @param encodedSampleRate The encoded sample rate in samples per second.
     * @throws IllegalArgumentException if <code>name</code> or
     * <code>encoding</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>numChannels&nbsp;&lt;&nbsp;1</code>.
     * @throws IllegalArgumentException if <code>encodedSampleRate&nbsp;&le;&nbsp;0.0</code>.
     */
    public AudioTrack(boolean enabled, long trackID, String name, Locale locale, Track.Encoding encoding,
            int numChannels, int channelMask, float encodedSampleRate)
    {
        super(enabled, trackID, name, locale, encoding);

        if (numChannels < 1) {
            throw new IllegalArgumentException("numChannels < 1!");
        }

        if (encodedSampleRate <= 0.0F) {
            throw new IllegalArgumentException("encodedSampleRate <= 0.0");
        }

        this.numChannels = numChannels;
        this.channelMask = channelMask;
        this.encodedSampleRate = encodedSampleRate;
    }

    /**
     * Retrieve the number of audio channels in the track.
     *
     * @return The number of channels.
     */
    public int getNumChannels() {
        return this.numChannels;
    }

    /**
     * Retrieve the channel mask of the track.
     *
     * @return The channel mask.
     */
    public int getChannelMask() {
        return this.channelMask;
    }

    /**
     * Retrieve the encoded sample rate.
     *
     * @return The sample rate.
     */
    public float getEncodedSampleRate() {
        return this.encodedSampleRate;
    }

    @Override
    public final String toString() {
        return "AudioTrack {\n"+
                "    name: "+this.getName()+"\n"+
                "    encoding: "+this.getEncodingType()+"\n"+
                "    language: "+getLocale()+"\n"+
                "    numChannels: "+this.numChannels+"\n"+
                "    channelMask: "+this.channelMask+"\n"+
                "    encodedSampleRate: "+this.encodedSampleRate+"\n"+
                "}";
    }
}
