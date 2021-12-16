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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.AffineBase;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGPerspectiveCamera;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.BufferUtil;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.RadialGradient;
import com.sun.prism.paint.Stop;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderGraphics;
import java.util.WeakHashMap;

class PaintHelper {

/****************** Shared MultipleGradientPaint support ********************/

    /**
     * The maximum number of gradient "stops" supported by our native
     * fragment shader implementations.
     *
     * This value has been empirically determined and capped to allow
     * our native shaders to run on all shader-level graphics hardware,
     * even on the older, more limited GPUs.  Even the oldest Nvidia
     * hardware could handle 16, or even 32 fractions without any problem.
     * But the first-generation boards from ATI would fall back into
     * software mode (which is unusably slow) for values larger than 12;
     * it appears that those boards do not have enough native registers
     * to support the number of array accesses required by our gradient
     * shaders.  So for now we will cap this value at 12, but we can
     * re-evaluate this in the future as hardware becomes more capable.
     */
    static final int MULTI_MAX_FRACTIONS = 12;

    /**
     * Make the texture width a power of two value larger
     * than MULTI_MAX_FRACTIONS.
     */
    private static final int MULTI_TEXTURE_SIZE = 16;
    private static final int MULTI_CACHE_SIZE = 256;
    private static final int GTEX_CLR_TABLE_SIZE = 101; // for every % from 0% to 100%
    private static final int GTEX_CLR_TABLE_MIRRORED_SIZE =
        GTEX_CLR_TABLE_SIZE * 2 - 1;

    private static final float FULL_TEXEL_Y = 1.0f / MULTI_CACHE_SIZE;
    private static final float HALF_TEXEL_Y = FULL_TEXEL_Y / 2.0f;

    private static final FloatBuffer stopVals =
        BufferUtil.newFloatBuffer(MULTI_MAX_FRACTIONS * 4);
    private static final ByteBuffer bgraColors =
        BufferUtil.newByteBuffer(MULTI_TEXTURE_SIZE*4);
    private static final Image colorsImg =
        Image.fromByteBgraPreData(bgraColors, MULTI_TEXTURE_SIZE, 1);
    private static final int[] previousColors = new int[MULTI_TEXTURE_SIZE];

    private static final byte gtexColors[] = new byte[GTEX_CLR_TABLE_MIRRORED_SIZE * 4];
    private static final Image gtexImg =
        Image.fromByteBgraPreData(ByteBuffer.wrap(gtexColors), GTEX_CLR_TABLE_MIRRORED_SIZE, 1);

    private static long cacheOffset = -1;

    private static Texture gradientCacheTexture = null;
    private static Texture gtexCacheTexture = null;

    /**
     * The keySet of this map is used to track the gradients that have
     * a valid entry (offset) in the gradient cache. We need this so that we can
     * invalidate all entries when a device is lost (at which time we recreate
     * the gradient cache textures). This prevents using a stale offset that is
     * no longer valid.
     * The values in this map are unused.
     */
    private static final WeakHashMap<Gradient, Void> gradientMap = new WeakHashMap<>();

    private static final Affine2D scratchXform2D = new Affine2D();
    private static final Affine3D scratchXform3D = new Affine3D();

    private static float len(float dx, float dy) {
        return ((dx == 0f) ? Math.abs(dy)
                : ((dy == 0f) ? Math.abs(dx)
                   : (float)Math.sqrt(dx * dx + dy * dy)));
    }

    static void initGradientTextures(ShaderGraphics g) {
        // We must clear cached gradient texture and offsets when the
        // device is removed and recreated
        cacheOffset = -1;
        gradientMap.clear();

        gradientCacheTexture = g.getResourceFactory().createTexture(
                PixelFormat.BYTE_BGRA_PRE, Usage.DEFAULT, WrapMode.CLAMP_TO_EDGE,
                MULTI_TEXTURE_SIZE, MULTI_CACHE_SIZE);
        gradientCacheTexture.setLinearFiltering(true);
        // gradientCacheTexture remains permanently locked, useful, and permanent
        // an additional lock is added when a caller calls getGreientTeture for
        // them to unlock
        gradientCacheTexture.contentsUseful();
        gradientCacheTexture.makePermanent();

        gtexCacheTexture = g.getResourceFactory().createTexture(
                PixelFormat.BYTE_BGRA_PRE, Usage.DEFAULT, WrapMode.CLAMP_NOT_NEEDED,
                GTEX_CLR_TABLE_MIRRORED_SIZE, MULTI_CACHE_SIZE);
        gtexCacheTexture.setLinearFiltering(true);
        // gtexCacheTexture remains permanently locked, useful, and permanent
        // an additional lock is added when a caller calls getWrapGreientTeture for
        // them to unlock
        gtexCacheTexture.contentsUseful();
        gtexCacheTexture.makePermanent();
    }

    static Texture getGradientTexture(ShaderGraphics g, Gradient paint) {
        if (gradientCacheTexture == null || gradientCacheTexture.isSurfaceLost()) {
            initGradientTextures(g);
        }

        // gradientCacheTexture is left permanent and locked, although we still
        // must check for isSurfaceLost() in case the device is disposed.
        // We add a lock here so that the caller can unlock without knowing
        // our inner implementation details.
        gradientCacheTexture.lock();
        return gradientCacheTexture;
    }

    static Texture getWrapGradientTexture(ShaderGraphics g) {
        if (gtexCacheTexture == null || gtexCacheTexture.isSurfaceLost()) {
            initGradientTextures(g);
        }

        // gtexCacheTexture is left permanent and locked, although we still
        // must check for isSurfaceLost() in case the device is disposed.
        // We add a lock here so that the caller can unlock without knowing
        // our inner implementation details.
        gtexCacheTexture.lock();
        return gtexCacheTexture;
    }

    private static void stopsToImage(List<Stop> stops, int numStops)
    {
        if (numStops > MULTI_MAX_FRACTIONS) {
            throw new RuntimeException(
                "Maximum number of gradient stops exceeded " +
                "(paint uses " + numStops +
                " stops, but max is " + MULTI_MAX_FRACTIONS + ")");
        }

        bgraColors.clear();
        Color lastColor = null;
        for (int i = 0; i < MULTI_TEXTURE_SIZE; i++) {
            Color c;
            if (i < numStops) {
                c = stops.get(i).getColor();
                lastColor = c;
            } else {
                // repeat the last color for the remaining slots so that
                // we can simply reference the last pixel of the texture when
                // dealing with edge conditions
                c = lastColor;
            }
            c.putBgraPreBytes(bgraColors);

            // optimization: keep track of colors used each time so that
            // we can skip updating the texture if the colors are same as
            // the last time
            int argb = c.getIntArgbPre();
            if (argb != previousColors[i]) {
                previousColors[i] = argb;
            }
        }
        bgraColors.rewind();
    }

    private static void insertInterpColor(byte colors[], int index,
                                          Color c0, Color c1, float t)
    {
        t *= 255.0f;
        float u = 255.0f - t;
        index *= 4;
        colors[index + 0] = (byte) (c0.getBluePremult()  * u + c1.getBluePremult()  * t + 0.5f);
        colors[index + 1] = (byte) (c0.getGreenPremult() * u + c1.getGreenPremult() * t + 0.5f);
        colors[index + 2] = (byte) (c0.getRedPremult()   * u + c1.getRedPremult()   * t + 0.5f);
        colors[index + 3] = (byte) (c0.getAlpha()        * u + c1.getAlpha()        * t + 0.5f);
    }

    private static Color PINK = new Color(1.0f, 0.078431375f, 0.5764706f, 1.0f);

    private static void stopsToGtexImage(List<Stop> stops, int numStops) {
        Color lastColor = stops.get(0).getColor();
        float offset = stops.get(0).getOffset();
        int lastIndex = (int) (offset * (GTEX_CLR_TABLE_SIZE - 1) + 0.5f);
        insertInterpColor(gtexColors, 0, lastColor, lastColor, 0.0f);
        for (int i = 1; i < numStops; i++) {
            Color color = stops.get(i).getColor();
            offset = stops.get(i).getOffset();
            int index = (int) (offset * (GTEX_CLR_TABLE_SIZE - 1) + 0.5f);
            if (index == lastIndex) {
                insertInterpColor(gtexColors, index, lastColor, color, 0.5f);
            } else {
                for (int j = lastIndex+1; j <= index; j++) {
                    float t = j - lastIndex;
                    t /= (index - lastIndex);
                    insertInterpColor(gtexColors, j, lastColor, color, t);
                }
            }
            lastIndex = index;
            lastColor = color;
        }
        // assert (lastIndex = GTEX_CLR_TABLE_SIZE);
        // now mirror the list for fast REFLECT calculations
        // mirroring is around index = (GTEX_CLR_TABLE_SIZE-1) which is
        // where the last color for fract=1.0 should have been stored
        for (int i = 1; i < GTEX_CLR_TABLE_SIZE; i++) {
            int j = (GTEX_CLR_TABLE_SIZE - 1 + i) * 4;
            int k = (GTEX_CLR_TABLE_SIZE - 1 - i) * 4;
            gtexColors[j + 0] = gtexColors[k + 0];
            gtexColors[j + 1] = gtexColors[k + 1];
            gtexColors[j + 2] = gtexColors[k + 2];
            gtexColors[j + 3] = gtexColors[k + 3];
        }
    }

    // Uses a least recently allocated algorithm for caching Gradient colors.
    // This could be optimized so that we never use the same color twice.
    // We always increment the cacheOffset (long) and keep the gradients stored
    // the cache in the range [cacheOffset - cacheSize + 1, cacheOffset]..
    public static int initGradient(Gradient paint) {
        long offset = paint.getGradientOffset();
        if (gradientMap.containsKey(paint) && offset >= 0 && (offset > cacheOffset - MULTI_CACHE_SIZE)) {
            return (int) (offset % MULTI_CACHE_SIZE);
        } else {
            List<Stop> stops = paint.getStops();
            int numStops = paint.getNumStops();
            stopsToImage(stops,numStops);
            stopsToGtexImage(stops, numStops);
            long nextOffset = ++cacheOffset;
            paint.setGradientOffset(nextOffset);
            int cacheIdx = (int)(nextOffset % MULTI_CACHE_SIZE);
            // both gradientCacheTexture and gtexCacheTexture should be
            // left permanent and locked so we can always call update on
            // either or both of them here.
            gradientCacheTexture.update(colorsImg, 0, cacheIdx);
            gtexCacheTexture.update(gtexImg, 0, cacheIdx);
            gradientMap.put(paint, null);
            return cacheIdx;
        }
    }

    private static void setMultiGradient(Shader shader,
                                         Gradient paint)
    {
        List<Stop> stops = paint.getStops();
        int numStops = paint.getNumStops();

        stopVals.clear();
        for (int i = 0; i < MULTI_MAX_FRACTIONS; i++) {
            // TODO: optimize this... (RT-27377)
            stopVals.put((i < numStops)   ?
                         stops.get(i).getOffset() : 0f);
            stopVals.put((i < numStops-1) ?
                         1f / (stops.get(i+1).getOffset() - stops.get(i).getOffset()) : 0f);
            stopVals.put(0f); // unused
            stopVals.put(0f); // unused
        }
        stopVals.rewind();
        shader.setConstants("fractions", stopVals, 0, MULTI_MAX_FRACTIONS);
        float index_y = initGradient(paint);
        shader.setConstant("offset", index_y / (float)MULTI_CACHE_SIZE + HALF_TEXEL_Y);

        // Note that the colors image/texture has already been updated
        // in BaseShaderContext.validatePaintOp()...
    }

    private static void setTextureGradient(Shader shader,
                                           Gradient paint)
    {
        float cy = initGradient(paint) + 0.5f;
        float cx = 0.5f;
        float fractmul = 0.0f, clampmul = 0.0f;
        switch (paint.getSpreadMethod()) {
            case Gradient.PAD:
                // distance from 0.5 texels to TABLE_SIZE - 0.5 texels
                clampmul = GTEX_CLR_TABLE_SIZE - 1.0f;
                break;
            case Gradient.REPEAT:
                // distance from 0.5 texels to TABLE_SIZE - 0.5 texels
                fractmul = GTEX_CLR_TABLE_SIZE - 1.0f;
                break;
            case Gradient.REFLECT:
                // distance from 0.5 texels to MIRROR_TABLE_SIZE - 0.5 texels
                fractmul = GTEX_CLR_TABLE_MIRRORED_SIZE - 1.0f;
                break;
        }
        float xscale = 1.0f / gtexCacheTexture.getPhysicalWidth();
        float yscale = 1.0f / gtexCacheTexture.getPhysicalHeight();
        cx *= xscale;
        cy *= yscale;
        fractmul *= xscale;
        clampmul *= xscale;
        shader.setConstant("content", cx, cy, fractmul, clampmul);

        // Note that the colors image/texture has already been updated
        // in BaseShaderContext.validatePaintOp()...
    }

/********************** LinearGradientPaint support *************************/

    /**
     * This method uses techniques that are nearly identical to those
     * employed in setGradientPaint() above.  The primary difference
     * is that at the native level we use a fragment shader to manually
     * apply the plane equation constants to the current fragment position
     * to calculate the gradient position in the range [0,1] (the native
     * code for GradientPaint does the same, except that it uses OpenGL's
     * automatic texture coordinate generation facilities). We Also, project
     * in the 3D case to create a perspective vector which is used in the
     * fragment shader.
     *
     * One other minor difference worth mentioning is that
     * setGradientPaint() calculates the plane equation constants
     * such that the gradient end points are positioned at 0.25 and 0.75
     * (for reasons discussed in the comments for that method).  In
     * contrast, for LinearGradientPaint we setup the equation constants
     * such that the gradient end points fall at 0.0 and 1.0.  The
     * reason for this difference is that in the fragment shader we
     * have more control over how the gradient values are interpreted
     * (depending on the paint's CycleMethod).
     */
    static void setLinearGradient(ShaderGraphics g,
                                  Shader shader,
                                  LinearGradient paint,
                                  float rx, float ry, float rw, float rh)
    {
        BaseTransform paintXform = paint.getGradientTransformNoClone();
        Affine3D at = scratchXform3D;
        g.getPaintShaderTransform(at);

        if (paintXform != null) {
            at.concatenate(paintXform);
        }

        float x1 = rx + (paint.getX1() * rw);
        float y1 = ry + (paint.getY1() * rh);
        float x2 = rx + (paint.getX2() * rw);
        float y2 = ry + (paint.getY2() * rh);

        // calculate plane equation constants
        float x = x1;
        float y = y1;
        at.translate(x, y);
        // now gradient point 1 is at the origin
        x = x2 - x;
        y = y2 - y;
        double len = len(x, y);

        at.rotate(Math.atan2(y, x));
        // now gradient point 2 is on the positive x-axis
        at.scale(len, 1);
        // now gradient point 1 is at (0.0, 0), point 2 is at (1.0, 0)

        double p0, p1, p2;

        if (!at.is2D()) {
            BaseTransform inv;
            try {
                inv = at.createInverse();
            } catch (NoninvertibleTransformException e) {
                at.setToScale(0, 0, 0);
                inv = at;
            }

            NGCamera cam = g.getCameraNoClone();
            Vec3d tmpVec = new Vec3d();
            PickRay tmpvec = new PickRay();

            PickRay ray00 = project(0,0,cam,inv,tmpvec,tmpVec,null);
            PickRay ray10 = project(1,0,cam,inv,tmpvec,tmpVec,null);
            PickRay ray01 = project(0,1,cam,inv,tmpvec,tmpVec,null);

            p0 = ray10.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            p1 = ray01.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            p2 = ray00.getDirectionNoClone().x;

            p0 *= -ray00.getOriginNoClone().z;
            p1 *= -ray00.getOriginNoClone().z;
            p2 *= -ray00.getOriginNoClone().z;

            double wv0 = ray10.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv1 = ray01.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv2 = ray00.getDirectionNoClone().z;

            shader.setConstant("gradParams", (float)p0, (float)p1, (float)p2, (float)ray00.getOriginNoClone().x);
            shader.setConstant("perspVec", (float)wv0, (float)wv1, (float)wv2);
        } else {
            try {
                at.invert();
            } catch (NoninvertibleTransformException ex) {
                at.setToScale(0, 0, 0);
            }
            p0 = (float)at.getMxx();
            p1 = (float)at.getMxy();
            p2 = (float)at.getMxt();
            shader.setConstant("gradParams", (float)p0, (float)p1, (float)p2, 0.0f);
            shader.setConstant("perspVec", 0.0f, 0.0f, 1.0f);
        }

        setMultiGradient(shader, paint);
    }

    static AffineBase getLinearGradientTx(LinearGradient paint,
                                          Shader shader,
                                          BaseTransform renderTx,
                                          float rx, float ry, float rw, float rh)
    {
        AffineBase ret;

        float x1 = paint.getX1();
        float y1 = paint.getY1();
        float x2 = paint.getX2();
        float y2 = paint.getY2();
        if (paint.isProportional()) {
            x1 = rx + x1 * rw;
            y1 = ry + y1 * rh;
            x2 = rx + x2 * rw;
            y2 = ry + y2 * rh;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = len(dx, dy);
        if (paint.getSpreadMethod() == Gradient.REFLECT) {
            len *= 2.0f;
        }

        BaseTransform paintXform = paint.getGradientTransformNoClone();
        if (paintXform.isIdentity() && renderTx.isIdentity()) {
            Affine2D at = scratchXform2D;

            // calculate plane equation constants
            at.setToTranslation(x1, y1);
            // now gradient point 1 is at the origin
            at.rotate(dx, dy);
            // now gradient point 2 is on the positive x-axis
            at.scale(len, 1);
            // now 0,0 maps to gradient point 1 and 1,0 maps to point 2

            ret = at;
        } else {
            Affine3D at = scratchXform3D;
            at.setTransform(renderTx);
            at.concatenate(paintXform);

            // calculate plane equation constants
            at.translate(x1, y1);
            // now gradient point 1 is at the origin
            at.rotate(Math.atan2(dy, dx));
            // now gradient point 2 is on the positive x-axis
            at.scale(len, 1);
            // now 0,0 maps to gradient point 1 and 1,0 maps to point 2

            ret = at;
        }

        try {
            ret.invert();
        } catch (NoninvertibleTransformException e) {
            scratchXform2D.setToScale(0, 0);
            ret = scratchXform2D;
        }

        setTextureGradient(shader, paint);

        return ret;
    }

/********************** RadialGradientPaint support *************************/

    /**
     * This method calculates six m** values and a focus adjustment value that
     * are used by the native fragment shader. (See LinearGradient Comment for
     * the 3D case.) These techniques are based on a whitepaper by Daniel Rice
     * on radial gradient performance (attached to the bug report for 6521533).
     * One can refer to that document for the complete set of formulas and
     * calculations, but the basic goal is to compose a transform that will
     * convert an (x,y) position in device space into a "u" value that represents
     * the relative distance to the gradient focus point.  The resulting
     * value can be used to look up the appropriate color by linearly
     * interpolating between the two nearest colors in the gradient.
     */
    static void setRadialGradient(ShaderGraphics g,
                                  Shader shader,
                                  RadialGradient paint,
                                  float rx, float ry, float rw, float rh)
    {
        Affine3D at = scratchXform3D;
        g.getPaintShaderTransform(at);

        // save original (untransformed) center and focus points and
        // adjust to account for proportional attribute if necessary
        float radius = paint.getRadius();
        float cx = paint.getCenterX();
        float cy = paint.getCenterY();
        float fa = paint.getFocusAngle();
        float fd = paint.getFocusDistance();
        if (fd < 0) {
            fd = -fd;
            fa = fa+180;
        }
        fa = (float) Math.toRadians(fa);
        if (paint.isProportional()) {
            float bcx = rx + (rw / 2f);
            float bcy = ry + (rh / 2f);
            float scale = Math.min(rw, rh);
            cx = (cx - 0.5f) * scale + bcx;
            cy = (cy - 0.5f) * scale + bcy;
            if (rw != rh && rw != 0f && rh != 0f) {
                at.translate(bcx, bcy);
                at.scale(rw / scale, rh / scale);
                at.translate(-bcx, -bcy);
            }
            radius = radius * scale;
        }

        // transform from gradient coords to device coords
        BaseTransform paintXform = paint.getGradientTransformNoClone();
        if (paintXform != null) {
            at.concatenate(paintXform);
        }

        // transform unit circle to gradient coords; we start with the
        // unit circle (center=(0,0), focus on positive x-axis, radius=1)
        // and then transform into gradient space
        at.translate(cx, cy);
        at.rotate(fa);
        at.scale(radius, radius);

            // invert to get mapping from device coords to unit circle
        try {
            at.invert();
        } catch (Exception e) {
            at.setToScale(0.0, 0.0, 0.0);
        }

        if (!at.is2D()) {
            NGCamera cam = g.getCameraNoClone();
            Vec3d tmpVec = new Vec3d();
            PickRay tmpvec = new PickRay();

            PickRay ray00 = project(0, 0, cam, at, tmpvec, tmpVec, null);
            PickRay ray10 = project(1, 0, cam, at, tmpvec, tmpVec, null);
            PickRay ray01 = project(0, 1, cam, at, tmpvec, tmpVec, null);

            double p0 = ray10.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            double p1 = ray01.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            double p2 = ray00.getDirectionNoClone().x;

            double py0 = ray10.getDirectionNoClone().y - ray00.getDirectionNoClone().y;
            double py1 = ray01.getDirectionNoClone().y - ray00.getDirectionNoClone().y;
            double py2 = ray00.getDirectionNoClone().y;

            p0 *= -ray00.getOriginNoClone().z;
            p1 *= -ray00.getOriginNoClone().z;
            p2 *= -ray00.getOriginNoClone().z;

            py0 *= -ray00.getOriginNoClone().z;
            py1 *= -ray00.getOriginNoClone().z;
            py2 *= -ray00.getOriginNoClone().z;

            double wv0 = ray10.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv1 = ray01.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv2 = ray00.getDirectionNoClone().z;

            shader.setConstant("perspVec", (float) wv0, (float) wv1, (float) wv2);
            shader.setConstant("m0", (float) p0, (float) p1, (float) p2, (float) ray00.getOriginNoClone().x);
            shader.setConstant("m1", (float) py0, (float) py1, (float) py2, (float) ray00.getOriginNoClone().y);
        } else {
            float m00 = (float) at.getMxx();
            float m01 = (float) at.getMxy();
            float m02 = (float) at.getMxt();
            shader.setConstant("m0", m00, m01, m02, 0.0f);

            float m10 = (float) at.getMyx();
            float m11 = (float) at.getMyy();
            float m12 = (float) at.getMyt();
            shader.setConstant("m1", m10, m11, m12, 0.0f);

            shader.setConstant("perspVec", 0.0f, 0.0f, 1.0f);
        }

        // clamp the focus point so that it does not rest on, or outside
        // of, the circumference of the gradient circle
        fd = (float) Math.min(fd, 0.99f);

        // pack a few unrelated, precalculated values into a single float4
        float denom = 1.0f - (fd * fd);
        float inv_denom = 1.0f / denom;
        shader.setConstant("precalc", fd, denom, inv_denom);

        setMultiGradient(shader, paint);
    }

    static AffineBase getRadialGradientTx(RadialGradient paint,
                                          Shader shader,
                                          BaseTransform renderTx,
                                          float rx, float ry, float rw, float rh)
    {
        Affine3D at = scratchXform3D;
        at.setTransform(renderTx);

        // save original (untransformed) center and focus points and
        // adjust to account for proportional attribute if necessary
        float radius = paint.getRadius();
        float cx = paint.getCenterX();
        float cy = paint.getCenterY();
        float fa = paint.getFocusAngle();
        float fd = paint.getFocusDistance();
        if (fd < 0) {
            fd = -fd;
            fa = fa+180;
        }
        fa = (float) Math.toRadians(fa);
        if (paint.isProportional()) {
            float bcx = rx + (rw / 2f);
            float bcy = ry + (rh / 2f);
            float scale = Math.min(rw, rh);
            cx = (cx - 0.5f) * scale + bcx;
            cy = (cy - 0.5f) * scale + bcy;
            if (rw != rh && rw != 0f && rh != 0f) {
                at.translate(bcx, bcy);
                at.scale(rw / scale, rh / scale);
                at.translate(-bcx, -bcy);
            }
            radius = radius * scale;
        }
        if (paint.getSpreadMethod() == Gradient.REFLECT) {
            radius *= 2.0f;
        }

        // transform from gradient coords to device coords
        BaseTransform paintXform = paint.getGradientTransformNoClone();
        if (paintXform != null) {
            at.concatenate(paintXform);
        }

        // transform unit circle to gradient coords; we start with the
        // unit circle (center=(0,0), focus on positive x-axis, radius=1)
        // and then transform into gradient space
        at.translate(cx, cy);
        at.rotate(fa);
        at.scale(radius, radius);

            // invert to get mapping from device coords to unit circle
        try {
            at.invert();
        } catch (Exception e) {
            at.setToScale(0.0, 0.0, 0.0);
        }

        // clamp the focus point so that it does not rest on, or outside
        // of, the circumference of the gradient circle
        fd = (float) Math.min(fd, 0.99f);

        // pack a few unrelated, precalculated values into a single float4
        float denom = 1.0f - (fd * fd);
        float inv_denom = 1.0f / denom;
        shader.setConstant("precalc", fd, denom, inv_denom);

        setTextureGradient(shader, paint);

        return at;
    }

/************************** ImagePattern support ****************************/

    /**
     * We use the plane equation to automatically
     * map the ImagePattern image to the geometry being rendered.  The
     * shader uses two separate plane equations that take the (x,y)
     * location (in device space) of the fragment being rendered to
     * calculate (u,v) texture coordinates for that fragment:
     *     u = Ax + By + Cz + Dw
     *     v = Ex + Fy + Gz + Hw
     *
     * For the 3D case we calculate a perspective vector by projecting rays
     * at various points, can finding the deltas. So we need to calculate appropriate
     * values for the plane equation constants (A,B,D) and (E,F,H) such
     * that {u,v}=0 for the top-left of the ImagePattern's anchor
     * rectangle and {u,v}=1 for the bottom-right of the anchor rectangle.
     * We can easily make the texture image repeat for {u,v} values
     * outside the range [0,1] by using the fract() instruction within
     * the shader.
     *
     * Calculating the plane equation constants is surprisingly simple.
     * We can think of it as an inverse matrix operation that takes
     * device space coordinates and transforms them into user space
     * coordinates that correspond to a location relative to the anchor
     * rectangle.  First, we translate and scale the current user space
     * transform by applying the anchor rectangle bounds.  We then take
     * the inverse of this affine transform.  The rows of the resulting
     * inverse matrix correlate nicely to the plane equation constants
     * we were seeking.
     */

    static void setImagePattern(ShaderGraphics g,
                                Shader shader,
                                ImagePattern paint,
                                float rx, float ry, float rw, float rh)
    {
        float x1 = rx + (paint.getX() * rw);
        float y1 = ry + (paint.getY() * rh);
        float x2 = x1 + (paint.getWidth() * rw);
        float y2 = y1 + (paint.getHeight() * rh);

        ResourceFactory rf = g.getResourceFactory();
        Image img = paint.getImage();
        Texture paintTex = rf.getCachedTexture(img, Texture.WrapMode.REPEAT);
        float cx = paintTex.getContentX();
        float cy = paintTex.getContentY();
        float cw = paintTex.getContentWidth();
        float ch = paintTex.getContentHeight();
        float texw = paintTex.getPhysicalWidth();
        float texh = paintTex.getPhysicalHeight();
        paintTex.unlock();

        // calculate plane equation constants
        Affine3D at = scratchXform3D;
        g.getPaintShaderTransform(at);

        BaseTransform paintXform = paint.getPatternTransformNoClone();
        if (paintXform != null) {
            at.concatenate(paintXform);
        }

        at.translate(x1, y1);
        at.scale(x2 - x1, y2 - y1);
        // Adjustment for case when WrapMode.REPEAT is simulated
        if (cw < texw) {
            at.translate(0.5/cw, 0.0);
            cx += 0.5f;
        }
        if (ch < texh) {
            at.translate(0.0, 0.5/ch);
            cy += 0.5f;
        }

        try {
            at.invert();
        } catch (Exception e) {
            at.setToScale(0.0, 0.0, 0.0);
        }

        if (!at.is2D()) {
            NGCamera cam = g.getCameraNoClone();
            Vec3d tmpVec = new Vec3d();
            PickRay tmpvec = new PickRay();
            PickRay ray00 = project(0,0,cam,at,tmpvec,tmpVec,null);
            PickRay ray10 = project(1,0,cam,at,tmpvec,tmpVec,null);
            PickRay ray01 = project(0,1,cam,at,tmpvec,tmpVec,null);

            double p0 = ray10.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            double p1 = ray01.getDirectionNoClone().x - ray00.getDirectionNoClone().x;
            double p2 = ray00.getDirectionNoClone().x;

            double py0 = ray10.getDirectionNoClone().y - ray00.getDirectionNoClone().y;
            double py1 = ray01.getDirectionNoClone().y - ray00.getDirectionNoClone().y;
            double py2 = ray00.getDirectionNoClone().y;

            p0 *= -ray00.getOriginNoClone().z;
            p1 *= -ray00.getOriginNoClone().z;
            p2 *= -ray00.getOriginNoClone().z;

            py0 *= -ray00.getOriginNoClone().z;
            py1 *= -ray00.getOriginNoClone().z;
            py2 *= -ray00.getOriginNoClone().z;

            double wv0 = ray10.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv1 = ray01.getDirectionNoClone().z - ray00.getDirectionNoClone().z;
            double wv2 = ray00.getDirectionNoClone().z;

            shader.setConstant("perspVec", (float)wv0, (float)wv1, (float)wv2);
            shader.setConstant("xParams", (float)p0, (float)p1, (float)p2, (float)ray00.getOriginNoClone().x);
            shader.setConstant("yParams", (float)py0, (float)py1, (float)py2, (float)ray00.getOriginNoClone().y);
        } else {
            float m00 = (float)at.getMxx();
            float m01 = (float)at.getMxy();
            float m02 = (float)at.getMxt();
            shader.setConstant("xParams", m00, m01, m02, 0.0f);

            float m10 = (float)at.getMyx();
            float m11 = (float)at.getMyy();
            float m12 = (float)at.getMyt();
            shader.setConstant("yParams", m10, m11, m12, 0.0f);
            shader.setConstant("perspVec", 0.0f, 0.0f, 1.0f);
        }

        cx /= texw;
        cy /= texh;
        cw /= texw;
        ch /= texh;
        shader.setConstant("content", cx, cy, cw, ch);
    }

    static AffineBase getImagePatternTx(ShaderGraphics g,
                                        ImagePattern paint,
                                        Shader shader,
                                        BaseTransform renderTx,
                                        float rx, float ry, float rw, float rh)
    {
        float px = paint.getX();
        float py = paint.getY();
        float pw = paint.getWidth();
        float ph = paint.getHeight();
        if (paint.isProportional()) {
            px = rx + px * rw;
            py = ry + py * rh;
            pw = pw * rw;
            ph = ph * rh;
        }

        ResourceFactory rf = g.getResourceFactory();
        Image img = paint.getImage();
        Texture paintTex = rf.getCachedTexture(img, Texture.WrapMode.REPEAT);
        float cx = paintTex.getContentX();
        float cy = paintTex.getContentY();
        float cw = paintTex.getContentWidth();
        float ch = paintTex.getContentHeight();
        float texw = paintTex.getPhysicalWidth();
        float texh = paintTex.getPhysicalHeight();
        paintTex.unlock();

        // calculate plane equation constants
        AffineBase ret;
        BaseTransform paintXform = paint.getPatternTransformNoClone();
        if (paintXform.isIdentity() && renderTx.isIdentity()) {
            Affine2D at = scratchXform2D;

            at.setToTranslation(px, py);
            at.scale(pw, ph);
            ret = at;
        } else {
            Affine3D at = scratchXform3D;
            at.setTransform(renderTx);
            at.concatenate(paintXform);

            at.translate(px, py);
            at.scale(pw, ph);
            ret = at;
        }

        // Adjustment for case when WrapMode.REPEAT is simulated
        if (cw < texw) {
            ret.translate(0.5/cw, 0.0);
            cx += 0.5f;
        }
        if (ch < texh) {
            ret.translate(0.0, 0.5/ch);
            cy += 0.5f;
        }

        try {
            ret.invert();
        } catch (Exception e) {
            ret = scratchXform2D;
            scratchXform2D.setToScale(0.0, 0.0);
        }

        cx /= texw;
        cy /= texh;
        cw /= texw;
        ch /= texh;
        shader.setConstant("content", cx, cy, cw, ch);

        return ret;
    }

    static PickRay project(float x, float y,
                           NGCamera cam, BaseTransform inv,
                           PickRay tmpray, Vec3d tmpvec, Point2D ret)
    {
        tmpray = cam.computePickRay(x, y, tmpray);
        return tmpray.project(inv, cam instanceof NGPerspectiveCamera,
                                         tmpvec, ret);
    }

}
