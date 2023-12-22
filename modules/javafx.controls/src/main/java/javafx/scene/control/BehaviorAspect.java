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
import javafx.scene.input.KeyEvent;
import javafx.util.Subscription;

/**
 * An immutable aspect of a {@link BehaviorConfiguration}.
 *
 * @param <N> the type of {@link Control} the aspect is suited for
 * @param <C> the type of controller the aspect requires
 */
public class BehaviorAspect<N extends Control, C> {
    private final Class<C> controllerClass;
    private final Function<N, C> controllerFactory;
    private final List<Command<C, N>> commands;
    private final List<KeyHandler> keyHandlers;

    /**
     * Constructs a new builder for a {@link BehaviorAspect}.
     *
     * @param <N> the type of {@link Control} the aspect is suited for
     * @param <C> the type of controller the aspect requires
     * @param controllerClass the type of the controller produced by the given factory, cannot be {@code null}
     * @param controllerFactory a factory for a controller of type {@code C}, cannot be {@code null}
     * @return a builder, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public static <N extends Control, C> Builder<N, C> builder(Class<C> controllerClass, Function<N, C> controllerFactory) {
        return new Builder<>(controllerClass, controllerFactory);
    }

    private BehaviorAspect(Class<C> controllerClass, Function<N, C> controllerFactory, List<Command<C, N>> commands, List<KeyHandler> keyHandlers) {
        this.controllerClass = Objects.requireNonNull(controllerClass);
        this.controllerFactory = Objects.requireNonNull(controllerFactory);
        this.commands = List.copyOf(commands);
        this.keyHandlers = List.copyOf(keyHandlers);
    }

    Class<C> getControllerClass() {
        return controllerClass;
    }

    Function<N, C> getControllerFactory() {
        return controllerFactory;
    }

    Subscription install(N control, ControllerCache<N> controllerCache) {
        Subscription subscription = Subscription.EMPTY;
        Function<N, C> controllerProvider = n -> controllerCache.get(n, controllerClass);

        /*
         * Execute commands in order they were added:
         */

        for (Command<C, N> command : commands) {
            command.execute(control, controllerProvider);
        }

        /*
         * Install key handlers (as a single event handler) on the control:
         */

        if (!keyHandlers.isEmpty()) {
            EventHandler<? super KeyEvent> eventHandler = e -> {
                KeyState keyState = new KeyState(e.getCode(), e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown());

                for (KeyHandler keyHandler : keyHandlers) {
                    if (keyHandler.trigger(keyState, controllerProvider.apply(control))) {
                        e.consume();
                    }
                }
            };

            control.addEventHandler(KeyEvent.KEY_PRESSED, eventHandler);

            subscription = subscription.and(() -> control.removeEventHandler(KeyEvent.KEY_PRESSED, eventHandler));
        }

        return subscription;
    }

    interface Command<C, N extends Control> {
        Subscription execute(N control, Function<N, C> controllerProvider);
    }

    /**
     * A builder for {@link BehaviorAspect}s.
     *
     * @param <N> the type of {@link Control} the aspect is suited for
     * @param <C> the type of controller the aspect requires
     */
    public static class Builder<N extends Control, C> {
        private final List<Command<C, N>> commands = new ArrayList<>();
        private final List<KeyHandler> keyHandlers = new ArrayList<>();
        private final Class<C> controllerClass;
        private final Function<N, C> controllerFactory;

        private Builder(Class<C> controllerClass, Function<N, C> controllerFactory) {
            this.controllerClass = controllerClass;
            this.controllerFactory = controllerFactory;
        }

        /**
         * Creates a new {@link BehaviorAspect}.
         *
         * @return a new aspect, never {@code null}
         */
        public BehaviorAspect<N, C> build() {
            return new BehaviorAspect<>(controllerClass, controllerFactory, commands, keyHandlers);
        }

        /**
         * Registers an event handler as part of this aspect.
         *
         * @param <E> the type of event to be handled
         * @param eventType an event type, cannot be {@code null}
         * @param eventHandler an event handler, cannot be {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException when any argument is {@code null}
         */
        public <E extends Event> Builder<N, C> registerEventHandler(EventType<E> eventType, BiConsumer<C, ? super E> eventHandler) {
            this.commands.add(new AddEventHandlerCommand<>(
                Objects.requireNonNull(eventType, "eventType"),
                Objects.requireNonNull(eventHandler, "eventHandler")
            ));

            return this;
        }

        /**
         * Registers a property listener as part of this aspect.
         *
         * @param <T> the type of the values of the property
         * @param supplier a property supplier, cannot be {@code null}
         * @param listener a property listener, cannot be {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException when any argument is {@code null}
         */
        public <T> Builder<N, C> registerPropertyListener(Function<N, ObservableValue<T>> supplier, BiConsumer<C, T> listener) {
            this.commands.add(new AddListenerCommand<>(
                Objects.requireNonNull(supplier, "supplier"),
                Objects.requireNonNull(listener, "listener")
            ));

            return this;
        }

        /**
         * Registers a {@link KeyHandler}.
         *
         * @param keyHandler a key handler, cannot be {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException when any argument is {@code null}
         */
        public Builder<N, C> registerKeyHandler(KeyHandler keyHandler) {
            this.keyHandlers.add(Objects.requireNonNull(keyHandler, "keyHandler"));

            return this;
        }

        record AddEventHandlerCommand<C, N extends Control, E extends Event>(EventType<E> eventType, BiConsumer<C, ? super E> eventHandler) implements Command<C, N> {
            @Override
            public Subscription execute(N control, Function<N, C> controllerProvider) {
                EventHandler<E> handler = e -> eventHandler.accept(controllerProvider.apply(control), e);

                control.addEventHandler(eventType, handler);

                return () -> control.removeEventHandler(eventType, handler);
            }
        }

        record AddListenerCommand<C, N extends Control, T>(Function<N, ObservableValue<T>> propertySupplier, BiConsumer<C, T> listener) implements Command<C, N> {
            @Override
            public Subscription execute(N control, Function<N, C> controllerProvider) {
                return propertySupplier.apply(control)
                        .subscribe(value -> listener.accept(controllerProvider.apply(control), value));
            }
        }
    }

    static final class ControllerCache<N> {
        private final Map<Class<?>, Function<N, ?>> controllerFactories;
        private final Map<Class<?>, Object> controllers = new HashMap<>();

        public ControllerCache(Map<Class<?>, Function<N, ?>> controllerFactories) {
            this.controllerFactories = controllerFactories;
        }

        <C> C get(N control, Class<C> controllerClass) {
            Object controller = controllers.get(controllerClass);

            if (controller == null) {
                controller = controllerFactories.get(controllerClass).apply(control);

                /*
                 * Attach the created controller to all compatible classes:
                 */

                for (Class<?> cls : controllerFactories.keySet()) {
                    if (cls.isInstance(controller)) {
                        controllers.put(cls, controller);
                    }
                }
            }

            @SuppressWarnings("unchecked")
            C castedController = (C) controller;  // safe cast, everything is key'd on controller class

            return castedController;
        }
    }
}