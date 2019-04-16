/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Provides access to the {@link Framebuffer} class by making its
 * package-private methods public for test cases in
 * {@link test.com.sun.glass.ui.monocle.FramebufferY8Test FramebufferY8Test}.
 */
public class FramebufferY8SuperShim extends Framebuffer {

    /**
     * Creates a new {@code FramebufferY8SuperShim}.
     *
     * @param bb the 32-bit composition buffer
     * @param width the width of the composition buffer in pixels
     * @param height the height of the composition buffer in pixels
     * @param depth the color depth of the target channel or buffer in bits per
     * pixel
     * @param clear {@code true} to clear the composition buffer on the first
     * upload of each frame unless that upload already overwrites the entire
     * buffer; otherwise {@code false}
     */
    public FramebufferY8SuperShim(ByteBuffer bb, int width, int height, int depth, boolean clear) {
        super(bb, width, height, depth, clear);
    }

    @Override
    public void write(WritableByteChannel out) throws IOException {
        super.write(out);
    }

    @Override
    public void copyToBuffer(ByteBuffer out) {
        super.copyToBuffer(out);
    }
}
