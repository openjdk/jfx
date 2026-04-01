/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;

/**
 * Helper to fire MouseEvents onto a EventTarget which is either Node or Scene.
 * There are methods to configure the event by eventType, clickCount, location (delta from default),
 * mouseButton, and keyModifiers.
 * <p>
 * The default local coordinates are at the center of the target.
 */
public final class MouseEventFirer {
    private final EventTarget target;

    private final Bounds targetBounds;
    private StageLoader sl;

    public MouseEventFirer(EventTarget target) {
        this.target = target;

        // Force the target node onto a stage so that it is accessible
        if (target instanceof Node n) {
            Scene s = n.getScene();
            Window w = s == null ? null : s.getWindow();

            if (w == null || w.getScene() == null) {
                sl = new StageLoader(n);
                targetBounds = n.getLayoutBounds();
            } else {
                targetBounds = n.getLayoutBounds();
            }
        } else if (target instanceof Scene scene) {
            sl = new StageLoader(scene);
            targetBounds = new BoundingBox(0, 0, scene.getWidth(), scene.getHeight());
        } else {
            throw new RuntimeException("EventTarget of invalid type (" + target + ")");
        }
    }

    public void dispose() {
        if (sl != null) {
            sl.dispose();
        }
    }

    public void fireMousePressAndRelease(KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, modifiers);
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, modifiers);
    }

    public void fireMousePressAndRelease(int clickCount, KeyModifier... modifiers) {
        fireMousePressAndRelease(clickCount, 0, 0, modifiers);
    }

    public void fireMousePressAndRelease(int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, clickCount, deltaX, deltaY, modifiers);
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, clickCount, deltaX, deltaY, modifiers);
    }

    public void fireMouseClicked() {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED);
    }

    public void fireMouseClicked(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, button, 0, 0);
    }

    public void fireMouseClicked(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, deltaX, deltaY);
    }

    public void fireMouseClicked(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_CLICKED, deltaX, deltaY, modifiers);
    }

    public void fireMousePressed() {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED);
    }

    public void fireMousePressed(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, button, 0, 0);
    }

    public void fireMousePressed(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, deltaX, deltaY);
    }

    public void fireMousePressed(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, deltaX, deltaY, modifiers);
    }

    public void fireMousePressed(int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, clickCount, deltaX, deltaY, modifiers);
    }

    public void fireMouseReleased() {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED);
    }

    public void fireMouseReleased(MouseButton button) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, button, 0, 0);
    }

    public void fireMouseReleased(double deltaX, double deltaY) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, deltaX, deltaY);
    }

    public void fireMouseReleased(double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(MouseEvent.MOUSE_RELEASED, deltaX, deltaY, modifiers);
    }

    public void fireMouseEvent(EventType<MouseEvent> evtType, KeyModifier... modifiers) {
        fireMouseEvent(evtType, 0, 0 , modifiers);
    }

    public void fireMouseEvent(EventType<MouseEvent> evtType, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(evtType, MouseButton.PRIMARY, deltaX, deltaY, modifiers);
    }

    public void fireMouseEvent(EventType<MouseEvent> evtType, MouseButton button, double deltaX, double deltaY, KeyModifier... modifiers) {
        fireMouseEvent(evtType, button, 1, deltaX, deltaY, modifiers);
    }

    private void fireMouseEvent(EventType<MouseEvent> evtType, MouseButton button, int clickCount, double deltaX, double deltaY, KeyModifier... modifiers) {
        // width / height of target node
        final double w = targetBounds.getWidth();
        final double h = targetBounds.getHeight();

        // x / y click position is centered
        final double x = w / 2.0 + deltaX;
        final double y = h / 2.0 + deltaY;

        Node node = (Node) target;

        Point2D localP = new Point2D(x, y);
        Point2D sceneP = node.localToScene(localP);
        Point2D screenP = node.localToScreen(localP);

        final List<KeyModifier> ml = Arrays.asList(modifiers);

        MouseEvent evt = new MouseEvent(
                target, // target of this firer
                null,   // default source (don't care, event dispatch will take over)
                evtType,
                sceneP.getX(), sceneP.getY(), // can use scene coordinates because source is null
                screenP.getX(), screenP.getY(),
                button,
                clickCount,
                ml.contains(KeyModifier.SHIFT),    // shiftDown
                ml.contains(KeyModifier.CTRL),     // ctrlDown
                ml.contains(KeyModifier.ALT),      // altDown
                ml.contains(KeyModifier.META),     // metaData
                button == MouseButton.PRIMARY,     // primary button
                button == MouseButton.MIDDLE,      // middle button
                button == MouseButton.SECONDARY,   // secondary button
                button == MouseButton.BACK,        // back button
                button == MouseButton.FORWARD,     // forward button
                false,                             // synthesized
                button == MouseButton.SECONDARY,   // is popup trigger
                true,                              // still since pick
                null    // default pick (don't care, event constructor will take over)
        );

        Event.fireEvent(target, evt);
    }

}
