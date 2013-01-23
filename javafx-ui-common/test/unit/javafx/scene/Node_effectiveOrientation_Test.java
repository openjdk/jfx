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

package javafx.scene;

import com.sun.javafx.test.OrientationHelper;
import com.sun.javafx.test.OrientationHelper.StateEncoder;
import java.util.Arrays;
import java.util.Collection;
import javafx.geometry.NodeOrientation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class Node_effectiveOrientation_Test {
    private final Scene testScene;
    private final String orientationUpdate;
    private final String expectedOrientation;

    private static Scene lriiliScene() {
        return ltrScene(rtlGroup(inhGroup(inhGroup(ltrGroup(inhGroup())))));
    }

    private static Scene liirliPrecachedScene() {
        final Scene scene =
                ltrScene(inhGroup(inhGroup(rtlGroup(ltrGroup(inhGroup())))));
        // force caching
        collectOrientation(scene);
        return scene;
    }

    private static Scene riirliPlugedPrecachedScenegraphScene() {
        final Group root =
                inhGroup(inhGroup(rtlGroup(ltrGroup(inhGroup()))));
        // force caching
        collectOrientation(root);

        final Scene scene = new Scene(new Group());
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        scene.setRoot(root);

        return scene;
    }

    /*
     * Parameters: [testScene], [orientationUpdate], [expectedOrientation]
     */
    @Parameters
    public static Collection data() {
        return Arrays.asList(
                new Object[][] {
                        { lriiliScene(), "......", "LRRRLL" },
                        { lriiliScene(), ".I....", "LLLLLL" },
                        { lriiliScene(), "...L..", "LRRLLL" },
                        { lriiliScene(), "....I.", "LRRRRR" },
                        { lriiliScene(), "RIIIII", "RRRRRR" },

                        { liirliPrecachedScene(), "......", "LLLRLL" },
                        { liirliPrecachedScene(), "R.....", "RRRRLL" },
                        { liirliPrecachedScene(), "...I..", "LLLLLL" },
                        { liirliPrecachedScene(), "R..IR.", "RRRRRR" },

                        {
                            riirliPlugedPrecachedScenegraphScene(),
                            "......", "RRRRLL"
                        }
                    });
    }

    public Node_effectiveOrientation_Test(
            final Scene testScene,
            final String orientationUpdate,
            final String expectedOrientation) {
        this.testScene = testScene;
        this.orientationUpdate = orientationUpdate;
        this.expectedOrientation = expectedOrientation;
    }

    @Test
    public void effectiveOrientationTest() {
        OrientationHelper.updateOrientation(testScene, orientationUpdate);
        assertOrientation(testScene, expectedOrientation);
    }

    private static Scene ltrScene(final Parent rootNode) {
        final Scene scene = new Scene(rootNode);
        scene.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        return scene;
    }

    private static Group ltrGroup(final Node... childNodes) {
        return group(NodeOrientation.LEFT_TO_RIGHT, childNodes);
    }

    private static Group rtlGroup(final Node... childNodes) {
        return group(NodeOrientation.RIGHT_TO_LEFT, childNodes);
    }

    private static Group inhGroup(final Node... childNodes) {
        return group(NodeOrientation.INHERIT, childNodes);
    }

    private static Group group(final NodeOrientation nodeOrientation,
                               final Node... childNodes) {
        final Group group = new Group();
        group.setNodeOrientation(nodeOrientation);
        group.getChildren().setAll(childNodes);

        return group;
    }

    private static void assertOrientation(
            final Scene scene,
            final String expectedOrientation) {
        final String actualOrientation = collectOrientation(scene);
        Assert.assertEquals("Orientation mismatch",
                            expectedOrientation, actualOrientation);
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
        return OrientationHelper.collectState(scene,
                                              EFFECTIVE_ORIENTATION_ENCODER);
    }

    private static String collectOrientation(final Node node) {
        return OrientationHelper.collectState(node,
                                              EFFECTIVE_ORIENTATION_ENCODER);
    }

}
