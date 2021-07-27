/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import com.sun.javafx.binding.BidirectionalContentBinding;
import com.sun.javafx.binding.ContentBinding;
import com.sun.javafx.binding.SetExpressionHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.Objects;

/**
 * Base class for all readonly properties wrapping an {@link javafx.collections.ObservableSet}.
 * This class provides a default implementation to attach listener.
 *
 * @see ReadOnlySetProperty
 *
 * @param <E> the type of the {@code Set} elements
 * @since JavaFX 2.1
 */
public abstract class ReadOnlySetPropertyBase<E> extends ReadOnlySetProperty<E> {

    private SetExpressionHelper<E> helper;

    /**
     * Creates a default {@code ReadOnlySetPropertyBase}.
     */
    public ReadOnlySetPropertyBase() {
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = SetExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = SetExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super ObservableSet<E>> listener) {
        helper = SetExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super ObservableSet<E>> listener) {
        helper = SetExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(SetChangeListener<? super E> listener) {
        helper = SetExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(SetChangeListener<? super E> listener) {
        helper = SetExpressionHelper.removeListener(helper, listener);
    }

    /**
     * This method needs to be called if the reference to the
     * {@link javafx.collections.ObservableSet} changes.
     *
     * It sends notifications to all attached
     * {@link javafx.beans.InvalidationListener InvalidationListeners},
     * {@link javafx.beans.value.ChangeListener ChangeListeners}, and
     * {@link javafx.collections.SetChangeListener}.
     *
     * This method needs to be called, if the value of this property changes.
     */
    protected void fireValueChangedEvent() {
        SetExpressionHelper.fireValueChangedEvent(helper);
    }

    /**
     * This method needs to be called if the content of the referenced
     * {@link javafx.collections.ObservableSet} changes.
     *
     * Sends notifications to all attached
     * {@link javafx.beans.InvalidationListener InvalidationListeners},
     * {@link javafx.beans.value.ChangeListener ChangeListeners}, and
     * {@link javafx.collections.SetChangeListener}.
     *
     * This method is called when the content of the list changes.
     *
     * @param change the change that needs to be propagated
     */
    protected void fireValueChangedEvent(SetChangeListener.Change<? extends E> change) {
        SetExpressionHelper.fireValueChangedEvent(helper, change);
    }

        @Override
    public void bindContent(ObservableSet<E> source) {
        Objects.requireNonNull(source, "Source cannot be null");
        SetExpressionHelper.requireNotContentBoundBidirectional(helper);
        ContentBinding.bind(this, source);
    }

    @Override
    public void unbindContent() {
        ContentBinding binding = SetExpressionHelper.getCollectionChangeListener(helper, ContentBinding.class);
        if (binding != null) {
            binding.dispose();
        }
    }

    @Override
    public void unbindContent(Object object) {
        ContentBinding.unbind(this, object);
    }

    @Override
    public void bindContentBidirectional(ObservableSet<E> other) {
        Objects.requireNonNull(other, "Set cannot be null");
        SetExpressionHelper.requireNotContentBound(helper);
        BidirectionalContentBinding.bind(this, other);
    }

    @Override
    public void unbindContentBidirectional(ObservableSet<E> other) {
        Objects.requireNonNull(other, "Set cannot be null");
        BidirectionalContentBinding.unbind(this, other);
    }

    @Override
    public void unbindContentBidirectional(Object object) {
        if (object instanceof ObservableSet<?>) {
            unbindContentBidirectional((ObservableSet<E>)object);
        }
    }

}
