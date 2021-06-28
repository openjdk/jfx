/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.media.jfxmedia.*;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.platform.PlatformManager;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A class representing a native media engine.
 */
public class NativeMediaManager {
    /**
     * Whether the native layer has been initialized.
     */
    private static boolean isNativeLayerInitialized = false;
    /**
     * The {@link MediaErrorListener}s.
     */
    // FIXME: Change to WeakHashMap<MediaErrorListener,Boolean> as it's more efficient
    private final List<WeakReference<MediaErrorListener>> errorListeners =
            new ArrayList();
    private final static NativeMediaPlayerDisposer playerDisposer =
            new NativeMediaPlayerDisposer();
    /**
     * List of all un-disposed players.
     */
    private final static Map<MediaPlayer,Boolean> allMediaPlayers =
            new WeakHashMap();

    // cached content types, so we don't have to poll and sort each time, this list
    // should never change once we're initialized
    private final List<String> supportedContentTypes =
            new ArrayList();
    private final List<String> supportedProtocols =
            new ArrayList<>();

    /**
     * The NativeMediaManager singleton.
     */
    private static class NativeMediaManagerInitializer {
        private static final NativeMediaManager globalInstance
                = new NativeMediaManager();
    }

    /**
     * Get the default
     * <code>NativeMediaManager</code>.
     *
     * @return the singleton
     * <code>NativeMediaManager</code> instance.
     */
    public static NativeMediaManager getDefaultInstance() {
        return NativeMediaManagerInitializer.globalInstance;
    }

    //**************************************************************************
    //***** Constructors
    //**************************************************************************
    /**
     * Create a <code>NativeMediaManager</code>.
     */
    @SuppressWarnings("removal")
    protected NativeMediaManager() {
        /*
         * Load native libraries. This must be done early as platforms may need
         * to attempt loading their own native libs that are dependent on these
         * This is a slight performance hit, but necessary otherwise we could
         * erroneously report content types for platforms that cannot be loaded
         */
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction) () -> {
                ArrayList<String> dependencies = new ArrayList<>();
                if (HostUtils.isWindows() || HostUtils.isMacOSX()) {
                    NativeLibLoader.loadLibrary("glib-lite");
                }

                if (!HostUtils.isLinux() && !HostUtils.isIOS()) {
                    NativeLibLoader.loadLibrary("gstreamer-lite");
                } else {
                    dependencies.add("gstreamer-lite");
                }
                if (HostUtils.isLinux()) {
                    dependencies.add("fxplugins");
                    dependencies.add("avplugin");
                    dependencies.add("avplugin-54");
                    dependencies.add("avplugin-56");
                    dependencies.add("avplugin-57");
                    dependencies.add("avplugin-ffmpeg-56");
                    dependencies.add("avplugin-ffmpeg-57");
                    dependencies.add("avplugin-ffmpeg-58");
                }
                if (HostUtils.isMacOSX()) {
                    dependencies.add("fxplugins");
                    dependencies.add("glib-lite");
                    dependencies.add("jfxmedia_avf");
                }
                if (HostUtils.isWindows()) {
                    dependencies.add("fxplugins");
                    dependencies.add("glib-lite");
                }
                NativeLibLoader.loadLibrary("jfxmedia", dependencies);
                return null;
            });
        } catch (PrivilegedActionException pae) {
            MediaUtils.error(null, MediaError.ERROR_MANAGER_ENGINEINIT_FAIL.code(),
                    "Unable to load one or more dependent libraries.", pae);
        }

        // Get the Logger native side rolling before we load platforms
        if (!Logger.initNative()) {
            MediaUtils.error(null, MediaError.ERROR_MANAGER_LOGGER_INIT.code(),
                    "Unable to init logger", null);
        }
    }

    /**
     * Initialize the native layer if it has not been so already.
     */
    synchronized static void initNativeLayer() {
        if (!isNativeLayerInitialized) {
            // load platforms
            PlatformManager.getManager().loadPlatforms();

            // Set the native initialization flag, even if initialization failed.
            isNativeLayerInitialized = true;
        }
    }

    //**************************************************************************
    //***** Public control functions
    //**************************************************************************

    private synchronized void loadContentTypes() {
        if (!supportedContentTypes.isEmpty()) {
            // already populated, just return
            return;
        }

        List<String> npt = PlatformManager.getManager().getSupportedContentTypes();
        if (null != npt && !npt.isEmpty()) {
            supportedContentTypes.addAll(npt);
        }

        if (Logger.canLog(Logger.DEBUG)) {
            StringBuilder sb = new StringBuilder("JFXMedia supported content types:\n");
            for (String type : supportedContentTypes) {
                sb.append("    ");
                sb.append(type);
                sb.append("\n");
            }
            Logger.logMsg(Logger.DEBUG, sb.toString());
        }
    }

    private synchronized void loadProtocols() {
        if (!supportedProtocols.isEmpty()) {
            // already populated, just return
            return;
        }

        List<String> npt = PlatformManager.getManager().getSupportedProtocols();
        if (null != npt && !npt.isEmpty()) {
            supportedProtocols.addAll(npt);
        }

        if (Logger.canLog(Logger.DEBUG)) {
            StringBuilder sb = new StringBuilder("JFXMedia supported protocols:\n");
            for (String type : supportedProtocols) {
                sb.append("    ");
                sb.append(type);
                sb.append("\n");
            }
            Logger.logMsg(Logger.DEBUG, sb.toString());
        }
    }

    /**
     * Whether a media source having the indicated content type may be played.
     *
     * @see MediaManager#canPlayContentType(java.lang.String)
     *
     * @throws IllegalArgumentException if
     * <code>contentType</code> is
     * <code>null</code>.
     */
    public boolean canPlayContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType == null!");
        }

        if (supportedContentTypes.isEmpty()) {
            loadContentTypes();
        }

        /*
         * Don't just use supportedContentType.contains(contentType) as that
         * is case sensitive, which we do not want
         */
        for (String type : supportedContentTypes) {
            if (contentType.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    public String[] getSupportedContentTypes() {
        if (supportedContentTypes.isEmpty()) {
            loadContentTypes();
        }

        return supportedContentTypes.toArray(new String[1]);
    }

    /**
     * Whether a media source having the indicated protocol may be played.
     *
     * @see MediaManager#canPlayProtocol(java.lang.String)
     *
     * @throws IllegalArgumentException if
     * <code>protocol</code> is
     * <code>null</code>.
     */
    public boolean canPlayProtocol(String protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null!");
        }

        if (supportedProtocols.isEmpty()) {
            loadProtocols();
        }

        /*
         * Don't just use supportedProtocols.contains(protocol) as that
         * is case sensitive, which we do not want
         */
        for (String type : supportedProtocols) {
            if (protocol.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    public static MetadataParser getMetadataParser(Locator locator) {
        return PlatformManager.getManager().createMetadataParser(locator);
    }

    /**
     * @see MediaManager#getPlayer(com.sun.media.jfxmedia.locator.Locator, int)
     */
    public MediaPlayer getPlayer(Locator locator) {
        // FIXME: remove this
        initNativeLayer();

        MediaPlayer player = PlatformManager.getManager().createMediaPlayer(locator);
        if (null == player) {
            throw new MediaException("Could not create player!");
        }

        // Cache a reference to the player.
        allMediaPlayers.put(player, Boolean.TRUE);

        return player;
    }

    /**
     * Get a player for the media locator. A preference may be set as to whether
     * to allow a full scan of the media.
     *
     * FIXME: Nuke permitFullScan, it is unused and has no effect
     *
     * @param locator
     * @param permitFullScan
     * @return MediaPlayer object
     */
    public Media getMedia(Locator locator) {
        initNativeLayer();
        return PlatformManager.getManager().createMedia(locator);
    }

    /**
     * @see
     * MediaManager#addMediaErrorListener(com.sun.media.jfxmedia.events.MediaErrorListener)
     */
    public void addMediaErrorListener(MediaErrorListener listener) {
        if (listener != null) {
            // Since we have only one instance of NativeMediaManager, all media players
            // created during application lifecycle will keep weak references to error
            // listeners in errorListeners. Lets clean up unused references.
            // FIXME: change to WeakHashMap<MEL,Boolean> as it's more efficient
            for (ListIterator<WeakReference<MediaErrorListener>> it = errorListeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l == null) {
                    it.remove();
                }
            }

            this.errorListeners.add(new WeakReference<MediaErrorListener>(listener));
        }
    }

    /**
     * @see
     * MediaManager#removeMediaErrorListener(com.sun.media.jfxmedia.events.MediaErrorListener)
     */
    public void removeMediaErrorListener(MediaErrorListener listener) {
        if (listener != null) {
            // FIXME: change to WeakHashMap<MEL,Boolean> as it's more efficient
            for (ListIterator<WeakReference<MediaErrorListener>> it = errorListeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
            }
        }
    }
        }

    /**
     * This function will register MediaPlayer for disposing when obj parameter
     * does not have any strong reference.
     *
     * FIXME: Nuke this and use MediaDisposer instead
     *
     * @param obj - Object to watch for strong references
     * @param player - MediaPlayer to dispose
     */
    public static void registerMediaPlayerForDispose(Object obj, MediaPlayer player) {
        MediaDisposer.addResourceDisposer(obj, player, playerDisposer);
    }

    /**
     * Retrieve all un-disposed {@link MediaPlayer}s.
     *
     * @return a {@link List} of all un-disposed players or
     * <code>null</code>.
     */
    public List<MediaPlayer> getAllMediaPlayers() {
        List<MediaPlayer> allPlayers = null;

        if (!allMediaPlayers.isEmpty()) {
            allPlayers = new ArrayList<MediaPlayer>(allMediaPlayers.keySet());
        }

        return allPlayers;
    }

    //**************************************************************************
    //***** Private functions
    //**************************************************************************
    List<WeakReference<MediaErrorListener>> getMediaErrorListeners() {
        return this.errorListeners;
    }

    private static class NativeMediaPlayerDisposer implements MediaDisposer.ResourceDisposer {

        public void disposeResource(Object resource) {
            // resource is a MediaPlayer
            MediaPlayer player = (MediaPlayer) resource;
            if (player != null) {
                player.dispose();
            }
        }
    }
}
