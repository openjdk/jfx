/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.inputmap;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * InputMap is a class that is set on a given {@link Node}. When the Node receives
 * an input event from the system, it passes this event in to the InputMap where
 * the InputMap can check all installed
 * {@link InputMap.Mapping mappings} to see if there is any
 * suitable mapping, and if so, fire the provided {@link EventHandler}.
 *
 * @param <N> The type of the Node that the InputMap is installed in.
 * @since 9
 */
public class InputMap<N extends Node> implements EventHandler<Event> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final N node;

    private final ObservableList<InputMap<N>> childInputMaps;

    private final ObservableList<Mapping<?>> mappings;

//    private final ObservableList<Predicate<? extends Event>> interceptors;

    private final Map<EventType<?>, List<EventHandler<? super Event>>> installedEventHandlers;

    private final Map<EventType, List<Mapping>> eventTypeMappings;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates the new InputMap instance which is related specifically to the
     * given Node.
     * @param node The Node for which this InputMap is attached.
     */
    public InputMap(N node) {
        if (node == null) {
            throw new IllegalArgumentException("Node can not be null");
        }

        this.node = node;
        this.eventTypeMappings = new HashMap<>();
        this.installedEventHandlers = new HashMap<>();
//        this.interceptors = FXCollections.observableArrayList();

        // listeners
        this.mappings = FXCollections.observableArrayList();
        mappings.addListener((ListChangeListener<Mapping<?>>) c -> {
            while (c.next()) {
                // TODO handle mapping removal
                if (c.wasRemoved()) {
                    for (Mapping<?> mapping : c.getRemoved()) {
                        removeMapping(mapping);
                    }
                }

                if (c.wasAdded()) {
                    List<Mapping<?>> toRemove = new ArrayList<>();
                    for (Mapping<?> mapping : c.getAddedSubList()) {
                        if (mapping == null) {
                            toRemove.add(null);
                        } else {
                            addMapping(mapping);
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        getMappings().removeAll(toRemove);
                        throw new IllegalArgumentException("Null mappings not permitted");
                    }
                }
            }
        });

        childInputMaps = FXCollections.observableArrayList();
        childInputMaps.addListener((ListChangeListener<InputMap<N>>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (InputMap<N> map : c.getRemoved()) {
                        map.setParentInputMap(null);
                    }
                }

                if (c.wasAdded()) {
                    List<InputMap<N>> toRemove = new ArrayList<>();
                    for (InputMap<N> map : c.getAddedSubList()) {
                        // we check that the child input map maps to the same node
                        // as this input map
                        if (map.getNode() != getNode()) {
                            toRemove.add(map);
                        } else {
                            map.setParentInputMap(this);
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        getChildInputMaps().removeAll(toRemove);
                        throw new IllegalArgumentException("Child InputMap intances need to share a common Node object");
                    }
                }
            }
        });
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- parent behavior - for now this is an private property
    private ReadOnlyObjectWrapper<InputMap<N>> parentInputMap = new ReadOnlyObjectWrapper<InputMap<N>>(this, "parentInputMap") {
        @Override protected void invalidated() {
            // whenever the parent InputMap changes, we uninstall all mappings and
            // then reprocess them so that they are installed in the correct root.
            reprocessAllMappings();
        }
    };
    private final void setParentInputMap(InputMap<N> value) { parentInputMap.set(value); }
    private final InputMap<N> getParentInputMap() {return parentInputMap.get(); }
    private final ReadOnlyObjectProperty<InputMap<N>> parentInputMapProperty() { return parentInputMap.getReadOnlyProperty(); }


    // --- interceptor
    /**
     * The role of the interceptor is to block the InputMap on which it is
     * set from executing any mappings (contained within itself, or within a
     * {@link #getChildInputMaps() child InputMap}, whenever the interceptor
     * returns true. The interceptor is called every time an input event is received,
     * and is allowed to reason on the given input event
     * before returning a boolean value, where boolean true means block
     * execution, and boolean false means to allow execution.
     */
    private ObjectProperty<Predicate<? extends Event>> interceptor = new SimpleObjectProperty<>(this, "interceptor");
    public final Predicate<? extends Event> getInterceptor() {
        return interceptor.get();
    }
    public final void setInterceptor(Predicate<? extends Event> value) {
        interceptor.set(value);
    }
    public final ObjectProperty<Predicate<? extends Event>> interceptorProperty() {
        return interceptor;
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The Node for which this InputMap is attached.
     */
    public final N getNode() {
        return node;
    }

    /**
     * A mutable list of input mappings. Each will be considered whenever an
     * input event is being looked up, and one of which may be used to handle
     * the input event, based on the specifificity returned by each mapping
     * (that is, the mapping with the highest specificity wins).
     */
    public ObservableList<Mapping<?>> getMappings() {
        return mappings;
    }

    /**
     * A mutable list of child InputMaps. An InputMap may have child input maps,
     * as this allows for easy addition
     * of mappings that are state-specific. For example, if a Node can be in two
     * different states, and the input mappings are different for each, then it
     * makes sense to have one root (and empty) InputMap, with two children
     * input maps, where each is populated with the specific input mappings for
     * one of the two states. To prevent the wrong input map from being considered,
     * it is simply a matter of setting an appropriate
     * {@link #interceptorProperty() interceptor} on each map, so that they are only
     * considered in one of the two states.
     */
    public ObservableList<InputMap<N>> getChildInputMaps() {
        return childInputMaps;
    }

    /**
     * Disposes all child InputMaps, removes all event handlers from the Node,
     * and clears the mappings list.
     */
    public void dispose() {
        for (InputMap<N> childInputMap : getChildInputMaps()) {
            childInputMap.dispose();
        }

        // uninstall event handlers
        removeAllEventHandlers();

        // clear out all mappings
        getMappings().clear();
    }

    /** {@inheritDoc} */
    @Override public void handle(Event e) {
        if (e == null || e.isConsumed()) return;

        List<Mapping<?>> mappings = lookup(e, true);
        for (Mapping<?> mapping : mappings) {
            EventHandler eventHandler = mapping.getEventHandler();
            if (eventHandler != null) {
                eventHandler.handle(e);
            }

            if (mapping.isAutoConsume()) {
                e.consume();
            }

            if (e.isConsumed()) {
                break;
            }

            // If we are here, the event has not been consumed, so we continue
            // looping through our list of matches. Refer to the documentation in
            // lookup(Event) for more details on the list ordering.
        }
    }

    /**
     * Looks up the most specific mapping given the input, ignoring all
     * interceptors. The valid values that can be passed into this method is
     * based on the values returned by the {@link Mapping#getMappingKey()}
     * method. Based on the subclasses of Mapping that ship with JavaFX, the
     * valid values are therefore:
     *
     * <ul>
     *     <li><strong>KeyMapping:</strong> A valid {@link KeyBinding}.</li>
     *     <li><strong>MouseMapping:</strong> A valid {@link MouseEvent} event
     *     type (e.g. {@code MouseEvent.MOUSE_PRESSED}).</li>
     * </ul>
     *
     * For other Mapping subclasses, refer to their javadoc, and specifically
     * what is returned by {@link Mapping#getMappingKey()},
     *
     * @param mappingKey
     * @return
     */
    // TODO return all mappings, or just the first one?
    public Optional<Mapping<?>> lookupMapping(Object mappingKey) {
        if (mappingKey == null) {
            return Optional.empty();
        }

        List<Mapping<?>> mappings = lookupMappingKey(mappingKey);

        // descend into our child input maps as well
        for (int i = 0; i < getChildInputMaps().size(); i++) {
            InputMap<N> childInputMap = getChildInputMaps().get(i);

            List<Mapping<?>> childMappings = childInputMap.lookupMappingKey(mappingKey);
            mappings.addAll(0, childMappings);
        }

        return mappings.size() > 0 ? Optional.of(mappings.get(0)) : Optional.empty();
    }




    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private List<Mapping<?>> lookupMappingKey(Object mappingKey) {
        return getMappings().stream()
                .filter(mapping -> !mapping.isDisabled())
                .filter(mapping -> mappingKey.equals(mapping.getMappingKey()))
                .collect(Collectors.toList());
    }

    /*
     * Returns a List of Mapping instances, in priority order (from highest priority
     * to lowest priority). All mappings in the list have the same value specificity,
     * so are ranked based on the input map (with the leaf input maps taking
     * precedence over parent / root input maps).
     */
    private List<Mapping<?>> lookup(Event event, boolean testInterceptors) {
        // firstly we look at ourselves to see if we have a mapping, assuming our
        // interceptors are valid
        if (testInterceptors) {
            boolean interceptorsApplies = testInterceptor(event, getInterceptor());

            if (interceptorsApplies) {
                return Collections.emptyList();
            }
        }

        List<Mapping<?>> mappings = new ArrayList<>();

        int minSpecificity = 0;
        List<Pair<Integer, Mapping<?>>> results = lookupMappingAndSpecificity(event, minSpecificity);
        if (! results.isEmpty()) {
            minSpecificity = results.get(0).getKey();
            mappings.addAll(results.stream().map(pair -> pair.getValue()).collect(Collectors.toList()));
        }

        // but we always descend into our child input maps as well, to see if there
        // is a more specific mapping there. If there is a mapping of equal
        // specificity, we take the child mapping over the parent mapping.
        for (int i = 0; i < getChildInputMaps().size(); i++) {
            InputMap childInputMap = getChildInputMaps().get(i);
            minSpecificity = scanRecursively(childInputMap, event, testInterceptors, minSpecificity, mappings);
        }

        return mappings;
    }

    private int scanRecursively(InputMap<?> inputMap, Event event, boolean testInterceptors, int minSpecificity, List<Mapping<?>> mappings) {
        // test if the childInputMap should be considered
        if (testInterceptors) {
            boolean interceptorsApplies = testInterceptor(event, inputMap.getInterceptor());
            if (interceptorsApplies) {
                return minSpecificity;
            }
        }

        // look at the given InputMap
        List<Pair<Integer, Mapping<?>>> childResults = inputMap.lookupMappingAndSpecificity(event, minSpecificity);
        if (!childResults.isEmpty()) {
            int specificity = childResults.get(0).getKey();
            List<Mapping<?>> childMappings = childResults.stream()
                    .map(pair -> pair.getValue())
                    .collect(Collectors.toList());
            if (specificity == minSpecificity) {
                mappings.addAll(0, childMappings);
            } else if (specificity > minSpecificity) {
                mappings.clear();
                minSpecificity = specificity;
                mappings.addAll(childMappings);
            }
        }

        // now look at the children of this input map, if any exist
        for (int i = 0; i < inputMap.getChildInputMaps().size(); i++) {
            minSpecificity = scanRecursively(inputMap.getChildInputMaps().get(i), event, testInterceptors, minSpecificity, mappings);
        }

        return minSpecificity;
    }

    private InputMap<N> getRootInputMap() {
        InputMap<N> rootInputMap = this;
        while (true) {
            if (rootInputMap == null) break;
            InputMap<N> parentInputMap = rootInputMap.getParentInputMap();
            if (parentInputMap == null) break;
            rootInputMap = parentInputMap;
        }
        return rootInputMap;
    }

    private void addMapping(Mapping<?> mapping) {
        InputMap<N> rootInputMap = getRootInputMap();

        // we want to track the event handlers we install, so that we can clean
        // up in the dispose() method (and also so that we don't duplicate
        // event handlers for a single event type). Because this is all handled
        // in the root InputMap, we firstly find it, and then we defer to it.
        rootInputMap.addEventHandler(mapping.eventType);

        // we maintain a separate map of all mappings, which maps from the
        // mapping event type into a list of mappings. This allows for easier
        // iteration in the lookup methods.
        EventType<?> et = mapping.getEventType();
        List<Mapping> _eventTypeMappings = this.eventTypeMappings.computeIfAbsent(et, f -> new ArrayList<>());
        _eventTypeMappings.add(mapping);
    }

    private void removeMapping(Mapping<?> mapping) {
        EventType<?> et = mapping.getEventType();
        if (this.eventTypeMappings.containsKey(et)) {
            List<?> _eventTypeMappings = this.eventTypeMappings.get(et);
            _eventTypeMappings.remove(mapping);

            // TODO remove the event handler in the root if there are no more mappings of this type
            // anywhere in the input map tree
        }
    }

    private void addEventHandler(EventType et) {
        List<EventHandler<? super Event>> eventHandlers =
                installedEventHandlers.computeIfAbsent(et, f -> new ArrayList<>());

        final EventHandler<? super Event> eventHandler = this::handle;

        if (eventHandlers.isEmpty()) {
//            System.out.println("Added event handler for type " + et);
            node.addEventHandler(et, eventHandler);
        }

        // We need to store these event handlers so we can dispose cleanly.
        eventHandlers.add(eventHandler);
    }

    private void removeAllEventHandlers() {
        for (EventType<?> et : installedEventHandlers.keySet()) {
            List<EventHandler<? super Event>> handlers = installedEventHandlers.get(et);
            for (EventHandler<? super Event> handler : handlers) {
//                System.out.println("Removed event handler for type " + et);
                node.removeEventHandler(et, handler);
            }
        }
    }

    private void reprocessAllMappings() {
        removeAllEventHandlers();
        this.mappings.stream().forEach(this::addMapping);

        // now do the same for all children
        for (InputMap<N> child : getChildInputMaps()) {
            child.reprocessAllMappings();
        }
    }

    private List<Pair<Integer, Mapping<?>>> lookupMappingAndSpecificity(final Event event, final int minSpecificity) {
        int _minSpecificity = minSpecificity;

        List<Mapping> mappings = this.eventTypeMappings.getOrDefault(event.getEventType(), Collections.emptyList());
        List<Pair<Integer, Mapping<?>>> result = new ArrayList<>();
        for (Mapping mapping : mappings) {
            if (mapping.isDisabled()) continue;

            // test if mapping has an interceptor that will block this event.
            // Interceptors return true if the interception should occur.
            boolean interceptorsApplies = testInterceptor(event, mapping.getInterceptor());
            if (interceptorsApplies) {
                continue;
            }

            int specificity = mapping.getSpecificity(event);
            if (specificity > 0 && specificity == _minSpecificity) {
                result.add(new Pair<>(specificity, mapping));
            } else if (specificity > _minSpecificity) {
                result.clear();
                result.add(new Pair<>(specificity, mapping));
                _minSpecificity = specificity;
            }
        }

        return result;
    }

    // Interceptors return true if the interception should occur.
    private boolean testInterceptor(Event e, Predicate interceptor) {
        return interceptor != null && interceptor.test(e);
    }



    /***************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * Abstract base class for all input mappings as used by the
     * {@link InputMap} class.
     *
     * @param <T> The type of {@link Event} the mapping represents.
     */
    public static abstract class Mapping<T extends Event> {

        /***********************************************************************
         *                                                                     *
         * Private fields                                                      *
         *                                                                     *
         **********************************************************************/
        private final EventType<T> eventType;
        private final EventHandler<T> eventHandler;



        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Creates a new Mapping instance.
         *
         * @param eventType The {@link EventType} that is being listened for.
         * @param eventHandler The {@link EventHandler} to fire when the mapping
         *                     is selected as the most-specific mapping.
         */
        public Mapping(final EventType<T> eventType, final EventHandler<T> eventHandler) {
            this.eventType = eventType;
            this.eventHandler = eventHandler;
        }



        /***********************************************************************
         *                                                                     *
         * Abstract methods                                                    *
         *                                                                     *
         **********************************************************************/

        /**
         * This method must be implemented by all mapping implementations such
         * that it returns an integer value representing how closely the mapping
         * matches the given {@link Event}. The higher the number, the greater
         * the match. This allows the InputMap to determine
         * which mapping is most specific, and to therefore fire the appropriate
         * mapping {@link Mapping#getEventHandler() EventHandler}.
         *
         * @param event The {@link Event} that needs to be assessed for its
         *              specificity.
         * @return An integer indicating how close of a match the mapping is to
         *          the given Event. The higher the number, the greater the match.
         */
        public abstract int getSpecificity(Event event);



        /***********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/

        // --- disabled
        /**
         * By default all mappings are enabled (so this disabled property is set
         * to false by default). In some cases it is useful to be able to disable
         * a mapping until it is applicable. In these cases, users may simply
         * toggle the disabled property until desired.
         *
         * <p>When the disabled property is true, the mapping will not be
         * considered when input events are received, even if it is the most
         * specific mapping available.</p>
         */
        private BooleanProperty disabled = new SimpleBooleanProperty(this, "disabled", false);
        public final void setDisabled(boolean value) { disabled.set(value); }
        public final boolean isDisabled() {return disabled.get(); }
        public final BooleanProperty disabledProperty() { return disabled; }


        // --- auto consume
        /**
         * By default mappings are set to 'auto consume' their specified event
         * handler. This means that the event handler will not propagate further,
         * but in some cases this is not desirable - sometimes it is preferred
         * that the event continue to 'bubble up' to parent nodes so that they
         * may also benefit from receiving this event. In these cases, it is
         * important that this autoConsume property be changed from the default
         * boolean true to instead be boolean false.
         */
        private BooleanProperty autoConsume = new SimpleBooleanProperty(this, "autoConsume", true);
        public final void setAutoConsume(boolean value) { autoConsume.set(value); }
        public final boolean isAutoConsume() {return autoConsume.get(); }
        public final BooleanProperty autoConsumeProperty() { return autoConsume; }



        /***********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /**
         * The {@link EventType} that is being listened for.
         */
        public final EventType<T> getEventType() {
            return eventType;
        }

        /**
         * The {@link EventHandler} that will be fired should this mapping be
         * the most-specific mapping for a given input, and should it not be
         * blocked by an interceptor (either at a
         * {@link InputMap#interceptorProperty() input map} level or a
         * {@link Mapping#interceptorProperty() mapping} level).
         */
        public final EventHandler<T> getEventHandler() {
            return eventHandler;
        }


        // --- interceptor
        /**
         * The role of the interceptor is to block the mapping on which it is
         * set from executing, whenever the interceptor returns true. The
         * interceptor is called every time the mapping is the best match for
         * a given input event, and is allowed to reason on the given input event
         * before returning a boolean value, where boolean true means block
         * execution, and boolean false means to allow execution.
         */
        private ObjectProperty<Predicate<? extends Event>> interceptor = new SimpleObjectProperty<>(this, "interceptor");
        public final Predicate<? extends Event> getInterceptor() {
            return interceptor.get();
        }
        public final void setInterceptor(Predicate<? extends Event> value) {
            interceptor.set(value);
        }
        public final ObjectProperty<Predicate<? extends Event>> interceptorProperty() {
            return interceptor;
        }

        /**
         *
         * @return
         */
        public Object getMappingKey() {
            return eventType;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Mapping)) return false;

            Mapping that = (Mapping) o;

            if (eventType != null ? !eventType.equals(that.getEventType()) : that.getEventType() != null)  return false;

            return true;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return eventType != null ? eventType.hashCode() : 0;
        }
    }

    /**
     * The KeyMapping class provides API to specify
     * {@link InputMap.Mapping mappings} related to key input.
     */
    public static class KeyMapping extends Mapping<KeyEvent> {

        /***********************************************************************
         *                                                                     *
         * Private fields                                                      *
         *                                                                     *
         **********************************************************************/
        private final KeyBinding keyBinding;



        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Creates a new KeyMapping instance that will fire when the given
         * {@link KeyCode} is entered into the application by the user, and this
         * will result in the given {@link EventHandler} being fired.
         *
         * @param keyCode The {@link KeyCode} to listen for.
         * @param eventHandler The {@link EventHandler} to fire when the
         *           {@link KeyCode} is observed.
         */
        public KeyMapping(final KeyCode keyCode, final EventHandler<KeyEvent> eventHandler) {
            this(new KeyBinding(keyCode), eventHandler);
        }

        /**
         * Creates a new KeyMapping instance that will fire when the given
         * {@link KeyCode} is entered into the application by the user, and this
         * will result in the given {@link EventHandler} being fired. The
         * eventType argument can be one of the following:
         *
         * <ul>
         *     <li>{@link KeyEvent#ANY}</li>
         *     <li>{@link KeyEvent#KEY_PRESSED}</li>
         *     <li>{@link KeyEvent#KEY_TYPED}</li>
         *     <li>{@link KeyEvent#KEY_RELEASED}</li>
         * </ul>
         *
         * @param keyCode The {@link KeyCode} to listen for.
         * @param eventType The type of {@link KeyEvent} to listen for.
         * @param eventHandler The {@link EventHandler} to fire when the
         *           {@link KeyCode} is observed.
         */
        public KeyMapping(final KeyCode keyCode, final EventType<KeyEvent> eventType, final EventHandler<KeyEvent> eventHandler) {
            this(new KeyBinding(keyCode, eventType), eventHandler);
        }

        /**
         * Creates a new KeyMapping instance that will fire when the given
         * {@link KeyBinding} is entered into the application by the user, and this
         * will result in the given {@link EventHandler} being fired.
         *
         * @param keyBinding The {@link KeyBinding} to listen for.
         * @param eventHandler The {@link EventHandler} to fire when the
         *           {@link KeyBinding} is observed.
         */
        public KeyMapping(KeyBinding keyBinding, final EventHandler<KeyEvent> eventHandler) {
            this(keyBinding, eventHandler, null);
        }

        /**
         * Creates a new KeyMapping instance that will fire when the given
         * {@link KeyBinding} is entered into the application by the user, and this
         * will result in the given {@link EventHandler} being fired, as long as the
         * given interceptor is not true.
         *
         * @param keyBinding The {@link KeyBinding} to listen for.
         * @param eventHandler The {@link EventHandler} to fire when the
         *           {@link KeyBinding} is observed.
         * @param interceptor A {@link Predicate} that, if true, will prevent the
         *            {@link EventHandler} from being fired.
         */
        public KeyMapping(KeyBinding keyBinding, final EventHandler<KeyEvent> eventHandler, Predicate<KeyEvent> interceptor) {
            super(keyBinding == null ? null : keyBinding.getType(), eventHandler);
            if (keyBinding == null) {
                throw new IllegalArgumentException("KeyMapping keyBinding constructor argument can not be null");
            }
            this.keyBinding = keyBinding;
            setInterceptor(interceptor);
        }



        /***********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public Object getMappingKey() {
            return keyBinding;
        }

        /** {@inheritDoc} */
        @Override public int getSpecificity(Event e) {
            if (isDisabled()) return 0;
            if (!(e instanceof KeyEvent)) return 0;
            return keyBinding.getSpecificity((KeyEvent)e);
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof KeyMapping)) return false;
            if (!super.equals(o)) return false;

            KeyMapping that = (KeyMapping) o;

            // we know keyBinding is non-null here
            return keyBinding.equals(that.keyBinding);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return Objects.hash(keyBinding);
        }
    }



    /**
     * The MouseMapping class provides API to specify
     * {@link InputMap.Mapping mappings} related to mouse input.
     */
    public static class MouseMapping extends Mapping<MouseEvent> {

        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Creates a new KeyMapping instance that will fire when the given
         * {@link KeyCode} is entered into the application by the user, and this
         * will result in the given {@link EventHandler} being fired. The
         * eventType argument can be any of the {@link MouseEvent} event types,
         * but typically it is one of the following:
         *
         * <ul>
         *     <li>{@link MouseEvent#ANY}</li>
         *     <li>{@link MouseEvent#MOUSE_PRESSED}</li>
         *     <li>{@link MouseEvent#MOUSE_CLICKED}</li>
         *     <li>{@link MouseEvent#MOUSE_RELEASED}</li>
         * </ul>
         *
         * @param eventType The type of {@link MouseEvent} to listen for.
         * @param eventHandler The {@link EventHandler} to fire when the
         *           {@link MouseEvent} is observed.
         */
        public MouseMapping(final EventType<MouseEvent> eventType, final EventHandler<MouseEvent> eventHandler) {
            super(eventType, eventHandler);
            if (eventType == null) {
                throw new IllegalArgumentException("MouseMapping eventType constructor argument can not be null");
            }
        }



        /***********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public int getSpecificity(Event e) {
            if (isDisabled()) return 0;
            if (!(e instanceof MouseEvent)) return 0;
            EventType<MouseEvent> et = getEventType();

            // FIXME naive
            int s = 0;
            if (e.getEventType() == MouseEvent.MOUSE_CLICKED && et != MouseEvent.MOUSE_CLICKED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_DRAGGED && et != MouseEvent.MOUSE_DRAGGED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_ENTERED && et != MouseEvent.MOUSE_ENTERED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_ENTERED_TARGET && et != MouseEvent.MOUSE_ENTERED_TARGET) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_EXITED && et != MouseEvent.MOUSE_EXITED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_EXITED_TARGET && et != MouseEvent.MOUSE_EXITED_TARGET) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_MOVED && et != MouseEvent.MOUSE_MOVED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED && et != MouseEvent.MOUSE_PRESSED) return 0; else s++;
            if (e.getEventType() == MouseEvent.MOUSE_RELEASED && et != MouseEvent.MOUSE_RELEASED) return 0; else s++;

            // TODO handle further checks

            return s;
        }
    }

    /**
     * Convenience class that can act as an keyboard input interceptor, either at a
     * {@link InputMap#interceptorProperty() input map} level or a
     * {@link Mapping#interceptorProperty() mapping} level.
     *
     * @see InputMap#interceptorProperty()
     * @see Mapping#interceptorProperty()
     */
    public static class KeyMappingInterceptor implements Predicate<Event> {

        private final KeyBinding keyBinding;

        /**
         * Creates a new KeyMappingInterceptor, which will block execution of
         * event handlers (either at a
         * {@link InputMap#interceptorProperty() input map} level or a
         * {@link Mapping#interceptorProperty() mapping} level), where the input
         * received is equal to the given {@link KeyBinding}.
         *
         * @param keyBinding The {@link KeyBinding} for which mapping execution
         *                   should be blocked.
         */
        public KeyMappingInterceptor(KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        /**  {@inheritDoc} */
        public boolean test(Event event) {
            if (!(event instanceof KeyEvent)) return false;
            return KeyBinding.toKeyBinding((KeyEvent)event).equals(keyBinding);
        }
    }

    /**
     * Convenience class that can act as a mouse input interceptor, either at a
     * {@link InputMap#interceptorProperty() input map} level or a
     * {@link Mapping#interceptorProperty() mapping} level.
     *
     * @see InputMap#interceptorProperty()
     * @see Mapping#interceptorProperty()
     */
    public static class MouseMappingInterceptor implements Predicate<Event> {

        private final EventType<MouseEvent> eventType;

        /**
         * Creates a new MouseMappingInterceptor, which will block execution of
         * event handlers (either at a
         * {@link InputMap#interceptorProperty() input map} level or a
         * {@link Mapping#interceptorProperty() mapping} level), where the input
         * received is equal to the given {@link EventType}.
         *
         * @param eventType The {@link EventType} for which mapping execution
         *                  should be blocked (typically one of
         *                  {@link MouseEvent#MOUSE_PRESSED},
         *                  {@link MouseEvent#MOUSE_CLICKED}, or
         *                  {@link MouseEvent#MOUSE_RELEASED}).
         */
        public MouseMappingInterceptor(EventType<MouseEvent> eventType) {
            this.eventType = eventType;
        }

        /**  {@inheritDoc} */
        public boolean test(Event event) {
            if (!(event instanceof MouseEvent)) return false;
            return event.getEventType() == this.eventType;
        }
    }
}
