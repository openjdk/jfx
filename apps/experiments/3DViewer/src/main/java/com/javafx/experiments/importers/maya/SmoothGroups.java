/*
 * Copyright (c) 2010, 2013 Oracle and/or its affiliates.
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
package com.javafx.experiments.importers.maya;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import com.javafx.experiments.importers.maya.values.MFloat3Array;
import com.javafx.experiments.importers.maya.values.MPolyFace;
import com.sun.javafx.geom.Vec3f;

/** Util for converting Normals to Smoothing Groups */
public class SmoothGroups {
    private BitSet visited, notVisited;
    private Queue<Integer> q;

    private List<MPolyFace.FaceData> faces;
    MFloat3Array normals;
    // offset in normals array for starting edge of each face
    private int normalsOffsets[] = null;

    private static boolean trace = false;
    private static boolean verbose = false;

    public SmoothGroups(List<MPolyFace.FaceData> faces, MFloat3Array normals) {
        this.faces = faces;
        this.normals = normals;
        visited = new BitSet(faces.size());
        notVisited = new BitSet(faces.size());
        notVisited.set(0, faces.size(), true);
        q = new LinkedList<Integer>();
    }

    // edge -> [faces]
    private List<Integer> getNextConnectedComponent(Map<Integer, List<Integer>> adjacentFaces) {
        int index = notVisited.previousSetBit(faces.size() - 1);
        q.add(index);
        visited.set(index);
        notVisited.set(index, false);
        List<Integer> res = new ArrayList<Integer>();
        while (!q.isEmpty()) {
            Integer faceIndex = q.remove();
            res.add(faceIndex);
            MPolyFace.FaceData faceData = faces.get(faceIndex);
            int[] faceEdges = faceData.getFaceEdges();
            for (int e = 0; e < faceEdges.length; e++) {
                int edge = reverse(faceEdges[e]);
                List<Integer> adjFaces = adjacentFaces.get(edge);
                if (adjFaces == null) {
                    continue;
                }
                // double get always, get(0); if(==) get(1) would be better?
                Integer adjFaceIndex = adjFaces.get(adjFaces.get(0).equals(faceIndex) ? 1 : 0);
                if (!visited.get(adjFaceIndex)) {
                    q.add(adjFaceIndex);
                    visited.set(adjFaceIndex);
                    notVisited.set(adjFaceIndex, false);
                }
            }
        }
        return res;
    }

    private boolean hasNextConnectedComponent() {
        return !notVisited.isEmpty();
    }

    private Map<Integer, List<Integer>> getAdjacentFaces() {
        Map<Integer, List<Integer>> adjacentFaces = new HashMap();
        for (int f = 0; f < faces.size(); f++) {
            MPolyFace.FaceData faceData = faces.get(f);
            int[] faceEdges = faceData.getFaceEdges();
            for (int i = 0; i < faceEdges.length; i++) {
                int edge = reverse(faceEdges[i]);
                Integer key = Integer.valueOf(edge);
                if (!adjacentFaces.containsKey(key)) {
                    adjacentFaces.put(key, new ArrayList<Integer>());
                }
                adjacentFaces.get(key).add(Integer.valueOf(f));
            }
        }
        //System.out.println("adjacentFaces = " + adjacentFaces);
        for (Iterator<Map.Entry<Integer, List<Integer>>> it = adjacentFaces.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, List<Integer>> e = it.next();
            if (e.getValue().size() != 2) {
                // just skip them
                //System.out.println("edge " + e.getKey() + " has too many adjacent faces: " + e.getValue());
                it.remove();
            }
        }
        return adjacentFaces;
    }

    private void calcNormalsOffsets() {
        normalsOffsets = new int[faces.size()];
        int offset = 0;
        for (int face = 0; face < faces.size(); face++) {
            normalsOffsets[face] = offset;
            MPolyFace.FaceData faceData = faces.get(face);
            offset += faceData.getFaceEdges().length;
        }
    }

    private int getNormalIndex(int face, int edge) {
        return normalsOffsets[face] + edge;
    }

    private static int reverse(int edge) {
        if (edge < 0) {
            return -edge - 1;
        }
        return edge;
    }

    private static int findEdge(int faceEdges[], int edge) {
        for (int i = 0; i < faceEdges.length; i++) {
            if (reverse(faceEdges[i]) == edge) {
                return i;
            }
        }
        return -1;
    }

    Vec3f getNormal(int index) {
        float normalsData[] = normals.get();
        return new Vec3f(normalsData[index * 3], normalsData[index * 3 + 1], normalsData[index * 3 + 2]);
    }

    private static final float normalAngle = 0.9994f; // cos(2)

    private static boolean isNormalsEqual(Vec3f n1, Vec3f n2) {
        if (n1.x == 1.0e20f || n1.y == 1.0e20f || n1.z == 1.0e20f
                || n2.x == 1.0e20f || n2.y == 1.0e20f || n2.z == 1.0e20f) {
            //System.out.println("unlocked normal found, skipping");
            return false;
        }
        Vec3f myN1 = new Vec3f(n1);
        myN1.normalize();
        Vec3f myN2 = new Vec3f(n2);
        myN2.normalize();
        return myN1.dot(myN2) >= normalAngle;
    }

    private static void dumpNormals(MFloat3Array normals) {
        System.out.println("normals size = " + normals.getSize());
        float data[] = normals.get();
        for (int i = 0; i < normals.getSize(); i++) {
            String out = String.format("%3d: %8.5f %8.5f %8.5f", i, data[i * 3], data[i * 3 + 1], data[i * 3 + 2]);
            System.out.println(out);
        }
    }

    private static int getEdgeSubindex(int[] faceEdges, int edge, int n) {
        if (faceEdges[edge] < 0) {
            n = 1 - n;
        }
        return edge + n < faceEdges.length ? edge + n : 0;
    }

    private Map<Integer, List<Integer>> getSmoothEdges(Map<Integer, List<Integer>> adjacentFaces) {
        Map<Integer, List<Integer>> smoothEdges = new HashMap<Integer, List<Integer>>();

        for (int face = 0; face < faces.size(); face++) {
            MPolyFace.FaceData faceData = faces.get(face);
            int[] faceEdges = faceData.getFaceEdges();
            //System.out.println("face = " + face + ", edges = " + Arrays.toString(faceEdges));
            for (int e = 0; e < faceEdges.length; e++) {
                int edge = reverse(faceEdges[e]);
                List<Integer> adjFaces = adjacentFaces.get(edge);
                if (adjFaces == null || adjFaces.size() != 2) {
                    // should never happen
                    // actually could happen when we skip edges!
                    //System.out.println("adjacent faces for edge " + faceEdges[e] + " : " + adjFaces);
                    continue;
                }
                int adjFace = adjFaces.get(adjFaces.get(0) == face ? 1 : 0);
                MPolyFace.FaceData adjFaceData = faces.get(adjFace);
                int e2 = findEdge(adjFaceData.getFaceEdges(), edge);
                if (e2 == -1) {
                    System.out.println("Can't find edge " + edge + " in face " + adjFace);
                    System.out.println(Arrays.asList(adjFaceData.getFaceEdges()));
                    continue;
                }
                boolean smooth = true;
                //TODO: loop through normals themselves instead of edges
                for (int n = 0; n < 2; n++) {
                    int n1 = getNormalIndex(face, getEdgeSubindex(faceEdges, e, n));
                    int n2 = getNormalIndex(adjFace, getEdgeSubindex(adjFaceData.getFaceEdges(), e2, n));
                    boolean eq = isNormalsEqual(getNormal(n1), getNormal(n2));
                    //System.out.println("edge " + edge + ", v" + n + ": " + n1 + (eq ? " == " : " != ") + n2 + " faces: " + adjFaces);
                    smooth &= eq;
                }
                if (smooth) {
                    if (!smoothEdges.containsKey(edge)) {
                        smoothEdges.put(edge, adjFaces);
                    }
                }
            }
        }
        return smoothEdges;
    }

    private List<List<Integer>> calcConnComponents(Map<Integer, List<Integer>> smoothEdges) {
        //System.out.println("smoothEdges = " + smoothEdges);
        List<List<Integer>> groups = new ArrayList<List<Integer>>();
        while (hasNextConnectedComponent()) {
            List<Integer> smoothGroup = getNextConnectedComponent(smoothEdges);
            groups.add(smoothGroup);
        }
        if (verbose) {
            dumpConnComponents(groups);
        }
        return groups;
    }

    private void dumpConnComponents(List<List<Integer>> groups) {
        System.out.println("Connected components size = " + groups.size());
        for (int i = 0; i < groups.size(); i++) {
            List<Integer> group = groups.get(i);
            System.out.format("%2d(size = %d): %s", i, group.size(), group).println();
        }
    }

    private void dumpSmGroups(int[] smGroups) {
        System.out.println("smGroups size = " + smGroups.length);
        for (int i = 0; i < smGroups.length; i++) {
            System.out.format("%3d: %d", i, smGroups[i]).println();
        }
    }

    private int[] generateSmGroups(List<List<Integer>> groups) {
        int[] smGroups = new int[faces.size()];
        int curGroup = 0;
        int nonEmptyGroupsCount = 0;
        for (int i = 0; i < groups.size(); i++) {
            List<Integer> list = groups.get(i);
            if (list.size() == 1) {
                smGroups[list.get(0)] = 0;
            } else {
                for (int j = 0; j < list.size(); j++) {
                    Integer faceIndex = list.get(j);
                    smGroups[faceIndex] = 1 << curGroup;
                }
                if (curGroup++ == 31) {
                    curGroup = 0;
                }
                nonEmptyGroupsCount++;
            }
        }
        if (verbose) {
            dumpSmGroups(smGroups);
        }
        if (trace || verbose) {
            System.out.println("Smoothing groups count = " + nonEmptyGroupsCount);
        }
        return smGroups;
    }

    private static int calcLockedNormals(List<MPolyFace.FaceData> faces) {
        int offset = 0;
        for (MPolyFace.FaceData faceData : faces) {
            offset += faceData.getFaceEdges().length;
        }
        return offset;
    }

    private static boolean canCalcSmoothGroups(List<MPolyFace.FaceData> faces, MFloat3Array normals) {
        if (normals != null) {
            int lockedNormalsCount = calcLockedNormals(faces);
            int normalsCount = normals.getSize();
            if (lockedNormalsCount == normalsCount) {
                return true;
            }
            if (trace || verbose) {
                System.out.println(
                        "Can't generate smoothing groups, normalsCount = "
                                + normalsCount + ", lockedNormalsCount = " + lockedNormalsCount);
            }
        }
        return false;
    }

    private int[] calcSmoothGroups() {
        if (verbose) {
            dumpNormals(normals);
        }

        calcNormalsOffsets();

        // edge -> [faces]
        Map<Integer, List<Integer>> adjacentFaces = getAdjacentFaces();

        // smooth edge -> [faces]
        Map<Integer, List<Integer>> smoothEdges = getSmoothEdges(adjacentFaces);

        //System.out.println("smoothEdges = " + smoothEdges);
        List<List<Integer>> groups = calcConnComponents(smoothEdges);

        return generateSmGroups(groups);
    }

    public static int[] calcSmoothGroups(List<MPolyFace.FaceData> faces, MFloat3Array normals) {
        if (canCalcSmoothGroups(faces, normals)) {
            SmoothGroups smoothGroups = new SmoothGroups(faces, normals);
            return smoothGroups.calcSmoothGroups();
        }
        int[] smGroups = new int[faces.size()];
        Arrays.fill(smGroups, 1);
        return smGroups;
    }
}
