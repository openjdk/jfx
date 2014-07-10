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

package com.sun.media.jfxmediaimpl;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.media.jfxmedia.*;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.platform.PlatformManager;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A class representing a native media engine.
 */
public class NativeMediaManager {
    /**
     * The NativeMediaManager singleton.
     */
    // If we create NativeMediaManager here we will not be able to catch exception from constructor.
    private static NativeMediaManager theInstance = null;
    /**
     * Whether the native layer has been initialized.
     */
    private static boolean isNativeLayerInitialized = false;
    /**
     * The {@link MediaErrorListener}s.
     */
    // FIXME: Change to WeakHashMap<MediaErrorListener,Boolean> as it's more efficient
    private List<WeakReference<MediaErrorListener>> errorListeners =
            new ArrayList<WeakReference<MediaErrorListener>>();
    private static NativeMediaPlayerDisposer playerDisposer = new NativeMediaPlayerDisposer();
    /**
     * List of all un-disposed players.
     */
    private static Map<MediaPlayer,Boolean> allMediaPlayers =
            new WeakHashMap<MediaPlayer,Boolean>();

    // cached content types, so we don't have to poll and sort each time, this list
    // should never change once we're initialized
    private final List<String> supportedContentTypes =
            new ArrayList<String>();

    /**
     * Get the default
     * <code>NativeMediaManager</code>.
     *
     * @return the singleton
     * <code>NativeMediaManager</code> instance.
     */
    public static synchronized NativeMediaManager getDefaultInstance() {
        if (theInstance == null) {
            theInstance = new NativeMediaManager();
        }
        return theInstance;
    }

    //**************************************************************************
    //***** Constructors
    //**************************************************************************
    /**
     * Create a <code>NativeMediaManager</code>.
     */
    protected NativeMediaManager() {}

    /**
     * Initialize the native layer if it has not been so already.
     */
    synchronized static void initNativeLayer() {
        if (!isNativeLayerInitialized) {
            // preload platforms
            PlatformManager.getManager().preloadPlatforms();

            // Load native libraries.
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction) () -> {
                    if (HostUtils.isWindows() || HostUtils.isMacOSX()) {
                        NativeLibLoader.loadLibrary("glib-lite");
                    }

                    if (!HostUtils.isLinux() && !HostUtils.isIOS()) {
                        NativeLibLoader.loadLibrary("gstreamer-lite");
                    }

                    NativeLibLoader.loadLibrary("jfxmedia");
                    return null;
                });
            } catch (Exception e) {
                MediaUtils.error(null, MediaError.ERROR_MANAGER_ENGINEINIT_FAIL.code(),
                        "Unable to load one or more dependent libraries.", e);
                return; // abort
            }

            // Get the Logger native side rolling before we load platforms
            if (!Logger.initNative()) {
                MediaUtils.error(null, MediaError.ERROR_MANAGER_LOGGER_INIT.code(),
                        "Unable to init logger", null);
                return; // abort
            }

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

    /**
     * Returns a copy of the array of supported content types.
     *
     * @return {@link String} array of supported content types.
     */
    public String[] getSupportedContentTypes() {
        if (supportedContentTypes.isEmpty()) {
            loadContentTypes();
        }

        return supportedContentTypes.toArray(new String[1]);
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
