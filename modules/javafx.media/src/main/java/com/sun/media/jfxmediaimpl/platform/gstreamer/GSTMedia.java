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

package com.sun.media.jfxmediaimpl.platform.gstreamer;

import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.MediaUtils;
import com.sun.media.jfxmediaimpl.NativeMedia;
import com.sun.media.jfxmediaimpl.platform.Platform;

/**
 * GStreamer implementation of Media
 */
final class GSTMedia extends NativeMedia {
    /**
     * Synchronization mutex for markers.
     */
    private final Object markerMutex = new Object();

    /**
     * Handle to the native media player.
     */
    protected long refNativeMedia;

    GSTMedia(Locator locator) {
        super(locator);

        init();
    }

    @Override
    public Platform getPlatform() {
        return GSTPlatform.getPlatformInstance();
    }

    private void init() {
        //***** Initialize the native media components
        long[] nativeMediaHandle = new long[1];
        MediaError ret;
        Locator loc = getLocator();
        ret = MediaError.getFromCode(gstInitNativeMedia(loc,
                loc.getContentType(), loc.getContentLength(),
                nativeMediaHandle));
        if (ret != MediaError.ERROR_NONE && ret != MediaError.ERROR_PLATFORM_UNSUPPORTED) {
            MediaUtils.nativeError(this, ret);
        }
        this.refNativeMedia = nativeMediaHandle[0];
    }

    long getNativeMediaRef() {
        return refNativeMedia;
    }

    @Override
    public synchronized void dispose() {
        if (0 != refNativeMedia) {
            gstDispose(refNativeMedia);
            refNativeMedia = 0L;
        }
    }

    /**
     * Initialize the native peer of this {@link Media}.
     *
     * @param locator Media location as a Locator object.
     * @return A handle to the native peer of the media.
     */
    private native int gstInitNativeMedia(Locator locator,
                                               String contentType,
                                               long sizeHint,
                                               long[] nativeMediaHandle);
    private native void gstDispose(long refNativeMedia);
}
