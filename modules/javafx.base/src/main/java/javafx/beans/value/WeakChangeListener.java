/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.value;

import javafx.beans.NamedArg;
import javafx.beans.WeakListener;

import java.lang.ref.WeakReference;

/**
 * A {@code WeakChangeListener} can be used if an {@link ObservableValue}
 * should only maintain a weak reference to the listener. This helps to avoid
 * memory leaks which can occur if observers are not unregistered from observed
 * objects after use.
 * <p>
 * {@code WeakChangeListener} instances are created by passing in the original
 * {@link ChangeListener}. The {@code WeakChangeListener} should then be
 * registered to listen for changes of the observed object.
 * <p>
 * Note: You have to keep a reference to the {@code ChangeListener} that
 * was passed in for as long as it is in use, otherwise it will be garbage collected
 * too soon.
 *
 * @see ChangeListener
 * @see ObservableValue
 *
 * @param <T> The type of the observed value
 *
 * @since JavaFX 2.0
 */
public final class WeakChangeListener<T> implements ChangeListener<T>, WeakListener {

    private final WeakReference<ChangeListener<T>> ref;

    /**
     * The constructor of {@code WeakChangeListener}.
     *
     * @param listener The original listener that should be notified
     */
    public WeakChangeListener(@NamedArg("listener") ChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }
        this.ref = new WeakReference<ChangeListener<T>>(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasGarbageCollected() {
        return (ref.get() == null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changed(ObservableValue<? extends T> observable, T oldValue,
            T newValue) {
        ChangeListener<T> listener = ref.get();
        if (listener != null) {
            listener.changed(observable, oldValue, newValue);
        } else {
            // The weakly reference listener has been garbage collected,
            // so this WeakListener will now unhook itself from the
            // source bean
            observable.removeListener(this);
        }
    }

}
