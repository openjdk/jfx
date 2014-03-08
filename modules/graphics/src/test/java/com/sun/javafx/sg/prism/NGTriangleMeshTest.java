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
package com.sun.javafx.sg.prism;

import com.sun.javafx.collections.FloatArraySyncer;
import com.sun.javafx.collections.IntegerArraySyncer;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class NGTriangleMeshTest {

    private static final float EPSILON_FLOAT = 1e-5f;

    /**
     * Test of syncFaceSmoothingGroups method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncFaceSmoothingGroups() {
        final int[] faceSmoothingGroups = new int[]{0, 1, 2, 3, 4, 5};
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncFaceSmoothingGroups(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                return faceSmoothingGroups;
            }
        });
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncFaceSmoothingGroups(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                return faceSmoothingGroups;
            }
        });
        instance.syncFaceSmoothingGroups(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                Arrays.fill(array, 1, 1 + 4, 1);
                return array;
            }
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncPoints(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                return points;
            }
        });
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncPoints(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                return points;
            }
        });
        instance.syncPoints(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                Arrays.fill(array, 1, 1 + 4, 1);
                return array;
            }
        });
        float[] actuals = instance.test_getPoints();
        float[] expecteds = new float[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals, EPSILON_FLOAT);
    }

    /**
     * Test of syncTexCoords method, of class NGTriangleMesh.
     */
    @Test
    public void testSyncTexCoords() {
        final float[] texcoords = new float[]{0, 1, 2, 3, 4, 5};
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncTexCoords(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                return texcoords;
            }
        });
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncTexCoords(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                return texcoords;
            }
        });
        instance.syncTexCoords(new FloatArraySyncer() {

            public float[] syncTo(float[] array, int[] fromAndLengthIndices) {
                Arrays.fill(array, 1, 1 + 4, 1);
                return array;
            }
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncFaces(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                return faces;
            }
        });
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
        NGTriangleMesh instance = new NGTriangleMesh();
        instance.syncFaces(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                return faces;
            }
        });
        instance.syncFaces(new IntegerArraySyncer() {

            public int[] syncTo(int[] array, int[] fromAndLengthIndices) {
                Arrays.fill(array, 1, 1 + 4, 1);
                return array;
            }
        });
        int[] actuals = instance.test_getFaces();
        int[] expecteds = new int[]{0, 1, 1, 1, 1, 5};
        assertArrayEquals(expecteds, actuals);
    }
}
