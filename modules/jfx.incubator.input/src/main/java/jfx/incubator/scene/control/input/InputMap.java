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
package jfx.incubator.scene.control.input;

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
import javafx.scene.input.KeyEvent;
import com.sun.javafx.ModuleUtil;
import com.sun.jfx.incubator.scene.control.input.EventHandlerPriority;
import com.sun.jfx.incubator.scene.control.input.InputMapHelper;
import com.sun.jfx.incubator.scene.control.input.KeyEventMapper;
import com.sun.jfx.incubator.scene.control.input.PHList;
import com.sun.jfx.incubator.scene.control.input.SkinInputMap;

/**
 * InputMap is a property of the {@link Control} class which enables customization
 * by allowing creation of custom key mappings and event handlers.
 * <p>
 * The {@code InputMap} serves as a bridge between the Control and its Skin.
 * The {@code InputMap} provides an ordered repository of event handlers,
 * working together with the input map managed by the skin, which
 * guarantees the order in which handlers are invoked.
 * It also stores key mappings with a similar guarantee that the application mappings
 * always take precedence over mappings created by the skin,
 * regardless of when the skin was created or replaced.
 * <p>
 * The class supports the following scenarios:
 * <ul>
 * <li>Mapping a key binding to a function
 * <li>Removing a key binding
 * <li>Mapping a new function to an existing key binding
 * <li>Retrieving the default function
 * <li>Ensuring that the application key mappings take priority over mappings created by the skin
 * </ul>
 * For key mappings, the {@code InputMap} utilizes a two-stage lookup.
 * First, the key event is matched to a {@link FunctionTag} which identifies a function provided either by the skin
 * or the associated behavior (the "default" function), or by the application.
 * When such a mapping exists, the found function tag is matched to a function registered either by
 * the application or by the skin.
 * <p>
 * Additionally, the {@link #register(KeyBinding, Runnable)} method allows mapping to a function directly,
 * bypassing the function tag.
 * <p>
 * This mechanism allows for customizing the key mappings and the underlying functions independently and separately.
 *
 * @since 24
 */
public final class InputMap {
    private static final Object NULL = new Object();
    private final Control control;
    /**
     * <pre> KeyBinding -> FunctionTag or Runnable
     * FunctionTag -> Runnable
     * EventType -> PHList</pre>
     */
    private final HashMap<Object, Object> map = new HashMap<>();
    private SkinInputMap skinInputMap;
    private final KeyEventMapper kmapper = new KeyEventMapper();
    private final EventHandler<Event> eventHandler = this::handleEvent;

    static {
        ModuleUtil.incubatorWarning();
        initAccessor();
    }

    /**
     * The constructor.
     * @param control the owner control
     */
    public InputMap(Control control) {
        this.control = control;
    }

    /**
     * Adds an event handler for the specified event type.
     * Event handlers added with this method will always be called before any handlers registered by the skin.
     *
     * @param <T> the actual event type
     * @param type the event type
     * @param handler the event handler
     */
    public <T extends Event> void addHandler(EventType<T> type, EventHandler<T> handler) {
        extendHandler(type, handler, EventHandlerPriority.USER_HIGH);
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
        if (x instanceof PHList h) {
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
            boolean consume = execute(ev.getSource(), k);
            if (consume) {
                ev.consume();
            }
        }
    }

    private boolean execute(Object source, KeyBinding k) {
        Object x = resolve(k);
        if (x instanceof FunctionTag tag) {
            return execute(source, tag);
        } else if (x instanceof BooleanSupplier h) {
            return h.getAsBoolean();
        } else if (x instanceof Runnable r) {
            r.run();
            return true;
        }
        return false;
    }

    // package protected to prevent unauthorized code to supply wrong instance of control (source)
    boolean execute(Object source, FunctionTag tag) {
        Object x = map.get(tag);
        if (x instanceof Runnable r) {
            r.run();
            return true;
        }

        return executeDefault(source, tag);
    }

    // package protected to prevent unauthorized code to supply wrong instance of control (source)
    boolean executeDefault(Object source, FunctionTag tag) {
        if (skinInputMap != null) {
            return skinInputMap.execute(source, tag);
        }
        return false;
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
     * Registers a function for the given key binding.  This mapping will take precedence
     * over the default mapping set by the skin.
     *
     * @param k the key binding
     * @param function the function
     */
    public void register(KeyBinding k, Runnable function) {
        Objects.requireNonNull(k, "key binding must not be null");
        Objects.requireNonNull(function, "function must not be null");
        map.put(k, function);
    }

    /**
     * Adds (or overrides) a user-specified function under the given function tag.
     * This function will take precedence over any default function set by the skin.
     *
     * @param tag the function tag
     * @param function the function
     */
    public void registerFunction(FunctionTag tag, Runnable function) {
        Objects.requireNonNull(tag, "function tag must not be null");
        Objects.requireNonNull(function, "function must not be null");
        map.put(tag, function);
    }

    /**
     * Link a key binding to the specified function tag.
     * When the key binding matches the input event, the function is executed, the event is consumed,
     * and the process of dispatching is stopped.
     * <p>
     * This method will take precedence over any default function set by the skin.
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
     * Disables the specified key binding.
     * Calling this method will disable any mappings made with
     * {@link #register(KeyBinding, Runnable)},
     * {@link #registerKey(KeyBinding, FunctionTag)},
     * or registered by the skin.
     *
     * @param k the key binding
     */
    public void disableKeyBinding(KeyBinding k) {
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
     * Restores the specified key binding to the value set by the skin, if any.
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
     * Restores the specified function tag to the value set by the skin, if any.
     *
     * @param tag the function tag
     */
    public void restoreDefaultFunction(FunctionTag tag) {
        Objects.requireNonNull(tag, "function tag must not be null");
        map.remove(tag);
    }

    /**
     * Collects all mapped key bindings.
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

    // null tag collects all bindings
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
    public void removeKeyBindingsFor(FunctionTag tag) {
        if (skinInputMap != null) {
            skinInputMap.unbind(tag);
        }
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> en = it.next();
            if (tag == en.getValue()) {
                // the entry must be KeyBinding -> FunctionTag
                if (en.getKey() instanceof KeyBinding) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Sets the skin input map, adding necessary event handlers to the control instance when required.
     * This method must be called by the skin only from its
     * {@link javafx.scene.control.Skin#install() Skin.install()}
     * method.
     * <p>
     * This method removes all the mappings from the previous skin input map, if any.
     * @param m the skin input map
     */
    // TODO change to public once SkinInputMap is public
    // or add getSkinInputMap() to Skin.
    private void setSkinInputMap(SkinInputMap m) {
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

    private static void initAccessor() {
        InputMapHelper.setAccessor(new InputMapHelper.Accessor() {
            // TODO will be unnecessary after JDK-8314968
            @Override
            public void executeDefault(Object source, InputMap inputMap, FunctionTag tag) {
                inputMap.executeDefault(source, tag);
            }

            // TODO will be unnecessary after JDK-8314968
            @Override
            public void execute(Object source, InputMap inputMap, FunctionTag tag) {
                inputMap.execute(source, tag);
            }

            // TODO will be unnecessary once SkinInputMap is public
            @Override
            public void setSkinInputMap(InputMap inputMap, SkinInputMap sm) {
                inputMap.setSkinInputMap(sm);
            }
        });
    }
}
