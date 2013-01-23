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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class OrientationHelper {
    private OrientationHelper() {
    }

    public interface StateEncoder {
        char map(Scene scene);
        char map(Node node);
    }

    public static void updateOrientation(final Scene scene,
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

    public static String collectState(final Scene scene,
                                      final StateEncoder encoder) {
        final StringBuilder dest = new StringBuilder();
        collectState(dest, scene, encoder);
        return dest.toString();
    }

    public static String collectState(final Node node,
                                      final StateEncoder encoder) {
        final StringBuilder dest = new StringBuilder();
        collectState(dest, node, encoder);
        return dest.toString();
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
        }
    }

}
