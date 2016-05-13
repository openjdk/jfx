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

package com.sun.javafx.scene.transform;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.transform.Transform;

/**
 * Used to access internal methods of Transform.
 */
public class TransformHelper {

    private static TransformAccessor transformAccessor;

    static {
        Utils.forceInit(Transform.class);
    }

    private TransformHelper() {
    }

    public static void add(Transform transform, Node node) {
        transformAccessor.add(transform, node);
    }

    public static void remove(Transform transform, Node node) {
        transformAccessor.remove(transform, node);
    }

    public static void apply(Transform transform, Affine3D affine3D) {
        transformAccessor.apply(transform, affine3D);
    }

    public static BaseTransform derive(Transform transform, BaseTransform baseTransform) {
        return transformAccessor.derive(transform, baseTransform);
    }

    public static Transform createImmutableTransform() {
        return transformAccessor.createImmutableTransform();
    }

    public static Transform createImmutableTransform(
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return transformAccessor.createImmutableTransform(mxx, mxy, mxz, tx,
                myx, myy, myz, ty, mzx, mzy, mzz, tz);
    }

    public static Transform createImmutableTransform(Transform transform,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return transformAccessor.createImmutableTransform(transform, mxx, mxy, mxz, tx,
                myx, myy, myz, ty, mzx, mzy, mzz, tz);
    }

    public static Transform createImmutableTransform(Transform transform,
            Transform left, Transform right) {
        return transformAccessor.createImmutableTransform(transform, left, right);
    }

    public static void setTransformAccessor(final TransformAccessor newAccessor) {
        if (transformAccessor != null) {
            throw new IllegalStateException();
        }

        transformAccessor = newAccessor;
    }

    public interface TransformAccessor {

        void add(Transform transform, Node node);

        void remove(Transform transform, Node node);

        void apply(Transform transform, Affine3D affine3D);

        BaseTransform derive(Transform transform, BaseTransform baseTransform);

        Transform createImmutableTransform();

        Transform createImmutableTransform(
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz);

        Transform createImmutableTransform(Transform transform,
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz);

        Transform createImmutableTransform(Transform transform,
                Transform left, Transform right);
    }

}
