/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import java.nio.ByteBuffer;

/**
 * This interface describes a video image as received by the media stack. It is
 * generally just a wrapper/shim for VideoDataBuffer to avoid dependency issues.
 */
public interface MediaFrame {
    /**
     * @param plane the numeric index of the plane, for chunky formats pass zero
     * @return the {@code ByteBuffer} for the specified plane or null for
     * non-existent planes
     */
    public ByteBuffer getBufferForPlane(int plane);

    /**
     * @return {@link PixelFormat} describing how pixels are stored in this
     * frame's buffer
     */
    public PixelFormat getPixelFormat();

    /**
     * @return width in pixels of the video image contained in this frame
     */
    public int getWidth();

    /**
     * @return height in pixels of the video image contained in this frame
     */
    public int getHeight();

    /**
     * @return width in pixels of the video image as produced by the video
     * decoder
     */
    public int getEncodedWidth();

    /**
     * @return height in pixels of the video image as produced by the video
     * decoder
     */
    public int getEncodedHeight();

    /**
     * @return the number of component planes, for packed formats this will
     * always be one
     */
    public int planeCount();

    /**
     * The plane line stride is the number of bytes between two consecutive
     * lines in the buffer. This number will vary depending on the frame's
     * {@code PixelFormat} and decoder output.
     * @return int array containing the line stride for each plane
     */
    public int[] planeStrides();

    /**
     * The plane line stride is the number of bytes between two consecutive
     * lines in the buffer. This number will vary depending on the frame's
     * {@code PixelFormat} and decoder output.
     * @param planeIndex which plane to get the stride for, valid range is zero
     * to {@link #planeCount() planeCount()} non-inclusive
     * @return the line stride for the specified plane
     */
    public int strideForPlane(int planeIndex);

    /**
     * Converts the video frame to a different video format.
     * @param fmt The new video pixel format, if the same format is specified then
     * the same frame will be returned. If a conversion is unsupported then this
     * will return null. The converted frame must be released when you're done
     * with it by calling {@link #releaseFrame} or it will leak.
     * @return valid MediaFrame in the specified format, or null if it cannot be
     * converted
     */
    public MediaFrame convertToFormat(PixelFormat fmt);

    /**
     * This method will prevent the frame from being deallocated or recycled. It
     * is very important to balance the use of this method by calling releaseFrame
     * when you're done with it otherwise the memory occupied by the frame will
     * never be released which could lead to out of memory conditions.
     */
    public void holdFrame();

    /**
     * When you're finished with a video frame, call this to allow the media
     * subsystem to deallocate or recycle the frame immediately.
     */
    public void releaseFrame();
}
