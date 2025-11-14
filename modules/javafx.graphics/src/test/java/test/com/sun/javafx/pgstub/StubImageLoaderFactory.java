/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.pgstub;

import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;

public final class StubImageLoaderFactory {
    private final Map<Object, StubPlatformImageInfo> imageInfos;

    private StubAsyncImageLoader lastAsyncLoader;

    private final ImageLoader ERROR_IMAGE_LOADER =
            new ImageLoader() {
                private final Exception exception =
                            new Exception("Loading failed");

                @Override
                public Exception getException() {
                    return exception;
                }

                @Override
                public int getFrameCount() {
                    throw new IllegalStateException();
                }

                @Override
                public PlatformImage getFrame(int i) {
                    throw new IllegalStateException();
                }

                @Override
                public int getFrameDelay(int i) {
                    throw new IllegalStateException();
                }

                @Override
                public int getLoopCount() {
                    throw new IllegalStateException();
                }

                @Override
                public double getWidth() {
                    throw new IllegalStateException();
                }

                @Override
                public double getHeight() {
                    throw new IllegalStateException();
                }
            };

    public StubImageLoaderFactory() {
        imageInfos = new HashMap<>();
    }

    public void reset() {
        imageInfos.clear();
        lastAsyncLoader = null;
    }

    public void registerImage(final Object source,
                              final StubPlatformImageInfo imageInfo) {
        imageInfos.put(source, imageInfo);
    }

    public StubAsyncImageLoader getLastAsyncImageLoader() {
        return lastAsyncLoader;
    }

    public ImageLoader createImageLoader(final Object source,
                                         final double loadWidth,
                                         final double loadHeight,
                                         final boolean preserveRatio,
                                         final boolean smooth) {
        final StubPlatformImageInfo imageInfo = imageInfos.get(source);
        if (imageInfo == null) {
            return ERROR_IMAGE_LOADER;
        }

        return new StubImageLoader(source, imageInfo, loadWidth, loadHeight,
                                   preserveRatio, smooth);
    }

    public AsyncOperation createAsyncImageLoader(
            final AsyncOperationListener<ImageLoader> listener,
            final Object url, final double loadWidth, final double loadHeight,
            final boolean preserveRatio, final boolean smooth) {
        final ImageLoader imageLoader =
                createImageLoader(url, loadWidth, loadHeight,
                                  preserveRatio, smooth);
        final StubAsyncImageLoader asyncLoader =
                new StubAsyncImageLoader(imageLoader, listener);

        lastAsyncLoader = asyncLoader;
        return asyncLoader;
    }
}
