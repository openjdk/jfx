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

package javafx.beans.value;

import com.sun.javafx.binding.ExpressionHelper;
import javafx.beans.InvalidationListener;

/**
 * A convenience class for creating implementations of {@link ObservableValue}.
 * It contains all of the infrastructure support for value invalidation- and
 * change event notification.
 *
 * This implementation can handle adding and removing listeners while the
 * observers are being notified, but it is not thread-safe.
 *
 *
 * @since JavaFX 2.0
 */
public abstract class ObservableValueBase<T> implements ObservableValue<T> {

    private ExpressionHelper<T> helper;

    /**
     * Creates a default {@code ObservableValueBase}.
     */
    public ObservableValueBase() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(InvalidationListener listener) {
        helper = ExpressionHelper.addListener(helper, this, listener, this::observed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(ChangeListener<? super T> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener, this::observed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(InvalidationListener listener) {
        ExpressionHelper<T> newHelper = ExpressionHelper.removeListener(helper, listener);
        boolean unobserved = newHelper == null && helper != null;

        helper = newHelper;

        if (unobserved) {
            unobserved();  // when called, isObserved should already be returning false
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        ExpressionHelper<T> newHelper = ExpressionHelper.removeListener(helper, listener);
        boolean unobserved = newHelper == null && helper != null;

        helper = newHelper;

        if (unobserved) {
            unobserved();  // when called, isObserved should already be returning false
        }
    }

    @Override
    public final boolean isObserved() {
        return helper != null;
    }

    /**
     * Called immediately before this observable transitions from unobserved to observed.
     */
    protected void observed() {
    }

    /**
     * Called immediately after this observable transitions from observed to unobserved.
     */
    protected void unobserved() {
    }

    /**
     * Notify the currently registered observers of a value change.
     *
     * This implementation will ignore all adds and removes of observers that
     * are done while a notification is processed. The changes take effect in
     * the following call to fireValueChangedEvent.
     */
    protected void fireValueChangedEvent() {
        ExpressionHelper.fireValueChangedEvent(helper);
    }
}
