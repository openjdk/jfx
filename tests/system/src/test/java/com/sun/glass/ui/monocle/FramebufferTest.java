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

package com.sun.glass.ui.monocle;

import org.junit.Test;

import java.nio.ByteBuffer;

public class FramebufferTest {

    @Test
    public void testOverflow32() {
        ByteBuffer screenBuffer = ByteBuffer.allocate(100 * 100 * 4);
        Framebuffer fb = new Framebuffer(screenBuffer, 100, 100, 4, false);
        ByteBuffer windowBuffer = ByteBuffer.allocate(200 * 200 * 4);
        fb.reset();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 1f);
        windowBuffer.clear();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 1f);
        windowBuffer.clear();
        fb.reset();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 0.5f);
        windowBuffer.clear();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 0.5f);
        windowBuffer.clear();
    }

    @Test
    public void testOverflow16() {
        // A 16-bit framebuffer stores its content in 32-bits and writes out
        // to a 32-bit target. So it needs four bytes per pixel in its buffer.
        ByteBuffer screenBuffer = ByteBuffer.allocate(100 * 100 * 4);
        Framebuffer fb = new Framebuffer(screenBuffer, 100, 100, 2, false);
        ByteBuffer windowBuffer = ByteBuffer.allocate(200 * 200 * 4);
        fb.reset();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 1f);
        windowBuffer.clear();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 1f);
        windowBuffer.clear();
        fb.reset();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 0.5f);
        windowBuffer.clear();
        fb.composePixels(windowBuffer, -50, -50, 200, 200, 0.5f);
        windowBuffer.clear();
    }

}
