/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.collections.transformation;

import javafx.collections.ListChangeListener.Change;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.WeakListChangeListener;

/**
 * A base class for all lists that wrap another list in a way that changes
 * (transforms) the wrapped list's elements, order, size, or structure.
 *
 * If the source list is observable, a listener is automatically added to it
 * and the events are delegated to {@link #sourceChanged(javafx.collections.ListChangeListener.Change)}.
 *
 * @param <E> the type parameter of this list
 * @param <F> the upper bound of the type of the source list
 * @since JavaFX 8.0
 */
public abstract class TransformationList<E, F> extends ObservableListBase<E> {

    /**
     * Contains the source list of this transformation list.
     * This is never null and should be used to directly access source list content
     */
    private ObservableList<? extends F> source;
    /**
     * This field contains the result of expression "source instanceof {@link javafx.collections.ObservableList}".
     * If this is true, it is possible to do transforms online.
     */
    private ListChangeListener<F> sourceListener;

    /**
     * Creates a new Transformation list wrapped around the source list.
     * @param source the wrapped list
     */
    protected TransformationList(ObservableList<? extends F> source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
        source.addListener(new WeakListChangeListener<>(getListener()));
    }

    /**
     * The source list specified in the constructor of this transformation list.
     * @return The List that is directly wrapped by this TransformationList
     */
    public final ObservableList<? extends F> getSource() {
        return source;
    }

    /**
     * Checks whether the provided list is in the chain under this
     * {@code TransformationList}.
     *
     * This means the list is either the direct source as returned by
     * {@link #getSource()} or the direct source is a {@code TransformationList},
     * and the list is in it's transformation chain.
     * @param list the list to check
     * @return true if the list is in the transformation chain as specified above.
     */
    public final boolean isInTransformationChain(ObservableList<?> list) {
        if (source == list) {
            return true;
        }
        List<?> currentSource = source;
        while (currentSource instanceof TransformationList<?, ?> transformationList) {
            currentSource = transformationList.source;
            if (currentSource == list) {
                return true;
            }
        }
        return false;
    }

    private ListChangeListener<F> getListener() {
        if (sourceListener == null) {
            sourceListener = c -> {
                TransformationList.this.sourceChanged(c);
            };
        }
        return sourceListener;
    }

    /**
     * Called when a change from the source is triggered.
     * @param c the change
     */
    protected abstract void sourceChanged(Change<? extends F> c);

    /**
     * Maps the index of this list's element to an index in the direct source list.
     * @param index the index in this list
     * @return the index of the element's origin in the source list
     * @see #getSource()
     */
    public abstract int getSourceIndex(int index);

    /**
     * Maps the index of this list's element to an index of the provided {@code list}.
     *
     * The {@code list} must be in the transformation chain.
     *
     * @param list a list from the transformation chain
     * @param index the index of an element in this list
     * @return the index of the element's origin in the provided list
     * @see #isInTransformationChain(javafx.collections.ObservableList)
     */
    public final int getSourceIndexFor(ObservableList<?> list, int index) {
        if (!isInTransformationChain(list)) {
            throw new IllegalArgumentException("Provided list is not in the transformation chain of this"
                    + "transformation list");
        }
        List<?> currentSource = source;
        int idx = getSourceIndex(index);
        while (currentSource != list && currentSource instanceof TransformationList<?, ?> tSource) {
            idx = tSource.getSourceIndex(idx);
            currentSource = tSource.source;
        }
        return idx;
    }

    /**
     * Maps the index of the direct source list's element to an index in this list.
     * @param index the index in the source list
     * @return the index of the element in this list if it is contained
     * in this list or negative value otherwise
     * @see #getSource()
     * @see #getSourceIndex(int)
     *
     * @since 9
     */
    public abstract int getViewIndex(int index);
}
