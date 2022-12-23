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

package com.sun.prism.d3d;

class D3DFrameStats {

    public int numTrianglesDrawn;
    public int numDrawCalls;
    public int numBufferLocks;
    public int numTextureLocks;
    public int numTextureTransferBytes;
    public int numSetTexture;
    public int numSetPixelShader;
    public int numRenderTargetSwitch;

    static int divr(int x, int d) {
        return (x + d / 2) / d;
    }

    public String toDebugString(int nFrames) {
        return "D3D Statistics per last " + nFrames + " frame(s) :\n"
                + "\tnumTrianglesDrawn=" + divr(numTrianglesDrawn, nFrames)
                + ", numDrawCalls=" + divr(numDrawCalls, nFrames)
                + ", numBufferLocks=" + divr(numBufferLocks, nFrames)
                + "\n\tnumTextureLocks=" + divr(numTextureLocks, nFrames)
                + ", numTextureTransferKBytes=" + divr(numTextureTransferBytes / 1024, nFrames)
                + "\n\tnumRenderTargetSwitch=" + divr(numRenderTargetSwitch, nFrames)
                + ", numSetTexture=" + divr(numSetTexture, nFrames)
                + ", numSetPixelShader=" + divr(numSetPixelShader, nFrames);
    }
}
