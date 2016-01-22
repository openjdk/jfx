/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the format of the vertices in a mesh. A vertex consists of an array
 * of points, normals (optional), and texture coordinates.
 *
 * @since JavaFX 8u40
 */
public final class VertexFormat {

    /**
     * Specifies the format of a vertex that consists of a point and texture coordinates.
     */
    public static final VertexFormat POINT_TEXCOORD = new VertexFormat("POINT_TEXCOORD", 2, 0, -1, 1);

    /**
     * Specifies the format of a vertex that consists of a point, normal and texture coordinates.
     */
    public static final VertexFormat POINT_NORMAL_TEXCOORD = new VertexFormat("POINT_NORMAL_TEXCOORD", 3, 0, 1, 2);

    // For internal use only
    private static final int POINT_ELEMENT_SIZE = 3;
    private static final int NORMAL_ELEMENT_SIZE = 3;
    private static final int TEXCOORD_ELEMENT_SIZE = 2;

    private final String name;
    private final int vertexIndexSize;
    private final int pointIndexOffset;
    private final int normalIndexOffset;
    private final int texCoordIndexOffset;

    private VertexFormat(String name, int vertexIndexSize,
            int pointIndexOffset, int normalIndexOffset, int texCoordIndexOffset) {
        this.name = name;
        this.vertexIndexSize = vertexIndexSize;
        this.pointIndexOffset = pointIndexOffset;
        this.normalIndexOffset = normalIndexOffset;
        this.texCoordIndexOffset = texCoordIndexOffset;
    }

    int getPointElementSize() {
        return POINT_ELEMENT_SIZE;
    }

    int getNormalElementSize() {
        return NORMAL_ELEMENT_SIZE;
    }

    int getTexCoordElementSize() {
        return TEXCOORD_ELEMENT_SIZE;
    }

    /**
     * Returns the number of component indices that represents a vertex. For example,
     * a POINT_TEXCOORD vertex consists of 2 indices, one for point component and
     * the other for texture coordinates component. Hence its value will be 2.
     *
     * @return the number of component indices
     */
    public int getVertexIndexSize() {
        return vertexIndexSize;
    }

    /**
     * Returns the index offset in the face array of the point component within
     * a vertex.
     *
     * @return the offset to the point component.
     */
    public int getPointIndexOffset() {
        return pointIndexOffset;
    }

    /**
     * Returns the index offset in the face array of the normal component within
     * a vertex.
     *
     * @return the offset to the normal component.
     */
    public int getNormalIndexOffset() {
        return normalIndexOffset;
    }

    /**
     * Returns the index offset in the face array of the texture coordinates
     * component within a vertex.
     *
     * @return the offset to the texture coordinates component.
     */
    public int getTexCoordIndexOffset() {
        return texCoordIndexOffset;
    }

    @Override
    public String toString() {
        return name;
    }

}
