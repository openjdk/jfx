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
public final class Node_hasMirroring_Test {
    private final Scene testScene;
    private final String orientationUpdate;
    private final String expectedMirroring;

    private static Scene lriiliScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhAutGroup(
                           inhAutGroup(
                               ltrAutGroup(
                                   inhAutGroup())))));
    }

    private static Scene lrIiliScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhManGroup(
                           inhAutGroup(
                               ltrAutGroup(
                                   inhAutGroup())))));
    }

    private static Scene lrLRlrScene() {
        return ltrScene(
                   rtlAutGroup(
                       ltrManGroup(
                           rtlManGroup(
                               ltrAutGroup(
                                   rtlAutGroup())))));
    }

    /*
     * Parameters: [testScene], [orientationUpdate], [expectedMirroring]
     */
    @Parameters
    public static Collection data() {
        return Arrays.asList(
                new Object[][] {
                        { lriiliScene(), "......", ".M..M." }, // LRRRLL
                        { lriiliScene(), ".I....", "......" }, // LLLLLL
                        { lriiliScene(), "...L..", ".M.M.." }, // LRRLLL
                        { lriiliScene(), "....I.", ".M...." }, // LRRRRR
                        { lriiliScene(), "RIIIII", ".M...." }, // RRRRRR

                        /* effective: LRRRLL, automatic: LRLLLL */
                        { lrIiliScene(), "......", ".MM..." },
                        /* effective: LRLRLR, automatic: LRLLLR */
                        { lrLRlrScene(), "......", ".MM..M" }
                    });
    }

    public Node_hasMirroring_Test(
            final Scene testScene,
            final String orientationUpdate,
            final String expectedMirroring) {
        this.testScene = testScene;
        this.orientationUpdate = orientationUpdate;
        this.expectedMirroring = expectedMirroring;
    }

    @Test
    public void hasMirroringTest() {
        OrientationHelper.updateOrientation(testScene, orientationUpdate);
        assertMirroring(testScene, expectedMirroring);
    }

    private static Scene ltrScene(final Parent rootNode) {
        final Scene scene = new Scene(rootNode);
        scene.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        return scene;
    }

    private static Group ltrAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.LEFT_TO_RIGHT, childNodes);
    }

    private static Group rtlAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.RIGHT_TO_LEFT, childNodes);
    }

    private static Group inhAutGroup(final Node... childNodes) {
        return autGroup(NodeOrientation.INHERIT, childNodes);
    }

    private static Group ltrManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.LEFT_TO_RIGHT, childNodes);
    }

    private static Group rtlManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.RIGHT_TO_LEFT, childNodes);
    }

    private static Group inhManGroup(final Node... childNodes) {
        return manGroup(NodeOrientation.INHERIT, childNodes);
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
                                    public boolean isAutomaticallyMirrored() {
                                        return false;
                                    }
                                };
        group.setNodeOrientation(nodeOrientation);
        group.getChildren().setAll(childNodes);

        return group;
    }

    private static void assertMirroring(
            final Scene scene,
            final String expectedMirroring) {
        final String actualMirroring = collectMirroring(scene);
        Assert.assertEquals("Mirroring mismatch",
                            expectedMirroring, actualMirroring);
    }

    private static final StateEncoder HAS_MIRRORING_ENCODER =
            new StateEncoder() {
                @Override
                public char map(final Scene scene) {
                    // no mirroring on scene
                    return map(false);
                }

                @Override
                public char map(final Node node) {
                    return map(node.hasMirroring());
                }

                private char map(final boolean hasMirroring) {
                    return hasMirroring ? 'M' : '.';
                }
            };

    private static String collectMirroring(final Scene scene) {
        return OrientationHelper.collectState(scene, HAS_MIRRORING_ENCODER);
    }
}
