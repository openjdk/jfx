/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NodePath;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

/**
 *
 */
public interface Graphics {

    public BaseTransform getTransformNoClone();
    public void setTransform(BaseTransform xform);
    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12);
    public void setTransform3D(double mxx, double mxy, double mxz, double mxt,
                               double myx, double myy, double myz, double myt,
                               double mzx, double mzy, double mzz, double mzt);
    public void transform(BaseTransform xform);
    public void translate(float tx, float ty);
    public void translate(float tx, float ty, float tz);
    public void scale(float sx, float sy);
    public void scale(float sx, float sy, float sz);

    public void setPerspectiveTransform(GeneralTransform3D perspectiveTransform);
    public void setCamera(NGCamera camera);
    public NGCamera getCameraNoClone();
    public void setDepthTest(boolean depthTest);
    public boolean isDepthTest();
    public void setDepthBuffer(boolean depthBuffer);
    public boolean isDepthBuffer();
    public boolean isAlphaTestShader();
    public void setAntialiasedShape(boolean aa);
    public boolean isAntialiasedShape();
    public RectBounds getFinalClipNoClone();
    public Rectangle getClipRect();
    public Rectangle getClipRectNoClone();
    public void setHasPreCullingBits(boolean hasBits);
    public boolean hasPreCullingBits();
    public void setClipRect(Rectangle clipRect);
    public void setClipRectIndex(int index);
    public int getClipRectIndex();
    public float getExtraAlpha();
    public void setExtraAlpha(float extraAlpha);
    public void setLights(NGLightBase[] lights);
    public NGLightBase[] getLights();
    public Paint getPaint();
    public void setPaint(Paint paint);
    public BasicStroke getStroke();
    public void setStroke(BasicStroke stroke);
    public void setCompositeMode(CompositeMode mode);
    public CompositeMode getCompositeMode();

    /**
     * Clears the current {@code RenderTarget} with transparent pixels.
     * Note that this operation is affected by the current clip rectangle,
     * if set.  To clear the entire surface, call {@code setClipRect(null)}
     * prior to calling {@code clear()}.
     * <p>
     * This is equivalent to calling:
     * <code>
     * <pre>
     *     clear(Color.TRANSPARENT);
     * </pre>
     * </code>
     */
    public void clear();
    /**
     * Clears the current {@code RenderTarget} with the given {@code Color}.
     * Note that this operation is affected by the current clip rectangle,
     * if set.  To clear the entire surface, call {@code setClipRect(null)}
     * prior to calling {@code clear()}.
     */
    public void clear(Color color);
    /**
     * Clears the region represented by the given quad with transparent pixels.
     * Note that this operation is affected by the current clip rectangle,
     * if set, as well as the current transform (the quad is specified in
     * user space).  Also note that unlike the {@code clear()} methods, this
     * method does not attempt to clear the depth buffer.
     */
    public void clearQuad(float x1, float y1, float x2, float y2);

    public void fill(Shape shape);
    public void fillQuad(float x1, float y1, float x2, float y2);
    public void fillRect(float x, float y, float width, float height);
    public void fillRoundRect(float x, float y, float width, float height, float arcw, float arch);
    public void fillEllipse(float x, float y, float width, float height);
    public void draw(Shape shape);
    public void drawLine(float x1, float y1, float x2, float y2);
    public void drawRect(float x, float y, float width, float height);
    public void drawRoundRect(float x, float y, float width, float height, float arcw, float arch);
    public void drawEllipse(float x, float y, float width, float height);

    /**
     * Set the node bounds for any node that would like to render objects of a
     * different size to self.
     *
     * This is useful for proportional paints, where by definition proportional
     * paint should be stretched across node bounds to which it has been
     * applied.
     *
     * In most cases nodes/shapes are rendered in a single draw call, and if
     * needed bounds can be determined by inspecting object to be drawn.
     * However not all nodes are so simple, NGText for example can have
     * hundreds of Graphics.drawString(...), draw(...), and fill(...) calls per
     * a single NGText.renderContent().
     *
     * Use: Before making several draw calls from a single node,
     * setNodeBounds(bounds), when complete invalidate nodeBounds by
     * setNodeBounds(null).
     *
     * @param bounds should not include node transform
     */
    public void setNodeBounds(RectBounds bounds);
    public void drawString(GlyphList gl, FontStrike strike, float x, float y,
                           Color selectColor, int selectStart, int selectEnd);

    public void blit(RTTexture srcTex, RTTexture dstTex,
                     int srcX0, int srcY0, int srcX1, int srcY1,
                     int dstX0, int dstY0, int dstX1, int dstY1);
    public void drawTexture(Texture tex, float x, float y, float w, float h);
    public void drawTexture(Texture tex,
                            float dx1, float dy1, float dx2, float dy2,
                            float sx1, float sy1, float sx2, float sy2);
    public void drawTexture3SliceH(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dh1, float dh2, float sh1, float sh2);
    public void drawTexture3SliceV(Texture tex,
                                   float dx1, float dy1, float dx2, float dy2,
                                   float sx1, float sy1, float sx2, float sy2,
                                   float dv1, float dv2, float sv1, float sv2);
    public void drawTexture9Slice(Texture tex,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float sx1, float sy1, float sx2, float sy2,
                                  float dh1, float dv1, float dh2, float dv2,
                                  float sh1, float sv1, float sh2, float sv2);
    public void drawTextureVO(Texture tex,
                              float topopacity, float botopacity,
                              float dx1, float dy1, float dx2, float dy2,
                              float sx1, float sy1, float sx2, float sy2);
    public void drawTextureRaw(Texture tex,
                               float dx1, float dy1, float dx2, float dy2,
                               float tx1, float ty1, float tx2, float ty2);
    public void drawMappedTextureRaw(Texture tex,
                                     float dx1, float dy1, float dx2, float dy2,
                                     float tx11, float ty11, float tx21, float ty21,
                                     float tx12, float ty12, float tx22, float ty22);

    /**
     * Synchronize, or flush, any outstanding rendering operations to the
     * destination in preparation for some caller potentially reusing or
     * disposing a resource that has been used as the source of a recently
     * invoked rendering operation.
     *
     * This method does not (yet?) guarantee that the actual pixels in the
     * destination will be updated - it simply guarantees that no currently
     * buffered rendering will depend on any texture data included by
     * reference.  The typical usage is before a {@code Texture} disposal
     * or "return to texture cache pool" operation.
     */
    public void sync();

    public Screen getAssociatedScreen();
    public ResourceFactory getResourceFactory();
    public RenderTarget getRenderTarget();

    public void setRenderRoot(NodePath root);
    public NodePath getRenderRoot();

    public void setState3D(boolean flag);
    public boolean isState3D();

    // TODO: 3D Get better name, and may want to move this into node render method.
    // TODO this is dangerous, must be called *after* setState3D is called, or it won't work
    public void setup3DRendering();

    public void setPixelScaleFactors(float pixelScaleX, float pixelScaleY);
    public float getPixelScaleFactorX();
    public float getPixelScaleFactorY();
}
