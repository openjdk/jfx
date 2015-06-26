/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/** NativeScreen provides access to a device's screen */
public interface NativeScreen {

    /**
     * Returns the bit depth of the screen/
     */
    int getDepth();

    /**
     * Returns the native format of the screen, as a constant from the Pixels
     * class.
     */
    int getNativeFormat();

    /**
     * Returns the pixel width of the screen.
     */
    int getWidth();

    /**
     * Returns the pixel height of the screen.
     */
    int getHeight();

    /**
     * Returns the number of pixels per inch in the screen.
     */
    int getDPI();

    /**
     * Returns a native handle for the screen. The handle is platform-specific.
     */
    long getNativeHandle();

    /**
     * Called during JavaFX shutdown to release the screen. Called only once.
     */
    void shutdown();

    /** Uploads a pixel buffer to the screen. Called on the JavaFX application thread.
     *
     * @param b Pixel data, in BYTE_BGRA_PRE format. The byte stride of the
     *          data is equal to width * 4.
     * @param x The X offset of the pixel data on the screen
     * @param y The Y offset of the pixel data on the screen
     * @param width The pixel width of the data
     * @param height The pixel height of the data
     * @param alpha The alpha level to use to compose the data over existing
     *              pixels
     */
    void uploadPixels(Buffer b,
                             int x, int y, int width, int height, float alpha);

    /**
     * Called on the JavaFX application thread when pixel data for all windows
     * has been uploaded.
     */
    public void swapBuffers();

    /**
     * Returns a read-only ByteBuffer in the native pixel format containing the screen contents.
     * @return ByteBuffer a read-only ByteBuffer containing the screen contents
     */
    public ByteBuffer getScreenCapture();

    /**
     * An Object to lock against when swapping screen buffers.
     */
    public static final Object framebufferSwapLock = new Object();

    /**
     * Return the scale factor between the physical pixels and the logical pixels
     * e.g. hdpi = 1.5, xhdpi = 2.0
     */
    public float getScale();

}
