/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import com.sun.javafx.scene.NodeHelper;
import test.com.sun.javafx.test.NodeOrientationTestBase;
import java.util.stream.Stream;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class Node_effectiveOrientation_Test
        extends NodeOrientationTestBase {

    private static Scene lriiliScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhAutGroup(
                           inhAutGroup(
                               ltrAutGroup(
                                   inhAutGroup())))));
    }

    private static Scene lriiliWithSubSceneScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhSubScene(
                           inhAutGroup(
                               ltrAutGroup(
                                   inhAutGroup())))));
    }

    private static Scene liirliPrecachedScene() {
        final Scene scene =
                ltrScene(
                    inhAutGroup(
                        inhAutGroup(
                            rtlAutGroup(
                                ltrAutGroup(
                                    inhAutGroup())))));
        // force caching
        collectOrientation(scene);
        return scene;
    }

    private static Scene riirliPlugedPrecachedScenegraphScene() {
        final Group root =
                inhAutGroup(
                    inhAutGroup(
                        rtlAutGroup(
                            ltrAutGroup(
                                inhAutGroup()))));
        // force caching
        collectOrientation(root);

        final Scene scene = new Scene(new Group());
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        scene.setRoot(root);

        return scene;
    }

    private static Scene lrIiilScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhManGroup(
                           inhAutGroup(
                               inhAutGroup(
                                   ltrAutGroup())))));
    }

    /*
     * Parameters: [testScene], [orientationUpdate], [expectedOrientation]
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(lriiliScene(), "......", "LRRRLL" ),
            Arguments.of(lriiliScene(), ".I....", "LLLLLL" ),
            Arguments.of(lriiliScene(), "...L..", "LRRLLL" ),
            Arguments.of(lriiliScene(), "....I.", "LRRRRR" ),
            Arguments.of(lriiliScene(), "RIIIII", "RRRRRR" ),

            Arguments.of(
                lriiliWithSubSceneScene(),
                ".......", "LRRRLL"
            ),
            Arguments.of(
                lriiliWithSubSceneScene(),
                ".L.....", "LLLLLL"
            ),

            Arguments.of(liirliPrecachedScene(), "......", "LLLRLL" ),
            Arguments.of(liirliPrecachedScene(), "R.....", "RRRRLL" ),
            Arguments.of(liirliPrecachedScene(), "...I..", "LLLLLL" ),
            Arguments.of(liirliPrecachedScene(), "R..IR.", "RRRRRR" ),

            Arguments.of(
                riirliPlugedPrecachedScenegraphScene(),
                "......", "RRRRLL"
            ),

            Arguments.of(lrIiilScene(), "......", "LRRRRL" ),
            Arguments.of(lrIiilScene(), ".L....", "LLLLLL" )
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void effectiveOrientationTest(Scene testScene,
                                         String orientationUpdate,
                                         String expectedOrientation) {
        updateOrientation(testScene, orientationUpdate);
        assertOrientation(testScene, expectedOrientation);
    }

    private static void assertOrientation(
            final Scene scene,
            final String expectedOrientation) {
        final String actualOrientation = collectOrientation(scene);
        assertEquals(expectedOrientation, actualOrientation,
                     "Orientation mismatch");
    }

    private static final StateEncoder EFFECTIVE_ORIENTATION_ENCODER =
            new StateEncoder() {
                @Override
                public char map(final Scene scene) {
                    return map(scene.getEffectiveNodeOrientation());
                }

                @Override
                public char map(final Node node) {
                    return map(node.getEffectiveNodeOrientation());
                }

                private char map(final NodeOrientation effectiveOrientation) {
                    switch (effectiveOrientation) {
                        case LEFT_TO_RIGHT:
                            return 'L';
                        case RIGHT_TO_LEFT:
                            return 'R';
                        default:
                            throw new IllegalArgumentException(
                                          "Invalid orientation");
                    }
                }
            };

    private static String collectOrientation(final Scene scene) {
        return collectState(scene, EFFECTIVE_ORIENTATION_ENCODER);
    }

    private static String collectOrientation(final Node node) {
        return collectState(node, EFFECTIVE_ORIENTATION_ENCODER);
    }

    @Test
    public void overlayOrientationIsInheritedOnlyFromSceneOrientation() {
        var node = new Group();
        node.setNodeOrientation(NodeOrientation.INHERIT);
        NodeHelper.setInheritOrientationFromScene(node, true);
        var parent = new Group(node);
        parent.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        var scene = new Scene(parent);
        scene.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        NodeHelper.nodeResolvedOrientationInvalidated(node);
        assertEquals(NodeOrientation.LEFT_TO_RIGHT, node.getEffectiveNodeOrientation());

        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        NodeHelper.nodeResolvedOrientationInvalidated(node);
        assertEquals(NodeOrientation.RIGHT_TO_LEFT, node.getEffectiveNodeOrientation());
    }
}
