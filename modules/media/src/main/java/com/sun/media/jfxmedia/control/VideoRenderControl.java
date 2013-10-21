/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.media.jfxmedia.events.VideoFrameRateListener;
import com.sun.media.jfxmedia.events.VideoRendererListener;

/**
 * This interface is supported by the player to support video rendering.
 * It provides methods for registering listeners for getting media frames
 * and a notification mechanism for when new data is available.
 */
public interface VideoRenderControl {
    /**
     * Adds the listener to the player's videoUpdate. The listener
     * will be called whenever a new frame of video is ready to be
     * painted or fetched by getData()
     * @param listener the object which provides the VideoUpdateListener
     * callback interface
     */
    public void addVideoRendererListener(VideoRendererListener listener);

    /**
     * Removes the listener from the player.
     * @param listener to be removed from the player
     */
    public void removeVideoRendererListener(VideoRendererListener listener);

    /**
     * Adds the listener to the player's <code>VideoRenderControl</code>. The
     * listener will be invoked when there is a significant change in the
     * decoded video frame rate.
     *
     * @param listener
     */
    public void addVideoFrameRateListener(VideoFrameRateListener listener);

    /**
     * Remove the listener from the player's <code>VideoRenderControl</code>.
     *
     * @param listener
     */
    public void removeVideoFrameRateListener(VideoFrameRateListener listener);

    /**
     * Gets the width of a video frame
     *
     * @return An integer value for the width.
     */
    public int getFrameWidth();

    /**
     * Gets the height of a video frame
     *
     * @return An integer value for the height.
     */
    public int getFrameHeight();
}
