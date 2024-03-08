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
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Skinnable;
import javafx.scene.input.KeyEvent;
import com.sun.javafx.scene.control.input.EventHandlerPriority;
import com.sun.javafx.scene.control.input.KeyEventMapper;
import com.sun.javafx.scene.control.input.PHList;

/**
 * InputMap is a class that is set on a given {@link Control}. When the Node receives
 * an input event from the system, it passes this event in to the InputMap where
 * the InputMap can check all installed mappings to see if there is any
 * suitable mapping, and if so, fire the provided {@link EventHandler}.
 *
 * @since 999 TODO
 */
public final class InputMap {
    private static final Object NULL = new Object();
    private final Control control;
    // KeyBinding -> FunctionTag or FunctionHandler
    // FunctionTag -> FunctionHandler
    // EventType -> PHList
    private final HashMap<Object,Object> map = new HashMap<>();
    private SkinInputMap skinInputMap;
    private final KeyEventMapper kmapper = new KeyEventMapper();
    private final EventHandler<Event> eventHandler = this::handleEvent;

    /**
     * The constructor.
     * @param control the owner control
     */
    public InputMap(Control control) {
        this.control = control;
    }

    /**
     * Adds an event handler for the specified event type, at the control level.
     * This mapping always consumes the matching event.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    public <T extends Event> void addHandler(EventType<T> type, EventHandler<T> handler) {
        extendHandler(type, handler, EventHandlerPriority.USER_HIGH);
    }

    /**
     * Adds an event handler for the specified event type, at the control level.
     * This event handler will get invoked after all handlers added via map() methods.
     * This mapping always consumes the matching event.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    public <T extends Event> void addHandlerLast(EventType<T> type, EventHandler<T> handler) {
        extendHandler(type, handler, EventHandlerPriority.USER_LOW);
    }

    /**
     * Removes the specified handler.
     *
     * @param <T> the event class
     * @param type the event type
     * @param handler the handler to remove
     */
    public <T extends Event> void removeHandler(EventType<T> type, EventHandler<T> handler) {
        Object x = map.get(type);
        if (x instanceof PHList hs) {
            if (hs.remove(handler)) {
                map.remove(type);
                control.removeEventHandler(type, eventHandler);
            }
        }
    }
    
    private <T extends Event> void removeHandler(EventType<T> type, EventHandlerPriority pri) {
        Object x = map.get(type);
        if (x instanceof PHList hs) {
            if (hs.removeHandlers(Set.of(pri))) {
                map.remove(type);
                control.removeEventHandler(type, eventHandler);
            }
        }
    }

    private <T extends Event> void extendHandler(EventType<T> t, EventHandler<T> handler, EventHandlerPriority pri) {
        Object x = map.get(t);
        PHList hs;
        if(x instanceof PHList h) {
            hs = h;
        } else {
            // first entry for this event type
            hs = new PHList();
            map.put(t, hs);
            control.addEventHandler(t, eventHandler);
        }
        
        hs.add(pri, handler);
    }

    private void handleEvent(Event ev) {
        // probably unnecessary
        if (ev == null || ev.isConsumed()) {
            return;
        }

        EventType<?> t = ev.getEventType();
        Object x = map.get(t);
        if (x instanceof PHList hs) {
            hs.forEach((pri, h) -> {
                if (h == null) {
                    handleKeyBindingEvent(ev);
                } else {
                    h.handle(ev);
                }
                return !ev.isConsumed();
            });
        }
    }

    private void handleKeyBindingEvent(Event ev) {
        // probably unnecessary
        if (ev == null || ev.isConsumed()) {
            return;
        }

        KeyBinding k = KeyBinding.from((KeyEvent)ev);
        if (k != null) {
            FunctionHandler f = getFunction(k);
            if (f != null) {
                f.handleKeyBinding(ev, control);
            }
        }
    }

    static <C extends Skinnable> FunctionHandler<C> toFunctionHandler(FunctionHandlerConditional<C> h) {
        return new FunctionHandler<C>() {
            @Override
            public void handle(C control) {
                boolean consume = h.handle(control);
            }

            @Override
            public void handleKeyBinding(Event ev, C control) {
                boolean consume = h.handle(control);
                if (consume) {
                    ev.consume();
                }
            }
        };
    }

    /**
     * Registers a function for the given key binding.  This mapping will  take precedence
     * over any such mapping set by the skin.
     * @param <C> the skinnable type
     * @param k the key binding
     * @param function the function
     */
    public <C extends Skinnable> void register(KeyBinding k, FunctionHandler<C> function) {
        Objects.requireNonNull(k, "key binding must not be null");
        Objects.requireNonNull(function, "function must not be null");
        map.put(k, function);
    }

    /**
     * Adds (or overrides) a user-specified function under the given function tag.
     * This function will take precedence over any function set by the skin.
     *
     * @param <C> the skinnable type
     * @param tag the function tag
     * @param function the function
     */
    public <C extends Skinnable> void registerFunction(FunctionTag tag, FunctionHandler<C> function) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        map.put(tag, function);
    }

    /**
     * Adds (or overrides) a user-specified function under the given function tag.
     * This function will take precedence over any function set by the skin.
     * This method allows for controlling whether the matching event will be consumed or not.
     *
     * @param <C> the skinnable type
     * @param tag the function tag
     * @param function the function
     */
    public <C extends Skinnable> void registerFunctionCond(FunctionTag tag, FunctionHandlerConditional<C> function) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        map.put(tag, toFunctionHandler(function));
    }

    /**
     * Link a key binding to the specified function tag.
     * When the key binding matches the input event, the function is executed, the event is consumed,
     * and the process of dispatching is stopped.
     * <p>
     * This method will take precedence over any function set by the skin.
     *
     * @param k the key binding
     * @param tag the function tag
     */
    public void registerKey(KeyBinding k, FunctionTag tag) {
        Objects.requireNonNull(k, "KeyBinding must not be null");
        Objects.requireNonNull(tag, "function tag must not be null");
        map.put(k, tag);

        EventType<KeyEvent> t = kmapper.addType(k);
        extendHandler(t, null, EventHandlerPriority.USER_KB);
    }

    /**
     * Returns a {@code FunctionHandler} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @param <C> the skinnable type
     * @param tag the function tag
     * @return the function, or null
     */
    public <C extends Skinnable> FunctionHandler<C> getFunction(FunctionTag tag) {
        Object x = map.get(tag);
        // TODO check for NULL?
        if (x instanceof FunctionHandler r) {
            return r;
        } else if (skinInputMap != null) {
            return skinInputMap.getFunction(tag);
        }
        return null;
    }

    /**
     * Returns a default {@code FunctionHandler} mapped to the specified function tag, or null if no such mapping exists.
     *
     * @implNote the return value might be a lambda, i.e. it will return a new instance each time this method is called.
     *
     * @param <C> the skinnable type
     * @param tag the function tag
     * @return the function, or null
     */
    public <C extends Skinnable> FunctionHandler<C> getDefaultFunction(FunctionTag tag) {
        if (skinInputMap != null) {
            return skinInputMap.getFunction(tag);
        }
        return null;
    }

    /**
     * Returns a {@code FunctionHandler} mapped to the specified {@link KeyBinding},
     * or null if no such mapping exists.
     *
     * @param <C> the skinnable type
     * @param k the key binding
     * @return the function, or null
     */
    public <C extends Skinnable> FunctionHandler<C> getFunction(KeyBinding k) {
        Object x = resolve(k);
        if (x instanceof FunctionTag tag) {
            return getFunction(tag);
        } else if (x instanceof FunctionHandler h) {
            return h;
        }
        return null;
    }

    private Object resolve(KeyBinding k) {
        Object x = map.get(k);
        if (x != null) {
            return x;
        }
        if (skinInputMap != null) {
            return skinInputMap.resolve(k);
        }
        return null;
    }

    /**
     * Unbinds the specified key binding.
     *
     * @param k the key binding
     */
    public void unbind(KeyBinding k) {
        map.put(k, NULL);
    }

    /**
     * Reverts all the key bindings set by user.
     * This method restores key bindings set by the skin which were overwritten by the user.
     */
    public void resetKeyBindings() {
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> me = it.next();
            if (me.getKey() instanceof KeyBinding) {
                it.remove();
            }
        }
    }

    /**
     * Restores the specified key binding to the value set by the behavior, if any.
     *
     * @param k the key binding
     */
    public void restoreDefaultKeyBinding(KeyBinding k) {
        Object x = map.get(k);
        if (x != null) {
            map.remove(k);
        }
    }

    /**
     * Restores the specified function tag to the value set by the behavior, if any.
     *
     * @param tag the function tag
     */
    public void restoreDefaultFunction(FunctionTag tag) {
        Objects.requireNonNull(tag, "function tag must not be null");
        map.remove(tag);
    }

    /**
     * Collects all mapped key bindings (set either by the user or the behavior).
     * @return the set of key bindings
     */
    public Set<KeyBinding> getKeyBindings() {
        return collectKeyBindings(null);
    }

    /**
     * Returns the set of key bindings mapped to the specified function tag.
     * @param tag the function tag
     * @return the set of KeyBindings, non-null
     */
    public Set<KeyBinding> getKeyBindingsFor(FunctionTag tag) {
        return collectKeyBindings(tag);
    }

    private Set<KeyBinding> collectKeyBindings(FunctionTag tag) {
        HashSet<KeyBinding> bindings = new HashSet<>();
        for (Map.Entry<Object, Object> en : map.entrySet()) {
            if (en.getKey() instanceof KeyBinding k) {
                if ((tag == null) || (tag == en.getValue())) {
                    bindings.add(k);
                }
            }
        }

        if (skinInputMap != null) {
            skinInputMap.collectKeyBindings(bindings, tag);
        }
        return bindings;
    }

    /**
     * Removes all the key bindings mapped to the specified function tag, either by the application or by the skin.
     * This is an irreversible operation.
     * @param tag the function tag
     */
    // TODO this should not affect the skin input map, but perhaps place NULL for each found KeyBinding
    public void unbind(FunctionTag tag) {
        if (skinInputMap != null) {
            skinInputMap.unbind(tag);
        }
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> en = it.next();
            if (tag == en.getValue()) {
                // the entry must be KeyBinding -> FunctionTag
                it.remove();
            }
        }
    }

    /**
     * Sets the skin input map, adding necessary event handlers to the control instance when required.
     * This method must be called by the skin only from its {@link Skin#install()} or
     * {@link SkinBase#setSkinInputMap(SkinInputMap)} method.
     * <p>
     * This method removes all the mappings from the previous skin input map, if any.
     * @param m the skin input map
     */
    public void setSkinInputMap(SkinInputMap m) {
        if (skinInputMap != null) {
            // uninstall all handlers with SKIN_* priority
            Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Object, Object> en = it.next();
                if (en.getKey() instanceof EventType t) {
                    PHList hs = (PHList)en.getValue();
                    if (hs.removeHandlers(EventHandlerPriority.ALL_SKIN)) {
                        it.remove();
                        control.removeEventHandler(t, eventHandler);
                    }
                }
            }
        }

        skinInputMap = m;

        if (skinInputMap != null) {
            // install skin handlers with their priority
            skinInputMap.forEach((type, pri, h) -> {
                extendHandler(type, h, pri);
            });

            // add key bindings listeners if needed
            if (!kmapper.hasKeyPressed() && skinInputMap.kmapper.hasKeyPressed()) {
                extendHandler(KeyEvent.KEY_PRESSED, null, EventHandlerPriority.SKIN_KB);
            }
            if (!kmapper.hasKeyReleased() && skinInputMap.kmapper.hasKeyReleased()) {
                extendHandler(KeyEvent.KEY_RELEASED, null, EventHandlerPriority.SKIN_KB);
            }
            if (!kmapper.hasKeyTyped() && skinInputMap.kmapper.hasKeyTyped()) {
                extendHandler(KeyEvent.KEY_TYPED, null, EventHandlerPriority.SKIN_KB);
            }
        }
    }
}
