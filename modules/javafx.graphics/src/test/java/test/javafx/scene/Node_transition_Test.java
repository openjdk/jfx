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

import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.pgstub.StubToolkit;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.TransitionDefinition;
import javafx.css.TransitionEvent;
import javafx.css.TransitionPropertySelector;
import javafx.scene.Group;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

        List<TransitionDefinition> transitions = NodeShim.getTransitionDefinitions(node);
        assertEquals(2, transitions.size());
        assertTransitionEquals("-fx-fill", Duration.seconds(1), Duration.ZERO, LINEAR, transitions.get(0));
        assertTransitionEquals("all", Duration.seconds(2), Duration.ZERO, EASE_IN_OUT, transitions.get(1));
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
    public void testTransitionWithZeroDurationIsElided() {
        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        node.setStyle("transition: -fx-fill 1s, -fx-fill 0s;");
        node.applyCss();

        CssMetaData<?, ?> propertyMetadata = node.getCssMetaData().stream()
            .filter(m -> m.getProperty().equals("-fx-fill"))
            .findFirst().get();

        assertNull(NodeHelper.findTransition(node, propertyMetadata));
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

        List<TransitionDefinition> transitions = NodeShim.getTransitionDefinitions(node);
        assertEquals(3, transitions.size());
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.seconds(0.5), EASE, transitions.get(0));
        assertTransitionEquals("-fx-scale-x", Duration.seconds(1), Duration.ZERO, LINEAR, transitions.get(1));
        assertTransitionEquals("-fx-scale-y", Duration.seconds(1), Duration.ZERO, LINEAR, transitions.get(2));

        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        transitions = NodeShim.getTransitionDefinitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("-fx-background-color", Duration.seconds(1), Duration.ZERO, LINEAR, transitions.get(0));
    }

    @Test
    public void testRunningTimersAreTrackedInNode() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass { -fx-opacity: 0; transition: all 1s; }
            .testClass:hover { -fx-opacity: 1; }
            """.getBytes(StandardCharsets.UTF_8));

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionTimer> timers = NodeShim.getTransitionTimers(node);
        assertEquals(0, timers.size());

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        assertEquals(1, timers.size());

        // Complete the timer, which removes it from the list.
        timers.get(0).update(1);
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

        List<TransitionTimer> timers = NodeShim.getTransitionTimers(node);
        assertEquals(0, timers.size());

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        assertEquals(1, timers.size());
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

        List<TransitionTimer> timers = NodeShim.getTransitionTimers(node);
        assertEquals(0, timers.size());

        // The hover state starts the timer.
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        assertEquals(1, timers.size());
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

        List<TransitionDefinition> transitions = NodeShim.getTransitionDefinitions(node);
        assertEquals(1, transitions.size());
        assertTransitionEquals("-fx-scale-x", Duration.seconds(2), Duration.ZERO, LINEAR, transitions.get(0));
    }

    @Test
    public void testTransitionEventCycle() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 0.75s 0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        StubToolkit tk = (StubToolkit)Toolkit.getToolkit();
        tk.setCurrentTime(0);

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The transition starts with a delay, which means the elapsed time is 0.
        assertEquals(1, trace.size());
        assertEquals(TransitionEvent.RUN.getName(), trace.get(0).getEventType().getName());
        assertEquals(Duration.millis(0), trace.get(0).getElapsedTime());

        // After 0.5s, the transition is in the active period (elapsed time was 0 at START).
        tk.setCurrentTime(500);
        tk.handleAnimation();
        assertEquals(2, trace.size());
        assertEquals(TransitionEvent.START.getName(), trace.get(1).getEventType().getName());
        assertEquals(Duration.millis(0), trace.get(1).getElapsedTime());

        // After 1s, the transition has already ended (elapsed time was 0.75s at END).
        tk.setCurrentTime(1000);
        tk.handleAnimation();
        assertEquals(3, trace.size());
        assertEquals(TransitionEvent.END.getName(), trace.get(2).getEventType().getName());
        assertEquals(Duration.millis(750), trace.get(2).getElapsedTime());
    }

    @Test
    public void testTransitionCancelEvent() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 0.75s 0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        StubToolkit tk = (StubToolkit)Toolkit.getToolkit();
        tk.setCurrentTime(0);

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The animation advances 500ms and is then cancelled, which means that the
        // elapsed time is 250ms (since we have a 250ms delay).
        tk.setCurrentTime(500);
        tk.handleAnimation();
        NodeShim.cancelTransitionTimers(node);

        assertEquals(3, trace.size());
        assertEquals(TransitionEvent.RUN.getName(), trace.get(0).getEventType().getName());
        assertEquals(TransitionEvent.START.getName(), trace.get(1).getEventType().getName());
        assertEquals(TransitionEvent.CANCEL.getName(), trace.get(2).getEventType().getName());
        assertEquals(Duration.millis(0), trace.get(0).getElapsedTime());
        assertEquals(Duration.millis(0), trace.get(1).getElapsedTime());
        assertEquals(Duration.millis(250), trace.get(2).getElapsedTime());
    }

    @Test
    public void testTransitionCancelEventWithNegativeDelay() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 1s -0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        StubToolkit tk = (StubToolkit)Toolkit.getToolkit();
        tk.setCurrentTime(0);

        var node = new Rectangle();
        var scene = new Scene(new Group(node));
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        node.applyCss();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The animation advances 500ms and is then cancelled, which means that the
        // elapsed time is 750ms (since we started with a negative 250ms delay).
        tk.setCurrentTime(500);
        tk.handleAnimation();
        NodeShim.cancelTransitionTimers(node);

        assertEquals(3, trace.size());
        assertEquals(TransitionEvent.RUN.getName(), trace.get(0).getEventType().getName());
        assertEquals(TransitionEvent.START.getName(), trace.get(1).getEventType().getName());
        assertEquals(TransitionEvent.CANCEL.getName(), trace.get(2).getEventType().getName());
        assertEquals(Duration.millis(250), trace.get(0).getElapsedTime());
        assertEquals(Duration.millis(250), trace.get(1).getElapsedTime());
        assertEquals(Duration.millis(750), trace.get(2).getElapsedTime());
    }

}
