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

package com.sun.media.jfxmedia.track;

import java.util.Locale;

/**
 * Abstract base class for all types of media tracks.
 */
public abstract class Track
{
    /** Enumeration of supported encoding types. */
    public enum Encoding
    {
        NONE,

        // Audio encodings
        PCM,                // Uncompressed PCM
        MPEG1AUDIO,         // MPEG1 Audio (layer1,2)
        MPEG1LAYER3,        // MPEG1 Layer3 (mp3)
        AAC,                // Advanced Audio Coding

        // Video encodings
        H264,               // H.264 ("ISO/IEC 14496-10" standard
                            // or "ITU-T Recommendation H.264")
                            // (aka MPEG-4 part 10 video, also known as AVC)
        H265,

        // custom encoding
        CUSTOM;

        public static Encoding toEncoding(int ordinal) {
            for(Encoding value : Encoding.values()){
                if (value.ordinal() == ordinal) {
                    return value;
                }
            }
            return NONE;
        }
    }

    private boolean trackEnabled;
    private long trackID;
    private String name;
    private Locale locale;
    private Encoding encoding;

    /**
     * Constructor.
     *
     * @param enabled Whether this track is enabled by default or not (if the container supports it)
     * @param trackID A unique identifier for this track.
     * @param name The name of the track.
     * @param locale The language information for this track.
     * @param encoding The encoding of the track.
     * @throws IllegalArgumentException if <code>name</code> or
     * <code>encoding</code> is <code>null</code>.
     */
    protected Track(boolean enabled, long trackID, String name, Locale locale, Encoding encoding)
    {
        if (name == null) {
            throw new IllegalArgumentException("name == null!");
        } else if (encoding == null) {
            throw new IllegalArgumentException("encoding == null!");
        }

        trackEnabled = enabled;
        this.trackID = trackID;
        this.locale = locale;
        this.encoding = encoding;
        this.name = name;
    }

    /**
     * Gets the media encoding for this track.
     *
     * @return The media encoding for this track.
     */
    public Encoding getEncodingType()
    {
       return this.encoding;
    }

    /**
     * Returns the name of this track
     *
     * @return The name of this track
     */
    public String getName()
    {
        return this.name;
    }

    public Locale getLocale() {
        return locale;
    }

    public long getTrackID() {
        return trackID;
    }

    /**
     * Whether this track is enabled by default in the source container. If the
     * container type does not support disabling tracks then all tracks will
     * implicitly be enabled.
     * @return whether the track is enabled by default
     */
    public boolean isEnabled() {
        return trackEnabled;
    }
}
