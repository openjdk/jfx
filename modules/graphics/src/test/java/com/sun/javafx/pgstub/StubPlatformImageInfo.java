/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

public final class StubPlatformImageInfo {
    private final int width;
    private final int height;
    private final int[] frameDelays;
    private final int loopCount;

    public StubPlatformImageInfo(final int width,
                                 final int height) {
        this(width, height, null, 0);
    }

    public StubPlatformImageInfo(final int width,
                                 final int height,
                                 final int[] frameDelays, final int loopCount) {
        this.width = width;
        this.height = height;
        this.frameDelays = frameDelays;
        this.loopCount = loopCount;
    }

    public int getFrameCount() {
        return (frameDelays != null) ? frameDelays.length : 1;
    }

    public int getFrameDelay(final int index) {
        return frameDelays[index];
    }

    int getLoopCount() {
        return loopCount;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
    
    public boolean contains(final int x, final int y) {
        final int i = (2 * x / width) & 1;
        final int j = (2 * y / height) & 1;

        return (i ^ j) == 0;
    }
}
