/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.Arrays;

/**
 * A convenience class for creating implementations of {@link javafx.beans.value.ObservableValue}.
 * It contains all of the infrastructure support for value invalidation- and
 * change event notification.<p>
 *
 * This implementation can handle adding and removing listeners while the
 * observers are being notified, but it is not thread-safe.<p>
 *
 * This class keeps track of the latest value it has seen to determine if change
 * listeners should be called when next {@link #fireValueChangedEvent()} is called.
 * So while this value is usually the current value of the involved observable,
 * it becomes the "old" value as soon as the observable is changed, until such time
 * it is updated again (by calling {@link #fireValueChangedEvent()}).<p>
 *
 * During this brief period, listeners may be added or removed causing the ExpressionHelper
 * to perhaps switch to a different variant of itself. These different variants must be
 * made aware of the currently stored latest value, as obtaining this value from the
 * {@link ObservableValue} would (during that brief period) be a different value. Using
 * the incorrect latest value would result in change listeners not being fired as they
 * perform an equality check.
 */
public abstract class ExpressionHelper<T> extends ExpressionHelperBase {

    //------------------------------------------------------------------------------------------------------------------
    // Static methods

    public static <T> ExpressionHelper<T> addListener(ExpressionHelper<T> helper, ObservableValue<T> observable, InvalidationListener listener) {
        if ((observable == null) || (listener == null)) {
            throw new NullPointerException();
        }
        observable.getValue(); // validate observable
        return (helper == null)? new SingleInvalidation<>(observable, listener) : helper.addListener(listener);
    }

    public static <T> ExpressionHelper<T> removeListener(ExpressionHelper<T> helper, InvalidationListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        return (helper == null)? null : helper.removeListener(listener);
    }

    public static <T> ExpressionHelper<T> addListener(ExpressionHelper<T> helper, ObservableValue<T> observable, ChangeListener<? super T> listener) {
        if ((observable == null) || (listener == null)) {
            throw new NullPointerException();
        }
        return (helper == null)? new SingleChange<>(observable, observable.getValue(), listener) : helper.addListener(listener);
    }

    public static <T> ExpressionHelper<T> removeListener(ExpressionHelper<T> helper, ChangeListener<? super T> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        return (helper == null)? null : helper.removeListener(listener);
    }

    public static <T> void fireValueChangedEvent(ExpressionHelper<T> helper) {
        if (helper != null) {
            helper.fireValueChangedEvent();
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // Common implementations

    protected final ObservableValue<T> observable;

    private ExpressionHelper(ObservableValue<T> observable) {
        this.observable = observable;
    }

    protected abstract ExpressionHelper<T> addListener(InvalidationListener listener);
    protected abstract ExpressionHelper<T> removeListener(InvalidationListener listener);

    protected abstract ExpressionHelper<T> addListener(ChangeListener<? super T> listener);
    protected abstract ExpressionHelper<T> removeListener(ChangeListener<? super T> listener);

    protected abstract void fireValueChangedEvent();

    //------------------------------------------------------------------------------------------------------------------
    // Implementations

    private static class SingleInvalidation<T> extends ExpressionHelper<T> {

        private final InvalidationListener listener;

        private SingleInvalidation(ObservableValue<T> expression, InvalidationListener listener) {
            super(expression);
            this.listener = listener;
        }

        @Override
        protected ExpressionHelper<T> addListener(InvalidationListener listener) {
            return new Generic<>(observable, this.listener, listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            return (listener.equals(this.listener))? null : this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            return new Generic<>(observable, observable.getValue(), this.listener, listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(ChangeListener<? super T> listener) {
            return this;
        }

        @Override
        protected void fireValueChangedEvent() {
            try {
                listener.invalidated(observable);
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    private static class SingleChange<T> extends ExpressionHelper<T> {

        private final ChangeListener<? super T> listener;
        private T currentValue;

        private SingleChange(ObservableValue<T> observable, T currentValue, ChangeListener<? super T> listener) {
            super(observable);
            this.listener = listener;
            this.currentValue = currentValue;
        }

        @Override
        protected ExpressionHelper<T> addListener(InvalidationListener listener) {
            return new Generic<>(observable, currentValue, listener, this.listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            return this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            return new Generic<>(observable, currentValue, this.listener, listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(ChangeListener<? super T> listener) {
            return (listener.equals(this.listener))? null : this;
        }

        @Override
        protected void fireValueChangedEvent() {
            final T oldValue = currentValue;
            currentValue = observable.getValue();
            final boolean changed = (currentValue == null)? (oldValue != null) : !currentValue.equals(oldValue);
            if (changed) {
                try {
                    listener.changed(observable, oldValue, currentValue);
                } catch (Exception e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }
    }

    private static class Generic<T> extends ExpressionHelper<T> {

        private InvalidationListener[] invalidationListeners;
        private ChangeListener<? super T>[] changeListeners;
        private int invalidationSize;
        private int changeSize;
        private boolean locked;
        private T currentValue;

        private Generic(ObservableValue<T> observable, InvalidationListener listener0, InvalidationListener listener1) {
            super(observable);
            this.invalidationListeners = new InvalidationListener[] {listener0, listener1};
            this.invalidationSize = 2;
        }

        private Generic(ObservableValue<T> observable, T currentValue, ChangeListener<? super T> listener0, ChangeListener<? super T> listener1) {
            super(observable);
            this.changeListeners = new ChangeListener[] {listener0, listener1};
            this.changeSize = 2;
            this.currentValue = currentValue;
        }

        private Generic(ObservableValue<T> observable, T currentValue, InvalidationListener invalidationListener, ChangeListener<? super T> changeListener) {
            super(observable);
            this.invalidationListeners = new InvalidationListener[] {invalidationListener};
            this.invalidationSize = 1;
            this.changeListeners = new ChangeListener[] {changeListener};
            this.changeSize = 1;
            this.currentValue = currentValue;
        }

        @Override
        protected Generic<T> addListener(InvalidationListener listener) {
            if (invalidationListeners == null) {
                invalidationListeners = new InvalidationListener[] {listener};
                invalidationSize = 1;
            } else {
                final int oldCapacity = invalidationListeners.length;
                if (locked) {
                    final int newCapacity = (invalidationSize < oldCapacity)? oldCapacity : (oldCapacity * 3)/2 + 1;
                    invalidationListeners = Arrays.copyOf(invalidationListeners, newCapacity);
                } else if (invalidationSize == oldCapacity) {
                    invalidationSize = trim(invalidationSize, invalidationListeners);
                    if (invalidationSize == oldCapacity) {
                        final int newCapacity = (oldCapacity * 3)/2 + 1;
                        invalidationListeners = Arrays.copyOf(invalidationListeners, newCapacity);
                    }
                }
                invalidationListeners[invalidationSize++] = listener;
            }
            return this;
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            if (invalidationListeners != null) {
                for (int index = 0; index < invalidationSize; index++) {
                    if (listener.equals(invalidationListeners[index])) {
                        if (invalidationSize == 1) {
                            if (changeSize == 1) {
                                return new SingleChange<>(observable, currentValue, changeListeners[0]);
                            }
                            invalidationListeners = null;
                            invalidationSize = 0;
                        } else if ((invalidationSize == 2) && (changeSize == 0)) {
                            return new SingleInvalidation<>(observable, invalidationListeners[1-index]);
                        } else {
                            final int numMoved = invalidationSize - index - 1;
                            final InvalidationListener[] oldListeners = invalidationListeners;
                            if (locked) {
                                invalidationListeners = new InvalidationListener[invalidationListeners.length];
                                System.arraycopy(oldListeners, 0, invalidationListeners, 0, index);
                            }
                            if (numMoved > 0) {
                                System.arraycopy(oldListeners, index+1, invalidationListeners, index, numMoved);
                            }
                            invalidationSize--;
                            if (!locked) {
                                invalidationListeners[invalidationSize] = null; // Let gc do its work
                            }
                        }
                        break;
                    }
                }
            }
            return this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            if (changeListeners == null) {
                changeListeners = new ChangeListener[] {listener};
                changeSize = 1;
            } else {
                final int oldCapacity = changeListeners.length;
                if (locked) {
                    final int newCapacity = (changeSize < oldCapacity)? oldCapacity : (oldCapacity * 3)/2 + 1;
                    changeListeners = Arrays.copyOf(changeListeners, newCapacity);
                } else if (changeSize == oldCapacity) {
                    changeSize = trim(changeSize, changeListeners);
                    if (changeSize == oldCapacity) {
                        final int newCapacity = (oldCapacity * 3)/2 + 1;
                        changeListeners = Arrays.copyOf(changeListeners, newCapacity);
                    }
                }
                changeListeners[changeSize++] = listener;
            }
            if (changeSize == 1) {
                currentValue = observable.getValue();
            }
            return this;
        }

        @Override
        protected ExpressionHelper<T> removeListener(ChangeListener<? super T> listener) {
            if (changeListeners != null) {
                for (int index = 0; index < changeSize; index++) {
                    if (listener.equals(changeListeners[index])) {
                        if (changeSize == 1) {
                            if (invalidationSize == 1) {
                                return new SingleInvalidation<>(observable, invalidationListeners[0]);
                            }
                            changeListeners = null;
                            changeSize = 0;
                            currentValue = null;  // clear current value to avoid stale reference
                        } else if ((changeSize == 2) && (invalidationSize == 0)) {
                            return new SingleChange<>(observable, currentValue, changeListeners[1-index]);
                        } else {
                            final int numMoved = changeSize - index - 1;
                            final ChangeListener<? super T>[] oldListeners = changeListeners;
                            if (locked) {
                                changeListeners = new ChangeListener[changeListeners.length];
                                System.arraycopy(oldListeners, 0, changeListeners, 0, index);
                            }
                            if (numMoved > 0) {
                                System.arraycopy(oldListeners, index+1, changeListeners, index, numMoved);
                            }
                            changeSize--;
                            if (!locked) {
                                changeListeners[changeSize] = null; // Let gc do its work
                            }
                        }
                        break;
                    }
                }
            }
            return this;
        }

        @Override
        protected void fireValueChangedEvent() {
            final InvalidationListener[] curInvalidationList = invalidationListeners;
            final int curInvalidationSize = invalidationSize;
            final ChangeListener<? super T>[] curChangeList = changeListeners;
            final int curChangeSize = changeSize;

            try {
                locked = true;

                final T oldValue = currentValue;

                if (curChangeSize > 0) {

                    /*
                     * Because invalidation listeners may get removed during notification, this may
                     * change the Helper type from Generic to SingleChange. When this transition
                     * occurs, it is essential the correct current value is passed to the new
                     * SingleChange instance. This is why the currentValue is already obtained
                     * before notifying the invalidation listeners.
                     */

                    currentValue = observable.getValue();
                }

                for (int i = 0; i < curInvalidationSize; i++) {
                    try {
                        curInvalidationList[i].invalidated(observable);
                    } catch (Exception e) {
                        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                    }
                }
                if (curChangeSize > 0) {
                    final boolean changed = (currentValue == null)? (oldValue != null) : !currentValue.equals(oldValue);
                    if (changed) {
                        for (int i = 0; i < curChangeSize; i++) {
                            try {
                                curChangeList[i].changed(observable, oldValue, currentValue);
                            } catch (Exception e) {
                                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                            }
                        }
                    }
                }
            } finally {
                locked = false;
            }
        }
    }

}
