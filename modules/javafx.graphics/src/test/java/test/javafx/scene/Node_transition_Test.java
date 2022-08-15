/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.TransitionDefinition;
import javafx.css.TransitionPropertySelector;
import javafx.scene.Group;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static test.javafx.animation.InterpolatorUtils.*;

public class Node_transition_Test {

    private static void assertTransitionEquals(
            String property, Duration duration, Duration delay, Interpolator interpolator,
            TransitionDefinition transition) {
        if (property.equals("all")) {
            assertEquals(TransitionPropertySelector.ALL, transition.getSelector());
            assertNull(transition.getProperty());
        } else {
            assertEquals(TransitionPropertySelector.CSS, transition.getSelector());
            assertEquals(property, transition.getProperty());
        }

        assertEquals(duration, transition.getDuration());
        assertEquals(delay, transition.getDelay());
        assertInterpolatorEquals(interpolator, transition.getInterpolator());
    }

    @Test
    public void testInlineStyleTransitionIsApplied() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        node.setStyle("transition: -fx-fill 1s, ALL 2s ease-in-out;");
        node.applyCss();

        TransitionDefinition[] transitions = NodeShim.getTransitionsProperty(node).getValue();
        assertEquals(2, transitions.length);
        assertTransitionEquals("-fx-fill", Duration.seconds(1), Duration.ZERO, LINEAR, transitions[0]);
        assertTransitionEquals("all", Duration.seconds(2), Duration.ZERO, EASE_IN_OUT, transitions[1]);
    }

    @Test
    public void testLastOccurrenceOfMultiplyReferencedPropertyIsSelected() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        node.setStyle("transition: fill 1s, -fx-fill 2s ease-in-out;");
        node.applyCss();

        CssMetaData<?, ?> propertyMetadata = node.getCssMetaData().stream()
            .filter(m -> m.getProperty().equals("-fx-fill"))
            .findFirst().get();
        TransitionDefinition transition = NodeHelper.findTransition(node, propertyMetadata);
        assertTransitionEquals("-fx-fill", Duration.seconds(2), Duration.ZERO, EASE_IN_OUT, transition);
    }

    @Test
    public void testTransitionsAreAppliedWhenPseudoClassIsChanged() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-background-color: green;
                transition: -fx-background-color 1s 0.5s ease, -fx-scale-x 1s, -fx-scale-y 1s;
            }

            .testClass:hover {
                -fx-background-color: red;
                -fx-scale-x: 1.2;
                -fx-scale-y: 1.2;
                transition: -fx-background-color 1s;
            }
            """.getBytes(StandardCharsets.UTF_8));

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        TransitionDefinition[] transitions = NodeShim.getTransitionsProperty(node).getValue();
        assertEquals(3, transitions.length);
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.seconds(0.5), EASE, transitions[0]);
        assertTransitionEquals("-fx-scale-x", Duration.seconds(1), Duration.ZERO, LINEAR, transitions[1]);
        assertTransitionEquals("-fx-scale-y", Duration.seconds(1), Duration.ZERO, LINEAR, transitions[2]);

        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        transitions = NodeShim.getTransitionsProperty(node).getValue();
        assertEquals(1, transitions.length);
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.ZERO, LINEAR, transitions[0]);
    }

    @Test
    @Disabled("CssParser cannot handle mixed long-hand/short-hand declarations")
    public void testLonghandDeclarationIsMergedWithShorthandDeclaration() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                transition: -fx-background-color 1s, -fx-scale-x 2s, -fx-scale-y 3s;
                transition-property: -fx-scale-x;
            }
            """.getBytes(StandardCharsets.UTF_8));

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        TransitionDefinition[] transitions = NodeShim.getTransitionsProperty(node).getValue();
        assertEquals(1, transitions.length);
        assertTransitionEquals("-fx-scale-x", Duration.seconds(2), Duration.ZERO, LINEAR, transitions[0]);
    }

}
