/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.util.Pair;

/**
 * An {@link ActionEvent} representing a media marker. A marker is added to a
 * {@link Media} which is then played by a {@link MediaPlayer}. The event
 * is fired when the playback position reaches the position of the marker.
 *
 * @see MediaPlayer#onMarkerProperty()
 * @since JavaFX 2.0
 */
public class MediaMarkerEvent extends ActionEvent {

    private static final long serialVersionUID = 20121107L;

    private Pair<String,Duration> marker;

    MediaMarkerEvent(Pair<String,Duration> marker) {
        super();
        this.marker = marker;
    }

    /**
     * Retrieves the marker the event represents.
     *
     * @return the value of the marker.
     */
    public Pair<String,Duration> getMarker() {
        return marker;
    }
}
