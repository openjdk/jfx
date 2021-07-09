/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * A convenience class for creating implementations of {@link javafx.beans.value.ObservableValue}.
 * It contains all of the infrastructure support for value invalidation- and
 * change event notification.
 *
 * This implementation can handle adding and removing listeners while the
 * observers are being notified, but it is not thread-safe.
 *
 *
 */
public abstract class ExpressionHelper<T> extends ExpressionHelperBase {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Static methods

    public static <T> ExpressionHelper<T> addListener(ExpressionHelper<T> helper, ObservableValue<T> observable, InvalidationListener listener) {
        if ((observable == null) || (listener == null)) {
            throw new NullPointerException();
        }
        observable.getValue(); // validate observable
        return (helper == null)? new SingleInvalidation<T>(observable, listener) : helper.addListener(listener);
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
        return (helper == null)? new SingleChange<T>(observable, listener) : helper.addListener(listener);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations

    private static class SingleInvalidation<T> extends ExpressionHelper<T> {

        private final InvalidationListener listener;

        private SingleInvalidation(ObservableValue<T> expression, InvalidationListener listener) {
            super(expression);
            this.listener = listener;
        }

        @Override
        protected ExpressionHelper<T> addListener(InvalidationListener listener) {
            return new Generic<T>(observable, this.listener, listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            return (listener.equals(this.listener))? null : this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            return new Generic<T>(observable, this.listener, listener);
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

        private SingleChange(ObservableValue<T> observable, ChangeListener<? super T> listener) {
            super(observable);
            this.listener = listener;
            this.currentValue = observable.getValue();
        }

        @Override
        protected ExpressionHelper<T> addListener(InvalidationListener listener) {
            return new Generic<T>(observable, listener, this.listener);
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            return this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            return new Generic<T>(observable, this.listener, listener);
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

        private Map<InvalidationListener, Integer> invalidationListeners = new LinkedHashMap<>();
        private Map<ChangeListener<? super T>, Integer> changeListeners = new LinkedHashMap<>();
        private T currentValue;
        private int weakChangeListenerGcCount = 2;
        private int weakInvalidationListenerGcCount = 2;

        private Generic(ObservableValue<T> observable, InvalidationListener listener0, InvalidationListener listener1) {
            super(observable);
            this.invalidationListeners.put(listener0, 1);
            // use merge here in case listener1 == listener0
            this.invalidationListeners.merge(listener1, 1, Integer::sum);
        }

        private Generic(ObservableValue<T> observable, ChangeListener<? super T> listener0, ChangeListener<? super T> listener1) {
            super(observable);
            this.changeListeners.put(listener0, 1);
            // use merge here in case listener1 == listener0
            this.changeListeners.merge(listener1, 1, Integer::sum);
            this.currentValue = observable.getValue();
        }

        private Generic(ObservableValue<T> observable, InvalidationListener invalidationListener, ChangeListener<? super T> changeListener) {
            super(observable);
            this.invalidationListeners.put(invalidationListener, 1);
            this.changeListeners.put(changeListener, 1);
            this.currentValue = observable.getValue();
        }

        @Override
        protected Generic<T> addListener(InvalidationListener listener) {
            if (invalidationListeners.size() == weakInvalidationListenerGcCount) {
                removeWeakListeners(invalidationListeners);
                if (invalidationListeners.size() == weakInvalidationListenerGcCount) {
                    weakInvalidationListenerGcCount = (weakInvalidationListenerGcCount * 3)/2 + 1;
                }
            }
            invalidationListeners.merge(listener, 1, Integer::sum);
            return this;
        }

        @Override
        protected ExpressionHelper<T> removeListener(InvalidationListener listener) {
            if (invalidationListeners.containsKey(listener)) {
                if (invalidationListeners.merge(listener, -1, Integer::sum) == 0) {
                    invalidationListeners.remove(listener);
                    if (invalidationListeners.isEmpty() && changeListeners.size() == 1) {
                        return new SingleChange<T>(observable, changeListeners.keySet().iterator().next());
                    } else if ((invalidationListeners.size() == 1) && changeListeners.isEmpty()) {
                        return new SingleInvalidation<T>(observable, invalidationListeners.keySet().iterator().next());
                    }
                }
            }
            return this;
        }

        @Override
        protected ExpressionHelper<T> addListener(ChangeListener<? super T> listener) {
            if (changeListeners.size() == weakChangeListenerGcCount) {
                removeWeakListeners(changeListeners);
                if (changeListeners.size() == weakChangeListenerGcCount) {
                    weakChangeListenerGcCount = (weakChangeListenerGcCount * 3)/2 + 1;
                }
            }
            changeListeners.merge(listener, 1, Integer::sum);
            if (changeListeners.size() == 1) {
                currentValue = observable.getValue();
            }
            return this;
        }

        @Override
        protected ExpressionHelper<T> removeListener(ChangeListener<? super T> listener) {
            if (changeListeners.containsKey(listener)) {
                if (changeListeners.merge(listener, -1, Integer::sum) == 0) {
                    changeListeners.remove(listener);
                    if (changeListeners.isEmpty() && invalidationListeners.size() == 1) {
                        return new SingleInvalidation<T>(observable, invalidationListeners.keySet().iterator().next());
                    } else if ((changeListeners.size() == 1) && invalidationListeners.isEmpty()) {
                        return new SingleChange<T>(observable, changeListeners.keySet().iterator().next());
                    }
                }
            }
            return this;
        }

        @Override
        protected void fireValueChangedEvent() {
            // Take a copy of listeners to ensure adding and removing listeners
            // while the observers are being notified is safe
            final Map<InvalidationListener, Integer> curInvalidationList = new LinkedHashMap<>(invalidationListeners);
            final Map<ChangeListener<? super T>, Integer> curChangeList = new LinkedHashMap<>(changeListeners);

            curInvalidationList.entrySet().forEach(entry -> fireInvalidationListeners(entry));

            if (!curChangeList.isEmpty()) {
                final T oldValue = currentValue;
                currentValue = observable.getValue();
                final boolean changed = (currentValue == null)? (oldValue != null) : !currentValue.equals(oldValue);
                if (changed) {
                    curChangeList.entrySet().forEach(entry -> fireChangeListeners(oldValue, entry));
                }
            }
        }

        private void fireInvalidationListeners(Entry<InvalidationListener, Integer> entry) {
            final InvalidationListener listener = entry.getKey();
            final int registrationCount = entry.getValue();
            try {
                for (int i = 0; i < registrationCount; i++) {
                    listener.invalidated(observable);
                }
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(
                    Thread.currentThread(), e);
            }
        }

        private void fireChangeListeners(final T oldValue, Entry<ChangeListener<? super T>, Integer> entry) {
            final ChangeListener<? super T> listener = entry.getKey();
            final int registrationCount = entry.getValue();
            try {
                for (int i  = 0; i < registrationCount; i++) {
                    listener.changed(observable, oldValue, currentValue);
                }
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(
                    Thread.currentThread(), e);
            }
        }
    }
}
