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

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;

/**
 * A binding holding the value of an indirect source. The indirect source results from
 * applying a mapping to the given source.
 *
 * <p>Implementation:
 *
 * <p>In a flat mapped binding there are always two subscriptions involved:
 * <ul>
 * <li>The subscription on its source</li>
 * <li>The subscription on the value resulting from the mapping of the source: the indirect source</li>
 * </ul>
 * The subscription on its given source is present when this binding itself is observed and not present otherwise.
 *
 * <p>The subscription on the indirect source must change whenever the value of the given source changes or is invalidated. More
 * specifically, when the given source is invalidated the indirect subscription should be removed, and when it is revalidated it
 * should resubscribe to the newly calculated indirect source. The binding avoids resubscribing when only the value of
 * the indirect source changes.
 *
 * @param <S> the type of the source
 * @param <T> the type of the resulting binding
 */
public class FlatMappedBinding<S, T> extends LazyObjectBinding<T> {

    private final ObservableValue<S> source;
    private final Function<? super S, ? extends ObservableValue<? extends T>> mapper;

    private Subscription indirectSourceSubscription = Subscription.EMPTY;
    private ObservableValue<? extends T> indirectSource;

    public FlatMappedBinding(ObservableValue<S> source, Function<? super S, ? extends ObservableValue<? extends T>> mapper) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    protected T computeValue() {
        S value = source.getValue();
        ObservableValue<? extends T> newIndirectSource = value == null ? null : mapper.apply(value);

        if (isObserved() && indirectSource != newIndirectSource) {  // only resubscribe when observed and the indirect source changed
            indirectSourceSubscription.unsubscribe();
            indirectSourceSubscription = newIndirectSource == null ? Subscription.EMPTY : Subscription.subscribeInvalidations(newIndirectSource, this::invalidate);
            indirectSource = newIndirectSource;
        }

        return newIndirectSource == null ? null : newIndirectSource.getValue();
    }

    @Override
    protected Subscription observeSources() {
        Subscription subscription = Subscription.subscribeInvalidations(source, this::invalidateAll);

        return () -> {
            subscription.unsubscribe();
            unsubscribeIndirectSource();
        };
    }

    /**
     * Called when the primary source changes. Invalidates this binding and unsubscribes the indirect source
     * to avoid holding a strong reference to it. If the binding becomes valid later, {@link #computeValue()} will
     * subscribe to a newly calculated indirect source.
     *
     * <p>Note that this only needs to be called for changes of the primary source; changes in the indirect
     * source only need to invalidate this binding without also unsubscribing, as it would be wasteful to resubscribe
     * to the same indirect source for each invalidation of that source.
     */
    private void invalidateAll() {
        unsubscribeIndirectSource();
        invalidate();
    }

    private void unsubscribeIndirectSource() {
        indirectSourceSubscription.unsubscribe();
        indirectSourceSubscription = Subscription.EMPTY;
        indirectSource = null;
    }
}
