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
import java.util.Map.Entry;
import java.util.Set;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.Property;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.transform.Transform;

/**
 * Optimizer to take 3D model and timeline loaded by one of the importers and do as much optimization on
 * the scene graph that was create as we can while still being able to play the given animation.
 */
public class Optimizer {

    private Timeline timeline;
    private Parent root;
    private Set<Transform> bound = new HashSet<>();
    private List<Parent> emptyParents = new ArrayList<>();

    public Optimizer(Timeline timeline, Parent root) {
        this.timeline = timeline;
        this.root = root;
    }

    private int removed, total, groupsTotal, candidate, emptyTransforms;

    public void optimize() {
        removed = 0;
        total = 0;
        candidate = 0;
        emptyTransforms = 0;
        groupsTotal = 0;
        emptyParents.clear();

        parseTimeline();
        optimize(root);
        removeEmptyGroups();

        System.out.printf("removed %d (%.2f%%) out of total %d transforms\n", removed, 100d * removed / total, total);
        System.out.printf("there are %d more multiplications that can be done of matrices that never change\n", candidate);
        System.out.printf("there are %d (%.2f%%) out of total %d groups with no transforms in them\n", emptyTransforms, 100d * emptyTransforms / groupsTotal, groupsTotal);
    }

    private void optimize(Node node) {
        ObservableList<Transform> transforms = node.getTransforms();
        Iterator<Transform> iterator = transforms.iterator();
        boolean prevIsStatic = false;
        while (iterator.hasNext()) {
            Transform transform = iterator.next();
            total++;
            if (transform.isIdentity()) {
                if (timeline == null || !bound.contains(transform)) {
                    iterator.remove();
                    removed++;
                }
            } else {
                if (timeline == null || !bound.contains(transform)) {
                    if (prevIsStatic) {
                        candidate++;
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
                    emptyTransforms++;
//                    System.out.println("Empty group = " + node.getId());
                    emptyParents.add(p);
                } else {
//                    System.err.println("parent is not group = " + parent);
                }
            }
        }
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
                if (prev != null && (prev.keyValue.equals(keyValue) || (prev.first && prev.keyValue.getEndValue().equals(keyValue.getEndValue())))) {
                    KeyInfo prevPrev = prevPrevValues.get(target);
                    if ((prevPrev != null && prevPrev.keyValue.equals(keyValue))
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
