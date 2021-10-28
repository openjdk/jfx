/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;

/**
 * The SpinnerValueFactory is the model behind the JavaFX
 * {@link Spinner Spinner control} - without a value factory installed a
 * Spinner is unusable. It is the role of the value factory to handle almost all
 * aspects of the Spinner, including:
 *
 * <ul>
 *     <li>Representing the current state of the {@link javafx.scene.control.SpinnerValueFactory#valueProperty() value},</li>
 *     <li>{@link SpinnerValueFactory#increment(int) Incrementing}
 *         and {@link SpinnerValueFactory#decrement(int) decrementing} the
 *         value, with one or more steps per call,</li>
 *     <li>{@link javafx.scene.control.SpinnerValueFactory#converterProperty() Converting} text input
 *         from the user (via the Spinner {@link Spinner#editorProperty() editor},</li>
 *     <li>Converting {@link javafx.scene.control.SpinnerValueFactory#converterProperty() objects to user-readable strings}
 *         for display on screen</li>
 * </ul>
 *
 * <p>SpinnerValueFactory classes for some common types are provided with JavaFX, including:
 *
 * <ul>
 *     <li>{@link SpinnerValueFactory.IntegerSpinnerValueFactory}</li>
 *     <li>{@link SpinnerValueFactory.DoubleSpinnerValueFactory}</li>
 *     <li>{@link SpinnerValueFactory.ListSpinnerValueFactory}</li>
 * </ul>
 *
 * @param <T> The type of the data this value factory deals with, which must
 *            coincide with the type of the Spinner that the value factory is set on.
 * @see Spinner
 * @see SpinnerValueFactory.IntegerSpinnerValueFactory
 * @see SpinnerValueFactory.DoubleSpinnerValueFactory
 * @see SpinnerValueFactory.ListSpinnerValueFactory
 * @since JavaFX 8u40
 */
public abstract class SpinnerValueFactory<T> {

    /**
     * Creates a default SpinnerValueFactory.
     */
    public SpinnerValueFactory() {}

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/



    /* *************************************************************************
     *                                                                         *
     * Abstract methods                                                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Attempts to decrement the {@link #valueProperty() value} by the given
     * number of steps.
     *
     * @param steps The number of decrements that should be performed on the value.
     */
    public abstract void decrement(int steps);


    /**
     * Attempts to omcrement the {@link #valueProperty() value} by the given
     * number of steps.
     *
     * @param steps The number of increments that should be performed on the value.
     */
    public abstract void increment(int steps);



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- value
    /**
     * Represents the current value of the SpinnerValueFactory, or null if no
     * value has been set.
     */
    private ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");
    public final T getValue() {
        return value.get();
    }
    public final void setValue(T newValue) {
        value.set(newValue);
    }
    public final ObjectProperty<T> valueProperty() {
        return value;
    }


    // --- converter
    /**
     * Converts the user-typed input (when the Spinner is
     * {@link Spinner#editableProperty() editable}) to an object of type T,
     * such that the input may be retrieved via the  {@link #valueProperty() value}
     * property.
     */
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");
    public final StringConverter<T> getConverter() {
        return converter.get();
    }
    public final void setConverter(StringConverter<T> newValue) {
        converter.set(newValue);
    }
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }


    // --- wrapAround
    /**
     * The wrapAround property is used to specify whether the value factory should
     * be circular. For example, should an integer-based value model increment
     * from the maximum value back to the minimum value (and vice versa).
     */
    private BooleanProperty wrapAround;
    public final void setWrapAround(boolean value) {
        wrapAroundProperty().set(value);
    }
    public final boolean isWrapAround() {
        return wrapAround == null ? false : wrapAround.get();
    }
    public final BooleanProperty wrapAroundProperty() {
        if (wrapAround == null) {
            wrapAround = new SimpleBooleanProperty(this, "wrapAround", false);
        }
        return wrapAround;
    }



    /* *************************************************************************
     *                                                                         *
     * Subclasses of SpinnerValueFactory                                       *
     *                                                                         *
     **************************************************************************/

    /**
     * A {@link javafx.scene.control.SpinnerValueFactory} implementation designed to iterate through
     * a list of values.
     *
     * <p>Note that the default {@link #converterProperty() converter} is implemented
     * simply as shown below, which may be adequate in many cases, but it is important
     * for users to ensure that this suits their needs (and adjust when necessary):
     *
     * <pre>
     * setConverter(new StringConverter&lt;T&gt;() {
     *     &#064;Override public String toString(T value) {
     *         if (value == null) {
     *             return "";
     *         }
     *         return value.toString();
     *     }
     *
     *     &#064;Override public T fromString(String string) {
     *         return (T) string;
     *     }
     * });</pre>
     *
     * @param <T> The type of the elements in the {@link java.util.List}.
     * @since JavaFX 8u40
     */
    public static class ListSpinnerValueFactory<T> extends SpinnerValueFactory<T> {

        /* *********************************************************************
         *                                                                     *
         * Private fields                                                      *
         *                                                                     *
         **********************************************************************/

        private int currentIndex = 0;

        private final ListChangeListener<T> itemsContentObserver = c -> {
            // the items content has changed. We do not try to find the current
            // item, instead we remain at the currentIndex, if possible, or else
            // we go back to index 0, and if that fails, we go to null
            updateCurrentIndex();
        };

        private WeakListChangeListener<T> weakItemsContentObserver =
                new WeakListChangeListener<T>(itemsContentObserver);



        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Creates a new instance of the ListSpinnerValueFactory with the given
         * list used as the list to step through.
         *
         * @param items The list of items to step through with the Spinner.
         */
        public ListSpinnerValueFactory(@NamedArg("items") ObservableList<T> items) {
            setItems(items);
            setConverter(new StringConverter<T>() {
                @Override public String toString(T value) {
                    if (value == null) {
                        return "";
                    }
                    return value.toString();
                }

                @Override public T fromString(String string) {
                    return (T) string;
                }
            });

            valueProperty().addListener((o, oldValue, newValue) -> {
                // when the value is set, we need to react to ensure it is a
                // valid value (and if not, blow up appropriately)
                int newIndex = -1;
                if (items.contains(newValue)) {
                    newIndex = items.indexOf(newValue);
                } else {
                    // add newValue to list
                    items.add(newValue);
                    newIndex = items.indexOf(newValue);
                }
                currentIndex = newIndex;
            });
            setValue(_getValue(currentIndex));
        }



        /* *********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/
        // --- Items
        private ObjectProperty<ObservableList<T>> items;

        /**
         * Sets the underlying data model for the ListSpinnerValueFactory. Note that it has a generic
         * type that must match the type of the Spinner itself.
         * @param value the list of items
         */
        public final void setItems(ObservableList<T> value) {
            itemsProperty().set(value);
        }

        /**
         * Returns an {@link javafx.collections.ObservableList} that contains the items currently able
         * to be iterated through by the user. This may be null if
         * {@link #setItems(javafx.collections.ObservableList)} has previously been
         * called, however, by default it is an empty ObservableList.
         *
         * @return An ObservableList containing the items to be shown to the user, or
         *      null if the items have previously been set to null.
         */
        public final ObservableList<T> getItems() {
            return items == null ? null : items.get();
        }

        /**
         * The underlying data model for the ListView. Note that it has a generic
         * type that must match the type of the ListView itself.
         * @return the list of items
         */
        public final ObjectProperty<ObservableList<T>> itemsProperty() {
            if (items == null) {
                items = new SimpleObjectProperty<ObservableList<T>>(this, "items") {
                    WeakReference<ObservableList<T>> oldItemsRef;

                    @Override protected void invalidated() {
                        ObservableList<T> oldItems = oldItemsRef == null ? null : oldItemsRef.get();
                        ObservableList<T> newItems = getItems();

                        // update listeners
                        if (oldItems != null) {
                            oldItems.removeListener(weakItemsContentObserver);
                        }
                        if (newItems != null) {
                            newItems.addListener(weakItemsContentObserver);
                        }

                        // update the current value based on the index
                        updateCurrentIndex();

                        oldItemsRef = new WeakReference<>(getItems());
                    }
                };
            }
            return items;
        }



        /* *********************************************************************
         *                                                                     *
         * Overridden methods                                                  *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void decrement(int steps) {
            final int max = getItemsSize() - 1;
            int newIndex = currentIndex - steps;
            currentIndex = newIndex >= 0 ? newIndex : (isWrapAround() ? Spinner.wrapValue(newIndex, 0, max + 1) : 0);
            setValue(_getValue(currentIndex));
        }

        /** {@inheritDoc} */
        @Override public void increment(int steps) {
            final int max = getItemsSize() - 1;
            int newIndex = currentIndex + steps;
            currentIndex = newIndex <= max ? newIndex : (isWrapAround() ? Spinner.wrapValue(newIndex, 0, max + 1) : max);
            setValue(_getValue(currentIndex));
        }



        /* *********************************************************************
         *                                                                     *
         * Private implementation                                              *
         *                                                                     *
         **********************************************************************/
        private int getItemsSize() {
            List<T> items = getItems();
            return items == null ? 0 : items.size();
        }

        private void updateCurrentIndex() {
            int itemsSize = getItemsSize();
            if (currentIndex < 0 || currentIndex >= itemsSize) {
                currentIndex = 0;
            }
            setValue(_getValue(currentIndex));
        }

        private T _getValue(int index) {
            List<T> items = getItems();
            return items == null ? null : (index >= 0 && index < items.size()) ? items.get(index) : null;
        }
    }



    /**
     * A {@link javafx.scene.control.SpinnerValueFactory} implementation designed to iterate through
     * integer values.
     *
     * <p>Note that the default {@link #converterProperty() converter} is implemented
     * as an {@link javafx.util.converter.IntegerStringConverter} instance.
     *
     * @since JavaFX 8u40
     */
    public static class IntegerSpinnerValueFactory extends SpinnerValueFactory<Integer> {

        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Constructs a new IntegerSpinnerValueFactory that sets the initial value
         * to be equal to the min value, and a default {@code amountToStepBy} of one.
         *
         * @param min The minimum allowed integer value for the Spinner.
         * @param max The maximum allowed integer value for the Spinner.
         */
        public IntegerSpinnerValueFactory(@NamedArg("min") int min,
                                          @NamedArg("max") int max) {
            this(min, max, min);
        }

        /**
         * Constructs a new IntegerSpinnerValueFactory with a default
         * {@code amountToStepBy} of one.
         *
         * @param min The minimum allowed integer value for the Spinner.
         * @param max The maximum allowed integer value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated, must
         *                     be within the bounds of the min and max arguments, or
         *                     else the min value will be used.
         */
        public IntegerSpinnerValueFactory(@NamedArg("min") int min,
                                          @NamedArg("max") int max,
                                          @NamedArg("initialValue") int initialValue) {
            this(min, max, initialValue, 1);
        }

        /**
         * Constructs a new IntegerSpinnerValueFactory.
         *
         * @param min The minimum allowed integer value for the Spinner.
         * @param max The maximum allowed integer value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated, must
         *                     be within the bounds of the min and max arguments, or
         *                     else the min value will be used.
         * @param amountToStepBy The amount to increment or decrement by, per step.
         */
        public IntegerSpinnerValueFactory(@NamedArg("min") int min,
                                          @NamedArg("max") int max,
                                          @NamedArg("initialValue") int initialValue,
                                          @NamedArg("amountToStepBy") int amountToStepBy) {
            setMin(min);
            setMax(max);
            setAmountToStepBy(amountToStepBy);
            setConverter(new IntegerStringConverter());

            valueProperty().addListener((o, oldValue, newValue) -> {
                // when the value is set, we need to react to ensure it is a
                // valid value (and if not, blow up appropriately)
                if (newValue < getMin()) {
                    setValue(getMin());
                } else if (newValue > getMax()) {
                    setValue(getMax());
                }
            });
            setValue(initialValue >= min && initialValue <= max ? initialValue : min);
        }


        /* *********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/

        // --- min
        private IntegerProperty min = new SimpleIntegerProperty(this, "min") {
            @Override protected void invalidated() {
                Integer currentValue = IntegerSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                int newMin = get();
                if (newMin > getMax()) {
                    setMin(getMax());
                    return;
                }

                if (currentValue < newMin) {
                    IntegerSpinnerValueFactory.this.setValue(newMin);
                }
            }
        };

        public final void setMin(int value) {
            min.set(value);
        }
        public final int getMin() {
            return min.get();
        }
        /**
         * Sets the minimum allowable value for this value factory
         * @return the minimum allowable value for this value factory
         */
        public final IntegerProperty minProperty() {
            return min;
        }

        // --- max
        private IntegerProperty max = new SimpleIntegerProperty(this, "max") {
            @Override protected void invalidated() {
                Integer currentValue = IntegerSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                int newMax = get();
                if (newMax < getMin()) {
                    setMax(getMin());
                    return;
                }

                if (currentValue > newMax) {
                    IntegerSpinnerValueFactory.this.setValue(newMax);
                }
            }
        };

        public final void setMax(int value) {
            max.set(value);
        }
        public final int getMax() {
            return max.get();
        }
        /**
         * Sets the maximum allowable value for this value factory
         * @return the maximum allowable value for this value factory
         */
        public final IntegerProperty maxProperty() {
            return max;
        }

        // --- amountToStepBy
        private IntegerProperty amountToStepBy = new SimpleIntegerProperty(this, "amountToStepBy");
        public final void setAmountToStepBy(int value) {
            amountToStepBy.set(value);
        }
        public final int getAmountToStepBy() {
            return amountToStepBy.get();
        }
        /**
         * Sets the amount to increment or decrement by, per step.
         * @return the amount to increment or decrement by, per step
         */
        public final IntegerProperty amountToStepByProperty() {
            return amountToStepBy;
        }



        /* *********************************************************************
         *                                                                     *
         * Overridden methods                                                  *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void decrement(int steps) {
            final int min = getMin();
            final int max = getMax();
            final int newIndex = getValue() - steps * getAmountToStepBy();
            setValue(newIndex >= min ? newIndex : (isWrapAround() ? Spinner.wrapValue(newIndex, min, max) + 1 : min));
        }

        /** {@inheritDoc} */
        @Override public void increment(int steps) {
            final int min = getMin();
            final int max = getMax();
            final int currentValue = getValue();
            final int newIndex = currentValue + steps * getAmountToStepBy();
            setValue(newIndex <= max ? newIndex : (isWrapAround() ? Spinner.wrapValue(newIndex, min, max) - 1 : max));
        }
    }



    /**
     * A {@link javafx.scene.control.SpinnerValueFactory} implementation designed to iterate through
     * double values.
     *
     * <p>Note that the default {@link #converterProperty() converter} is implemented
     * simply as shown below, which may be adequate in many cases, but it is important
     * for users to ensure that this suits their needs (and adjust when necessary). The
     * main point to note is that this {@link javafx.util.StringConverter} embeds
     * within it a {@link java.text.DecimalFormat} instance that shows the Double
     * to two decimal places. This is used for both the toString and fromString
     * methods:
     *
     * <pre>
     * setConverter(new StringConverter&lt;Double&gt;() {
     *     private final DecimalFormat df = new DecimalFormat("#.##");
     *
     *     &#064;Override public String toString(Double value) {
     *         // If the specified value is null, return a zero-length String
     *         if (value == null) {
     *             return "";
     *         }
     *
     *         return df.format(value);
     *     }
     *
     *     &#064;Override public Double fromString(String value) {
     *         try {
     *             // If the specified value is null or zero-length, return null
     *             if (value == null) {
     *                 return null;
     *             }
     *
     *             value = value.trim();
     *
     *             if (value.length() &lt; 1) {
     *                 return null;
     *             }
     *
     *             // Perform the requested parsing
     *             return df.parse(value).doubleValue();
     *         } catch (ParseException ex) {
     *             throw new RuntimeException(ex);
     *         }
     *     }
     * });</pre>
     *
     * @since JavaFX 8u40
     */
    public static class DoubleSpinnerValueFactory extends SpinnerValueFactory<Double> {

        /**
         * Constructs a new DoubleSpinnerValueFactory that sets the initial value
         * to be equal to the min value, and a default {@code amountToStepBy} of
         * one.
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         */
        public DoubleSpinnerValueFactory(@NamedArg("min") double min,
                                         @NamedArg("max") double max) {
            this(min, max, min);
        }

        /**
         * Constructs a new DoubleSpinnerValueFactory with a default
         * {@code amountToStepBy} of one.
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated, must
         *                     be within the bounds of the min and max arguments, or
         *                     else the min value will be used.
         */
        public DoubleSpinnerValueFactory(@NamedArg("min") double min,
                                         @NamedArg("max") double max,
                                         @NamedArg("initialValue") double initialValue) {
            this(min, max, initialValue, 1);
        }

        /**
         * Constructs a new DoubleSpinnerValueFactory.
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated, must
         *                     be within the bounds of the min and max arguments, or
         *                     else the min value will be used.
         * @param amountToStepBy The amount to increment or decrement by, per step.
         */
        public DoubleSpinnerValueFactory(@NamedArg("min") double min,
                                         @NamedArg("max") double max,
                                         @NamedArg("initialValue") double initialValue,
                                         @NamedArg("amountToStepBy") double amountToStepBy) {
            setMin(min);
            setMax(max);
            setAmountToStepBy(amountToStepBy);
            setConverter(new StringConverter<Double>() {
                private final DecimalFormat df = new DecimalFormat("#.##");

                @Override public String toString(Double value) {
                    // If the specified value is null, return a zero-length String
                    if (value == null) {
                        return "";
                    }

                    return df.format(value);
                }

                @Override public Double fromString(String value) {
                    try {
                        // If the specified value is null or zero-length, return null
                        if (value == null) {
                            return null;
                        }

                        value = value.trim();

                        if (value.length() < 1) {
                            return null;
                        }

                        // Perform the requested parsing
                        return df.parse(value).doubleValue();
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            valueProperty().addListener((o, oldValue, newValue) -> {
                if (newValue == null) return;

                // when the value is set, we need to react to ensure it is a
                // valid value (and if not, blow up appropriately)
                if (newValue < getMin()) {
                    setValue(getMin());
                } else if (newValue > getMax()) {
                    setValue(getMax());
                }
            });
            setValue(initialValue >= min && initialValue <= max ? initialValue : min);
        }



        /* *********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/

        // --- min
        private DoubleProperty min = new SimpleDoubleProperty(this, "min") {
            @Override protected void invalidated() {
                Double currentValue = DoubleSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final double newMin = get();
                if (newMin > getMax()) {
                    setMin(getMax());
                    return;
                }

                if (currentValue < newMin) {
                    DoubleSpinnerValueFactory.this.setValue(newMin);
                }
            }
        };

        public final void setMin(double value) {
            min.set(value);
        }
        public final double getMin() {
            return min.get();
        }
        /**
         * Sets the minimum allowable value for this value factory
         * @return the minimum allowable value for this value factory
         */
        public final DoubleProperty minProperty() {
            return min;
        }

        // --- max
        private DoubleProperty max = new SimpleDoubleProperty(this, "max") {
            @Override protected void invalidated() {
                Double currentValue = DoubleSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final double newMax = get();
                if (newMax < getMin()) {
                    setMax(getMin());
                    return;
                }

                if (currentValue > newMax) {
                    DoubleSpinnerValueFactory.this.setValue(newMax);
                }
            }
        };

        public final void setMax(double value) {
            max.set(value);
        }
        public final double getMax() {
            return max.get();
        }
        /**
         * Sets the maximum allowable value for this value factory
         * @return the maximum allowable value for this value factory
         */
        public final DoubleProperty maxProperty() {
            return max;
        }

        // --- amountToStepBy
        private DoubleProperty amountToStepBy = new SimpleDoubleProperty(this, "amountToStepBy");
        public final void setAmountToStepBy(double value) {
            amountToStepBy.set(value);
        }
        public final double getAmountToStepBy() {
            return amountToStepBy.get();
        }
        /**
         * Sets the amount to increment or decrement by, per step.
         * @return the amount to increment or decrement by, per step
         */
        public final DoubleProperty amountToStepByProperty() {
            return amountToStepBy;
        }



        /** {@inheritDoc} */
        @Override public void decrement(int steps) {
            final BigDecimal currentValue = BigDecimal.valueOf(getValue());
            final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
            final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
            final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
            BigDecimal newValue = currentValue.subtract(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
            setValue(newValue.compareTo(minBigDecimal) >= 0 ? newValue.doubleValue() :
                    (isWrapAround() ? Spinner.wrapValue(newValue, minBigDecimal, maxBigDecimal).doubleValue() : getMin()));
        }

        /** {@inheritDoc} */
        @Override public void increment(int steps) {
            final BigDecimal currentValue = BigDecimal.valueOf(getValue());
            final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
            final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
            final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
            BigDecimal newValue = currentValue.add(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
            setValue(newValue.compareTo(maxBigDecimal) <= 0 ? newValue.doubleValue() :
                    (isWrapAround() ? Spinner.wrapValue(newValue, minBigDecimal, maxBigDecimal).doubleValue() : getMax()));
        }
    }

    /**
     * A {@link javafx.scene.control.SpinnerValueFactory} implementation designed to iterate through
     * {@link java.time.LocalDate} values.
     *
     * <p>Note that the default {@link #converterProperty() converter} is implemented
     * simply as shown below, which may be adequate in many cases, but it is important
     * for users to ensure that this suits their needs (and adjust when necessary):
     *
     * <pre>
     * setConverter(new StringConverter&lt;LocalDate&gt;() {
     *     &#064;Override public String toString(LocalDate object) {
     *         if (object == null) {
     *             return "";
     *         }
     *         return object.toString();
     *     }
     *
     *     &#064;Override public LocalDate fromString(String string) {
     *         return LocalDate.parse(string);
     *     }
     * });</pre>
     */
    static class LocalDateSpinnerValueFactory extends SpinnerValueFactory<LocalDate> {

        /**
         * Creates a new instance of the LocalDateSpinnerValueFactory, using the
         * value returned by calling {@code LocalDate#now()} as the initial value,
         * and using a stepping amount of one day.
         */
        public LocalDateSpinnerValueFactory() {
            this(LocalDate.now());
        }

        /**
         * Creates a new instance of the LocalDateSpinnerValueFactory, using the
         * provided initial value, and a stepping amount of one day.
         *
         * @param initialValue The value of the Spinner when first instantiated.
         */
        public LocalDateSpinnerValueFactory(@NamedArg("initialValue") LocalDate initialValue) {
            this(LocalDate.MIN, LocalDate.MAX, initialValue);
        }

        /**
         * Creates a new instance of the LocalDateSpinnerValueFactory, using the
         * provided initial value, and a stepping amount of one day.
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated.
         */
        public LocalDateSpinnerValueFactory(@NamedArg("min") LocalDate min,
                                            @NamedArg("min") LocalDate max,
                                            @NamedArg("initialValue") LocalDate initialValue) {
            this(min, max, initialValue, 1, ChronoUnit.DAYS);
        }

        /**
         * Creates a new instance of the LocalDateSpinnerValueFactory, using the
         * provided min, max, and initial values, as well as the amount to step
         * by and {@link java.time.temporal.TemporalUnit}.
         *
         * <p>To better understand, here are a few examples:
         *
         * <ul>
         *     <li><strong>To step by one day from today: </strong> {@code new LocalDateSpinnerValueFactory(LocalDate.MIN, LocalDate.MAX, LocalDate.now(), 1, ChronoUnit.DAYS)}</li>
         *     <li><strong>To step by one month from today: </strong> {@code new LocalDateSpinnerValueFactory(LocalDate.MIN, LocalDate.MAX, LocalDate.now(), 1, ChronoUnit.MONTHS)}</li>
         *     <li><strong>To step by one year from today: </strong> {@code new LocalDateSpinnerValueFactory(LocalDate.MIN, LocalDate.MAX, LocalDate.now(), 1, ChronoUnit.YEARS)}</li>
         * </ul>
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated.
         * @param amountToStepBy The amount to increment or decrement by, per step.
         * @param temporalUnit The size of each step (e.g. day, week, month, year, etc)
         */
        public LocalDateSpinnerValueFactory(@NamedArg("min") LocalDate min,
                                            @NamedArg("min") LocalDate max,
                                            @NamedArg("initialValue") LocalDate initialValue,
                                            @NamedArg("amountToStepBy") long amountToStepBy,
                                            @NamedArg("temporalUnit") TemporalUnit temporalUnit) {
            setMin(min);
            setMax(max);
            setAmountToStepBy(amountToStepBy);
            setTemporalUnit(temporalUnit);
            setConverter(new StringConverter<LocalDate>() {
                @Override public String toString(LocalDate object) {
                    if (object == null) {
                        return "";
                    }
                    return object.toString();
                }

                @Override public LocalDate fromString(String string) {
                    return LocalDate.parse(string);
                }
            });

            valueProperty().addListener((o, oldValue, newValue) -> {
                // when the value is set, we need to react to ensure it is a
                // valid value (and if not, blow up appropriately)
                if (getMin() != null && newValue.isBefore(getMin())) {
                    setValue(getMin());
                } else if (getMax() != null && newValue.isAfter(getMax())) {
                    setValue(getMax());
                }
            });
            setValue(initialValue != null ? initialValue : LocalDate.now());
        }



        /* *********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/

        // --- min
        private ObjectProperty<LocalDate> min = new SimpleObjectProperty<LocalDate>(this, "min") {
            @Override protected void invalidated() {
                LocalDate currentValue = LocalDateSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final LocalDate newMin = get();
                if (newMin.isAfter(getMax())) {
                    setMin(getMax());
                    return;
                }

                if (currentValue.isBefore(newMin)) {
                    LocalDateSpinnerValueFactory.this.setValue(newMin);
                }
            }
        };

        public final void setMin(LocalDate value) {
            min.set(value);
        }
        public final LocalDate getMin() {
            return min.get();
        }
        /**
         * Sets the minimum allowable value for this value factory
         */
        public final ObjectProperty<LocalDate> minProperty() {
            return min;
        }

        // --- max
        private ObjectProperty<LocalDate> max = new SimpleObjectProperty<LocalDate>(this, "max") {
            @Override protected void invalidated() {
                LocalDate currentValue = LocalDateSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final LocalDate newMax = get();
                if (newMax.isBefore(getMin())) {
                    setMax(getMin());
                    return;
                }

                if (currentValue.isAfter(newMax)) {
                    LocalDateSpinnerValueFactory.this.setValue(newMax);
                }
            }
        };

        public final void setMax(LocalDate value) {
            max.set(value);
        }
        public final LocalDate getMax() {
            return max.get();
        }
        /**
         * Sets the maximum allowable value for this value factory
         */
        public final ObjectProperty<LocalDate> maxProperty() {
            return max;
        }

        // --- temporalUnit
        private ObjectProperty<TemporalUnit> temporalUnit = new SimpleObjectProperty<>(this, "temporalUnit");
        public final void setTemporalUnit(TemporalUnit value) {
            temporalUnit.set(value);
        }
        public final TemporalUnit getTemporalUnit() {
            return temporalUnit.get();
        }
        /**
         * The size of each step (e.g. day, week, month, year, etc).
         */
        public final ObjectProperty<TemporalUnit> temporalUnitProperty() {
            return temporalUnit;
        }

        // --- amountToStepBy
        private LongProperty amountToStepBy = new SimpleLongProperty(this, "amountToStepBy");
        public final void setAmountToStepBy(long value) {
            amountToStepBy.set(value);
        }
        public final long getAmountToStepBy() {
            return amountToStepBy.get();
        }
        /**
         * Sets the amount to increment or decrement by, per step.
         */
        public final LongProperty amountToStepByProperty() {
            return amountToStepBy;
        }



        /* *********************************************************************
         *                                                                     *
         * Overridden methods                                                  *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void decrement(int steps) {
            final LocalDate currentValue = getValue();
            final LocalDate min = getMin();
            LocalDate newValue = currentValue.minus(getAmountToStepBy() * steps, getTemporalUnit());

            if (min != null && isWrapAround() && newValue.isBefore(min)) {
                // we need to wrap around
                newValue = getMax();
            }

            setValue(newValue);
        }

        /** {@inheritDoc} */
        @Override public void increment(int steps) {
            final LocalDate currentValue = getValue();
            final LocalDate max = getMax();
            LocalDate newValue = currentValue.plus(getAmountToStepBy() * steps, getTemporalUnit());

            if (max != null && isWrapAround() && newValue.isAfter(max)) {
                // we need to wrap around
                newValue = getMin();
            }

            setValue(newValue);
        }
    }





    /**
     * A {@link javafx.scene.control.SpinnerValueFactory} implementation designed to iterate through
     * {@link java.time.LocalTime} values.
     *
     * <p>Note that the default {@link #converterProperty() converter} is implemented
     * simply as shown below, which may be adequate in many cases, but it is important
     * for users to ensure that this suits their needs (and adjust when necessary):
     *
     * <pre>
     * setConverter(new StringConverter&lt;LocalTime&gt;() {
     *     &#064;Override public String toString(LocalTime object) {
     *         if (object == null) {
     *             return "";
     *         }
     *         return object.toString();
     *     }
     *
     *     &#064;Override public LocalTime fromString(String string) {
     *         return LocalTime.parse(string);
     *     }
     * });</pre>
     */
    static class LocalTimeSpinnerValueFactory extends SpinnerValueFactory<LocalTime> {

        /**
         * Creates a new instance of the LocalTimepinnerValueFactory, using the
         * value returned by calling {@code LocalTime#now()} as the initial value,
         * and using a stepping amount of one day.
         */
        public LocalTimeSpinnerValueFactory() {
            this(LocalTime.now());
        }

        /**
         * Creates a new instance of the LocalTimeSpinnerValueFactory, using the
         * provided initial value, and a stepping amount of one hour.
         *
         * @param initialValue The value of the Spinner when first instantiated.
         */
        public LocalTimeSpinnerValueFactory(@NamedArg("initialValue") LocalTime initialValue) {
            this(LocalTime.MIN, LocalTime.MAX, initialValue);
        }

        /**
         * Creates a new instance of the LocalTimeSpinnerValueFactory, using the
         * provided initial value, and a stepping amount of one hour.
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated.
         */
        public LocalTimeSpinnerValueFactory(@NamedArg("min") LocalTime min,
                                            @NamedArg("min") LocalTime max,
                                            @NamedArg("initialValue") LocalTime initialValue) {
            this(min, max, initialValue, 1, ChronoUnit.HOURS);
        }

        /**
         * Creates a new instance of the LocalTimeSpinnerValueFactory, using the
         * provided min, max, and initial values, as well as the amount to step
         * by and {@link java.time.temporal.TemporalUnit}.
         *
         * <p>To better understand, here are a few examples:
         *
         * <ul>
         *     <li><strong>To step by one hour from the current time: </strong> {@code new LocalTimeSpinnerValueFactory(LocalTime.MIN, LocalTime.MAX, LocalTime.now(), 1, ChronoUnit.HOURS)}</li>
         *     <li><strong>To step by one minute from the current time: </strong> {@code new LocalTimeSpinnerValueFactory(LocalTime.MIN, LocalTime.MAX, LocalTime.now(), 1, ChronoUnit.MINUTES)}</li>
         * </ul>
         *
         * @param min The minimum allowed double value for the Spinner.
         * @param max The maximum allowed double value for the Spinner.
         * @param initialValue The value of the Spinner when first instantiated.
         * @param amountToStepBy The amount to increment or decrement by, per step.
         * @param temporalUnit The size of each step (e.g. day, week, month, year, etc)
         */
        public LocalTimeSpinnerValueFactory(@NamedArg("min") LocalTime min,
                                            @NamedArg("min") LocalTime max,
                                            @NamedArg("initialValue") LocalTime initialValue,
                                            @NamedArg("amountToStepBy") long amountToStepBy,
                                            @NamedArg("temporalUnit") TemporalUnit temporalUnit) {
            setMin(min);
            setMax(max);
            setAmountToStepBy(amountToStepBy);
            setTemporalUnit(temporalUnit);
            setConverter(new StringConverter<LocalTime>() {
                private DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

                @Override public String toString(LocalTime localTime) {
                    if (localTime == null) {
                        return "";
                    }
                    return localTime.format(dtf);
                }

                @Override public LocalTime fromString(String string) {
                    return LocalTime.parse(string);
                }
            });

            valueProperty().addListener((o, oldValue, newValue) -> {
                // when the value is set, we need to react to ensure it is a
                // valid value (and if not, blow up appropriately)
                if (getMin() != null && newValue.isBefore(getMin())) {
                    setValue(getMin());
                } else if (getMax() != null && newValue.isAfter(getMax())) {
                    setValue(getMax());
                }
            });
            setValue(initialValue != null ? initialValue : LocalTime.now());
        }



        /* *********************************************************************
         *                                                                     *
         * Properties                                                          *
         *                                                                     *
         **********************************************************************/

        // --- min
        private ObjectProperty<LocalTime> min = new SimpleObjectProperty<LocalTime>(this, "min") {
            @Override protected void invalidated() {
                LocalTime currentValue = LocalTimeSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final LocalTime newMin = get();
                if (newMin.isAfter(getMax())) {
                    setMin(getMax());
                    return;
                }

                if (currentValue.isBefore(newMin)) {
                    LocalTimeSpinnerValueFactory.this.setValue(newMin);
                }
            }
        };

        public final void setMin(LocalTime value) {
            min.set(value);
        }
        public final LocalTime getMin() {
            return min.get();
        }
        /**
         * Sets the minimum allowable value for this value factory
         */
        public final ObjectProperty<LocalTime> minProperty() {
            return min;
        }

        // --- max
        private ObjectProperty<LocalTime> max = new SimpleObjectProperty<LocalTime>(this, "max") {
            @Override protected void invalidated() {
                LocalTime currentValue = LocalTimeSpinnerValueFactory.this.getValue();
                if (currentValue == null) {
                    return;
                }

                final LocalTime newMax = get();
                if (newMax.isBefore(getMin())) {
                    setMax(getMin());
                    return;
                }

                if (currentValue.isAfter(newMax)) {
                    LocalTimeSpinnerValueFactory.this.setValue(newMax);
                }
            }
        };

        public final void setMax(LocalTime value) {
            max.set(value);
        }
        public final LocalTime getMax() {
            return max.get();
        }
        /**
         * Sets the maximum allowable value for this value factory
         */
        public final ObjectProperty<LocalTime> maxProperty() {
            return max;
        }

        // --- temporalUnit
        private ObjectProperty<TemporalUnit> temporalUnit = new SimpleObjectProperty<>(this, "temporalUnit");
        public final void setTemporalUnit(TemporalUnit value) {
            temporalUnit.set(value);
        }
        public final TemporalUnit getTemporalUnit() {
            return temporalUnit.get();
        }
        /**
         * The size of each step (e.g. day, week, month, year, etc).
         */
        public final ObjectProperty<TemporalUnit> temporalUnitProperty() {
            return temporalUnit;
        }

        // --- amountToStepBy
        private LongProperty amountToStepBy = new SimpleLongProperty(this, "amountToStepBy");
        public final void setAmountToStepBy(long value) {
            amountToStepBy.set(value);
        }
        public final long getAmountToStepBy() {
            return amountToStepBy.get();
        }
        /**
         * Sets the amount to increment or decrement by, per step.
         */
        public final LongProperty amountToStepByProperty() {
            return amountToStepBy;
        }



        /* *********************************************************************
         *                                                                     *
         * Overridden methods                                                  *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void decrement(int steps) {
            final LocalTime currentValue = getValue();
            final LocalTime min = getMin();

            final Duration duration = Duration.of(getAmountToStepBy() * steps, getTemporalUnit());

            final long durationInSeconds = duration.toMinutes() * 60;
            final long currentValueInSeconds = currentValue.toSecondOfDay();

            if (! isWrapAround() && durationInSeconds > currentValueInSeconds) {
                setValue(min == null ? LocalTime.MIN : min);
            } else {
                setValue(currentValue.minus(duration));
            }
        }

        /** {@inheritDoc} */
        @Override public void increment(int steps) {
            final LocalTime currentValue = getValue();
            final LocalTime max = getMax();

            final Duration duration = Duration.of(getAmountToStepBy() * steps, getTemporalUnit());

            final long durationInSeconds = duration.toMinutes() * 60;
            final long currentValueInSeconds = currentValue.toSecondOfDay();

            if (! isWrapAround() && durationInSeconds > (LocalTime.MAX.toSecondOfDay() - currentValueInSeconds)) {
                setValue(max == null ? LocalTime.MAX : max);
            } else {
                setValue(currentValue.plus(duration));
            }
        }
    }
}
