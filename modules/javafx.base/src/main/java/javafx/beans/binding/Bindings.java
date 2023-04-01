/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.binding;

import java.lang.ref.WeakReference;
import java.text.Format;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.util.StringConverter;
import com.sun.javafx.binding.BidirectionalBinding;
import com.sun.javafx.binding.BidirectionalContentBinding;
import com.sun.javafx.binding.ContentBinding;
import com.sun.javafx.binding.DoubleConstant;
import com.sun.javafx.binding.FloatConstant;
import com.sun.javafx.binding.IntegerConstant;
import com.sun.javafx.binding.Logging;
import com.sun.javafx.binding.LongConstant;
import com.sun.javafx.binding.ObjectConstant;
import com.sun.javafx.binding.SelectBinding;
import com.sun.javafx.binding.StringConstant;
import com.sun.javafx.binding.StringFormatter;
import com.sun.javafx.collections.ImmutableObservableList;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;

/**
 * Bindings is a helper class with a lot of utility functions to create simple
 * bindings.
 * <p>
 * Usually there are two possibilities to define the same operation: the Fluent
 * API and the the factory methods in this class. This allows a developer to
 * define complex expression in a way that is most easy to understand. For
 * instance the expression {@code result = a*b + c*d} can be defined using only
 * the Fluent API:
 * <p>
 * {@code DoubleBinding result = a.multiply(b).add(c.multiply(d));}
 * <p>
 * Or using only factory methods in Bindings:
 * <p>
 * {@code NumberBinding result = add (multiply(a, b), multiply(c,d));}
 * <p>
 * Or mixing both possibilities:
 * <p>
 * {@code NumberBinding result = add (a.multiply(b), c.multiply(d));}
 * <p>
 * The main difference between using the Fluent API and using the factory
 * methods in this class is that the Fluent API requires that at least one of
 * the operands is an Expression (see {@link javafx.beans.binding}). (Every
 * Expression contains a static method that generates an Expression from an
 * {@link javafx.beans.value.ObservableValue}.)
 * <p>
 * Also if you watched closely, you might have noticed that the return type of
 * the Fluent API is different in the examples above. In a lot of cases the
 * Fluent API allows to be more specific about the returned type (see
 * {@link javafx.beans.binding.NumberExpression} for more details about implicit
 * casting.
 * </p>
 * <p><a id="DeployAppAsModule"></a><b>Deploying an Application as a Module</b></p>
 * <p>
 * If any class used in a select-binding (see the various {@code select*}
 * methods) is in a named module, then it must be reflectively accessible to the
 * {@code javafx.base} module.
 * A class is reflectively accessible if the module
 * {@link Module#isOpen(String,Module) opens} the containing package to at
 * least the {@code javafx.base} module.
 * </p>
 * <p>
 * For example, if {@code com.foo.MyClass} is in the {@code foo.app} module,
 * the {@code module-info.java} might
 * look like this:
 * </p>
 *
<pre>{@code module foo.app {
    opens com.foo to javafx.base;
}}</pre>
 *
 * <p>
 * Alternatively, a class is reflectively accessible if the module
 * {@link Module#isExported(String) exports} the containing package
 * unconditionally.
 * </p>
 *
 * @see Binding
 * @see NumberBinding
 *
 *
 * @since JavaFX 2.0
 */
public final class Bindings {

    private Bindings() {
    }

    // =================================================================================================================
    // Helper functions to create custom bindings

    /**
     * Helper function to create a custom {@link BooleanBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static BooleanBinding createBooleanBinding(final Callable<Boolean> func, final Observable... dependencies) {
        return new BooleanBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return false;
                }
            }

            /**
             * Calls {@link BooleanBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link DoubleBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static DoubleBinding createDoubleBinding(final Callable<Double> func, final Observable... dependencies) {
        return new DoubleBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected double computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return 0.0;
                }
            }

            /**
             * Calls {@link DoubleBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link FloatBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static FloatBinding createFloatBinding(final Callable<Float> func, final Observable... dependencies) {
        return new FloatBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected float computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return 0.0f;
                }
            }

            /**
             * Calls {@link FloatBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link IntegerBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static IntegerBinding createIntegerBinding(final Callable<Integer> func, final Observable... dependencies) {
        return new IntegerBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected int computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return 0;
                }
            }

            /**
             * Calls {@link IntegerBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link LongBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static LongBinding createLongBinding(final Callable<Long> func, final Observable... dependencies) {
        return new LongBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected long computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return 0L;
                }
            }

            /**
             * Calls {@link LongBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link ObjectBinding}.
     *
     * @param <T> the type of the bound {@code Object}
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static <T> ObjectBinding<T> createObjectBinding(final Callable<T> func, final Observable... dependencies) {
        return new ObjectBinding<>() {
            {
                bind(dependencies);
            }

            @Override
            protected T computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return null;
                }
            }

            /**
             * Calls {@link ObjectBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Helper function to create a custom {@link StringBinding}.
     *
     * @param func The function that calculates the value of this binding
     * @param dependencies The dependencies of this binding
     * @return The generated binding
     * @since JavaFX 2.1
     */
    public static StringBinding createStringBinding(final Callable<String> func, final Observable... dependencies) {
        return new StringBinding() {
            {
                bind(dependencies);
            }

            @Override
            protected String computeValue() {
                try {
                    return func.call();
                } catch (Exception e) {
                    Logging.getLogger().warning("Exception while evaluating binding", e);
                    return "";
                }
            }

            /**
             * Calls {@link StringBinding#unbind(Observable...)}.
             */
            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            /**
             * Returns an immutable list of the dependencies of this binding.
             */
            @Override
            public ObservableList<?> getDependencies() {
                return  ((dependencies == null) || (dependencies.length == 0))?
                            FXCollections.emptyObservableList()
                        : (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }


    // =================================================================================================================
    // Select Bindings

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code null} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being the right type etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     * <p>
     * Since 19, it is recommended to use {@link ObservableValue#flatMap(java.util.function.Function)}
     * to select a nested member of an {@link ObservableValue}.
     * </p>
     *
     * @param <T> the type of the wrapped {@code Object}
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link ObjectBinding}
     * @see ObservableValue#flatMap(java.util.function.Function)
     */
    public static <T> ObjectBinding<T> select(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsObject<>(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0.0} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link DoubleBinding}
     */
    public static DoubleBinding selectDouble(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsDouble(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0.0f} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link FloatBinding}
     */
    public static FloatBinding selectFloat(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsFloat(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link IntegerBinding}
     */
    public static IntegerBinding selectInteger(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsInteger(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0L} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link LongBinding}
     */
    public static LongBinding selectLong(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsLong(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code false} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code boolean} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link ObjectBinding}
     */
    public static BooleanBinding selectBoolean(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsBoolean(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code ""} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code String} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     * <p>
     * Note: since 8.0, JavaBeans properties are supported and might be in the chain.
     * </p>
     *
     * @param root
     *            The root {@link javafx.beans.value.ObservableValue}
     * @param steps
     *            The property names to reach the final property
     * @return the created {@link ObjectBinding}
     */
    public static StringBinding selectString(ObservableValue<?> root, String... steps) {
        return new SelectBinding.AsString(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code null} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being the right type etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #select(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param <T> the type of the wrapped {@code Object}
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link ObjectBinding}
     * @since JavaFX 8.0
     */
    public static <T> ObjectBinding<T> select(Object root, String... steps) {
        return new SelectBinding.AsObject<>(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0.0} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectDouble(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link DoubleBinding}
     * @since JavaFX 8.0
     */
    public static DoubleBinding selectDouble(Object root, String... steps) {
        return new SelectBinding.AsDouble(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0.0f} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectFloat(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link FloatBinding}
     * @since JavaFX 8.0
     */
    public static FloatBinding selectFloat(Object root, String... steps) {
        return new SelectBinding.AsFloat(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectInteger(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link IntegerBinding}
     * @since JavaFX 8.0
     */
    public static IntegerBinding selectInteger(Object root, String... steps) {
        return new SelectBinding.AsInteger(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code 0L} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code Number} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectLong(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link LongBinding}
     * @since JavaFX 8.0
     */
    public static LongBinding selectLong(Object root, String... steps) {
        return new SelectBinding.AsLong(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code false} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code boolean} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectBoolean(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link ObjectBinding}
     * @since JavaFX 8.0
     */
    public static BooleanBinding selectBoolean(Object root, String... steps) {
        return new SelectBinding.AsBoolean(root, steps);
    }

    /**
     * Creates a binding used to get a member, such as {@code a.b.c}. The value
     * of the binding will be {@code c}, or {@code ""} if {@code c} could not
     * be reached (due to {@code b} not having a {@code c} property,
     * {@code b} being {@code null}, or {@code c} not being a {@code String} etc.).
     * <p>
     * All classes and properties used in a select-binding have to be
     * declared public.
     * Additionally, if any class is in a named module, then it must be
     * reflectively accessible to the {@code javafx.base} module (see
     * <a href="#DeployAppAsModule">Deploying an Application as a Module</a>).
     * </p>
     *
     * <p>
     * If root has JavaFX properties, this call is equivalent to {@link #selectString(javafx.beans.value.ObservableValue, java.lang.String[])},
     * with the {@code root} and {@code step[0]} being substituted with the relevant property object.
     * </p>
     *
     * @param root
     *            The root bean.
     * @param steps
     *            The property names to reach the final property. The first step
     *            must be specified as it marks the property of the root bean.
     * @return the created {@link ObjectBinding}
     * @since JavaFX 8.0
     */
    public static StringBinding selectString(Object root, String... steps) {
        return new SelectBinding.AsString(root, steps);
    }

    /**
     * Creates a binding that calculates the result of a ternary expression. See
     * the description of class {@link When} for details.
     *
     * @see When
     *
     * @param condition
     *            the condition of the ternary expression
     * @return an intermediate class to build the complete binding
     */
    public static When when(final ObservableBooleanValue condition) {
        return new When(condition);
    }

    // =================================================================================================================
    // Bidirectional Bindings

    /**
     * Generates a bidirectional binding (or "bind with inverse") between two
     * instances of {@link javafx.beans.property.Property}.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * A bidirectional binding can be removed with
     * {@link #unbindBidirectional(Property, Property)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param <T>
     *            the types of the properties
     * @param property1
     *            the first {@code Property<T>}
     * @param property2
     *            the second {@code Property<T>}
     * @throws NullPointerException
     *            if one of the properties is {@code null}
     * @throws IllegalArgumentException
     *            if both properties are equal
     */
    public static <T> void bindBidirectional(Property<T> property1, Property<T> property2) {
        BidirectionalBinding.bind(property1, property2);
    }

    /**
     * Delete a bidirectional binding that was previously defined with
     * {@link #bindBidirectional(Property, Property)}.
     *
     * @param <T>
     *            the types of the properties
     * @param property1
     *            the first {@code Property<T>}
     * @param property2
     *            the second {@code Property<T>}
     * @throws NullPointerException
     *            if one of the properties is {@code null}
     * @throws IllegalArgumentException
     *            if both properties are equal
     */
    public static <T> void unbindBidirectional(Property<T> property1, Property<T> property2) {
        BidirectionalBinding.unbind(property1, property2);
    }

    /**
     * Delete a bidirectional binding that was previously defined with
     * {@link #bindBidirectional(Property, Property)} or
     * {@link #bindBidirectional(javafx.beans.property.Property, javafx.beans.property.Property, java.text.Format)}.
     *
     * @param property1
     *            the first {@code Property<T>}
     * @param property2
     *            the second {@code Property<T>}
     * @throws NullPointerException
     *            if one of the properties is {@code null}
     * @throws IllegalArgumentException
     *            if both properties are equal
     * @since JavaFX 2.1
     */
    public static void unbindBidirectional(Object property1, Object property2) {
        BidirectionalBinding.unbind(property1, property2);
    }

    /**
     * Generates a bidirectional binding (or "bind with inverse") between a
     * {@code String}-{@link javafx.beans.property.Property} and another {@code Property}
     * using the specified {@code Format} for conversion.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * A bidirectional binding can be removed with
     * {@link #unbindBidirectional(Object, Object)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param stringProperty
     *            the {@code String} {@code Property}
     * @param otherProperty
     *            the other (non-{@code String}) {@code Property}
     * @param format
     *            the {@code Format} used to convert between the properties
     * @throws NullPointerException
     *            if one of the properties or the {@code format} is {@code null}
     * @throws IllegalArgumentException
     *            if both properties are equal
     * @since JavaFX 2.1
     */
    public  static void bindBidirectional(Property<String> stringProperty, Property<?> otherProperty, Format format) {
        BidirectionalBinding.bind(stringProperty, otherProperty, format);
    }

    /**
     * Generates a bidirectional binding (or "bind with inverse") between a
     * {@code String}-{@link javafx.beans.property.Property} and another {@code Property}
     * using the specified {@link javafx.util.StringConverter} for conversion.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * A bidirectional binding can be removed with
     * {@link #unbindBidirectional(Object, Object)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param <T> the type of the wrapped {@code Object}
     * @param stringProperty
     *            the {@code String} {@code Property}
     * @param otherProperty
     *            the other (non-{@code String}) {@code Property}
     * @param converter
     *            the {@code StringConverter} used to convert between the properties
     * @throws NullPointerException
     *            if one of the properties or the {@code converter} is {@code null}
     * @throws IllegalArgumentException
     *            if both properties are equal
     * @since JavaFX 2.1
     */
    public static <T> void bindBidirectional(Property<String> stringProperty, Property<T> otherProperty, StringConverter<T> converter) {
        BidirectionalBinding.bind(stringProperty, otherProperty, converter);
    }

    /**
     * Generates a bidirectional binding (or "bind with inverse") between two
     * instances of {@link javafx.collections.ObservableList}.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * Only the content of the two lists is synchronized, which means that
     * both lists are different, but they contain the same elements.
     * <p>
     * A bidirectional content-binding can be removed with
     * {@link #unbindContentBidirectional(Object, Object)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param <E>
     *            the type of the list elements
     * @param list1
     *            the first {@code ObservableList<E>}
     * @param list2
     *            the second {@code ObservableList<E>}
     * @throws NullPointerException
     *            if one of the lists is {@code null}
     * @throws IllegalArgumentException
     *            if {@code list1} == {@code list2}
     * @since JavaFX 2.1
     */
    public static <E> void bindContentBidirectional(ObservableList<E> list1, ObservableList<E> list2) {
        BidirectionalContentBinding.bind(list1, list2);
    }

    /**
     * Generates a bidirectional binding (or "bind with inverse") between two
     * instances of {@link javafx.collections.ObservableSet}.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * Only the content of the two sets is synchronized, which means that
     * both sets are different, but they contain the same elements.
     * <p>
     * A bidirectional content-binding can be removed with
     * {@link #unbindContentBidirectional(Object, Object)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param <E>
     *            the type of the set elements
     * @param set1
     *            the first {@code ObservableSet<E>}
     * @param set2
     *            the second {@code ObservableSet<E>}
     * @throws NullPointerException
     *            if one of the sets is {@code null}
     * @throws IllegalArgumentException
     *            if {@code set1} == {@code set2}
     * @since JavaFX 2.1
     */
    public static <E> void bindContentBidirectional(ObservableSet<E> set1, ObservableSet<E> set2) {
        BidirectionalContentBinding.bind(set1, set2);
    }

    /**
     * Generates a bidirectional binding (or "bind with inverse") between two
     * instances of {@link javafx.collections.ObservableMap}.
     * <p>
     * A bidirectional binding is a binding that works in both directions. If
     * two properties {@code a} and {@code b} are linked with a bidirectional
     * binding and the value of {@code a} changes, {@code b} is set to the same
     * value automatically. And vice versa, if {@code b} changes, {@code a} is
     * set to the same value.
     * <p>
     * Only the content of the two maps is synchronized, which means that
     * both maps are different, but they contain the same elements.
     * <p>
     * A bidirectional content-binding can be removed with
     * {@link #unbindContentBidirectional(Object, Object)}.
     * <p>
     * Note: this implementation of a bidirectional binding behaves differently
     * from all other bindings here in two important aspects. A property that is
     * linked to another property with a bidirectional binding can still be set
     * (usually bindings would throw an exception). Secondly bidirectional
     * bindings are calculated eagerly, i.e. a bound property is updated
     * immediately.
     *
     * @param <K>
     *            the type of the key elements
     * @param <V>
     *            the type of the value elements
     * @param map1
     *            the first {@code ObservableMap<K, V>}
     * @param map2
     *            the second {@code ObservableMap<K, V>}
     * @since JavaFX 2.1
     */
    public static <K, V> void bindContentBidirectional(ObservableMap<K, V> map1, ObservableMap<K, V> map2) {
        BidirectionalContentBinding.bind(map1, map2);
    }

    /**
     * Remove a bidirectional content binding.
     *
     * @param obj1
     *            the first {@code Object}
     * @param obj2
     *            the second {@code Object}
     * @since JavaFX 2.1
     */
    public static void unbindContentBidirectional(Object obj1, Object obj2) {
        BidirectionalContentBinding.unbind(obj1, obj2);
    }

    /**
     * Generates a content binding between an {@link javafx.collections.ObservableList} and a {@link java.util.List}.
     * <p>
     * A content binding ensures that the {@code List} contains the same elements as the {@code ObservableList}.
     * If the content of the {@code ObservableList} changes, the {@code List} will be updated automatically.
     * <p>
     * Once a {@code List} is bound to an {@code ObservableList}, the {@code List} must not be changed directly
     * anymore. Doing so would lead to unexpected results.
     * <p>
     * A content-binding can be removed with {@link #unbindContent(Object, Object)}.
     *
     * @param <E>
     *            the type of the {@code List} elements
     * @param list1
     *            the {@code List}
     * @param list2
     *            the {@code ObservableList}
     * @since JavaFX 2.1
     */
    public static <E> void bindContent(List<E> list1, ObservableList<? extends E> list2) {
        ContentBinding.bind(list1, list2);
    }

    /**
     * Generates a content binding between an {@link javafx.collections.ObservableSet} and a {@link java.util.Set}.
     * <p>
     * A content binding ensures that the {@code Set} contains the same elements as the {@code ObservableSet}.
     * If the content of the {@code ObservableSet} changes, the {@code Set} will be updated automatically.
     * <p>
     * Once a {@code Set} is bound to an {@code ObservableSet}, the {@code Set} must not be changed directly
     * anymore. Doing so would lead to unexpected results.
     * <p>
     * A content-binding can be removed with {@link #unbindContent(Object, Object)}.
     *
     * @param <E>
     *            the type of the {@code Set} elements
     * @param set1
     *            the {@code Set}
     * @param set2
     *            the {@code ObservableSet}
     * @throws NullPointerException
     *            if one of the sets is {@code null}
     * @throws IllegalArgumentException
     *            if {@code set1} == {@code set2}
     * @since JavaFX 2.1
     */
    public static <E> void bindContent(Set<E> set1, ObservableSet<? extends E> set2) {
        ContentBinding.bind(set1, set2);
    }

    /**
     * Generates a content binding between an {@link javafx.collections.ObservableMap} and a {@link java.util.Map}.
     * <p>
     * A content binding ensures that the {@code Map} contains the same elements as the {@code ObservableMap}.
     * If the content of the {@code ObservableMap} changes, the {@code Map} will be updated automatically.
     * <p>
     * Once a {@code Map} is bound to an {@code ObservableMap}, the {@code Map} must not be changed directly
     * anymore. Doing so would lead to unexpected results.
     * <p>
     * A content-binding can be removed with {@link #unbindContent(Object, Object)}.
     *
     * @param <K>
     *            the type of the key elements of the {@code Map}
     * @param <V>
     *            the type of the value elements of the {@code Map}
     * @param map1
     *            the {@code Map}
     * @param map2
     *            the {@code ObservableMap}
     * @throws NullPointerException
     *            if one of the maps is {@code null}
     * @throws IllegalArgumentException
     *            if {@code map1} == {@code map2}
     * @since JavaFX 2.1
     */
    public static <K, V> void bindContent(Map<K, V> map1, ObservableMap<? extends K, ? extends V> map2) {
        ContentBinding.bind(map1, map2);
    }

    /**
     * Remove a content binding.
     *
     * @param obj1
     *            the first {@code Object}
     * @param obj2
     *            the second {@code Object}
     * @throws NullPointerException
     *            if one of the {@code Objects} is {@code null}
     * @throws IllegalArgumentException
     *            if {@code obj1} == {@code obj2}
     * @since JavaFX 2.1
     */
    public static void unbindContent(Object obj1, Object obj2) {
        ContentBinding.unbind(obj1, obj2);
    }



    // =================================================================================================================
    // Negation

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the negation of a {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param value
     *            the operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the value is {@code null}
     */
    public static NumberBinding negate(final ObservableNumberValue value) {
        if (value == null) {
            throw new NullPointerException("Operand cannot be null.");
        }

        if (value instanceof ObservableDoubleValue) {
            return new DoubleBinding() {
                {
                    super.bind(value);
                }

                @Override
                public void dispose() {
                    super.unbind(value);
                }

                @Override
                protected double computeValue() {
                    return -value.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return FXCollections.singletonObservableList(value);
                }
            };
        } else if (value instanceof ObservableFloatValue) {
            return new FloatBinding() {
                {
                    super.bind(value);
                }

                @Override
                public void dispose() {
                    super.unbind(value);
                }

                @Override
                protected float computeValue() {
                    return -value.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return FXCollections.singletonObservableList(value);
                }
            };
        } else if (value instanceof ObservableLongValue) {
            return new LongBinding() {
                {
                    super.bind(value);
                }

                @Override
                public void dispose() {
                    super.unbind(value);
                }

                @Override
                protected long computeValue() {
                    return -value.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return FXCollections.singletonObservableList(value);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(value);
                }

                @Override
                public void dispose() {
                    super.unbind(value);
                }

                @Override
                protected int computeValue() {
                    return -value.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return FXCollections.singletonObservableList(value);
                }
            };
        }
    }

    // =================================================================================================================
    // Sum

    private static NumberBinding add(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return op1.doubleValue() + op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return op1.floatValue() + op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return op1.longValue() + op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return op1.intValue() + op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding add(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return Bindings.add(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding add(final ObservableNumberValue op1, double op2) {
        return (DoubleBinding) Bindings.add(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding add(double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) Bindings.add(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(final ObservableNumberValue op1, float op2) {
        return Bindings.add(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(float op1, final ObservableNumberValue op2) {
        return Bindings.add(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(final ObservableNumberValue op1, long op2) {
        return Bindings.add(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(long op1, final ObservableNumberValue op2) {
        return Bindings.add(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(final ObservableNumberValue op1, int op2) {
        return Bindings.add(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the sum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding add(int op1, final ObservableNumberValue op2) {
        return Bindings.add(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Diff

    private static NumberBinding subtract(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return op1.doubleValue() - op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return op1.floatValue() - op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return op1.longValue() - op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return op1.intValue() - op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding subtract(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return Bindings.subtract(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the difference of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding subtract(final ObservableNumberValue op1, double op2) {
        return (DoubleBinding) Bindings.subtract(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the difference of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding subtract(double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) Bindings.subtract(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(final ObservableNumberValue op1, float op2) {
        return Bindings.subtract(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(float op1, final ObservableNumberValue op2) {
        return Bindings.subtract(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(final ObservableNumberValue op1, long op2) {
        return Bindings.subtract(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(long op1, final ObservableNumberValue op2) {
        return Bindings.subtract(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(final ObservableNumberValue op1, int op2) {
        return Bindings.subtract(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the difference of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding subtract(int op1, final ObservableNumberValue op2) {
        return Bindings.subtract(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Multiply

    private static NumberBinding multiply(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return op1.doubleValue() * op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return op1.floatValue() * op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return op1.longValue() * op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return op1.intValue() * op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding multiply(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return Bindings.multiply(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding multiply(final ObservableNumberValue op1, double op2) {
        return (DoubleBinding) Bindings.multiply(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding multiply(double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) Bindings.multiply(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(final ObservableNumberValue op1, float op2) {
        return Bindings.multiply(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(float op1, final ObservableNumberValue op2) {
        return Bindings.multiply(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(final ObservableNumberValue op1, long op2) {
        return Bindings.multiply(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(long op1, final ObservableNumberValue op2) {
        return Bindings.multiply(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(final ObservableNumberValue op1, int op2) {
        return Bindings.multiply(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the product of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding multiply(int op1, final ObservableNumberValue op2) {
        return Bindings.multiply(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Divide

    private static NumberBinding divide(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return op1.doubleValue() / op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return op1.floatValue() / op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return op1.longValue() / op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return op1.intValue() / op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding divide(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return Bindings.divide(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the division of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding divide(final ObservableNumberValue op1, double op2) {
        return (DoubleBinding) Bindings.divide(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the division of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding divide(double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) Bindings.divide(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(final ObservableNumberValue op1, float op2) {
        return Bindings.divide(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(float op1, final ObservableNumberValue op2) {
        return Bindings.divide(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(final ObservableNumberValue op1, long op2) {
        return Bindings.divide(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(long op1, final ObservableNumberValue op2) {
        return Bindings.divide(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(final ObservableNumberValue op1, int op2) {
        return Bindings.divide(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the division of a constant value and the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding divide(int op1, final ObservableNumberValue op2) {
        return Bindings.divide(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Equals

    private static BooleanBinding equal(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.doubleValue() - op2.doubleValue()) <= epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.floatValue() - op2.floatValue()) <= epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.longValue() - op2.longValue()) <= epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.intValue() - op2.intValue()) <= epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue} are equal (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon) {
        return Bindings.equal(op1, op2, epsilon, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue} are equal.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #equal(ObservableNumberValue, ObservableNumberValue, double)
     * equal()} method that allows a small tolerance.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return equal(op1, op2, 0.0, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final double op2, final double epsilon) {
        return equal(op1, DoubleConstant.valueOf(op2), epsilon,  op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final double op1, final ObservableNumberValue op2, final double epsilon) {
        return equal(DoubleConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final float op2, final double epsilon) {
        return equal(op1, FloatConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final float op1, final ObservableNumberValue op2, final double epsilon) {
        return equal(FloatConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final long op2, final double epsilon) {
        return equal(op1, LongConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #equal(ObservableNumberValue, long, double) equal()} method that
     * allows a small tolerance.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final long op2) {
        return equal(op1, LongConstant.valueOf(op2), 0.0, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final long op1, final ObservableNumberValue op2, final double epsilon) {
        return equal(LongConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #equal(long, ObservableNumberValue, double) equal()} method that
     * allows a small tolerance.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final long op1, final ObservableNumberValue op2) {
        return equal(LongConstant.valueOf(op1), op2, 0.0, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final int op2, final double epsilon) {
        return equal(op1, IntegerConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #equal(ObservableNumberValue, int, double) equal()} method that
     * allows a small tolerance.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableNumberValue op1, final int op2) {
        return equal(op1, IntegerConstant.valueOf(op2), 0.0, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final int op1, final ObservableNumberValue op2, final double epsilon) {
        return equal(IntegerConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #equal(int, ObservableNumberValue, double) equal()} method that
     * allows a small tolerance.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding equal(final int op1, final ObservableNumberValue op2) {
        return equal(IntegerConstant.valueOf(op1), op2, 0.0, op2);
    }

    // =================================================================================================================
    // Not Equal

    private static BooleanBinding notEqual(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.doubleValue() - op2.doubleValue()) > epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.floatValue() - op2.floatValue()) > epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.longValue() - op2.longValue()) > epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return Math.abs(op1.intValue() - op2.intValue()) > epsilon;
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue} are not equal (with a
     * tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon) {
        return Bindings.notEqual(op1, op2, epsilon, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue} are not equal.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #notEqual(ObservableNumberValue, ObservableNumberValue, double)
     * notEqual()} method that allows a small tolerance.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return notEqual(op1, op2, 0.0, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final double op2, final double epsilon) {
        return notEqual(op1, DoubleConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final double op1, final ObservableNumberValue op2, final double epsilon) {
        return notEqual(DoubleConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final float op2, final double epsilon) {
        return notEqual(op1, FloatConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final float op1, final ObservableNumberValue op2, final double epsilon) {
        return notEqual(FloatConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final long op2, final double epsilon) {
        return notEqual(op1, LongConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #notEqual(ObservableNumberValue, long, double) notEqual()} method
     * that allows a small tolerance.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final long op2) {
        return notEqual(op1, LongConstant.valueOf(op2), 0.0, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final long op1, final ObservableNumberValue op2, final double epsilon) {
        return notEqual(LongConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #notEqual(long, ObservableNumberValue, double) notEqual()} method
     * that allows a small tolerance.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final long op1, final ObservableNumberValue op2) {
        return notEqual(LongConstant.valueOf(op1), op2, 0.0, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final int op2, final double epsilon) {
        return notEqual(op1, IntegerConstant.valueOf(op2), epsilon, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #notEqual(ObservableNumberValue, int, double) notEqual()} method
     * that allows a small tolerance.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final int op2) {
        return notEqual(op1, IntegerConstant.valueOf(op2), 0.0, op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value (with a tolerance).
     * <p>
     * Two operands {@code a} and {@code b} are considered equal if
     * {@code Math.abs(a-b) <= epsilon}.
     * <p>
     * Allowing a small tolerance is recommended when comparing floating-point
     * numbers because of rounding-errors.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @param epsilon
     *            the permitted tolerance
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final int op1, final ObservableNumberValue op2, final double epsilon) {
        return notEqual(IntegerConstant.valueOf(op1), op2, epsilon, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is not
     * equal to a constant value.
     * <p>
     * When comparing floating-point numbers it is recommended to use the
     * {@link #notEqual(int, ObservableNumberValue, double) notEqual()} method
     * that allows a small tolerance.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding notEqual(final int op1, final ObservableNumberValue op2) {
        return notEqual(IntegerConstant.valueOf(op1), op2, 0.0, op2);
    }

    // =================================================================================================================
    // Greater Than

    private static BooleanBinding greaterThan(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.doubleValue() > op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.floatValue() > op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.longValue() > op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.intValue() > op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableNumberValue} is greater than the
     * value of the second.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return Bindings.greaterThan(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final double op2) {
        return greaterThan(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final double op1, final ObservableNumberValue op2) {
        return greaterThan(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final float op2) {
        return greaterThan(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final float op1, final ObservableNumberValue op2) {
        return greaterThan(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final long op2) {
        return greaterThan(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final long op1, final ObservableNumberValue op2) {
        return greaterThan(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final int op2) {
        return greaterThan(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final int op1, final ObservableNumberValue op2) {
        return greaterThan(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Less Than

    private static BooleanBinding lessThan(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        return greaterThan(op2, op1, dependencies);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableNumberValue} is less than the value
     * of the second.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return lessThan(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final double op2) {
        return lessThan(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final double op1, final ObservableNumberValue op2) {
        return lessThan(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final float op2) {
        return lessThan(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final float op1, final ObservableNumberValue op2) {
        return lessThan(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final long op2) {
        return lessThan(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final long op1, final ObservableNumberValue op2) {
        return lessThan(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final int op2) {
        return lessThan(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThan(final int op1, final ObservableNumberValue op2) {
        return lessThan(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Greater Than or Equal

    private static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.doubleValue() >= op2.doubleValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.floatValue() >= op2.floatValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.longValue() >= op2.longValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new BooleanBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected boolean computeValue() {
                    return op1.intValue() >= op2.intValue();
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableNumberValue} is greater than or equal
     * to the value of the second.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return greaterThanOrEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final double op2) {
        return greaterThanOrEqual(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final double op1, final ObservableNumberValue op2) {
        return greaterThanOrEqual(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final float op2) {
        return greaterThanOrEqual(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final float op1, final ObservableNumberValue op2) {
        return greaterThanOrEqual(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final long op2) {
        return greaterThanOrEqual(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final long op1, final ObservableNumberValue op2) {
        return greaterThanOrEqual(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * greater than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final int op2) {
        return greaterThanOrEqual(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final int op1, final ObservableNumberValue op2) {
        return greaterThanOrEqual(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Less Than or Equal

    private static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2, Observable... dependencies) {
        return greaterThanOrEqual(op2, op1, dependencies);
    }


    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableNumberValue} is less than or equal to
     * the value of the second.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return lessThanOrEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final double op2) {
        return lessThanOrEqual(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final double op1, final ObservableNumberValue op2) {
        return lessThanOrEqual(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final float op2) {
        return lessThanOrEqual(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final float op1, final ObservableNumberValue op2) {
        return lessThanOrEqual(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final long op2) {
        return lessThanOrEqual(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final long op1, final ObservableNumberValue op2) {
        return lessThanOrEqual(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableNumberValue} is
     * less than or equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final int op2) {
        return lessThanOrEqual(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than or equal to the value of a
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final int op1, final ObservableNumberValue op2) {
        return lessThanOrEqual(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Minimum

    private static NumberBinding min(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return Math.min(op1.doubleValue(), op2.doubleValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return Math.min(op1.floatValue(), op2.floatValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return Math.min(op1.longValue(), op2.longValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return Math.min(op1.intValue(), op2.intValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding min(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return min(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding min(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) min(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding min(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) min(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final ObservableNumberValue op1, final float op2) {
        return min(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final float op1, final ObservableNumberValue op2) {
        return min(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final ObservableNumberValue op1, final long op2) {
        return min(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final long op1, final ObservableNumberValue op2) {
        return min(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final ObservableNumberValue op1, final int op2) {
        return min(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the minimum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding min(final int op1, final ObservableNumberValue op2) {
        return min(IntegerConstant.valueOf(op1), op2, op2);
    }

    // =================================================================================================================
    // Maximum

    private static NumberBinding max(final ObservableNumberValue op1, final ObservableNumberValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        if ((op1 instanceof ObservableDoubleValue) || (op2 instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected double computeValue() {
                    return Math.max(op1.doubleValue(), op2.doubleValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableFloatValue) || (op2 instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected float computeValue() {
                    return Math.max(op1.floatValue(), op2.floatValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else if ((op1 instanceof ObservableLongValue) || (op2 instanceof ObservableLongValue)) {
            return new LongBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected long computeValue() {
                    return Math.max(op1.longValue(), op2.longValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        } else {
            return new IntegerBinding() {
                {
                    super.bind(dependencies);
                }

                @Override
                public void dispose() {
                    super.unbind(dependencies);
                }

                @Override
                protected int computeValue() {
                    return Math.max(op1.intValue(), op2.intValue());
                }

                @Override
                public ObservableList<?> getDependencies() {
                    return (dependencies.length == 1)?
                            FXCollections.singletonObservableList(dependencies[0])
                            : new ImmutableObservableList<>(dependencies);
                }
            };
        }
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the values of two instances of
     * {@link javafx.beans.value.ObservableNumberValue}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static NumberBinding max(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return max(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding max(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) max(op1, DoubleConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static DoubleBinding max(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) max(DoubleConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final ObservableNumberValue op1, final float op2) {
        return max(op1, FloatConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final float op1, final ObservableNumberValue op2) {
        return max(FloatConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final ObservableNumberValue op1, final long op2) {
        return max(op1, LongConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final long op1, final ObservableNumberValue op2) {
        return max(LongConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the {@code ObservableNumberValue}
     * @param op2
     *            the constant value
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final ObservableNumberValue op1, final int op2) {
        return max(op1, IntegerConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.NumberBinding} that calculates
     * the maximum of the value of a
     * {@link javafx.beans.value.ObservableNumberValue} and a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableNumberValue}
     * @return the new {@code NumberBinding}
     * @throws NullPointerException
     *             if the {@code ObservableNumberValue} is {@code null}
     */
    public static NumberBinding max(final int op1, final ObservableNumberValue op2) {
        return max(IntegerConstant.valueOf(op1), op2, op2);
    }

    // boolean
    // =================================================================================================================

     private static class BooleanAndBinding extends BooleanBinding {

        private final ObservableBooleanValue op1;
        private final ObservableBooleanValue op2;
        private final InvalidationListener observer;

        public BooleanAndBinding(ObservableBooleanValue op1, ObservableBooleanValue op2) {
            this.op1 = op1;
            this.op2 = op2;

            observer = new ShortCircuitAndInvalidator(this);

            op1.addListener(observer);
            op2.addListener(observer);
        }


        @Override
        public void dispose() {
            op1.removeListener(observer);
            op2.removeListener(observer);
        }

        @Override
        protected boolean computeValue() {
            return op1.get() && op2.get();
        }

        @Override
        public ObservableList<?> getDependencies() {
            return new ImmutableObservableList<>(op1, op2);
        }
    }

    private static class ShortCircuitAndInvalidator implements InvalidationListener {

        private final WeakReference<BooleanAndBinding> ref;

        private ShortCircuitAndInvalidator(BooleanAndBinding binding) {
            assert binding != null;
            ref = new WeakReference<>(binding);
        }

        @Override
        public void invalidated(Observable observable) {
            final BooleanAndBinding binding = ref.get();
            if (binding == null) {
                observable.removeListener(this);
            } else {
                // short-circuit invalidation. This BooleanBinding becomes
                // only invalid if the first operator changes or the
                // first parameter is true.
                if ((binding.op1.equals(observable) || (binding.isValid() && binding.op1.get()))) {
                    binding.invalidate();
                }
            }
        }

    }

    /**
     * Creates a {@link BooleanBinding} that calculates the conditional-AND
     * operation on the value of two instance of
     * {@link javafx.beans.value.ObservableBooleanValue}.
     *
     * @param op1
     *            first {@code ObservableBooleanValue}
     * @param op2
     *            second {@code ObservableBooleanValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding and(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanAndBinding(op1, op2);
    }

    private static class BooleanOrBinding extends BooleanBinding {

        private final ObservableBooleanValue op1;
        private final ObservableBooleanValue op2;
        private final InvalidationListener observer;

        public BooleanOrBinding(ObservableBooleanValue op1, ObservableBooleanValue op2) {
            this.op1 = op1;
            this.op2 = op2;
            observer = new ShortCircuitOrInvalidator(this);
            op1.addListener(observer);
            op2.addListener(observer);
        }


        @Override
        public void dispose() {
            op1.removeListener(observer);
            op2.removeListener(observer);
        }

        @Override
        protected boolean computeValue() {
            return op1.get() || op2.get();
        }

        @Override
        public ObservableList<?> getDependencies() {
            return new ImmutableObservableList<>(op1, op2);
        }
    }


    private static class ShortCircuitOrInvalidator implements InvalidationListener {

        private final WeakReference<BooleanOrBinding> ref;

        private ShortCircuitOrInvalidator(BooleanOrBinding binding) {
            assert binding != null;
            ref = new WeakReference<>(binding);
        }

        @Override
        public void invalidated(Observable observable) {
            final BooleanOrBinding binding = ref.get();
            if (binding == null) {
                observable.removeListener(this);
            } else {
                // short circuit invalidation. This BooleanBinding becomes
                // only invalid if the first operator changes or the
                // first parameter is false.
                if ((binding.op1.equals(observable) || (binding.isValid() && !binding.op1.get()))) {
                    binding.invalidate();
                }
            }
        }

    }

    /**
     * Creates a {@link BooleanBinding} that calculates the conditional-OR
     * operation on the value of two instance of
     * {@link javafx.beans.value.ObservableBooleanValue}.
     *
     * @param op1
     *            first {@code ObservableBooleanValue}
     * @param op2
     *            second {@code ObservableBooleanValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding or(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanOrBinding(op1, op2);
    }

    /**
     * Creates a {@link BooleanBinding} that calculates the inverse of the value
     * of a {@link javafx.beans.value.ObservableBooleanValue}.
     *
     * @param op
     *            the {@code ObservableBooleanValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the operand is {@code null}
     */
    public static BooleanBinding not(final ObservableBooleanValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return !op.get();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the values of two
     * instances of {@link javafx.beans.value.ObservableBooleanValue} are equal.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equal(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op1, op2);
            }

            @Override
            public void dispose() {
                super.unbind(op1, op2);
            }

            @Override
            protected boolean computeValue() {
                return op1.get() == op2.get();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op1, op2);
            }
        };
    }

    /**
     * Creates a new {@link BooleanBinding} that holds {@code true} if the values of two
     * instances of {@link javafx.beans.value.ObservableBooleanValue} are not
     * equal.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op1, op2);
            }

            @Override
            public void dispose() {
                super.unbind(op1, op2);
            }

            @Override
            protected boolean computeValue() {
                return op1.get() != op2.get();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op1, op2);
            }
        };
    }

    // String
    // =================================================================================================================

    /**
     * Returns a {@link javafx.beans.binding.StringExpression} that wraps a
     * {@link javafx.beans.value.ObservableValue}. If the
     * {@code ObservableValue} is already a {@code StringExpression}, it will be
     * returned. Otherwise a new {@link javafx.beans.binding.StringBinding} is
     * created that holds the value of the {@code ObservableValue} converted to
     * a {@code String}.
     *
     * @param observableValue
     *            The source {@code ObservableValue}
     * @return A {@code StringExpression} that wraps the {@code ObservableValue}
     *         if necessary
     * @throws NullPointerException
     *             if {@code observableValue} is {@code null}
     */
    public static StringExpression convert(ObservableValue<?> observableValue) {
        return StringFormatter.convert(observableValue);
    }

    /**
     * Returns a {@link javafx.beans.binding.StringExpression} that holds the
     * value of the concatenation of multiple {@code Objects}.
     * <p>
     * If one of the arguments implements
     * {@link javafx.beans.value.ObservableValue} and the value of this
     * {@code ObservableValue} changes, the change is automatically reflected in
     * the {@code StringExpression}.
     * <p>
     * If {@code null} or an empty array is passed to this method, a
     * {@code StringExpression} that contains an empty {@code String} is
     * returned
     *
     * @param args
     *            the {@code Objects} that should be concatenated
     * @return the new {@code StringExpression}
     */
    public static StringExpression concat(Object... args) {
        return StringFormatter.concat(args);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringExpression} that holds the
     * value of multiple {@code Objects} formatted according to a format
     * {@code String}.
     * <p>
     * If one of the arguments implements
     * {@link javafx.beans.value.ObservableValue} and the value of this
     * {@code ObservableValue} changes, the change is automatically reflected in
     * the {@code StringExpression}.
     * <p>
     * See {@code java.util.Formatter} for formatting rules.
     *
     * @param format
     *            the formatting {@code String}
     * @param args
     *            the {@code Objects} that should be inserted in the formatting
     *            {@code String}
     * @return the new {@code StringExpression}
     */
    public static StringExpression format(String format, Object... args) {
        return StringFormatter.format(format, args);
    }

    /**
     * Creates a {@link javafx.beans.binding.StringExpression} that holds the
     * value of multiple {@code Objects} formatted according to a format
     * {@code String} and a specified {@code Locale}
     * <p>
     * If one of the arguments implements
     * {@link javafx.beans.value.ObservableValue} and the value of this
     * {@code ObservableValue} changes, the change is automatically reflected in
     * the {@code StringExpression}.
     * <p>
     * See {@code java.util.Formatter} for formatting rules. See
     * {@code java.util.Locale} for details on {@code Locale}.
     *
     * @param locale
     *            the {@code Locale} to use during formatting
     * @param format
     *            the formatting {@code String}
     * @param args
     *            the {@code Objects} that should be inserted in the formatting
     *            {@code String}
     * @return the new {@code StringExpression}
     */
    public static StringExpression format(Locale locale, String format,
            Object... args) {
        return StringFormatter.format(locale, format, args);
    }

    private static String getStringSafe(String value) {
        return value == null ? "" : value;
    }

    private static BooleanBinding equal(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return s1.equals(s2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableStringValue} are equal.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equal(final ObservableStringValue op1, final ObservableStringValue op2) {
        return equal(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableStringValue op1, String op2) {
        return equal(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding equal(String op1, final ObservableStringValue op2) {
        return equal(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding notEqual(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return ! s1.equals(s2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableStringValue} are not equal.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return notEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is not
     * equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableStringValue op1, String op2) {
        return notEqual(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is not
     * equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding notEqual(String op1, final ObservableStringValue op2) {
        return notEqual(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding equalIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return s1.equalsIgnoreCase(s2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableStringValue} are equal ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equalIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2) {
        return equalIgnoreCase(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * equal to a constant value ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding equalIgnoreCase(final ObservableStringValue op1, String op2) {
        return equalIgnoreCase(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * equal to a constant value ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding equalIgnoreCase(String op1, final ObservableStringValue op2) {
        return equalIgnoreCase(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding notEqualIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return ! s1.equalsIgnoreCase(s2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableStringValue} are not equal ignoring
     * case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqualIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2) {
        return notEqualIgnoreCase(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is not
     * equal to a constant value ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding notEqualIgnoreCase(final ObservableStringValue op1, String op2) {
        return notEqualIgnoreCase(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is not
     * equal to a constant value ignoring case.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding notEqualIgnoreCase(String op1, final ObservableStringValue op2) {
        return notEqualIgnoreCase(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding greaterThan(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return s1.compareTo(s2) > 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableStringValue} is greater than the
     * value of the second.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableStringValue op1, final ObservableStringValue op2) {
        return greaterThan(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * greater than a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding greaterThan(final ObservableStringValue op1, String op2) {
        return greaterThan(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a constant value is greater than the value of a
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding greaterThan(String op1, final ObservableStringValue op2) {
        return greaterThan(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding lessThan(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        return greaterThan(op2, op1, dependencies);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableStringValue} is less than the value
     * of the second.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableStringValue op1, final ObservableStringValue op2) {
        return lessThan(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * less than a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding lessThan(final ObservableStringValue op1, String op2) {
        return lessThan(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than the value of a
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding lessThan(String op1, final ObservableStringValue op2) {
        return lessThan(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding greaterThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final String s1 = getStringSafe(op1.get());
                final String s2 = getStringSafe(op2.get());
                return s1.compareTo(s2) >= 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableStringValue} is greater than or equal
     * to the value of the second.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return greaterThanOrEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * greater than or equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(final ObservableStringValue op1, String op2) {
        return greaterThanOrEqual(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is greater than or equal to the value of a
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding greaterThanOrEqual(String op1, final ObservableStringValue op2) {
        return greaterThanOrEqual(StringConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding lessThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2, final Observable... dependencies) {
        return greaterThanOrEqual(op2, op1, dependencies);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of the first
     * {@link javafx.beans.value.ObservableStringValue} is less than or equal to
     * the value of the second.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return lessThanOrEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is
     * less than or equal to a constant value.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the {@code ObservableStringValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(final ObservableStringValue op1, String op2) {
        return lessThanOrEqual(op1, StringConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a constant value is less than or equal to the value of a
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered equal to an empty {@code String}.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     */
    public static BooleanBinding lessThanOrEqual(String op1, final ObservableStringValue op2) {
        return lessThanOrEqual(StringConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that holds the length of a
     * {@link javafx.beans.value.ObservableStringValue}.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered to have a length of {@code 0}.
     *
     * @param op
     *            the {@code ObservableStringValue}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     * @since JavaFX 8.0
     */
    public static IntegerBinding length(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                return getStringSafe(op.get()).length();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is empty.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered to be empty.
     *
     * @param op
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     * @since JavaFX 8.0
     */
    public static BooleanBinding isEmpty(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return getStringSafe(op.get()).isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of a {@link javafx.beans.value.ObservableStringValue} is not empty.
     * <p>
     * Note: In this comparison a {@code String} that is {@code null} is
     * considered to be empty.
     *
     * @param op
     *            the {@code ObservableStringValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableStringValue} is {@code null}
     * @since JavaFX 8.0
     */
    public static BooleanBinding isNotEmpty(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return !getStringSafe(op.get()).isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    // Object
    // =================================================================================================================

    private static BooleanBinding equal(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final Object obj1 = op1.get();
                final Object obj2 = op2.get();
                return obj1 == null ? obj2 == null : obj1.equals(obj2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableObjectValue} are equal.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding equal(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2) {
        return equal(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableObjectValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding equal(final ObservableObjectValue<?> op1, Object op2) {
        return equal(op1, ObjectConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * equal to a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableObjectValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding equal(Object op1, final ObservableObjectValue<?> op2) {
        return equal(ObjectConstant.valueOf(op1), op2, op2);
    }

    private static BooleanBinding notEqual(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2, final Observable... dependencies) {
        if ((op1 == null) || (op2 == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }
        assert (dependencies != null) && (dependencies.length > 0);

        return new BooleanBinding() {
            {
                super.bind(dependencies);
            }

            @Override
            public void dispose() {
                super.unbind(dependencies);
            }

            @Override
            protected boolean computeValue() {
                final Object obj1 = op1.get();
                final Object obj2 = op2.get();
                return obj1 == null ? obj2 != null : ! obj1.equals(obj2);
            }

            @Override
            public ObservableList<?> getDependencies() {
                return (dependencies.length == 1)?
                        FXCollections.singletonObservableList(dependencies[0])
                        : new ImmutableObservableList<>(dependencies);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the values of two instances of
     * {@link javafx.beans.value.ObservableObjectValue} are not equal.
     *
     * @param op1
     *            the first operand
     * @param op2
     *            the second operand
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if one of the operands is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2) {
        return notEqual(op1, op2, op1, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * not equal to a constant value.
     *
     * @param op1
     *            the {@code ObservableObjectValue}
     * @param op2
     *            the constant value
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding notEqual(final ObservableObjectValue<?> op1, Object op2) {
        return notEqual(op1, ObjectConstant.valueOf(op2), op1);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * not equal to a constant value.
     *
     * @param op1
     *            the constant value
     * @param op2
     *            the {@code ObservableObjectValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding notEqual(Object op1, final ObservableObjectValue<?> op2) {
        return notEqual(ObjectConstant.valueOf(op1), op2, op2);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * {@code null}.
     *
     * @param op
     *            the {@code ObservableObjectValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding isNull(final ObservableObjectValue<?> op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return op.get() == null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if the value of an {@link javafx.beans.value.ObservableObjectValue} is
     * not {@code null}.
     *
     * @param op
     *            the {@code ObservableObjectValue}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableObjectValue} is {@code null}
     */
    public static BooleanBinding isNotNull(final ObservableObjectValue<?> op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return op.get() != null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    // List
    // =================================================================================================================

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the size
     * of an {@link javafx.collections.ObservableList}.
     *
     * @param op
     *            the {@code ObservableList}
     * @param <E> type of the {@code List} elements
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException
     *             if the {@code ObservableList} is {@code null}
     * @since JavaFX 2.1
     */
    public static <E> IntegerBinding size(final ObservableList<E> op) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                return op.size();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableList} is empty.
     *
     * @param op
     *            the {@code ObservableList}
     * @param <E> type of the {@code List} elements
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableList} is {@code null}
     * @since JavaFX 2.1
     */
    public static <E> BooleanBinding isEmpty(final ObservableList<E> op) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableList} is not empty.
     *
     * @param op
     *            the {@code ObservableList}
     * @param <E> type of the {@code List} elements
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableList} is {@code null}
     * @since JavaFX 8.0
     */
    public static <E> BooleanBinding isNotEmpty(final ObservableList<E> op)     {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return !op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.ObjectBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code ObjectBinding}
     * will contain {@code null} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @param <E> the type of the {@code List} elements
     * @return the new {@code ObjectBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new ObjectBinding<>() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected E computeValue() {
                try {
                    return op.get(index);
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.ObjectBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code ObjectBinding}
     * will contain {@code null} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @param <E> the type of the {@code List} elements
     * @return the new {@code ObjectBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final ObservableIntegerValue index) {
        return valueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.ObjectBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code ObjectBinding}
     * will contain {@code null} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @param <E> the type of the {@code List} elements
     * @return the new {@code ObjectBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new ObjectBinding<>() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected E computeValue() {
                try {
                    return op.get(index.intValue());
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code BooleanBinding}
     * will hold {@code false} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static BooleanBinding booleanValueAt(final ObservableList<Boolean> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                try {
                    final Boolean value = op.get(index);
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return false;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code BooleanBinding}
     * will hold {@code false} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static BooleanBinding booleanValueAt(final ObservableList<Boolean> op, final ObservableIntegerValue index) {
        return booleanValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code BooleanBinding}
     * will hold {@code false} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static BooleanBinding booleanValueAt(final ObservableList<Boolean> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected boolean computeValue() {
                try {
                    final Boolean value = op.get(index.intValue());
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return false;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code DoubleBinding}
     * will hold {@code 0.0} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static DoubleBinding doubleValueAt(final ObservableList<? extends Number> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new DoubleBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected double computeValue() {
                try {
                    final Number value = op.get(index);
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.doubleValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code DoubleBinding}
     * will hold {@code 0.0} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static DoubleBinding doubleValueAt(final ObservableList<? extends Number> op, final ObservableIntegerValue index) {
        return doubleValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code DoubleBinding}
     * will hold {@code 0.0} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static DoubleBinding doubleValueAt(final ObservableList<? extends Number> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new DoubleBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected double computeValue() {
                try {
                    final Number value = op.get(index.intValue());
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.doubleValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static FloatBinding floatValueAt(final ObservableList<? extends Number> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new FloatBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected float computeValue() {
                try {
                    final Number value = op.get(index);
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.floatValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static FloatBinding floatValueAt(final ObservableList<? extends Number> op, final ObservableIntegerValue index) {
        return floatValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static FloatBinding floatValueAt(final ObservableList<? extends Number> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new FloatBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected float computeValue() {
                try {
                    final Number value = op.get(index.intValue());
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.floatValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static IntegerBinding integerValueAt(final ObservableList<? extends Number> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                try {
                    final Number value = op.get(index);
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.intValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static IntegerBinding integerValueAt(final ObservableList<? extends Number> op, final ObservableIntegerValue index) {
        return integerValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static IntegerBinding integerValueAt(final ObservableList<? extends Number> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected int computeValue() {
                try {
                    final Number value = op.get(index.intValue());
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.intValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.LongBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code LongBinding}
     * will hold {@code 0L} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code LongBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static LongBinding longValueAt(final ObservableList<? extends Number> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new LongBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected long computeValue() {
                try {
                    final Number value = op.get(index);
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.longValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0L;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.LongBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code LongBinding}
     * will hold {@code 0L} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code LongBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static LongBinding longValueAt(final ObservableList<? extends Number> op, final ObservableIntegerValue index) {
        return longValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.LongBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code LongBinding}
     * will hold {@code 0L} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code LongBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static LongBinding longValueAt(final ObservableList<? extends Number> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new LongBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected long computeValue() {
                try {
                    final Number value = op.get(index.intValue());
                    if (value == null) {
                        Logging.getLogger().fine("List element is null, returning default value instead.", new NullPointerException());
                    } else {
                        return value.longValue();
                    }
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0L;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.StringBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code StringBinding}
     * will hold {@code null} if the {@code index} points behind the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code StringBinding}
     * @throws NullPointerException if the {@code ObservableList} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 2.1
     */
    public static StringBinding stringValueAt(final ObservableList<String> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new StringBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected String computeValue() {
                try {
                    return op.get(index);
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.StringBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code StringBinding}
     * will hold {@code ""} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}
     * @return the new {@code StringBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 2.1
     */
    public static StringBinding stringValueAt(final ObservableList<String> op, final ObservableIntegerValue index) {
        return stringValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.StringBinding} that contains the element
     * of an {@link javafx.collections.ObservableList} at the specified position. The {@code StringBinding}
     * will hold {@code ""} if the {@code index} is outside of the {@code ObservableList}.
     *
     * @param op the {@code ObservableList}
     * @param index the position in the {@code List}, converted to int
     * @return the new {@code StringBinding}
     * @throws NullPointerException if the {@code ObservableList} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static StringBinding stringValueAt(final ObservableList<String> op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new StringBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected String computeValue() {
                try {
                    return op.get(index.intValue());
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    // Set
    // =================================================================================================================

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the size
     * of an {@link javafx.collections.ObservableSet}.
     *
     * @param op
     *            the {@code ObservableSet}
     * @param <E> the type of the {@code Set} elements
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException
     *             if the {@code ObservableSet} is {@code null}
     * @since JavaFX 2.1
     */
    public static <E> IntegerBinding size(final ObservableSet<E> op) {
        if (op == null) {
            throw new NullPointerException("Set cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                return op.size();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableSet} is empty.
     *
     * @param op
     *            the {@code ObservableSet}
     * @param <E> the type of the {@code Set} elements
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableSet} is {@code null}
     * @since JavaFX 2.1
     */
    public static <E> BooleanBinding isEmpty(final ObservableSet<E> op) {
        if (op == null) {
            throw new NullPointerException("Set cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableSet} is not empty.
     *
     * @param op
     *            the {@code ObservableSet}
     * @param <E> the type of the {@code Set} elements
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableSet} is {@code null}
     * @since JavaFX 8.0
     */
    public static <E> BooleanBinding isNotEmpty(final ObservableSet<E> op)     {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return !op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    // Array
    // =================================================================================================================

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the size
     * of an {@link javafx.collections.ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException
     *             if the {@code ObservableArray} is {@code null}
     * @since JavaFX 8.0
     */
    public static IntegerBinding size(final ObservableArray op) {
        if (op == null) {
            throw new NullPointerException("Array cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                return op.size();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} points behind the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableArray} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 8.0
     */
    public static FloatBinding floatValueAt(final ObservableFloatArray op, final int index) {
        if (op == null) {
            throw new NullPointerException("Array cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new FloatBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected float computeValue() {
                try {
                    return op.get(index);
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} is outside of the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableArray} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static FloatBinding floatValueAt(final ObservableFloatArray op, final ObservableIntegerValue index) {
        return floatValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code index} is outside of the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}, converted to int
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableArray} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static FloatBinding floatValueAt(final ObservableFloatArray op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new FloatBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected float computeValue() {
                try {
                    return op.get(index.intValue());
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} points behind the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableArray} is {@code null}
     * @throws IllegalArgumentException if {@code index < 0}
     * @since JavaFX 8.0
     */
    public static IntegerBinding integerValueAt(final ObservableIntegerArray op, final int index) {
        if (op == null) {
            throw new NullPointerException("Array cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                try {
                    return op.get(index);
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} is outside of the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableArray} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static IntegerBinding integerValueAt(final ObservableIntegerArray op, final ObservableIntegerValue index) {
        return integerValueAt(op, (ObservableNumberValue)index);
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the element
     * of an {@link javafx.collections.ObservableArray} at the specified position. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code index} is outside of the {@code ObservableArray}.
     *
     * @param op the {@code ObservableArray}
     * @param index the position in the {@code ObservableArray}, converted to int
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableArray} or {@code index} is {@code null}
     * @since JavaFX 8.0
     */
    public static IntegerBinding integerValueAt(final ObservableIntegerArray op, final ObservableNumberValue index) {
        if ((op == null) || (index == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op, index);
            }

            @Override
            public void dispose() {
                super.unbind(op, index);
            }

            @Override
            protected int computeValue() {
                try {
                    return op.get(index.intValue());
                } catch (IndexOutOfBoundsException ex) {
                    Logging.getLogger().fine("Exception while evaluating binding", ex);
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, index);
            }
        };
    }

    // Map
    // =================================================================================================================

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the size
     * of an {@link javafx.collections.ObservableMap}.
     *
     * @param op
     *            the {@code ObservableMap}
     * @param <K> type of the key elements of the {@code Map}
     * @param <V> type of the value elements of the {@code Map}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException
     *             if the {@code ObservableMap} is {@code null}
     *
     * @since JavaFX 2.1
     */
    public static <K, V> IntegerBinding size(final ObservableMap<K, V> op) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                return op.size();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableMap} is empty.
     *
     * @param op
     *            the {@code ObservableMap}
     * @param <K> type of the key elements of the {@code Map}
     * @param <V> type of the value elements of the {@code Map}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K, V> BooleanBinding isEmpty(final ObservableMap<K, V> op) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that holds {@code true}
     * if a given {@link javafx.collections.ObservableMap} is not empty.
     *
     * @param op
     *            the {@code ObservableMap}
     * @param <K> type of the key elements of the {@code Map}
     * @param <V> type of the value elements of the {@code Map}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException
     *             if the {@code ObservableMap} is {@code null}
     * @since JavaFX 8.0
     */
    public static <K, V> BooleanBinding isNotEmpty(final ObservableMap<K, V> op)     {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                return !op.isEmpty();
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.ObjectBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @param <V> type of the value elements of the {@code Map}
     * @return the new {@code ObjectBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K, V> ObjectBinding<V> valueAt(final ObservableMap<K, V> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new ObjectBinding<>() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected V computeValue() {
                try {
                    return op.get(key);
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.ObjectBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @param <V> type of the value elements of the {@code Map}
     * @return the new {@code ObjectBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K, V> ObjectBinding<V> valueAt(final ObservableMap<K, V> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new ObjectBinding<>() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected V computeValue() {
                try {
                    return op.get(key.getValue());
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code BooleanBinding}
     * will hold {@code false} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> BooleanBinding booleanValueAt(final ObservableMap<K, Boolean> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected boolean computeValue() {
                try {
                    final Boolean value = op.get(key);
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value;
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return false;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.BooleanBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code BooleanBinding}
     * will hold {@code false} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code BooleanBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> BooleanBinding booleanValueAt(final ObservableMap<K, Boolean> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new BooleanBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected boolean computeValue() {
                try {
                    final Boolean value = op.get(key.getValue());
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value;
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return false;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code DoubleBinding}
     * will hold {@code 0.0} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> DoubleBinding doubleValueAt(final ObservableMap<K, ? extends Number> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new DoubleBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected double computeValue() {
                try {
                    final Number value = op.get(key);
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.doubleValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0.0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.DoubleBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code DoubleBinding}
     * will hold {@code 0.0} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code DoubleBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> DoubleBinding doubleValueAt(final ObservableMap<K, ? extends Number> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new DoubleBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected double computeValue() {
                try {
                    final Number value = op.get(key.getValue());
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.doubleValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0.0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> FloatBinding floatValueAt(final ObservableMap<K, ? extends Number> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new FloatBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected float computeValue() {
                try {
                    final Number value = op.get(key);
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.floatValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.FloatBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code FloatBinding}
     * will hold {@code 0.0f} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code FloatBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> FloatBinding floatValueAt(final ObservableMap<K, ? extends Number> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new FloatBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected float computeValue() {
                try {
                    final Number value = op.get(key.getValue());
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.floatValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0.0f;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> IntegerBinding integerValueAt(final ObservableMap<K, ? extends Number> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected int computeValue() {
                try {
                    final Number value = op.get(key);
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.intValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.IntegerBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code IntegerBinding}
     * will hold {@code 0} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code IntegerBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> IntegerBinding integerValueAt(final ObservableMap<K, ? extends Number> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new IntegerBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected int computeValue() {
                try {
                    final Number value = op.get(key.getValue());
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.intValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.LongBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code LongBinding}
     * will hold {@code 0L} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code LongBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> LongBinding longValueAt(final ObservableMap<K, ? extends Number> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new LongBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected long computeValue() {
                try {
                    final Number value = op.get(key);
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.longValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0L;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.LongBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code LongBinding}
     * will hold {@code 0L} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code LongBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> LongBinding longValueAt(final ObservableMap<K, ? extends Number> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new LongBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected long computeValue() {
                try {
                    final Number value = op.get(key.getValue());
                    if (value == null) {
                        Logging.getLogger().fine("Element not found in map, returning default value instead.", new NullPointerException());
                    } else {
                        return value.longValue();
                    }
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return 0L;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.StringBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code StringBinding}
     * will hold {@code null} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code StringBinding}
     * @throws NullPointerException if the {@code ObservableMap} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> StringBinding stringValueAt(final ObservableMap<K, String> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }

        return new StringBinding() {
            {
                super.bind(op);
            }

            @Override
            public void dispose() {
                super.unbind(op);
            }

            @Override
            protected String computeValue() {
                try {
                    return op.get(key);
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return FXCollections.singletonObservableList(op);
            }
        };
    }

    /**
     * Creates a new {@link javafx.beans.binding.StringBinding} that contains the mapping of a specific key
     * in an {@link javafx.collections.ObservableMap}. The {@code StringBinding}
     * will hold {@code ""} if the {@code key} cannot be found in the {@code ObservableMap}.
     *
     * @param op the {@code ObservableMap}
     * @param key the key in the {@code Map}
     * @param <K> type of the key elements of the {@code Map}
     * @return the new {@code StringBinding}
     * @throws NullPointerException if the {@code ObservableMap} or {@code key} is {@code null}
     * @since JavaFX 2.1
     */
    public static <K> StringBinding stringValueAt(final ObservableMap<K, String> op, final ObservableValue<? extends K> key) {
        if ((op == null) || (key == null)) {
            throw new NullPointerException("Operands cannot be null.");
        }

        return new StringBinding() {
            {
                super.bind(op, key);
            }

            @Override
            public void dispose() {
                super.unbind(op, key);
            }

            @Override
            protected String computeValue() {
                try {
                    return op.get(key.getValue());
                } catch (ClassCastException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                } catch (NullPointerException ex) {
                    Logging.getLogger().warning("Exception while evaluating binding", ex);
                    // ignore
                }
                return null;
            }

            @Override
            public ObservableList<?> getDependencies() {
                return new ImmutableObservableList<>(op, key);
            }
        };
    }


}
