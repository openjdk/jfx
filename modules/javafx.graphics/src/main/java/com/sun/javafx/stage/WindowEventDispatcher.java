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

package com.sun.javafx.stage;

import com.sun.javafx.event.BasicEventDispatcher;
import com.sun.javafx.event.CompositeEventDispatcher;
import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.event.EventRedirector;

import javafx.stage.Window;
import javafx.stage.PopupWindow;

/**
 * An {@code EventDispatcher} for {@code Window}. It is formed by a chain
 * in which a received event is first passed through {@code EventRedirector}
 * and then through {@code EventHandlerManager}.
 */
public class WindowEventDispatcher extends CompositeEventDispatcher {
    private final EventRedirector eventRedirector;

    private final WindowCloseRequestHandler windowCloseRequestHandler;

    private final EventHandlerManager eventHandlerManager;

    private final WindowSystemMenuHandler systemMenuHandler;

    public WindowEventDispatcher(final Window window) {
        this(new EventRedirector(window),
             new WindowCloseRequestHandler(window),
             new EventHandlerManager(window),
             new WindowSystemMenuHandler(window));
    }

    public WindowEventDispatcher(final PopupWindow popupWindow) {
        this(new EventRedirector(popupWindow),
             new WindowCloseRequestHandler(popupWindow),
             new EventHandlerManager(popupWindow),
             null);
    }

    private WindowEventDispatcher(
            final EventRedirector eventRedirector,
            final WindowCloseRequestHandler windowCloseRequestHandler,
            final EventHandlerManager eventHandlerManager,
            final WindowSystemMenuHandler systemMenuHandler) {
        this.eventRedirector = eventRedirector;
        this.windowCloseRequestHandler = windowCloseRequestHandler;
        this.eventHandlerManager = eventHandlerManager;
        this.systemMenuHandler = systemMenuHandler;

        eventRedirector.insertNextDispatcher(windowCloseRequestHandler);
        windowCloseRequestHandler.insertNextDispatcher(eventHandlerManager);
        if (systemMenuHandler != null) {
            eventHandlerManager.insertNextDispatcher(systemMenuHandler);
        }
    }

    public final EventRedirector getEventRedirector() {
        return eventRedirector;
    }

    public final WindowCloseRequestHandler getWindowCloseRequestHandler() {
        return windowCloseRequestHandler;
    }

    public final EventHandlerManager getEventHandlerManager() {
        return eventHandlerManager;
    }

    @Override
    public BasicEventDispatcher getFirstDispatcher() {
        return eventRedirector;
    }

    @Override
    public BasicEventDispatcher getLastDispatcher() {
        if (systemMenuHandler != null) {
            return systemMenuHandler;
        }
        return eventHandlerManager;
    }
}
