/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg;

import com.sun.scenario.effect.Effect;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 */
public abstract class BaseNodeEffectInput extends Effect {
    private BaseNode node;
    private BaseBounds tempBounds = new RectBounds();

    public BaseNodeEffectInput() {
        this(null);
    }

    public BaseNodeEffectInput(BaseNode node) {
        setNode(node);
    }

    public BaseNode getNode() {
        return node;
    }

    public void setNode(BaseNode node) {
        if (this.node != node) {
            this.node = node;
            flush();
        }
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform,
                              Effect defaultInput)
    {
        // TODO: update Effect.getBounds() to take Rectangle2D param so
        // that we can avoid creating garbage here? (RT-23958)
        BaseTransform t = transform == null ? 
                BaseTransform.IDENTITY_TRANSFORM : transform;
        tempBounds = node.getContentBounds(tempBounds, t);
        return tempBounds.copy();
    }

    public abstract void flush();
}
