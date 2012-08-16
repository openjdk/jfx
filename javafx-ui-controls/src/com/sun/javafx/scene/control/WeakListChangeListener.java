/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import java.lang.ref.WeakReference;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;

/**
 * A {@code WeakListChangeListener} can be used, if an {@code ObservableList}
 * should only maintain a weak reference to the listener. This helps to avoid
 * memory leaks, that can occur if observers are not unregistered from observed
 * objects after use.
 * <p>
 * {@code WeakListChangeListener} are created by passing in the original
 * {@link ListChangeListener}. The {@code WeakListChangeListener} should
 * then be registered to listen for changes of the observed object.
 * <p>
 * Note: You have to keep a reference to the {@code ListChangeListener}, that
 * was passed in as long as it is in use, otherwise it will be garbage collected
 * to soon.
 * 
 * @see ListChangeListener
 * @see ObservableList
 * 
 * @param T The type of the observed value
 * 
 */
public class WeakListChangeListener<T> implements ListChangeListener<T> {
    
    private final WeakReference<ListChangeListener<T>> ref;

    /**
     * The constructor of {@code WeakListChangeListener}.
     * 
     * @param listener
     *            The original listener that should be notified
     */
    public WeakListChangeListener(ListChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }
        this.ref = new WeakReference<ListChangeListener<T>>(listener);
    }

    @Override public void onChanged(Change<? extends T> c) {
        ListChangeListener<T> listener = ref.get();
        if (listener != null) {
            listener.onChanged(c);
        } else {
            // The weakly reference listener has been garbage collected,
            // so this WeakListener will now unhook itself from the
            // source bean
            c.getList().removeListener(this);
        }
    }
}
