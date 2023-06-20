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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * InputMap is a class that is set on a given {@link Control}. When the Node receives
 * an input event from the system, it passes this event in to the InputMap where
 * the InputMap can check all installed mappings to see if there is any
 * suitable mapping, and if so, fire the provided {@link EventHandler}.
 *
 * @param <C> The type of the Control that the InputMap is installed in.
 */
public class InputMap2<C extends Control> {
    /** contains user- and behavior-set key binding or function mappings */
    private static class Entry { // TODO possible rename Mapping
        Object value;
        IBehavior behavior;
        Object behaviorValue;

        public Object getValue() {
            if (value == NULL) {
                return null;
            } else if (value == null) {
                return behaviorValue;
            }
            return value;
        }
    }

    private static final Object NULL = new Object();
    private static final Object ON_KEY_ENTER = new Object();
    private static final Object ON_KEY_EXIT = new Object();
    private final C control;
    // EventType<?> -> Entry with value=List<EventHandler> (behavior only)
    // KeyBinding -> Entry with value=FunctionTag
    // FunctionTag -> Entry with value=Runnable
    private final HashMap<Object,Entry> map = new HashMap<>();

    public InputMap2(C control) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }
        this.control = control;
    }

    /**
     * The Control to which this InputMap is attached.
     */
    public final C getControl() {
        return control;
    }
    
    public final InputMap2 getInputMap2() {
        return control.getInputMap2();
    }

    private void handleEvent(Event ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        EventType<?> t = ev.getEventType();
        List<EventHandler> handlers = getHandlers(t);
        if (handlers != null) {
            for (EventHandler h: handlers) {
                h.handle(ev);
                if (ev.isConsumed()) {
                    break;
                }
            }
        }
    }

    public void handleKeyEvent(Event ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        KeyBinding2 k = KeyBinding2.from((KeyEvent)ev);
        KeyMap km = control.getKeyMap();
        Runnable f = km.getFunction(k);
        if (f != null) {
            // on key function enter
            Entry en = map.get(ON_KEY_ENTER);
            if (en != null) {
                Object x = en.getValue();
                if (x instanceof Runnable r) {
                    r.run();
                }
            }

            try {
                f.run();
                ev.consume();
            } finally {
                // on key function exit
                en = map.get(ON_KEY_EXIT);
                if (en != null) {
                    Object x = en.getValue();
                    if (x instanceof Runnable r) {
                        r.run();
                    }
                }
            }
        }

        if (ev.isConsumed()) {
            return;
        }

        EventType<?> t = ev.getEventType();
        List<EventHandler> handlers = getHandlers(t);
        if (handlers != null) {
            for (EventHandler h: handlers) {
                h.handle(ev);
                if (ev.isConsumed()) {
                    break;
                }
            }
        }
    }

    /**
     * Removes all the mappings set by the behavior.
     * Behavior developers do not need to call this method directly, as it is being called in BehaviorBase.dispose().
     *
     * @param behavior
     */
    void unregister(IBehavior behavior) {
        Objects.nonNull(behavior);

        for (var x: map.entrySet()) {
            Entry en = x.getValue();
            if (en.behavior == behavior) {
                if (x.getKey() instanceof EventType t) {
                    // remove event handler
                    if (en.behaviorValue instanceof List list) {
                        for (Object h: list) {
                            if (h instanceof EventHandler li) {
                                control.removeEventHandler(t, li);
                            }
                        }
                    }
                } else {
                    // remove behavior key mapping
                    en.behavior = null;
                    en.behaviorValue = null;
                }
            }
        }
    }
    
    <T extends Event> void map(IBehavior behavior, EventType<T> type, EventHandler<T> handler) {
        extendOrReplaceHandlers(behavior, type, handler);
    }
    
    <T extends Event> void map(IBehavior behavior, EventCriteria<T> criteria, EventHandler<T> handler) {
        EventType<T> type = criteria.getEventType();
        extendOrReplaceHandlers(behavior, type, new EventHandler<T>() {
            @Override
            public void handle(T ev) {
                if (criteria.isEventAcceptable(ev)) {
                    handler.handle(ev);
                    ev.consume();
                }
            }
        });
    }

    private <T extends Event> void extendOrReplaceHandlers(IBehavior behavior, EventType<T> type, EventHandler<T> handler) {
        Objects.nonNull(behavior);
        Entry en = addListenerIfNeeded(type);
        if ((en.behavior != null) && (en.behavior != behavior)) {
            // remove the old behavior mapping when replacing behavior
            // TODO problem!  a single map with criteria will wipe out valid old behavior mappings?
        }
        
        List<EventHandler<?>> handlers = (en.behaviorValue instanceof List li) ? li : new ArrayList<>(2);
        handlers.add(handler);
        en.behaviorValue = handlers;
    }
    
    /**
     * Adds a user-specified function under the given function tag.
     * This function will override any function set by the behavior.
     */
    public void func(FunctionTag tag, Runnable function) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(tag, function, null);
    }

    /**
     * Maps a function to the function tag, for use by the behavior.
     * This method will not override any previous mapping added by {@link #func(FunctionTag,Runnable)}.
     *
     * @param behavior
     * @param tag
     * @param function
     */
    void func(IBehavior behavior, FunctionTag tag, Runnable function) {
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(tag, function, behavior);
    }
    
    /**
     * Link a key binding to the specified function tag.
     * This method will override a mapping set by the behavior.
     *
     * @param k
     * @param tag
     */
    public void key(KeyBinding2 k, FunctionTag tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, null);
    }
    
    /**
     * Maps a key binding to the specified function tag, for use by the behavior.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param k key binding, can be null
     * @param tag function tag
     */
    void key(IBehavior behavior, KeyBinding2 k, FunctionTag tag) {
        if (k == null) {
            return;
        }
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(k, tag, behavior);
    }

    /**
     * Maps a key binding to the specified function tag, as a part of the behavior.
     * This method will not override a user mapping added by {@link #key(KeyBinding2,FunctionTag)}.
     *
     * @param behavior
     * @param code key code to construct a {@link KeyBinding2}
     * @param tag function tag
     */
    void key(IBehavior behavior, KeyCode code, FunctionTag tag) {
        key(behavior, KeyBinding2.of(code), tag);
    }

    private void addFunction(FunctionTag tag, Runnable function, IBehavior behavior) {
        Entry en = map.get(tag);
        if (en == null) {
            en = new Entry();
            map.put(tag, en);
        }

        if (behavior == null) {
            // user mapping
            en.value = function;
        } else {
            // behavior mapping
            en.behavior = behavior;
            en.behaviorValue = function;
        }
    }

    private void addBinding(KeyBinding2 k, FunctionTag tag, IBehavior behavior) {
        Entry en = map.get(k);
        if (en == null) {
            en = new Entry();
            map.put(k, en);
        }

        EventType<KeyEvent> type = k.getEventType();
        en = addListenerIfNeeded(type);

        if (behavior == null) {
            // user mapping
        } else {
            // behavior mapping
            en.behavior = behavior;
            en.behaviorValue = tag;
        }
    }

    /**
     * Returns a {@code Runnable} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @param tag
     */
    public Runnable getFunction(FunctionTag tag) {
        Entry en = map.get(tag);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof Runnable r) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns a {@code Runnable} mapped to the specified {@link KeyBinding2},
     * or null if no such mapping exists.
     *
     * @param k
     */
    public Runnable getFunction(KeyBinding2 k) {
        Entry en = map.get(k);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof FunctionTag tag) {
                return getFunction(tag);
            }
        }
        return null;
    }

    private List<EventHandler> getHandlers(EventType<?> t) {
        Entry en = map.get(t);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof List list) {
                return list;
            }
        }
        return null;
    }

    private Entry addListenerIfNeeded(EventType<?> t) {
        Entry en = map.get(t);
        if (en == null) {
            en = new Entry();
            map.put(t, en);

            if (t.getSuperType() == KeyEvent.ANY) {
                // key handler is special because of key bindings
                getControl().addEventHandler(t, this::handleKeyEvent);
            } else {
                getControl().addEventHandler(t, this::handleEvent);
            }
        }
        return en;
    }

    /**
     * Unbinds the specified key binding.
     *
     * @param k
     */
    public void unbind(KeyBinding2 k) {
        Entry en = map.get(k);
        if (en != null) {
            en.value = NULL;
        }
    }

    /**
     * Resets all key bindings set by user to the values set by the behavior, if any.
     */
    public void resetKeyBindings() {
        Iterator<Map.Entry<Object, Entry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Entry> me = it.next();
            if (me.getKey() instanceof KeyBinding2) {
                Entry en = me.getValue();
                en.value = null;
            }
        }
    }

    /**
     * Restores the specified key binding to the value set by the behavior, if any.
     *
     * @param k
     */
    public void restoreDefaultBinding(KeyBinding2 k) {
        Entry en = map.get(k);
        if (en != null) {
            en.value = null;
            if (en.behaviorValue == null) {
                map.remove(k);
            }
        }
    }

    /**
     * Restores the specified function tag to the value set by the behavior, if any.
     *
     * @param tag
     */
    public void restoreDefaultFunction(FunctionTag tag) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Entry en = map.get(tag);
        if (en != null) {
            en.value = null;
            if (en.behaviorValue == null) {
                map.remove(tag);
            }
        }
    }

    /**
     * Collects all mapped key bindings (set either by the user or the behavior).
     *
     * @return a Set of KeyBindings
     */
    public Set<KeyBinding2> getKeyBindings() {
        return map.keySet().stream().
            filter((k) -> (k instanceof KeyBinding2)).
            map((x) -> (KeyBinding2)x).
            collect(Collectors.toSet());
    }

    /**
     * Maps a new KeyBinding as an alias to the existing one with the same owner and function tag.
     * This method does nothing if there is no mapping for k1.
     *
     * @param k1 existing key binding
     * @param k2 new key binding
     */
    public void addAlias(KeyBinding2 k1, KeyBinding2 k2) {
        Entry en1 = map.get(k1);
        if (en1 != null) {
            Entry en2 = new Entry();
            en2.behavior = en1.behavior;
            en2.behaviorValue = en1.behaviorValue;
            en2.value = en1.value;
            map.put(k2, en2);
        }
    }

    void setOnKeyEventEnter(IBehavior behavior, Runnable action) {
        Objects.nonNull(behavior);
        Entry en = map.get(ON_KEY_ENTER);
        if (en == null) {
            en = new Entry();
            map.put(ON_KEY_ENTER, en);
        }

        if (behavior == null) {
            en.value = action;
        } else {
            en.behavior = behavior;
            en.behaviorValue = action;
        }
    }

    void setOnKeyEventExit(IBehavior behavior, Runnable action) {
        Objects.nonNull(behavior);
        Entry en = map.get(ON_KEY_EXIT);
        if (en == null) {
            en = new Entry();
            map.put(ON_KEY_EXIT, en);
        }

        if (behavior == null) {
            en.value = action;
        } else {
            en.behavior = behavior;
            en.behaviorValue = action;
        }
    }
}
