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

package test.com.sun.javafx.css;

import com.sun.javafx.css.TransitionMediator;
import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.css.StyleableProperty;
import javafx.css.TransitionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static javafx.animation.Interpolator.*;
import static javafx.util.Duration.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransitionTimerTest {

    private Stage stage;
    private Rectangle node;

    private static class TimerWrapper {
        final TransitionMediator mediator;
        TransitionTimer timer;
        long now;

        TimerWrapper(TransitionMediator mediator) {
            this.mediator = mediator;
        }

        void run(TransitionDefinition definition, Node node) {
            now = Toolkit.getToolkit().getPrimaryTimer().nanos();
            mediator.run(definition, definition.propertyName(), now);
            timer = NodeHelper.findTransitionTimer(node, definition.propertyName());
        }

        void fire(Duration elapsedTime) {
            now += (long)(elapsedTime.toMillis()) * 1_000_000;
            timer.handle(now);
        }
    }

    private class TestTransitionMediator extends TransitionMediator {
        @Override
        public void onUpdate(double progress) {}

        @Override
        public void onStop() {}

        @Override
        public StyleableProperty<?> getStyleableProperty() {
            return (StyleableProperty<?>)node.opacityProperty();
        }

        @Override
        public boolean updateReversingAdjustedStartValue(TransitionMediator existingMediator) {
            return true;
        }
    }

    @BeforeEach
    public void startup() {
        node = new Rectangle();
        stage = new Stage();
        stage.setScene(new Scene(new Group(node)));
        stage.show();
    }

    @AfterEach
    public void teardown() {
        ((Group)stage.getScene().getRoot()).getChildren().clear(); // stops running timers
        stage.close();
    }

    @Test
    public void testTimerEndsWithProgressExactlyOne() {
        var trace = new ArrayList<Double>();
        var transition = new TransitionDefinition("-fx-opacity", seconds(1), ZERO, LINEAR);
        var timer = new TimerWrapper(new TestTransitionMediator() {
            @Override public void onUpdate(double progress) {
                trace.add(progress);
            }
        });

        timer.run(transition, node);
        timer.fire(seconds(0.4));
        assertEquals(1, trace.size());
        assertTrue(trace.get(0) > 0.3 && trace.get(0) < 0.5);

        timer.fire(seconds(0.7));
        assertEquals(2, trace.size());
        assertTrue(trace.get(1) == 1.0); // must be exactly 1
    }

    @Test
    public void testTimerStopsWhenProgressIsOne() {
        var flag = new boolean[1];
        var transition = new TransitionDefinition("-fx-opacity", seconds(1), ZERO, LINEAR);
        var timer = new TimerWrapper(new TestTransitionMediator() {
            @Override public void onStop() {
                flag[0] = true;
            }
        });

        timer.run(transition, node);
        timer.fire(seconds(0.9));
        assertFalse(flag[0]);
        timer.fire(seconds(0.2));
        assertTrue(flag[0]);
    }

    @Test
    public void testRunningTimerCanBeCancelled() {
        var transition = new TransitionDefinition("-fx-opacity", seconds(1), ZERO, LINEAR);
        var timer = new TimerWrapper(new TestTransitionMediator());

        timer.run(transition, node);
        timer.fire(seconds(0.2));
        assertEquals(1, NodeShim.getTransitionTimers(node).size());
        timer.mediator.cancel();
        assertNull(NodeShim.getTransitionTimers(node));
    }

    @Test
    public void testTimerFollowsExpectedOutputProgressCurve() {
        final int steps = 100;
        var expectedOutput = new ArrayList<Double>(steps);
        var actualOutput = new ArrayList<Double>(steps);
        var transition = new TransitionDefinition("-fx-opacity", seconds(1), ZERO, EASE_BOTH);
        var timer = new TimerWrapper(new TestTransitionMediator() {
            @Override public void onUpdate(double progress) {
                actualOutput.add(progress);
            }
        });

        timer.run(transition, node);

        for (int i = 0; i < steps; ++i) {
            double elapsed = 1D / (double)steps;
            timer.fire(seconds(elapsed));

            double progress = (double)(i + 1) / (double)steps;
            expectedOutput.add(EASE_BOTH.interpolate(0D, 1D, progress));
        }

        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testInterruptingTransitionIsShortened() {
        var transition = new TransitionDefinition("-fx-opacity", seconds(1), ZERO, LINEAR);
        var timer1 = new TimerWrapper(new TestTransitionMediator());
        var timer2 = new TimerWrapper(new TestTransitionMediator());
        var trace = new ArrayList<TransitionEvent>();
        node.addEventHandler(TransitionEvent.ANY, trace::add);

        // Start timer1 and advance 0.25s into the first transition.
        timer1.run(transition, node);
        timer1.fire(seconds(0.25));
        assertEquals(2, trace.size());
        assertSame(TransitionEvent.RUN, trace.get(0).getEventType());
        assertSame(TransitionEvent.START, trace.get(1).getEventType());

        // Now we start timer2. This immediately cancels timer1 and adjusts the duration
        // of timer2 so that it completes in less time than specified.
        timer2.run(transition, node);
        assertEquals(4, trace.size());
        assertSame(TransitionEvent.CANCEL, trace.get(2).getEventType());
        assertSame(TransitionEvent.RUN, trace.get(3).getEventType());

        // Advance 0.25s into the second transition, completing timer2.
        timer2.fire(seconds(0.25));
        assertEquals(6, trace.size());
        assertSame(TransitionEvent.START, trace.get(4).getEventType());
        assertSame(TransitionEvent.END, trace.get(5).getEventType());
    }
}
