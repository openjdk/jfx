/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.util.Map;

import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;
import com.sun.prism.MediaFrame;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.PhongMaterial;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.impl.VertexBuffer;
import com.sun.prism.impl.ps.BaseShaderFactory;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ES2ResourceFactory extends BaseShaderFactory {

    private ES2Context context;
    private final int maxTextureSize;

    ES2ResourceFactory(Screen screen) {
        context = new ES2Context(screen, this);
        maxTextureSize = computeMaxTextureSize();

        if (PrismSettings.verbose) {
            System.out.println("Non power of two texture support = " + 
                    context.getGLContext().canCreateNonPowTwoTextures());
        }
    }

    public TextureResourcePool getTextureResourcePool() {
        return ES2VramPool.instance;
    }

    public Presentable createPresentable(PresentableState pState) {
        return new ES2SwapChain(context, pState);
    }

    public Texture createTexture(PixelFormat formatHint,
                                 Usage usageHint,
                                 WrapMode wrapMode,
                                 int w, int h)
    {
        return ES2Texture.create(context, formatHint, wrapMode, w, h);
    }

    public Texture createTexture(MediaFrame frame) {
        return ES2Texture.create(context, frame);
    }

    public int getRTTWidth(int w, WrapMode wrapMode) {
        return ES2RTTexture.getCompatibleDimension(context, w, wrapMode);
    }

    public int getRTTHeight(int h, WrapMode wrapMode) {
        return ES2RTTexture.getCompatibleDimension(context, h, wrapMode);
    }

    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode) {
        return createRTTexture(width, height, wrapMode, false);
    }

    public RTTexture createRTTexture(int width, int height, WrapMode wrapMode, boolean antiAliasing) {
        return ES2RTTexture.create(context, width, height, wrapMode, antiAliasing);
    }

    public boolean isFormatSupported(PixelFormat format) {
        GLFactory glFactory = ES2Pipeline.glFactory;
        switch (format) {
            case BYTE_RGB:
            case BYTE_GRAY:
            case BYTE_ALPHA:
            case MULTI_YCbCr_420:
                return true;
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                if (glFactory.isGL2() || PlatformUtil.isIOS()) {
                    return true;
                } else {
                    // for OpenGLES, BGRA can be supported by extension - if
                    // we have it, use it
                    return glFactory.isGLExtensionSupported("GL_EXT_texture_format_BGRA8888");
                }
            case FLOAT_XYZW:
                return glFactory.isGL2()
                    // Unfortunately our support for float textures on GLES
                    // seems to be broken so we will defer use of this extension
                    // until we fix RT-26286.
//                        || glFactory.isGLExtensionSupported("GL_OES_texture_float")
                        ;
            case BYTE_APPLE_422:
                return glFactory.isGLExtensionSupported("GL_APPLE_ycbcr_422");
            default:
                return false;
        }
    }

    private int computeMaxTextureSize() {
        int size = context.getGLContext().getMaxTextureSize();
        if (PrismSettings.verbose) {
            System.err.println("Maximum supported texture size: " + size);
        }
        if (size > PrismSettings.maxTextureSize) {
            size = PrismSettings.maxTextureSize;
            if (PrismSettings.verbose) {
                System.err.println("Maximum texture size clamped to " + size);
            }
        }
        return size;
    }

    public int getMaximumTextureSize() {
        return maxTextureSize;
    }

    public Shader createShader(InputStream pixelShaderCode,
            Map<String, Integer> samplers,
            Map<String, Integer> params,
            int maxTexCoordIndex,
            boolean isPixcoordUsed,
            boolean isPerVertexColorUsed) {
        // figure out the appropriate vertex shader and minimal set of
        // vertex attributes to enable based on the given parameters
        Map<String, Integer> attributes =
                getVertexAttributes(isPerVertexColorUsed, maxTexCoordIndex);

        // create the combined shader program
        ES2Shader shader;
        String vertexShaderCode =
                createVertexShaderCode(isPerVertexColorUsed, maxTexCoordIndex);
        shader = ES2Shader.createFromSource(context, vertexShaderCode,
                pixelShaderCode, samplers, attributes,
                maxTexCoordIndex, isPixcoordUsed);

        return shader;
    }

    private static String createVertexShaderCode(boolean includePerVertexColor,
            int maxTexCoordIndex) {
        StringBuilder vsAttr = new StringBuilder();
        StringBuilder vsVary = new StringBuilder();
        StringBuilder vsMain = new StringBuilder();
        vsMain.append("void main() {\n");

        boolean includePosition = true;
        if (includePosition) {
            vsAttr.append("attribute vec2 positionAttr;\n");
            vsMain.append("    vec4 tmp = vec4(positionAttr, 0, 1);\n");
            vsMain.append("    gl_Position = mvpMatrix * tmp;\n");
        }
        if (includePerVertexColor) {
            vsAttr.append("attribute vec4 colorAttr;\n");
            vsVary.append("varying lowp vec4 perVertexColor;\n");
            vsMain.append("    perVertexColor = colorAttr;\n");
        }
        if (maxTexCoordIndex >= 0) {
            vsAttr.append("attribute vec2 texCoord0Attr;\n");
            vsVary.append("varying vec2 texCoord0;\n");
            vsMain.append("    texCoord0 = texCoord0Attr;\n");
        }
        if (maxTexCoordIndex >= 1) {
            vsAttr.append("attribute vec2 texCoord1Attr;\n");
            vsVary.append("varying vec2 texCoord1;\n");
            vsMain.append("    texCoord1 = texCoord1Attr;\n");
        }

        vsMain.append("}\n");
        StringBuilder vs = new StringBuilder();

        vs.append("#ifdef GL_ES\n");
        vs.append("#else\n");
        vs.append("#define lowp\n");
        vs.append("#endif\n");
        vs.append("uniform mat4 mvpMatrix;\n");
        vs.append(vsAttr);
        vs.append(vsVary);
        vs.append(vsMain);

        return vs.toString();
    }

    private Map<String, Integer> getVertexAttributes(boolean includePerVertexColor,
            int maxTexCoordIndex) {
        Map<String, Integer> attributes = new HashMap<String, Integer>();

        boolean includePosition = true;
        if (includePosition) {
            attributes.put("positionAttr", 0);
        }
        if (includePerVertexColor) {
            attributes.put("colorAttr", 1);
        }
        if (maxTexCoordIndex >= 0) {
            attributes.put("texCoord0Attr", 2);
        }
        if (maxTexCoordIndex >= 1) {
            attributes.put("texCoord1Attr", 3);
        }

        return attributes;
    }

    public Shader createStockShader(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Shader name must be non-null");
        }
        try {
            InputStream stream =
                    ES2ResourceFactory.class.getResourceAsStream(
                    "glsl/" + name + ".frag");
            Class klass =
                    Class.forName("com.sun.prism.shader." + name + "_Loader");
            Method m =
                    klass.getMethod("loadShader", new Class[]{ShaderFactory.class,
                        InputStream.class});
            return (Shader) m.invoke(null, new Object[]{this, stream});
        } catch (Throwable e) {
            e.printStackTrace();
            throw new InternalError("Error loading stock shader " + name);
        }
    }

    public VertexBuffer createVertexBuffer(int maxQuads) {
        return new VertexBuffer(maxQuads);
    }

    public void dispose() {
        context.clearContext();
    }

    public PhongMaterial createPhongMaterial() {
        return ES2PhongMaterial.create(context);
}

    public MeshView createMeshView(Mesh mesh) {
        return ES2MeshView.create(context, (ES2Mesh) mesh);
    }

    public Mesh createMesh() {
        return ES2Mesh.create(context);
    }
}
