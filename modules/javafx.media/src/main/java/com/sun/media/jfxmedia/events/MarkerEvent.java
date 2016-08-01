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

package com.sun.media.jfxmedia.events;

/**
 * The MarkerEvent is returned on marker notifications to a PlayerStateListener.
 * Markers may be embedded in the media source or programmatically inserted.
 *
 * @see com.sun.media.jfxmedia.Media#addMarker(java.lang.String, double)
 * @see com.sun.media.jfxmedia.events.PlayerStateListener
 */
public class MarkerEvent extends PlayerEvent
{
    private String markerName;
    private double presentationTime;

    /** Constructor.
     *
     * @param name The name of the marker.
     * @param time The presentation (stream) time of the marker.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     * or <code>time&lt;0.0</code>.
     */
    public MarkerEvent(String name, double time) {
        if (name == null) {
            throw new IllegalArgumentException("name == null!");
        } else if (time < 0.0) {
            throw new IllegalArgumentException("time < 0.0!");
        }

        this.markerName = name;
        this.presentationTime = time;
    }

    /**
     * Returns the marker name.
     *
     * @return The marker name
     */
    public String getMarkerName()
    {
        return this.markerName;
    }

    /**
     * Returns the presentation time of the Marker.
     *
     * @return The marker time
     */
    public double getPresentationTime()
    {
        return this.presentationTime;
    }
}
