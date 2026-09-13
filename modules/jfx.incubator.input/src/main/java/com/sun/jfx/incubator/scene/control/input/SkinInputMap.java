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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.KeyBinding;

/**
 * The Input Map for use by the Skin.
 * <p>
 * Skins whose behavior encapsulates state information must use a Stateful variant obtained with
 * the {@link #create()} factory method.
 * <p>
 * Skins whose behavior requires no state, or when state is fully encapsulated by the Control itself,
 * could use a Stateless variant obtained with the {@link #createStateless()} method.
 */
public abstract sealed class SkinInputMap permits SkinInputMap.Stateful, SkinInputMap.Stateless {
    /**
     * <pre> KeyBinding -> FunctionTag
     * FunctionTag -> Runnable or FunctionHandler
     * EventType -> PHList</pre>
     */
    final HashMap<Object, Object> map = new HashMap<>();
    // TODO change to package protected once SkinInputMap is public
    public final KeyEventMapper kmapper = new KeyEventMapper();

    /**
     * Creates a skin input map.
     */
    public SkinInputMap() {
    }

    /**
     * Adds an event handler for the specified event type, in the context of this skin.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    public final <T extends Event> void addHandler(EventType<T> type, EventHandler<T> handler) {
        putHandler(type, EventHandlerPriority.SKIN_HIGH, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this skin.
     * This is a more specific version of {@link #addHandler(EventType,EventHandler)} method.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param handler the event handler
     */
    public final <T extends Event> void addHandler(EventCriteria<T> criteria, EventHandler<T> handler) {
        EventType<T> type = criteria.getEventType();
        putHandler(type, EventHandlerPriority.SKIN_HIGH, new EventHandler<T>() {
            @Override
            public void handle(T ev) {
                if (criteria.isEventAcceptable(ev)) {
                    handler.handle(ev);
                }
            }
        });
    }

    // adds the specified handler to input map with the given priority
    // and event type.
    private <T extends Event> void putHandler(EventType<T> type, EventHandlerPriority pri, EventHandler<T> handler) {
        Object x = map.get(type);
        PHList hs;
        if (x instanceof PHList h) {
            hs = h;
        } else {
            hs = new PHList();
            map.put(type, hs);
        }
        hs.add(pri, handler);
    }

    /**
     * Maps a key binding to the specified function tag.
     *
     * @param k the key binding
     * @param tag the function tag
     */
    public final void registerKey(KeyBinding k, FunctionTag tag) {
        map.put(k, tag);
        kmapper.addType(k);
    }

    /**
     * Maps a key binding to the specified function tag.
     *
     * @param code the key code to construct a {@link KeyBinding}
     * @param tag the function tag
     */
    public final void registerKey(KeyCode code, FunctionTag tag) {
        registerKey(KeyBinding.of(code), tag);
    }

    // TODO change to package protected once SkinInputMap is public
    public Object resolve(KeyBinding k) {
        return map.get(k);
    }

    /**
     * Collects the key bindings mapped by the skin.
     *
     * @return a Set of key bindings
     */
    public final Set<KeyBinding> getKeyBindings() {
        return collectKeyBindings(null, null);
    }

    /**
     * Returns the set of key bindings mapped to the specified function tag.
     * @param tag the function tag
     * @return the set of KeyBindings
     */
    public final Set<KeyBinding> getKeyBindingsFor(FunctionTag tag) {
        return collectKeyBindings(null, tag);
    }

    // TODO change to package protected once SkinInputMap is public
    public Set<KeyBinding> collectKeyBindings(Set<KeyBinding> bindings, FunctionTag tag) {
        if (bindings == null) {
            bindings = new HashSet<>();
        }
        for (Map.Entry<Object, Object> en : map.entrySet()) {
            if (en.getKey() instanceof KeyBinding k) {
                if ((tag == null) || (tag == en.getValue())) {
                    bindings.add(k);
                }
            }
        }
        return bindings;
    }

    /**
     * This convenience method registers a copy of the behavior-specific mappings from one key binding to another.
     * The method does nothing if no behavior specific mapping can be found.
     * @param existing the existing key binding
     * @param newk the new key binding
     */
    public final void duplicateMapping(KeyBinding existing, KeyBinding newk) {
        Object x = map.get(existing);
        if (x != null) {
            map.put(newk, x);
        }
    }

    // TODO change to package protected once SkinInputMap is public
    public final boolean execute(Object source, FunctionTag tag) {
        Object x = map.get(tag);
        if (x instanceof Runnable r) {
            r.run();
            return true;
        } else if (x instanceof BooleanSupplier f) {
            return f.getAsBoolean();
        } else if (x instanceof Stateless.FHandler h) {
            h.handleFunction(source);
            return true;
        } else if (x instanceof Stateless.FHandlerConditional h) {
            return h.handleFunction(source);
        }
        return false;
    }

    // TODO change to package protected once SkinInputMap is public
    public void unbind(FunctionTag tag) {
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> en = it.next();
            if (tag == en.getValue()) {
                // the entry must be KeyBinding -> FunctionTag
                it.remove();
            }
        }
    }

    // TODO change to package protected once SkinInputMap is public
    public void forEach(TriConsumer client) {
        for (Map.Entry<Object, Object> en : map.entrySet()) {
            if (en.getKey() instanceof EventType type) {
                PHList hs = (PHList)en.getValue();
                hs.forEach((pri, h) -> {
                    client.accept(type, pri, h);
                    return true;
                });
            }
        }
    }

    // TODO change to package protected once SkinInputMap is public
    @FunctionalInterface
    public static interface TriConsumer<T extends Event> {
        public void accept(EventType<T> type, EventHandlerPriority pri, EventHandler<T> h);
    }

    /**
     * Creates the stateful SkinInputMap.
     * @return the stateful SkinInputMap
     */
    public static SkinInputMap.Stateful create() {
        return new Stateful();
    }

    /**
     * Creates the stateless SkinInputMap.
     * @param <C> the type of Control
     * @return the stateless SkinInputMap
     */
    public static <C extends Control> SkinInputMap.Stateless<C> createStateless() {
        return new Stateless<C>();
    }

    /** SkinInputMap for skins that maintain stateful behaviors */
    public static final class Stateful extends SkinInputMap {
        Stateful() {
        }

        /**
         * Maps a function to the specified function tag.
         *
         * @param tag the function tag
         * @param function the function
         */
        public final void registerFunction(FunctionTag tag, Runnable function) {
            map.put(tag, function);
        }

        /**
         * Maps a function to the specified function tag.
         * <p>
         * The event which triggered execution of the function will be consumed if the function returns {@code true}.
         *
         * @param tag the function tag
         * @param function the function
         */
        public final void registerFunction(FunctionTag tag, BooleanSupplier function) {
            map.put(tag, function);
        }

        /**
         * This convenience method maps the function tag to the specified function, and at the same time
         * maps the specified key binding to that function tag.
         * @param tag the function tag
         * @param k the key binding
         * @param func the function
         */
        public final void register(FunctionTag tag, KeyBinding k, Runnable func) {
            registerFunction(tag, func);
            registerKey(k, tag);
        }

        /**
         * This convenience method maps the function tag to the specified function, and at the same time
         * maps the specified key binding to that function tag.
         * @param tag the function tag
         * @param code the key code
         * @param func the function
         */
        public final void register(FunctionTag tag, KeyCode code, Runnable func) {
            registerFunction(tag, func);
            registerKey(KeyBinding.of(code), tag);
        }
    }

    /**
     * SkinInputMap for skins that either encapsulate the state fully in their Controls,
     * or don't require a state at all.
     *
     * @param <C> the type of Control
     */
    // NOTE: The stateless skin input map adds significant complexity to the API surface while providing
    // limited (some say non-existent) savings in terms of memory.  There aren't many Controls that
    // have a stateless behavior, which further reduces the usefulness of this class.
    // I'd rather remove this feature altogether.
    public static final class Stateless<C extends Control> extends SkinInputMap {
        /**
         * The function handler that always consumes the corresponding event.
         * @param <C> the type of Control
         */
        public interface FHandler<C> {
            /**
             * The function mapped to a key binding.
             * @param control the instance of Control
             */
            public void handleFunction(C control);
        }

        /**
         * The function handler that allows to control whether the corresponding event will get consumed.
         * @param <C> the type of Control
         */
        public interface FHandlerConditional<C> {
            /**
             * The function mapped to a key binding.  The return value instructs the owning InputMap
             * to consume the triggering event or not.
             * @param control the instance of Control
             * @return true to consume the event, false otherwise
             */
            public boolean handleFunction(C control);
        }

        Stateless() {
        }

        /**
         * Maps a function to the specified function tag.
         *
         * @param tag the function tag
         * @param function the function
         */
        public final void registerFunction(FunctionTag tag, FHandler<C> function) {
            map.put(tag, function);
        }

        /**
         * Maps a function to the specified function tag.
         * This method allows for controlling whether the matching event will be consumed or not.
         *
         * @param tag the function tag
         * @param function the function
         */
        public final void registerFunction(FunctionTag tag, FHandlerConditional<C> function) {
            map.put(tag, function);
        }

        /**
         * This convenience method maps the function tag to the specified function, and at the same time
         * maps the specified key binding to that function tag.
         * @param tag the function tag
         * @param k the key binding
         * @param func the function
         */
        public final void register(FunctionTag tag, KeyBinding k, FHandler<C> func) {
            registerFunction(tag, func);
            registerKey(k, tag);
        }

        /**
         * This convenience method maps the function tag to the specified function, and at the same time
         * maps the specified key binding to that function tag.
         * @param tag the function tag
         * @param code the key code
         * @param func the function
         */
        public final void register(FunctionTag tag, KeyCode code, FHandler<C> func) {
            registerFunction(tag, func);
            registerKey(KeyBinding.of(code), tag);
        }
    }
}
