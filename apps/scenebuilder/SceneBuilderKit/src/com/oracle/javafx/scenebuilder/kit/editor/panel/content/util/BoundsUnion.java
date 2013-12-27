/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/**
 * A BoundsUnion instance allows to compute the union of multiple bounds.
 * 
 * 
 */
public class BoundsUnion {
    
    private Bounds result;
    
    public void add(Bounds b) {
        if (result == null) {
            result = b;
        } else {
            result = compute(result, b);
        }
    }
    
    public Bounds getResult() {
        return result;
    }

    public static Bounds compute(Bounds b1, Bounds b2) {
        double minX, minY, minZ, maxX, maxY, maxZ;

        minX = Math.min(b1.getMinX(), b2.getMinX());
        minY = Math.min(b1.getMinY(), b2.getMinY());
        minZ = Math.min(b1.getMinZ(), b2.getMinZ());

        maxX = Math.max(b1.getMaxX(), b2.getMaxX());
        maxY = Math.max(b1.getMaxY(), b2.getMaxY());
        maxZ = Math.max(b1.getMaxZ(), b2.getMaxZ());

        return new BoundingBox(minX, minY, minZ,
                   maxX - minX, maxY - minY, maxZ - minZ);

    }
}
