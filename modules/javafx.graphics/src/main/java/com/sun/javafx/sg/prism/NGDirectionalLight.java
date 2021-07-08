/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.Vec3d;

import javafx.geometry.Point3D;

/**
 * The peer of the {@code DirectionalLight} class. Holds the default values of {@code DirectionalLight}'s
 * properties and updates the visuals via {@link NGNode#visualsChanged} when one of the current
 * values changes. The peer receives its changes by {@link javafx.scene.DirectionalLight#doUpdatePeer} calls.
 */
public class NGDirectionalLight extends NGLightBase {

    /** Direction default value */
    private static final Point3D DEFAULT_DIRECTION = new Point3D(0, 0, 1);

    public NGDirectionalLight() {
    }

    public static Point3D getDefaultDirection() {
        return DEFAULT_DIRECTION;
    }

    private Point3D direction = DEFAULT_DIRECTION;
    private final Vec3d effectiveDir = new Vec3d();

    public Point3D getDirection() {
        var dir = new Vec3d(direction.getX(), direction.getY(), direction.getZ());
        getWorldTransform().deltaTransform(dir, effectiveDir);
        return new Point3D(effectiveDir.x, effectiveDir.y, effectiveDir.z);
    }

    public void setDirection(Point3D direction) {
        if (!this.direction.equals(direction)) {
            this.direction = direction;
            visualsChanged();
        }
    }
}
