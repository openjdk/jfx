/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.javafx.util.Utils;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;

/**
 * Used to access internal methods of javafx.scene.effect.Effect.
 */
public class EffectHelper {

    private static EffectAccessor effectAccessor;

    static {
        Utils.forceInit(javafx.scene.effect.Effect.class);
    }

    private EffectHelper() {
    }

    public static Effect getPeer(javafx.scene.effect.Effect effect) {
        return effectAccessor.getPeer(effect);
    }

    public static void sync(javafx.scene.effect.Effect effect) {
        effectAccessor.sync(effect);
    }

    public static IntegerProperty effectDirtyProperty(javafx.scene.effect.Effect effect) {
        return effectAccessor.effectDirtyProperty(effect);
    }

    public static boolean isEffectDirty(javafx.scene.effect.Effect effect) {
        return effectAccessor.isEffectDirty(effect);
    }

    public static BaseBounds getBounds(javafx.scene.effect.Effect effect,
            BaseBounds bounds, BaseTransform tx, Node node, BoundsAccessor boundsAccessor) {
        return effectAccessor.getBounds(effect, bounds, tx, node, boundsAccessor);
    }

    public static javafx.scene.effect.Effect copy(javafx.scene.effect.Effect effect) {
        return effectAccessor.copy(effect);
    }

    public static com.sun.scenario.effect.Blend.Mode getToolkitBlendMode(BlendMode mode) {
        return effectAccessor.getToolkitBlendMode(mode);
    }

    public static void setEffectAccessor(final EffectAccessor newAccessor) {
        if (effectAccessor != null) {
            throw new IllegalStateException();
        }

        effectAccessor = newAccessor;
    }

    public interface EffectAccessor {
        Effect getPeer(javafx.scene.effect.Effect effect);
        void sync(javafx.scene.effect.Effect effect);
        IntegerProperty effectDirtyProperty(javafx.scene.effect.Effect effect);
        boolean isEffectDirty(javafx.scene.effect.Effect effect);
        BaseBounds getBounds(javafx.scene.effect.Effect effect, BaseBounds bounds,
                BaseTransform tx, Node node, BoundsAccessor boundsAccessor);
        javafx.scene.effect.Effect copy(javafx.scene.effect.Effect effect);
        com.sun.scenario.effect.Blend.Mode getToolkitBlendMode(javafx.scene.effect.BlendMode mode);
    }

}
