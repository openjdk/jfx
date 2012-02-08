/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.event.EventHandlerManager;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.ScrollEvent;

public final class EventHandlerProperties {
    private final EventHandlerManager eventDispatcher;
    private final Object bean;

    public EventHandlerProperties(
            final EventHandlerManager eventDispatcher,
            final Object bean) {
        this.eventDispatcher = eventDispatcher;
        this.bean = bean;
    }
    
    private EventHandlerProperty<ContextMenuEvent> onMenuContextRequested;

    public final EventHandler<? super ContextMenuEvent> onContextMenuRequested() {
        return (onMenuContextRequested == null) ? null : onMenuContextRequested.get();
    }

    public ObjectProperty<EventHandler<? super ContextMenuEvent>>
            onContextMenuRequestedProperty() {
        if (onMenuContextRequested == null) {
            onMenuContextRequested = new EventHandlerProperty<ContextMenuEvent>(
                                    bean,
                                    "onMenuContextRequested",
                                    ContextMenuEvent.CONTEXT_MENU_REQUESTED);
        }
        return onMenuContextRequested;
    }

    private EventHandlerProperty<MouseEvent> onMouseClicked;

    public final EventHandler<? super MouseEvent> getOnMouseClicked() {
        return (onMouseClicked == null) ? null : onMouseClicked.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseClickedProperty() {
        if (onMouseClicked == null) {
            onMouseClicked = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseClicked",
                                     MouseEvent.MOUSE_CLICKED);
        }
        return onMouseClicked;
    }

    private EventHandlerProperty<MouseEvent> onMouseDragged;

    public final EventHandler<? super MouseEvent> getOnMouseDragged() {
        return (onMouseDragged == null) ? null : onMouseDragged.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseDraggedProperty() {
        if (onMouseDragged == null) {
            onMouseDragged = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseDragged",
                                     MouseEvent.MOUSE_DRAGGED);
        }
        return onMouseDragged;
    }

    private EventHandlerProperty<MouseEvent> onMouseEntered;

    public final EventHandler<? super MouseEvent> getOnMouseEntered() {
        return (onMouseEntered == null) ? null : onMouseEntered.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseEnteredProperty() {
        if (onMouseEntered == null) {
            onMouseEntered = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseEntered",
                                     MouseEvent.MOUSE_ENTERED);
        }
        return onMouseEntered;
    }

    private EventHandlerProperty<MouseEvent> onMouseExited;

    public final EventHandler<? super MouseEvent> getOnMouseExited() {
        return (onMouseExited == null) ? null : onMouseExited.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseExitedProperty() {
        if (onMouseExited == null) {
            onMouseExited = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseExited",
                                     MouseEvent.MOUSE_EXITED);
        }
        return onMouseExited;
    }

    private EventHandlerProperty<MouseEvent> onMouseMoved;

    public final EventHandler<? super MouseEvent> getOnMouseMoved() {
        return (onMouseMoved == null) ? null : onMouseMoved.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseMovedProperty() {
        if (onMouseMoved == null) {
            onMouseMoved = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseMoved",
                                     MouseEvent.MOUSE_MOVED);
        }
        return onMouseMoved;
    }

    private EventHandlerProperty<MouseEvent> onMousePressed;

    public final EventHandler<? super MouseEvent> getOnMousePressed() {
        return (onMousePressed == null) ? null : onMousePressed.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMousePressedProperty() {
        if (onMousePressed == null) {
            onMousePressed = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMousePressed",
                                     MouseEvent.MOUSE_PRESSED);
        }
        return onMousePressed;
    }

    private EventHandlerProperty<MouseEvent> onMouseReleased;

    public final EventHandler<? super MouseEvent> getOnMouseReleased() {
        return (onMouseReleased == null) ? null : onMouseReleased.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onMouseReleasedProperty() {
        if (onMouseReleased == null) {
            onMouseReleased = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onMouseReleased",
                                     MouseEvent.MOUSE_RELEASED);
        }
        return onMouseReleased;
    }

    private EventHandlerProperty<MouseEvent> onDragDetected;

    public final EventHandler<? super MouseEvent> getOnDragDetected() {
        return (onDragDetected == null) ? null : onDragDetected.get();
    }

    public ObjectProperty<EventHandler<? super MouseEvent>>
            onDragDetectedProperty() {
        if (onDragDetected == null) {
            onDragDetected = new EventHandlerProperty<MouseEvent>(
                                     bean,
                                     "onDragDetected",
                                     MouseEvent.DRAG_DETECTED);
        }
        return onDragDetected;
    }

    private EventHandlerProperty<ScrollEvent> onScroll;

    public final EventHandler<? super ScrollEvent> getOnScroll() {
        return (onScroll == null) ? null : onScroll.get();
    }

    public ObjectProperty<EventHandler<? super ScrollEvent>>
            onScrollProperty() {
        if (onScroll == null) {
            onScroll = new EventHandlerProperty<ScrollEvent>(
                                     bean,
                                     "onScroll",
                                     ScrollEvent.SCROLL);
        }
        return onScroll;
    }

    private EventHandlerProperty<MouseDragEvent> onMouseDragOver;

    public final EventHandler<? super MouseDragEvent> getOnMouseDragOver() {
        return (onMouseDragOver == null) ? null : onMouseDragOver.get();
    }

    public ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragOverProperty() {
        if (onMouseDragOver == null) {
            onMouseDragOver = new EventHandlerProperty<MouseDragEvent>(
                                     bean,
                                     "onMouseDragOver",
                                     MouseDragEvent.MOUSE_DRAG_OVER);
        }
        return onMouseDragOver;
    }

    private EventHandlerProperty<MouseDragEvent> onMouseDragReleased;

    public final EventHandler<? super MouseDragEvent> getOnMouseDragReleased() {
        return (onMouseDragReleased == null) ? null : onMouseDragReleased.get();
    }

    public ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragReleasedProperty() {
        if (onMouseDragReleased == null) {
            onMouseDragReleased = new EventHandlerProperty<MouseDragEvent>(
                                     bean,
                                     "onMouseDragReleased",
                                     MouseDragEvent.MOUSE_DRAG_RELEASED);
        }
        return onMouseDragReleased;
    }

    private EventHandlerProperty<MouseDragEvent> onMouseDragEntered;

    public final EventHandler<? super MouseDragEvent> getOnMouseDragEntered() {
        return (onMouseDragEntered == null) ? null : onMouseDragEntered.get();
    }

    public ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragEnteredProperty() {
        if (onMouseDragEntered == null) {
            onMouseDragEntered = new EventHandlerProperty<MouseDragEvent>(
                                     bean,
                                     "onMouseDragEntered",
                                     MouseDragEvent.MOUSE_DRAG_ENTERED);
        }
        return onMouseDragEntered;
    }

    private EventHandlerProperty<MouseDragEvent> onMouseDragExited;

    public final EventHandler<? super MouseDragEvent> getOnMouseDragExited() {
        return (onMouseDragExited == null) ? null : onMouseDragExited.get();
    }

    public ObjectProperty<EventHandler<? super MouseDragEvent>>
            onMouseDragExitedProperty() {
        if (onMouseDragExited == null) {
            onMouseDragExited = new EventHandlerProperty<MouseDragEvent>(
                                     bean,
                                     "onMouseDragExited",
                                     MouseDragEvent.MOUSE_DRAG_EXITED);
        }
        return onMouseDragExited;
    }

    private EventHandlerProperty<KeyEvent> onKeyPressed;

    public final EventHandler<? super KeyEvent> getOnKeyPressed() {
        return (onKeyPressed == null) ? null : onKeyPressed.get();
    }

    public ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyPressedProperty() {
        if (onKeyPressed == null) {
            onKeyPressed = new EventHandlerProperty<KeyEvent>(
                                     bean,
                                     "onKeyPressed",
                                     KeyEvent.KEY_PRESSED);
        }
        return onKeyPressed;
    }

    private EventHandlerProperty<KeyEvent> onKeyReleased;

    public final EventHandler<? super KeyEvent> getOnKeyReleased() {
        return (onKeyReleased == null) ? null : onKeyReleased.get();
    }

    public ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyReleasedProperty() {
        if (onKeyReleased == null) {
            onKeyReleased = new EventHandlerProperty<KeyEvent>(
                                     bean,
                                     "onKeyReleased",
                                     KeyEvent.KEY_RELEASED);
        }
        return onKeyReleased;
    }

    private EventHandlerProperty<KeyEvent> onKeyTyped;

    public final EventHandler<? super KeyEvent> getOnKeyTyped() {
        return (onKeyTyped == null) ? null : onKeyTyped.get();
    }

    public ObjectProperty<EventHandler<? super KeyEvent>>
            onKeyTypedProperty() {
        if (onKeyTyped == null) {
            onKeyTyped = new EventHandlerProperty<KeyEvent>(
                                     bean,
                                     "onKeyTyped",
                                     KeyEvent.KEY_TYPED);
        }
        return onKeyTyped;
    }

    private EventHandlerProperty<InputMethodEvent> onInputMethodTextChanged;

    public final EventHandler<? super InputMethodEvent>
            getOnInputMethodTextChanged() {
        return (onInputMethodTextChanged == null)
                ? null : onInputMethodTextChanged.get();
    }

    public ObjectProperty<EventHandler<? super InputMethodEvent>>
            onInputMethodTextChangedProperty() {
        if (onInputMethodTextChanged == null) {
            onInputMethodTextChanged =
                    new EventHandlerProperty<InputMethodEvent>(
                            bean,
                            "onInputMethodTextChanged",
                            InputMethodEvent.INPUT_METHOD_TEXT_CHANGED);
        }
        return onInputMethodTextChanged;
    }

    private EventHandlerProperty<DragEvent> onDragEntered;

    public final EventHandler<? super DragEvent> getOnDragEntered() {
        return (onDragEntered == null) ? null : onDragEntered.get();
    }

    public ObjectProperty<EventHandler<? super DragEvent>>
            onDragEnteredProperty() {
        if (onDragEntered == null) {
            onDragEntered = new EventHandlerProperty<DragEvent>(
                                    bean,
                                    "onDragEntered",
                                    DragEvent.DRAG_ENTERED);
        }
        return onDragEntered;
    }

    private EventHandlerProperty<DragEvent> onDragExited;

    public final EventHandler<? super DragEvent> getOnDragExited() {
        return (onDragExited == null) ? null : onDragExited.get();
    }

    public ObjectProperty<EventHandler<? super DragEvent>>
            onDragExitedProperty() {
        if (onDragExited == null) {
            onDragExited = new EventHandlerProperty<DragEvent>(
                                    bean,
                                    "onDragExited",
                                    DragEvent.DRAG_EXITED);
        }
        return onDragExited;
    }

    private EventHandlerProperty<DragEvent> onDragOver;

    public final EventHandler<? super DragEvent> getOnDragOver() {
        return (onDragOver == null) ? null : onDragOver.get();
    }

    public ObjectProperty<EventHandler<? super DragEvent>>
            onDragOverProperty() {
        if (onDragOver == null) {
            onDragOver = new EventHandlerProperty<DragEvent>(
                                    bean,
                                    "onDragOver",
                                    DragEvent.DRAG_OVER);
        }
        return onDragOver;
    }

    // Do we want DRAG_TRANSFER_MODE_CHANGED event?
//    private EventHandlerProperty<DragEvent> onDragTransferModeChanged;
//
//    public final EventHandler<? super DragEvent> getOnDragTransferModeChanged() {
//        return (onDragTransferModeChanged == null) ?
//            null : onDragTransferModeChanged.get();
//    }
//
//    public ObjectProperty<EventHandler<? super DragEvent>>
//            onDragTransferModeChanged() {
//        if (onDragTransferModeChanged == null) {
//            onDragTransferModeChanged = new EventHandlerProperty<DragEvent>(
//                                    DragEvent.DRAG_TRANSFER_MODE_CHANGED);
//        }
//        return onDragTransferModeChanged;
//    }

    private EventHandlerProperty<DragEvent> onDragDropped;

    public final EventHandler<? super DragEvent> getOnDragDropped() {
        return (onDragDropped == null) ? null : onDragDropped.get();
    }

    public ObjectProperty<EventHandler<? super DragEvent>>
            onDragDroppedProperty() {
        if (onDragDropped == null) {
            onDragDropped = new EventHandlerProperty<DragEvent>(
                                    bean,
                                    "onDragDropped",
                                    DragEvent.DRAG_DROPPED);
        }
        return onDragDropped;
    }

    private EventHandlerProperty<DragEvent> onDragDone;

    public final EventHandler<? super DragEvent> getOnDragDone() {
        return (onDragDone == null) ? null : onDragDone.get();
    }

    public ObjectProperty<EventHandler<? super DragEvent>>
            onDragDoneProperty() {
        if (onDragDone == null) {
            onDragDone = new EventHandlerProperty<DragEvent>(
                                    bean,
                                    "onDragDone",
                                    DragEvent.DRAG_DONE);
        }
        return onDragDone;
    }

    private final class EventHandlerProperty<T extends Event>
            extends SimpleObjectProperty<EventHandler<? super T>> {
        private final EventType<T> eventType;

        public EventHandlerProperty(final Object bean,
                                    final String name,
                                    final EventType<T> eventType) {
            super(bean, name);
            this.eventType = eventType;
        }

        @Override
        protected void invalidated() {
            eventDispatcher.setEventHandler(eventType, get());
        }
    }
}
