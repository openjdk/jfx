/*
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.glass.ui.monocle.AcceleratedScreen;
import com.sun.glass.ui.monocle.GLException;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.prism.es2.GLPixelFormat.Attributes;
import com.sun.prism.impl.PrismSettings;
import java.util.HashMap;

/**
 * The GLFactory of the Monocle platforms. The GLES context is created and made current by Java, in
 * {@link AcceleratedScreen}, and {@code prism_es2_monocle} adopts it through {@link ES2Native#contextAdopt}
 * with {@link AcceleratedScreen#lookupGLProc} resolving the entry points, where MonocleGLFactory.c of commit
 * 21d5a654f6 populated the context through JNI with the same {@code dlsym} calls. The adapter count and
 * ordinal are the constants that C returned; {@code gl2} is false, as its never-written field was.
 */
class MonocleGLFactory extends GLFactory {

    // Entries must be in lowercase and null string is a wild card
    // For Linux Beta release we will limit es2 pipe qualification check to NVidia GPUs only
    private GLGPUInfo preQualificationFilter[] = null;
    private GLGPUInfo rejectList[] = null;

    private AcceleratedScreen accScreen = null;

    @Override
    GLGPUInfo[] getPreQualificationFilter() {
        return preQualificationFilter;
    }

    @Override
    GLGPUInfo[] getRejectList() {
        return rejectList;
    }

    @Override
    GLContext createGLContext(long nativeCtxInfo) {
        return new MonocleGLContext(nativeCtxInfo);
    }

    @Override
    GLContext createGLContext(GLDrawable drawable, GLPixelFormat pixelFormat,
                                     GLContext shareCtx, boolean vSyncRequest) {

        // No need to pass down shareCtx as we don't use shared ctx on Monocle
        return new MonocleGLContext(drawable, pixelFormat, vSyncRequest,
                                    accScreen, nativeCtxInfo);
    }

    @Override
    GLDrawable createDummyGLDrawable(GLPixelFormat pixelFormat) {
        return new MonocleGLDrawable(pixelFormat, accScreen);
    }

    @Override
    GLDrawable createGLDrawable(long nativeWindow, GLPixelFormat pixelFormat) {
        return new MonocleGLDrawable(nativeWindow, pixelFormat, accScreen);
    }

    @Override
    GLPixelFormat createGLPixelFormat(long nativeScreen, Attributes attributes) {
        return new MonocleGLPixelFormat(nativeScreen, attributes);
    }

    @Override
    boolean initialize(Class psClass, Attributes attrs) {

        // the pixel format attributes, in the order the accelerated screen reads them
        int attrArr[] = new int[GLPixelFormat.Attributes.NUM_ITEMS];

        attrArr[GLPixelFormat.Attributes.RED_SIZE] = attrs.getRedSize();
        attrArr[GLPixelFormat.Attributes.GREEN_SIZE] = attrs.getGreenSize();
        attrArr[GLPixelFormat.Attributes.BLUE_SIZE] = attrs.getBlueSize();
        attrArr[GLPixelFormat.Attributes.ALPHA_SIZE] = attrs.getAlphaSize();
        attrArr[GLPixelFormat.Attributes.DEPTH_SIZE] = attrs.getDepthSize();
        attrArr[GLPixelFormat.Attributes.DOUBLEBUFFER] = attrs.isDoubleBuffer() ? 1 : 0;
        attrArr[GLPixelFormat.Attributes.ONSCREEN] = attrs.isOnScreen() ? 1 : 0;

        try {
            accScreen = NativePlatformFactory.getNativePlatform().getAcceleratedScreen(

                    attrArr);

            // If the NativePlatform cannot provide hardware accelerated rendering,
            // accScreen can be null
            if (accScreen == null) {
                return false;
            }

            accScreen.enableRendering(true);

            nativeCtxInfo = ES2Native.contextAdopt(accScreen.getGLHandle(), AcceleratedScreen::lookupGLProc);

            accScreen.enableRendering(false);

            if (nativeCtxInfo == 0) {
                // current pipe doesn't support this pixelFormat request
                return false;
            } else {
                // The GLES 2 profile: the C's ContextInfo.gl2 was zeroed and never written.
                gl2 = false;
                return true;
            }
        } catch (GLException e) {
            if (PrismSettings.verbose) {
                e.printStackTrace();
            }
            return false;
        } catch (UnsatisfiedLinkError e) {
            if (PrismSettings.verbose) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /** One adapter, as {@code nGetAdapterCount} of the C answered. */
    @Override
    int getAdapterCount() {
        return 1;
    }

    /** Ordinal 0, as {@code nGetAdapterOrdinal} of the C answered for every screen. */
    @Override
    int getAdapterOrdinal(long nativeScreen) {
        return 0;
    }

    @Override
    void updateDeviceDetails(HashMap deviceDetails) {
    }
}
