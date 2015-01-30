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

package com.sun.javafx.pgstub;

import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;

public final class StubImageLoader implements ImageLoader {
    private final Object source;

    private final StubPlatformImageInfo imageInfo;
    private final int loadWidth;
    private final int loadHeight;
    private final boolean preserveRatio;
    private final boolean smooth;

    private final PlatformImage[] frames;

    public StubImageLoader(final Object source,
                           final StubPlatformImageInfo imageInfo,
                           final int loadWidth,
                           final int loadHeight,
                           final boolean preserveRatio,
                           final boolean smooth) {
        this.source = source;
        
        this.imageInfo = imageInfo;
        this.loadWidth = loadWidth;
        this.loadHeight = loadHeight;
        this.preserveRatio = preserveRatio;
        this.smooth = smooth;

        frames = new PlatformImage[imageInfo.getFrameCount()];
        for (int i = 0; i < frames.length; ++i) {
            frames[i] = source instanceof PlatformImage ? (PlatformImage) source : new StubPlatformImage(this, i);
        }
    }

    public Object getSource() {
        return source;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public int getFrameCount() {
        return frames.length;
    }

    @Override
    public PlatformImage getFrame(final int i) {
        return frames[i];
    }

    @Override
    public int getFrameDelay(final int i) {
        return imageInfo.getFrameDelay(i);
    }

    @Override
    public int getLoopCount() {
        return imageInfo.getLoopCount();
    }

    @Override
    public int getWidth() {
        return imageInfo.getWidth();
    }

    @Override
    public int getHeight() {
        return imageInfo.getHeight();
    }

    public StubPlatformImageInfo getImageInfo() {
        return imageInfo;
    }

    public int getLoadHeight() {
        return loadHeight;
    }

    public int getLoadWidth() {
        return loadWidth;
    }

    public boolean getPreserveRatio() {
        return preserveRatio;
    }

    public boolean getSmooth() {
        return smooth;
    }
}
