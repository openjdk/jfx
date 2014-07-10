/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.stage.FocusUngrabEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.InputEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.TransformChangedEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

@RunWith(Parameterized.class)
public class EventAnyTest {
    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { ActionEvent.ANY,           actionEvent(),           true},
            { ActionEvent.ANY,           focusUngrabEvent(),      false},
            { FocusUngrabEvent.ANY,      focusUngrabEvent(),      true},
            { FocusUngrabEvent.ANY,      actionEvent(),           false},
            { ContextMenuEvent.ANY,      contextMenuEvent(),      true},
            { ContextMenuEvent.ANY,      actionEvent(),           false},
            { DragEvent.ANY,             dragEvent(),             true },
            { DragEvent.ANY,             keyEvent(),              false },
            { InputMethodEvent.ANY,      inputMethodEvent(),      true },
            { InputMethodEvent.ANY,      keyEvent(),              false },
            { KeyEvent.ANY,              keyEvent(),              true },
            { KeyEvent.ANY,              inputMethodEvent(),      false },
            { MouseDragEvent.ANY,        mouseDragEvent(),        true },
            { MouseDragEvent.ANY,        mouseEvent(),            false },
            { MouseEvent.ANY,            mouseEvent(),            true },
            { MouseEvent.ANY,            mouseDragEvent(),        true },
            { MouseEvent.ANY,            keyEvent(),              false },
            { RotateEvent.ANY,           rotateEvent(),           true },
            { RotateEvent.ANY,           zoomEvent(),             false },
            { ZoomEvent.ANY,             zoomEvent(),             true },
            { ZoomEvent.ANY,             rotateEvent(),           false },
            { ScrollEvent.ANY,           scrollEvent(),           true },
            { ScrollEvent.ANY,           swipeEvent(),            false },
            { SwipeEvent.ANY,            swipeEvent(),            true },
            { SwipeEvent.ANY,            scrollEvent(),           false },
            { TouchEvent.ANY,            touchEvent(),            true },
            { TouchEvent.ANY,            rotateEvent(),           false },
            { TransformChangedEvent.ANY, transformChangedEvent(), true },
            { TransformChangedEvent.ANY, mouseEvent(),            false },
            { WindowEvent.ANY,           windowEvent(),           true },
            { WindowEvent.ANY,           actionEvent(),           false },
            { GestureEvent.ANY,          rotateEvent(),           true },
            { GestureEvent.ANY,          mouseEvent(),            false },
            { InputEvent.ANY,            mouseEvent(),            true },
            { InputEvent.ANY,            actionEvent(),           false },
        });
    }

    private boolean delivered;
    private EventType type;
    private Event event;
    private boolean matches;

    public EventAnyTest(EventType type, Event event, boolean matches) {
        this.type = type;
        this.event = event;
        this.matches = matches;
    }

    @Test
    public void testEventDelivery() {
        Node n = new Rectangle();
        delivered = false;

        n.addEventHandler(type, event1 -> {
            delivered = true;
        });

        Event.fireEvent(n, event);
        assertTrue(matches == delivered);
    }

    private static Event actionEvent() {
        return new ActionEvent();
    }

    private static Event focusUngrabEvent() {
        return new FocusUngrabEvent();
    }

    private static Event contextMenuEvent() {
        return new ContextMenuEvent(
                ContextMenuEvent.CONTEXT_MENU_REQUESTED, 10, 10, 10, 10, true,
                null);
    }

    private static Event dragEvent() {
        return new DragEvent(DragEvent.DRAG_DROPPED, null,
                1, 1, 1, 1, TransferMode.MOVE, null, null, null);
    }

    private static Event keyEvent() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, null, null,
                KeyCode.TAB, true, true, true, true);
    }

    private static Event inputMethodEvent() {
        return new InputMethodEvent(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new ArrayList<InputMethodTextRun>(), null, 1);
    }

    private static Event mouseDragEvent() {
        return new MouseDragEvent(MouseDragEvent.MOUSE_DRAG_OVER,
                1, 1, 1, 1, MouseButton.NONE, 1, true, true, true, true, true,
                true, true, true, true, null, null);
    }

    private static Event mouseEvent() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED,
                1, 1, 1, 1, MouseButton.NONE, 1, true, true, true, true, true,
                true, true, true, true, true, null);
    }

    private static Event rotateEvent() {
        return new RotateEvent(RotateEvent.ROTATION_FINISHED,
                1, 1, 1, 1, true, true, true, true, true, true, 1, 1, null);
    }

    private static Event zoomEvent() {
        return new ZoomEvent(ZoomEvent.ZOOM_STARTED,
                1, 1, 1, 1, true, true, true, true, true, true, 1, 1, null);
    }

    private static Event scrollEvent() {
        return new ScrollEvent(ScrollEvent.SCROLL_STARTED,
                1, 1, 1, 1, true, true, true, true, true, true, 1, 1, 1, 1,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 1,
                ScrollEvent.VerticalTextScrollUnits.NONE, 1, 1, null);
    }

    private static Event swipeEvent() {
        return new SwipeEvent(SwipeEvent.SWIPE_DOWN, 1, 1, 1, 1,
                true, true, true, true, true, 1, null);
    }

    private static Event transformChangedEvent() {
        return new TransformChangedEvent();
    }

    private static Event windowEvent() {
        return new WindowEvent(new Stage(), WindowEvent.WINDOW_SHOWN);
    }

    private static Event touchEvent() {
        TouchPoint tp = new TouchPoint(
                1, TouchPoint.State.MOVED, 1, 1, 1, 1, null, null);

        return new TouchEvent(TouchEvent.TOUCH_MOVED, tp,
                new ArrayList<>(Arrays.asList(new TouchPoint[] { tp })),
                1, true, true, true, true);
    }
}
