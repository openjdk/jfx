/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.geom.transform;

import com.javafx.experiments.utils3d.geom.BaseBounds;
import com.javafx.experiments.utils3d.geom.Vec3d;

public class TransformHelper {

    private TransformHelper() {
    }


    public static BaseBounds general3dBoundsTransform(CanTransformVec3d tx, BaseBounds src, BaseBounds dst, Vec3d tempV3d) {

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
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMinY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMinY, srcMaxZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMaxY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMaxY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMinX, srcMinY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        tempV3d.set(srcMaxX, srcMinY, srcMinZ);
        tempV3d = tx.transform(tempV3d, tempV3d);
        if (tempV3d.x > maxX) maxX = tempV3d.x;
        if (tempV3d.y > maxY) maxY = tempV3d.y;
        if (tempV3d.z > maxZ) maxZ = tempV3d.z;
        if (tempV3d.x < minX) minX = tempV3d.x;
        if (tempV3d.y < minY) minY = tempV3d.y;
        if (tempV3d.z < minZ) minZ = tempV3d.z;

        return dst.deriveWithNewBounds((float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ);
    }

}
