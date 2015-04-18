/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.util.Builder;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class TriangleMeshBuilder extends TreeMap<String, Object> implements Builder<TriangleMesh> {

    private static final String VALUE_SEPARATOR_REGEX = "[,\\s]+";

    private float[] points;
    private float[] texCoords;
    private float[] normals;
    private int[] faces;
    private int[] faceSmoothingGroups;
    private VertexFormat vertexFormat;

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
        if (normals != null) {
            mesh.getNormals().setAll(normals);
        }
        if (vertexFormat != null) {
            mesh.setVertexFormat(vertexFormat);
        }
        return mesh;
    }

    @Override
    public Object put(String key, Object value) {

        if ("points".equalsIgnoreCase(key)) {
            String[] split = ((String) value).split(VALUE_SEPARATOR_REGEX);
            points = new float[split.length];
            for (int i = 0; i < split.length; ++i) {
                points[i] = Float.parseFloat(split[i]);
            }
        } else if ("texcoords".equalsIgnoreCase(key)) {
            String[] split = ((String) value).split(VALUE_SEPARATOR_REGEX);
            texCoords = new float[split.length];
            for (int i = 0; i < split.length; ++i) {
                texCoords[i] = Float.parseFloat(split[i]);
            }
        } else if ("faces".equalsIgnoreCase(key)) {
            String[] split = ((String) value).split(VALUE_SEPARATOR_REGEX);
            faces = new int[split.length];
            for (int i = 0; i < split.length; ++i) {
                faces[i] = Integer.parseInt(split[i]);
            }
        } else if ("facesmoothinggroups".equalsIgnoreCase(key)) {
            String[] split = ((String) value).split(VALUE_SEPARATOR_REGEX);
            faceSmoothingGroups = new int[split.length];
            for (int i = 0; i < split.length; ++i) {
                faceSmoothingGroups[i] = Integer.parseInt(split[i]);
            }
        } else if ("normals".equalsIgnoreCase(key)) {
            String[] split = ((String) value).split(VALUE_SEPARATOR_REGEX);
            normals = new float[split.length];
            for (int i = 0; i < split.length; ++i) {
                normals[i] = Float.parseFloat(split[i]);
            }
        } else if ("vertexformat".equalsIgnoreCase(key)) {
            if (value instanceof VertexFormat) {
                vertexFormat = (VertexFormat) value;
            } else if ("point_texcoord".equalsIgnoreCase((String)value)) {
                vertexFormat = VertexFormat.POINT_TEXCOORD;
            } else if ("point_normal_texcoord".equalsIgnoreCase((String)value)) {
                vertexFormat = VertexFormat.POINT_NORMAL_TEXCOORD;
            }
        }

        return super.put(key.toLowerCase(Locale.ROOT), value);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return super.entrySet();
    }

}
