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

package javafx.scene;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;

public class CameraShim {

    public static PickRay computePickRay(Camera c,
            double x, double y, PickRay pickRay) {
        return c.computePickRay(x, y, pickRay);
    }

    public static Vec3d computePosition(Camera c, Vec3d position) {
        return c.computePosition(position);
    }

    public static double getFarClipInScene(Camera c) {
        return c.getFarClipInScene();
    }

    public static double getNearClipInScene(Camera c) {
        return c.getNearClipInScene();
    }

    public static GeneralTransform3D getProjViewTransform(Camera c)  {
        return c.getProjViewTransform();
    }

    public static Affine3D getSceneToLocalTransform(Camera c) {
        return c.getSceneToLocalTransform();
    }

    public static double getViewHeight(Camera c) {
        return c.getViewHeight();
    }

    public static double getViewWidth(Camera c) {
        return c.getViewWidth();
    }


}
