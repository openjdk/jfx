/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.css.PseudoClass;
import javafx.css.TransitionEvent;
import javafx.scene.Group;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The following invariants must hold for transition events:
 * 1. For every RUN event there will be exactly one END or CANCEL, never both.
 * 2. For every START event there will be exactly one END or CANCEL, never both.
 * 3. Every END event is preceded by a corresponding START event.
 */
public class Node_transitionEvent_Test {

    private StubToolkit toolkit;
    private Stage stage;
    private Scene scene;
    private Rectangle node;

    @BeforeEach
    public void startup() {
        toolkit = (StubToolkit)Toolkit.getToolkit();
        node = new Rectangle();
        scene = new Scene(new Group(node));
        stage = new Stage();
        stage.setScene(scene);
    }

    @AfterEach
    public void teardown() {
        stage.close();
    }

    @Test
    public void testRegularPlayback() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 0.75s 0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        toolkit.setCurrentTime(0);
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        stage.show();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The transition starts with a delay, which means the elapsed time is 0.
        assertEquals(1, trace.size());
        assertSame(TransitionEvent.RUN, trace.get(0).getEventType());
        assertEquals(Duration.millis(0), trace.get(0).getElapsedTime());

        // After 0.5s, the transition is in the active period (elapsed time was 0 at START).
        toolkit.setCurrentTime(500);
        toolkit.handleAnimation();
        assertEquals(2, trace.size());
        assertSame(TransitionEvent.START, trace.get(1).getEventType());
        assertEquals(Duration.millis(0), trace.get(1).getElapsedTime());

        // After 1s, the transition has already ended (elapsed time was 0.75s at END).
        toolkit.setCurrentTime(1000);
        toolkit.handleAnimation();
        assertEquals(3, trace.size());
        assertSame(TransitionEvent.END, trace.get(2).getEventType());
        assertEquals(Duration.millis(750), trace.get(2).getElapsedTime());
    }

    @Test
    public void testPlaybackIsElidedWhenDurationIsZero() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 0s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        toolkit.setCurrentTime(0);
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        stage.show();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();
        assertEquals(0, trace.size());

        toolkit.setCurrentTime(1);
        toolkit.handleAnimation();
        assertEquals(0, trace.size());
    }

    @Test
    public void testInterruptedPlayback() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 0.75s 0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        toolkit.setCurrentTime(0);
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        stage.show();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The animation advances 500ms and is then cancelled, which means that the
        // elapsed time is 250ms (since we have a 250ms delay).
        toolkit.setCurrentTime(500);
        toolkit.handleAnimation();
        NodeShim.completeTransitionTimers(node);

        assertEquals(3, trace.size());
        assertSame(TransitionEvent.RUN, trace.get(0).getEventType());
        assertSame(TransitionEvent.START, trace.get(1).getEventType());
        assertSame(TransitionEvent.CANCEL, trace.get(2).getEventType());
        assertEquals(Duration.millis(0), trace.get(0).getElapsedTime());
        assertEquals(Duration.millis(0), trace.get(1).getElapsedTime());
        assertEquals(Duration.millis(250), trace.get(2).getElapsedTime());
    }

    @Test
    public void testInterruptedPlaybackWithNegativeDelay() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 1s -0.25s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        toolkit.setCurrentTime(0);
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        stage.show();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The animation advances 500ms and is then cancelled, which means that the
        // elapsed time is 750ms (since we started with a negative 250ms delay).
        toolkit.setCurrentTime(500);
        toolkit.handleAnimation();
        NodeShim.completeTransitionTimers(node);

        assertEquals(3, trace.size());
        assertSame(TransitionEvent.RUN, trace.get(0).getEventType());
        assertSame(TransitionEvent.START, trace.get(1).getEventType());
        assertSame(TransitionEvent.CANCEL, trace.get(2).getEventType());
        assertEquals(Duration.millis(250), trace.get(0).getElapsedTime());
        assertEquals(Duration.millis(250), trace.get(1).getElapsedTime());
        assertEquals(Duration.millis(750), trace.get(2).getElapsedTime());
    }

    @Test
    public void testInterruptedPlaybackDuringDelayPhase() {
        String url = "data:text/css;base64," + Base64.getUrlEncoder().encodeToString("""
            .testClass {
                -fx-opacity: 0;
                transition: -fx-opacity 1s 0.5s;
            }

            .testClass:hover {
                -fx-opacity: 1;
            }
            """.getBytes(StandardCharsets.UTF_8));

        toolkit.setCurrentTime(0);
        scene.getStylesheets().add(url);
        node.getStyleClass().add("testClass");
        stage.show();

        List<TransitionEvent> trace = new ArrayList<>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);
        node.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        node.applyCss();

        // The animation advances 250ms and is then cancelled, which means that we're
        // still in the delay phase. The elapsed time of the CANCEL event will be 0.
        toolkit.setCurrentTime(250);
        toolkit.handleAnimation();
        NodeShim.completeTransitionTimers(node);

        assertEquals(2, trace.size());
        assertSame(TransitionEvent.RUN, trace.get(0).getEventType());
        assertSame(TransitionEvent.CANCEL, trace.get(1).getEventType());
        assertEquals(Duration.millis(0), trace.get(0).getElapsedTime());
        assertEquals(Duration.millis(0), trace.get(1).getElapsedTime());
    }

}
