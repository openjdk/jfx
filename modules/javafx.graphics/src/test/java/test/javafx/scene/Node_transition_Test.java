/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static javafx.animation.Interpolator.*;
import static com.sun.javafx.css.InterpolatorConverter.*;
import static test.javafx.animation.InterpolatorUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class Node_transition_Test {

    private static void assertTransitionEquals(
            String property, Duration duration, Duration delay, Interpolator interpolator,
            TransitionDefinition transition) {
        assertEquals(property, transition.getPropertyName());
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

        List<TransitionDefinition> transitions = NodeShim.getTransitions(node);
        assertEquals(2, transitions.size());
        assertTransitionEquals("-fx-fill", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(0));
        assertTransitionEquals("all", Duration.seconds(2), Duration.ZERO, CSS_EASE_IN_OUT, transitions.get(1));
    }

    @Test
    public void testPropertyNameIsCaseSensitive() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        CssMetaData<?, ?> opacityProperty = Node.getClassCssMetaData().stream()
            .filter(md -> md.getProperty().equals("-fx-opacity"))
            .findFirst().get();

        node.setStyle("transition: -fx-OPACITY 1s");
        node.applyCss();
        assertNull(NodeHelper.findTransitionDefinition(node, opacityProperty));

        node.setStyle("transition: -fx-opacity 1s");
        node.applyCss();
        assertNotNull(NodeHelper.findTransitionDefinition(node, opacityProperty));
    }

    @Test
    public void testAllIdentifierIsCaseInsensitive() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));

        node.setStyle("transition: ALL 1s");
        node.applyCss();
        List<TransitionDefinition> transitions = NodeShim.getTransitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("all", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(0));

        node.setStyle("transition: all 1s");
        node.applyCss();
        transitions = NodeShim.getTransitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("all", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(0));
    }

    @Test
    public void testLastOccurrenceOfMultiplyReferencedPropertyIsSelected() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        node.setStyle("transition: -fx-fill 1s, -fx-fill 2s ease-in-out;");
        node.applyCss();

        CssMetaData<?, ?> propertyMetadata = node.getCssMetaData().stream()
            .filter(m -> m.getProperty().equals("-fx-fill"))
            .findFirst().get();
        TransitionDefinition transition = NodeHelper.findTransitionDefinition(node, propertyMetadata);
        assertTransitionEquals("-fx-fill", Duration.seconds(2), Duration.ZERO, CSS_EASE_IN_OUT, transition);
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

        List<TransitionDefinition> transitions = NodeShim.getTransitions(node);
        assertEquals(3, transitions.size());
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.seconds(0.5), CSS_EASE, transitions.get(0));
        assertTransitionEquals("-fx-scale-x", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(1));
        assertTransitionEquals("-fx-scale-y", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(2));

        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        transitions = NodeShim.getTransitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.ZERO, CSS_EASE, transitions.get(0));
    }

    @Test
    public void testRunningTimersAreTrackedInNode() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass { -fx-opacity: 0; transition: all 1s; }
            .testClass:hover { -fx-opacity: 1; }
            """.getBytes(StandardCharsets.UTF_8));

        StubToolkit tk = (StubToolkit)Toolkit.getToolkit();
        tk.setCurrentTime(0);

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionTimer<?, ?>> timers = NodeShim.getTransitionTimers(node);
        assertNull(timers);

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        timers = NodeShim.getTransitionTimers(node);
        assertEquals(1, timers.size());
        assertNotNull(timers.get(0));

        // Complete the timer, which removes it from the list.
        tk.setCurrentTime(2000);
        tk.handleAnimation();
        assertEquals(0, timers.size());
    }

    @Test
    public void testRunningTimerIsCompletedWhenNodeIsRemovedFromSceneGraph() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass { -fx-opacity: 0; transition: all 1s; }
            .testClass:hover { -fx-opacity: 1; }
            """.getBytes(StandardCharsets.UTF_8));

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionTimer<?, ?>> timers = NodeShim.getTransitionTimers(node);
        assertNull(timers);

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        timers = NodeShim.getTransitionTimers(node);
        assertEquals(1, timers.size());
        assertNotNull(timers.get(0));
        assertTrue(node.getOpacity() < 1);

        // The original node is removed from the scene graph, causing the timer to complete early
        // with the target value of the transition.
        scene.setRoot(new Group());
        assertEquals(0, timers.size());
        assertEquals(1, node.getOpacity(), 0.001);
    }

    @Test
    public void testRunningTimerIsCompletedWhenNodeBecomesInvisible() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass { -fx-opacity: 0; transition: all 1s; }
            .testClass:hover { -fx-opacity: 1; }
            """.getBytes(StandardCharsets.UTF_8));

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionTimer<?, ?>> timers = NodeShim.getTransitionTimers(node);
        assertNull(timers);

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        timers = NodeShim.getTransitionTimers(node);
        assertEquals(1, timers.size());
        assertNotNull(timers.get(0));
        assertTrue(node.getOpacity() < 1);

        // The node is made invisible, causing the timer to complete early with the
        // target value of the transition.
        node.setVisible(false);
        assertEquals(0, timers.size());
        assertEquals(1, node.getOpacity(), 0.001);
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

        List<TransitionDefinition> transitions = NodeShim.getTransitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("-fx-scale-x", Duration.seconds(2), Duration.ZERO, LINEAR, transitions.get(0));
    }

}
