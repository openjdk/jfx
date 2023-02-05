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

import javafx.beans.binding.ObjectBinding;

/**
 * Extends {@link ObjectBinding} with the ability to lazily register and eagerly unregister listeners on its
 * dependencies.
 *
 * @param <T> the type of the wrapped {@code Object}
 */
abstract class LazyObjectBinding<T> extends ObjectBinding<T> {

    private Subscription subscription;

    @Override
    protected boolean allowValidation() {
        return isSubscribed();
    }

    @Override
    protected final void observed() {
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
    }

    @Override
    protected final void unobserved() {
        subscription.unsubscribe();
        subscription = null;
        invalidate(); // make binding invalid as source is no longer tracked
    }

    /**
     * Called when this binding was previously not observed and a new observer was added. Implementors must return a
     * {@link Subscription} which will be cancelled when this binding no longer has any observers.
     *
     * @return a {@link Subscription} which will be cancelled when this binding no longer has any observers, never null
     */
    protected abstract Subscription observeSources();

    /**
     * Checks if this binding has a subscription associated with it. This is similar to {@link #isObserved()} but
     * with a different timing. This call returns {@code false} before {@link #observeSources()} is called, and
     * {@code true} after the call completes. Similarly, this method returns {@code true} before
     * {@link Subscription#unsubscribe()} is called, and becomes {@code false} after it completes.<p>
     *
     * The table below shows the two flags and how they change when the first listener is added and when the
     * last listener is removed:
     *
     * <table>
     * <tr><th>State</th><th>isObserved</th><th>isSubscribed</th></tr>
     * <tr><td>{@code addListener} called with first listener</td><td>false</td><td>false</td></tr>
     * <tr><td>{@code observed} called</td><td>false</td><td>false</td></tr>
     * <tr><td>{@code observeSources} called</td><td>false</td><td>false</td></tr>
     * <tr><td>{@code observeSources} completes, returning a {@code Subscription}</td><td>false</td><td>true</td></tr>
     * <tr><td>{@code observed} completes</td><td>false</td><td>true</td></tr>
     * <tr><td>{@code addListener} completes</td><td>true</td><td>true</td></tr>
     * <tr><td><br></td></tr>
     * <tr><td>{@code removeListener} called with last listener</td><td>true</td><td>true</td></tr>
     * <tr><td>{@code unobserved} called</td><td>false</td><td>true</td></tr>
     * <tr><td>{@code unsubscribe} on {@code Subscription} called</td><td>false</td><td>true</td></tr>
     * <tr><td>{@code unsubscribe} on {@code Subscription} completes</td><td>false</td><td>false</td></tr>
     * <tr><td>{@code unobserved} completes</td><td>false</td><td>false</td></tr>
     * <tr><td>{@code removeListener} completes</td><td>false</td><td>false</td></tr>
     * </table>
     *
     * @return {@code true} if this binding has a {@link Subscription} associated with it, otherwise {@code false}
     */
    protected final boolean isSubscribed() {
        return subscription != null;
    }
}
