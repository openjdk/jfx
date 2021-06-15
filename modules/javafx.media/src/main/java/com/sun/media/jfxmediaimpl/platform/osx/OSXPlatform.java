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
import java.util.Arrays;

/**
 * Mac OS X Platform implementation. This class implements both the QTKit based
 * platform and the AVFoundation based platforms.
 *
 * NOTE: The QTKit based platform is deprecated and will be removed in a future
 * release.
 */
public final class OSXPlatform extends Platform {
    /**
     * The MIME types of all supported media.
     */
    private static final String[] CONTENT_TYPES = {
        "audio/x-aiff",
        "audio/mp3",
        "audio/mpeg",
        "audio/x-m4a",
        "video/mp4",
        "video/x-m4v",
        "application/vnd.apple.mpegurl",
        "audio/mpegurl"
    };

    /**
     * All supported protocols.
     */
    private static final String[] PROTOCOLS = {
        "file",
        "http",
        "https"
    };

    private static final class OSXPlatformInitializer {
        private static final OSXPlatform globalInstance;
        static {
            // Platform is only available if we can load it's native lib
            // Do this early so we can report the correct content types
            boolean isLoaded = false;
            try {
                @SuppressWarnings("removal")
                boolean tmp = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
                    boolean avf = false;
                    boolean qtk = false;
                    // attempt to load the AVFoundation based player first
                    // AVFoundation will have precedence
                    try {
                        NativeLibLoader.loadLibrary("jfxmedia_avf");
                        avf = true;
                    } catch (UnsatisfiedLinkError ule) {}
                    try {
                        NativeLibLoader.loadLibrary("jfxmedia_qtkit");
                        qtk = true;
                    } catch (UnsatisfiedLinkError ule) {}

                    return avf || qtk;
                });
                isLoaded = tmp;
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
            return osxPlatformInit();
        } catch (UnsatisfiedLinkError ule) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "Unable to load OSX platform.");
            }
//            MediaUtils.nativeError(OSXPlatform.class, MediaError.ERROR_MANAGER_ENGINEINIT_FAIL);
            return false;
        }
    }

    @Override
    public String[] getSupportedContentTypes() {
        return Arrays.copyOf(CONTENT_TYPES, CONTENT_TYPES.length);
    }

    @Override
    public String[] getSupportedProtocols() {
        return Arrays.copyOf(PROTOCOLS, PROTOCOLS.length);
    }

    @Override
    public Media createMedia(Locator source) {
        return new OSXMedia(source);
    }

    @Override
    public MediaPlayer createMediaPlayer(Locator source) {
        try {
            return new OSXMediaPlayer(source);
        } catch (Exception ex) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "OSXPlatform caught exception while creating media player: "+ex);
                ex.printStackTrace();
            }
        }
        return null;
    }

    private static native boolean osxPlatformInit();
}
