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

package com.sun.prism.d3d12.ni;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.CompositeMode;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;

public final class D3D12NativeDevice extends D3D12NativeObject {
    D3D12NativeDevice(long ptr) {
        super(ptr);
    }

    @Override protected native void nReleaseNativeObject(long ptr);
    private native boolean nIsDeviceReady(long ptr);
    private native boolean nCheckFormatSupport(long ptr, int dxgiFormat);
    private native long nCreateMesh(long ptr);
    private native long nCreateMeshView(long ptr, long meshPtr);
    private native long nCreatePhongMaterial(long ptr);
    private native long nCreateRenderTarget(long ptr, long texturePtr, boolean enableDirtyBBox);
    private native long nCreateShader(long ptr, String name, ByteBuffer code);
    private native long nCreateTexture(long ptr, int width, int height, int format, int usage, int wrapMode, int samples, boolean useMipmap, boolean isRTT);
    private native int nGetMaximumMSAASampleSize(long ptr, int format);
    private native int nGetMaximumTextureSize(long ptr);
    private native void nClear(long ptr, float r, float g, float b, float a, boolean clearDepth);
    private native void nRenderMeshView(long ptr, long meshViewPtr);
    private native void nRenderQuads(long ptr, float[] vertices, byte[] colors, int elementCount);
    private native void nSetCompositeMode(long ptr, int compositeMode);
    private native void nSetPixelShader(long ptr, long pixelShaderPtr);
    private native void nSetRenderTarget(long ptr, long renderTargetPtr, boolean enableDepthTest);
    private native void nSetScissor(long ptr, boolean enabled, int x1, int y1, int x2, int y2);
    private native boolean nSetShaderConstantsF(long ptr, long shaderPtr, String name, FloatBuffer buf, int off, int count);
    private native boolean nSetShaderConstantsI(long ptr, long shaderPtr, String name, IntBuffer buf, int off, int count);
    private native void nSetTexture(long ptr, int unit, long texturePtr);
    private native void nSetCameraPos(long ptr, double x, double y, double z);
    private native void nSetViewProjTransform(long ptr,
        double m00, double m01, double m02, double m03,
        double m10, double m11, double m12, double m13,
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33);
    private native void nSetWorldTransform(long ptr,
        double m00, double m01, double m02, double m03,
        double m10, double m11, double m12, double m13,
        double m20, double m21, double m22, double m23,
        double m30, double m31, double m32, double m33);
    private native boolean nBlit(long ptr, long srcRT, int srcX0, int srcY0, int srcX1, int srcY1,
                                           long dstRT, int dstX0, int dstY0, int dstX1, int dstY1);
    private native boolean nBlitToSwapChain(long ptr, long srcRT, int srcX0, int srcY0, int srcX1, int srcY1,
                                                      long dstSwapChain, int dstX0, int dstY0, int dstX1, int dstY1);
    private native boolean nReadTextureB(long ptr, long srcTexturePtr,
                                         ByteBuffer buf, byte[] array,
                                         int x, int y, int w, int h);
    private native boolean nReadTextureI(long ptr, long srcTexturePtr,
                                         IntBuffer buf, int[] array,
                                         int x, int y, int w, int h);
    private native boolean nUpdateTextureF(long ptr, long texturePtr,
                                           FloatBuffer buf, float[] array, int pixelFormat,
                                           int dstx, int dsty,
                                           int srcx, int srcy,
                                           int srcw, int srch,
                                           int srcscan);
    private native boolean nUpdateTextureI(long ptr, long texturePtr,
                                           IntBuffer buf, int[] array, int pixelFormat,
                                           int dstx, int dsty,
                                           int srcx, int srcy,
                                           int srcw, int srch,
                                           int srcscan);
    private native boolean nUpdateTextureB(long ptr, long texturePtr,
                                           ByteBuffer buf, byte[] array, int pixelFormat,
                                           int dstx, int dsty,
                                           int srcx, int srcy,
                                           int srcw, int srch,
                                           int srcscan);
    private native boolean nPrepareSwapChain(long ptr, long swapChainPtr, long left, long top, long right, long bottom);
    private native boolean nPresent(long ptr, long swapChainPtr);
    private native void nFinishFrame(long ptr);
    private native boolean nWaitUntilIdle(long ptr);

    public boolean isDeviceReady() {
        return nIsDeviceReady(ptr);
    }

    public boolean checkFormatSupport(PixelFormat format) {
        return nCheckFormatSupport(ptr, DXGIFormat.fromPixelFormat(format).format);
    }

    public D3D12NativeMesh createMesh() {
        return new D3D12NativeMesh(nCreateMesh(ptr));
    }

    public D3D12NativeMeshView createMeshView(D3D12NativeMesh mesh) {
        return new D3D12NativeMeshView(nCreateMeshView(ptr, mesh.getPtr()));
    }

    public D3D12NativePhongMaterial createPhongMaterial() {
        return new D3D12NativePhongMaterial(nCreatePhongMaterial(ptr));
    }

    public D3D12NativeRenderTarget createRenderTarget(D3D12NativeTexture texture, boolean enableDirtyBBox) {
        return new D3D12NativeRenderTarget(nCreateRenderTarget(ptr, texture.getPtr(), enableDirtyBBox));
    }

    public D3D12NativeShader createShader(String name, ByteBuffer code) {
        return new D3D12NativeShader(nCreateShader(ptr, name, code));
    }

    public D3D12NativeTexture createTexture(int width, int height, PixelFormat format, Usage usage,
                                            WrapMode wrapMode, int samples, boolean useMipmap, boolean isRTT) {
        return new D3D12NativeTexture(nCreateTexture(
            ptr, width, height, DXGIFormat.fromPixelFormat(format).format, usage.ordinal(),
            wrapMode.ordinal(), samples, useMipmap, isRTT
        ));
    }

    public void clear(float r, float g, float b, float a, boolean clearDepth) {
        nClear(ptr, r, g, b, a, clearDepth);
    }

    public int getMaximumMSAASampleSize(PixelFormat format) {
        return nGetMaximumMSAASampleSize(ptr, DXGIFormat.fromPixelFormat(format).format);
    }

    public int getMaximumTextureSize() {
        return nGetMaximumTextureSize(ptr);
    }

    public void renderMeshView(D3D12NativeMeshView meshView) {
        nRenderMeshView(ptr, meshView.getPtr());
    }

    public void renderQuads(float[] vertices, byte[] colors, int elementCount) {
        nRenderQuads(ptr, vertices, colors, elementCount);
    }

    public void setCompositeMode(CompositeMode mode) {
        nSetCompositeMode(ptr, mode.ordinal());
    }

    public void setPixelShader(D3D12NativeShader pixelShader) {
        if (pixelShader == null) nSetPixelShader(ptr, 0);
        else nSetPixelShader(ptr, pixelShader.getPtr());
    }

    public void setRenderTarget(D3D12NativeRenderTarget renderTarget, boolean enableDepthTest) {
        nSetRenderTarget(ptr, renderTarget.getPtr(), enableDepthTest);
    }

    public void setScissor(boolean enabled, int x1, int y1, int x2, int y2) {
        nSetScissor(ptr, enabled, x1, y1, x2, y2);
    }

    public boolean setShaderConstants(D3D12NativeShader shader, String name, FloatBuffer buf, int off, int count) {
        return nSetShaderConstantsF(ptr, shader.getPtr(), name, buf, off, count);
    }

    public boolean setShaderConstants(D3D12NativeShader shader, String name, IntBuffer buf, int off, int count) {
        return nSetShaderConstantsI(ptr, shader.getPtr(), name, buf, off, count);
    }

    public void setTexture(int unit, D3D12NativeTexture texture) {
        nSetTexture(ptr, unit, texture != null ? texture.getPtr() : 0);
    }

    public void setCameraPos(Vec3d pos) {
        nSetCameraPos(ptr, pos.x, pos.y, pos.z);
    }

    public void setViewProjTransform(GeneralTransform3D tx) {
        nSetViewProjTransform(ptr,
            tx.get(0),  tx.get(1),  tx.get(2),  tx.get(3),
            tx.get(4),  tx.get(5),  tx.get(6),  tx.get(7),
            tx.get(8),  tx.get(9),  tx.get(10), tx.get(11),
            tx.get(12), tx.get(13), tx.get(14), tx.get(15));
    }

    public void setWorldTransform(GeneralTransform3D tx) {
        nSetWorldTransform(ptr,
            tx.get(0),  tx.get(1),  tx.get(2),  tx.get(3),
            tx.get(4),  tx.get(5),  tx.get(6),  tx.get(7),
            tx.get(8),  tx.get(9),  tx.get(10), tx.get(11),
            tx.get(12), tx.get(13), tx.get(14), tx.get(15));
    }

    public void setWorldTransform(BaseTransform tx) {
        nSetWorldTransform(ptr,
            tx.getMxx(), tx.getMxy(), tx.getMxz(), tx.getMxt(),
            tx.getMyx(), tx.getMyy(), tx.getMyz(), tx.getMyt(),
            tx.getMzx(), tx.getMzy(), tx.getMzz(), tx.getMzt(),
                    0.0,         0.0,         0.0,         1.0);
    }

    public boolean blit(D3D12NativeRenderTarget srcRT, int srcX0, int srcY0, int srcX1, int srcY1,
                        D3D12NativeRenderTarget dstRT, int dstX0, int dstY0, int dstX1, int dstY1) {
        return nBlit(ptr, srcRT.getPtr(), srcX0, srcY0, srcX1, srcY1,
                          dstRT.getPtr(), dstX0, dstY0, dstX1, dstY1);
    }

    public boolean blit(D3D12NativeRenderTarget srcRT, int srcX0, int srcY0, int srcX1, int srcY1,
                        D3D12NativeSwapChain dstSwapChain, int dstX0, int dstY0, int dstX1, int dstY1) {
        return nBlitToSwapChain(ptr, srcRT.getPtr(), srcX0, srcY0, srcX1, srcY1,
                                     dstSwapChain.getPtr(), dstX0, dstY0, dstX1, dstY1);
    }

    public boolean readTexture(D3D12NativeTexture tex, Buffer buffer, int x, int y, int width, int height) {
        if (buffer instanceof ByteBuffer) {
            ByteBuffer byteBuf = (ByteBuffer)buffer;
            byte[] arr = byteBuf.hasArray() ? byteBuf.array() : null;
            return nReadTextureB(ptr, tex.ptr, byteBuf, arr, x, y, width, height);
        } else if (buffer instanceof IntBuffer) {
            IntBuffer intBuf = (IntBuffer)buffer;
            int[] arr = intBuf.hasArray() ? intBuf.array() : null;
            return nReadTextureI(ptr, tex.ptr, intBuf, arr, x, y, width, height);
        } else {
            throw new IllegalArgumentException("Buffer of this type is not supported: " + buffer);
        }
    }

    public boolean updateTexture(D3D12NativeTexture texture,
                                 Buffer buf, PixelFormat format,
                                 int dstx, int dsty,
                                 int srcx, int srcy,
                                 int srcw, int srch,
                                 int srcscan) {
        if (format.getDataType() == PixelFormat.DataType.INT && buf instanceof IntBuffer) {
            IntBuffer intBuf = (IntBuffer)buf;
            int[] arr = intBuf.hasArray() ? intBuf.array() : null;
            return nUpdateTextureI(ptr, texture.getPtr(), intBuf, arr, format.ordinal(),
                                   dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } else if (format.getDataType() == PixelFormat.DataType.FLOAT && buf instanceof FloatBuffer) {
            FloatBuffer floatBuf = (FloatBuffer)buf;
            float[] arr = floatBuf.hasArray() ? floatBuf.array() : null;
            return nUpdateTextureF(ptr, texture.getPtr(), floatBuf, arr, format.ordinal(),
                                dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } else if (buf instanceof ByteBuffer) {
            ByteBuffer byteBuf = (ByteBuffer)buf;
            byteBuf.rewind();
            byte[] arr = byteBuf.hasArray() ? byteBuf.array() : null;
            return nUpdateTextureB(ptr, texture.getPtr(), byteBuf, arr, format.ordinal(),
                                   dstx, dsty, srcx, srcy, srcw, srch, srcscan);
        } else {
            throw new IllegalArgumentException("Buffer of this type is not supported: " + buf);
        }
    }

    public boolean prepareSwapChain(D3D12NativeSwapChain swapChain, long left, long top, long right, long bottom) {
        return nPrepareSwapChain(ptr, swapChain.ptr, left, top, right, bottom);
    }

    public boolean present(D3D12NativeSwapChain swapChain) {
        return nPresent(ptr, swapChain.ptr);
    }

    public void finishFrame() {
        nFinishFrame(ptr);
    }

    public boolean waitUntilIdle() {
        return nWaitUntilIdle(ptr);
    }
}
