/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Builder;

public class TriangleMeshBuilder extends TreeMap<String, Object> implements Builder<TriangleMesh> {

    private static final String VALUE_SEPARATOR = ",";

    private float[] points;
    private float[] texCoords;
    private int[] faces;
    private int[] faceSmoothingGroups;

    @Override
    public TriangleMesh build() {
        TriangleMesh mesh = new TriangleMesh();
        if (points != null) {
            mesh.getPoints().setAll(points);
        }
        if (texCoords != null) {
            mesh.getTexCoords().setAll(texCoords);
        }
        if (faces != null) {
            mesh.getFaces().setAll(faces);
        }
        if (faceSmoothingGroups != null) {
            mesh.getFaceSmoothingGroups().setAll(faceSmoothingGroups);
        }
        return mesh;
    }

    @Override
    public Object put(String key, Object value) {

        if ("points".equalsIgnoreCase(key)) {
            StringTokenizer tokenizer = new StringTokenizer((String) value, VALUE_SEPARATOR);
            points = new float[tokenizer.countTokens()];
            int counter = 0;
            while (tokenizer.hasMoreTokens()) {
                points[counter++] = Float.parseFloat(tokenizer.nextToken().trim());
            }
        } else if ("texcoords".equalsIgnoreCase(key)) {
            StringTokenizer tokenizer = new StringTokenizer((String) value, VALUE_SEPARATOR);
            texCoords = new float[tokenizer.countTokens()];
            int counter = 0;
            while (tokenizer.hasMoreTokens()) {
                texCoords[counter++] = Float.parseFloat(tokenizer.nextToken().trim());
            }
        } else if ("faces".equalsIgnoreCase(key)) {
            StringTokenizer tokenizer = new StringTokenizer((String) value, VALUE_SEPARATOR);
            faces = new int[tokenizer.countTokens()];
            int counter = 0;
            while (tokenizer.hasMoreTokens()) {
                faces[counter++] = Integer.parseInt(tokenizer.nextToken().trim());
            }
        } else if ("facesmoothinggroups".equalsIgnoreCase(key)) {
            StringTokenizer tokenizer = new StringTokenizer((String) value, VALUE_SEPARATOR);
            faceSmoothingGroups = new int[tokenizer.countTokens()];
            int counter = 0;
            while (tokenizer.hasMoreTokens()) {
                faceSmoothingGroups[counter++] = Integer.parseInt(tokenizer.nextToken().trim());
            }
        }

        return super.put(key.toLowerCase(Locale.ROOT), value);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return super.entrySet();
    }

}
