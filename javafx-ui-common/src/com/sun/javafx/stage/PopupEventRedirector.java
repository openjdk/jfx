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

package com.sun.javafx.stage;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;

import com.sun.javafx.event.DirectEvent;
import com.sun.javafx.event.EventRedirector;
import com.sun.javafx.event.EventUtil;

/**
 * Defines reaction of a popup to redirected events from its parent stage or
 * popup.
 */
public final class PopupEventRedirector extends EventRedirector {
    private static final KeyCombination ESCAPE_KEY_COMBINATION =
            KeyCombination.keyCombination("Esc");

    private final PopupWindow popupWindow;

    public PopupEventRedirector(final PopupWindow popupWindow) {
        super(popupWindow);
        this.popupWindow = popupWindow;
    }
    
    @Override
    protected void handleRedirectedEvent(final Object eventSource,
                                         final Event event) {
        if (event instanceof KeyEvent) {
            handleKeyEvent((KeyEvent) event);
            return;
        }

        final EventType<?> eventType = event.getEventType();

        if (eventType == MouseEvent.MOUSE_PRESSED) {
            handleMousePressedEvent(eventSource, event);
            return;
        }

        if (eventType == FocusUngrabEvent.FOCUS_UNGRAB) {
            handleFocusUngrabEvent();
            return;
        }
    }

    private void handleKeyEvent(final KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }

        final Scene scene = popupWindow.getScene();
        if (scene != null) {
            final Node sceneFocusOwner = scene.impl_getFocusOwner();
            final EventTarget eventTarget =
                    (sceneFocusOwner != null) ? sceneFocusOwner : scene;
            if (EventUtil.fireEvent(eventTarget, new DirectEvent(event))
                    == null) {
                event.consume();
                return;
            }
        }

        if ((event.getEventType() == KeyEvent.KEY_PRESSED)
                && ESCAPE_KEY_COMBINATION.match(event)) {
            handleEscapeKeyPressedEvent();
        }
    }

    private void handleEscapeKeyPressedEvent() {
        if (popupWindow.isHideOnEscape()) {
            popupWindow.doAutoHide();
        }
    }

    private void handleMousePressedEvent(final Object eventSource,
                                         final Event event) {
        // we handle mouse pressed only for the immediate parent window,
        // where we can check whether the mouse press is inside of the owner
        // control or not, we will force possible child popups to close
        // by sending the FOCUS_UNGRAB event
        if (popupWindow.getOwnerWindow() != eventSource) {
            return;
        }

        if (popupWindow.isAutoHide() && !isOwnerNodeEvent(event)) {
            // the mouse press is outside of the owner control,
            // fire FOCUS_UNGRAB to child popups
            Event.fireEvent(popupWindow, new FocusUngrabEvent());

            popupWindow.doAutoHide();

            // we can consume the press which caused the autohide here,
            // if we do that it won't have any effect in the target window
            // from discussions, not consuming it seems preferable
            // event.consume();
        }
    }

    private void handleFocusUngrabEvent() {
        if (popupWindow.isAutoHide()) {
            popupWindow.doAutoHide();
        }
    }

    private boolean isOwnerNodeEvent(final Event event) {
        final Node ownerNode = popupWindow.getOwnerNode();
        if (ownerNode == null) {
            return false;
        }

        final EventTarget eventTarget = event.getTarget();
        if (!(eventTarget instanceof Node)) {
            return false;
        }

        Node node = (Node) eventTarget;
        do {
            if (node == ownerNode) {
                return true;
            }
            node = node.getParent();
        } while (node != null);

        return false;
    }
}
