package javafx.scene.control.behavior;

import java.util.function.Function;

import javafx.scene.control.HandlerRegistry;
import javafx.scene.control.Control;

/**
 * Allows registration of controllers as part of a behavior installation. Controllers
 * hold state and functions that could be inherited by other more specific controllers.
 * Once a controller is registered, a {@link HandlerRegistry} is returned for registration
 * of handlers the controller requires to exhibit the desired behavior.
 *
 * @param <N> the control type
 */
public interface ControllerRegistry<N extends Control> {

    /**
     * Registers a controller's type and factory. The controller is lazily instantiated
     * the first time any of its handlers is triggered. If no handlers were registered,
     * the controller will never be created.
     *
     * @param <C> the controller type
     * @param controllerClass the controller class, cannot be {@code null}
     * @param controllerFactory a controller factory, cannot be {@code null}
     * @return a registry for handlers associated with this controller, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    <C> HandlerRegistry<N, C> register(Class<C> controllerClass, Function<N, C> controllerFactory);
}
