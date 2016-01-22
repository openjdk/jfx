/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.transform.TransformUtils.ImmutableTransform;
import javafx.scene.transform.Transform;

public class TransformUtilsShim {

    public static Transform getImmutableTransform(Transform transform) {
        return new TransformUtils.ImmutableTransform(transform);
    }

    public static Transform getImmutableTransform(
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return new TransformUtils.ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }

    public static int getImmutableState2d(Transform t) {
        return ((TransformUtils.ImmutableTransform) t).getState2d();
    }

    public static int getImmutableState3d(Transform t) {
        return ((TransformUtils.ImmutableTransform) t).getState3d();
    }

    public static class ImmutableTransformShim extends ImmutableTransform  {

    }

}
