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

package com.sun.prism.impl;

import com.sun.glass.ui.Pixels;
import com.sun.prism.PixelSource;
import java.lang.ref.WeakReference;
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
         new ArrayList<WeakReference<Pixels>>(3);
    private int pixelW;
    private int pixelH;
    private float pixelScale;

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

    /**
     * Validates the saved pixels objects against the specified dimensions
     * and pixel scale and returns a boolean indicating if the pixel buffers
     * are still valid.
     * This method may free old saved buffers so that they will not be reused,
     * but it will leave the existing enqueued buffers alone until they are
     * eventually replaced.
     * 
     * @param w the intended width of the {@code Pixels} objects
     * @param h the intended height of the {@code Pixels} objects
     * @param scale the intended pixel scale of the {@code Pixels} objects
     * @return 
     */
    public synchronized boolean validate(int w, int h, float scale) {
        if (w != pixelW || h != pixelH || scale != pixelScale) {
            saved.clear();
            pixelW = w;
            pixelH = h;
            pixelScale = scale;
            return false;
        }
        return true;
    }

    /**
     * Return an unused Pixels object previously used with this
     * {@code PixelSource}, or null if there are none.
     * The caller should create a new {@code Pixels} object if
     * this method returns null and register it with a call
     * to {@link #enqueuePixels(com.sun.glass.ui.Pixels) enqueuePixels()}
     * when it is filled with data.
     * 
     * @return an unused {@code Pixels} object or null if the caller
     *         should create a new one
     */
    public synchronized Pixels getUnusedPixels() {
        int i = 0;
        while (i < saved.size()) {
            WeakReference<Pixels> ref = saved.get(i);
            Pixels p = ref.get();
            if (p == null) {
                saved.remove(i);
                continue;
            }
            if (p != beingConsumed && p != enqueued) {
                assert(p.getWidthUnsafe() == pixelW &&
                       p.getHeightUnsafe() == pixelH &&
                       p.getScaleUnsafe() == pixelScale);
                return p;
            }
            i++;
        }
        return null;
    }

    /**
     * Place the indicated {@code Pixels} object into the enqueued state,
     * replacing any other objects that are currently enqueued but not yet
     * being used by the consumer, and register the object for later
     * reuse.
     * 
     * @param pixels the {@code Pixels} object to be enqueued (and saved for reuse)
     */
    public synchronized void enqueuePixels(Pixels pixels) {
        if (pixels.getWidthUnsafe() != pixelW ||
            pixels.getHeightUnsafe() != pixelH ||
            pixels.getScaleUnsafe() != pixelScale)
        {
            throw new IllegalArgumentException("Pixels object: "+pixels+
                                               "does not match validated parameters: "+
                                               pixelW+" x "+pixelH+" @ "+pixelScale+"x");
        }
        enqueued = pixels;
        // Now make sure it is in our saved array since this could be
        // the first time we have seen this particular storage object.
        int i = 0;
        while (i < saved.size()) {
            WeakReference<Pixels> ref = saved.get(i);
            Pixels p = ref.get();
            if (p == null) {
                saved.remove(i);
                continue;
            }
            if (p == pixels) {
                // Found it - already known.
                return;
            }
            i++;
        }
        // Did not find it, this must be a new storage object.
        if (saved.size() >= 3) {
            throw new InternalError("too many Pixels objects saved");
        }
        saved.add(new WeakReference<Pixels>(pixels));
    }
}
