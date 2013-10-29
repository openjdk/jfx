package javafx.fxml;
/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.shape.TriangleMesh;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

public class FXMLLoader_BuilderTest {

    @Test
    public void testTriangleMeshBuilder() throws IOException {
        TriangleMesh mesh = FXMLLoader.load(getClass().getResource("builders_trianglemesh.fxml"));
        float[] refFloatArray = {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f};
        int[] refIntArray = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertArrayEquals(refIntArray, mesh.getFaces().toArray(new int[0]));
        assertArrayEquals(refIntArray, mesh.getFaceSmoothingGroups().toArray(new int[0]));
        assertArrayEquals(refFloatArray, mesh.getPoints().toArray(new float[0]), 1e-10f);
        assertArrayEquals(refFloatArray, mesh.getTexCoords().toArray(new float[0]), 1e-10f);
    }

}
