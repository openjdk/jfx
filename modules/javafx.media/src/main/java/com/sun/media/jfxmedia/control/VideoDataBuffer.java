/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.media.jfxmedia.control;

import java.lang.annotation.Native;
import java.nio.ByteBuffer;

/**
 * A {@code VideoDataBuffer} describes a single frame of video.
 */
public interface VideoDataBuffer {
    /** Plane index used by all packed formats */
    @Native public static final int PACKED_FORMAT_PLANE = 0;

    /** Plane index for YCbCr luminance data */
    @Native public static final int YCBCR_PLANE_LUMA = 0;
    /** Plane index for YCbCr red chrominance data */
    @Native public static final int YCBCR_PLANE_CR = 1;
    /** Plane index for YCbCr blue chrominance data */
    @Native public static final int YCBCR_PLANE_CB = 2;
    /** Plane index for YCbCr alpha data, this plane is optional */
    @Native public static final int YCBCR_PLANE_ALPHA = 3;

    /**
     * Retrieve the data buffer for the specified plane. For chunky formats,
     * pass {@link PACKED_FORMAT_PLANE} as the plane index. If an invalid plane
     * index is passed this method returns null.
     *
     * @param plane The numeric index of the plane
     * @return the {@code ByteBuffer} containing video data for the specified
     * plane or null for non-existent or invalid planes
     */
    public ByteBuffer getBufferForPlane(int plane);

    /**
     * Retrieve the timestamp of the buffer.
     *
     * @return The buffer's timestamp.
     */
    public double getTimestamp();

    /**
     * Gets the width of the VideoDataBuffer
     * @return the width of the buffer
     */
    public int getWidth();

    /**
     * Gets the height of the VideoDataBuffer
     * @return the height
     */
    public int getHeight();

    /**
     * Gets the width of the image as created by the decoder, this may be larger
     * than the display width.
     * @return the number of pixels per row in the image
     */
    public int getEncodedWidth();

    /**
     * Gets the height of the image as created by the decoder, this may be larger
     * than the display height.
     * @return the number of rows in the image
     */
    public int getEncodedHeight();

    /**
     * @return the format of the videoDataBuffer
     */
    public VideoFormat getFormat();

    /**
     * Determine if a video buffer has an alpha channel. This merely determines
     * if the buffer itself has an alpha channel, not if there is any transparency
     * to the image.
     *
     * @return true if an alpha channel is present
     */
    public boolean hasAlpha();

    /**
     * @return the number of planes this video buffer contains, or 1 for
     * non-planar formats
     */
    public int getPlaneCount();

    /**
     * Returns the number of bytes in each row of pixels for the specified plane.
     *
     * @param planeIndex The numeric index of the plane.
     * @return Number of bytes that comprises a single row of pixels in the
     * specified plane. Will return zero if the plane is not in use.
     */
    public int getStrideForPlane(int planeIndex);

    /**
     * @see getStrideForPlane
     * @return an array containing the plane strides for all planes
     */
    public int[] getPlaneStrides();

    /**
     * Converts the video image to the specified format. You can only convert TO
     * either {@code ARGB_PRE} or {@code BGRA_PRE}, converting to YCbCr is not
     * supported here. Once a conversion is done, a reference to the converted
     * buffer is retained so that future conversions do not need to be performed.
     *
     * @param newFormat the video format to convert to
     * @return new buffer containing a converted copy of the source video image
     */
    public VideoDataBuffer convertToFormat(VideoFormat newFormat);

    /**
     * Flags a video buffer indicating the contents of the buffer have been
     * updated and any cached representations need to be updated.
     */
    public void setDirty();

    /**
     * Place a hold on a buffer so that it cannot be reused by the buffer pool
     * from whence it came. Holding a buffer too long may cause additional
     * buffers to be allocated which will increase memory usage, so one should
     * take care to release a frame as soon as possible.
     */
    public void holdFrame();

    /**
     * Releases a hold previously placed on this frame. When the hold count
     * reaches zero then the frame will be disposed or reused, thus preventing
     * memory allocation overhead.
     */
    public void releaseFrame();
}
