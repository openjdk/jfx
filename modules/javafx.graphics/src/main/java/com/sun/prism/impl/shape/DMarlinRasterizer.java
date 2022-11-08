/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.marlin.DMarlinRenderingEngine;
import com.sun.marlin.MarlinProperties;
import com.sun.marlin.MarlinRenderer;
import com.sun.marlin.MaskMarlinAlphaConsumer;
import com.sun.marlin.RendererContext;
import com.sun.prism.BasicStroke;
import com.sun.prism.impl.PrismSettings;

/**
 * Thread-safe Marlin rasterizer (TL or CLQ storage)
 */
public final class DMarlinRasterizer implements ShapeRasterizer {
    private static final MaskData EMPTY_MASK = MaskData.create(new byte[1], 0, 0, 1, 1);

    private static final boolean DO_RENDER = !MarlinProperties.isSkipRenderTiles();

    @Override
    public MaskData getMaskData(Shape shape,
                                BasicStroke stroke,
                                RectBounds xformBounds,
                                BaseTransform xform,
                                boolean close, boolean antialiasedShape)
    {
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
        if (xformBounds.isEmpty()) {
            return EMPTY_MASK;
        }

        final RendererContext rdrCtx = DMarlinRenderingEngine.getRendererContext();
        MarlinRenderer renderer = null;
        try {
            final Rectangle rclip = rdrCtx.clip;
            rclip.setBounds(xformBounds);

            renderer = DMarlinPrismUtils.setupRenderer(rdrCtx, shape, stroke, xform, rclip,
                    antialiasedShape);

            final int outpix_xmin = renderer.getOutpixMinX();
            final int outpix_xmax = renderer.getOutpixMaxX();
            final int outpix_ymin = renderer.getOutpixMinY();
            final int outpix_ymax = renderer.getOutpixMaxY();
            final int w = outpix_xmax - outpix_xmin;
            final int h = outpix_ymax - outpix_ymin;
            if ((w <= 0) || (h <= 0)) {
                return EMPTY_MASK;
            }

            MaskMarlinAlphaConsumer consumer = rdrCtx.consumer;
            if (consumer == null || (w * h) > consumer.getAlphaLength()) {
                final int csize = (w * h + 0xfff) & (~0xfff);
                rdrCtx.consumer = consumer = new MaskMarlinAlphaConsumer(csize);
                if (PrismSettings.verbose) {
                    System.out.println("new alphas with length = " + csize);
                }
            }
            consumer.setBoundsNoClone(outpix_xmin, outpix_ymin, w, h);
            renderer.produceAlphas(consumer);

            if (!DO_RENDER) {
                return EMPTY_MASK;
            }
            return consumer.getMaskData();
        } finally {
            if (renderer != null) {
                renderer.dispose();
            }
            // recycle the RendererContext instance
            DMarlinRenderingEngine.returnRendererContext(rdrCtx);
        }
    }

    static Shape createCenteredStrokedShape(Shape s, BasicStroke stroke)
    {
        final float lw = (stroke.getType() == BasicStroke.TYPE_CENTERED) ?
                             stroke.getLineWidth() : stroke.getLineWidth() * 2.0f;

        final RendererContext rdrCtx = DMarlinRenderingEngine.getRendererContext();
        try {
            // initialize a large copyable Path2D to avoid a lot of array growing:
            final Path2D p2d = rdrCtx.getPath2D();

            DMarlinPrismUtils.strokeTo(rdrCtx, s, stroke, lw,
                     rdrCtx.transformerPC2D.wrapPath2D(p2d)
            );

            // Use Path2D copy constructor (trim)
            return new Path2D(p2d);

        } finally {
            // recycle the RendererContext instance
            DMarlinRenderingEngine.returnRendererContext(rdrCtx);
        }
    }
}
