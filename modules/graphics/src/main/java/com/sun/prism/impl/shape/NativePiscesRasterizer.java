/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.shape;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.PrismSettings;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class NativePiscesRasterizer implements ShapeRasterizer {
    private static MaskData emptyData = MaskData.create(new byte[1], 0, 0, 1, 1);

    private static final byte SEG_MOVETO  = PathIterator.SEG_MOVETO;
    private static final byte SEG_LINETO  = PathIterator.SEG_LINETO;
    private static final byte SEG_QUADTO  = PathIterator.SEG_QUADTO;
    private static final byte SEG_CUBICTO = PathIterator.SEG_CUBICTO;
    private static final byte SEG_CLOSE   = PathIterator.SEG_CLOSE;

    private byte cachedMask[];
    private ByteBuffer cachedBuffer;
    private MaskData cachedData;
    private int bounds[] = new int[4];
    private boolean lastAntialiasedShape;
    private boolean firstTimeAASetting = true;

    native static void init(int subpixelLgPositionsX, int subpixelLgPositionsY);

    native static void produceFillAlphas(float coords[], byte commands[], int nsegs, boolean nonzero,
                                         double mxx, double mxy, double mxt,
                                         double myx, double myy, double myt,
                                         int bounds[], byte mask[]);
    native static void produceStrokeAlphas(float coords[], byte commands[], int nsegs,
                                           float lw, int cap, int join, float mlimit,
                                           float dashes[], float dashoff,
                                           double mxx, double mxy, double mxt,
                                           double myx, double myy, double myt,
                                           int bounds[], byte mask[]);

    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            String libName = "prism_common";

            if (PrismSettings.verbose) {
                System.out.println("Loading Prism common native library ...");
            }
            NativeLibLoader.loadLibrary(libName);
            if (PrismSettings.verbose) {
                System.out.println("\tsucceeded.");
            }
            return null;
        });
    }

    @Override
    public MaskData getMaskData(Shape shape, BasicStroke stroke,
                                RectBounds xformBounds, BaseTransform xform,
                                boolean close, boolean antialiasedShape)
    {

        if (firstTimeAASetting || (lastAntialiasedShape != antialiasedShape)) {
            int subpixelLgPositions = antialiasedShape ? 3 : 0;
            NativePiscesRasterizer.init(subpixelLgPositions, subpixelLgPositions);
            firstTimeAASetting = false;
            lastAntialiasedShape = antialiasedShape;
        }

        if (stroke != null && stroke.getType() != BasicStroke.TYPE_CENTERED) {
            // RT-27427
            // TODO: Optimize the combinatorial strokes for simple
            // shapes and/or teach the rasterizer to be able to
            // do a "differential fill" between two shapes.
            // Note that most simple shapes will use a more optimized path
            // than this method for the INNER/OUTER strokes anyway.
            shape = stroke.createStrokedShape(shape);
            stroke = null;
        }
        if (xformBounds == null) {
            if (stroke != null) {
                // Note that all places that pass null for xformbounds also
                // pass null for stroke so that the following is not typically
                // executed, but just here as a safety net.
                shape = stroke.createStrokedShape(shape);
                stroke = null;
            }

            xformBounds = new RectBounds();
            //TODO: Need to verify that this is a safe cast ... (RT-27427)
            xformBounds = (RectBounds) xform.transform(shape.getBounds(), xformBounds);
        }
        bounds[0] = (int) Math.floor(xformBounds.getMinX());
        bounds[1] = (int) Math.floor(xformBounds.getMinY());
        bounds[2] = (int) Math.ceil(xformBounds.getMaxX());
        bounds[3] = (int) Math.ceil(xformBounds.getMaxY());
        if (bounds[2] <= bounds[0] || bounds[3] <= bounds[1]) {
            return emptyData;
        }
        Path2D p2d = (shape instanceof Path2D) ? (Path2D) shape : new Path2D(shape);
        double mxx, mxy, mxt, myx, myy, myt;
        if (xform == null || xform.isIdentity()) {
            mxx = myy = 1.0;
            mxy = myx = 0.0;
            mxt = myt = 0.0;
        } else {
            mxx = xform.getMxx();
            mxy = xform.getMxy();
            mxt = xform.getMxt();
            myx = xform.getMyx();
            myy = xform.getMyy();
            myt = xform.getMyt();
        }
        int x = bounds[0];
        int y = bounds[1];
        int w = bounds[2] - x;
        int h = bounds[3] - y;
        if (w <= 0 || h <= 0) {
            return emptyData;
        }
        if (cachedMask == null || w * h > cachedMask.length) {
            cachedMask = null;
            cachedBuffer = null;
            cachedData = new MaskData();
            int csize = (w * h + 0xfff) & (~0xfff);
            cachedMask = new byte[csize];
            cachedBuffer = ByteBuffer.wrap(cachedMask);
        }
        if (stroke != null) {
            produceStrokeAlphas(p2d.getFloatCoordsNoClone(),
                                p2d.getCommandsNoClone(),
                                p2d.getNumCommands(),
                                stroke.getLineWidth(), stroke.getEndCap(),
                                stroke.getLineJoin(), stroke.getMiterLimit(),
                                stroke.getDashArray(), stroke.getDashPhase(),
                                mxx, mxy, mxt, myx, myy, myt,
                                bounds, cachedMask);
        } else {
            produceFillAlphas(p2d.getFloatCoordsNoClone(),
                              p2d.getCommandsNoClone(),
                              p2d.getNumCommands(), p2d.getWindingRule() == Path2D.WIND_NON_ZERO,
                              mxx, mxy, mxt, myx, myy, myt,
                              bounds, cachedMask);
        }
        x = bounds[0];
        y = bounds[1];
        w = bounds[2] - x;
        h = bounds[3] - y;
        if (w <= 0 || h <= 0) {
            return emptyData;
        }
        cachedData.update(cachedBuffer, x, y, w, h);
        return cachedData;
    }
}
