/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform;

import com.sun.javafx.PlatformUtil;
import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.MetadataParser;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.platform.java.JavaPlatform;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Method;

/**
 * Core media platform management code.
 */
public final class PlatformManager {
    private static String enabledPlatforms;
    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction) () -> {
            getPlatformSettings();
            return null;
        });
    }

    private static void getPlatformSettings() {
        // get enabled platforms, comma separated list, e.g., -Djfxmedia.platforms=GSTPlatform,OSXPlatform
        enabledPlatforms = System.getProperty("jfxmedia.platforms", "").toLowerCase();
    }

    private static boolean isPlatformEnabled(String name) {
        if (null == enabledPlatforms || enabledPlatforms.length() == 0) {
            // everything enabled
            return true;
        }
        return (enabledPlatforms.indexOf(name.toLowerCase()) != -1);
    }

    private static final class PlatformManagerInitializer {
        private static final PlatformManager globalInstance = new PlatformManager();
    }

    public static PlatformManager getManager() {
        return PlatformManagerInitializer.globalInstance;
    }

    private final List<Platform> platforms;

    private PlatformManager() {
        platforms = new ArrayList<>();

        Platform platty;

        /*
         * We don't want to fully initialize the platforms here for performance
         * reasons but some platforms may be dependent on native resources that
         * need to be loaded, those platforms need to be given a chance to load
         * those resources (without initializing) and determine if the natives
         * are available.
         */

        // Now "universal" platform(s)
        if (isPlatformEnabled("JavaPlatform")) {
            platty = JavaPlatform.getPlatformInstance();
            if (null != platty) {
                platforms.add(platty);
            }
        }

        if (!PlatformUtil.isIOS() && isPlatformEnabled("GSTPlatform")) {
            platty = getPlatformInstance(
                    "com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform");
            if (null != platty) {
                platforms.add(platty);
            }
        }

        // Add after GSTPlatform so it's used as a fallback
        if (PlatformUtil.isMac() && isPlatformEnabled("OSXPlatform")) {
            platty = getPlatformInstance(
                    "com.sun.media.jfxmediaimpl.platform.osx.OSXPlatform");
            if (null != platty) {
                platforms.add(platty);
            }
        }

        if (PlatformUtil.isIOS() && isPlatformEnabled("IOSPlatform")) {
            platty = getPlatformInstance(
                    "com.sun.media.jfxmediaimpl.platform.ios.IOSPlatform");
            if (null != platty) {
                platforms.add(platty);
            }
        }

        if (Logger.canLog(Logger.DEBUG)) {
            StringBuilder sb = new StringBuilder("Enabled JFXMedia platforms: ");
            for (Platform p : platforms) {
                sb.append("\n   - ");
                sb.append(p.getClass().getName());
            }
            Logger.logMsg(Logger.DEBUG, sb.toString());
        }
    }

    private Platform getPlatformInstance(String platformClass) {
        try {
            Class c = Class.forName(platformClass);
            Method m = c.getDeclaredMethod("getPlatformInstance", (Class[])null);
            Object platform = m.invoke(null, (Object[])null);
            return (Platform)platform;
        } catch (Exception e) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "Failed to get platform instance" +
                              " for " + platformClass + ". Exception: " +
                              e.getMessage());
            }
        }

        return null;
    }

    public synchronized void loadPlatforms() {
        // Use an iterator so we can remove on failure
        Iterator<Platform> iter = platforms.iterator();
        while (iter.hasNext()) {
            Platform platty = iter.next();
            if (!platty.loadPlatform()) {
                if (Logger.canLog(Logger.DEBUG)) {
                    Logger.logMsg(Logger.DEBUG, "Failed to load platform: "+platty);
                }
                // remove it so it can't be reused
                iter.remove();
            }
        }
    }

    public List<String> getSupportedContentTypes() {
        ArrayList<String> outTypes = new ArrayList<>();

        if (!platforms.isEmpty()) {
            for (Platform platty : platforms) {
                if (Logger.canLog(Logger.DEBUG)) {
                    Logger.logMsg(Logger.DEBUG, "Getting content types from platform: "+platty);
                }
                String[] npt = platty.getSupportedContentTypes();
                if (npt != null) {
                    for (String type : npt) {
                        if (!outTypes.contains(type)) {
                            outTypes.add(type);
                        }
                    }
                }
            }
        }

        return outTypes;
    }

    public List<String> getSupportedProtocols() {
        ArrayList<String> outProtocols = new ArrayList<>();

        if (!platforms.isEmpty()) {
            for (Platform platty : platforms) {
                String[] npt = platty.getSupportedProtocols();
                if (npt != null) {
                    for (String p : npt) {
                        if (!outProtocols.contains(p)) {
                            outProtocols.add(p);
                        }
                    }
                }
            }
        }
        if (PlatformUtil.isStaticBuild()) {
            outProtocols.add("resource");
        }

        return outProtocols;
    }

    public MetadataParser createMetadataParser(Locator source) {
        for (Platform platty : platforms) {
            MetadataParser parser = platty.createMetadataParser(source);
            if (parser != null) {
                return parser;
            }
        }

        return null;
    }

    // FIXME: Make Media non-platform specific, it doesn't need to be
    public Media createMedia(Locator source) {
        String mimeType = source.getContentType();
        String protocol = source.getProtocol();
        // go down the list until we get one that can be created
        for (Platform platty : platforms) {
            if (platty.canPlayContentType(mimeType) && platty.canPlayProtocol(protocol)) {
                Media outMedia = platty.createMedia(source);
                if (null != outMedia) {
                    return outMedia;
                }
            }
        }

        return null;
    }

    public MediaPlayer createMediaPlayer(Locator source) {
        String mimeType = source.getContentType();
        String protocol = source.getProtocol();
        // go down the list until we get one that can be created
        for (Platform platty : platforms) {
            if (platty.canPlayContentType(mimeType) && platty.canPlayProtocol(protocol)) {
                MediaPlayer outPlayer = platty.createMediaPlayer(source);
                if (null != outPlayer) {
                    return outPlayer;
                }
            }
        }

        return null;
    }
}
