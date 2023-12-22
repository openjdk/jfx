package javafx.scene.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javafx.util.Subscription;

/**
 * A configuration which describes a behavior. A behavior configuration consists
 * of one or more {@link BehaviorAspect} which together form a complete behavior
 * for a control.
 *
 * @param <N> the type of {@link Control} the configuration is suited for
 */
public class BehaviorConfiguration<N extends Control> {
    private final Map<Class<?>, Function<N, ?>> controllerFactories = new HashMap<>();
    private final List<BehaviorAspect<N, ?>> aspects;

    /**
     * Creates a builder for a {@link BehaviorConfiguration}.
     *
     * @param <N> the type of control the behavior is suited for
     * @return a builder, never {@code null}
     */
    public static <N extends Control> Builder<N> builder() {
        return new Builder<>();
    }

    /**
     * Constructs a new behavior configuration.
     *
     * @param aspects the aspects to include in the configuration, cannot be {@code null} or contain {@code null}s
     * @throws NullPointerException when any argument or element is {@code null}
     */
    @SafeVarargs
    public BehaviorConfiguration(BehaviorAspect<N, ?>... aspects) {
        this(Arrays.asList(aspects));
    }

    /**
     * Constructs a new behavior configuration.
     *
     * @param aspects the aspects to include in the configuration, cannot be {@code null} or contain {@code null}s
     * @throws NullPointerException when any argument or element is {@code null}
     */
    public BehaviorConfiguration(List<BehaviorAspect<N, ?>> aspects) {
        this.aspects = List.copyOf(aspects);

        for (BehaviorAspect<N, ?> aspect : this.aspects) {
            controllerFactories.put(aspect.getControllerClass(), aspect.getControllerFactory());
        }

        /*
         * Only the most specific factories associated with controller classes are of use.
         * If a controller class can be assigned to another controller class, only the factory
         * of the sub type is needed, and only one instance of the controller is created.
         *
         * It is an error to have two (or more) classes that are sub types of another
         * controller class, but the sub types are not in the same inheritance hierarchy.
         *
         * [1] A <- B <- C : only controller class factory C is used and its result shared for A, B and C
         * [2] A <- B + A <- C : exception, B and C both inherit from A but diverge
         * [3] A + B <- C : results in two controllers, A and C, where C is shared for B and C
         */

        ensureNoMultipleInheritance(controllerFactories.keySet());

        /*
         * Ensure that for each controller class, the most specific
         * factory is used:
         */

        for (Class<?> cls1 : controllerFactories.keySet()) {
            Class<?> mostSpecificClass = cls1;

            for (Class<?> cls2 : controllerFactories.keySet()) {
                boolean isSubType = mostSpecificClass.isAssignableFrom(cls2);

                if (isSubType) {
                    mostSpecificClass = cls2;
                }
            }


            controllerFactories.put(cls1, controllerFactories.get(mostSpecificClass));
        }
    }

    /**
     * Gets the controller factory associated with the given controller type.
     *
     * @param <C> a controller type
     * @param controllerClass the controller factory type to find, cannot be {@code null}
     * @return a controller factory function, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    @SuppressWarnings("unchecked")
    public <C> Function<N, C> getFactory(Class<C> controllerClass) {
        return (Function<N, C>) controllerFactories.get(Objects.requireNonNull(controllerClass, "controllerClass"));
    }

    // Package private, only intended for use by Control; making this public would allow installation
    // of behaviors directly, circumventing the behavior property(!)
    Subscription install(N control) {
        Subscription subscription = Subscription.EMPTY;
        BehaviorAspect.ControllerCache<N> controllerProvider = new BehaviorAspect.ControllerCache<>(controllerFactories);

        /*
         * Call installers:
         */

        for (BehaviorAspect<N, ?> aspect : aspects) {
            subscription = subscription.and(aspect.install(control, controllerProvider));
        }

        return subscription;
    }

    /**
     * A builder for behavior configurations.
     *
     * @param <N> the type of {@link Control} the configuration builder is suited for
     */
    public static class Builder<N extends Control> {
        private final List<BehaviorAspect<N, ?>> aspects = new ArrayList<>();

        private Builder() {
        }

        /**
         * Builds a new behavior configuration.
         *
         * @return a new configuration, never {@code null}
         */
        public BehaviorConfiguration<N> build() {
            return new BehaviorConfiguration<>(aspects);
        }

        /**
         * Includes all the aspects of another configuration.
         *
         * @param configuration another configuration, cannot be {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException when any argument is {@code null}
         */
        public Builder<N> include(BehaviorConfiguration<? super N> configuration) {
            for (BehaviorAspect<? super N, ?> aspect : configuration.aspects) {
                add(aspect);
            }

            return this;
        }

        /**
         * Adds a behavior aspect to this builder.
         *
         * @param <C> the type of controller the aspect requires
         * @param aspect a behavior aspect to add, cannot be {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException when any argument is {@code null}
         */
        public <C> Builder<N> add(BehaviorAspect<? super N, C> aspect) {
            @SuppressWarnings("unchecked")
            BehaviorAspect<N, C> castedAspect = (BehaviorAspect<N, C>) Objects.requireNonNull(aspect, "aspect");

            aspects.add(castedAspect);

            return this;
        }
    }

    /*
     * Checks if the given classes has no class which has sub types where the
     * sub types are not in a single inheritance hierarchy.
     */
    private static void ensureNoMultipleInheritance(Collection<Class<?>> classes) {
        for (Class<?> cls1 : classes) {
            List<Class<?>> subtypes = new ArrayList<>();

            for (Class<?> cls2 : classes) {
                if (!cls1.equals(cls2) && cls1.isAssignableFrom(cls2)) {
                    subtypes.add(cls2);
                }
            }

            if (!allInSameHierarchy(subtypes)) {
                subtypes.sort(Comparator.comparing(Object::toString));  // ensure fixed order for consistent (test) results

                throw new IllegalStateException(cls1 + " subtypes are not all in same hierarchy: " + subtypes);
            }
        }
    }

    private static boolean allInSameHierarchy(List<Class<?>> classes) {
        if (classes.isEmpty()) {
            return true;
        }

        outer:
        for (Class<?> cls : classes) {
            for (Class<?> otherCls : classes) {
                boolean isSuperType = otherCls.isAssignableFrom(cls);

                if (!isSuperType) {
                    continue outer;
                }
            }

            return true;
        }

        return false;
    }
}

