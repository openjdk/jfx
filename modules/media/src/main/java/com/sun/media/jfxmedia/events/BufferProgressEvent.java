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
 * Event class for buffering progress notification.
 */
public class BufferProgressEvent extends PlayerEvent {

    private double  duration;
    private long    start;
    private long    stop;
    private long    position;

    /**
     * Constructor. The state is set to
     * {@link PlayerStateEvent.PlayerState#STALLED}
     *
     * @param start Start position of the buffer
     * @param size Total size of the buffer in bytes
     * @param fill Number of bytes loaded
     */
    public BufferProgressEvent(double duration, long start, long stop, long position) {
        this.duration = duration;
        this.start = start;
        this.stop = stop;
        this.position = position;
    }

    public double getDuration()
    {
        return duration;
    }

    /**
     * Get buffer start position in bytes.
     * @return The buffer start position.
     */
    public long getBufferStart()
    {
        return start;
    }

    /**
     * Gets the stop position of the buffer size in bytes.
     *
     * @return The buffer stop position.
     */
    public long getBufferStop()
    {
        return stop;
    }

    /**
     * Get the total bytes loaded in the buffer from the 0 position.
     *
     * @return The number of bytes loaded.
     */
    public long getBufferPosition()
    {
        return position;
    }
}
