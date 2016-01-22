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

package com.sun.prism;

import java.nio.Buffer;

public interface Texture extends GraphicsResource {

    /**
     * A hint indicating whether the contents of the texture will
     * be changing frequently (DYNAMIC) or mostly stable for the lifetime
     * of the texture (STATIC).
     */
    public enum Usage {
        DEFAULT,
        DYNAMIC,
        STATIC
    }

    /**
     * Specifies the behavior when texels are accessed outside the physical
     * bounds of the texture.
     */
    public enum WrapMode {
        /**
         * CLAMP_NOT_NEEDED is used for applications where the caller knows
         * that the texture will never be sampled outside the center of the
         * first and last pixels in the content area of the texture.  For
         * this mode the fitting of the pixels to the physical area and the
         * hardware wrapping or clamping modes will not come into play.  The
         * most typical case for this is a back buffer or device-pixel cache
         * where the texture will always be rendered 1:1 on top of pixels
         * in the destination, but any case where the caller knows that the
         * samples will be limited to the "safe area" as described can use
         * this mode.
         */
        CLAMP_NOT_NEEDED,

        /**
         * CLAMP_TO_ZERO is used for applications where the area outside of
         * the defined pixels should interpolate from the edge pixel values
         * at the center of those edge pixels to fully transparent at the
         * center of the pixels just outside the content area, and then
         * remain fully transparent out to +/- infinity.
         * The most common uses will be cached renderings that might be
         * stretched, scaled, rotated, or otherwise be subjected to situations
         * where samples may be taken outside the "safe area" as described
         * for the CLAMP_NOT_NEEDED mode and those samples should fade to
         * zero (for instance, effect inputs and results).
         */
        CLAMP_TO_ZERO,

        /**
         * CLAMP_TO_EDGE is used for applications where the area outside of
         * the defined pixels should be clamped to the value of (the center
         * of) the edge pixels.
         * This is commonly used for rendering images in an ImageView or for
         * storing the colors of a NO_CYCLE gradient.
         */
        CLAMP_TO_EDGE,

        /**
         * REPEAT is used for applications where the pixels should be
         * repeated from edge to edge as per the GL_REPEAT mode.
         * This is typically used for ImagePattern, though it could be used
         * for REPEAT or REFLECT gradients with a properly initialized
         * color texture and with the caveat that the GL_REPEAT type of
         * math does not exactly match the Gradient REPEAT/REFLECT math.
         */
        REPEAT,

        /**
         * This value can be returned from the {@link #getWrapMode()} method
         * if the caller asked for {@link #CLAMP_TO_ZERO} and the mode was
         * not supported by the hardware (via GL_CLAMP_TO_BORDER for instance)
         * so a guard row of transparent pixels was included surrounding all
         * 4 sides of the content area.
         * The content coordinates will indicate the proper region to render
         * just the "true" content of the texture, but samples outside of
         * that area should accurately return fully transparent pixels if
         * they are included in the sampled areas.
         */
        CLAMP_TO_ZERO_SIMULATED(CLAMP_TO_ZERO),

        /**
         * This value can be returned from the {@link #getWrapMode()} method
         * if the caller asked for {@link #CLAMP_TO_EDGE} and the mode was
         * not supported by the hardware (due to the content area not fitting
         * tightly into the physical dimensions along one or both axes).
         * The resulting content will be packed tightly into the upper left
         * corner of the resulting texture and a copy of the last row and
         * column of pixels will be duplicated so that the samples from 0,0
         * to the content width and height will all return the correctly
         * clamped values, however sampling more than half a pixel past
         * the content width and height will not necessarily return clamped
         * values.  If the application needs the clamped values to be returned
         * out to +infinity on either the X or Y axis, then it should make
         * alternate arrangements to ensure that the data is fully padded to
         * the physical dimensions of the texture (currently only needed to
         * support NO_CYCLE gradients and the Rectangle Wrap texture, both of
         * which adjust for this limitation).
         */
        CLAMP_TO_EDGE_SIMULATED(CLAMP_TO_EDGE),

        /**
         * This value can be returned from the {@link #getWrapMode()} method
         * if the caller asked for {@link #REPEAT} and the mode was not
         * supported by the hardware (due to the content area not fitting
         * tightly into the physical dimensions along one or both axes).
         * The resulting content will be packed tightly into the upper left
         * corner of the resulting texture and a copy of the first row and
         * column of pixels will be duplicated and placed after the last
         * row and column so that samples from between the center of the
         * last row and column of pixels to the center of the next row or
         * column after that will interpolate back to the left or top edge
         * of the image.  The application should shift the texture coordinates
         * and restrict their access to texture coordinates in the range
         * [HALF_TEXEL, content_size+HALF_TEXEL] which will represent exactly
         * one whole cell of the infinite field of repeating copies of the
         * image, though shifted by half a texel in position.
         */
        REPEAT_SIMULATED(REPEAT);

        private WrapMode simulates;
        private WrapMode simulatedBy;
        private WrapMode(WrapMode simulates) {
            this.simulates = simulates;
            simulates.simulatedBy = this;
        }

        private WrapMode() {
        }

        public WrapMode simulatedVersion() {
            return simulatedBy;
        }

        public boolean isCompatibleWith(WrapMode requestedMode) {
            return (requestedMode == this ||
                    requestedMode == simulates ||
                    requestedMode == CLAMP_NOT_NEEDED);
        }
    }

    /**
     * Returns the {@code PixelFormat} of this texture.
     *
     * @return the {@code PixelFormat} of this texture
     */
    public PixelFormat getPixelFormat();

    /**
     * Returns the physical width of this texture, in pixels.  Note that the
     * physical size of a texture may be larger than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The physical width will be greater than or equal to the content width.
     *
     * @return the physical width of this texture, in pixels
     */
    public int getPhysicalWidth();

    /**
     * Returns the physical height of this texture, in pixels.  Note that the
     * physical size of a texture may be larger than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The physical height will be greater than or equal to the content height.
     *
     * @return the physical height of this texture, in pixels
     */
    public int getPhysicalHeight();

    /**
     * Returns the content x-origin of this texture relative to the upper-left
     * corner, in pixels.  This value will be greater than equal to zero.
     *
     * @return the content x-origin of this texture
     */
    public int getContentX();

    /**
     * Returns the content y-origin of this texture relative to the upper-left
     * corner, in pixels.  This value will be greater than equal to zero.
     *
     * @return the content y-origin of this texture
     */
    public int getContentY();

    /**
     * Returns the content width of this texture, in pixels.  Note that the
     * content size of a texture may be smaller than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The content width will be less than or equal to the content width.
     * <p>
     * For example, if the hardware does not support non-power-of-two textures,
     * and you call ResourceFactory.createTexture(400, 200), the returned
     * Texture will have physical dimensions of 512x256, but the content
     * dimensions will be 400x200.
     *
     * @return the content width of this texture, in pixels
     */
    public int getContentWidth();

    /**
     * Returns the content height of this texture, in pixels.  Note that the
     * content size of a texture may be smaller than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The content height will be less than or equal to the content height.
     * <p>
     * For example, if the hardware does not support non-power-of-two textures,
     * and you call ResourceFactory.createTexture(400, 200), the returned
     * Texture will have physical dimensions of 512x256, but the content
     * dimensions will be 400x200.
     *
     * @return the content height of this texture, in pixels
     */
    public int getContentHeight();


    /**
     * Returns the max content width of this texture, in pixels.  Note that the
     * content size of a texture may be smaller than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The content width will be less than or equal to the max content width.
     * <p>
     * For example, if the hardware does not support non-power-of-two textures,
     * and you call ResourceFactory.createTexture(400, 200), the returned
     * Texture will have physical dimensions of 512x256 and the max content
     * dimensions will be 512x256 minus any padding needed by the implementation
     * to simulate edge conditions., but the content dimensions will be 400x200.
     *
     * @return the max content width of this texture, in pixels
     */
    public int getMaxContentWidth();

    /**
     * Returns the max content height of this texture, in pixels.  Note that the
     * content size of a texture may be smaller than the requested size due
     * to hardware restrictions (e.g. lack of non-power-of-two texture support).
     * The content height will be less than or equal to the max content height.
     * <p>
     * For example, if the hardware does not support non-power-of-two textures,
     * and you call ResourceFactory.createTexture(400, 200), the returned
     * Texture will have physical dimensions of 512x256 and the max content
     * dimensions will be 512x256 minus any padding needed by the implementation
     * to simulate edge conditions., but the content dimensions will be 400x200.
     *
     * @return the max content height of this texture, in pixels
     */
    public int getMaxContentHeight();

    /**
     * Allows the content width, which is the current width of the actual content
     * in pixels, to be adjusted. The height must be between 0 and maxContentHeight
     *
     * @param contentWidth The actual new width of user pixels.
     */
    public void setContentWidth(int contentWidth);

    /**
     * Allows the content height, which is the current height of the actual content
     * in pixels, to be adjusted. The height must be between 0 and maxContentHeight
.    *
     * @param contentHeight The actual new height of user pixels.
     */
    public void setContentHeight(int contentHeight);



    public int getLastImageSerial();
    public void setLastImageSerial(int serial);

    /**
     * Updates this texture using the contents of the given {@code Image}.
     * The upper-left corner of the image data will be positioned
     * at (contentX, contentY) of the texture, and the full width and height
     * of the image will be uploaded.
     * This method will cause the vertex buffer to be flushed prior to
     * uploading the pixels.
     * <p>
     * This is equivalent to calling:
     * <code>
     * <pre>
     *     update(img, 0, 0);
     * </pre>
     * </code>
     *
     * @param img the image data to be uploaded to this texture
     */
    public void update(Image img);

    /**
     * Updates this texture using the contents of the given {@code Image}.
     * The upper-left corner of the image data will be positioned
     * at (contentX+dstx, contentY+dsty) of the texture, and the full width
     * and height of the image will be uploaded.
     * This method will cause the vertex buffer to be flushed prior to
     * uploading the pixels.
     * <p>
     * This is equivalent to calling:
     * <code>
     * <pre>
     *     update(img, dstx, dsty, img.getWidth(), img.getHeight());
     * </pre>
     * </code>
     *
     * @param img the image data to be uploaded to this texture
     * @param dstx the x-offset of the image data, in pixels, relative to the
     * contentX of this texture
     * @param dsty the y-offset of the image data, in pixels, relative to the
     * contentY of this texture
     */
    public void update(Image img, int dstx, int dsty);

    /**
     * Updates this texture using the contents of the given {@code Image}.
     * The upper-left corner of the image data will be positioned
     * at (contentX+dstx, contentY+dsty) of the texture, and the source
     * region to be uploaded will be {@code srcw} by {@code srch} pixels.
     * This method will cause the vertex buffer to be flushed prior to
     * uploading the pixels.
     * <p>
     * This is equivalent to calling:
     * <code>
     * <pre>
     *     update(img, dstx, dsty, srcw, srch, false);
     * </pre>
     * </code>
     *
     * @param img the image data to be uploaded to this texture
     * @param dstx the x-offset of the image data, in pixels, relative to the
     * contentX of this texture
     * @param dsty the y-offset of the image data, in pixels, relative to the
     * contentY of this texture
     * @param srcw the width of the pixel region from the source image
     * @param srch the height of the pixel region from the source image
     */
    public void update(Image img, int dstx, int dsty, int srcw, int srch);

    /**
     * Updates this texture using the contents of the given {@code Image}.
     * The upper-left corner of the image data will be positioned
     * at (contentX+dstx, contentY+dsty) of the texture, and the source
     * region to be uploaded will be {@code srcw} by {@code srch} pixels.
     * This method will cause the vertex buffer to be flushed unless
     * {@code skipFlush} is true.
     * <p>
     * This is equivalent to calling:
     * <code>
     * <pre>
     *     update(img.getPixelBuffer(), img.getPixelFormat(),
     *            dstx, dsty, img.getMinX(), img.getMinY(),
     *            srcw, srch, img.getScanlineStride(), skipFlush);
     * </pre>
     * </code>
     *
     * @param img the image data to be uploaded to this texture
     * @param dstx the x-offset of the image data, in pixels, relative to the
     * contentX of this texture
     * @param dsty the y-offset of the image data, in pixels, relative to the
     * contentY of this texture
     * @param srcw the width of the pixel region from the source image
     * @param srch the height of the pixel region from the source image
     * @param skipFlush if true, the vertex buffer will not be flushed
     */
    public void update(Image img, int dstx, int dsty, int srcw, int srch,
                       boolean skipFlush);

    /**
     * Updates this texture using the contents of the given {@code Buffer}.
     * The upper-left corner of the image data will be positioned
     * at (contentX+dstx, contentY+dsty) of the texture, and the source
     * region to be uploaded will be {@code srcw} by {@code srch} pixels.
     * This method will cause the vertex buffer to be flushed unless
     * {@code skipFlush} is true.
     *
     * @param pixels the image data to be uploaded to this texture
     * @param format the format of the data contained in the pixel buffer
     * @param dstx the x-offset of the image data, in pixels, relative to the
     * contentX of this texture
     * @param dsty the y-offset of the image data, in pixels, relative to the
     * contentY of this texture
     * @param srcx the x-offset into the source buffer, in pixels
     * @param srcy the y-offset into the source buffer, in pixels
     * @param srcw the width of the pixel region from the source buffer
     * @param srch the height of the pixel region from the source buffer
     * @param srcscan the scanline stride of the source buffer, in bytes
     * @param skipFlush if true, the vertex buffer will not be flushed
     */
    public void update(Buffer buffer, PixelFormat format,
                       int dstx, int dsty,
                       int srcx, int srcy,
                       int srcw, int srch, int srcscan,
                       boolean skipFlush);

    /**
     * Updates this texture using the contents of the provided
     * {@code MediaFrame}. The source and destination coordinates are implicit,
     * you can only update the entire video texture.
     * @param frame the source video buffer to update the texture data from
     * @param skipFlush if true, the vertex buffer will not be flushed
     */
    public void update(MediaFrame frame, boolean skipFlush);

    /**
     * Returns the {@code WrapMode} for this texture.
     *
     * @return the {@code WrapMode} for this texture
     */
    public WrapMode getWrapMode();

    /**
     * Returns the true if mipmapping is used for this texture.
     *
     * @return the {@code useMipmap} flag for this texture
     */
    public boolean getUseMipmap();

    /**
     * Constructs an alternate version of this {@code Texture} with an
     * alternate WrapMode if the two modes allow the underlying texture
     * to be shared, otherwise a null value is returned.
     * This method can only be used to create a shared texture for
     * {@code REPEAT} or {@code CLAMP_TO_EDGE} textures, which must
     * necessarily have content that spans their entire physical dimensions
     * (if their content was smaller then they would have a {@code _SIMULATED}
     * type of wrap mode).
     * This method expects the texture to be already locked (and checked for
     * a valid surface) and if it returns a non-null value then that return
     * value will have an outstanding lock in addition to retaining the lock
     * on the original texture.
     * Note that if the requested {@code WrapMode} is the same as the wrap
     * mode of this texture, then this same object will be returned after
     * having its lock count increased by 1.
     * Thus, in all cases, the caller is responsible for locking this texture
     * before the call, and eventually unlocking this texture after the call,
     * and also for eventually unlocking the return value if it is non-null.
     */
    public Texture getSharedTexture(WrapMode altMode);

    /**
     * Returns whether linear (smooth) filtering will be used when
     * rendering this texture.  If false, a simple nearest neighbor algorithm
     * will be used.
     *
     * @return whether linear filtering will be used for this texture
     */
    public boolean getLinearFiltering();

    /**
     * Sets whether linear filtering will be used when rendering this texture.
     *
     * @param linear if true, enables linear filtering; if false, enables
     * nearest neighbor filtering
     */
    public void setLinearFiltering(boolean linear);

    public void lock();
    public void unlock();
    public boolean isLocked();
    public int getLockCount();
    public void assertLocked();
    public void makePermanent();
    public void contentsUseful();
    public void contentsNotUseful();

    /**
     * Called by code wanting to know if the RTTexture's surface is lost. This happens
     * in some cases (mostly on Windows) when, for example, the user presses Ctrl+Alt+Delete,
     * or the system goes to sleep.
     * @return True if the backing surface of this RTTexture is gone and the image is therefore
     *         no longer usable. False if it is still OK.
     */
    public boolean isSurfaceLost();
}
