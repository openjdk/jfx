/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.importers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.Interpolator;
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
import javafx.scene.transform.Transform;
import javafx.util.Duration;

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
    private boolean convertToDiscrete = true;

    public Optimizer(Timeline timeline, Node root) {
        this(timeline, root, false);
    }

    public Optimizer(Timeline timeline, Node root, boolean convertToDiscrete) {
        this.timeline = timeline;
        this.root = root;
        this.convertToDiscrete = convertToDiscrete;
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
        ObservableIntegerArray newFaces = FXCollections.observableIntegerArray();
        ObservableIntegerArray newFaceSmoothingGroups = FXCollections.observableIntegerArray();
        for (MeshView meshView : meshViews) {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableIntegerArray faces = mesh.getFaces();
            ObservableIntegerArray faceSmoothingGroups = mesh.getFaceSmoothingGroups();
            ObservableFloatArray points = mesh.getPoints();
            newFaces.clear();
            newFaces.ensureCapacity(faces.size());
            newFaceSmoothingGroups.clear();
            newFaceSmoothingGroups.ensureCapacity(faceSmoothingGroups.size());
            int pointElementSize = mesh.getPointElementSize();
            int faceElementSize = mesh.getFaceElementSize();
            for (int i = 0; i < faces.size(); i += faceElementSize) {
                total++;
                int i1 = faces.get(i) * pointElementSize;
                int i2 = faces.get(i + 2) * pointElementSize;
                int i3 = faces.get(i + 4) * pointElementSize;
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
                double sqarea = p * (p - a) * (p - b) * (p - c);

                final float DEAD_FACE = 1.f/1024/1024/1024/1024; // taken from MeshNormal code

                if (sqarea < DEAD_FACE) {
                    smallArea++;
//                    System.out.printf("a = %e, b = %e, c = %e, sqarea = %e\n"
//                            + "p1 = %s\np2 = %s\np3 = %s\n", a, b, c, sqarea, p1.toString(), p2.toString(), p3.toString());
                    continue;
                }
                newFaces.addAll(faces, i, faceElementSize);
                int fIndex = i / faceElementSize;
                if (fIndex < faceSmoothingGroups.size()) {
                    newFaceSmoothingGroups.addAll(faceSmoothingGroups.get(fIndex));
                }
            }
            faces.setAll(newFaces);
            faceSmoothingGroups.setAll(newFaceSmoothingGroups);
            faces.trimToSize();
            faceSmoothingGroups.trimToSize();
        }
        int badTotal = sameIndexes + samePoints + smallArea;
        System.out.printf("Removed %d (%.2f%%) faces with same point indexes, "
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
            int pointElementSize = mesh.getPointElementSize();
            int os = points.size() / pointElementSize;

            pp.clear();
            newPoints.clear();
            newPoints.ensureCapacity(points.size());
            reindex.clear();
            reindex.resize(os);

            for (int i = 0, oi = 0, ni = 0; i < points.size(); i += pointElementSize, oi++) {
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

            int ns = newPoints.size() / pointElementSize;

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

            check += mesh.getPoints().size() / pointElementSize;
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
            int texcoordElementSize = mesh.getTexCoordElementSize();
            int os = texcoords.size() / texcoordElementSize;

            pp.clear();
            newTexCoords.clear();
            newTexCoords.ensureCapacity(texcoords.size());
            reindex.clear();
            reindex.resize(os);

            for (int i = 0, oi = 0, ni = 0; i < texcoords.size(); i += texcoordElementSize, oi++) {
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

            int ns = newTexCoords.size() / texcoordElementSize;

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

            check += mesh.getTexCoords().size() / texcoordElementSize;
        }
        System.out.printf("There are %d (%.2f%%) duplicate texcoords out of %d total.\n",
                duplicates, 100d * duplicates / total, total);
        System.out.printf("Now we have %d texcoords.\n", check);
    }

    private void cleanUpRepeatingFramesAndValues() {
        ObservableList<KeyFrame> timelineKeyFrames = timeline.getKeyFrames().sorted(new KeyFrameComparator());
//        Timeline timeline;
        int kfTotal = timelineKeyFrames.size(), kfRemoved = 0;
        int kvTotal = 0, kvRemoved = 0;
        Map<Duration, KeyFrame> kfUnique = new HashMap<>();
        Map<WritableValue, KeyValue> kvUnique = new HashMap<>();
        MapOfLists<KeyFrame, KeyFrame> duplicates = new MapOfLists<>();
        Iterator<KeyFrame> iterator = timelineKeyFrames.iterator();
        while (iterator.hasNext()) {
            KeyFrame duplicate = iterator.next();
            KeyFrame original = kfUnique.put(duplicate.getTime(), duplicate);
            if (original != null) {
                kfRemoved++;
                iterator.remove(); // removing duplicate keyFrame
                duplicates.add(original, duplicate);

                kfUnique.put(duplicate.getTime(), original);
            }
            kvUnique.clear();
            for (KeyValue kvDup : duplicate.getValues()) {
                kvTotal++;
                KeyValue kvOrig = kvUnique.put(kvDup.getTarget(), kvDup);
                if (kvOrig != null) {
                    kvRemoved++;
                    if (!kvOrig.getEndValue().equals(kvDup.getEndValue()) && kvOrig.getTarget() == kvDup.getTarget()) {
                        System.err.println("KeyValues set different values for KeyFrame " + duplicate.getTime() + ":"
                                + "\n kvOrig = " + kvOrig + ", \nkvDup = " + kvDup);
                    }
                }
            }
        }
        for (KeyFrame orig : duplicates.keySet()) {
            List<KeyValue> keyValues = new ArrayList<>();
            for (KeyFrame dup : duplicates.get(orig)) {
                keyValues.addAll(dup.getValues());
            }
            timelineKeyFrames.set(timelineKeyFrames.indexOf(orig),
                    new KeyFrame(orig.getTime(), keyValues.toArray(new KeyValue[keyValues.size()])));
        }
        System.out.printf("Removed %d (%.2f%%) duplicate KeyFrames out of total %d.\n",
                kfRemoved, 100d * kfRemoved / kfTotal, kfTotal);
        System.out.printf("Identified %d (%.2f%%) duplicate KeyValues out of total %d.\n",
                kvRemoved, 100d * kvRemoved / kvTotal, kvTotal);
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

    private static class MapOfLists<K, V> extends HashMap<K, List<V>> {

        public void add(K key, V value) {
            List<V> p = get(key);
            if (p == null) {
                p = new ArrayList<>();
                put(key, p);
            }
            p.add(value);
        }
    }

    private void parseTimeline() {
        bound.clear();
        if (timeline == null) {
            return;
        }
//        cleanUpRepeatingFramesAndValues(); // we don't need it usually as timeline is initially correct
        SortedList<KeyFrame> sortedKeyFrames = timeline.getKeyFrames().sorted(new KeyFrameComparator());
        MapOfLists<KeyFrame, KeyValue> toRemove = new MapOfLists<>();
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
                        toRemove.add(prev.keyFrame, prev.keyValue);
                    } else {
                        prevPrevValues.put(target, prev);
//                        KeyInfo oldKeyInfo = prevPrevValues.put(target, prev);
//                        if (oldKeyInfo != null && oldKeyInfo.keyFrame.getTime().equals(prev.keyFrame.getTime())) {
//                            System.err.println("prevPrev replaced more than once per keyFrame on " + target + "\n"
//                                    + "old = " + oldKeyInfo.keyFrame.getTime() + ", " + oldKeyInfo.keyValue + "\n"
//                                    + "new = " + prev.keyFrame.getTime() + ", " + prev.keyValue
//                                    );
//                        }
                    }
                }
                KeyInfo oldPrev = prevValues.put(target, new KeyInfo(keyFrame, keyValue, prev == null));
                if (oldPrev != null) prevPrevValues.put(target, oldPrev);
            }
        }
        // Deal with ending keyValues
        for (WritableValue target : prevValues.keySet()) {
            KeyInfo prev = prevValues.get(target);
            KeyInfo prevPrev = prevPrevValues.get(target);
            if (prevPrev != null && prevPrev.keyValue.getEndValue().equals(prev.keyValue.getEndValue())) {
                // prevPrev and prev match, so prev can be removed
                toRemove.add(prev.keyFrame, prev.keyValue);
            }
        }
        int kvRemoved = 0;
        int kfRemoved = 0, kfTotal = timeline.getKeyFrames().size(), kfSimplified = 0, kfNotRemoved = 0;
        // Removing unnecessary KeyValues and KeyFrames
        List<KeyValue> newKeyValues = new ArrayList<>();
        for (int i = 0; i < timeline.getKeyFrames().size(); i++) {
            KeyFrame keyFrame = timeline.getKeyFrames().get(i);
            List<KeyValue> keyValuesToRemove = toRemove.get(keyFrame);
            if (keyValuesToRemove != null) {
                newKeyValues.clear();
                for (KeyValue keyValue : keyFrame.getValues()) {
                    if (keyValuesToRemove.remove(keyValue)) {
                        kvRemoved++;
                    } else {
                        if (convertToDiscrete) {
                            newKeyValues.add(new KeyValue((WritableValue)keyValue.getTarget(), keyValue.getEndValue(), Interpolator.DISCRETE));
                        } else {
                            newKeyValues.add(keyValue);
                        }
                    }
                }
            } else if (convertToDiscrete) {
                newKeyValues.clear();
                for (KeyValue keyValue : keyFrame.getValues()) {
                    newKeyValues.add(new KeyValue((WritableValue)keyValue.getTarget(), keyValue.getEndValue(), Interpolator.DISCRETE));
                }
            }
            if (keyValuesToRemove != null || convertToDiscrete) {
                if (newKeyValues.isEmpty()) {
                    if (keyFrame.getOnFinished() == null) {
                        if (keyFrame.getName() != null) {
                            System.err.println("Removed KeyFrame with name = " + keyFrame.getName());
                        }
                        timeline.getKeyFrames().remove(i);
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
//            for (KeyValue keyValue : keyFrame.getValues()) {
//                if (keyValue.getInterpolator() != Interpolator.DISCRETE) {
//                    throw new IllegalStateException();
//                }
//            }
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

    private static class KeyFrameComparator implements Comparator<KeyFrame> {

        public KeyFrameComparator() {
        }

        @Override public int compare(KeyFrame o1, KeyFrame o2) {
//            int compareTo = o1.getTime().compareTo(o2.getTime());
//            if (compareTo == 0 && o1 != o2) {
//                System.err.println("those two KeyFrames are equal: o1 = " + o1.getTime() + " and o2 = " + o2.getTime());
//            }
            return o1.getTime().compareTo(o2.getTime());
        }
    }

}
