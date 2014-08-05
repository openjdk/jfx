/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform.osx;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.HostUtils;
import com.sun.media.jfxmediaimpl.platform.Platform;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Mac OS X Platform implementation.
 */
public final class OSXPlatform extends Platform {
    /**
     * The MIME types of all supported media.
     */
    private static final String[] CONTENT_TYPES = {
        "video/mp4",
        "audio/x-m4a",
        "video/x-m4v",
        "application/vnd.apple.mpegurl",
        "audio/mpegurl"
    };

    private static final class OSXPlatformInitializer {
        private static final OSXPlatform globalInstance;
        static {
            // Platform is only available if we can load it's native lib
            // Do this early so we can report the correct content types
            boolean isLoaded = false;
            try {
                isLoaded = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                    try {
                        NativeLibLoader.loadLibrary("jfxmedia_qtkit");
                    } catch (UnsatisfiedLinkError ule) {
                        // non-fatal condition, keep quiet about it
                        return Boolean.FALSE;
                    }
                    return Boolean.TRUE;
                });
            } catch (Exception e) {
                // Ignore
            }
            if (isLoaded) {
                globalInstance = new OSXPlatform();
            } else {
                globalInstance = null;
            }
        }
    }

    public static Platform getPlatformInstance() {
        return OSXPlatformInitializer.globalInstance;
    }

    private OSXPlatform() {
    }

    /**
     * @return false if the platform cannot be loaded
     */
    @Override
    public boolean loadPlatform() {
        if (!HostUtils.isMacOSX()) {
            return false;
        }

        // ULE should not happen here, but just in case
        try {
            osxPlatformInit();
        } catch (UnsatisfiedLinkError ule) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "Unable to load OSX platform.");
            }
//                MediaUtils.nativeError(OSXPlatform.class, MediaError.ERROR_MANAGER_ENGINEINIT_FAIL);
            return false;
        }
        return true;
    }

    @Override
    public String[] getSupportedContentTypes() {
        String[] contentTypesCopy = new String[CONTENT_TYPES.length];
        System.arraycopy(CONTENT_TYPES, 0, contentTypesCopy, 0, CONTENT_TYPES.length);
        return contentTypesCopy;
    }

    @Override
    public Media createMedia(Locator source) {
        return new OSXMedia(source);
    }

    @Override
    public Object prerollMediaPlayer(Locator source) {
        // attempt the actual player creation, then preroll here
        // on success we return a reference to the native player as the cookie
        return new OSXMediaPlayer(source);
    }

    @Override
    public MediaPlayer createMediaPlayer(Locator source, Object cookie) {
        if (cookie instanceof OSXMediaPlayer) {
            OSXMediaPlayer player = (OSXMediaPlayer)cookie;
            // do native initialization
            player.initializePlayer();
            return player;
        }
        return null;
    }

    private static native void osxPlatformInit();
}
