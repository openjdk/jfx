/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;

/**
 * Extends {@link ObjectBinding} with the ability to lazily register and eagerly unregister listeners on its
 * dependencies.
 *
 * @param <T> the type of the wrapped {@code Object}
 */
abstract class LazyObjectBinding<T> extends ObjectBinding<T> {

    private Subscription subscription;
    private boolean wasObserved;

    @Override
    public void addListener(ChangeListener<? super T> listener) {
        super.addListener(listener);

        updateSubscriptionAfterAdd();
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        super.removeListener(listener);

        updateSubscriptionAfterRemove();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        super.addListener(listener);

        updateSubscriptionAfterAdd();
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        super.removeListener(listener);

        updateSubscriptionAfterRemove();
    }

    @Override
    protected boolean allowValidation() {
        return isObserved();
    }

    /**
     * Called after a listener was added to start observing inputs if they're not observed already.
     */
    private void updateSubscriptionAfterAdd() {
        if (!wasObserved) { // was first observer registered?
            subscription = observeSources(); // start observing source

            /*
             * Although the act of registering a listener already attempts to make
             * this binding valid, allowValidation won't allow it as the binding is
             * not observed yet. This is because isObserved will not yet return true
             * when the process of registering the listener hasn't completed yet.
             *
             * As the binding must be valid after it becomes observed the first time
             * 'get' is called again.
             *
             * See com.sun.javafx.binding.ExpressionHelper (which is used
             * by ObjectBinding) where it will do a call to ObservableValue#getValue
             * BEFORE adding the actual listener. This results in ObjectBinding#get
             * to be called in which the #allowValidation call will block it from
             * becoming valid as the condition is "isObserved()"; this is technically
             * correct as the listener wasn't added yet, but means we must call
             * #get again to make this binding valid.
             */

            get(); // make binding valid as source wasn't tracked until now
            wasObserved = true;
        }
    }

    /**
     * Called after a listener was removed to stop observing inputs if this was the last listener
     * observing this binding.
     */
    private void updateSubscriptionAfterRemove() {
        if (wasObserved && !isObserved()) { // was last observer unregistered?
            subscription.unsubscribe();
            subscription = null;
            invalidate(); // make binding invalid as source is no longer tracked
            wasObserved = false;
        }
    }

    /**
     * Called when this binding was previously not observed and a new observer was added. Implementors must return a
     * {@link Subscription} which will be cancelled when this binding no longer has any observers.
     *
     * @return a {@link Subscription} which will be cancelled when this binding no longer has any observers, never null
     */
    protected abstract Subscription observeSources();
}
