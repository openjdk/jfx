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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * A class representing a track contained in a media resource.
 * A media resource may have multiple parallel tracks, such as a video
 * track with several audio tracks in different languages. The types of tracks
 * supported by the system may be inferred from the existing subclasses of this
 * class. Not all media resources will contain a track of each supported type,
 * and the time span of a given track need not be commensurate with the time
 * span of the containing media.
 * @since JavaFX 2.0
 */
public abstract class Track {
    /**
     * The name of the track or <code>null</code> if the track is unnamed.
     */
    private String name;
    private long trackID;   // opaque, unique track ID used by players to identify it
    private Locale locale;
    private Map<String,Object> metadata;

    /**
     * Retrieves the name of the track.
     * @return the track name or <code>null</code>.
     */
    public final String getName() {
        return name;
    }

    /**
     * The {@link Locale} specifying the language and possibly the country that
     * the <code>Track</code> contents are formatted for. For {@link AudioTrack}s
     * this will be the language spoken, for {@link SubtitleTrack}s this will be
     * the language presented in the captions. Not all <code>Track</code>s will
     * have an associated language, in which case this method will return null.
     *
     * @return the <code>Track</code>s language information or null
     * @since JavaFX 8.0
     */
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Get the track ID as defined by the media container format. The ID of each
     * <code>Track</code> must be unique for its source {@link Media}.
     * @return the <code>Track</code>s unique ID
     * @since JavaFX 8.0
     */
    public final long getTrackID() {
        return trackID;
    }

    /**
     * @return a Map containing all known metadata for this <code>Track</code>
     * @since JavaFX 8.0
     */
    public final Map<String,Object> getMetadata() {
        return metadata;
    }

    Track(long trackID, Map<String,Object> metadata) {
        this.trackID = trackID;

        Object value = metadata.get("name");
        if (null != value && value instanceof String) {
            name = (String)value;
        }

        value = metadata.get("locale");
        if (null != value && value instanceof Locale) {
            locale = (Locale)value;
        }

        this.metadata = Collections.unmodifiableMap(metadata);
    }

    private String description;
    @Override
    public final String toString() {
        synchronized(this) {
            if (null == description) {
                StringBuilder sb = new StringBuilder();
                Map<String,Object> md = getMetadata();

                sb.append(this.getClass().getName());
                sb.append("[ track id = ");
                sb.append(trackID);

                for (Map.Entry<String,Object> entry : md.entrySet()) {
                    Object value = entry.getValue();
                    if (null != value) {
                        sb.append(", ");
                        sb.append(entry.getKey());
                        sb.append(" = ");
                        sb.append(value.toString());
                    }
                }
                sb.append("]");
                description = sb.toString();
            }
        }
        return description;
    }
}
