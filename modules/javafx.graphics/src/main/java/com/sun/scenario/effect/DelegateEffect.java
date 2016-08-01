/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 * The implementation base class for {@code Effect} subclasses that
 * delegate their operations to a tree of other {@code Effect}
 * implementations.
 */
public abstract class DelegateEffect extends Effect {
    protected DelegateEffect(Effect input) {
        super(input);
    }

    protected DelegateEffect(Effect input1, Effect input2) {
        super(input1, input2);
    }

    /**
     * Returns the {@code Effect} object to delegate all operations to.
     *
     * @return the {@code Effect} object to delegate to.
     */
    protected abstract Effect getDelegate();

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        return getDelegate().getBounds(transform, defaultInput);
    }

    @Override
    public ImageData filter(FilterContext fctx,
                            BaseTransform transform,
                            Rectangle outputClip,
                            Object renderHelper,
                            Effect defaultInput)
    {
        return getDelegate().filter(fctx, transform, outputClip,
                                    renderHelper, defaultInput);
    }

    @Override
    public Point2D untransform(Point2D p, Effect defaultInput) {
        return getDelegate().untransform(p, defaultInput);
    }

    @Override
    public Point2D transform(Point2D p, Effect defaultInput) {
        return getDelegate().transform(p, defaultInput);
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        return getDelegate().getAccelType(fctx);
    }

    @Override
    public boolean reducesOpaquePixels() {
        return getDelegate().reducesOpaquePixels();
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        return getDelegate().getDirtyRegions(defaultInput, regionPool);
    }

}
