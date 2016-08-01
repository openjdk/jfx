/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.NativeMediaManager;
import java.util.List;

/**
 * Factory class used to create media objects, players, and recorders, and to
 * manage other global functionality.
 *
 * @see MediaPlayer
 * @see MediaRecorder
 */
public class MediaManager {

    private MediaManager() {
        // prevent instantiation of this class
    }

    /**
     * @return {@link String} array of supported content types.
     */
    public static String[] getSupportedContentTypes() {
        return NativeMediaManager.getDefaultInstance().getSupportedContentTypes();
    }

    /**
     * Whether a media source having the indicated content type may be
     * played.
     *
     * @throws IllegalArgumentException if <code>contentType</code> is
     * <code>null</code>.
     */
    public static boolean canPlayContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType == null!");
        }
        return NativeMediaManager.getDefaultInstance().canPlayContentType(contentType);
    }

    /**
     * Whether a media source having the indicated protocol may be
     * played.
     *
     * @throws IllegalArgumentException if <code>contentType</code> is
     * <code>null</code>.
     */
    public static boolean canPlayProtocol(String protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null!");
        }
        return NativeMediaManager.getDefaultInstance().canPlayProtocol(protocol);
    }


    // XXX javadoc
    public static MetadataParser getMetadataParser(Locator locator) {
        if (locator == null) {
            throw new IllegalArgumentException("locator == null!");
        }
        return NativeMediaManager.getDefaultInstance().getMetadataParser(locator);
    }

    /**
     * Gets a Media object for the clip.  It cannot be played without attaching
     * to a MediaPlayer.
     *
     * @param locator
     * @return Media object
     * @throws IllegalArgumentException if <code>locator</code> is
     * <code>null</code>.
     */
    public static Media getMedia(Locator locator) {
        if (locator == null) {
            throw new IllegalArgumentException("locator == null!");
        }
        return NativeMediaManager.getDefaultInstance().getMedia(locator);
    }

    /**
     * Get a player for the media locator
     *
     * @param locator
     * @return MediaPlayer object
     * @throws IllegalArgumentException if <code>locator</code> is
     * <code>null</code>.
     */
    public static MediaPlayer getPlayer(Locator locator) {
        if (locator == null) {
            throw new IllegalArgumentException("locator == null!");
        }
        return NativeMediaManager.getDefaultInstance().getPlayer(locator);
    }

    /**
     * Add a global listener for warnings. This listener will receive warnings
     * which occur fall outside the context of a particular player or recorder.
     *
     * @param listener The listener to add.
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public static void addMediaErrorListener(MediaErrorListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null!");
        }
        NativeMediaManager.getDefaultInstance().addMediaErrorListener(listener);
    }

    /**
     * Remove a global listener for warnings.
     *
     * @param listener The listener to remove.
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public static void removeMediaErrorListener(MediaErrorListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null!");
        }
        NativeMediaManager.getDefaultInstance().removeMediaErrorListener(listener);
    }

    /**
     * This function will register MediaPlayer for disposing when obj parameter
     * does not have any strong reference.
     *
     * @param obj - Object to watch for strong references
     * @param player - MediaPlayer to dispose
     */
    public static void registerMediaPlayerForDispose(Object obj, MediaPlayer player) {
        NativeMediaManager.registerMediaPlayerForDispose(obj, player);
    }

    /**
     * Retrieve all un-disposed {@link MediaPlayer}s.
     * @return a {@link List} of all un-disposed players or <code>null</code>.
     */
    public static List<MediaPlayer> getAllMediaPlayers() {
        return NativeMediaManager.getDefaultInstance().getAllMediaPlayers();
    }
}
