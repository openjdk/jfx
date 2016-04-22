/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom.transform;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Vec3d;

/**
 *
 */
public class TransformHelper {

    private TransformHelper() {}


    public static BaseBounds general3dBoundsTransform(CanTransformVec3d tx,BaseBounds src, BaseBounds dst, Vec3d tempV3d) {

        if (tempV3d == null) {
            tempV3d = new Vec3d();
        }

        double srcMinX = src.getMinX();
        double srcMinY = src.getMinY();
        double srcMinZ = src.getMinZ();
        double srcMaxX = src.getMaxX();
        double srcMaxY = src.getMaxY();
        double srcMaxZ = src.getMaxZ();

        // TODO: Optimize... (RT-26884)
        tempV3d.set(srcMaxX, srcMaxY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        double minX = tempV3d.x;
        double minY = tempV3d.y;
        double minZ = tempV3d.z;
        double maxX = tempV3d.x;
        double maxY = tempV3d.y;
        double maxZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMaxY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMinY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMinY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMaxY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMaxY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMinY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMinY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if ( tempV3d.x  > maxX ) maxX = tempV3d.x;
        if ( tempV3d.y  > maxY ) maxY = tempV3d.y;
        if ( tempV3d.z  > maxZ ) maxZ = tempV3d.z;
        if ( tempV3d.x  < minX ) minX = tempV3d.x;
        if ( tempV3d.y  < minY ) minY = tempV3d.y;
        if ( tempV3d.z  < minZ ) minZ = tempV3d.z;

        return dst.deriveWithNewBounds( (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ);
    }

}
