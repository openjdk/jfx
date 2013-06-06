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
package com.javafx.experiments.importers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.Property;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import static javafx.scene.shape.TriangleMesh.*;
import javafx.scene.transform.Transform;

/**
 * Optimizer to take 3D model and timeline loaded by one of the importers and do as much optimization on
 * the scene graph that was create as we can while still being able to play the given animation.
 */
public class Optimizer {

    private Timeline timeline;
    private Node root;
    private Set<Transform> bound = new HashSet<>();
    private List<Parent> emptyParents = new ArrayList<>();
    private List<MeshView> meshViews = new ArrayList<>();

    public Optimizer(Timeline timeline, Node root) {
        this.timeline = timeline;
        this.root = root;
    }

    private int trRemoved, trTotal, groupsTotal, trCandidate, trEmpty;

    public void optimize() {
        trRemoved = 0;
        trTotal = 0;
        trCandidate = 0;
        trEmpty = 0;
        groupsTotal = 0;
        emptyParents.clear();

        parseTimeline();
        optimize(root);
        removeEmptyGroups();
        optimizeMeshes();

        System.out.printf("removed %d (%.2f%%) out of total %d transforms\n", trRemoved, 100d * trRemoved / trTotal, trTotal);
        System.out.printf("there are %d more multiplications that can be done of matrices that never change\n", trCandidate);
        System.out.printf("there are %d (%.2f%%) out of total %d groups with no transforms in them\n", trEmpty, 100d * trEmpty / groupsTotal, groupsTotal);
    }

    private void optimize(Node node) {
        ObservableList<Transform> transforms = node.getTransforms();
        Iterator<Transform> iterator = transforms.iterator();
        boolean prevIsStatic = false;
        while (iterator.hasNext()) {
            Transform transform = iterator.next();
            trTotal++;
            if (transform.isIdentity()) {
                if (timeline == null || !bound.contains(transform)) {
                    iterator.remove();
                    trRemoved++;
                }
            } else {
                if (timeline == null || !bound.contains(transform)) {
                    if (prevIsStatic) {
                        trCandidate++;
                    }
                    prevIsStatic = true;
                } else {
                    prevIsStatic = false;
                }
            }
        }
        if (node instanceof Parent) {
            groupsTotal++;
            Parent p = (Parent) node;
            for (Node n : p.getChildrenUnmodifiable()) {
                optimize(n);
            }
            if (transforms.isEmpty()) {
                Parent parent = p.getParent();
                if (parent instanceof Group) {
                    trEmpty++;
//                    System.out.println("Empty group = " + node.getId());
                    emptyParents.add(p);
                } else {
//                    System.err.println("parent is not group = " + parent);
                }
            }
        }
        if (node instanceof MeshView) {
            meshViews.add((MeshView) node);
        }
    }

    private void optimizeMeshes() {
        optimizePoints();
        optimizeTexCoords();
        optimizeFaces();
    }

    private void optimizeFaces() {
        int total = 0, sameIndexes = 0, samePoints = 0, smallArea = 0;
        for (MeshView meshView : meshViews) {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableIntegerArray faces = mesh.getFaces();
            ObservableFloatArray points = mesh.getPoints();
            for (int i = 0; i < faces.size(); i += NUM_COMPONENTS_PER_FACE) {
                total++;
                int i1 = faces.get(i) * NUM_COMPONENTS_PER_POINT;
                int i2 = faces.get(i + 2) * NUM_COMPONENTS_PER_POINT;
                int i3 = faces.get(i + 4) * NUM_COMPONENTS_PER_POINT;
                if (i1 == i2 || i1 == i3 || i2 == i3) {
                    sameIndexes++;
                    continue;
                }
                Point3D p1 = new Point3D(points.get(i1), points.get(i1 + 1), points.get(i1 + 2));
                Point3D p2 = new Point3D(points.get(i2), points.get(i2 + 1), points.get(i2 + 2));
                Point3D p3 = new Point3D(points.get(i3), points.get(i3 + 1), points.get(i3 + 2));
                if (p1.equals(p2) || p1.equals(p3) || p2.equals(p3)) {
                    samePoints++;
                    continue;
                }
                double a = p1.distance(p2);
                double b = p2.distance(p3);
                double c = p3.distance(p1);
                double p = (a + b + c) / 2;
                double area = p * (p - a) * (p - b) * (p - c);

                final float DEAD_FACE = 1.f/1024/1024/1024/1024;

                if (area < DEAD_FACE) {
                    smallArea++;
                    System.out.printf("a = %e, b = %e, c = %e, area = %e\n"
                            + "p1 = %s\np2 = %s\np3 = %s\n", a, b, c, area, p1.toString(), p2.toString(), p3.toString());
                }
            }
        }
        int badTotal = sameIndexes + samePoints + smallArea;
        System.out.printf("There are %d (%.2f%%) faces with same point indexes, "
                + "%d (%.2f%%) faces with same points, "
                + "%d (%.2f%%) faces with small area. "
                + "Total %d (%.2f%%) bad faces out of %d total.\n",
                sameIndexes, 100d * sameIndexes / total,
                samePoints, 100d * samePoints / total,
                smallArea, 100d * smallArea / total,
                badTotal, 100d * badTotal / total, total);
    }

    private void optimizePoints() {
        int total = 0, duplicates = 0, check = 0;

        Map<Point3D, Integer> pp = new HashMap<>();
        ObservableIntegerArray reindex = FXCollections.observableIntegerArray();
        ObservableFloatArray newPoints = FXCollections.observableFloatArray();

        for (MeshView meshView : meshViews) {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray points = mesh.getPoints();
            int os = points.size() / NUM_COMPONENTS_PER_POINT;

            pp.clear();
            newPoints.clear();
            newPoints.ensureCapacity(points.size());
            reindex.clear();
            reindex.resize(os);

            for (int i = 0, oi = 0, ni = 0; i < points.size(); i += NUM_COMPONENTS_PER_POINT, oi++) {
                float x = points.get(i);
                float y = points.get(i + 1);
                float z = points.get(i + 2);
                Point3D p = new Point3D(x, y, z);
                Integer index = pp.get(p);
                if (index == null) {
                    pp.put(p, ni);
                    reindex.set(oi, ni);
                    newPoints.addAll(x, y, z);
                    ni++;
                } else {
                    reindex.set(oi, index);
                }
            }

            int ns = newPoints.size() / NUM_COMPONENTS_PER_POINT;

            int d = os - ns;
            duplicates += d;
            total += os;

            points.setAll(newPoints);
            points.trimToSize();

            ObservableIntegerArray faces = mesh.getFaces();
            for (int i = 0; i < faces.size(); i += 2) {
                faces.set(i, reindex.get(faces.get(i)));
            }

//            System.out.printf("There are %d (%.2f%%) duplicate points out of %d total for mesh '%s'.\n",
//                    d, 100d * d / os, os, meshView.getId());

            check += mesh.getPoints().size() / NUM_COMPONENTS_PER_POINT;
        }
        System.out.printf("There are %d (%.2f%%) duplicate points out of %d total.\n",
                duplicates, 100d * duplicates / total, total);
        System.out.printf("Now we have %d points.\n", check);
    }

    private void optimizeTexCoords() {
        int total = 0, duplicates = 0, check = 0;

        Map<Point2D, Integer> pp = new HashMap<>();
        ObservableIntegerArray reindex = FXCollections.observableIntegerArray();
        ObservableFloatArray newTexCoords = FXCollections.observableFloatArray();

        for (MeshView meshView : meshViews) {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray texcoords = mesh.getTexCoords();
            int os = texcoords.size() / NUM_COMPONENTS_PER_TEXCOORD;

            pp.clear();
            newTexCoords.clear();
            newTexCoords.ensureCapacity(texcoords.size());
            reindex.clear();
            reindex.resize(os);

            for (int i = 0, oi = 0, ni = 0; i < texcoords.size(); i += NUM_COMPONENTS_PER_TEXCOORD, oi++) {
                float x = texcoords.get(i);
                float y = texcoords.get(i + 1);
                Point2D p = new Point2D(x, y);
                Integer index = pp.get(p);
                if (index == null) {
                    pp.put(p, ni);
                    reindex.set(oi, ni);
                    newTexCoords.addAll(x, y);
                    ni++;
                } else {
                    reindex.set(oi, index);
                }
            }

            int ns = newTexCoords.size() / NUM_COMPONENTS_PER_TEXCOORD;

            int d = os - ns;
            duplicates += d;
            total += os;

            texcoords.setAll(newTexCoords);
            texcoords.trimToSize();

            ObservableIntegerArray faces = mesh.getFaces();
            for (int i = 1; i < faces.size(); i += 2) {
                faces.set(i, reindex.get(faces.get(i)));
            }

//            System.out.printf("There are %d (%.2f%%) duplicate texcoords out of %d total for mesh '%s'.\n",
//                    d, 100d * d / os, os, meshView.getId());

            check += mesh.getTexCoords().size() / NUM_COMPONENTS_PER_TEXCOORD;
        }
        System.out.printf("There are %d (%.2f%%) duplicate texcoords out of %d total.\n",
                duplicates, 100d * duplicates / total, total);
        System.out.printf("Now we have %d texcoords.\n", check);
    }

    private static class KeyInfo {
        KeyFrame keyFrame;
        KeyValue keyValue;
        boolean first;

        public KeyInfo(KeyFrame keyFrame, KeyValue keyValue) {
            this.keyFrame = keyFrame;
            this.keyValue = keyValue;
            first = false;
        }

        public KeyInfo(KeyFrame keyFrame, KeyValue keyValue, boolean first) {
            this.keyFrame = keyFrame;
            this.keyValue = keyValue;
            this.first = first;
        }
    }
    
    private void parseTimeline() {
        bound.clear();
        if (timeline == null) {
            return;
        }
        SortedList<KeyFrame> sortedKeyFrames = timeline.getKeyFrames().sorted(new Comparator<KeyFrame>() {
            @Override public int compare(KeyFrame o1, KeyFrame o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });
        Map<KeyFrame, List<KeyValue>> toRemove = new HashMap<>();
        Map<WritableValue, KeyInfo> prevValues = new HashMap<>();
        Map<WritableValue, KeyInfo> prevPrevValues = new HashMap<>();
        int kvTotal = 0;
        for (KeyFrame keyFrame : sortedKeyFrames) {
            for (KeyValue keyValue : keyFrame.getValues()) {
                WritableValue<?> target = keyValue.getTarget();
                KeyInfo prev = prevValues.get(target);
                kvTotal++;
                if (prev != null && prev.keyValue.getEndValue().equals(keyValue.getEndValue())) {
//                if (prev != null && (prev.keyValue.equals(keyValue) || (prev.first && prev.keyValue.getEndValue().equals(keyValue.getEndValue())))) {
                    KeyInfo prevPrev = prevPrevValues.get(target);
                    if ((prevPrev != null && prevPrev.keyValue.getEndValue().equals(keyValue.getEndValue()))
                            || (prev.first && target.getValue().equals(prev.keyValue.getEndValue()))) {
                        // All prevPrev, prev and current match, so prev can be removed
                        // or prev is first and its value equals to the property existing value, so prev can be removed
                        List<KeyValue> p = toRemove.get(prev.keyFrame);
                        if (p == null) {
                            p = new ArrayList<>();
                            toRemove.put(prev.keyFrame, p);
                        }
                        p.add(prev.keyValue);
                    } else {
                        prevPrevValues.put(target, prev);
                    }
                }
                prevValues.put(target, new KeyInfo(keyFrame, keyValue, prev == null));
            }
        }
        // Deal with ending keyValues
        for (WritableValue target : prevValues.keySet()) {
            KeyInfo prev = prevValues.get(target);
            KeyInfo prevPrev = prevPrevValues.get(target);
            if (prevPrev != null && prevPrev.keyValue.getEndValue().equals(prev.keyValue.getEndValue())) {
                // prevPrev and prev match, so prev can be removed
                List<KeyValue> p = toRemove.get(prev.keyFrame);
                if (p == null) {
                    p = new ArrayList<>();
                    toRemove.put(prev.keyFrame, p);
                }
                p.add(prev.keyValue);
            }
        }
        int kvRemoved = 0;
        int kfRemoved = 0, kfTotal = timeline.getKeyFrames().size(), kfSimplified = 0, kfNotRemoved = 0;
        // Removing unnecessary KeyValues and KeyFrames
        List<KeyValue> newKeyValues = new ArrayList<>();
        for (int i = 0; i < timeline.getKeyFrames().size(); i++) {
            KeyFrame keyFrame = timeline.getKeyFrames().get(i);
            if (toRemove.containsKey(keyFrame)) {
                newKeyValues.clear();
                for (KeyValue keyValue : keyFrame.getValues()) {
                    if (toRemove.get(keyFrame).remove(keyValue)) {
                        kvRemoved++;
                    } else {
                        newKeyValues.add(keyValue);
                    }
                }
                if (newKeyValues.isEmpty()) {
                    if (keyFrame.getOnFinished() == null) {
                        if (keyFrame.getName() != null) {
                            System.err.println("Removed KeyFrame with name = " + keyFrame.getName());
                        }
                        timeline.getKeyFrames().remove(keyFrame);
                        i--;
                        kfRemoved++;
                        continue; // for i
                    } else {
                        kfNotRemoved++;
                    }
                } else {
                    keyFrame = new KeyFrame(keyFrame.getTime(), keyFrame.getName(), keyFrame.getOnFinished(), newKeyValues);
                    timeline.getKeyFrames().set(i, keyFrame);
                    kfSimplified++;
                }
            }
            // collecting bound targets
            for (KeyValue keyValue : keyFrame.getValues()) {
                WritableValue<?> target = keyValue.getTarget();
                if (target instanceof Property) {
                    Property p = (Property) target;
                    Object bean = p.getBean();
                    if (bean instanceof Transform) {
                        bound.add((Transform) bean);
                    } else {
                        throw new UnsupportedOperationException("Bean is not transform, bean = " + bean);
                    }
                } else {
                    throw new UnsupportedOperationException("WritableValue is not property, can't identify what it changes, target = " + target);
                }
            }
        }
//        System.out.println("bound.size() = " + bound.size());
        System.out.printf("Removed %d (%.2f%%) repeating KeyValues out of total %d.\n", kvRemoved, 100d * kvRemoved / kvTotal, kvTotal);
        System.out.printf("Removed %d (%.2f%%) and simplified %d (%.2f%%) KeyFrames out of total %d. %d (%.2f%%) were not removed due to event handler attached.\n",
                kfRemoved, 100d * kfRemoved / kfTotal,
                kfSimplified, 100d * kfSimplified / kfTotal, kfTotal, kfNotRemoved, 100d * kfNotRemoved / kfTotal);
        int check = 0;
        for (KeyFrame keyFrame : timeline.getKeyFrames()) {
            check += keyFrame.getValues().size();
        }
        System.out.printf("Now there are %d KeyValues and %d KeyFrames.\n", check, timeline.getKeyFrames().size());
    }

    private void removeEmptyGroups() {
        for (Parent p : emptyParents) {
            Parent parent = p.getParent();
            Group g = (Group) parent;
            g.getChildren().addAll(p.getChildrenUnmodifiable());
            g.getChildren().remove(p);
        }
    }

}
