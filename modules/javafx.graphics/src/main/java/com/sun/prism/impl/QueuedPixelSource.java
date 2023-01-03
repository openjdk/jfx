/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Pixels;
import com.sun.prism.PixelSource;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Base concrete implementation of the {@code PixelSource} interface which
 * manages {@link Pixels} objects in the state of being consumed (uploaded
 * to the screen usually), in flight in the queue of upload requests, and
 * idle waiting to be reused for temporary storage for future uploads.
 * All {@code Pixels} objects currently saved for reuse will all be the
 * same dimensions and scale which are tracked by calling the
 * {@link #validate(int, int, float) validate()} method.
 * <p>
 * At most we will need 3 sets of pixels:
 * One may be "in use", a hard reference stored in beingConsumed
 * Another may be "in the queue", hard ref stored in enqueued
 * A third may be needed to prepare new pixels while those two are in
 * transit.
 * If the third is filled with pixels and enqueued while the previously
 * mentioned two are still in their stages of use, then it will replace
 * the second object as the "enqueued" reference and the previously
 * enqueued object will then become itself the "third unused" reference.
 * If everything happens in lock step we will often have only one
 * set of pixels.  If the consumer/displayer gets slightly or occasionally
 * behind we might end up with two sets of pixels in play.  Only when things
 * get really bad with multiple deliveries enqueued during the processing
 * of a single earlier delivery will we end up with three sets of
 * {@code Pixels} objects in play.
 */
public class QueuedPixelSource implements PixelSource {
    private volatile Pixels beingConsumed;
    private volatile Pixels enqueued;
    private final List<WeakReference<Pixels>> saved =
         new ArrayList<>(3);
    private final boolean useDirectBuffers;

    public QueuedPixelSource(boolean useDirectBuffers) {
        this.useDirectBuffers = useDirectBuffers;
    }

    @Override
    public synchronized Pixels getLatestPixels() {
        if (beingConsumed != null) {
            throw new IllegalStateException("already consuming pixels: "+beingConsumed);
        }
        if (enqueued != null) {
            beingConsumed = enqueued;
            enqueued = null;
        }
        return beingConsumed;
    }

    @Override
    public synchronized void doneWithPixels(Pixels used) {
        if (beingConsumed != used) {
            throw new IllegalStateException("wrong pixels buffer: "+used+" != "+beingConsumed);
        }
        beingConsumed = null;
    }

    @Override
    public synchronized void skipLatestPixels() {
        if (beingConsumed != null) {
            throw new IllegalStateException("cannot skip while processing: "+beingConsumed);
        }
        enqueued = null;
    }

    private boolean usesSameBuffer(Pixels p1, Pixels p2) {
        if (p1 == p2) return true;
        if (p1 == null || p2 == null) return false;
        return (p1.getBuffer() == p2.getBuffer());
    }

    /**
     * Return an unused Pixels with the indicated dimensions and scale.
     * The returned object may either be saved from a previous use, but
     * currently not being consumed or in the queue.
     * Or it may be an object that reuses a buffer from a previously
     * used (but not active) {@code Pixels} object.
     * Or it may be a brand new object.
     *
     * @param w the width of the desired Pixels object
     * @param h the height of the desired Pixels object
     * @param scalex the horizontal scale of the desired Pixels object
     * @param scaley the vertical scale of the desired Pixels object
     * @return an unused {@code Pixels} object
     */
    public synchronized Pixels getUnusedPixels(int w, int h, float scalex, float scaley) {
        int i = 0;
        IntBuffer reuseBuffer = null;
        while (i < saved.size()) {
            WeakReference<Pixels> ref = saved.get(i);
            Pixels p = ref.get();
            if (p == null) {
                saved.remove(i);
                continue;
            }
            if (usesSameBuffer(p, beingConsumed) || usesSameBuffer(p, enqueued)) {
                i++;
                continue;
            }
            if (p.getWidthUnsafe() == w &&
                p.getHeightUnsafe() == h &&
                p.getScaleXUnsafe() == scalex &&
                p.getScaleYUnsafe() == scaley)
            {
                return p;
            }
            // Whether or not we reuse its buffer, this Pixels object is going away.
            saved.remove(i);
            reuseBuffer = (IntBuffer) p.getPixels();
            if (reuseBuffer.capacity() >= w * h) {
                break;
            }
            reuseBuffer = null;
            // Loop around and see if there are any other buffers to reuse,
            // or get rid of all of the buffers that are too small before
            // we proceed on to the allocation code.
        }
        if (reuseBuffer == null) {
            int bufsize = w * h;
            if (useDirectBuffers) {
                reuseBuffer = BufferUtil.newIntBuffer(bufsize);
            } else {
                reuseBuffer = IntBuffer.allocate(bufsize);
            }
        }
        Pixels p = Application.GetApplication().createPixels(w, h, reuseBuffer, scalex, scaley);
        saved.add(new WeakReference<>(p));
        return p;
    }

    /**
     * Place the indicated {@code Pixels} object into the enqueued state,
     * replacing any other objects that are currently enqueued but not yet
     * being used by the consumer.
     *
     * @param pixels the {@code Pixels} object to be enqueued
     */
    public synchronized void enqueuePixels(Pixels pixels) {
        enqueued = pixels;
    }
}
