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

package test.com.sun.javafx.css;

import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.tk.Toolkit;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.scene.Group;
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

    private abstract class TransitionTimerMock extends TransitionTimer<Number, StyleableDoubleProperty> {
        long now = Toolkit.getToolkit().getPrimaryTimer().nanos();

        TransitionTimerMock() {
            super(new SimpleStyleableDoubleProperty(null, node, "test"));
        }

        public void fire(Duration elapsedTime) {
            now += (long)(elapsedTime.toMillis()) * 1_000_000;
            handle(now);
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
        stage.close();
    }

    @Test
    public void testTimerEndsWithProgressExactlyOne() {
        var trace = new ArrayList<Double>();
        var transition = new TransitionDefinition("test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock() {
            @Override
            protected void onUpdate(StyleableDoubleProperty property, double progress) {
                trace.add(progress);
            }

            @Override
            protected void onStop(StyleableDoubleProperty property) {}
        };

        TransitionTimer.run(timer, transition);

        timer.fire(seconds(0.4));
        assertEquals(1, trace.size());
        assertTrue(trace.get(0) > 0.3 && trace.get(0) < 0.5);

        timer.fire(seconds(0.7));
        assertEquals(2, trace.size());
        assertTrue(trace.get(1) == 1.0); // must be exactly 1

        timer.stop();
    }

    @Test
    public void testTimerStopsWhenProgressIsOne() {
        var flag = new boolean[1];
        var transition = new TransitionDefinition("test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock() {
            @Override
            protected void onUpdate(StyleableDoubleProperty property, double progress) {}

            @Override
            public void onStop(StyleableDoubleProperty property) {
                flag[0] = true;
            }
        };

        TransitionTimer.run(timer, transition);
        timer.fire(seconds(0.9));
        assertFalse(flag[0]);
        timer.fire(seconds(0.2));
        assertTrue(flag[0]);
        timer.stop();
    }

    @Test
    public void testNullTimerIsTriviallyCancelled() {
        assertTrue(TransitionTimer.cancel(null, false));
    }

    @Test
    public void testRunningTimerCanBeCancelled() {
        var transition = new TransitionDefinition("test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock() {
            @Override protected void onUpdate(StyleableDoubleProperty property, double progress) {}
            @Override protected void onStop(StyleableDoubleProperty property) {}
        };

        TransitionTimer.run(timer, transition);
        timer.fire(seconds(0.2));
        assertEquals(1, NodeShim.getTransitionTimers(node).size());
        assertTrue(TransitionTimer.cancel(timer, false));
        assertEquals(0, NodeShim.getTransitionTimers(node).size());
    }

    @Test
    public void testTimerDoesNotStopItselfWhenSettingValue() {
        var flag = new boolean[1];
        var transition = new TransitionDefinition("test", seconds(1), ZERO, LINEAR);
        var timer = new TransitionTimerMock() {
            @Override protected void onUpdate(StyleableDoubleProperty property, double progress) {
                flag[0] = TransitionTimer.cancel(this, false);
            }

            @Override protected void onStop(StyleableDoubleProperty property) {}
        };

        TransitionTimer.run(timer, transition);
        timer.fire(seconds(0.2));
        assertFalse(flag[0]);
        timer.stop();
    }

}
