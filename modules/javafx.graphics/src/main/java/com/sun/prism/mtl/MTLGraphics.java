/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.prism.RenderTarget;
import com.sun.prism.impl.ps.BaseShaderGraphics;
import com.sun.prism.paint.Color;

class MTLGraphics extends BaseShaderGraphics {

    private final MTLContext context;

    private MTLGraphics(MTLContext context, RenderTarget target) {
        super(context, target);
        this.context = context;
    }

    static MTLGraphics create(MTLContext context, RenderTarget target) {
        return target == null ? null : new MTLGraphics(context, target);
    }

    @Override
    public void clear(Color color) {
        float r = color.getRedPremult();
        float g = color.getGreenPremult();
        float b = color.getBluePremult();
        float a = color.getAlpha();

        context.validateClearOp(this);
        getRenderTarget().setOpaque(color.isOpaque());
        nClear(context.getContextHandle(), r, g, b, a, isDepthBuffer());
    }

    @Override
    public void sync() {
        context.flushVertexBuffer();
        context.commitCurrentCommandBuffer();
    }

    // Native methods
    private static native void nClear(long pContext, float red, float green, float blue, float alpha, boolean clearDepth);
}
