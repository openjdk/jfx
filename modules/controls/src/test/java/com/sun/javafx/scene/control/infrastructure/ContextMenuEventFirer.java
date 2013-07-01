/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.infrastructure;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.ContextMenuEvent;


public final class ContextMenuEventFirer {
    
    private ContextMenuEventFirer() {
        // no-op
    }
    
    public static void fireContextMenuEvent(Node target) {
        fireContextMenuEvent(target, 0, 0);
    }
    
    public static void fireContextMenuEvent(Node target, double deltaX, double deltaY) {
        Bounds screenBounds = target.localToScreen(target.getLayoutBounds());
        double screenX = screenBounds.getMaxX() - screenBounds.getWidth() / 2.0 + deltaX;
        double screenY = screenBounds.getMaxY() - screenBounds.getHeight() / 2.0 + deltaY;
        
        ContextMenuEvent evt = new ContextMenuEvent(
                target, 
                target, 
                ContextMenuEvent.CONTEXT_MENU_REQUESTED, 
                deltaX, deltaY, 
                screenX, screenY, 
                false,                                      // keyboardTrigger
                null);                                      // pickResult
        
        Event.fireEvent(target, evt);
    }
}
