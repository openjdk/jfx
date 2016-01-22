/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism;

import com.sun.prism.Graphics;
import com.sun.prism.RTTexture;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.impl.ImagePool;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.Renderer;
import java.lang.ref.WeakReference;

public abstract class PrDrawable extends PrTexture<RTTexture> implements PoolFilterable {
    private WeakReference<ImagePool> pool;

    public static PrDrawable create(FilterContext fctx, RTTexture rtt) {
        return ((PrRenderer) Renderer.getRenderer(fctx)).createDrawable(rtt);
    }

    protected PrDrawable(RTTexture rtt) {
        super(rtt);
    }

    @Override
    public void setImagePool(ImagePool pool) {
        this.pool = new WeakReference<>(pool);
    }

    @Override
    public ImagePool getImagePool() {
        return pool == null ? null : pool.get();
    }

    @Override public float getPixelScale() {
        return 1.0f;
    }

    @Override public int getMaxContentWidth() {
        return getTextureObject().getMaxContentWidth();
    }

    @Override public int getMaxContentHeight() {
        return getTextureObject().getMaxContentHeight();
    }

    @Override public void setContentWidth(int contentW) {
        getTextureObject().setContentWidth(contentW);
    }

    @Override public void setContentHeight(int contentH) {
        getTextureObject().setContentHeight(contentH);
    }

    public abstract Graphics createGraphics();

    public void clear() {
        Graphics g = createGraphics();
        g.clear();
    }
}
