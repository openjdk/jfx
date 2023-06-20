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
package javafx.scene.control.input;

import java.util.Objects;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;

/**
 * Class provides a foundation for behaviors.
 * <p>
 * A concrete behavior implementation should do three things:
 * 1. provide default behavior methods (a.k.a. functions)
 * 2. in install() method, called from Skin.install(), map control's function tags to
 *    behavior methods, map key bindings to function tags, and add additional event handlers,
 *    using func(), key(), and hand() methods correspondingly.
 *    Important: no mapping should be made in the behavior constructor, only in install().
 * <p>
 * The base class adds a dispose() method (called from Skin.dispose()),
 * which undoes the mapping done in install().
 */
public abstract class BehaviorBase2<C extends Control> implements IBehavior {
    private C control;
    
    public BehaviorBase2() {
    }
    
    // TODO rename getControl()
    protected final C getNode() {
        return control;
    }
    
    protected final InputMap2 getInputMap2() {
        return control.getInputMap2();
    }

    /** this method must be called in Skin.install() to actually install all the default mappings */
    public void install(Skin<C> skin) {
        Objects.nonNull(skin);
        this.control = skin.getSkinnable();
    }
    
    public void dispose() {
        control.getInputMap2().unregister(this);
    }
    
    /**
     * Maps a function to the function tag.
     * This method will not override any previous mapping added by {@link #func(FunctionTag,Runnable)}.
     *
     * @param behavior
     * @param tag
     * @param function
     */
    protected void func(FunctionTag tag, Runnable function) {
        getInputMap2().func(this, tag, function);
    }

    /**
     * Maps a key binding to the specified function tag.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping.
     *
     * @param behavior
     * @param k key binding, can be null
     * @param tag function tag
     */
    protected void key(KeyBinding2 k, FunctionTag tag) {
        getInputMap2().key(this, k, tag);
    }

    /**
     * Maps a key binding to the specified function tag.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param code key code to construct a {@link KeyBinding2}
     * @param tag function tag
     */
    protected void key(KeyCode code, FunctionTag tag) {
        getInputMap2().key(this, code, tag);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     * The handler will get removed in {@link#dispose()} method.
     *
     * @param type
     * @param handler
     */
    protected <T extends Event> void map(EventType<T> type, EventHandler<T> handler) {
        getInputMap2().map(this, type, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this Behavior.
     * This is a more specific version of {@link #map(EventType,EventHandler)} method.
     * The handler will get removed in {@link#dispose()} method.
     *
     * @param type
     * @param handler
     */
    protected <T extends Event> void map(EventCriteria<T> criteria, boolean consume, EventHandler<T> handler) {
        getInputMap2().map(this, criteria, consume, handler);
    }

    protected void setOnKeyEventEnter(Runnable action) {
        getInputMap2().setOnKeyEventEnter(this, action);
    }

    protected void setOnKeyEventExit(Runnable action) {
        getInputMap2().setOnKeyEventExit(this, action);
    }
}
