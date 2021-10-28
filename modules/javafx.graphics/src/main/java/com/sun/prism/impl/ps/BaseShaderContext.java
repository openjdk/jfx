/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.ps;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.prism.CompositeMode;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Paint;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;

/**
 * Maintains resources such as Shaders and GlyphCaches that are intended to
 * be cached on a per-Screen basis, and provides methods that are called by
 * BaseShaderGraphics to validate current state.  The inner State class is
 * used to encapsulate the current and previously validated state (such as
 * texture bindings) so that the validation routines can avoid redundant
 * state changes.  There should be only one BaseShaderContext per Screen,
 * however there may be one or more State instances per BaseShaderContext.
 * <p>
 * A note about State objects... The JOGL architecture creates a GLContext
 * for each GLDrawable (one GLContext per GLDrawable, and one GLDrawable
 * per onscreen window).  Resources such as textures and shaders can be
 * shared between those GLContext instances, but other state (texture bindings,
 * scissor rect, etc) cannot be shared.  Therefore we need to maintain
 * one State instance per GLContext instance, which means there may be more
 * than one State instance per BaseShaderContext.  The currentState variable
 * holds the current State instance corresponding to the current RenderTarget,
 * and is revalidated as part of the updateRenderTarget() method.  The ES2
 * backend will create a new State instance for each window, but the D3D
 * backend is free to create a single State instance that can be shared for
 * the entire Screen.
 */
public abstract class BaseShaderContext extends BaseContext {
    private static final int CHECK_SHADER    = (0x01     );
    private static final int CHECK_TRANSFORM = (0x01 << 1);
    private static final int CHECK_CLIP      = (0x01 << 2);
    private static final int CHECK_COMPOSITE = (0x01 << 3);
    private static final int CHECK_PAINT_OP_MASK =
        (CHECK_SHADER | CHECK_TRANSFORM | CHECK_CLIP | CHECK_COMPOSITE);
    private static final int CHECK_TEXTURE_OP_MASK =
        (CHECK_SHADER | CHECK_TRANSFORM | CHECK_CLIP | CHECK_COMPOSITE);
    private static final int CHECK_CLEAR_OP_MASK =
        (CHECK_CLIP);

    public enum MaskType {
        SOLID          ("Solid"),
        TEXTURE        ("Texture"),
        ALPHA_ONE           ("AlphaOne", true),
        ALPHA_TEXTURE       ("AlphaTexture", true),
        ALPHA_TEXTURE_DIFF  ("AlphaTextureDifference", true),
        FILL_PGRAM     ("FillPgram"),
        DRAW_PGRAM     ("DrawPgram", FILL_PGRAM),
        FILL_CIRCLE    ("FillCircle"),
        DRAW_CIRCLE    ("DrawCircle", FILL_CIRCLE),
        FILL_ELLIPSE   ("FillEllipse"),
        DRAW_ELLIPSE   ("DrawEllipse", FILL_ELLIPSE),
        FILL_ROUNDRECT ("FillRoundRect"),
        DRAW_ROUNDRECT ("DrawRoundRect", FILL_ROUNDRECT),
        DRAW_SEMIROUNDRECT("DrawSemiRoundRect");

        private String name;
        private MaskType filltype;
        private boolean newPaintStyle;
        private MaskType(String name) {
            this.name = name;
        }
        private MaskType(String name, boolean newstyle) {
            this.name = name;
            this.newPaintStyle = newstyle;
        }
        private MaskType(String name, MaskType filltype) {
            this.name = name;
            this.filltype = filltype;
        }
        public String getName() {
            return name;
        }
        public MaskType getFillType() {
            return filltype;
        }
        public boolean isNewPaintStyle() {
            return newPaintStyle;
        }
    }

    // mask type     4 bits (14 types)
    // paint type    2 bits
    // paint opts    2 bits
    private static final int NUM_STOCK_SHADER_SLOTS =
        MaskType.values().length << 4;
    private final Shader[] stockShaders = new Shader[NUM_STOCK_SHADER_SLOTS];
    // stockShaders with alpha test
    private final Shader[] stockATShaders = new Shader[NUM_STOCK_SHADER_SLOTS];

    public enum SpecialShaderType {
        TEXTURE_RGB          ("Solid_TextureRGB"),
        TEXTURE_MASK_RGB     ("Mask_TextureRGB"),
        TEXTURE_YV12         ("Solid_TextureYV12"),
        TEXTURE_First_LCD    ("Solid_TextureFirstPassLCD"),
        TEXTURE_SECOND_LCD   ("Solid_TextureSecondPassLCD"),
        SUPER                ("Mask_TextureSuper");

        private String name;
        private SpecialShaderType(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    private final Shader[] specialShaders = new Shader[SpecialShaderType.values().length];
    // specialShaders with alpha test
    private final Shader[] specialATShaders = new Shader[SpecialShaderType.values().length];

    private Shader externalShader;

    private RTTexture lcdBuffer;
    private final ShaderFactory factory;

    private State state;

    protected BaseShaderContext(Screen screen, ShaderFactory factory, int vbQuads) {
        super(screen, factory, vbQuads);
        this.factory = factory;
        init();
    }

    protected void init() {
        state = null;
        if (externalShader != null && !externalShader.isValid()) {
            externalShader.dispose();
            externalShader = null;
        }
        // the rest of the shaders will be re-validated as they are used
   }

    public static class State {
        private Shader lastShader;
        private RenderTarget lastRenderTarget;
        private NGCamera lastCamera;
        private boolean lastDepthTest;
        private BaseTransform lastTransform = new Affine3D();
        private Rectangle lastClip;
        private CompositeMode lastComp;
        private Texture[] lastTextures = new Texture[4];
        private boolean isXformValid;
        private float lastConst1 = Float.NaN;
        private float lastConst2 = Float.NaN;
        private float lastConst3 = Float.NaN;
        private float lastConst4 = Float.NaN;
        private float lastConst5 = Float.NaN;
        private float lastConst6 = Float.NaN;
        private boolean lastState3D = false;
    }

    @Override
    protected void setPerspectiveTransform(GeneralTransform3D transform) {
        if (checkDisposed()) return;

        state.isXformValid = false;
        super.setPerspectiveTransform(transform);
    }

    protected void resetLastClip(State state) {
        if (checkDisposed()) return;

        state.lastClip = null;
    }

    protected abstract State updateRenderTarget(RenderTarget target, NGCamera camera,
                                                boolean depthTest);

    protected abstract void updateTexture(int texUnit, Texture tex);

    protected abstract void updateShaderTransform(Shader shader,
                                                  BaseTransform xform);

    protected abstract void updateWorldTransform(BaseTransform xform);

    protected abstract void updateClipRect(Rectangle clipRect);

    protected abstract void updateCompositeMode(CompositeMode mode);

    private static int getStockShaderIndex(MaskType maskType, Paint paint) {
        int paintType;
        int paintOption;
        if (paint == null) {
            paintType = 0;
            paintOption = 0;
        } else {
            paintType = paint.getType().ordinal();
            if (paint.getType().isGradient()) {
                paintOption = ((Gradient)paint).getSpreadMethod();
            } else {
                paintOption = 0;
            }
        }
        return (maskType.ordinal() << 4) | (paintType << 2) | (paintOption << 0);
    }

    private Shader getPaintShader(boolean alphaTest, MaskType maskType, Paint paint) {
        if (checkDisposed()) return null;

        int index = getStockShaderIndex(maskType, paint);
        Shader shaders[] = alphaTest ? stockATShaders : stockShaders;
        Shader shader = shaders[index];
        if (shader != null && !shader.isValid()) {
            shader.dispose();
            shader = null;
        }
        if (shader == null) {
            String shaderName =
                maskType.getName() + "_" + paint.getType().getName();
            if (paint.getType().isGradient() && !maskType.isNewPaintStyle()) {
                Gradient grad = (Gradient) paint;
                int spreadMethod = grad.getSpreadMethod();
                if (spreadMethod == Gradient.PAD) {
                    shaderName += "_PAD";
                } else if (spreadMethod == Gradient.REFLECT) {
                    shaderName += "_REFLECT";
                } else if (spreadMethod == Gradient.REPEAT) {
                    shaderName += "_REPEAT";
                }
            }
            if (alphaTest) {
                shaderName += "_AlphaTest";
            }
            shader = shaders[index] = factory.createStockShader(shaderName);
        }
        return shader;
    }

    private void updatePaintShader(BaseShaderGraphics g, Shader shader,
                                   MaskType maskType, Paint paint,
                                   float bx, float by, float bw, float bh)
    {
        if (checkDisposed()) return;

        Paint.Type paintType = paint.getType();
        if (paintType == Paint.Type.COLOR || maskType.isNewPaintStyle()) {
            return;
        }

        float rx, ry, rw, rh;
        if (paint.isProportional()) {
            rx = bx; ry = by; rw = bw; rh = bh;
        } else {
            rx = 0f; ry = 0f; rw = 1f; rh = 1f;
        }

        switch (paintType) {
        case LINEAR_GRADIENT:
            PaintHelper.setLinearGradient(g, shader,
                                          (LinearGradient)paint,
                                          rx, ry, rw, rh);
            break;
        case RADIAL_GRADIENT:
            PaintHelper.setRadialGradient(g, shader,
                                          (RadialGradient)paint,
                                          rx, ry, rw, rh);
            break;
        case IMAGE_PATTERN:
            PaintHelper.setImagePattern(g, shader,
                                        (ImagePattern)paint,
                                        rx, ry, rw, rh);
        default:
            break;
        }
    }

    private Shader getSpecialShader(BaseGraphics g, SpecialShaderType sst) {
        if (checkDisposed()) return null;

        // We do alpha test if depth test is enabled
        boolean alphaTest = g.isAlphaTestShader();
        Shader shaders[] = alphaTest ? specialATShaders : specialShaders;
        Shader shader = shaders[sst.ordinal()];
        if (shader != null && !shader.isValid()) {
            shader.dispose();
            shader = null;
        }
        if (shader == null) {
            String shaderName = sst.getName();
            if (alphaTest) {
                shaderName += "_AlphaTest";
            }
            shaders[sst.ordinal()] = shader = factory.createStockShader(shaderName);
        }
        return shader;
    }

    @Override
    public boolean isSuperShaderEnabled() {
        if (checkDisposed()) return false;

        return state.lastShader == specialATShaders[SpecialShaderType.SUPER.ordinal()]
                || state.lastShader == specialShaders[SpecialShaderType.SUPER.ordinal()];
    }

    private void updatePerVertexColor(Paint paint, float extraAlpha) {
        if (checkDisposed()) return;

        if (paint != null && paint.getType() == Paint.Type.COLOR) {
            getVertexBuffer().setPerVertexColor((Color)paint, extraAlpha);
        } else {
            getVertexBuffer().setPerVertexColor(extraAlpha);
        }
    }

    @Override
    public void validateClearOp(BaseGraphics g) {
        checkState((BaseShaderGraphics) g, CHECK_CLEAR_OP_MASK, null, null);
    }

    @Override
    public void validatePaintOp(BaseGraphics g, BaseTransform xform,
                                Texture maskTex,
                                float bx, float by, float bw, float bh)
    {
        validatePaintOp((BaseShaderGraphics)g, xform,
                        maskTex, bx, by, bw, bh);
    }

    Shader validatePaintOp(BaseShaderGraphics g, BaseTransform xform,
                           MaskType maskType,
                           float bx, float by, float bw, float bh)
    {
        return validatePaintOp(g, xform, maskType, null, bx, by, bw, bh);
    }

    Shader validatePaintOp(BaseShaderGraphics g, BaseTransform xform,
                           MaskType maskType,
                           float bx, float by, float bw, float bh,
                           float k1, float k2, float k3, float k4, float k5, float k6)
    {
        if (checkDisposed()) return null;

        // this is not ideal, but will have to do for now (tm).
        // various paint primitives use shader parameters, and we have to flush
        // the vertex buffer if those change.  Ideally we would do this in
        // checkState but there is no mechanism to pass this info through.
        if (state.lastConst1 != k1 || state.lastConst2 != k2 ||
            state.lastConst3 != k3 || state.lastConst4 != k4 ||
            state.lastConst5 != k5 || state.lastConst6 != k6)
        {
            flushVertexBuffer();

            state.lastConst1 = k1;
            state.lastConst2 = k2;
            state.lastConst3 = k3;
            state.lastConst4 = k4;
            state.lastConst5 = k5;
            state.lastConst6 = k6;
        }

        return validatePaintOp(g, xform, maskType, null, bx, by, bw, bh);
    }

    Shader validatePaintOp(BaseShaderGraphics g, BaseTransform xform,
                           MaskType maskType, Texture maskTex,
                           float bx, float by, float bw, float bh,
                           float k1, float k2, float k3, float k4, float k5, float k6)
    {
        // this is not ideal, but will have to do for now (tm).
        // various paint primitives use shader parameters, and we have to flush
        // the vertex buffer if those change.  Ideally we would do this in
        // checkState but there is no mechanism to pass this info through.
        if (state.lastConst1 != k1 || state.lastConst2 != k2 ||
            state.lastConst3 != k3 || state.lastConst4 != k4 ||
            state.lastConst5 != k5 || state.lastConst6 != k6)
        {
            flushVertexBuffer();

            state.lastConst1 = k1;
            state.lastConst2 = k2;
            state.lastConst3 = k3;
            state.lastConst4 = k4;
            state.lastConst5 = k5;
            state.lastConst6 = k6;
        }

        return validatePaintOp(g, xform, maskType, maskTex, bx, by, bw, bh);
    }

    Shader validatePaintOp(BaseShaderGraphics g, BaseTransform xform,
                           Texture maskTex,
                           float bx, float by, float bw, float bh)
    {
        return validatePaintOp(g, xform, MaskType.TEXTURE,
                               maskTex, bx, by, bw, bh);
    }

    Shader validatePaintOp(BaseShaderGraphics g, BaseTransform xform,
                           MaskType maskType, Texture maskTex,
                           float bx, float by, float bw, float bh)
    {
        if (maskType == null) {
            throw new InternalError("maskType must be non-null");
        }

        if (externalShader == null) {
            Paint paint = g.getPaint();
            Texture paintTex = null;
            Texture tex0;
            Texture tex1;
            if (paint.getType().isGradient()) {
                // we need to flush here in case the paint shader is staying
                // the same but the paint parameters are changing; we do this
                // unconditionally for now (in theory we could keep track
                // of the last validated paint, and the shape bounds in the
                // case of proportional gradients, but the case where the
                // same paint parameters are used multiple times in a row
                // is so rare that it's not worth optimizing this any further)
                flushVertexBuffer();
                // we have to fetch the texture containing the gradient
                // colors in advance since checkState() is responsible for
                // binding the texture(s)
                if (maskType.isNewPaintStyle()) {
                    paintTex = PaintHelper.getWrapGradientTexture(g);
                } else {
                    paintTex = PaintHelper.getGradientTexture(g, (Gradient)paint);
                }
            } else if (paint.getType() == Paint.Type.IMAGE_PATTERN) {
                // We need to flush here. See comment above about paint parameters changing.
                flushVertexBuffer();
                ImagePattern texPaint = (ImagePattern)paint;
                ResourceFactory rf = g.getResourceFactory();
                paintTex = rf.getCachedTexture(texPaint.getImage(), Texture.WrapMode.REPEAT);
            }
            Shader shader;
            if (factory.isSuperShaderAllowed() &&
                paintTex == null &&
                maskTex == factory.getGlyphTexture())
            {
                // Enabling the super shader to be used to render text.
                // The texture pointed by tex0 is the region cache texture
                // and it does not affect text rendering
                shader = getSpecialShader(g, SpecialShaderType.SUPER);
                tex0 = factory.getRegionTexture();
                tex1 = maskTex;
            } else {
                // NOTE: We are making assumptions here about which texture
                // corresponds to which texture unit.  In a JSL file the
                // first sampler mentioned will correspond to texture unit 0,
                // the second sampler will correspond to texture unit 1,
                // and so on, and there's currently no way to explicitly
                // associate a sampler with a texture unit in the JSL file.
                // So for now we assume that mask-related samplers are
                // declared before any paint-related samplers in the
                // composed JSL files.
                if (maskTex != null) {
                    tex0 = maskTex;
                    tex1 = paintTex;
                } else {
                    tex0 = paintTex;
                    tex1 = null;
                }
                // We do alpha test if depth test is enabled
                shader = getPaintShader(g.isAlphaTestShader(), maskType, paint);
            }
            checkState(g, CHECK_PAINT_OP_MASK, xform, shader);
            setTexture(0, tex0);
            setTexture(1, tex1);
            updatePaintShader(g, shader, maskType, paint, bx, by, bw, bh);
            updatePerVertexColor(paint, g.getExtraAlpha());
            if (paintTex != null) paintTex.unlock();
            return shader;
        } else {
            // note that paint is assumed to be a simple Color in this case
            checkState(g, CHECK_PAINT_OP_MASK, xform, externalShader);
            setTexture(0, maskTex);
            setTexture(1, null);  // Needed?
            updatePerVertexColor(null, g.getExtraAlpha());
            return externalShader;
        }
    }

    @Override
    public void validateTextureOp(BaseGraphics g, BaseTransform xform,
                                  Texture tex0, PixelFormat format)
    {
        validateTextureOp((BaseShaderGraphics)g, xform, tex0, null, format);
    }

    //This function sets the first LCD sample shader.
    public Shader validateLCDOp(BaseShaderGraphics g, BaseTransform xform,
                                Texture tex0, Texture tex1, boolean firstPass,
                                Paint fillColor)
    {
        if (checkDisposed()) return null;

        Shader shader = firstPass ? getSpecialShader(g, SpecialShaderType.TEXTURE_First_LCD) :
                                    getSpecialShader(g, SpecialShaderType.TEXTURE_SECOND_LCD);

        checkState(g, CHECK_TEXTURE_OP_MASK, xform, shader);
        setTexture(0, tex0);
        setTexture(1, tex1);
        updatePerVertexColor(fillColor, g.getExtraAlpha());
        return shader;
    }

    Shader validateTextureOp(BaseShaderGraphics g, BaseTransform xform,
                             Texture[] textures, PixelFormat format)
    {
        if (checkDisposed()) return null;

        Shader shader;

        if (format == PixelFormat.MULTI_YCbCr_420) {
            // must have at least three textures, any more than four are ignored
            if (textures.length < 3) {
                return null;
            }

            if (externalShader == null) {
                shader = getSpecialShader(g, SpecialShaderType.TEXTURE_YV12);
            } else {
                shader = externalShader;
            }
        } else { // add more multitexture shaders here
            return null;
        }

        if (null != shader) {
            checkState(g, CHECK_TEXTURE_OP_MASK, xform, shader);
            // clamp to 0..4 textures for now, expand on this later if we need to
            int texCount = Math.max(0, Math.min(textures.length, 4));
            for (int index = 0; index < texCount; index++) {
                setTexture(index, textures[index]);
            }
            updatePerVertexColor(null, g.getExtraAlpha());
        }
        return shader;
    }

    Shader validateTextureOp(BaseShaderGraphics g, BaseTransform xform,
                             Texture tex0, Texture tex1, PixelFormat format)
    {
        if (checkDisposed()) return null;

        Shader shader;
        if (externalShader == null) {
            switch (format) {
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
            case BYTE_RGB:
            case BYTE_GRAY:
            case BYTE_APPLE_422: // uses GL_RGBA as internal format
                if (factory.isSuperShaderAllowed() &&
                    tex0 == factory.getRegionTexture() &&
                    tex1 == null)
                {
                    // Enabling the super shader to be used for texture rendering.
                    // The shader was designed to render many Regions (from the Region
                    // texture cache) and text (from the glyph cache texture) without
                    // changing the state in the context.
                    shader = getSpecialShader(g, SpecialShaderType.SUPER);
                    tex1 = factory.getGlyphTexture();
                } else {
                    shader = getSpecialShader(g, SpecialShaderType.TEXTURE_RGB);
                }
                break;
            case MULTI_YCbCr_420: // Must use multitexture method
            case BYTE_ALPHA:
            default:
                throw new InternalError("Pixel format not supported: " + format);
            }
        } else {
            shader = externalShader;
        }
        checkState(g, CHECK_TEXTURE_OP_MASK, xform, shader);
        setTexture(0, tex0);
        setTexture(1, tex1);
        updatePerVertexColor(null, g.getExtraAlpha());
        return shader;
    }

    Shader validateMaskTextureOp(BaseShaderGraphics g, BaseTransform xform,
                                 Texture tex0, Texture tex1, PixelFormat format)
    {
        if (checkDisposed()) return null;

        Shader shader;
        if (externalShader == null) {
            switch (format) {
            case INT_ARGB_PRE:
            case BYTE_BGRA_PRE:
            case BYTE_RGB:
            case BYTE_GRAY:
            case BYTE_APPLE_422: // uses GL_RGBA as internal format
                shader = getSpecialShader(g, SpecialShaderType.TEXTURE_MASK_RGB);
                break;
            case MULTI_YCbCr_420: // Must use multitexture method
            case BYTE_ALPHA:
            default:
                throw new InternalError("Pixel format not supported: " + format);
            }
        } else {
            shader = externalShader;
        }
        checkState(g, CHECK_TEXTURE_OP_MASK, xform, shader);
        setTexture(0, tex0);
        setTexture(1, tex1);
        updatePerVertexColor(null, g.getExtraAlpha());
        return shader;
    }

    void setExternalShader(BaseShaderGraphics g, Shader shader) {
        if (checkDisposed()) return;

        // Note that this method is called when the user calls
        // ShaderGraphics.setExternalShader().  We flush any pending
        // operations and synchronously enable the given shader here
        // because the caller (i.e., decora-prism-ps peer) needs to be
        // able to call shader.setConstant() after calling setExternalShader().
        // (In the ES2 backend, setConstant() bottoms out in glUniform(),
        // which can only be called when the program is active, i.e., after
        // shader.enable() is called.  Kind of gross, but that's why the
        // external shader mechanism is setup the way it is currently.)
        // So here we enable the shader just so that the user can update
        // shader constants, and we set the externalShader instance variable.
        // Later in checkState(), we will set the externalShader and
        // update the current transform state "for real".
        flushVertexBuffer();
        if (shader != null) {
            shader.enable();
        }
        externalShader = shader;
    }

    private void checkState(BaseShaderGraphics g,
                            int checkFlags,
                            BaseTransform xform,
                            Shader shader)
    {
        if (checkDisposed()) return;

        setRenderTarget(g);

        if ((checkFlags & CHECK_SHADER) != 0) {
            if (shader != state.lastShader) {
                flushVertexBuffer();
                shader.enable();
                state.lastShader = shader;
                // the transform matrix is part of the state of each shader
                // (in ES2 at least), so we need to make sure the transform
                // is updated for the current shader by setting isXformValid=false
                state.isXformValid = false;
                checkFlags |= CHECK_TRANSFORM;
            }
        }

        if ((checkFlags & CHECK_TRANSFORM) != 0) {
            if (!state.isXformValid || !xform.equals(state.lastTransform)) {
                flushVertexBuffer();
                updateShaderTransform(shader, xform);
                state.lastTransform.setTransform(xform);
                state.isXformValid = true;
            }
        }

        if ((checkFlags & CHECK_CLIP) != 0) {
            Rectangle clip = g.getClipRectNoClone();
            if (clip != state.lastClip) {
                flushVertexBuffer();
                updateClipRect(clip);
                state.lastClip = clip;
            }
        }

        if ((checkFlags & CHECK_COMPOSITE) != 0) {
            CompositeMode mode = g.getCompositeMode();
            if (mode != state.lastComp) {
                flushVertexBuffer();
                updateCompositeMode(mode);
                state.lastComp = mode;
            }
        }
    }

    private void setTexture(int texUnit, Texture tex) {
        if (checkDisposed()) return;

        if (tex != null) tex.assertLocked();
        if (tex != state.lastTextures[texUnit]) {
            flushVertexBuffer();
            updateTexture(texUnit, tex);
            state.lastTextures[texUnit] = tex;
        }
    }

    //Current RenderTarget is the lcdBuffer after this method.
    public void initLCDBuffer(int width, int height) {
        if (checkDisposed()) return;

        lcdBuffer = factory.createRTTexture(width, height, Texture.WrapMode.CLAMP_NOT_NEEDED);
        // TODO: RT-29488 we need to track the uses of the LCD buffer,
        // but the flow of control through the text methods is
        // not straight-forward enough for a simple set of lock/unlock
        // fixes at this time.
        lcdBuffer.makePermanent();
    }

    public void disposeLCDBuffer() {
        if (lcdBuffer != null) {
            lcdBuffer.dispose();
            lcdBuffer = null;
        }
    }

    @Override
    public RTTexture getLCDBuffer() {
        return lcdBuffer;
    }

    //Current RenderTarget is undefined after this method.
    public void validateLCDBuffer(RenderTarget renderTarget) {
        if (checkDisposed()) return;

        if (lcdBuffer == null ||
                lcdBuffer.getPhysicalWidth() < renderTarget.getPhysicalWidth() ||
                lcdBuffer.getPhysicalHeight() < renderTarget.getPhysicalHeight())
        {
            disposeLCDBuffer();
            initLCDBuffer(renderTarget.getPhysicalWidth(), renderTarget.getPhysicalHeight());
        }
    }

    abstract public void blit(RTTexture srcRTT, RTTexture dstRTT,
                          int srcX0, int srcY0, int srcX1, int srcY1,
                          int dstX0, int dstY0, int dstX1, int dstY1);

    @Override
    protected void setRenderTarget(RenderTarget target, NGCamera camera,
            boolean depthTest, boolean state3D)
    {
        if (checkDisposed()) return;

        if (target instanceof Texture) {
            ((Texture) target).assertLocked();
        }
        if (state == null ||
            state3D != state.lastState3D ||
            target != state.lastRenderTarget ||
            camera != state.lastCamera ||
            depthTest != state.lastDepthTest)
        {
            flushVertexBuffer();
            state = updateRenderTarget(target, camera, depthTest);
            state.lastRenderTarget = target;
            state.lastCamera = camera;
            state.lastDepthTest = depthTest;

            // the projection matrix is set in updateShaderTransform()
            // because it depends on the dimensions of the destination surface,
            // so if the RenderTarget is changing we force a call to the
            // updateShaderTransform() method by setting isXformValid=false
            state.isXformValid = false;

            // True if we switch between 2D and 3D primitives
            if (state3D != state.lastState3D) {
                state.lastState3D = state3D;
                state.lastShader = null;
                state.lastConst1 = Float.NaN;
                state.lastConst2 = Float.NaN;
                state.lastConst3 = Float.NaN;
                state.lastConst4 = Float.NaN;
                state.lastConst5 = Float.NaN;
                state.lastConst6 = Float.NaN;
                state.lastComp = null;
                state.lastClip = null;
                for (int i = 0; i != state.lastTextures.length; i++) {
                    state.lastTextures[i] = null;
                }
                if (state3D) {
                    // switch to 3D state
                    setDeviceParametersFor3D();
                } else {
                    // switch to 2D state
                    setDeviceParametersFor2D();
                }
            }
        }
    }

    @Override
    protected void releaseRenderTarget() {
        // Null out hard references that cause memory leak reported in RT-17304
        if (state != null) {
            state.lastRenderTarget = null;
            for (int i=0; i<state.lastTextures.length; i++) {
                state.lastTextures[i] = null;
            }
        }
    }

    private void disposeShaders(Shader[] shaders) {
        for (int i = 0; i < shaders.length; i++) {
            if (shaders[i] != null) {
                shaders[i].dispose();
                shaders[i] = null;
            }
        }
    }

    @Override
    public void dispose() {
        disposeShaders(stockShaders);
        disposeShaders(stockATShaders);
        disposeShaders(specialShaders);
        disposeShaders(specialATShaders);
        if (externalShader != null) {
            externalShader.dispose();
            externalShader = null;
        }

        disposeLCDBuffer();
        releaseRenderTarget();
        state = null;

        super.dispose();
    }
}
