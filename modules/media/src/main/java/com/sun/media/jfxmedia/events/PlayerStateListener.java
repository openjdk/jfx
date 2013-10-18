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

package com.sun.media.jfxmedia.events;

/**
 * Notifications related to playing media are sent to PlayerStateListener.
 */
public interface PlayerStateListener
{
    /**
     * The ready state indicates the media is loaded.
     * For best results, developers should wait on OnReady() before playing a media.
     *
     * @param evt
     */
    public void onReady(PlayerStateEvent evt);

    /**
     * The play state indicates the media is beginning to play.
     *
     * @param evt
     */
    public void onPlaying(PlayerStateEvent evt);

    /**
     * The pause state indicates playback has paused.
     *
     * @param evt
     */
    public void onPause(PlayerStateEvent evt);

    /**
     * The stop state indicates playback has paused and presentation time has been reset back to 0.
     * If the player is asked to play() again, playback begins from the beginning.
     *
     * @param evt
     */
    public void onStop(PlayerStateEvent evt);

    public void onStall(PlayerStateEvent evt);


    /**
     * The finish state indicates playback has completed playback to the end.
     *
     * @param evt
     */
    public void onFinish(PlayerStateEvent evt);

    /**
     * The error notification provides information on any error during playback.
     *
     * @param evt
     */
    public void onHalt(PlayerStateEvent evt);
}
