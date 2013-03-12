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

import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TriangleMeshTest {
    
    /**
     * Test of setFaceSmoothingGroups method, of class TriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_intArr() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] expected = new int[faceSmoothingGroups.length];
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        assertTrue(instance.getFaceSmoothingGroupCount() == faceSmoothingGroups.length);        
        assertArrayEquals(instance.getFaceSmoothingGroups(expected), faceSmoothingGroups);
    }

    /**
     * Test faceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test (expected=IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_intArrValueOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);

        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        instance.setFaceSmoothingGroups(faceSmoothingGroups);

        Arrays.fill(faceSmoothingGroups, -1);
        instance.setFaceSmoothingGroups(faceSmoothingGroups); // expect IllegalArgumentException

        int[] expected = new int[faceSmoothingGroups.length];
        int[] actuals = new int[faceSmoothingGroups.length];
        Arrays.fill(actuals, 1);
        // faceSmoothingGroups should not change
        assertArrayEquals(instance.getFaceSmoothingGroups(expected), actuals);
    }

    /**
     * Test of setFaceSmoothingGroups method, of class TriangleMesh.
     */
    @Test
    public void testSetFaceSmoothingGroups_4args() {        
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 4, 8};
        int[] expected = new int[setterArray.length];
        int index = 1;
        int start = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(faceSmoothingGroups);
        instance.setFaceSmoothingGroups(index, setterArray, start, length);
        assertArrayEquals(instance.getFaceSmoothingGroups(index, expected, length), setterArray);
    }

    /**
     * Test faceSmoothingGroups with illegal value of setFaceSmoothingGroups
     * method, of class TriangleMesh.
     */
    @Test (expected=IllegalArgumentException.class)
    public void testSetFaceSmoothingGroups_4argsValueOutOfRange() {
        int divX = 10;
        int divY = 10;
        TriangleMesh instance = buildTriangleMesh(divX, divY);
        int[] faceSmoothingGroups = new int[divX * divY * 2];
        Arrays.fill(faceSmoothingGroups, 1);
        int[] setterArray = new int[]{2, 0, -1};
        int index = 0;
        int start = 0;
        int length = setterArray.length;
        instance.setFaceSmoothingGroups(index, setterArray, start, length); // expect IllegalArgumentException
        int[] expected = new int[faceSmoothingGroups.length];
        // faceSmoothingGroups should not change
        assertArrayEquals(instance.getFaceSmoothingGroups(expected), faceSmoothingGroups);
    }

    TriangleMesh buildTriangleMesh(int subDivX, int subDivY) {
        final int pointSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 6; // 3 point indices and 3 texCoord indices per triangle
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float points[] = new float[numVerts * pointSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY * 2;
        int faces[] = new int[faceCount * faceSize];

        TriangleMesh triangleMesh = new TriangleMesh(points, texCoords, faces);

        return triangleMesh;
    }
}
