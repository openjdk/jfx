package javafx.scene.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.behavior.Behavior;
import javafx.scene.control.behavior.ControllerRegistry;
import javafx.scene.input.KeyEvent;
import javafx.util.Subscription;

class DefaultControllerRegistry<N extends Control> implements ControllerRegistry<N> {
    private final Map<Class<?>, Function<N, ?>> controllerFactories = new HashMap<>();
    private final Map<Function<N, ?>, Class<?>> implementingClasses = new HashMap<>();
    private final List<Installer<N, ?>> installers = new ArrayList<>();

    private boolean wasInstalled;

    static <N extends Control> Subscription install(Behavior<? super N> behavior, N control) {
        DefaultControllerRegistry<N> registry = new DefaultControllerRegistry<>();

        behavior.configure(registry);

        return registry.install(control);
    }

    @Override
    public <C> HandlerRegistry<N, C> register(Class<C> controllerClass, Function<N, C> controllerFactory) {

        /*
         * Controller classes are de-duplicated. If a controller class can be assigned
         * to another controller class, only the most specific class is kept and shared
         * for both [1]. If a controller class has the same super type as another controller
         * class, but cannot be assigned (their hierarchies diverge) an exception is
         * thrown [2]. If a controller class does not share a super type with another
         * controller class, it is instantiated separately [3].
         *
         * [1] A <- B <- C : only controller class C is used and shared for A, B and C
         * [2] A <- B + A <- C : exception, B and C both inherit from A but diverge
         * [3] A + B <- C : result in two controllers, A and C, where C is shared for B
         */

        /*
         * Below loop checks if an existing factory needs replacement or can be reused
         * for the given new controller class. Existing factories are replaced if the
         * new controller class is a sub type, and reused if the new controller class
         * is the same or a super type.
         */

        for (Class<?> type = controllerClass; type != null; type = type.getSuperclass()) {
            if (controllerFactories.containsKey(type)) {
                if (type == controllerClass) {
                    break;  // exact same factory already present
                }

                /*
                 * Another factory is present that shares part of the hierarchy of the new controller class:
                 */

                Function<N, ?> existingFactory = controllerFactories.get(type);
                Class<?> existingControllerClass = implementingClasses.get(existingFactory);
                boolean isSameOrSuperType = controllerClass.isAssignableFrom(existingControllerClass);
                boolean isSameOrSubType = existingControllerClass.isAssignableFrom(controllerClass);

                if (!isSameOrSuperType && !isSameOrSubType) {
                    throw new IllegalStateException("Divergent hierarchies detected between " + controllerClass + " and " + existingControllerClass);
                }

                /*
                 * Hierarchies are not divergent. If the new controller class is a sub type, then replace
                 * keys with the new controller factory, otherwise use the existing factory.
                 */

                controllerFactories.put(type, isSameOrSubType ? controllerFactory : existingFactory);
                implementingClasses.remove(existingFactory);
                implementingClasses.put(isSameOrSubType ? controllerFactory : existingFactory, isSameOrSubType ? controllerClass : existingControllerClass);
            }
        }

        /*
         * If after the above loop the controller class is not entered into the controller factories map,
         * then it wasn't sharable with any existing controllers. Add it as an independent controller:
         */

        if (!controllerFactories.containsKey(controllerClass)) {
            controllerFactories.put(controllerClass, controllerFactory);
            implementingClasses.put(controllerFactory, controllerClass);
        }

        /*
         * All looks okay, create and return an installer:
         */

        Installer<N, C> installer = new Installer<>(controllerClass);

        installers.add(installer);

        return installer;
    }

    private Subscription install(N control) {
        if (wasInstalled) {
            throw new IllegalStateException("Already installed");
        }

        wasInstalled = true;

        Subscription subscription = Subscription.EMPTY;
        ControllerProvider<N> controllerProvider = new ControllerProvider<>(controllerFactories, implementingClasses);

        /*
         * Call installers:
         */

        for (Installer<N, ?> installer : installers) {
            subscription = subscription.and(installer.install(control, controllerProvider));
        }

        return subscription;
    }

    static final class ControllerProvider<N> {
        private final Map<Class<?>, Function<N, ?>> controllerFactories;
        private final Map<Function<N, ?>, Class<?>> implementingClasses;
        private final Map<Class<?>, Object> controllers = new HashMap<>();

        public ControllerProvider(Map<Class<?>, Function<N, ?>> controllerFactories, Map<Function<N, ?>, Class<?>> implementingClasses) {
            this.controllerFactories = controllerFactories;
            this.implementingClasses = implementingClasses;
        }

        <C> C get(N control, Class<C> controllerClass) {
            Object controller = controllers.get(controllerClass);

            if (controller == null) {
                Function<N, ?> factory = controllerFactories.get(controllerClass);
                Class<?> implementingClass = implementingClasses.get(factory);

                controller = controllers.get(implementingClass);

                if (controller == null) {
                    controller = factory.apply(control);
                    controllers.put(implementingClass, controller);
                }

                controllers.put(controllerClass, controller);
            }

            return (C) controller;
        }
    }

    static final class Installer<N extends Control, C> implements HandlerRegistry<N, C> {
        private final List<EventHandlerDefinition<?, ?>> eventHandlerDefinitions = new ArrayList<>();
        private final List<PropertyListenerDefinition<?, ?, ?>> propertyListenerDefinitions = new ArrayList<>();
        private final List<KeyHandler> keyHandlers = new ArrayList<>();

        private Class<C> controllerClass;

        private Installer(Class<C> controllerClass) {
            this.controllerClass = Objects.requireNonNull(controllerClass);
        }

        private <E extends Event> Subscription install(N control, ControllerProvider<N> controllerProvider) {
            ensureNotInstalled();

            Subscription subscription = Subscription.EMPTY;

            /*
             * Install event handlers on the control:
             */

            for (EventHandlerDefinition<?, ?> rawDefinition : eventHandlerDefinitions) {
                @SuppressWarnings("unchecked")
                EventHandlerDefinition<C, E> mapping = (EventHandlerDefinition<C, E>) rawDefinition;
                EventHandler<E> handler = e -> mapping.eventHandler.accept(controllerProvider.get(control, controllerClass), e);
                EventType<E> eventType = mapping.eventType;

                control.addEventHandler(eventType, handler);

                subscription = subscription.and(() -> control.removeEventHandler(eventType, handler));
            }

            /*
             * Install property listeners on the control:
             */

            for (PropertyListenerDefinition<?, ?, ?> rawDefinition : propertyListenerDefinitions) {
                @SuppressWarnings("unchecked")
                PropertyListenerDefinition<C, N, Object> definition = (PropertyListenerDefinition<C, N, Object>) rawDefinition;

                subscription = subscription.and(
                    definition.propertySupplier.apply(control)
                        .subscribe(value -> definition.listener.accept(controllerProvider.get(control, controllerClass), value))
                );
            }

            /*
             * Install key handlers (as a single event handler) on the control:
             */

            if (!keyHandlers.isEmpty()) {
                EventHandler<? super KeyEvent> eventHandler = e -> {
                    KeyState keyState = new KeyState(e.getCode(), e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown());

                    for (KeyHandler keyHandler : keyHandlers) {
                        if (keyHandler.trigger(keyState, controllerProvider.get(control, controllerClass))) {
                            e.consume();
                        }
                    }
                };

                control.addEventHandler(KeyEvent.KEY_PRESSED, eventHandler);

                subscription = subscription.and(() -> control.removeEventHandler(KeyEvent.KEY_PRESSED, eventHandler));
            }

            this.controllerClass = null;  // prevent double installations

            return subscription;
        }

        @Override
        public <E extends Event> HandlerRegistry<N, C> registerEventHandler(EventType<E> eventType, BiConsumer<C, ? super E> eventHandler) {
            ensureNotInstalled();

            this.eventHandlerDefinitions.add(new EventHandlerDefinition<>(
                Objects.requireNonNull(eventType, "eventType"),
                Objects.requireNonNull(eventHandler, "eventHandler")
            ));

            return this;
        }

        @Override
        public <T> HandlerRegistry<N, C> registerPropertyListener(Function<N, ObservableValue<T>> supplier, BiConsumer<C, T> listener) {
            ensureNotInstalled();

            this.propertyListenerDefinitions.add(new PropertyListenerDefinition<>(
                Objects.requireNonNull(supplier, "supplier"),
                Objects.requireNonNull(listener, "listener")
            ));

            return this;
        }

        @Override
        public HandlerRegistry<N, C> registerKeyHandler(KeyHandler keyHandler) {
            ensureNotInstalled();

            this.keyHandlers.add(Objects.requireNonNull(keyHandler, "keyHandler"));

            return this;
        }

        private void ensureNotInstalled() {
            if (controllerClass == null) {
                throw new IllegalStateException("Already installed");
            }
        }

        record EventHandlerDefinition<S, T extends Event>(EventType<T> eventType, BiConsumer<S, ? super T> eventHandler) {}
        record PropertyListenerDefinition<S, C, V>(Function<C, ObservableValue<V>> propertySupplier, BiConsumer<S, V> listener) {}
    }
}


