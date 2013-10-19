/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.shape;

/**
 * NOTE: Do not make this a public class for FX 8.0
 * 
 * A flexible Vertex Format Class used to describe the contents of vertices interleaved
 * in an associated single Face buffer.
 */
class VertexFormat {
    /*
     *  TODO: 1) Need to handle multiple of the same component such as texCoord in the future.
     *        2) Change from a list of constants to set of enums? 
     */

    // For internal use only
    private static final int POINT_ELEMENT_SIZE = 3;
    private static final int TEXCOORD_ELEMENT_SIZE = 2;
    private static final int FACE_ELEMENT_SIZE = 6;

    VertexFormat() {
    }

    int getPointElementSize() {
        return POINT_ELEMENT_SIZE;
    }

    int getTexCoordElementSize() {
        return TEXCOORD_ELEMENT_SIZE;
    }

    int getFaceElementSize() {
        return FACE_ELEMENT_SIZE;
    }

}
