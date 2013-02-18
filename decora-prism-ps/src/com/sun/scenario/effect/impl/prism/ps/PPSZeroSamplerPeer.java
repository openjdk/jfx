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

package com.sun.scenario.effect.impl.prism.ps;

import com.sun.javafx.geom.Rectangle;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderGraphics;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.Renderer;

public abstract class PPSZeroSamplerPeer extends PPSEffectPeer {

    private Shader shader;

    protected PPSZeroSamplerPeer(FilterContext fctx, Renderer r, String shaderName) {
        super(fctx, r, shaderName);
    }

    @Override
    public void dispose() {
        if (shader != null) {
            shader.dispose();
        }
    }

    @Override
    ImageData filterImpl(ImageData... inputs) {
        final Rectangle dstBounds = getDestBounds();
        final int dstw = dstBounds.width;
        final int dsth = dstBounds.height;

        PPSRenderer renderer = getRenderer();
        PPSDrawable dst = renderer.getCompatibleImage(dstw, dsth);
        if (dst == null) {
            renderer.markLost();
            return new ImageData(getFilterContext(), dst, dstBounds);
        }
        setDestNativeBounds(dst.getPhysicalWidth(), dst.getPhysicalHeight());

        ShaderGraphics g = dst.createGraphics();
        if (g == null) {
            renderer.markLost();
            return new ImageData(getFilterContext(), dst, dstBounds);
        }
        if (shader == null) {
            shader = createShader();
        }
        if (shader == null || !shader.isValid()) {
            renderer.markLost();
            return new ImageData(getFilterContext(), dst, dstBounds);
        }
        g.setExternalShader(shader);
        updateShader(shader);

        float dx1 = 0;
        float dy1 = 0;
        float dx2 = dstw;
        float dy2 = dsth;

        g.setPaint(Color.WHITE);
        g.fillQuad(dx1, dy1, dx2, dy2);
        g.setExternalShader(null);

        return new ImageData(getFilterContext(), dst, dstBounds);
    }
}
