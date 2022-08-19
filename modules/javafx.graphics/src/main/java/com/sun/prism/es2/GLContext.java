/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.sun.javafx.PlatformUtil;
import com.sun.prism.MeshView;
import com.sun.prism.PhongMaterial.MapType;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;

abstract class GLContext {

    // Use by glBlendFunc
    final static int GL_ZERO                      = 0;
    final static int GL_ONE                       = 1;
    final static int GL_SRC_COLOR                 = 2;
    final static int GL_ONE_MINUS_SRC_COLOR       = 3;
    final static int GL_DST_COLOR                 = 4;
    final static int GL_ONE_MINUS_DST_COLOR       = 5;
    final static int GL_SRC_ALPHA                 = 6;
    final static int GL_ONE_MINUS_SRC_ALPHA       = 7;
    final static int GL_DST_ALPHA                 = 8;
    final static int GL_ONE_MINUS_DST_ALPHA       = 9;
    final static int GL_CONSTANT_COLOR            = 10;
    final static int GL_ONE_MINUS_CONSTANT_COLOR  = 11;
    final static int GL_CONSTANT_ALPHA            = 12;
    final static int GL_ONE_MINUS_CONSTANT_ALPHA  = 13;
    final static int GL_SRC_ALPHA_SATURATE        = 14;

    // Texture Type
    final static int GL_FLOAT                     = 20;
    final static int GL_UNSIGNED_BYTE             = 21;
    final static int GL_UNSIGNED_INT_8_8_8_8_REV  = 22;
    final static int GL_UNSIGNED_INT_8_8_8_8      = 23;
    final static int GL_UNSIGNED_SHORT_8_8_APPLE  = 24;

    // Use by Texture: Pixel Format
    final static int GL_RGBA                      = 40;
    final static int GL_BGRA                      = 41;
    final static int GL_RGB                       = 42;
    final static int GL_LUMINANCE                 = 43;
    final static int GL_ALPHA                     = 44;
    final static int GL_RGBA32F                   = 45;
    final static int GL_YCBCR_422_APPLE           = 46;

    // Use by Texture
    final static int GL_TEXTURE_2D                = 50;
    final static int GL_TEXTURE_BINDING_2D        = 51;
    final static int GL_NEAREST                   = 52;
    final static int GL_LINEAR                    = 53;
    final static int GL_NEAREST_MIPMAP_NEAREST    = 54;
    final static int GL_LINEAR_MIPMAP_LINEAR      = 55;


    // Use by glPixelStorei
    final static int GL_UNPACK_ALIGNMENT          = 60;
    final static int GL_UNPACK_ROW_LENGTH         = 61;
    final static int GL_UNPACK_SKIP_PIXELS        = 62;
    final static int GL_UNPACK_SKIP_ROWS          = 63;

    // Use by WrapState
    final static int WRAPMODE_REPEAT              = 100;
    final static int WRAPMODE_CLAMP_TO_EDGE       = 101;
    final static int WRAPMODE_CLAMP_TO_BORDER     = 102;

    // Use by face culling for 3D implementation
    final static int GL_BACK                      = 110;
    final static int GL_FRONT                     = 111;
    final static int GL_NONE                      = 112;

    // Use for querying hardware/implementation limits
    final static int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS  = 120;
    final static int GL_MAX_FRAGMENT_UNIFORM_VECTORS     = 121;
    final static int GL_MAX_TEXTURE_IMAGE_UNITS          = 122;
    final static int GL_MAX_TEXTURE_SIZE                 = 123;
    final static int GL_MAX_VERTEX_ATTRIBS               = 124;
    final static int GL_MAX_VARYING_COMPONENTS           = 125;
    final static int GL_MAX_VARYING_VECTORS              = 126;
    final static int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS   = 127;
    final static int GL_MAX_VERTEX_UNIFORM_COMPONENTS    = 128;
    final static int GL_MAX_VERTEX_UNIFORM_VECTORS       = 129;

    final static int MAPTYPE_DIFFUSE = MapType.DIFFUSE.ordinal();
    final static int MAPTYPE_SPECULAR = MapType.SPECULAR.ordinal();
    final static int MAPTYPE_BUMP = MapType.BUMP.ordinal();
    final static int MAPTYPE_SELFILLUM = MapType.SELF_ILLUM.ordinal();

    // Use by Uniform Matrix
    final static int NUM_MATRIX_ELEMENTS          = 16;

    long nativeCtxInfo;
    private int maxTextureSize = -1;
    private Boolean nonPowTwoExtAvailable;
    private Boolean clampToZeroAvailable;

    // TODO : Consider moving these cached values to ES2Context.
    // track some other state here to avoid redundant state changes
    private int activeTexUnit;
    private int[] boundTextures = new int[4];
    private int viewportX, viewportY, viewportWidth, viewportHeight;
    // depthTest is initialized to false in the native initState method
    private boolean depthTest = false;
    private boolean msaa = false;
    private int maxSampleSize = -1;

    private static final int FBO_ID_UNSET = -1;
    private static final int FBO_ID_NOCACHE = -2;
    private int nativeFBOID = PlatformUtil.isMac() || PlatformUtil.isIOS() ? FBO_ID_NOCACHE : FBO_ID_UNSET;

    private static native void nActiveTexture(long nativeCtxInfo, int texUnit);
    private static native void nBindFBO(long nativeCtxInfo, int nativeFBOID);
    private static native void nBindTexture(long nativeCtxInfo, int texID);
    private static native void nBlendFunc(int sFactor, int dFactor);
    private static native void nClearBuffers(long nativeCtxInfo,
            float red, float green, float blue, float alpha,
            boolean clearColor, boolean clearDepth, boolean ignoreScissor);
    private static native int nCompileShader(long nativeCtxInfo, String src,
            boolean vertex);
    private static native int nCreateDepthBuffer(long nativeCtxInfo, int width,
            int height, int msaa);
    private static native int nCreateRenderBuffer(long nativeCtxInfo, int width,
            int height, int msaa);
    private static native int nCreateFBO(long nativeCtxInfo, int texID);
    private static native int nCreateProgram(long nativeCtxInfo,
            int vertexShaderID, int[] fragmentShaderID,
            int numAttrs, String[] attrs, int[] indexs);
    private static native int nCreateTexture(long nativeCtxInfo, int width,
            int height);
    private static native void nDeleteRenderBuffer(long nativeCtxInfo, int rbID);
    private static native void nDeleteFBO(long nativeCtxInfo, int fboID);
    private static native void nDeleteShader(long nativeCtxInfo, int shadeID);
    private static native void nDeleteTexture(long nativeCtxInfo, int tID);
    private static native void nDisposeShaders(long nativeCtxInfo,
            int pID, int vID, int[] fID);
    private static native void nFinish();
    private static native int nGenAndBindTexture();
    private static native int nGetFBO();
    private static native int nGetIntParam(int pname);
    private static native int nGetMaxSampleSize();
    private static native int nGetUniformLocation(long nativeCtxInfo,
            int programID, String name);
    private static native void nPixelStorei(int pname, int param);
    private static native boolean nReadPixelsByte(long nativeCtxInfo, int length,
            Buffer buffer, byte[] pixelArr, int x, int y, int w, int h);
    private static native boolean nReadPixelsInt(long nativeCtxInfo, int length,
            Buffer buffer, int[] pixelArr, int x, int y, int w, int h);
    private static native void nScissorTest(long nativeCtxInfo, boolean enable,
            int x, int y, int w, int h);
    private static native void nSetDepthTest(long nativeCtxInfo, boolean depthTest);
    private static native void nSetMSAA(long nativeCtxInfo, boolean msaa);
    private static native void nTexParamsMinMax(int min, int max);
    private static native boolean nTexImage2D0(int target, int level, int internalFormat,
            int width, int height, int border, int format,
            int type, Object pixels, int pixelsByteOffset, boolean useMipmap);
    private static native boolean nTexImage2D1(int target, int level, int internalFormat,
            int width, int height, int border, int format,
            int type, Object pixels, int pixelsByteOffset, boolean useMipmap);
    private static native void nTexSubImage2D0(int target, int level,
            int xoffset, int yoffset, int width, int height, int format,
            int type, Object pixels, int pixelsByteOffset);
    private static native void nTexSubImage2D1(int target, int level,
            int xoffset, int yoffset, int width, int height, int format,
            int type, Object pixels, int pixelsByteOffset);
    private static native void nUpdateViewport(long nativeCtxInfo, int x, int y,
            int w, int h);
    private static native void nUniform1f(long nativeCtxInfo, int location, float v0);
    private static native void nUniform2f(long nativeCtxInfo, int location, float v0,
            float v1);
    private static native void nUniform3f(long nativeCtxInfo, int location, float v0,
            float v1, float v2);
    private static native void nUniform4f(long nativeCtxInfo, int location, float v0,
            float v1, float v2, float v3);
    private static native void nUniform4fv0(long nativeCtxInfo, int location, int count,
            Object value, int valueByteOffset);
    private static native void nUniform4fv1(long nativeCtxInfo, int location, int count,
            Object value, int valueByteOffset);
    private static native void nUniform1i(long nativeCtxInfo, int location, int v0);
    private static native void nUniform2i(long nativeCtxInfo, int location, int v0,
            int v1);
    private static native void nUniform3i(long nativeCtxInfo, int location, int v0,
            int v1, int v2);
    private static native void nUniform4i(long nativeCtxInfo, int location, int v0,
            int v1, int v2, int v3);
    private static native void nUniform4iv0(long nativeCtxInfo, int location, int count,
            Object value, int valueByteOffset);
    private static native void nUniform4iv1(long nativeCtxInfo, int location, int count,
            Object value, int valueByteOffset);
    private static native void nUniformMatrix4fv(long nativeCtxInfo, int location,
            boolean transpose, float values[]);
    private static native void nUpdateFilterState(long nativeCtxInfo, int texID,
            boolean linearFilter);
    private static native void nUpdateWrapState(long nativeCtxInfo, int texID,
            int wrapMode);
    private static native void nUseProgram(long nativeCtxInfo, int pID);

    private static native void nEnableVertexAttributes(long nativeCtxInfo);
    private static native void nDisableVertexAttributes(long nativeCtxInfo);
    private static native void nDrawIndexedQuads(long nativeCtxInfo, int numVertices,
            float dataf[], byte datab[]);
    private static native int nCreateIndexBuffer16(long nativeCtxInfo, short data[], int n);
    private static native void nSetIndexBuffer(long nativeCtxInfo, int buffer);

    private static native void nSetDeviceParametersFor2D(long nativeCtxInfo);
    private static native void nSetDeviceParametersFor3D(long nativeCtxInfo);
    private static native long nCreateES2Mesh(long nativeCtxInfo);
    private static native void nReleaseES2Mesh(long nativeCtxInfo, long nativeHandle);
    private static native boolean nBuildNativeGeometryShort(long nativeCtxInfo, long nativeHandle,
            float[] vertexBuffer, int vertexBufferLength, short[] indexBuffer, int indexBufferLength);
    private static native boolean nBuildNativeGeometryInt(long nativeCtxInfo, long nativeHandle,
            float[] vertexBuffer, int vertexBufferLength, int[] indexBuffer, int indexBufferLength);
    private static native long nCreateES2PhongMaterial(long nativeCtxInfo);
    private static native void nReleaseES2PhongMaterial(long nativeCtxInfo, long nativeHandle);
    private static native void nSetSolidColor(long nativeCtxInfo, long nativePhongMaterial,
            float r, float g, float b, float a);
    private static native void nSetMap(long nativeCtxInfo, long nativePhongMaterial,
            int mapType, int texID);
    private static native long nCreateES2MeshView(long nativeCtxInfo, long nativeMeshInfo);
    private static native void nReleaseES2MeshView(long nativeCtxInfo, long nativeHandle);
    private static native void nSetCullingMode(long nativeCtxInfo, long nativeMeshViewInfo,
            int cullingMode);
    private static native void nSetMaterial(long nativeCtxInfo, long nativeMeshViewInfo,
            long nativePhongMaterialInfo);
    private static native void nSetWireframe(long nativeCtxInfo, long nativeMeshViewInfo,
            boolean wireframe);
    private static native void nSetAmbientLight(long nativeCtxInfo, long nativeMeshViewInfo,
            float r, float g, float b);
    private static native void nSetLight(long nativeCtxInfo, long nativeMeshViewInfo,
            int index, float x, float y, float z, float r, float g, float b, float w, float ca, float la, float qa,
            float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff);
    private static native void nRenderMeshView(long nativeCtxInfo, long nativeMeshViewInfo);
    private static native void nBlit(long nativeCtxInfo, int srcFBO, int dstFBO,
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1);

    void activeTexture(int texUnit) {
        nActiveTexture(nativeCtxInfo, texUnit);
    }

    void bindFBO(int nativeFBOID) {
        switch (this.nativeFBOID) {
            case FBO_ID_UNSET:
                this.nativeFBOID = nativeFBOID;
                nBindFBO(nativeCtxInfo, nativeFBOID);
                break;
            case FBO_ID_NOCACHE:
                nBindFBO(nativeCtxInfo, nativeFBOID);
                break;
            default:
                if (this.nativeFBOID != nativeFBOID) {
                    nBindFBO(nativeCtxInfo, nativeFBOID);
                    this.nativeFBOID = nativeFBOID;
                }
                break;
        }
    }

    void bindTexture(int texID) {
        nBindTexture(nativeCtxInfo, texID);
    }

    void blendFunc(int sFactor, int dFactor) {
        nBlendFunc(sFactor, dFactor);
    }

    boolean canCreateNonPowTwoTextures() {
        if (nonPowTwoExtAvailable == null) {
            /* Note: Currently we are assuming a system with a single
             * or homogeneous GPUs. For the case of heterogeneous GPUs
             * system the string information will need to be per
             * GLContext class. */
            nonPowTwoExtAvailable = PrismSettings.forcePow2
                ? Boolean.FALSE : ES2Pipeline.glFactory.isNPOTSupported();
        }
        return nonPowTwoExtAvailable.booleanValue();
    }

    boolean canClampToZero() {
        if (clampToZeroAvailable == null) {
            /* Note: Currently we are assuming a system with a single
             * or homogeneous GPUs. For the case of heterogeneous GPUs
             * system the string information will need to be per
             * GLContext class. */
            clampToZeroAvailable = PrismSettings.noClampToZero
                ? Boolean.FALSE
                : ES2Pipeline.glFactory.isGL2();
        }
        return clampToZeroAvailable.booleanValue();
    }

    void clearBuffers(Color color, boolean clearColor,
            boolean clearDepth, boolean ignoreScissor) {
        float r = color.getRedPremult();
        float g = color.getGreenPremult();
        float b = color.getBluePremult();
        float a = color.getAlpha();
        nClearBuffers(nativeCtxInfo, r, g, b, a, clearColor, clearDepth,
                ignoreScissor);
    }

    /**
     * Compiles the given shader program.  If successful, this function returns
     * a handle to the newly created shader object; otherwise returns 0.
     */
    int compileShader(String shaderSource, boolean vertex) {
        return nCompileShader(nativeCtxInfo, shaderSource, vertex);
    }

    int createDepthBuffer(int width, int height, int msaaSamples) {
        return nCreateDepthBuffer(nativeCtxInfo, width, height, msaaSamples);
    }

    int createRenderBuffer(int width, int height, int msaaSamples) {
        return nCreateRenderBuffer(nativeCtxInfo, width, height, msaaSamples);
    }

    /**
     * Will create FBO by generate new FBO and binding it.
     * Note: Will not restore previously bound FBO.
     * @param texID if defined, will attach texture to generated FBO
     * @return FBO id
     */
    int createFBO(int texID) {
        if (nativeFBOID != FBO_ID_NOCACHE) {
            nativeFBOID = FBO_ID_UNSET; // invalidate FBO ID cache
        }
        return nCreateFBO(nativeCtxInfo, texID);
    }

    /**
     * Compiles and links a new shader program using the given shaders.  If
     * successful, this function returns a handle to the newly created shader
     * program; otherwise returns 0.
     */
    int createProgram(int vertexShaderID, int[] fragmentShaderIDArr,
            String[] attrs, int[] indexs) {

        if (fragmentShaderIDArr == null) {
            System.err.println("Error: fragmentShaderIDArr is null");
            return 0;
        }

        boolean shaderSpecified = true;
        for (int i = 0; i < fragmentShaderIDArr.length; i++) {
            if (fragmentShaderIDArr[i] == 0) {
                shaderSpecified = false;
                break;
            }
        }

        if (vertexShaderID == 0 || fragmentShaderIDArr.length == 0
                || !shaderSpecified) {
            System.err.println("Both vertexShader and fragmentShader(s) must be specified");
        }
        if (attrs.length != indexs.length) {
            System.err.println("attrs.length must be equal to index.length");
        }
        return nCreateProgram(nativeCtxInfo, vertexShaderID, fragmentShaderIDArr,
                attrs.length, attrs, indexs);
    }

    int createTexture(int width, int height) {
        return nCreateTexture(nativeCtxInfo, width, height);
    }

    void deleteRenderBuffer(int dbID) {
        nDeleteRenderBuffer(nativeCtxInfo, dbID);
    }

    void deleteFBO(int fboID) {
        nDeleteFBO(nativeCtxInfo, fboID);
    }

    void deleteShader(int shadeID) {
        nDeleteShader(nativeCtxInfo, shadeID);
    }

    void blitFBO(int msaaFboID, int dstFboID,
                 int srcX0, int srcY0, int srcX1, int srcY1,
                 int dstX0, int dstY0, int dstX1, int dstY1)
    {
        nBlit(nativeCtxInfo, msaaFboID, dstFboID,
              srcX0, srcY0, srcX1, srcY1,
              dstX0, dstY0, dstX1, dstY1);
    }

    void deleteTexture(int tID) {
        nDeleteTexture(nativeCtxInfo, tID);
    }

    void disposeShaders(int pID, int vID, int[] fID) {
        nDisposeShaders(nativeCtxInfo, pID, vID, fID);
    }

    void finish() {
        nFinish();
    }

    int genAndBindTexture() {
        int texID = nGenAndBindTexture();
        boundTextures[activeTexUnit] = texID;
        return texID;
    }

    int getBoundFBO() {
        switch (nativeFBOID) {
            case FBO_ID_UNSET:
                nativeFBOID = nGetFBO();
                return nativeFBOID;
            case FBO_ID_NOCACHE:
                return nGetFBO();
            default:
                return nativeFBOID;
        }
    }

    long getNativeCtxInfo() {
        return nativeCtxInfo;
    }

    abstract long getNativeHandle();

    /***************************************************/
    int getActiveTextureUnit() {
        return activeTexUnit;
    }

    // Forcibly sets the active texture unit to the given index.
    void setActiveTextureUnit(int unit) {
        activeTexture(unit);
        activeTexUnit = unit;
    }

    // Sets the active texture unit to the given index only if it was
    // not already the active index.
    void updateActiveTextureUnit(int unit) {
        if (unit != getActiveTextureUnit()) {
            setActiveTextureUnit(unit);
        }
    }

    int getBoundTexture() {
        return boundTextures[activeTexUnit];
    }

    int getBoundTexture(int unit) {
        return boundTextures[unit];
    }

    int getNumBoundTexture() {
        return boundTextures.length;
    }

    // Forcibly sets the currently bound texture to the given object.
    void setBoundTexture(int texid) {
        bindTexture(texid);
        boundTextures[activeTexUnit] = texid;
    }

    // Sets the currently bound texture to the given object only if it was
    // not already the current texture.
    void updateBoundTexture(int texid) {
        if (texid != getBoundTexture()) {
            setBoundTexture(texid);
        }
    }
    /***********************************************************/

    int getIntParam(int param) {
        return nGetIntParam(param);
    }

    int getSampleSize() {
        int maxSamples = getMaxSampleSize();
        return maxSamples < 2 ? 0 : (maxSamples < 4 ? 2 : 4);
    }

    int getMaxSampleSize() {
        if (maxSampleSize > -1) {
            return maxSampleSize;
        }
        maxSampleSize = ES2Pipeline.msaa ? nGetMaxSampleSize() : 0;
        return maxSampleSize;
    }

    int getMaxTextureSize() {
        if (maxTextureSize > -1) {
            return maxTextureSize;
        }
        return maxTextureSize = getIntParam(GLContext.GL_MAX_TEXTURE_SIZE);
    }

    int getUniformLocation(int programID, String name) {
        return nGetUniformLocation(nativeCtxInfo, programID, name);
    }

    boolean isShaderCompilerSupported() {
        // GL2
        return true;
        // else TODO: glGetBooleanv(GL_SHADER_COMPILER, supported, 0); (RT-27526)
    }

    abstract void makeCurrent(GLDrawable drawable);

    void pixelStorei(int pname, int param) {
        nPixelStorei(pname, param);
    }

    boolean readPixels(Buffer buffer, int x, int y, int w, int h) {
        boolean res = false;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer buf = (ByteBuffer) buffer;
            byte[] arr = buf.hasArray() ? buf.array() : null;
            int length = buf.capacity();
            res = nReadPixelsByte(nativeCtxInfo, length, buffer, arr, x, y, w, h);
        } else if (buffer instanceof IntBuffer) {
            IntBuffer buf = (IntBuffer) buffer;
            int[] arr = buf.hasArray() ? buf.array() : null;
            int length = buf.capacity() * 4;
            // Note: This implementation only handle little-endian architectures,
            // which includes all the platforms we need to support for JavaFX 2.2.
            // We will need to do extra byte swapping at pixel level if we ever needs
            // to support big-endian architectures such as Solaris Sparc.
            res = nReadPixelsInt(nativeCtxInfo, length, buffer, arr, x, y, w, h);
        } else {
            throw new IllegalArgumentException("readPixel: pixel's buffer type is not supported: "
                    + buffer);
        }
        return res;
    }

    void scissorTest(boolean enable, int x, int y, int w, int h) {
        nScissorTest(nativeCtxInfo, enable, x, y, w, h);
    }

    void setShaderProgram(int progid) {
        nUseProgram(nativeCtxInfo, progid);
    }

    void texParamsMinMax(int pname, boolean useMipmap) {
        int min = pname;
        int max = pname;
        if (useMipmap) {
            min = (min == GLContext.GL_LINEAR) ? GLContext.GL_LINEAR_MIPMAP_LINEAR
                    : GLContext.GL_NEAREST_MIPMAP_NEAREST;
        }
        nTexParamsMinMax(min, max);
    }

    boolean texImage2D(int target, int level, int internalFormat,
            int width, int height, int border, int format, int type,
            java.nio.Buffer pixels, boolean useMipmap) {
        boolean result;
        boolean direct = BufferFactory.isDirect(pixels);
        if (direct) {
            result = nTexImage2D0(target, level, internalFormat, width, height, border, format,
                    type, pixels, BufferFactory.getDirectBufferByteOffset(pixels), useMipmap);
        } else {
            result = nTexImage2D1(target, level, internalFormat, width, height, border, format,
                    type, BufferFactory.getArray(pixels),
                    BufferFactory.getIndirectBufferByteOffset(pixels), useMipmap);
        }
        return result;

    }

    void texSubImage2D(int target, int level, int xoffset, int yoffset,
            int width, int height, int format, int type, java.nio.Buffer pixels) {
        boolean direct = BufferFactory.isDirect(pixels);
        if (direct) {
            nTexSubImage2D0(target, level, xoffset, yoffset, width, height,
                    format, type, pixels,
                    BufferFactory.getDirectBufferByteOffset(pixels));
        } else {
            nTexSubImage2D1(target, level, xoffset, yoffset, width, height,
                    format, type, BufferFactory.getArray(pixels),
                    BufferFactory.getIndirectBufferByteOffset(pixels));
        }
    }

    void updateViewportAndDepthTest(int x, int y, int w, int h,
            boolean depthTest) {
        if (viewportX != x || viewportY != y || viewportWidth != w || viewportHeight != h) {
            viewportX = x;
            viewportY = y;
            viewportWidth = w;
            viewportHeight = h;
            nUpdateViewport(nativeCtxInfo, x, y, w, h);
        }
        if (this.depthTest != depthTest) {
            nSetDepthTest(nativeCtxInfo, depthTest);
            this.depthTest = depthTest;
        }
    }

    void updateMSAAState(boolean msaa) {
        if (this.msaa != msaa) {
            nSetMSAA(nativeCtxInfo, msaa);
            this.msaa = msaa;
        }
    }

    void updateFilterState(int texID, boolean linearFilter) {
        nUpdateFilterState(nativeCtxInfo, texID, linearFilter);
    }

    void updateWrapState(int texID, WrapMode wrapMode) {
        int wm;
        switch (wrapMode) {
            case REPEAT_SIMULATED:  // mode should not matter for this case
            case REPEAT:
                wm = WRAPMODE_REPEAT;
                break;
            case CLAMP_TO_ZERO_SIMULATED:
            case CLAMP_TO_EDGE_SIMULATED: // needed for top/left edge cases
            case CLAMP_TO_EDGE:
                wm = WRAPMODE_CLAMP_TO_EDGE;
                break;
            case CLAMP_TO_ZERO:
                wm = WRAPMODE_CLAMP_TO_BORDER;
                break;
            case CLAMP_NOT_NEEDED:
                return;
            default:
                throw new InternalError("Unrecognized wrap mode: "+wrapMode);
        }
        nUpdateWrapState(nativeCtxInfo, texID, wm);
    }

    void uniform1f(int location, float v0) {
        nUniform1f(nativeCtxInfo, location, v0);
    }

    void uniform2f(int location, float v0, float v1) {
        nUniform2f(nativeCtxInfo, location, v0, v1);
    }

    void uniform3f(int location, float v0, float v1, float v2) {
        nUniform3f(nativeCtxInfo, location, v0, v1, v2);
    }

    void uniform4f(int location, float v0, float v1, float v2, float v3) {
        nUniform4f(nativeCtxInfo, location, v0, v1, v2, v3);
    }

    void uniform4fv(int location, int count, java.nio.FloatBuffer value) {
        boolean direct = BufferFactory.isDirect(value);
        if (direct) {
            nUniform4fv0(nativeCtxInfo, location, count, value,
                    BufferFactory.getDirectBufferByteOffset(value));
        } else {
            nUniform4fv1(nativeCtxInfo, location, count, BufferFactory.getArray(value),
                    BufferFactory.getIndirectBufferByteOffset(value));
        }
    }

    void uniform1i(int location, int v0) {
        nUniform1i(nativeCtxInfo, location, v0);
    }

    void uniform2i(int location, int v0, int v1) {
        nUniform2i(nativeCtxInfo, location, v0, v1);
    }

    void uniform3i(int location, int v0, int v1, int v2) {
        nUniform3i(nativeCtxInfo, location, v0, v1, v2);
    }

    void uniform4i(int location, int v0, int v1, int v2, int v3) {
        nUniform4i(nativeCtxInfo, location, v0, v1, v2, v3);
    }

    void uniform4iv(int location, int count, java.nio.IntBuffer value) {
        boolean direct = BufferFactory.isDirect(value);
        if (direct) {
            nUniform4iv0(nativeCtxInfo, location, count, value,
                    BufferFactory.getDirectBufferByteOffset(value));
        } else {
            nUniform4iv1(nativeCtxInfo, location, count, BufferFactory.getArray(value),
                    BufferFactory.getIndirectBufferByteOffset(value));
        }
    }

    void uniformMatrix4fv(int location, boolean transpose, float values[]) {
        nUniformMatrix4fv(nativeCtxInfo, location, transpose, values);
    }

    void enableVertexAttributes() {
        nEnableVertexAttributes(nativeCtxInfo);
    }

    void disableVertexAttributes() {
        nDisableVertexAttributes(nativeCtxInfo);
    }

    void drawIndexedQuads(float coords[], byte colors[], int numVertices) {
        nDrawIndexedQuads(nativeCtxInfo, numVertices, coords, colors);
    }

    int createIndexBuffer16(short data[]) {
        return nCreateIndexBuffer16(nativeCtxInfo, data, data.length);
    }

    void setIndexBuffer(int ib) {
        nSetIndexBuffer(nativeCtxInfo, ib);
    }

    void setDeviceParametersFor2D() {
        nSetDeviceParametersFor2D(nativeCtxInfo);
    }

    void setDeviceParametersFor3D() {
        nSetDeviceParametersFor3D(nativeCtxInfo);
    }

    long createES2Mesh() {
        return nCreateES2Mesh(nativeCtxInfo);
    }

    void releaseES2Mesh(long nativeHandle) {
        nReleaseES2Mesh(nativeCtxInfo, nativeHandle);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, short[] indexBuffer, int indexBufferLength) {
        return nBuildNativeGeometryShort(nativeCtxInfo, nativeHandle,
                vertexBuffer, vertexBufferLength, indexBuffer, indexBufferLength);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, int[] indexBuffer, int indexBufferLength) {
        return nBuildNativeGeometryInt(nativeCtxInfo, nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    long createES2PhongMaterial() {
        return nCreateES2PhongMaterial(nativeCtxInfo);
    }

    void releaseES2PhongMaterial(long nativeHandle) {
        nReleaseES2PhongMaterial(nativeCtxInfo, nativeHandle);
    }

    void setSolidColor(long nativePhongMaterial, float r, float g, float b, float a) {
        nSetSolidColor(nativeCtxInfo, nativePhongMaterial, r, g, b, a);
    }

    void setMap(long nativePhongMaterial, int mapType, int texID) {
        nSetMap(nativeCtxInfo, nativePhongMaterial, mapType, texID);
    }

    long createES2MeshView(long nativeMeshInfo) {
        return nCreateES2MeshView(nativeCtxInfo, nativeMeshInfo);
    }

    void releaseES2MeshView(long nativeHandle) {
        nReleaseES2MeshView(nativeCtxInfo, nativeHandle);
    }

    void setCullingMode(long nativeMeshViewInfo, int cullMode) {
        int cm;
        if (cullMode == MeshView.CULL_NONE) {
            cm = GL_NONE;
        } else if (cullMode == MeshView.CULL_BACK) {
            cm = GL_BACK;
        } else if (cullMode == MeshView.CULL_FRONT) {
            cm = GL_FRONT;
        } else {
            throw new IllegalArgumentException("illegal value for CullMode: " + cullMode);
        }
       nSetCullingMode(nativeCtxInfo, nativeMeshViewInfo, cm);
    }

    void setMaterial(long nativeMeshViewInfo, long nativePhongMaterialInfo) {
        nSetMaterial(nativeCtxInfo, nativeMeshViewInfo, nativePhongMaterialInfo);
    }

    void setWireframe(long nativeMeshViewInfo, boolean wireframe) {
        nSetWireframe(nativeCtxInfo, nativeMeshViewInfo, wireframe);
    }

    void setAmbientLight(long nativeMeshViewInfo, float r, float g, float b) {
        nSetAmbientLight(nativeCtxInfo, nativeMeshViewInfo, r, g, b);
    }

    void setLight(long nativeMeshViewInfo, int index, float x, float y, float z, float r, float g, float b, float w,
            float ca, float la, float qa, float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff) {
        nSetLight(nativeCtxInfo, nativeMeshViewInfo, index, x, y, z, r, g, b, w, ca, la, qa, isAttenuated,
                maxRange, dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
    }

    void renderMeshView(long nativeMeshViewInfo) {
        nRenderMeshView(nativeCtxInfo, nativeMeshViewInfo);
    }
}
