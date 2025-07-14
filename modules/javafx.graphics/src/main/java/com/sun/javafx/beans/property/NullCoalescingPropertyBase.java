/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.property;

import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import java.util.Objects;

/**
 * Base class for null-coalescing properties that evaluate to their local value if non-{@code null},
 * or to their base value if the local value is {@code null}.
 *
 * @param <T> the value type
 */
public abstract class NullCoalescingPropertyBase<T> extends ObjectPropertyBase<T> {

    private final ChangeListener<T> listener = (_, _, _) -> {
        invalidated();
        fireValueChangedEvent();
    };

    private final WeakChangeListener<T> weakListener = new WeakChangeListener<>(listener);
    private final ObservableValue<T> baseObservable;
    private boolean currentValueChanged;
    private T currentValue;

    /**
     * Initializes a new {@code NullCoalescingPropertyBase} with the specified base value.
     *
     * @param baseObservable the base observable
     * @throws NullPointerException if {@code baseObservable} is {@code null}
     */
    protected NullCoalescingPropertyBase(ObservableValue<T> baseObservable) {
        this.baseObservable = Objects.requireNonNull(baseObservable, "baseObservable");
        this.currentValue = baseObservable.getValue();
    }

    /**
     * Connects this property to the base observable and starts observing.
     */
    public final void connect() {
        baseObservable.addListener(weakListener);
        invalidated();
        fireValueChangedEvent();
    }

    /**
     * Disconnects this property from the base observable and stops observing.
     */
    public final void disconnect() {
        baseObservable.removeListener(weakListener);
    }

    @Override
    public final T get() {
        return currentValue;
    }

    @Override
    protected final void fireValueChangedEvent() {
        if (currentValueChanged) {
            currentValueChanged = false;
            super.fireValueChangedEvent();
        }
    }

    @Override
    protected final void invalidated() {
        T localValue = super.get();
        T newValue = localValue != null ? localValue : baseObservable.getValue();

        if (currentValue != newValue) {
            currentValue = newValue;
            currentValueChanged = true;
            onInvalidated();
        }
    }

    /**
     * Called when the current value has changed.
     */
    protected void onInvalidated() {}
}
