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

package com.sun.javafx.sg;

/**
 */
public interface PGMediaView extends PGNode {
    // This is a total and complete hack. Media integration with prism
    // needs to be revamped.
    public void renderNextFrame();
    public void setMediaProvider(Object provider);
    public void setX(float x);
    public void setY(float y);
    public void setViewport(float fitWidth, float fitHeight,
                            float vx, float vy, float vw, float vh,
                            boolean preserveRatio);

    public void setSmooth(boolean smooth);
    
    /*
     * Defaults to null to disable metrics. Reset to null to disable when not
     * needed.
     */
    public void setFrameTracker(MediaFrameTracker t);
    
    public static interface MediaFrameTracker {
        public void incrementDecodedFrameCount(int count);
        public void incrementRenderedFrameCount(int count);
    }
}
