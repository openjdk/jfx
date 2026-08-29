/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The {@code WKJHostMedia} group: the sixteen upcalls of {@code MediaPlayerPrivateJava}. All
 * sixteen are filled.
 * <p>
 * The first two are {@link WCGraphicsManager} methods rather than {@link WCMediaPlayer} ones and so
 * take no target ref; they are here rather than in {@link GraphicsUpcalls} because
 * {@code MediaPlayerPrivateJava} is their only caller, which is where the C header puts them too.
 * <p>
 * {@code get_supported_types} is the one shape change in the group: {@code getSupportedMediaTypes}
 * returns a {@code String[]} and the ABI carries it as one string whose elements are separated by
 * {@code WKJ_MEDIA_TYPE_SEPARATOR}, which is {@code U+000A}. A MIME type cannot contain a newline,
 * so the join is lossless, and an empty array is an empty string rather than {@code null} - the C
 * header keeps those two apart.
 * <p>
 * <b>Threading.</b> Every slot runs on the WebKit main thread: {@code MediaPlayerPrivateInterface}
 * is main-thread by WebKit contract.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class MediaUpcalls {

    /** {@code WKJ_MEDIA_TYPE_SEPARATOR}, the {@code U+000A} that joins the supported MIME types. */
    private static final char MEDIA_TYPE_SEPARATOR = '\n';

    private MediaUpcalls() {
    }

    /**
     * Fills the {@code media} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    public static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        WebKitNative.installHostSlot(host, "media.create_player", lookup, "createPlayer",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.get_supported_types", lookup, "getSupportedTypes",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        WebKitNative.installHostSlot(host, "media.dispose", lookup, "dispose",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.load", lookup, "load",
                FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "media.cancel_load", lookup, "cancelLoad",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.prepare_to_play", lookup, "prepareToPlay",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.play", lookup, "play",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.pause", lookup, "pause",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.get_current_time", lookup, "getCurrentTime",
                FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG));
        WebKitNative.installHostSlot(host, "media.seek", lookup, "seek",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT));
        WebKitNative.installHostSlot(host, "media.set_rate", lookup, "setRate",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT));
        WebKitNative.installHostSlot(host, "media.set_preserves_pitch", lookup, "setPreservesPitch",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        WebKitNative.installHostSlot(host, "media.set_volume", lookup, "setVolume",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT));
        WebKitNative.installHostSlot(host, "media.set_mute", lookup, "setMute",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        WebKitNative.installHostSlot(host, "media.set_size", lookup, "setSize",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT));
        WebKitNative.installHostSlot(host, "media.set_preload", lookup, "setPreload",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    }

    /*
     * WCGraphicsManager.fwkCreateMediaPlayer(long) -> WCMediaPlayer. "player" is the
     * WebCore::MediaPlayerPrivate* the wkj_media_notify_ downcalls carry back. Default: 0.
     */
    private static long createPlayer(long player) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            return manager == null ? 0L
                    : WebKitNative.register(manager.fwkCreateMediaPlayer(player));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.create_player", t);
            return 0L;
        }
    }

    /*
     * WCGraphicsManager.getSupportedMediaTypes() -> String[], joined with U+000A. An empty array is
     * WKJ_STR_OK with length 0, not WKJ_STR_NULL. Default when NULL: WKJ_STR_NULL.
     */
    private static int getSupportedTypes(MemorySegment out, int capacity, MemorySegment length) {
        try {
            WCGraphicsManager manager = WCGraphicsManager.getGraphicsManager();
            String[] types = manager == null ? null : manager.getSupportedMediaTypes();
            return WebKitNative.emitString(join(types), out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.get_supported_types", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    private static String join(String[] types) {
        if (types == null) {
            return null;
        }
        StringBuilder joined = new StringBuilder();
        for (String type : types) {
            if (type == null) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(MEDIA_TYPE_SEPARATOR);
            }
            joined.append(type);
        }
        return joined.toString();
    }

    /* fwkDispose(). Called from ~MediaPlayerPrivate. Default when NULL: no-op. */
    private static void dispose(long player) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkDispose();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.dispose", t);
        }
    }

    /*
     * fwkLoad(String url, String userAgent). A NULL user agent is the null the JNI code passed when
     * the user agent was empty, and is deliberately not normalised to "".
     * Default when NULL: no-op.
     */
    private static void load(long player, MemorySegment url, int urlLength,
                             MemorySegment userAgent, int userAgentLength) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkLoad(WebKitNative.readString(url, urlLength),
                        WebKitNative.readString(userAgent, userAgentLength));
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.load", t);
        }
    }

    /* fwkCancelLoad(). Default when NULL: no-op. */
    private static void cancelLoad(long player) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkCancelLoad();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.cancel_load", t);
        }
    }

    /* fwkPrepareToPlay(). Default when NULL: no-op. */
    private static void prepareToPlay(long player) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkPrepareToPlay();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.prepare_to_play", t);
        }
    }

    /* fwkPlay(). Default when NULL: no-op. */
    private static void play(long player) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkPlay();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.play", t);
        }
    }

    /* fwkPause(). Default when NULL: no-op. */
    private static void pause(long player) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkPause();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.pause", t);
        }
    }

    /* fwkGetCurrentTime(), in seconds. Default when NULL: 0.0f. */
    private static float getCurrentTime(long player) {
        try {
            return WebKitNative.lookup(player) instanceof WCMediaPlayer target
                    ? target.fwkGetCurrentTime()
                    : 0.0f;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.get_current_time", t);
            return 0.0f;
        }
    }

    /* fwkSeek(float). Default when NULL: no-op. */
    private static void seek(long player, float time) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSeek(time);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.seek", t);
        }
    }

    /* fwkSetRate(float). Default when NULL: no-op. */
    private static void setRate(long player, float rate) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetRate(rate);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_rate", t);
        }
    }

    /* fwkSetPreservesPitch(boolean). Default when NULL: no-op. */
    private static void setPreservesPitch(long player, int preserve) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetPreservesPitch(preserve != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_preserves_pitch", t);
        }
    }

    /* fwkSetVolume(float). Default when NULL: no-op. */
    private static void setVolume(long player, float volume) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetVolume(volume);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_volume", t);
        }
    }

    /* fwkSetMute(boolean). Default when NULL: no-op. */
    private static void setMute(long player, int mute) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetMute(mute != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_mute", t);
        }
    }

    /* fwkSetSize(int, int). Default when NULL: no-op. */
    private static void setSize(long player, int width, int height) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetSize(width, height);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_size", t);
        }
    }

    /* fwkSetPreload(int), one of the WKJ_MEDIA_PRELOAD_ values. Default when NULL: no-op. */
    private static void setPreload(long player, int preload) {
        try {
            if (WebKitNative.lookup(player) instanceof WCMediaPlayer target) {
                target.fwkSetPreload(preload);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("media.set_preload", t);
        }
    }
}
