/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.importers.max;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;

/** Max file format data objects */
public class MaxData {
    public static class MappingChannel {
        public int ntPoints;
        public float tPoints[];
        public int faces[]; // t0 t1 t2
    }

    public static class Mesh {
        public String name;
        public int nPoints;
        public float points[]; // x,y,z, x,y,z, ....
        public int nFaces;
        public int faces[];    // [[p0,p1,p2, smoothing]...]
        public MappingChannel mapping[];
    }

    public static class NodeTM {
        public String name;
        public Point3D pos;
        public Point3D tm[] = new Point3D[3];
    }

    public static class Material {
        public String name;
        public String diffuseMap;
        public Point3D ambientColor;
        public Point3D diffuseColor;
        public Point3D specularColor;
    }

    public static class Node {
        public String name;
        public NodeTM nodeTM;
        public Node parent;
        public List<Node> children;
    }

    public static class LightNode extends Node {
        public float intensity;
        public float r, g, b;
    }

    public static class CameraNode extends Node {
        public NodeTM target;
        public float near, far, fov;
    }

    public static class GeomNode extends Node {
        public Mesh mesh;
        public int materialRef;
    }

    public Material materials[];
    public Map<String, Node> nodes = new HashMap<>();
    public Map<String, Node> roots = new HashMap<>();
}
