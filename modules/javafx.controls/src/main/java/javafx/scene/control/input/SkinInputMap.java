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
package javafx.scene.control.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Skinnable;
import javafx.scene.input.KeyCode;
import com.sun.javafx.scene.control.input.EventHandlerPriority;
import com.sun.javafx.scene.control.input.KeyEventMapper;
import com.sun.javafx.scene.control.input.PHList;

/**
 * The Input Map for use by the Skin.
 *
 * @param <C> the control type
 * @since 999 TODO
 */
public class SkinInputMap<C extends Skinnable> {
    // KeyBinding -> FunctionTag
    // FunctionTag -> FunctionHandler
    // ON_KEY_ENTER/ON_KEY_EXIT -> Runnable
    // EventType -> PHList
    final HashMap<Object,Object> map = new HashMap<>();
    final KeyEventMapper kmapper = new KeyEventMapper();

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
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    public <T extends Event> void addHandler(EventType<T> type, boolean consume, EventHandler<T> handler) {
        addHandler(type, consume, EventHandlerPriority.SKIN_HIGH, handler);
    }

    /**
     * Adds an event handler for the specified event type, in the context of this skin.
     * This event handler will get invoked after all handlers added via map() methods.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    public <T extends Event> void addHandlerLast(EventType<T> type, boolean consume, EventHandler<T> handler) {
        addHandler(type, consume, EventHandlerPriority.SKIN_LOW, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this skin.
     * This is a more specific version of {@link #addHandler(EventType,boolean,EventHandler)} method.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param consume determines whether the matching event is consumed or not
     * @param handler the event handler
     */
    public <T extends Event> void addHandler(EventCriteria<T> criteria, boolean consume, EventHandler<T> handler) {
        addHandler(criteria, consume, EventHandlerPriority.SKIN_HIGH, handler);
    }

    /**
     * Adds an event handler for the specific event criteria, in the context of this skin.
     * This event handler will get invoked after all handlers added via map() methods.
     *
     * @param <T> the actual event type
     * @param criteria the matching criteria
     * @param consume determines whether the matching event is consumed or not
     * @param h the event handler
     */
    public <T extends Event> void addHandlerLast(EventCriteria<T> criteria, boolean consume, EventHandler<T> h) {
        addHandler(criteria, consume, EventHandlerPriority.SKIN_LOW, h);
    }

    private <T extends Event> void addHandler(
        EventType<T> type,
        boolean consume,
        EventHandlerPriority pri,
        EventHandler<T> handler)
    {
        if (consume) {
            putHandler(type, pri, new EventHandler<T>() {
                @Override
                public void handle(T ev) {
                    handler.handle(ev);
                    ev.consume();
                }
            });
        } else {
            putHandler(type, pri, handler);
        }
    }

    private <T extends Event> void addHandler(
        EventCriteria<T> criteria,
        boolean consume,
        EventHandlerPriority pri,
        EventHandler<T> handler
    ) {
        EventType<T> type = criteria.getEventType();
        putHandler(type, pri, new EventHandler<T>() {
            @Override
            public void handle(T ev) {
                if (criteria.isEventAcceptable(ev)) {
                    handler.handle(ev);
                    if (consume) {
                        ev.consume();
                    }
                }
            }
        });
    }

    // adds the specified handler to input map with the given priority
    // and event type.
    private <T extends Event> void putHandler(EventType<T> type, EventHandlerPriority pri, EventHandler<T> handler)
    {
        Object x = map.get(type);
        PHList hs;
        if(x instanceof PHList h) {
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
    public void registerKey(KeyBinding k, FunctionTag tag) {
        map.put(k, tag);
        kmapper.addType(k);
    }
    
    /**
     * Maps a key binding to the specified function tag.
     *
     * @param code the key code to construct a {@link KeyBinding}
     * @param tag the function tag
     */
    public void registerKey(KeyCode code, FunctionTag tag) {
        registerKey(KeyBinding.of(code), tag);
    }

    /**
     * Maps a function to the specified function tag.
     *
     * @param tag the function tag
     * @param function the function
     */
    public final void registerFunction(FunctionTag tag, FunctionHandler<C> function) {
        map.put(tag, function);
    }

    /**
     * Maps a function to the specified function tag.
     * This method allows for controlling whether the matching event will be consumed or not.
     *
     * @param tag the function tag
     * @param function the function
     */
    public final void registerFunction(FunctionTag tag, FunctionHandlerConditional<C> function) {
        map.put(tag, InputMap.toFunctionHandler(function));
    }
    
    /**
     * This convenience method maps the function tag to the specified function, and at the same time
     * maps the specified key binding to that function tag.
     * @param tag the function tag
     * @param k the key binding
     * @param func the function
     */
    public void register(FunctionTag tag, KeyBinding k, FunctionHandler<C> func) {
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
    public void register(FunctionTag tag, KeyCode code, FunctionHandler<C> func) {
        registerFunction(tag, func);
        registerKey(KeyBinding.of(code), tag);
    }
    
    Object resolve(KeyBinding k) {
        return map.get(k);
    }

    /**
     * Collects the key bindings mapped by the skin.
     *
     * @return a Set of key bindings
     */
    public Set<KeyBinding> getKeyBindings() {
        return collectKeyBindings(null, null);
    }

    /**
     * Returns the set of key bindings mapped to the specified function tag.
     * @param tag the function tag
     * @return the set of KeyBindings
     */
    public Set<KeyBinding> getKeyBindingsFor(FunctionTag tag) {
        return collectKeyBindings(null, tag);
    }

    Set<KeyBinding> collectKeyBindings(Set<KeyBinding> bindings, FunctionTag tag) {
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
    public void duplicateMapping(KeyBinding existing, KeyBinding newk) {
        Object x = map.get(existing);
        if (x != null) {
            map.put(newk, x);
        }
    }

    final FunctionHandler<C> getFunction(FunctionTag tag) {
        Object x = map.get(tag);
        if (x instanceof FunctionHandler f) {
            return f;
        }
        return null;
    }

    void unbind(FunctionTag tag) {
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> en = it.next();
            if (tag == en.getValue()) {
                // the entry must be KeyBinding -> FunctionTag
                it.remove();
            }
        }
    }

    void forEach(TriConsumer client) {
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

    @FunctionalInterface
    static interface TriConsumer<T extends Event> {
        public void accept(EventType<T> type, EventHandlerPriority pri, EventHandler<T> h);
    }
}
