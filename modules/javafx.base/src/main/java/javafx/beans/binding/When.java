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

import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.Observable;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.javafx.binding.DoubleConstant;
import com.sun.javafx.binding.FloatConstant;
import com.sun.javafx.binding.IntegerConstant;
import com.sun.javafx.binding.Logging;
import com.sun.javafx.binding.LongConstant;

/**
 * Starting point for a binding that calculates a ternary expression.
 * <p>
 * A ternary expression has the basic form
 * {@code new When(cond).then(value1).otherwise(value2);}. The expression
 * {@code cond} needs to be a {@link javafx.beans.value.ObservableBooleanValue}.
 * Based on the value of {@code cond}, the binding contains the value of
 * {@code value1} (if {@code cond.getValue() == true}) or {@code value2} (if
 * {@code cond.getValue() == false}). The values {@code value1} and
 * {@code value2} have to be of the same type. They can be constant values or
 * implementations of {@link javafx.beans.value.ObservableValue}.
 * @since JavaFX 2.0
 */
public class When {
    private final ObservableBooleanValue condition;

    /**
     * The constructor of {@code When}.
     *
     * @param condition
     *            the condition of the ternary expression
     */
    public When(final @NamedArg("condition") ObservableBooleanValue condition) {
        if (condition == null) {
            throw new NullPointerException("Condition must be specified.");
        }
        this.condition = condition;
    }

    private static class WhenListener implements InvalidationListener {

        private final ObservableBooleanValue condition;
        private final ObservableValue<?> thenValue;
        private final ObservableValue<?> otherwiseValue;
        private final WeakReference<Binding<?>> ref;

        private WhenListener(Binding<?> binding, ObservableBooleanValue condition, ObservableValue<?> thenValue, ObservableValue<?> otherwiseValue) {
            this.ref = new WeakReference<>(binding);
            this.condition = condition;
            this.thenValue = thenValue;
            this.otherwiseValue = otherwiseValue;
        }

        @Override
        public void invalidated(Observable observable) {
            final Binding<?> binding = ref.get();
            if (binding == null) {
                condition.removeListener(this);
                if (thenValue != null) {
                    thenValue.removeListener(this);
                }
                if (otherwiseValue != null) {
                    otherwiseValue.removeListener(this);
                }
            } else {
                // short-circuit invalidation. This Binding becomes
                // only invalid if the condition changes or the
                // active branch.
                if (condition.equals(observable) || (binding.isValid() && (condition.get() == observable.equals(thenValue)))) {
                    binding.invalidate();
                }
            }
        }

    }

    private static NumberBinding createNumberCondition(
            final ObservableBooleanValue condition,
            final ObservableNumberValue thenValue,
            final ObservableNumberValue otherwiseValue) {
        if ((thenValue instanceof ObservableDoubleValue) || (otherwiseValue instanceof ObservableDoubleValue)) {
            return new DoubleBinding() {
                final InvalidationListener observer = new WhenListener(this, condition, thenValue, otherwiseValue);
                {
                    condition.addListener(observer);
                    thenValue.addListener(observer);
                    otherwiseValue.addListener(observer);
                }

                @Override
                public void dispose() {
                    condition.removeListener(observer);
                    thenValue.removeListener(observer);
                    otherwiseValue.removeListener(observer);
                }

                @Override
                protected double computeValue() {
                    final boolean conditionValue = condition.get();
                    Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
                    return conditionValue ? thenValue.doubleValue() : otherwiseValue.doubleValue();
                }

                @Override
                public ObservableList<ObservableValue<?>> getDependencies() {
                    return FXCollections.unmodifiableObservableList(
                            FXCollections.<ObservableValue<?>> observableArrayList(condition, thenValue, otherwiseValue));
                }
            };
        } else if ((thenValue instanceof ObservableFloatValue) || (otherwiseValue instanceof ObservableFloatValue)) {
            return new FloatBinding() {
                final InvalidationListener observer = new WhenListener(this, condition, thenValue, otherwiseValue);
                {
                    condition.addListener(observer);
                    thenValue.addListener(observer);
                    otherwiseValue.addListener(observer);
                }

                @Override
                public void dispose() {
                    condition.removeListener(observer);
                    thenValue.removeListener(observer);
                    otherwiseValue.removeListener(observer);
                }

                @Override
                protected float computeValue() {
                    final boolean conditionValue = condition.get();
                    Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
                    return conditionValue ? thenValue.floatValue() : otherwiseValue.floatValue();
                }

                @Override
                public ObservableList<ObservableValue<?>> getDependencies() {
                    return FXCollections.unmodifiableObservableList(
                            FXCollections.<ObservableValue<?>> observableArrayList(condition, thenValue, otherwiseValue));
                }
            };
        } else if ((thenValue instanceof ObservableLongValue) || (otherwiseValue instanceof ObservableLongValue)) {
            return new LongBinding() {
                final InvalidationListener observer = new WhenListener(this, condition, thenValue, otherwiseValue);
                {
                    condition.addListener(observer);
                    thenValue.addListener(observer);
                    otherwiseValue.addListener(observer);
                }

                @Override
                public void dispose() {
                    condition.removeListener(observer);
                    thenValue.removeListener(observer);
                    otherwiseValue.removeListener(observer);
                }

                @Override
                protected long computeValue() {
                    final boolean conditionValue = condition.get();
                    Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
                    return conditionValue ? thenValue.longValue() : otherwiseValue.longValue();
                }

                @Override
                public ObservableList<ObservableValue<?>> getDependencies() {
                    return FXCollections.unmodifiableObservableList(
                            FXCollections.<ObservableValue<?>> observableArrayList(condition, thenValue, otherwiseValue));
                }
            };
        } else {
            return new IntegerBinding() {
                final InvalidationListener observer = new WhenListener(this, condition, thenValue, otherwiseValue);
                {
                    condition.addListener(observer);
                    thenValue.addListener(observer);
                    otherwiseValue.addListener(observer);
                }

                @Override
                public void dispose() {
                    condition.removeListener(observer);
                    thenValue.removeListener(observer);
                    otherwiseValue.removeListener(observer);
                }

                @Override
                protected int computeValue() {
                    final boolean conditionValue = condition.get();
                    Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
                    return conditionValue ? thenValue.intValue(): otherwiseValue.intValue();
                }

                @Override
                public ObservableList<ObservableValue<?>> getDependencies() {
                    return FXCollections.unmodifiableObservableList(
                            FXCollections.<ObservableValue<?>> observableArrayList(condition, thenValue, otherwiseValue));
                }
            };
        }
    }

    /**
     * If-then-else expression returning a number.
     * @since JavaFX 2.0
     */
    public class NumberConditionBuilder {

        private ObservableNumberValue thenValue;

        private NumberConditionBuilder(final ObservableNumberValue thenValue) {
            this.thenValue = thenValue;
        }

        /**
         * Defines the {@link javafx.beans.value.ObservableNumberValue} which
         * value is returned by the ternary expression if the condition is
         * {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link DoubleBinding}
         */
        public NumberBinding otherwise(ObservableNumberValue otherwiseValue) {
            if (otherwiseValue == null) {
                throw new NullPointerException("Value needs to be specified");
            }
            return When.createNumberCondition(condition, thenValue, otherwiseValue);
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link DoubleBinding}
         */
        public DoubleBinding otherwise(double otherwiseValue) {
            return (DoubleBinding) otherwise(DoubleConstant.valueOf(otherwiseValue));
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link NumberBinding}
         */
        public NumberBinding otherwise(float otherwiseValue) {
            return otherwise(FloatConstant.valueOf(otherwiseValue));
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link NumberBinding}
         */
        public NumberBinding otherwise(long otherwiseValue) {
            return otherwise(LongConstant.valueOf(otherwiseValue));
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link NumberBinding}
         */
        public NumberBinding otherwise(int otherwiseValue) {
            return otherwise(IntegerConstant.valueOf(otherwiseValue));
        }
    }

    /**
     * Defines the {@link javafx.beans.value.ObservableNumberValue} which value
     * is returned by the ternary expression if the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public NumberConditionBuilder then(final ObservableNumberValue thenValue) {
        if (thenValue == null) {
            throw new NullPointerException("Value needs to be specified");
        }
        return new NumberConditionBuilder(thenValue);
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public NumberConditionBuilder then(double thenValue) {
        return new NumberConditionBuilder(DoubleConstant.valueOf(thenValue));
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public NumberConditionBuilder then(float thenValue) {
        return new NumberConditionBuilder(FloatConstant.valueOf(thenValue));
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public NumberConditionBuilder then(long thenValue) {
        return new NumberConditionBuilder(LongConstant.valueOf(thenValue));
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public NumberConditionBuilder then(int thenValue) {
        return new NumberConditionBuilder(IntegerConstant.valueOf(thenValue));
    }

    /**
     * If-then-else expression returning Boolean.
     */
    private class BooleanCondition extends BooleanBinding {
        private final ObservableBooleanValue trueResult;
        private final boolean trueResultValue;

        private final ObservableBooleanValue falseResult;
        private final boolean falseResultValue;

        private final InvalidationListener observer;

        private BooleanCondition(final ObservableBooleanValue then, final ObservableBooleanValue otherwise) {
            this.trueResult = then;
            this.trueResultValue = false;
            this.falseResult = otherwise;
            this.falseResultValue = false;
            this.observer = new WhenListener(this, condition, then, otherwise);
            condition.addListener(observer);
            then.addListener(observer);
            otherwise.addListener(observer);
        }

        private BooleanCondition(final boolean then, final ObservableBooleanValue otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = otherwise;
            this.falseResultValue = false;
            this.observer = new WhenListener(this, condition, null, otherwise);
            condition.addListener(observer);
            otherwise.addListener(observer);
        }

        private BooleanCondition(final ObservableBooleanValue then, final boolean otherwise) {
            this.trueResult = then;
            this.trueResultValue = false;
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = new WhenListener(this, condition, then, null);
            condition.addListener(observer);
            then.addListener(observer);
        }

        private BooleanCondition(final boolean then, final boolean otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = null;
            super.bind(condition);
        }

        @Override
        protected boolean computeValue() {
            final boolean conditionValue = condition.get();
            Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
            return conditionValue ? (trueResult != null ? trueResult.get() : trueResultValue)
                    : (falseResult != null ? falseResult.get() : falseResultValue);
        }

        @Override
        public void dispose() {
            if (observer == null) {
                super.unbind(condition);
            } else {
                condition.removeListener(observer);
                if (trueResult != null) {
                    trueResult.removeListener(observer);
                }
                if (falseResult != null) {
                    falseResult.removeListener(observer);
                }
            }
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            assert condition != null;
            final ObservableList<ObservableValue<?>> seq = FXCollections.<ObservableValue<?>> observableArrayList(condition);
            if (trueResult != null) {
                seq.add(trueResult);
            }
            if (falseResult != null) {
                seq.add(falseResult);
            }
            return FXCollections.unmodifiableObservableList(seq);
        }
    }

    /**
     * An intermediate class needed while assembling the ternary expression. It
     * should not be used in another context.
     * @since JavaFX 2.0
     */
    public class BooleanConditionBuilder {

        private ObservableBooleanValue trueResult;
        private boolean trueResultValue;

        private BooleanConditionBuilder(final ObservableBooleanValue thenValue) {
            this.trueResult = thenValue;
        }

        private BooleanConditionBuilder(final boolean thenValue) {
            this.trueResultValue = thenValue;
        }

        /**
         * Defines the {@link javafx.beans.value.ObservableBooleanValue} which
         * value is returned by the ternary expression if the condition is
         * {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link BooleanBinding}
         */
        public BooleanBinding otherwise(final ObservableBooleanValue otherwiseValue) {
            if (otherwiseValue == null) {
                throw new NullPointerException("Value needs to be specified");
            }
            if (trueResult != null)
                return new BooleanCondition(trueResult, otherwiseValue);
            else
                return new BooleanCondition(trueResultValue, otherwiseValue);
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link BooleanBinding}
         */
        public BooleanBinding otherwise(final boolean otherwiseValue) {
            if (trueResult != null)
                return new BooleanCondition(trueResult, otherwiseValue);
            else
                return new BooleanCondition(trueResultValue, otherwiseValue);
        }
    }

    /**
     * Defines the {@link javafx.beans.value.ObservableBooleanValue} which value
     * is returned by the ternary expression if the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public BooleanConditionBuilder then(final ObservableBooleanValue thenValue) {
        if (thenValue == null) {
            throw new NullPointerException("Value needs to be specified");
        }
        return new BooleanConditionBuilder(thenValue);
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public BooleanConditionBuilder then(final boolean thenValue) {
        return new BooleanConditionBuilder(thenValue);
    }

    /**
     * If-then-else expression returning String.
     */
    private class StringCondition extends StringBinding {

        private final ObservableStringValue trueResult;
        private final String trueResultValue;

        private final ObservableStringValue falseResult;
        private final String falseResultValue;

        private final InvalidationListener observer;

        private StringCondition(final ObservableStringValue then, final ObservableStringValue otherwise) {
            this.trueResult = then;
            this.trueResultValue = "";
            this.falseResult = otherwise;
            this.falseResultValue = "";
            this.observer = new WhenListener(this, condition, then, otherwise);
            condition.addListener(observer);
            then.addListener(observer);
            otherwise.addListener(observer);
        }

        private StringCondition(final String then, final ObservableStringValue otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = otherwise;
            this.falseResultValue = "";
            this.observer = new WhenListener(this, condition, null, otherwise);
            condition.addListener(observer);
            otherwise.addListener(observer);
        }

        private StringCondition(final ObservableStringValue then, final String otherwise) {
            this.trueResult = then;
            this.trueResultValue = "";
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = new WhenListener(this, condition, then, null);
            condition.addListener(observer);
            then.addListener(observer);
        }

        private StringCondition(final String then, final String otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = null;
            super.bind(condition);
        }

        @Override
        protected String computeValue() {
            final boolean conditionValue = condition.get();
            Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
            return conditionValue ? (trueResult != null ? trueResult.get() : trueResultValue)
                    : (falseResult != null ? falseResult.get() : falseResultValue);
        }

        @Override
        public void dispose() {
            if (observer == null) {
                super.unbind(condition);
            } else {
                condition.removeListener(observer);
                if (trueResult != null) {
                    trueResult.removeListener(observer);
                }
                if (falseResult != null) {
                    falseResult.removeListener(observer);
                }
            }
        }


        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            assert condition != null;
            final ObservableList<ObservableValue<?>> seq = FXCollections.<ObservableValue<?>> observableArrayList(condition);
            if (trueResult != null) {
                seq.add(trueResult);
            }
            if (falseResult != null) {
                seq.add(falseResult);
            }
            return FXCollections.unmodifiableObservableList(seq);
        }
    }

    /**
     * An intermediate class needed while assembling the ternary expression. It
     * should not be used in another context.
     * @since JavaFX 2.0
     */
    public class StringConditionBuilder {

        private ObservableStringValue trueResult;
        private String trueResultValue;

        private StringConditionBuilder(final ObservableStringValue thenValue) {
            this.trueResult = thenValue;
        }

        private StringConditionBuilder(final String thenValue) {
            this.trueResultValue = thenValue;
        }

        /**
         * Defines the {@link javafx.beans.value.ObservableStringValue} which
         * value is returned by the ternary expression if the condition is
         * {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link StringBinding}
         */
        public StringBinding otherwise(final ObservableStringValue otherwiseValue) {
            if (trueResult != null)
                return new StringCondition(trueResult, otherwiseValue);
            else
                return new StringCondition(trueResultValue, otherwiseValue);
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link StringBinding}
         */
        public StringBinding otherwise(final String otherwiseValue) {
            if (trueResult != null)
                return new StringCondition(trueResult, otherwiseValue);
            else
                return new StringCondition(trueResultValue, otherwiseValue);
        }
    }

    /**
     * Defines the {@link javafx.beans.value.ObservableStringValue} which value
     * is returned by the ternary expression if the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public StringConditionBuilder then(final ObservableStringValue thenValue) {
        if (thenValue == null) {
            throw new NullPointerException("Value needs to be specified");
        }
        return new StringConditionBuilder(thenValue);
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public StringConditionBuilder then(final String thenValue) {
        return new StringConditionBuilder(thenValue);
    }

    /**
     * If-then-else expression returning general objects.
     */
    private class ObjectCondition<T> extends ObjectBinding<T> {

        private final ObservableObjectValue<T> trueResult;
        private final T trueResultValue;

        private final ObservableObjectValue<T> falseResult;
        private final T falseResultValue;

        private final InvalidationListener observer;

        private ObjectCondition(final ObservableObjectValue<T> then, final ObservableObjectValue<T> otherwise) {
            this.trueResult = then;
            this.trueResultValue = null;
            this.falseResult = otherwise;
            this.falseResultValue = null;
            this.observer = new WhenListener(this, condition, then, otherwise);
            condition.addListener(observer);
            then.addListener(observer);
            otherwise.addListener(observer);
        }

        private ObjectCondition(final T then, final ObservableObjectValue<T> otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = otherwise;
            this.falseResultValue = null;
            this.observer = new WhenListener(this, condition, null, otherwise);
            condition.addListener(observer);
            otherwise.addListener(observer);
        }

        private ObjectCondition(final ObservableObjectValue<T> then, final T otherwise) {
            this.trueResult = then;
            this.trueResultValue = null;
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = new WhenListener(this, condition, then, null);
            condition.addListener(observer);
            then.addListener(observer);
        }

        private ObjectCondition(final T then, final T otherwise) {
            this.trueResult = null;
            this.trueResultValue = then;
            this.falseResult = null;
            this.falseResultValue = otherwise;
            this.observer = null;
            super.bind(condition);
        }

        @Override
        protected T computeValue() {
            final boolean conditionValue = condition.get();
            Logging.getLogger().finest("Condition of ternary binding expression was evaluated: {0}", conditionValue);
            return conditionValue ? (trueResult != null ? trueResult.get() : trueResultValue)
                    : (falseResult != null ? falseResult.get() : falseResultValue);
        }

        @Override
        public void dispose() {
            if (observer == null) {
                super.unbind(condition);
            } else {
                condition.removeListener(observer);
                if (trueResult != null) {
                    trueResult.removeListener(observer);
                }
                if (falseResult != null) {
                    falseResult.removeListener(observer);
                }
            }
        }


        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            assert condition != null;
            final ObservableList<ObservableValue<?>> seq = FXCollections.<ObservableValue<?>> observableArrayList(condition);
            if (trueResult != null) {
                seq.add(trueResult);
            }
            if (falseResult != null) {
                seq.add(falseResult);
            }
            return FXCollections.unmodifiableObservableList(seq);
        }
    }

    /**
     * An intermediate class needed while assembling the ternary expression. It
     * should not be used in another context.
     * @since JavaFX 2.0
     */
    public class ObjectConditionBuilder<T> {

        private ObservableObjectValue<T> trueResult;
        private T trueResultValue;

        private ObjectConditionBuilder(final ObservableObjectValue<T> thenValue) {
            this.trueResult = thenValue;
        }

        private ObjectConditionBuilder(final T thenValue) {
            this.trueResultValue = thenValue;
        }

        /**
         * Defines the {@link javafx.beans.value.ObservableObjectValue} which
         * value is returned by the ternary expression if the condition is
         * {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link ObjectBinding}
         */
        public ObjectBinding<T> otherwise(final ObservableObjectValue<T> otherwiseValue) {
            if (otherwiseValue == null) {
                throw new NullPointerException("Value needs to be specified");
            }
            if (trueResult != null)
                return new ObjectCondition<>(trueResult, otherwiseValue);
            else
                return new ObjectCondition<>(trueResultValue, otherwiseValue);
        }

        /**
         * Defines a constant value of the ternary expression, that is returned
         * if the condition is {@code false}.
         *
         * @param otherwiseValue
         *            the value
         * @return the complete {@link ObjectBinding}
         */
        public ObjectBinding<T> otherwise(final T otherwiseValue) {
            if (trueResult != null)
                return new ObjectCondition<>(trueResult, otherwiseValue);
            else
                return new ObjectCondition<>(trueResultValue, otherwiseValue);
        }
    }

    /**
     * Defines the {@link javafx.beans.value.ObservableObjectValue} which value
     * is returned by the ternary expression if the condition is {@code true}.
     *
     * @param <T> the type of the intermediate result
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public <T> ObjectConditionBuilder<T> then(final ObservableObjectValue<T> thenValue) {
        if (thenValue == null) {
            throw new NullPointerException("Value needs to be specified");
        }
        return new ObjectConditionBuilder<>(thenValue);
    }

    /**
     * Defines a constant value of the ternary expression, that is returned if
     * the condition is {@code true}.
     *
     * @param <T> the type of the intermediate result
     * @param thenValue
     *            the value
     * @return the intermediate result which still requires the otherwise-branch
     */
    public <T> ObjectConditionBuilder<T> then(final T thenValue) {
        return new ObjectConditionBuilder<>(thenValue);
    }

}
