/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import com.sun.prism.MediaFrame;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class D3D12Utils {
    // Copied from D3DResourceFactory.java
    static ByteBuffer getShaderCodeBuffer(InputStream is) {
        if (is == null) {
           throw new RuntimeException("InputStream must be non-null");
        }
        int len = 4096;
        byte[] data = new byte[len];
        try (BufferedInputStream bis = new BufferedInputStream(is, len)) {
            int offset = 0;
            int readBytes = -1;
            while ((readBytes = bis.read(data, offset, len - offset)) != -1) {
                offset += readBytes;
                if (len - offset == 0) {
                    // grow the array
                    len *= 2;
                    // was
                    // data = Arrays.copyOf(data, len);
                    //
                    byte[] newdata = new byte[len];
                    System.arraycopy(data, 0, newdata, 0, data.length);
                    data = newdata;
                }
            }
            bis.close();
            // D3D12 expects both a pointer to shader's code and its size,
            // a direct ByteBuffer can handle that (I think?)
            ByteBuffer buf = ByteBuffer.allocateDirect(offset);
            buf.put(data, 0, offset);
            return buf;
        } catch (IOException e) {
            throw new RuntimeException("Error loading D3D shader object", e);
        }
    }

    static ByteBuffer getShaderCodeBuffer(String shaderResourcePath) {
        return getShaderCodeBuffer(D3D12Utils.class.getResourceAsStream(shaderResourcePath));
    }

    static class AutoReleasableMediaFrame implements AutoCloseable {
        private MediaFrame mediaFrame;

        public AutoReleasableMediaFrame(MediaFrame mf) {
            mediaFrame = mf;
            mediaFrame.holdFrame();
        }

        @Override
        public void close() {
            mediaFrame.releaseFrame();
        }

        public MediaFrame get() {
            return mediaFrame;
        }
    }
}
