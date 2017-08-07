/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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


public abstract class WCImageDecoder {

    /**
     * Receives a portion of image data.
     *
     * @param data  a portion of image data,
     *              or {@code null} if all data received
     */
    protected abstract void addImageData(byte[] data);

    /**
     * Returns image size.
     */
    protected abstract int[] getImageSize();

    /**
     * Returns a number of frames of the decoded image.
     *
     * @return  a number of image frames
     */
    protected abstract int getFrameCount();

    /**
     * Returns image frame at the specified index.
     * @param index frame index
     */
    protected abstract WCImageFrame getFrame(int index);

    /**
     * Returns frame duration in ms
     * @param index frame index
     */
    protected abstract int getFrameDuration(int index);

    /**
     * Returns frame size, array[0] represents width and array[1]
     * represents height.
     * @param index frame index
     */
    protected abstract int[] getFrameSize(int index);

    /**
     * Returns whether the frame is complete or partial
     * @param index frame index
     */
    protected abstract boolean getFrameCompleteStatus(int index);

    protected abstract void loadFromResource(String name);

    protected abstract void destroy();

    protected abstract String getFilenameExtension();

}
