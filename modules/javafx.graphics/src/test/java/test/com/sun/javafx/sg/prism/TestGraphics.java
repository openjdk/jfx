/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.sg.prism;

import java.nio.Buffer;
import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.MediaFrame;
import com.sun.prism.Mesh;
import com.sun.prism.MeshView;
import com.sun.prism.PhongMaterial;
import com.sun.prism.PixelFormat;
import com.sun.prism.Presentable;
import com.sun.prism.PresentableState;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BaseContext;
import com.sun.prism.impl.BaseGraphics;
import com.sun.prism.impl.TextureResourcePool;
import com.sun.prism.paint.Color;
import com.sun.prism.shape.ShapeRep;


/**
 *
 */
public class TestGraphics extends BaseGraphics {

    public static final Graphics TEST_GRAPHICS = new TestGraphics();

    public TestGraphics() {
        super(new TestContext(), new TestRenderTarget());
    }

    @Override
    protected void renderShape(Shape shape, BasicStroke stroke, float bx, float by, float bw, float bh) {
    }

    public void clear(Color color) {
    }

    public void clearQuad(float x1, float y1, float x2, float y2) {
    }

    public void fillQuad(float x1, float y1, float x2, float y2) {
    }

    public void fillRect(float x, float y, float width, float height) {
    }

    public void fillRoundRect(float x, float y, float width, float height, float arcw, float arch) {
    }

    public void fillEllipse(float x, float y, float width, float height) {
    }

    public void drawLine(float x1, float y1, float x2, float y2) {
    }

    public void drawRect(float x, float y, float width, float height) {
    }

    public void drawRoundRect(float x, float y, float width, float height, float arcw, float arch) {
    }

    public void drawEllipse(float x, float y, float width, float height) {
    }

    public void setNodeBounds(RectBounds bounds) {
    }

    @Override public void drawString(GlyphList gl, FontStrike strike, float x, float y, Color selectColor, int selectStart, int selectEnd) {
    }

    @Override public void drawTexture(Texture tex, float x, float y, float w, float h) {
    }

    @Override
    public void drawTexture(Texture tex, float dx1, float dy1, float dx2, float dy2, float sx1, float sy1, float sx2, float sy2) {
    }

    @Override
    public void drawTexture3SliceH(Texture tex, float dx1, float dy1, float dx2, float dy2, float sx1, float sy1, float sx2, float sy2, float dh1, float dh2, float sh1, float sh2) {
    }

    @Override
    public void drawTexture3SliceV(Texture tex, float dx1, float dy1, float dx2, float dy2, float sx1, float sy1, float sx2, float sy2, float dv1, float dv2, float sv1, float sv2) {
    }

    @Override
    public void drawTexture9Slice(Texture tex, float dx1, float dy1, float dx2, float dy2, float sx1, float sy1, float sx2, float sy2, float dh1, float dv1, float dh2, float dv2, float sh1, float sv1, float sh2, float sv2) {
    }

    public void sync() {
    }

    @Override
    public void setState3D(boolean flag) {
    }

    @Override
    public boolean isState3D() {
        return false;
    }

    public void setup3DRendering() {
    }

    public void blit(RTTexture srcTex, RTTexture dstTex, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1) {
    }

    private static class TestContext extends BaseContext {

        public TestContext() {
            super(null, new TestResourceFactory(), 32);
        }

        @Override
        protected void renderQuads(float[] coordArray, byte[] colorArray, int numVertices) {
        }

        @Override
        protected void setRenderTarget(RenderTarget target, NGCamera camera, boolean depthTest, boolean state3D) {
        }

        @Override
        public void validateClearOp(BaseGraphics g) {
        }

        @Override
        public void validatePaintOp(BaseGraphics g, BaseTransform xform, Texture maskTex, float bx, float by, float bw, float bh) {
        }

        @Override
        public void validateTextureOp(BaseGraphics g, BaseTransform xform, Texture src, PixelFormat format) {
        }

        @Override
        public RTTexture getLCDBuffer() {
            return null;
        }
    }

    private static class TestResourceFactory implements ResourceFactory {
        @Override public boolean isDeviceReady() { return true; }
        @Override public boolean isDisposed() { return false; }

        @Override public TextureResourcePool getTextureResourcePool() { return null; }
        @Override public Texture createTexture(Image image, Texture.Usage usageHint, WrapMode wrapMode) { return null; }
        @Override public Texture createTexture(Image image, Texture.Usage usageHint, Texture.WrapMode wrapMode, boolean useMipmap) { return null; }
        @Override public Texture createTexture(PixelFormat formatHint, Texture.Usage usageHint, WrapMode wrapMode, int w, int h) { return null; }
        @Override public Texture createTexture(PixelFormat formatHint, Texture.Usage usageHint, Texture.WrapMode wrapMode, int w, int h, boolean useMipmap) { return null; }
        @Override public Texture createTexture(MediaFrame frame) { return null; }
        @Override public boolean isCompatibleTexture(Texture tex) { return true; }
        @Override public boolean isWrapModeSupported(Texture.WrapMode mode) { return true; }
        @Override public Texture getCachedTexture(Image image, WrapMode wrapMode) { return null; }
        @Override public Texture getCachedTexture(Image image, WrapMode wrapMode, boolean useMipmap) { return null; }
        @Override public boolean isFormatSupported(PixelFormat format) { return false; }
        @Override public int getMaximumTextureSize() { return 0; }
        @Override public Texture createMaskTexture(int width, int height, Texture.WrapMode wrapMode) { return null; }
        @Override public Texture createFloatTexture(int width, int height) { return null; }
        @Override public int getRTTWidth(int w, WrapMode wrapMode) { return w; }
        @Override public int getRTTHeight(int h, WrapMode wrapMode) { return h; }
        @Override public RTTexture createRTTexture(final int width, final int height, Texture.WrapMode wrapMode) {
            return createRTTexture(width, height, wrapMode, false);
        }
        @Override public RTTexture createRTTexture(final int width, final int height, Texture.WrapMode wrapMode, boolean msaa) {
            return new RTTexture() {
                @Override public int[] getPixels() { return new int[0]; }
                @Override public boolean readPixels(Buffer pixels, int x, int y, int width, int height) { return false; }
                @Override public boolean readPixels(Buffer pixels) { return false; }
                @Override public boolean isVolatile() { return false; }
                @Override public boolean isSurfaceLost() { return false; }
                @Override public Screen getAssociatedScreen() { return null; }
                @Override public Graphics createGraphics() {
                    return new TestGraphics();
                }

                @Override public Texture getSharedTexture(WrapMode altMode) { return null; }
                @Override public boolean isOpaque() { return false; }
                @Override public PixelFormat getPixelFormat() { return null; }
                @Override public int getPhysicalWidth() { return width; }
                @Override public int getPhysicalHeight() { return height; }
                @Override public int getContentX() { return 0; }
                @Override public int getContentY() { return 0; }
                @Override public int getContentWidth() { return width; }
                @Override public int getContentHeight() { return height; }
                @Override public int getLastImageSerial() { return 0; }
                @Override public void setLastImageSerial(int serial) { }
                @Override public void update(Image img) { }
                @Override public void update(Image img, int dstx, int dsty) { }
                @Override public void update(Image img, int dstx, int dsty, int srcw, int srch) { }
                @Override public void update(Image img, int dstx, int dsty, int srcw, int srch, boolean skipFlush) { }
                @Override public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan, boolean skipFlush) { }
                @Override public void update(MediaFrame frame, boolean skipFlush) { }
                @Override public WrapMode getWrapMode() { return null; }
                @Override public boolean getLinearFiltering() { return false; }
                @Override public void setLinearFiltering(boolean linear) { }
                @Override public void dispose() { }
                @Override public void lock() { }
                @Override public void unlock() { }
                @Override public boolean isLocked() { return true; }
                @Override public int getLockCount() { return 1; }
                @Override public void assertLocked() { }
                @Override public void makePermanent() { }
                @Override public void contentsUseful() { }
                @Override public void contentsNotUseful() { }
                @Override public void setOpaque(boolean opaque) { }
                @Override public boolean isMSAA() { return false; }
                @Override public int getMaxContentWidth() { return getPhysicalWidth(); }
                @Override public int getMaxContentHeight() { return getPhysicalHeight(); }
                @Override public void setContentWidth(int contentWidth) { }
                @Override public void setContentHeight(int contentHeight) { }
                @Override public boolean getUseMipmap() { return false; }
            };
        }
        @Override public Presentable createPresentable(PresentableState pstate) { return null; }
        @Override public ShapeRep createPathRep() { return null; }
        @Override public ShapeRep createRoundRectRep() { return null; }
        @Override public ShapeRep createEllipseRep() { return null; }
        @Override public ShapeRep createArcRep() { return null; }
        @Override public void addFactoryListener(ResourceFactoryListener l) { }
        @Override public void removeFactoryListener(ResourceFactoryListener l) { }
        @Override public void dispose() { }

        @Override public PhongMaterial createPhongMaterial() { return null; }
        @Override public MeshView createMeshView(Mesh mesh) { return null; }
        @Override public Mesh createMesh() { return null; }
        @Override public void setRegionTexture(Texture texture) { }
        @Override public Texture getRegionTexture() { return null; }
        @Override public void setGlyphTexture(Texture texture) { }
        @Override public Texture getGlyphTexture() { return null; }
        @Override public boolean isSuperShaderAllowed() {return false; }

    }

    private static class TestRenderTarget implements RenderTarget {

        public Screen getAssociatedScreen() {
            return null;
        }

        public Graphics createGraphics() {
            return null;
        }

        public boolean isOpaque() {
            return true;
        }

        public void setOpaque(boolean opaque) {
        }

        public int getPhysicalWidth() {
            return 0;
        }

        public int getPhysicalHeight() {
            return 0;
        }

        public int getContentX() {
            return 0;
        }

        public int getContentY() {
            return 0;
        }

        public int getContentWidth() {
            return 0;
        }

        public int getContentHeight() {
            return 0;
        }

        public boolean isMSAA() {
            return false;
        }

    }

    @Override
    public void setLights(NGLightBase[] lights) {
    }

    @Override
    public NGLightBase[] getLights() {
        return null;
    }
}
