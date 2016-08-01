/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Pixels;

/**
 * An interface to facilitate the asynchronous delivery of frames of pixels
 * between threads with support for an arbitrary number of outstanding
 * delivery requests.
 */
public interface PixelSource {
    /**
     * Gets the next "latest" set of {@code Pixels} to be processed or displayed.
     * This method is called by the consumer of Pixels objects.
     * The return value may be null if the delivery request was obsoleted
     * or superceded while in transit between the threads.
     * The caller should call
     * {@link #doneWithPixels(com.sun.glass.ui.Pixels) doneWithPixels()}
     * if and only if the return value was non-null.
     *
     * @return the next Pixels object to process, or null if there is nothing
     *         to process
     */
    public Pixels getLatestPixels();

    /**
     * Indicates that the specified non-null {@code Pixels} object which was
     * obtained from {@link #getLatestPixels()} is done being processed and
     * can be reused for future pixel deliveries if needed.
     * This method should be called by the consumer if and only if the call to
     * {@code getLatestPixels()} returned a non-null object.
     *
     * @param used a non-null {@code Pixels} object previously obtained from
     *             {@code getLatestPixels()}
     */
    public void doneWithPixels(Pixels used);

    /**
     * A one step method for skipping a pixel delivery object in the case
     * where the consumer is not ready to process any pixels.
     * Examples of cases where this method is used is when deliveries are
     * received for closed, iconified, or disposed windows.
     * This call is equivalent to {@code doneWithPixels(getLatestPixels())}.
     */
    public void skipLatestPixels();
}
