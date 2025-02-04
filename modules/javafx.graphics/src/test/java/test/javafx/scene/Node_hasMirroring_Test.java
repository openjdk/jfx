/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import test.com.sun.javafx.test.NodeOrientationTestBase;
import java.util.stream.Stream;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Scene;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class Node_hasMirroring_Test extends NodeOrientationTestBase {

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

    private static Scene lrIiilScene() {
        return ltrScene(
                   rtlAutGroup(
                       inhManGroup(
                           inhAutGroup(
                               inhAutGroup(
                                   ltrAutGroup())))));
    }

    /*
     * Parameters: [testScene], [orientationUpdate], [expectedMirroring]
     */
    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( lriiliScene(), "......", ".M..M." ), // LRRRLL
            Arguments.of( lriiliScene(), ".I....", "......" ), // LLLLLL
            Arguments.of( lriiliScene(), "...L..", ".M.M.." ), // LRRLLL
            Arguments.of( lriiliScene(), "....I.", ".M...." ), // LRRRRR
            Arguments.of( lriiliScene(), "RIIIII", ".M...." ), // RRRRRR

            Arguments.of(
                lriiliWithSubSceneScene(),
                "......", ".M..M."
            ),

            /* effective: LRRRLL, automatic: LRLLLL */
            Arguments.of( lrIiliScene(), "......", ".MMMM." ),
            /* effective: LRLRLR, automatic: LRLLLR */
            Arguments.of( lrLRlrScene(), "......", ".MM..M" ),

            /* effective: LRRRRL, automatic: LRLRRL */
            Arguments.of( lrIiilScene(), "...R..", ".MMM.M" )
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void hasMirroringTest(Scene testScene,
                                 String orientationUpdate,
                                 String expectedMirroring) {
        updateOrientation(testScene, orientationUpdate);
        assertMirroring(testScene, expectedMirroring);
    }

    private static void assertMirroring(
            final Scene scene,
            final String expectedMirroring) {
        final String actualMirroring = collectMirroring(scene);
        assertEquals(expectedMirroring, actualMirroring,
                     "Mirroring mismatch");
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
                    return map(NodeShim.hasMirroring(node));
                }

                private char map(final boolean hasMirroring) {
                    return hasMirroring ? 'M' : '.';
                }
            };

    private static String collectMirroring(final Scene scene) {
        return collectState(scene, HAS_MIRRORING_ENCODER);
    }
}
