/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.io.IOException;
import java.io.InputStream;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.runtime.async.AbstractRemoteResource;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.tk.PlatformImage;
import com.sun.prism.Image;
import com.sun.prism.impl.PrismSettings;

class PrismImageLoader2 implements com.sun.javafx.tk.ImageLoader {

    private Image[] images;
    private int[] delayTimes;
    private int width;
    private int height;
    private float pixelScale;
    private boolean error;
    private Exception exception;

    public PrismImageLoader2(String url, int width, int height,
                             boolean preserveRatio, float pixelScale,
                             boolean smooth)
    {
        loadAll(url, width, height, preserveRatio, pixelScale, smooth);
    }

    public PrismImageLoader2(InputStream stream, int width, int height,
                             boolean preserveRatio, boolean smooth)
    {
        loadAll(stream, width, height, preserveRatio, smooth);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrameCount() {
        if (images == null) {
            return 0;
        }
        return images.length;
    }

    public PlatformImage getFrame(int index) {
        if (images == null) {
            return null;
        }
        return images[index];
    }

    public int getFrameDelay(int index) {
        if (images == null) {
            return 0;
        }
        return delayTimes[index];
    }

    public boolean getError() {
        return error;
    }

    public Exception getException() {
        return exception;
    }

    private void loadAll(String url, int w, int h,
                         boolean preserveRatio, float pixelScale,
                         boolean smooth)
    {
        ImageLoadListener listener = new PrismLoadListener();
        try {
            ImageFrame[] imgFrames =
                ImageStorage.loadAll(url, listener, w, h, preserveRatio, pixelScale, smooth);
            convertAll(imgFrames);
        } catch (Exception e) {
            if (PrismSettings.verbose) {
                e.getCause().printStackTrace(System.err);
            }
            error = true;
            exception = e;
        }
    }

    private void loadAll(InputStream stream, int w, int h,
                         boolean preserveRatio, boolean smooth)
    {
        ImageLoadListener listener = new PrismLoadListener();
        try {
            ImageFrame[] imgFrames =
                ImageStorage.loadAll(stream, listener, w, h, preserveRatio, 1.0f, smooth);
            convertAll(imgFrames);
        } catch (Exception e) {
            if (PrismSettings.verbose) {
                e.getCause().printStackTrace(System.err);
            }
            error = true;
            exception = e;
        }
    }

    private void convertAll(ImageFrame[] imgFrames) {
        int numFrames = imgFrames.length;
        images = new Image[numFrames];
        delayTimes = new int[numFrames];
        for (int i = 0; i < numFrames; i++) {
            ImageFrame frame = imgFrames[i];
            images[i] = com.sun.prism.Image.convertImageFrame(frame);
            ImageMetadata metadata = frame.getMetadata();
            if (metadata != null) {
                Integer delay = metadata.delayTime;
                if (delay != null) {
                    delayTimes[i] = delay.intValue();
                }
            }
            if (i == 0) {
                width = frame.getWidth();
                height = frame.getHeight();
            }
        }
    }

    
    private class PrismLoadListener implements ImageLoadListener {
        public void imageLoadWarning(ImageLoader loader, String message) {
            error = true;
        }

        public void imageLoadProgress(ImageLoader loader,
                                      float percentageComplete)
        {
            // progress only matters when backgroundLoading=true, but
            // currently we are relying on AbstractRemoteResource for tracking
            // progress of the InputStream, so there's no need to implement
            // this for now; eventually though we might want to consider
            // moving away from AbstractRemoteResource and instead use
            // the built-in support for progress in the javafx-iio library...
        }

        public void imageLoadMetaData(ImageLoader loader, ImageMetadata metadata) {
            // We currently have no need to listen for ImageMetadata ready.
        }
    }

    static class AsyncImageLoader
        extends AbstractRemoteResource<PrismImageLoader2>
    {
        int width, height;
        boolean preserveRatio;
        boolean smooth;

        public AsyncImageLoader(
                AsyncOperationListener<PrismImageLoader2> listener,
                String url,
                int width, int height, boolean preserveRatio, boolean smooth)
        {
            super(url, listener);
            this.width = width;
            this.height = height;
            this.preserveRatio = preserveRatio;
            this.smooth = smooth;
        }

        protected PrismImageLoader2 processStream(InputStream stream) throws IOException {
            return new PrismImageLoader2(stream, width, height, preserveRatio, smooth);
        }
    }
}
