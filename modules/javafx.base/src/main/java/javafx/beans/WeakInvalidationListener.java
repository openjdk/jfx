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

package javafx.beans;

import java.lang.ref.WeakReference;

/**
 * A {@code WeakInvalidationListener} can be used if an {@link Observable}
 * should only maintain a weak reference to the listener. This helps to avoid
 * memory leaks that can occur if observers are not unregistered from observed
 * objects after use.
 * <p>
 * A {@code WeakInvalidationListener} is created by passing in the original
 * {@link InvalidationListener}. The {@code WeakInvalidationListener} should
 * then be registered to listen for changes of the observed object.
 * <p>
 * Note: You have to keep a reference to the {@code InvalidationListener} that
 * was passed in as long as it is in use, otherwise it can be garbage collected
 * too soon.
 *
 * @see InvalidationListener
 * @see Observable
 *
 *
 * @since JavaFX 2.0
 */
public final class WeakInvalidationListener implements InvalidationListener, WeakListener {

    private final WeakReference<InvalidationListener> ref;

    /**
     * The constructor of {@code WeakInvalidationListener}.
     *
     * @param listener
     *            The original listener that should be notified
     */
    public WeakInvalidationListener(@NamedArg("listener") InvalidationListener listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }
        this.ref = new WeakReference<>(listener);
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
    public void invalidated(Observable observable) {
        InvalidationListener listener = ref.get();
        if (listener != null) {
            listener.invalidated(observable);
        } else {
            // The weakly reference listener has been garbage collected,
            // so this WeakListener will now unhook itself from the
            // source bean
            observable.removeListener(this);
        }
    }
}
