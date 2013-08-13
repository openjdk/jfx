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

package com.sun.javafx.test;

import java.util.List;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;

public abstract class NodeOrientationTestBase {
    protected NodeOrientationTestBase() {
    }

    public interface StateEncoder {
        char map(Scene scene);
        char map(Node node);
    }

    protected static Scene ltrScene(final Parent rootNode) {
        final Scene scene = new Scene(rootNode);
        scene.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        return scene;
    }

    protected static Group ltrAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.LEFT_TO_RIGHT, childNodes);
    }

    protected static Group rtlAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.RIGHT_TO_LEFT, childNodes);
    }

    protected static Group inhAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.INHERIT, childNodes);
    }

    protected static SubScene inhSubScene(final Parent rootNode) {
        final SubScene subScene = new SubScene(rootNode, 400, 300);
        subScene.setNodeOrientation(NodeOrientation.INHERIT);
        return subScene;
    }

    protected static Group ltrManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.LEFT_TO_RIGHT, childNodes);
    }

    protected static Group rtlManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.RIGHT_TO_LEFT, childNodes);
    }

    protected static Group inhManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.INHERIT, childNodes);
    }

    protected static void updateOrientation(final Scene scene,
                                            final String updateString) {
        final NodeOrientation update =
                decode(updateString.charAt(0));
        if (update != null) {
            scene.setNodeOrientation(update);
        }

        final Node rootNode = scene.getRoot();
        if (rootNode != null) {
            updateOrientation(rootNode, updateString, 1);
        }
    }

    protected static String collectState(final Scene scene,
                                         final StateEncoder encoder) {
        final StringBuilder dest = new StringBuilder();
        collectState(dest, scene, encoder);
        return dest.toString();
    }

    protected static String collectState(final Node node,
                                         final StateEncoder encoder) {
        final StringBuilder dest = new StringBuilder();
        collectState(dest, node, encoder);
        return dest.toString();
    }

    private static Group autGroup(final NodeOrientation nodeOrientation,
                                  final Node... childNodes) {
        final Group group = new Group();
        group.setNodeOrientation(nodeOrientation);
        group.getChildren().setAll(childNodes);

        return group;
    }

    private static Group manGroup(final NodeOrientation nodeOrientation,
                                  final Node... childNodes) {
        final Group group = new Group() {
                                    @Override
                                    public boolean usesMirroring() {
                                        return false;
                                    }
                                };
        group.setNodeOrientation(nodeOrientation);
        group.getChildren().setAll(childNodes);

        return group;
    }

    private static int updateOrientation(final Node node,
                                         final String updateString,
                                         final int index) {
        final NodeOrientation update =
                decode(updateString.charAt(index));
        if (update != null) {
            node.setNodeOrientation(update);
        }

        int nextIndex = index + 1;
        if (node instanceof Parent) {
            final List<Node> childNodes =
                    ((Parent) node).getChildrenUnmodifiable();
            for (final Node childNode: childNodes) {
                nextIndex = updateOrientation(childNode, updateString,
                                              nextIndex);
            }
        } else if (node instanceof SubScene) {
            final Node nextRoot = ((SubScene) node).getRoot();
            nextIndex = updateOrientation(nextRoot, updateString,
                                          nextIndex);
        }

        return nextIndex;
    }

    private static NodeOrientation decode(final char updateChar) {
        switch (updateChar) {
            case '.':
                return null;
            case 'L':
                return NodeOrientation.LEFT_TO_RIGHT;
            case 'R':
                return NodeOrientation.RIGHT_TO_LEFT;
            case 'I':
                return NodeOrientation.INHERIT;
            default:
                throw new IllegalArgumentException("Invalid update character");
        }
    }

    private static void collectState(final StringBuilder dest,
                                     final Scene scene,
                                     final StateEncoder encoder) {
        dest.append(encoder.map(scene));
        final Node rootNode = scene.getRoot();
        if (rootNode != null) {
            collectState(dest, rootNode, encoder);
        }
    }

    private static void collectState(final StringBuilder dest,
                                     final Node node,
                                     final StateEncoder encoder) {
        dest.append(encoder.map(node));
        if (node instanceof Parent) {
            final List<Node> childNodes =
                    ((Parent) node).getChildrenUnmodifiable();
            for (final Node childNode: childNodes) {
                collectState(dest, childNode, encoder);
            }
        } else if (node instanceof SubScene) {
            final Node nextRoot = ((SubScene) node).getRoot();
            collectState(dest, nextRoot, encoder);
        }
    }
}
