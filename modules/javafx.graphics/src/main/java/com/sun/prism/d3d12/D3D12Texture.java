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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.d3d12.ni.D3D12NativeTexture;

class D3D12Texture extends BaseTexture<D3D12Resource<D3D12TextureData>> {
    protected D3D12Context mContext;

    protected D3D12Texture(D3D12Resource<D3D12TextureData> resource, D3D12Context context, PixelFormat format, WrapMode wrapMode,
                           int physWidth, int physHeight, int contentWidth, int contentHeight) {
        // NOTE: D3D provides a constructor with contentX and contentY parameters, however I did not find
        // any places that would use it that would provide non-zero contentX and contentY.
        super(resource, format, wrapMode, physWidth, physHeight,
              0, 0, contentWidth, contentHeight, false);
        mContext = context;
    }

    private D3D12Texture(D3D12Texture sharedTex, WrapMode altMode) {
        super(sharedTex, altMode, false);
        mContext = sharedTex.mContext;
    }

    public static D3D12Texture create(D3D12NativeTexture nativeTexture, D3D12Context context,
                                      PixelFormat format, WrapMode wrapMode,
                                      int contentWidth, int contentHeight) {
        return new D3D12Texture(new D3D12Resource<D3D12TextureData>(
                                    new D3D12TextureData(
                                        nativeTexture, null, nativeTexture.getWidth(), nativeTexture.getHeight(), format
                                    )
                                ),
                                context,
                                format,
                                wrapMode,
                                nativeTexture.getWidth(), nativeTexture.getHeight(),
                                contentWidth, contentHeight
                                );
    }

    D3D12NativeTexture getNativeTexture() {
        return resource.getResource().getNativeTexture();
    }

    @Override
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy, int srcw, int srch,
                       int srcscan, boolean skipFlush) {
        if (!resource.isValid()) {
            System.err.println("update cancelled, resource not valid");
            return;
        }

        checkUpdateParams(buffer, format, dstx, dsty, srcx, srcy, srcw, srch, srcscan);

        if (!skipFlush) {
            mContext.flushVertexBuffer();
        }

        int contentX = getContentX();
        int contentY = getContentY();
        int contentW = getContentWidth();
        int contentH = getContentHeight();
        int texWidth = getPhysicalWidth();
        int texHeight = getPhysicalHeight();
        boolean res = mContext.getDevice().updateTexture(getNativeTexture(),
                                                         buffer, format,
                                                         dstx, dsty,
                                                         srcx, srcy, srcw, srch, srcscan);
        if (!res) {
            new Exception(String.format("D3D12: Texture update failed. Stack trace:")).printStackTrace(System.err);
        }

        switch (getWrapMode()) {
            case CLAMP_TO_EDGE:
                break; // done by hardware and SamplerStates
            case REPEAT:
                break; // done by hardware and SamplerStates
            case CLAMP_TO_EDGE_SIMULATED: {
                boolean copyR = (contentW < texWidth  && dstx + srcw == contentW);
                boolean copyL = (contentH < texHeight && dsty + srch == contentH);
                // Repeat right edge, if it was modified
                if (copyR) {
                    mContext.getDevice().updateTexture(getNativeTexture(),
                                                       buffer, format,
                                                       contentX + contentW, contentY + dsty,
                                                       srcx + srcw - 1, srcy, 1, srch, srcscan);
                }
                // Repeat bottom edge, if it was modified
                if (copyL) {
                    mContext.getDevice().updateTexture(getNativeTexture(),
                                                       buffer, format,
                                                       contentX + dstx, contentY + contentH,
                                                       srcx, srcy + srch - 1, srcw, 1, srcscan);
                    // Repeat LR corner, if it was modified
                    if (copyR) {
                        mContext.getDevice().updateTexture(getNativeTexture(),
                                                           buffer, format,
                                                           contentX + contentW, contentY + contentH,
                                                           srcx + srcw - 1, srcy + srch - 1, 1, 1, srcscan);
                    }
                }
                break;
            }
            case REPEAT_SIMULATED: {
                boolean repeatL = (contentW < texWidth  && dstx == 0);
                boolean repeatT = (contentH < texHeight && dsty == 0);
                // Repeat left edge on right, if it was modified
                if (repeatL) {
                    mContext.getDevice().updateTexture(getNativeTexture(),
                                                       buffer, format,
                                                       contentX + contentW, contentY + dsty,
                                                       srcx, srcy, 1, srch, srcscan);
                }
                // Repeat top edge on bottom, if it was modified
                if (repeatT) {
                    mContext.getDevice().updateTexture(getNativeTexture(),
                                                       buffer, format,
                                                       contentX + dstx, contentY + contentH,
                                                       srcx, srcy, srcw, 1, srcscan);
                    // Repeat UL pixel at LR, if it was modified
                    if (repeatL) {
                        mContext.getDevice().updateTexture(getNativeTexture(),
                                                           buffer, format,
                                                           contentX + contentW, contentY + contentH,
                                                           srcx, srcy, 1, 1, srcscan);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void update(MediaFrame frame, boolean skipFlush) {
        if (!resource.isValid()) {
            System.err.println("update(MediaFrame) cancelled, resource not valid");
            return;
        }

        if (frame.getPixelFormat() == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("Unsupported format " + frame.getPixelFormat());
        }

        try (D3D12Utils.AutoReleasableMediaFrame mf =
                new D3D12Utils.AutoReleasableMediaFrame(frame)) {
            ByteBuffer pixels = mf.get().getBufferForPlane(0);
            PixelFormat format = mf.get().getPixelFormat();

            if (!skipFlush) {
                mContext.flushVertexBuffer();
            }

            boolean res;

            // NOTE: checkUpdateParams is not feasible here because video params might slightly differ
            if (format.getDataType() == PixelFormat.DataType.INT) {
                res = mContext.getDevice().updateTexture(getNativeTexture(),
                                                        pixels.asIntBuffer(), format,
                                                        0, 0, 0, 0,
                                                        mf.get().getEncodedWidth(), mf.get().getEncodedHeight(),
                                                        mf.get().strideForPlane(0));
            } else {
                res = mContext.getDevice().updateTexture(getNativeTexture(),
                                                        pixels, format,
                                                        0, 0, 0, 0,
                                                        mf.get().getEncodedWidth(), mf.get().getEncodedHeight(),
                                                        mf.get().strideForPlane(0));
            }

            if (!res) {
                new Exception(String.format("D3D12: Texture update failed. Stack trace:")).printStackTrace(System.err);
            }
        }
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return new D3D12Texture(this, newMode);
    }

}
