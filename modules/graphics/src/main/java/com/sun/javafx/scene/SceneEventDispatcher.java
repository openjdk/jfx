/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.event.BasicEventDispatcher;
import com.sun.javafx.event.CompositeEventDispatcher;
import com.sun.javafx.event.EventHandlerManager;

/**
 * An {@code EventDispatcher} for {@code Scene}. It is formed by a chain
 * of {@code KeyboardShortcutsHandler} followed by {@code EventHandlerManager}.
 */
public class SceneEventDispatcher extends CompositeEventDispatcher {
    private final KeyboardShortcutsHandler keyboardShortcutsHandler;

    private final EnteredExitedHandler enteredExitedHandler;

    private final EventHandlerManager eventHandlerManager;

    public SceneEventDispatcher(final Object eventSource) {
        this(new KeyboardShortcutsHandler(),
             new EnteredExitedHandler(eventSource),
             new EventHandlerManager(eventSource));

    }

    public SceneEventDispatcher(
            final KeyboardShortcutsHandler keyboardShortcutsHandler,
            final EnteredExitedHandler enteredExitedHandler,
            final EventHandlerManager eventHandlerManager) {
        this.keyboardShortcutsHandler = keyboardShortcutsHandler;
        this.enteredExitedHandler = enteredExitedHandler;
        this.eventHandlerManager = eventHandlerManager;

        keyboardShortcutsHandler.insertNextDispatcher(enteredExitedHandler);
        enteredExitedHandler.insertNextDispatcher(eventHandlerManager);

    }

    public final KeyboardShortcutsHandler getKeyboardShortcutsHandler() {
        return keyboardShortcutsHandler;
    }

    public final EnteredExitedHandler getEnteredExitedHandler() {
        return enteredExitedHandler;
    }

    public final EventHandlerManager getEventHandlerManager() {
        return eventHandlerManager;
    }

    @Override
    public BasicEventDispatcher getFirstDispatcher() {
        return keyboardShortcutsHandler;
    }

    @Override
    public BasicEventDispatcher getLastDispatcher() {
        return eventHandlerManager;
    }
}
