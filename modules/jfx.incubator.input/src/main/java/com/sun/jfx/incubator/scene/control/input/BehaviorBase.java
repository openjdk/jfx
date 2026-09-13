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
package com.sun.jfx.incubator.scene.control.input;

import java.util.function.BooleanSupplier;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.TraversalDirection;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import com.sun.javafx.PlatformUtil;
import com.sun.jfx.incubator.scene.control.input.SkinInputMap.Stateful;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.KeyBinding;

/**
 * This class provides convenient foundation for custom Control developers intended to simplify writing
 * stateful behaviors.
 * <p>
 * A concrete behavior implementation should do the following:
 * <ol>
 * <li> provide default behavior methods (one for each function tag)
 * <li> implement {@link #populateSkinInputMap()} method, in which map control's function tags to
 *      the behavior methods, map key bindings to the function tags, add additional event handlers, using
 *      {@link #registerFunction(FunctionTag, Runnable)},
 *      {@link #registerKey(KeyBinding, FunctionTag)},
 *      {@link #registerKey(KeyCode, FunctionTag)},
 *      and
 *      {@code addHandler()} methods correspondingly.
 * <li> in the corresponding skin's {code Skin.install()}, set the skin input map to the control's input map.
 * </ol>
 * Example (in the actual skin class):
 * <pre>{@code
 *     @Override
 *     public void install() {
 *         super.install();
 *         setSkinInputMap(behavior.getSkinInputMap());
 *   }
 * }</pre>
 *
 * @param <C> the type of the control
 */
public abstract class BehaviorBase<C extends Control> {
    private final C control;
    private SkinInputMap.Stateful skinInputMap;

    /**
     * The constructor.
     * @param c the owner Control instance
     */
    public BehaviorBase(C c) {
        this.control = c;
    }

    /**
     * In this method, which is called by {@link javafx.scene.control.Skin#install()},
     * the child class populates the {@code SkinInputMap}
     * by registering key mappings and event handlers.
     * <p>
     * If a subclass overrides this method, it is important to call the superclass implementation.
     */
    protected abstract void populateSkinInputMap();

    /**
     * Returns the associated Control instance.
     * @return the owner
     */
    protected final C getControl() {
        return control;
    }

    /**
     * Returns the skin input map associated with this behavior.
     * @return the input map
     */
    public final SkinInputMap.Stateful getSkinInputMap() {
        if (skinInputMap == null) {
            this.skinInputMap = SkinInputMap.create();
            populateSkinInputMap();
        }
        return skinInputMap;
    }

    /**
     * Maps a function to the specified function tag.
     *
     * @param tag the function tag
     * @param function the function
     */
    protected final void registerFunction(FunctionTag tag, Runnable function) {
        getSkinInputMap().registerFunction(tag, function);
    }

    /**
     * Maps a function to the specified function tag.
     * <p>
     * The event which triggered execution of the function will be consumed if the function returns {@code true}.
     *
     * @param tag the function tag
     * @param function the function
     */
    protected final void registerFunction(FunctionTag tag, BooleanSupplier function) {
        getSkinInputMap().registerFunction(tag, function);
    }

    /**
     * Maps a key binding to the specified function tag.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping.
     *
     * @param k the key binding
     * @param tag the function tag
     */
    protected final void registerKey(KeyBinding k, FunctionTag tag) {
        getSkinInputMap().registerKey(k, tag);
    }

    /**
     * Maps a key binding to the specified function tag.
     * This method will not override a user mapping added by {@link #registerKey(KeyBinding,FunctionTag)}.
     *
     * @param code the key code to construct a {@link KeyBinding}
     * @param tag the function tag
     */
    protected final void registerKey(KeyCode code, FunctionTag tag) {
        getSkinInputMap().registerKey(code, tag);
    }

    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * @param tag the function tag
     * @param k the key binding
     * @param func the function
     */
    protected final void register(FunctionTag tag, KeyBinding k, Runnable func) {
        getSkinInputMap().registerFunction(tag, func);
        getSkinInputMap().registerKey(k, tag);
    }

    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * <p>
     * The event which triggered execution of the function will be consumed if the function returns {@code true}.
     *
     * @param tag the function tag
     * @param k the key binding
     * @param func the function
     */
    protected final void register(FunctionTag tag, KeyBinding k, BooleanSupplier func) {
        getSkinInputMap().registerFunction(tag, func);
        getSkinInputMap().registerKey(k, tag);
    }

    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * @param tag the function tag
     * @param code the key code
     * @param func the function
     */
    protected final void register(FunctionTag tag, KeyCode code, Runnable func) {
        getSkinInputMap().registerFunction(tag, func);
        getSkinInputMap().registerKey(KeyBinding.of(code), tag);
    }

    /**
     * This convenience method registers a copy of the behavior-specific mappings from one key binding to another.
     * The method does nothing if no behavior specific mapping can be found.
     * @param existing the existing key binding
     * @param newk the new key binding
     */
    protected final void duplicateMapping(KeyBinding existing, KeyBinding newk) {
        getSkinInputMap().duplicateMapping(existing, newk);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this Behavior.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    protected final <T extends Event> void addHandler(EventType<T> type, EventHandler<T> handler) {
        getSkinInputMap().addHandler(type, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this Behavior.
     * This is a more specific version of {@link #addHandler(EventType,EventHandler)} method.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param handler the event handler
     */
    protected final <T extends Event> void addHandler(EventCriteria<T> criteria, EventHandler<T> handler) {
        getSkinInputMap().addHandler(criteria, handler);
    }

    /**
     * Returns true if this method is invoked on a Linux platform.
     * @return true on a Linux platform
     */
    protected final boolean isLinux() {
        return PlatformUtil.isLinux();
    }

    /**
     * Returns true if this method is invoked on a Mac OS platform.
     * @return true on a Mac OS platform
     */
    protected final boolean isMac() {
        return PlatformUtil.isMac();
    }

    /**
     * Returns true if this method is invoked on a Windows platform.
     * @return true on a Windows platform
     */
    protected final boolean isWindows() {
        return PlatformUtil.isWindows();
    }

    /**
     * Called by any of the BehaviorBase traverse methods to actually effect a
     * traversal of the focus. The default behavior of this method is to simply
     * traverse on the given node, passing the given direction. A
     * subclass may override this method.
     *
     * @param dir The direction to traverse
     */
    private void traverse(TraversalDirection dir) {
        control.requestFocusTraversal(dir);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node above the current one.
     */
    public final void traverseUp() {
        traverse(TraversalDirection.UP);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node below the current one.
     */
    public final void traverseDown() {
        traverse(TraversalDirection.DOWN);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node left of the current one.
     */
    public final void traverseLeft() {
        traverse(TraversalDirection.LEFT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node right of the current one.
     */
    public final void traverseRight() {
        traverse(TraversalDirection.RIGHT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node in the focus traversal cycle.
     */
    public final void traverseNext() {
        traverse(TraversalDirection.NEXT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the previous focusTraversable Node in the focus traversal cycle.
     */
    public final void traversePrevious() {
        traverse(TraversalDirection.PREVIOUS);
    }
}
