/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl;

import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * The abstract base class for all {@code Effect} implementation peers.
 *
 * @param <T> an optional subclass of RenderState that can be assumed as the
 *            return value for the getRenderState() method
 */
public abstract class EffectPeer<T extends RenderState> {

    private final FilterContext fctx;
    private final Renderer renderer;
    private final String uniqueName;
    private Effect effect;
    private T renderState;
    private int pass;

    protected EffectPeer(FilterContext fctx, Renderer renderer, String uniqueName) {
        if (fctx == null) {
            throw new IllegalArgumentException("FilterContext must be non-null");
        }
        this.fctx = fctx;
        this.renderer = renderer;
        this.uniqueName = uniqueName;
    }

    public boolean isImageDataCompatible(ImageData id) {
        return getRenderer().isImageDataCompatible(id);
    }

    public abstract ImageData filter(Effect effect,
                                     T renderState,
                                     BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputs);

    /**
     * Disposes resources associated with this peer.
     * Warning: may be called from the rendering thread.
     */
    public void dispose() {
    }

    public AccelType getAccelType() {
        return renderer.getAccelType();
    }

    protected final FilterContext getFilterContext() {
        return fctx;
    }

    protected Renderer getRenderer() {
        return renderer;
    }

    /**
     * Returns the unique name of this peer.  This value can be used as
     * the key value in a hashmap of cached peer instances.  In the case
     * of hardware peers, this value is typically the name of the shader that
     * is used by the peer.
     *
     * @return the unique name of this peer
     */
    public String getUniqueName() {
        return uniqueName;
    }

    protected Effect getEffect() {
        return effect;
    }

    protected void setEffect(Effect effect) {
        this.effect = effect;
    }

    protected T getRenderState() {
        return renderState;
    }

    protected void setRenderState(T renderState) {
        this.renderState = renderState;
    }

    public final int getPass() {
        return pass;
    }

    public void setPass(int pass) {
        this.pass = pass;
    }

    // NOTE: this input(Native)Bounds stuff is unpleasant, but we somehow
    // need to provide access to the native surface bounds for various glue
    // methods (e.g. getKvals())

    private final Rectangle[] inputBounds = new Rectangle[2];
    /**
     * Returns the "valid" bounds of the source image for the given input.
     * Since Effect implementations try to recycle temporary Images, it is
     * quite possible that the input bounds returned by this method will
     * differ from the size of the associated input Image.  For example,
     * this method may return (0, 0, 210, 180) even though the associated
     * Image has dimensions of 230x200 pixels.  Pixels in the input Image
     * outside these "valid" bounds are undefined and should be avoided.
     *
     * @param inputIndex the index of the source input
     * @return the valid bounds of the source Image
     */
    protected final Rectangle getInputBounds(int inputIndex) {
        return inputBounds[inputIndex];
    }
    protected final void setInputBounds(int inputIndex, Rectangle r) {
        inputBounds[inputIndex] = r;
    }

    private final BaseTransform[] inputTransforms = new BaseTransform[2];
    protected final BaseTransform getInputTransform(int inputIndex) {
        return inputTransforms[inputIndex];
    }
    protected final void setInputTransform(int inputIndex, BaseTransform tx) {
        inputTransforms[inputIndex] = tx;
    }

    private final Rectangle[] inputNativeBounds = new Rectangle[2];
    /**
     * Returns the bounds of the native surface for the given input.
     * It is quite possible that the input native bounds returned by this
     * method will differ from the size of the associated input (Java-level)
     * Image.  This is common for the OGL and D3D backends of Java 2D,
     * where on older hardware the dimensions of a VRAM surface (e.g. texture)
     * must be a power of two.  For example, this method may return
     * (0, 0, 256, 256) even though the associated (Volatile)Image has
     * dimensions of 230x200 pixels.
     * <p>
     * This method is useful in cases where it is necessary to access
     * adjacent pixels in a native surface.  For example, the horizontal
     * distance between two texel centers of a native surface can be
     * calculated as (1f/inputNativeBounds.width); for the vertical distance,
     * (1f/inputNativeBounds.height).
     *
     * @param inputIndex the index of the source input
     * @return the native surface bounds
     */
    protected final Rectangle getInputNativeBounds(int inputIndex) {
        return inputNativeBounds[inputIndex];
    }
    protected final void setInputNativeBounds(int inputIndex, Rectangle r) {
        inputNativeBounds[inputIndex] = r;
    }

    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        return getEffect().getResultBounds(transform, outputClip, inputDatas);
    }

    /**
     * Returns an array of four floats that represent the mapping of the
     * data for the specified input to the given effect area.
     * The interpretation of the returned values is entirely dependent on
     * the algorithm of the pixel shader, but typical values are in the
     * "unit" coordinate space of the destination effect area, where
     * {@code (0,0)} is at the upper-left corner, and {@code (1,1)} is at
     * the lower-right corner.
     * The returned array contains the values in order (x1, y1, x2, y2).
     * <p>
     * The default implementation converts the logical destination effect
     * region into the coordinate space of the native surface of the
     * specified source input according to the
     * {@link getSourceRegion(Rectangle, Rectangle, Rectangle)} method.
     * <p>
     * Subclasses can override this method to provide more sophisticated
     * positioning behavior.
     *
     * @param inputIndex the index of the source input
     * @return an array of four float values
     */
    protected float[] getSourceRegion(int inputIndex)
    {
        return getSourceRegion(getInputBounds(inputIndex),
                               getInputNativeBounds(inputIndex),
                               getDestBounds());
    }

    /**
     * Returns an array of four floats that represent the mapping of the
     * specified source region for the specified effect area.
     * The returned values are in the "unit" coordinate space of the source
     * native surface, where (0,0) is at the upper-left corner, and (1,1)
     * is at the lower-right corner.
     * For example, if the native input surface (i.e. texture) is 256x256
     * pixels, and the effect output region is at the same coordinates as
     * the input region and is 200x200, this method will
     * return (0, 0, 200/256, 220/256).
     * The returned array contains the values in order (x1, y1, x2, y2).
     * <p>
     * Subclasses can override this method to provide more sophisticated
     * positioning behavior.
     *
     * @param srcBounds the logical bounds of the input data
     * @param srcNativeBounds the actual dimensions of the input image
     *                        containing the input data in its upper left
     * @param dstBounds the logical bounds of the resulting effect output
     * @return an array of four float values
     */
    static float[] getSourceRegion(Rectangle srcBounds,
                                   Rectangle srcNativeBounds,
                                   Rectangle dstBounds)
    {
        float x1 = dstBounds.x - srcBounds.x;
        float y1 = dstBounds.y - srcBounds.y;
        float x2 = x1 + dstBounds.width;
        float y2 = y1 + dstBounds.height;
        float sw = srcNativeBounds.width;
        float sh = srcNativeBounds.height;
        return new float[] {x1 / sw, y1 / sh, x2 / sw, y2 / sh};
    }

    /**
     * Returns either 4 or 8 source texture coordinates depending on the
     * transform being applied to the source.
     * <p>
     * If the mapping is rectilinear then 4 floats are returned.  The
     * texture coordinates are thus mapped using the following table:
     * <pre>
     *     dx1,dy1 => ret[0], ret[1]
     *     dx2,dy1 => ret[2], ret[1]
     *     dx1,dy2 => ret[0], ret[3]
     *     dx2,dy2 => ret[2], ret[3]
     * </pre>
     * If the mapping is non-rectilinear then 8 floats are returned and
     * the texture coordinates are mapped using the following table (note
     * that the dx1,dy1 and dx2,dy2 mappings are still from the same
     * indices as in the 4 float return value):
     * <pre>
     *     dx1,dy1 => ret[0], ret[1]
     *     dx2,dy1 => ret[4], ret[5]
     *     dx1,dy2 => ret[6], ret[7]
     *     dx2,dy2 => ret[2], ret[3]
     * </pre>
     * The default implementation of this method simply calls the static
     * method {@link getTextureCoordinates(float[],float,float,float,float,Rectangle,BaseTransform)}.
     *
     * @param inputIndex the index of the input whose texture coordinates
     *                   are being queried
     * @param coords An array that can hold up to 8 floats for returning
     *               the texture coordinates.
     * @param srcX The X coordinate of the origin of the source texture
     *             in the untransformed coordinate space.
     * @param srcY The Y coordinate of the origin of the source texture
     *             in the untransformed coordinate space.
     * @param srcNativeWidth the native width of the source texture
     * @param srcNativeHeight the native height of the source texture
     * @param dstBounds the output bounds that the texture is
     *                  being stretched over
     * @param transform the transform to be implicitly applied to the
     *                  source texture as it is mapped onto the destination
     * @return the number of texture coordinates stored in the {@code coords}
     *         array (either 4 or 8)
     */
    public int getTextureCoordinates(int inputIndex, float coords[],
                                     float srcX, float srcY,
                                     float srcNativeWidth,
                                     float srcNativeHeight,
                                     Rectangle dstBounds,
                                     BaseTransform transform)
    {
        return getTextureCoordinates(coords,
                                     srcX, srcY,
                                     srcNativeWidth, srcNativeHeight,
                                     dstBounds, transform);
    }

    /**
     * Returns either 4 or 8 source texture coordinates depending on the
     * transform being applied to the source.
     * <p>
     * If the mapping is rectilinear then 4 floats are returned.  The
     * texture coordinates are thus mapped using the following table:
     * <pre>
     *     dx1,dy1 => ret[0], ret[1]
     *     dx2,dy1 => ret[2], ret[1]
     *     dx1,dy2 => ret[0], ret[3]
     *     dx2,dy2 => ret[2], ret[3]
     * </pre>
     * If the mapping is non-rectilinear then 8 floats are returned and
     * the texture coordinates are mapped using the following table (note
     * that the dx1,dy1 and dx2,dy2 mappings are still from the same
     * indices as in the 4 float return value):
     * <pre>
     *     dx1,dy1 => ret[0], ret[1]
     *     dx2,dy1 => ret[4], ret[5]
     *     dx1,dy2 => ret[6], ret[7]
     *     dx2,dy2 => ret[2], ret[3]
     * </pre>
     *
     * @param coords An array that can hold up to 8 floats for returning
     *               the texture coordinates.
     * @param srcX The X coordinate of the origin of the source texture
     *             in the untransformed coordinate space.
     * @param srcY The Y coordinate of the origin of the source texture
     *             in the untransformed coordinate space.
     * @param srcNativeWidth the native width of the source texture
     * @param srcNativeHeight the native height of the source texture
     * @param dstBounds the output bounds that the texture is
     *                  being stretched over
     * @param transform the transform to be implicitly applied to the
     *                  source texture as it is mapped onto the destination
     * @return the number of texture coordinates stored in the {@code coords}
     *         array (either 4 or 8)
     */
    public static int getTextureCoordinates(float coords[],
                                            float srcX, float srcY,
                                            float srcNativeWidth,
                                            float srcNativeHeight,
                                            Rectangle dstBounds,
                                            BaseTransform transform)
    {
        coords[0] = dstBounds.x;
        coords[1] = dstBounds.y;
        coords[2] = coords[0] + dstBounds.width;
        coords[3] = coords[1] + dstBounds.height;
        int numCoords;
        if (transform.isTranslateOrIdentity()) {
            srcX += (float) transform.getMxt();
            srcY += (float) transform.getMyt();
            numCoords = 4;
        } else {
            coords[4] = coords[2];
            coords[5] = coords[1];
            coords[6] = coords[0];
            coords[7] = coords[3];
            numCoords = 8;
            try {
                transform.inverseTransform(coords, 0, coords, 0, 4);
            } catch (NoninvertibleTransformException e) {
                coords[0] = coords[1] = coords[2] = coords[4] = 0f;
                return 4;
            }
        }
        for (int i = 0; i < numCoords; i += 2) {
            coords[i  ] = (coords[i  ] - srcX) / srcNativeWidth;
            coords[i+1] = (coords[i+1] - srcY) / srcNativeHeight;
        }
        return numCoords;
    }

    private Rectangle destBounds;
    protected final void setDestBounds(Rectangle r) {
        destBounds = r;
    }
    protected final Rectangle getDestBounds() {
        return destBounds;
    }

    private final Rectangle destNativeBounds = new Rectangle();
    protected final Rectangle getDestNativeBounds() {
        return destNativeBounds;
    }
    protected final void setDestNativeBounds(int w, int h) {
        destNativeBounds.width = w;
        destNativeBounds.height = h;
    }

    protected Object getSamplerData(int i) {
        return null;
    }

    /**
     * Returns true if the native coordinate system has its origin at
     * the upper-left corner of the destination surface; otherwise, returns
     * false, indicating that the origin is at the lower-left corner.
     * <p>
     * This method may be useful in determining the direction of adjacent
     * pixels in an OpenGL surface (since many OpenGL methods take parameters
     * assuming a lower-left origin).
     *
     * @return true if the coordinate system has an upper-left origin
     */
    protected boolean isOriginUpperLeft() {
        return (getAccelType() != Effect.AccelType.OPENGL);
    }
}
