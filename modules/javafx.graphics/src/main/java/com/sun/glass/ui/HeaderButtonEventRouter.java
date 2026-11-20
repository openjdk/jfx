/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;

/**
 * Decides whether a mouse event in an extended window is sent to the header button overlay or to the application.
 * <p>
 * The overlay draws the window buttons on top of the application's scene, and both receive mouse events from
 * the same view. For example, when the cursor moves from an application button in the header bar directly onto
 * the minimize button, the application must receive EXIT, or its button stays in the hovered state.
 * <p>
 * The router uses the rules that the scene uses for its nodes: The layer under the cursor owns the cursor, and
 * during a press-drag-release gesture, it is captured by the layer on which the press started. When the owner
 * changes, the old layer receives EXIT and the new layer receives ENTER.
 */
final class HeaderButtonEventRouter {

    private enum Layer {
        NONE,
        APPLICATION,
        OVERLAY
    }

    private static final int BUTTON_MODIFIERS =
        KeyEvent.MODIFIER_BUTTON_PRIMARY
        | KeyEvent.MODIFIER_BUTTON_SECONDARY
        | KeyEvent.MODIFIER_BUTTON_MIDDLE
        | KeyEvent.MODIFIER_BUTTON_BACK
        | KeyEvent.MODIFIER_BUTTON_FORWARD;

    private final View view;

    // The layer that owns the cursor and has received ENTER,
    // or NONE while the cursor is outside the window.
    private Layer mouseLayer = Layer.NONE;

    // The layer in which the current press-drag-release gesture started,
    // or NONE if there is no gesture.
    private Layer captureLayer = Layer.NONE;

    HeaderButtonEventRouter(View view) {
        this.view = view;
    }

    /**
     * Returns {@code true} if the event was routed to the overlay, or {@code false} if the view should send
     * it to the application. If the cursor changes its owner, both layers receive EXIT and ENTER first.
     */
    boolean routeEvent(long time, int type, int button, int x, int y, int xAbs, int yAbs,
                       int modifiers, boolean isSynthesized) {
        if (type == MouseEvent.EXIT) {
            Layer owner = mouseLayer;
            mouseLayer = Layer.NONE;

            if (owner == Layer.OVERLAY) {
                handleOverlay(view, type, button, x, y);
                return true;
            }

            return false;
        }

        // A move without pressed buttons means that a release got lost, for example because
        // the application opened a modal dialog on press. This ends the old gesture.
        if (type == MouseEvent.MOVE && (modifiers & BUTTON_MODIFIERS) == 0) {
            captureLayer = Layer.NONE;
        }

        Layer target = captureLayer != Layer.NONE
            ? captureLayer
            : layerAt(view, x, y);

        if (type == MouseEvent.ENTER) {
            mouseLayer = target;
        } else {
            // The native window can send a mouse event without a previous ENTER. The application is the
            // owner in this case, so that it receives EXIT when the cursor moves onto a window button.
            if (mouseLayer == Layer.NONE && captureLayer == Layer.NONE) {
                mouseLayer = Layer.APPLICATION;
            }

            if (mouseLayer != Layer.NONE && mouseLayer != target) {
                // The owner changes before the layers are notified, because an event handler can start
                // a nested event loop that calls the router again.
                Layer previous = mouseLayer;
                mouseLayer = target;
                handleCrossing(view, previous, MouseEvent.EXIT, time, x, y, xAbs, yAbs, modifiers, isSynthesized);
                handleCrossing(view, target, MouseEvent.ENTER, time, x, y, xAbs, yAbs, modifiers, isSynthesized);
            }
        }

        if (type == MouseEvent.DOWN && captureLayer == Layer.NONE) {
            captureLayer = target;
        } else if (type == MouseEvent.UP && (modifiers & BUTTON_MODIFIERS & ~buttonModifier(button)) == 0) {
            // Platforms differ in whether a release event still reports the released button, so we ignore it.
            // The next event picks the layer under the cursor again.
            captureLayer = Layer.NONE;
        }

        if (target == Layer.OVERLAY) {
            handleOverlay(view, type, button, x, y);
            return true;
        }

        return false;
    }

    private static Layer layerAt(View view, int x, int y) {
        return !view.isInFullscreen() // The window buttons are hidden in full-screen mode.
            && view.getWindow() instanceof Window window
            && window.headerButtonOverlayProperty().get() instanceof HeaderButtonOverlay overlay
            && overlay.buttonAt(x / window.getPlatformScaleX(), y / window.getPlatformScaleY()) != null
                ? Layer.OVERLAY
                : Layer.APPLICATION;
    }

    private static void handleCrossing(View view, Layer layer, int type, long time,
                                       int x, int y, int xAbs, int yAbs, int modifiers,
                                       boolean isSynthesized) {
        switch (layer) {
            case OVERLAY -> handleOverlay(view, type, MouseEvent.BUTTON_NONE, x, y);
            case APPLICATION -> view.handleMouseEvent(
                time, type, MouseEvent.BUTTON_NONE, x, y, xAbs, yAbs, modifiers, false, isSynthesized);
        }
    }

    private static void handleOverlay(View view, int type, int button, int x, int y) {
        if (view.getWindow() instanceof Window window
                && window.headerButtonOverlayProperty().get() instanceof HeaderButtonOverlay overlay) {
            overlay.handleMouseEvent(type, button, x / window.getPlatformScaleX(), y / window.getPlatformScaleY());
        }
    }

    private static int buttonModifier(int button) {
        return switch (button) {
            case MouseEvent.BUTTON_LEFT -> KeyEvent.MODIFIER_BUTTON_PRIMARY;
            case MouseEvent.BUTTON_RIGHT -> KeyEvent.MODIFIER_BUTTON_SECONDARY;
            case MouseEvent.BUTTON_OTHER -> KeyEvent.MODIFIER_BUTTON_MIDDLE;
            case MouseEvent.BUTTON_BACK -> KeyEvent.MODIFIER_BUTTON_BACK;
            case MouseEvent.BUTTON_FORWARD -> KeyEvent.MODIFIER_BUTTON_FORWARD;
            default -> 0;
        };
    }
}
