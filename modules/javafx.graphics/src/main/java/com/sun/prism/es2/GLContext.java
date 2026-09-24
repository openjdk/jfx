/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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
    private int nativeFBOID = PlatformUtil.isMac() ? FBO_ID_NOCACHE : FBO_ID_UNSET;

    /*
     * The JNI-era Prism-to-GL translation tables (GLContext.c translateScaleFactor, translatePrismToGL,
     * translatePixelStore), moved to Java: the es2_* ABI passes every "GL enum" argument straight to GL
     * (prism_es2_api.c), so the wrappers below translate the small GLContext.GL_* /
     * WRAPMODE_* indices to the real OpenGL values here, exactly as the native code used to. The values
     * are the ones prism_es2_api.h documents for the Java-side table test.
     */

    static int translateScaleFactor(int scaleFactor) {
        return switch (scaleFactor) {
            case GL_ZERO -> 0x0000;                        // GL_ZERO
            case GL_ONE -> 0x0001;                         // GL_ONE
            case GL_SRC_COLOR -> 0x0300;                   // GL_SRC_COLOR
            case GL_ONE_MINUS_SRC_COLOR -> 0x0301;         // GL_ONE_MINUS_SRC_COLOR
            case GL_SRC_ALPHA -> 0x0302;                   // GL_SRC_ALPHA
            case GL_ONE_MINUS_SRC_ALPHA -> 0x0303;         // GL_ONE_MINUS_SRC_ALPHA
            case GL_DST_ALPHA -> 0x0304;                   // GL_DST_ALPHA
            case GL_ONE_MINUS_DST_ALPHA -> 0x0305;         // GL_ONE_MINUS_DST_ALPHA
            case GL_DST_COLOR -> 0x0306;                   // GL_DST_COLOR
            case GL_ONE_MINUS_DST_COLOR -> 0x0307;         // GL_ONE_MINUS_DST_COLOR
            case GL_SRC_ALPHA_SATURATE -> 0x0308;          // GL_SRC_ALPHA_SATURATE
            case GL_CONSTANT_COLOR -> 0x8001;              // GL_CONSTANT_COLOR
            case GL_ONE_MINUS_CONSTANT_COLOR -> 0x8002;    // GL_ONE_MINUS_CONSTANT_COLOR
            case GL_CONSTANT_ALPHA -> 0x8003;              // GL_CONSTANT_ALPHA
            case GL_ONE_MINUS_CONSTANT_ALPHA -> 0x8004;    // GL_ONE_MINUS_CONSTANT_ALPHA
            default -> {
                System.err.println("Error: Unknown scale factor. Returning GL_ZERO (default)");
                yield 0x0000;
            }
        };
    }

    static int translatePrismToGL(int value) {
        return switch (value) {
            case GL_FLOAT -> 0x1406;                       // GL_FLOAT
            case GL_UNSIGNED_BYTE -> 0x1401;               // GL_UNSIGNED_BYTE
            case GL_UNSIGNED_INT_8_8_8_8_REV -> 0x8367;    // GL_UNSIGNED_INT_8_8_8_8_REV
            case GL_UNSIGNED_INT_8_8_8_8 -> 0x8035;        // GL_UNSIGNED_INT_8_8_8_8
            case GL_UNSIGNED_SHORT_8_8_APPLE -> 0x85BA;    // GL_UNSIGNED_SHORT_8_8_APPLE
            case GL_RGBA -> 0x1908;                        // GL_RGBA
            case GL_BGRA -> 0x80E1;                        // GL_BGRA
            case GL_RGB -> 0x1907;                         // GL_RGB
            case GL_LUMINANCE -> 0x1909;                   // GL_LUMINANCE
            case GL_ALPHA -> 0x1906;                       // GL_ALPHA
            case GL_RGBA32F -> 0x8814;                     // GL_RGBA32F
            case GL_YCBCR_422_APPLE -> 0x85B9;             // GL_YCBCR_422_APPLE
            case GL_TEXTURE_2D -> 0x0DE1;                  // GL_TEXTURE_2D
            case GL_TEXTURE_BINDING_2D -> 0x8069;          // GL_TEXTURE_BINDING_2D
            case GL_NEAREST -> 0x2600;                     // GL_NEAREST
            case GL_LINEAR -> 0x2601;                      // GL_LINEAR
            case GL_NEAREST_MIPMAP_NEAREST -> 0x2700;      // GL_NEAREST_MIPMAP_NEAREST
            case GL_LINEAR_MIPMAP_LINEAR -> 0x2703;        // GL_LINEAR_MIPMAP_LINEAR
            case WRAPMODE_REPEAT -> 0x2901;                // GL_REPEAT
            case WRAPMODE_CLAMP_TO_EDGE -> 0x812F;         // GL_CLAMP_TO_EDGE
            case WRAPMODE_CLAMP_TO_BORDER -> 0x812D;       // GL_CLAMP_TO_BORDER
            case GL_MAX_FRAGMENT_UNIFORM_COMPONENTS -> 0x8B49;   // GL_MAX_FRAGMENT_UNIFORM_COMPONENTS
            case GL_MAX_FRAGMENT_UNIFORM_VECTORS -> 0x8DFD;      // GL_MAX_FRAGMENT_UNIFORM_VECTORS
            case GL_MAX_TEXTURE_IMAGE_UNITS -> 0x8872;           // GL_MAX_TEXTURE_IMAGE_UNITS
            case GL_MAX_TEXTURE_SIZE -> 0x0D33;                  // GL_MAX_TEXTURE_SIZE
            case GL_MAX_VARYING_COMPONENTS -> 0x8B4B;            // GL_MAX_VARYING_COMPONENTS
            case GL_MAX_VARYING_VECTORS -> 0x8DFC;               // GL_MAX_VARYING_VECTORS
            case GL_MAX_VERTEX_ATTRIBS -> 0x8869;                // GL_MAX_VERTEX_ATTRIBS
            case GL_MAX_VERTEX_UNIFORM_COMPONENTS -> 0x8B4A;     // GL_MAX_VERTEX_UNIFORM_COMPONENTS
            case GL_MAX_VERTEX_UNIFORM_VECTORS -> 0x8DFB;        // GL_MAX_VERTEX_UNIFORM_VECTORS
            case GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS -> 0x8B4C;    // GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
            default -> {
                System.err.println("warning: Unknown value. Returning value = " + value);
                yield value;
            }
        };
    }

    static int translatePixelStore(int pname) {
        return switch (pname) {
            case GL_UNPACK_ALIGNMENT -> 0x0CF5;            // GL_UNPACK_ALIGNMENT
            case GL_UNPACK_ROW_LENGTH -> 0x0CF2;           // GL_UNPACK_ROW_LENGTH
            case GL_UNPACK_SKIP_PIXELS -> 0x0CF4;          // GL_UNPACK_SKIP_PIXELS
            case GL_UNPACK_SKIP_ROWS -> 0x0CF3;            // GL_UNPACK_SKIP_ROWS
            default -> {
                System.err.println("warning: Unknown pname. Returning pname = " + pname);
                yield pname;
            }
        };
    }

    void activeTexture(int texUnit) {
        ES2Native.activeTexture(nativeCtxInfo, texUnit);
    }

    void bindFBO(int nativeFBOID) {
        switch (this.nativeFBOID) {
            case FBO_ID_UNSET:
                this.nativeFBOID = nativeFBOID;
                ES2Native.bindFBO(nativeCtxInfo, nativeFBOID);
                break;
            case FBO_ID_NOCACHE:
                ES2Native.bindFBO(nativeCtxInfo, nativeFBOID);
                break;
            default:
                if (this.nativeFBOID != nativeFBOID) {
                    ES2Native.bindFBO(nativeCtxInfo, nativeFBOID);
                    this.nativeFBOID = nativeFBOID;
                }
                break;
        }
    }

    void bindTexture(int texID) {
        ES2Native.bindTexture(nativeCtxInfo, texID);
    }

    void blendFunc(int sFactor, int dFactor) {
        ES2Native.blendFunc(nativeCtxInfo, translateScaleFactor(sFactor), translateScaleFactor(dFactor));
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
        ES2Native.clearBuffers(nativeCtxInfo, r, g, b, a, clearColor, clearDepth,
                ignoreScissor);
    }

    /**
     * Compiles the given shader program.  If successful, this function returns
     * a handle to the newly created shader object; otherwise returns 0.
     */
    int compileShader(String shaderSource, boolean vertex) {
        return ES2Native.shaderCompile(nativeCtxInfo, shaderSource, vertex);
    }

    int createDepthBuffer(int width, int height, int msaaSamples) {
        return ES2Native.depthBufferCreate(nativeCtxInfo, width, height, msaaSamples);
    }

    int createRenderBuffer(int width, int height, int msaaSamples) {
        return ES2Native.renderBufferCreate(nativeCtxInfo, width, height, msaaSamples);
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
        return ES2Native.fboCreate(nativeCtxInfo, texID);
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
        return ES2Native.programCreate(nativeCtxInfo, vertexShaderID, fragmentShaderIDArr,
                attrs, indexs);
    }

    int createTexture(int width, int height) {
        return ES2Native.textureCreate(nativeCtxInfo, width, height);
    }

    void deleteRenderBuffer(int dbID) {
        ES2Native.renderBufferDelete(nativeCtxInfo, dbID);
    }

    void deleteFBO(int fboID) {
        ES2Native.fboDelete(nativeCtxInfo, fboID);
    }

    void deleteShader(int shadeID) {
        ES2Native.shaderDelete(nativeCtxInfo, shadeID);
    }

    void blitFBO(int msaaFboID, int dstFboID,
                 int srcX0, int srcY0, int srcX1, int srcY1,
                 int dstX0, int dstY0, int dstX1, int dstY1)
    {
        ES2Native.blit(nativeCtxInfo, msaaFboID, dstFboID,
              srcX0, srcY0, srcX1, srcY1,
              dstX0, dstY0, dstX1, dstY1);
    }

    void deleteTexture(int tID) {
        ES2Native.textureDelete(nativeCtxInfo, tID);
    }

    void disposeShaders(int pID, int vID, int[] fID) {
        ES2Native.shadersDispose(nativeCtxInfo, pID, vID, fID);
    }

    void finish() {
        ES2Native.finish(nativeCtxInfo);
    }

    int genAndBindTexture() {
        int texID = ES2Native.genAndBindTexture(nativeCtxInfo);
        boundTextures[activeTexUnit] = texID;
        return texID;
    }

    int getBoundFBO() {
        switch (nativeFBOID) {
            case FBO_ID_UNSET:
                nativeFBOID = ES2Native.getFBO(nativeCtxInfo);
                return nativeFBOID;
            case FBO_ID_NOCACHE:
                return ES2Native.getFBO(nativeCtxInfo);
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
        return ES2Native.getIntParam(nativeCtxInfo, translatePrismToGL(param));
    }

    int getSampleSize() {
        int maxSamples = getMaxSampleSize();
        return maxSamples < 2 ? 0 : (maxSamples < 4 ? 2 : 4);
    }

    int getMaxSampleSize() {
        if (maxSampleSize > -1) {
            return maxSampleSize;
        }
        maxSampleSize = ES2Pipeline.msaa ? ES2Native.getMaxSampleSize(nativeCtxInfo) : 0;
        return maxSampleSize;
    }

    int getMaxTextureSize() {
        if (maxTextureSize > -1) {
            return maxTextureSize;
        }
        return maxTextureSize = getIntParam(GLContext.GL_MAX_TEXTURE_SIZE);
    }

    int getUniformLocation(int programID, String name) {
        return ES2Native.getUniformLocation(nativeCtxInfo, programID, name);
    }

    boolean isShaderCompilerSupported() {
        // GL2
        return true;
        // else TODO: glGetBooleanv(GL_SHADER_COMPILER, supported, 0); (JDK-8091367)
    }

    abstract void makeCurrent(GLDrawable drawable);

    void pixelStorei(int pname, int param) {
        ES2Native.pixelStorei(nativeCtxInfo, translatePixelStore(pname), param);
    }

    boolean readPixels(Buffer buffer, int x, int y, int w, int h) {
        boolean res = false;
        if (buffer instanceof ByteBuffer) {
            ByteBuffer buf = (ByteBuffer) buffer;
            byte[] arr = buf.hasArray() ? buf.array() : null;
            int length = buf.capacity();
            res = ES2Native.readPixels(nativeCtxInfo, length, buf, arr, x, y, w, h);
        } else if (buffer instanceof IntBuffer) {
            IntBuffer buf = (IntBuffer) buffer;
            int[] arr = buf.hasArray() ? buf.array() : null;
            int length = buf.capacity() * 4;
            // Note: This implementation only handle little-endian architectures,
            // which includes all the platforms we need to support for JavaFX 2.2.
            // We will need to do extra byte swapping at pixel level if we ever needs
            // to support big-endian architectures such as Solaris Sparc.
            res = ES2Native.readPixels(nativeCtxInfo, length, buf, arr, x, y, w, h);
        } else {
            throw new IllegalArgumentException("readPixel: pixel's buffer type is not supported: "
                    + buffer);
        }
        return res;
    }

    void scissorTest(boolean enable, int x, int y, int w, int h) {
        ES2Native.scissorTest(nativeCtxInfo, enable, x, y, w, h);
    }

    void setShaderProgram(int progid) {
        ES2Native.useProgram(nativeCtxInfo, progid);
    }

    void texParamsMinMax(int pname, boolean useMipmap) {
        int min = pname;
        int max = pname;
        if (useMipmap) {
            min = (min == GLContext.GL_LINEAR) ? GLContext.GL_LINEAR_MIPMAP_LINEAR
                    : GLContext.GL_NEAREST_MIPMAP_NEAREST;
        }
        ES2Native.texParamsMinMax(nativeCtxInfo, translatePrismToGL(min), translatePrismToGL(max));
    }

    boolean texImage2D(int target, int level, int internalFormat,
            int width, int height, int border, int format, int type,
            java.nio.Buffer pixels, boolean useMipmap) {
        return ES2Native.texImage2D(nativeCtxInfo, translatePrismToGL(target), level,
                translatePrismToGL(internalFormat), width, height, border,
                translatePrismToGL(format), translatePrismToGL(type), pixels, useMipmap);
    }

    void texSubImage2D(int target, int level, int xoffset, int yoffset,
            int width, int height, int format, int type, java.nio.Buffer pixels) {
        ES2Native.texSubImage2D(nativeCtxInfo, translatePrismToGL(target), level, xoffset, yoffset,
                width, height, translatePrismToGL(format), translatePrismToGL(type), pixels);
    }

    void updateViewportAndDepthTest(int x, int y, int w, int h,
            boolean depthTest) {
        if (viewportX != x || viewportY != y || viewportWidth != w || viewportHeight != h) {
            viewportX = x;
            viewportY = y;
            viewportWidth = w;
            viewportHeight = h;
            ES2Native.updateViewport(nativeCtxInfo, x, y, w, h);
        }
        if (this.depthTest != depthTest) {
            ES2Native.setDepthTest(nativeCtxInfo, depthTest);
            this.depthTest = depthTest;
        }
    }

    void updateMSAAState(boolean msaa) {
        if (this.msaa != msaa) {
            ES2Native.setMSAA(nativeCtxInfo, msaa);
            this.msaa = msaa;
        }
    }

    void updateFilterState(int texID, boolean linearFilter) {
        ES2Native.updateFilterState(nativeCtxInfo, texID, linearFilter);
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
        ES2Native.updateWrapState(nativeCtxInfo, texID, translatePrismToGL(wm));
    }

    void uniform1f(int location, float v0) {
        ES2Native.uniform1f(nativeCtxInfo, location, v0);
    }

    void uniform2f(int location, float v0, float v1) {
        ES2Native.uniform2f(nativeCtxInfo, location, v0, v1);
    }

    void uniform3f(int location, float v0, float v1, float v2) {
        ES2Native.uniform3f(nativeCtxInfo, location, v0, v1, v2);
    }

    void uniform4f(int location, float v0, float v1, float v2, float v3) {
        ES2Native.uniform4f(nativeCtxInfo, location, v0, v1, v2, v3);
    }

    void uniform4fv(int location, int count, java.nio.FloatBuffer value) {
        ES2Native.uniform4fv(nativeCtxInfo, location, count, value);
    }

    void uniform1i(int location, int v0) {
        ES2Native.uniform1i(nativeCtxInfo, location, v0);
    }

    void uniform2i(int location, int v0, int v1) {
        ES2Native.uniform2i(nativeCtxInfo, location, v0, v1);
    }

    void uniform3i(int location, int v0, int v1, int v2) {
        ES2Native.uniform3i(nativeCtxInfo, location, v0, v1, v2);
    }

    void uniform4i(int location, int v0, int v1, int v2, int v3) {
        ES2Native.uniform4i(nativeCtxInfo, location, v0, v1, v2, v3);
    }

    void uniform4iv(int location, int count, java.nio.IntBuffer value) {
        ES2Native.uniform4iv(nativeCtxInfo, location, count, value);
    }

    void uniformMatrix4fv(int location, boolean transpose, float values[]) {
        ES2Native.uniformMatrix4fv(nativeCtxInfo, location, transpose, values);
    }

    void enableVertexAttributes() {
        ES2Native.enableVertexAttributes(nativeCtxInfo);
    }

    void disableVertexAttributes() {
        ES2Native.disableVertexAttributes(nativeCtxInfo);
    }

    void drawIndexedQuads(float coords[], byte colors[], int numVertices) {
        ES2Native.drawIndexedQuads(nativeCtxInfo, numVertices, coords, colors);
    }

    int createIndexBuffer16(short data[]) {
        return ES2Native.indexBuffer16Create(nativeCtxInfo, data, data.length);
    }

    void setIndexBuffer(int ib) {
        ES2Native.setIndexBuffer(nativeCtxInfo, ib);
    }

    void setDeviceParametersFor2D() {
        ES2Native.setDeviceParametersFor2D(nativeCtxInfo);
    }

    void setDeviceParametersFor3D() {
        ES2Native.setDeviceParametersFor3D(nativeCtxInfo);
    }

    long createES2Mesh() {
        return ES2Native.meshCreate(nativeCtxInfo);
    }

    void releaseES2Mesh(long nativeHandle) {
        ES2Native.meshRelease(nativeCtxInfo, nativeHandle);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, short[] indexBuffer, int indexBufferLength) {
        return ES2Native.meshBuildGeometryShort(nativeCtxInfo, nativeHandle,
                vertexBuffer, vertexBufferLength, indexBuffer, indexBufferLength);
    }

    boolean buildNativeGeometry(long nativeHandle, float[] vertexBuffer,
            int vertexBufferLength, int[] indexBuffer, int indexBufferLength) {
        return ES2Native.meshBuildGeometryInt(nativeCtxInfo, nativeHandle, vertexBuffer,
                vertexBufferLength, indexBuffer, indexBufferLength);
    }

    /**
     * Draws {@code meshHandle} with the culling and fill state {@link ES2MeshView} now holds directly
     * ({@code MeshViewInfo} is gone): {@code cullEnable} is a boolean, {@code cullModeGL} is
     * {@link ES2Native#GL_BACK} / {@link ES2Native#GL_FRONT} and {@code fillModeGL} is
     * {@link ES2Native#GL_FILL} / {@link ES2Native#GL_LINE}. Folds the JNI nSetCullingMode /
     * nSetWireframe / nSetMaterial / nRenderMeshView; the phong-material and light setters were dead
     * stores and are gone. {@link ES2MeshView} calls this only when a material is set, which is what
     * nRenderMeshView's NULL phongMaterialInfo gate did.
     */
    void meshRender(long meshHandle, int cullEnable, int cullModeGL, int fillModeGL) {
        ES2Native.meshRender(nativeCtxInfo, meshHandle, cullEnable, cullModeGL, fillModeGL);
    }
}
