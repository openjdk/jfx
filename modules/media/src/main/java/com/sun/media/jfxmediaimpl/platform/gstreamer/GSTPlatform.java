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

import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.events.PlayerStateEvent.PlayerState;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.HostUtils;
import com.sun.media.jfxmediaimpl.MediaUtils;
import com.sun.media.jfxmediaimpl.platform.Platform;

/**
 * GStreamer platform implementation.
 */
public final class GSTPlatform extends Platform {
    /**
     * The MIME types of all supported media.
     */
    private static final String[] CONTENT_TYPES = {
        "audio/x-aiff",
        "audio/mp3",
        "audio/mpeg",
        "audio/x-wav",
        "video/x-javafx",
        "video/x-flv",
        "video/x-fxm",
        "video/mp4",
        "audio/x-m4a",
        "video/x-m4v",
        "application/vnd.apple.mpegurl",
        "audio/mpegurl"
    };

    private static GSTPlatform globalInstance = null;

    // HACK: move this back to native when we fix the jdeveloper crash
    private static final String[] contentTypes;

    static {
        // HACK: remove this when we go back to polling native
        if (!HostUtils.isMacOSX()) {
            contentTypes = new String[CONTENT_TYPES.length];
            System.arraycopy(CONTENT_TYPES, 0, contentTypes,
                    0, CONTENT_TYPES.length);
        } else {
            contentTypes = CONTENT_TYPES;
        }
    }

    @Override
    public boolean loadPlatform() {
        // Initialize GStreamer JNI and supporting native classes.
        MediaError ret;
        try {
            ret = MediaError.getFromCode(gstInitPlatform());
        } catch (UnsatisfiedLinkError ule) {
            ret = MediaError.ERROR_MANAGER_ENGINEINIT_FAIL;
        }
        // Post an error if native initialization fails.
        if (ret != MediaError.ERROR_NONE) {
            MediaUtils.nativeError(GSTPlatform.class, ret);
        }
        return true;
    }

    /*
     * Get an instance of the platform.
     */
    public static synchronized Platform getPlatformInstance() {
        if (null == globalInstance) {
            globalInstance = new GSTPlatform();
        }

        return globalInstance;
    }

    private GSTPlatform() {}

    @Override
    public String[] getSupportedContentTypes() {
        String[] contentTypesCopy = new String[contentTypes.length];
        System.arraycopy(contentTypes, 0, contentTypesCopy, 0, contentTypes.length);
        return contentTypesCopy;
    }

    public Media createMedia(Locator source) {
        return new GSTMedia(source);
    }

    @Override
    public Object prerollMediaPlayer(Locator source) {
        GSTMediaPlayer player;
        try {
            player = new GSTMediaPlayer(source);
        } catch (Exception e) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "GSTPlatform caught exception while prerolling: "+e);
            }
            return null;
        }

        // Special case for H.264 decoding on Mac OS X.
        if (player != null && HostUtils.isMacOSX()) {
            String contentType = source.getContentType();
            if ("video/mp4".equals(contentType) || "video/x-m4v".equals(contentType)
                  || source.getStringLocation().endsWith(".m3u8"))
            {
                // Block until player transitions to READY or HALTED.

                // Timeouts in milliseconds.
                final long timeout = source.getURI().getScheme().equals("http") ?
                        60000L : 5000L;
                final long iterationTime = 50L;
                long timeWaited = 0L;

                final Object lock = new Object();
                PlayerState state = player.getState();

                while (timeWaited < timeout &&
                        (state == PlayerState.UNKNOWN || state == PlayerState.STALLED)) {
                    try {
                        synchronized(lock) {
                            lock.wait(iterationTime);
                            timeWaited += iterationTime;
                        }
                    } catch (InterruptedException ex) {
                        // Ignore it.
                    }

                    state = player.getState();
                }

                // If the player is not READY, an error occurred.
                if (player.getState() != PlayerState.READY) {
                    player.dispose();
                    player = null;
                }
            }
        }
        return player;
    }

    public MediaPlayer createMediaPlayer(Locator source, Object cookie) {
        if(cookie == null) {
            throw new NullPointerException("null player!");
        } else if(!(cookie instanceof GSTMediaPlayer)) {
            throw new IllegalArgumentException("!(cookie instanceof GSTMediaPlayer)");
        }

        return (MediaPlayer)cookie;
    }

    /**
     * Initialize the native peer of this media manager.
     *
     * @return A status code.
     */
    private static native int gstInitPlatform();
}
