/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * A Weak reference EventHandler wrapper in the same way as javafx.beans.value.WeakChangeListener works.
 *
 * This assumes the EventTarget has a standard "removeEventHandler" method.
 */
public class WeakEventHandler<T extends Event> implements EventHandler<T> {

    private final WeakReference<EventHandler<T>> ref;
    private final EventTarget target;
    private final EventType type;

    /**
     * The constructor of {@code WeakEventHandler}.
     *
     * @param listener The original listener that should be notified
     */
    public WeakEventHandler(EventTarget target, EventType type, EventHandler<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }
        this.target = target;
        this.type = type;
        this.ref = new WeakReference<EventHandler<T>>(listener);
    }

    @Override public void handle(T t) {
        EventHandler<T> eventHandler = ref.get();
        if (eventHandler != null) {
            eventHandler.handle(t);
        } else {
            // The weakly reference listener has been garbage collected,
            // so this WeakListener will now unhook itself from the
            // source bean
            try {
                Method removeEventHandler = target.getClass().getMethod("removeEventHandler", EventType.class, EventHandler.class);
                removeEventHandler.invoke(target,type, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
