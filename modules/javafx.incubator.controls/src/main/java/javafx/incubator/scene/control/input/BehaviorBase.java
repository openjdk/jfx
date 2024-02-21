/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package javafx.incubator.scene.control.input;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import com.sun.javafx.PlatformUtil;

/**
 * Class provides a foundation for behaviors.
 * <p>
 * A concrete behavior implementation should do the following:
 * 1. provide default behavior methods (a.k.a. functions)
 * 2. in install() method, called from Skin.install(), map control's function tags to
 *    behavior methods, map key bindings to function tags, and add additional event handlers,
 *    using regFunc(), regKey(), and addHandler() methods correspondingly.
 *    Important: no mapping should be made in the behavior constructor, only in install().
 * <p>
 * The base class adds a dispose() method (called from Skin.dispose()),
 * which undoes the mappings done in install().
 *
 * @param <C> the actual control type
 * @since 999 TODO
 */
public abstract class BehaviorBase<C extends Control> {
    /** the instance of Control associated with this behavior */
    protected final C control;

    /**
     * The constructor.
     * @param control the control
     */
    public BehaviorBase(C control) {
        this.control = control;
    }

    /**
     * Returns the associated Control instance.
     * @return the owner
     */
    protected final C getControl() {
        return control;
    }

    /**
     * Returns the input map of the associated Control.
     * TODO rename getInputMap()
     * @return the input map
     */
    protected abstract InputMap getInputMap();
//    protected final InputMap getInputMap() {
//        return control.getInputMap();
//    }

    /**
     * Installs this behavior by registering default key mappings and event handlers.
     */
    public abstract void install();

    /**
     * Disposes of this behavior by removing all key mappings and event handlers registered by
     * this behavior.  This method gets invoked by {@code Skin.dispose()}, the application should not need
     * to call it directly.
     * <p>
     * If a subclass overrides this method, it is important to call the superclass
     * implementation.
     */
    public void dispose() {
        getInputMap().removeAllMappings(this);
    }

    /**
     * Maps a function to the specified function tag.
     * This method will not override any previous mapping added by {@link #registerFunction(FunctionTag,Runnable)}.
     *
     * @param tag the function tag
     * @param function the function
     */
    protected void registerFunction(FunctionTag tag, Runnable function) {
        getInputMap().registerFunction(this, tag, function);
    }

    /**
     * Maps a key binding to the specified function tag.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping.
     *
     * @param k the key binding, can be null (TODO or KB.NA)
     * @param tag the function tag
     */
    protected void registerKey(KeyBinding k, FunctionTag tag) {
        getInputMap().registerKey(this, k, tag);
    }

    /**
     * Maps a key binding to the specified function tag.
     * This method will not override a user mapping added by {@link #registerKey(KeyBinding,FunctionTag)}.
     *
     * @param code the key code to construct a {@link KeyBinding}
     * @param tag the function tag
     */
    protected void registerKey(KeyCode code, FunctionTag tag) {
        getInputMap().registerKey(this, code, tag);
    }

    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * @param tag the function tag
     * @param k the key binding
     * @param func the function
     */
    protected void register(FunctionTag tag, KeyBinding k, Runnable func) {
        getInputMap().registerFunction(this, tag, func);
        getInputMap().registerKey(this, k, tag);
    }

    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * @param tag the function tag
     * @param code the key code
     * @param func the function
     */
    protected void register(FunctionTag tag, KeyCode code, Runnable func) {
        getInputMap().registerFunction(tag, func);
        getInputMap().registerKey(KeyBinding.of(code), tag);
    }

    /**
     * This convenience method registers a copy of the behavior-specific mappings from one key binding to another.
     * The method does nothing if no behavior specific mapping can be found.
     * @param existing the existing key binding
     * @param newk the new key binding
     */
    protected void duplicateMapping(KeyBinding existing, KeyBinding newk) {
        getInputMap().duplicateMapping(existing, newk);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     * The handler will get removed in {@link #dispose()} method.
     * This mapping always consumes the matching event.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    protected <T extends Event> void addHandler(EventType<T> type, EventHandler<T> handler) {
        getInputMap().addHandler(this, type, true, false, handler);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     * The handler will get removed in {@link #dispose()} method.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    protected <T extends Event> void addHandler(EventType<T> type, boolean consume, EventHandler<T> handler) {
        getInputMap().addHandler(this, type, consume, false, handler);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     * This event handler will get invoked after all handlers added via map() methods.
     * The handler will get removed in {@link #dispose()} method.
     * This mapping always consumes the matching event.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    protected <T extends Event> void addHandlerLast(EventType<T> type, EventHandler<T> handler) {
        getInputMap().addHandler(this, type, true, true, handler);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     * This event handler will get invoked after all handlers added via map() methods.
     * The handler will get removed in {@link #dispose()} method.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    protected <T extends Event> void addHandlerLast(EventType<T> type, boolean consume, EventHandler<T> handler) {
        getInputMap().addHandler(this, type, consume, true, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this Behavior.
     * This is a more specific version of {@link #addHandler(EventType,EventHandler)} method.
     * The handler will get removed in {@link #dispose()} method.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    protected <T extends Event> void addHandler(EventCriteria<T> criteria, boolean consume, EventHandler<T> handler) {
        getInputMap().addHandler(this, criteria, consume, false, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this Behavior.
     * This event handler will get invoked after all handlers added via map() methods.
     * The handler will get removed in {@link #dispose()} method.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    protected <T extends Event> void addHandlerLast(
        EventCriteria<T> criteria,
        boolean consume,
        EventHandler<T> handler
    ) {
        getInputMap().addHandler(this, criteria, consume, true, handler);
    }

    /**
     * Returns true if this method is invoked on a Linux platform.
     * @return true on a Linux platform
     */
    protected boolean isLinux() {
        return PlatformUtil.isLinux();
    }

    /**
     * Returns true if this method is invoked on a Mac OS platform.
     * @return true on a Mac OS platform
     */
    protected boolean isMac() {
        return PlatformUtil.isMac();
    }

    /**
     * Returns true if this method is invoked on a Windows platform.
     * @return true on a Windows platform
     */
    protected boolean isWindows() {
        return PlatformUtil.isWindows();
    }
}
