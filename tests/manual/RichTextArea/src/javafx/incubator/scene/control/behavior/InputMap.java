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
package javafx.incubator.scene.control.behavior;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.sun.javafx.scene.control.input.HList;

/**
 * InputMap is a class that is set on a given {@link Control}. When the Node receives
 * an input event from the system, it passes this event in to the InputMap where
 * the InputMap can check all installed mappings to see if there is any
 * suitable mapping, and if so, fire the provided {@link EventHandler}.
 *
 * @param <C> The type of the Control that the InputMap is installed in.
 * @since 22
 */
public final class InputMap<C extends Control> {
    /** contains user- and behavior-specific key bindings and function mappings */
    private static class Entry { // TODO rename to Mapping?
        Object value;
        BehaviorBase behavior;
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
    // ON_KEY_ENTER/EXIT -> Entry with value=Runnable
    private final HashMap<Object,Entry> map = new HashMap<>();

    /**
     * The constructor.
     * @param control the owner
     */
    public InputMap(C control) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }
        this.control = control;
    }

    /**
     * The Control to which this InputMap is attached.
     */
    private final C getControl() {
        return control;
    }

    private void handleEvent(Event ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        EventType<?> t = ev.getEventType();
        HList handlers = getHandlers(t);
        if (handlers != null) {
            for (EventHandler h: handlers) {
                h.handle(ev);
                if (ev.isConsumed()) {
                    break;
                }
            }
        }
    }

    private void handleKeyEvent(Event ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        KeyBinding k = KeyBinding.from((KeyEvent)ev);
        Runnable f = getFunction(k);
        if (f != null) {
            handleKeyFunctionEnter();
            try {
                f.run();
                ev.consume();
            } finally {
                handleKeyFunctionExit();
            }
            return;
        }

        EventType<?> t = ev.getEventType();
        HList handlers = getHandlers(t);
        if (handlers != null) {
            handleKeyFunctionEnter();
            try {
                for (EventHandler h: handlers) {
                    h.handle(ev);
                    if (ev.isConsumed()) {
                        break;
                    }
                }
            } finally {
                handleKeyFunctionExit();
            }
        }
    }

    private void handleKeyFunctionEnter() {
        Entry en = map.get(ON_KEY_ENTER);
        if (en != null) {
            Object x = en.getValue();
            if (x instanceof Runnable r) {
                r.run();
            }
        }
    }

    private void handleKeyFunctionExit() {
        Entry en = map.get(ON_KEY_EXIT);
        if (en != null) {
            Object x = en.getValue();
            if (x instanceof Runnable r) {
                r.run();
            }
        }
    }

    /**
     * Removes all the mappings set by the behavior.
     * Behavior developers do not need to call this method directly, as it is being called in BehaviorBase.dispose().
     *
     * @param behavior
     */
    void unregister(BehaviorBase behavior) {
        Objects.nonNull(behavior);

        for (Entry en: map.values()) {
            if (en.behavior == behavior) {
                en.behavior = null;
                en.behaviorValue = null;
            }
        }
    }

    <T extends Event> void addHandler(
        BehaviorBase behavior,
        EventType<T> type,
        boolean consume,
        boolean tail,
        EventHandler<T> handler
    ) {
        if (consume) {
            extendHandlers(behavior, type, tail, new EventHandler<T>() {
                @Override
                public void handle(T ev) {
                    handler.handle(ev);
                    ev.consume();
                }
            });
        } else {
            extendHandlers(behavior, type, tail, handler);
        }
    }

    <T extends Event> void addHandler(
        BehaviorBase behavior,
        EventCriteria<T> criteria,
        boolean consume,
        boolean tail,
        EventHandler<T> handler
    ) {
        EventType<T> type = criteria.getEventType();
        extendHandlers(behavior, type, tail, new EventHandler<T>() {
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

    private <T extends Event> void extendHandlers(
        BehaviorBase behavior,
        EventType<T> t,
        boolean tail,
        EventHandler<T> h
    ) {
        Objects.nonNull(behavior);
        Entry en = addListenerIfNeeded(t);

        HList handlers = HList.from(en.behaviorValue);
        handlers.add(h, tail);
        en.behavior = behavior;
        en.behaviorValue = handlers;
    }

    /**
     * Adds a user-specified function under the given function tag.
     * This function will override any function set by the behavior.
     * @param tag the function tag
     * @param function the function
     */
    public void registerFunction(FunctionTag tag, Runnable function) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(null, tag, function);
    }

    /**
     * Maps a function to the function tag, for use by the behavior.
     * This method will not override any previous mapping added by {@link #registerFunction(FunctionTag,Runnable)}.
     *
     * @param behavior the owner
     * @param tag the function tag
     * @param function the function
     */
    void registerFunction(BehaviorBase behavior, FunctionTag tag, Runnable function) {
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        addFunction(behavior, tag, function);
    }

    /**
     * Link a key binding to the specified function tag.
     * This method will override a mapping set by the behavior.
     *
     * @param k the key binding
     * @param tag the function tag
     */
    public void registerKey(KeyBinding k, FunctionTag tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(null, k, tag);
    }

    /**
     * Maps a key binding to the specified function tag, for use by the behavior.
     * A null key binding will result in no change to this input map.
     * This method will not override a user mapping added by {@link #registerKey(KeyBinding,FunctionTag)}.
     *
     * @param behavior the owner
     * @param k the key binding, can be null TODO variant: KeyBinding.NA
     * @param tag the function tag
     */
    void registerKey(BehaviorBase behavior, KeyBinding k, FunctionTag tag) {
        if (k == null) {
            return;
        }
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        addBinding(behavior, k, tag);
    }

    /**
     * Maps a key binding to the specified function tag, as a part of the behavior.
     * This method will not override a user mapping added by {@link #registerKey(KeyBinding,FunctionTag)}.
     *
     * @param behavior the owner
     * @param code the key code to construct a {@link KeyBinding}
     * @param tag the function tag
     */
    void registerKey(BehaviorBase behavior, KeyCode code, FunctionTag tag) {
        registerKey(behavior, KeyBinding.of(code), tag);
    }

    private void addFunction(BehaviorBase behavior, FunctionTag tag, Runnable function) {
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

    private void addBinding(BehaviorBase behavior, KeyBinding k, FunctionTag tag) {
        Entry en = map.get(k);
        if (en == null) {
            en = new Entry();
            map.put(k, en);
        }

        if (behavior == null) {
            // user mapping
            en.value = tag;
        } else {
            // behavior mapping
            en.behavior = behavior;
            en.behaviorValue = tag;
        }

        EventType<KeyEvent> type = k.getEventType();
        addListenerIfNeeded(type);
    }

    /**
     * Returns a {@code Runnable} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @param tag the function tag
     * @return the function, or null
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
     * Returns a default {@code Runnable} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @param tag the function tag
     * @return the function, or null
     */
    public Runnable getDefaultFunction(FunctionTag tag) {
        Entry en = map.get(tag);
        if (en != null) {
            Object v = en.behaviorValue;
            if (v instanceof Runnable r) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns a {@code Runnable} mapped to the specified {@link KeyBinding},
     * or null if no such mapping exists.
     * <p>
     * @implNote
     * This method is a functional equivalent of calling {@link #getFunctionTag(KeyBinding)}
     * followed by {@link #getFunction(FunctionTag)} (if the tag is not null).
     *
     * @param k the key binding
     * @return the function, or null
     */
    public Runnable getFunction(KeyBinding k) {
        FunctionTag tag = getFunctionTag(k);
        if (tag != null) {
            return getFunction(tag);
        }
        return null;
    }

    /**
     * Returns a {@code FunctionTag} mapped to the specified {@link KeyBinding},
     * or null if no such mapping exists.
     *
     * @param k the key binding
     * @return the function tag, or null
     */
    public FunctionTag getFunctionTag(KeyBinding k) {
        Entry en = map.get(k);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof FunctionTag tag) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Returns a default {@code Runnable} mapped to the specified {@link KeyBinding},
     * or null if no such mapping exists.
     *
     * @param k the key binding
     * @return the function, or null
     */
    public Runnable getDefaultFunction(KeyBinding k) {
        // TODO this needs to be tested
        Entry en = map.get(k);
        if (en != null) {
            Object v = en.behaviorValue;
            if (v instanceof FunctionTag tag) {
                return getDefaultFunction(tag);
            }
        }
        return null;
    }

    private HList getHandlers(EventType<?> t) {
        Entry en = map.get(t);
        if (en != null) {
            Object v = en.getValue();
            if (v instanceof HList list) {
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
     * @param k the key binding
     */
    public void unbind(KeyBinding k) {
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
            if (me.getKey() instanceof KeyBinding) {
                Entry en = me.getValue();
                en.value = null;
            }
        }
    }

    /**
     * Restores the specified key binding to the value set by the behavior, if any.
     *
     * @param k the key binding
     */
    public void restoreDefaultKeyBinding(KeyBinding k) {
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
     * @param tag the function tag
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
     * @return a Set of key bindings
     */
    public Set<KeyBinding> getKeyBindings() {
        return map.keySet().stream().
            filter((k) -> (k instanceof KeyBinding)).
            map((x) -> (KeyBinding)x).
            collect(Collectors.toSet());
    }

    /**
     * Returns the set of key bindings mapped to the specified function tag.
     * @param tag the function tag
     * @return the set of KeyBindings
     */
    public Set<KeyBinding> getKeyBindingFor(FunctionTag tag) {
        /*
        return map.entrySet().stream().
            filter((me) -> (me.getKey() instanceof KeyBinding) && (me.getValue().getValue() == tag)).
            map((me) -> (KeyBinding)me.getKey()).
            collect(Collectors.toSet());
        */
        HashSet<KeyBinding> set = new HashSet<>();
        for (Map.Entry<Object, Entry> k : map.entrySet()) {
            if (k.getKey() instanceof KeyBinding kb) {
                Entry en = k.getValue();
                Object v = en.getValue();
                if (tag == v) {
                    set.add(kb);
                }
            }
        }
        return set;
    }

    /**
     * Removes all the key bindings mapped to the specified function tag, either by the application or by the skin.
     * @param tag the function tag
     */
    public void unbind(FunctionTag tag) {
        Iterator<Object> it = map.keySet().iterator();
        while (it.hasNext()) {
            Object k = it.next();
            if (k instanceof KeyBinding kb) {
                Entry en = map.get(k);
                Object v = en.getValue();
                if (tag == v) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Maps a new KeyBinding as an alias to the existing one with the same owner and function tag.
     * This method does nothing if there is no mapping for k1.
     * Subsequent changes to the original mapping are not propagated to the alias.
     *
     * @param existing the existing key binding
     * @param newk the new key binding
     */
    void duplicateMapping(KeyBinding existing, KeyBinding newk) {
        Entry en1 = map.get(existing);
        if ((en1 != null) && (en1.behaviorValue != null)) {
            Entry en2 = map.get(newk);
            if (en2 == null) {
                en2 = new Entry();
            }
            en2.behavior = en1.behavior;
            en2.behaviorValue = en1.behaviorValue;
            map.put(newk, en2);
        }
    }

    void setOnKeyEventEnter(BehaviorBase behavior, Runnable action) {
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

   void setOnKeyEventExit(BehaviorBase behavior, Runnable action) {
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
