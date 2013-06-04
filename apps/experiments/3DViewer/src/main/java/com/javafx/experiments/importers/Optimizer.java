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
                    System.out.println("Empty group = " + node.getId());
                    emptyParents.add(p);
                } else {
                    System.err.println("parent is not group = " + parent);
                }
            }
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
        List<KeyValue> newKeyValues = new ArrayList<>();
        Map<WritableValue, KeyValue> prevValues = new HashMap<>();
        Map<KeyFrame, KeyFrame> replacements = new HashMap<>();
        int kvRemoved = 0, kvTotal = 0;
        for (KeyFrame keyFrame : sortedKeyFrames) {
            newKeyValues.clear();
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
//                    if (Translate.class.equals(bean.getClass())) {
//                        Translate t = (Translate) bean;
//                    } else if (Rotate.class.equals(bean.getClass())) {
//                        Rotate r = (Rotate) bean;
//                    } else if (Scale.class.equals(bean.getClass())) {
//                        Scale s = (Scale) bean;
//                    }
                } else {
                    throw new UnsupportedOperationException("WritableValue is not property, can't identify what it changes, target = " + target);
                }
                KeyValue prevValue = prevValues.get(target);
                kvTotal++;
                if (prevValue != null) {
                    if (prevValue.getEndValue().equals(keyValue.getEndValue())
                        /*&& prevValue.getInterpolator().equals(keyValue.getInterpolator())*/) {
                        // we can remove this KeyValue
                        kvRemoved ++;
                    } else {
                        if (prevValue.getEndValue().equals(keyValue.getEndValue())) {
                            System.err.println("prevValue = " + prevValue + " != keyValue = " + keyValue);
                        }
                        newKeyValues.add(keyValue);
                    }
                } else {
                    newKeyValues.add(keyValue);
                }
                prevValues.put(target, keyValue);
//                bound.add(keyValue.getTarget());
            }
            if (newKeyValues.size() < keyFrame.getValues().size()) {
                replacements.put(keyFrame, new KeyFrame(keyFrame.getTime(), keyFrame.getName(), keyFrame.getOnFinished(), newKeyValues));
            }
        }
        for (KeyFrame key : replacements.keySet()) {
            timeline.getKeyFrames().set(timeline.getKeyFrames().indexOf(key), replacements.get(key));
        }
        System.out.println("bound.size() = " + bound.size());
        System.out.printf("We've removed %d (%.2f%%) repeating KeyValues out of total %d.\n", kvRemoved, 100d * kvRemoved / kvTotal, kvTotal);
        int check = 0;
        for (KeyFrame keyFrame : timeline.getKeyFrames()) {
            check += keyFrame.getValues().size();
        }
        System.out.printf("Now we have %d KeyValues.\n", check);
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
