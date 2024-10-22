/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.sg.prism;

import com.sun.javafx.sg.prism.NGTriangleMeshShim;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class NGTriangleMeshTest {

    private static final float EPSILON_FLOAT = 1e-5f;

    /**
     * Test of syncFaceSmoothingGroups method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncFaceSmoothingGroups() {
        final int[] faceSmoothingGroups = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncFaceSmoothingGroups((array, fromAndLengthIndices) -> faceSmoothingGroups);
        int[] actuals = instance.test_getFaceSmoothingGroups();
        int[] expecteds = new int[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals(expecteds, actuals);
    }

    /**
     * Test of syncFaceSmoothingGroups method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncFaceSmoothingGroups2() {
        final int[] faceSmoothingGroups = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncFaceSmoothingGroups((array, fromAndLengthIndices) -> faceSmoothingGroups);
        instance.syncFaceSmoothingGroups((array, fromAndLengthIndices) -> {
            Arrays.fill(array, 1, 1 + 4, 1);
            return array;
        });
        int[] actuals = instance.test_getFaceSmoothingGroups();
        int[] expecteds = new int[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals);
    }

    /**
     * Test of syncPoints method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncPoints() {
        final float[] points = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncPoints((array, fromAndLengthIndices) -> points);
        float[] actuals = instance.test_getPoints();
        float[] expecteds = new float[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncPoints method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncPoints2() {
        final float[] points = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncPoints((array, fromAndLengthIndices) -> points);
        instance.syncPoints((array, fromAndLengthIndices) -> {
            Arrays.fill(array, 1, 1 + 4, 1);
            return array;
        });
        float[] actuals = instance.test_getPoints();
        float[] expecteds = new float[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncNormals method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncNormals() {
        final float[] normals = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncNormals((array, fromAndLengthIndices) -> normals);
        float[] actuals = instance.test_getNormals();
        float[] expecteds = new float[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncNormals method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncNormals2() {
        final float[] normals = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncNormals((array, fromAndLengthIndices) -> normals);
        instance.syncNormals((array, fromAndLengthIndices) -> {
            Arrays.fill(array, 1, 1 + 4, 1);
            return array;
        });
        float[] actuals = instance.test_getNormals();
        float[] expecteds = new float[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncTexCoords method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncTexCoords() {
        final float[] texcoords = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncTexCoords((array, fromAndLengthIndices) -> texcoords);
        float[] actuals = instance.test_getTexCoords();
        float[] expecteds = new float[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncTexCoords method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncTexCoords2() {
        final float[] texcoords = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncTexCoords((array, fromAndLengthIndices) -> texcoords);
        instance.syncTexCoords((array, fromAndLengthIndices) -> {
            Arrays.fill(array, 1, 1 + 4, 1);
            return array;
        });
        float[] actuals = instance.test_getTexCoords();
        float[] expecteds = new float[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncFaces method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncFaces() {
        final int[] faces = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncFaces((array, fromAndLengthIndices) -> faces);
        int[] actuals = instance.test_getFaces();
        int[] expecteds = new int[]{0, 1, 2, 3, 4, 5};
        assertArrayEquals(expecteds, actuals);
    }

    /**
     * Test of syncFaces method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncFaces2() {
        final int[] faces = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMeshShim instance = new NGTriangleMeshShim();
        instance.syncFaces((array, fromAndLengthIndices) -> faces);
        instance.syncFaces((array, fromAndLengthIndices) -> {
            Arrays.fill(array, 1, 1 + 4, 1);
            return array;
        });
        int[] actuals = instance.test_getFaces();
        int[] expecteds = new int[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals);
    }
}
