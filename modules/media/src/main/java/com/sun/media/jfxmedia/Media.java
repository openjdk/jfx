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

package com.sun.media.jfxmedia;

import java.util.Collections;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.track.Track;

/**
 * A class representing a particular media.
 *
 * @see MediaManager
 * @see MediaPlayer
 */
public abstract class Media {

    private Locator locator;
    private final List<Track> tracks = new ArrayList<Track>();

    /**
     * Create a <code>Media</code> object.
     *
     * @param locator <code>Locator</code> of the <code>Media</code>
     * @throws <code>IllegalArgumentException</code> if <code>locator</code>
     * is <code>null</code>.
     */
    protected Media(Locator locator) {
        if (locator == null) {
            throw new IllegalArgumentException("locator == null!");
        }

        this.locator = locator;
    }

    /**
     * Adds a marker to the media playback.  This marker will not be as precise
     * as embedded markers in the media file.
     *
     * @param markerName        Arbitrary name of the marker
     * @param presentationTime  Presentation time for the marker
     * @throws <code>IllegalArgumentException</code> if <code>markerName</code>
     * is <code>null</code> or <code>presentationTime</code> is negative.
     */
    public abstract void addMarker(String markerName, double presentationTime);

    /**
     * Removes a marker by name.
     *
     * @param markerName    Name of the marker
     * @return The presentation time of the deleted marker.
     * @throws <code>IllegalArgumentException</code> if <code>markerName</code>
     * is <code>null</code>.
     */
    public abstract double removeMarker(String markerName);

    /**
     * Removes all markers, added programmatically, from the media playback.
     * Embedded markers will still cause notifications to fire.
     */
    public abstract void removeAllMarkers();

    /**
     * Gets the tracks found in the media. The returned value will be
     * <code>null</code> if no tracks have yet been encountered while scanning
     * the media. The returned <code>List</code> us unmodifiable.
     *
     * @return the tracks in the media or <code>null</code> if no tracks found.
     */
    public List<Track> getTracks() {
        List<Track> returnValue;
        synchronized(tracks) {
            if (tracks.isEmpty()) {
                returnValue = null;
            } else {
                returnValue = Collections.unmodifiableList(new ArrayList<Track>(tracks));
            }
        }
        return returnValue;
    }

    /**
     * Get the markers of the media. The returned
     * <code>Map</code> is unmodifiable.
     *
     * @return the markers or <code>null</code> if no markers found.
     */
    public abstract Map<String, Double> getMarkers();

    /**
     * Gets the <code>Locator</code> which was the source of the media.
     *
     * @return the source <code>Locator</code>.
     */
    public Locator getLocator() {
        return locator;
    }

    /**
     * Adds a <code>Track</code>.
     * @throws <code>IllegalArgumentException</code> if <code>track</code> is
     * <code>null</code>.
     */
    protected void addTrack(Track track) {
        if (track == null) {
            throw new IllegalArgumentException("track == null!");
        }
        synchronized(tracks) {
            this.tracks.add(track);
        }
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        if(tracks != null && !tracks.isEmpty()) {
            for(Track track : tracks) {
                buffer.append(track);
                buffer.append("\n");
            }
        }

        return buffer.toString();
    }
}