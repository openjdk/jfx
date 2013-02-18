/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.collections.transformation;

import javafx.collections.ObservableListBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import java.util.List;
import javafx.collections.WeakListChangeListener;

/**
 * A base class for all lists that wraps other lists in a way that changes the list's
 * elements, order, size or generally it's structure.
 * 
 * If the source list is observable, a listener is automatically added to it
 * and the events are delegated to {@link #onSourceChanged(javafx.collections.ListChangeListener.Change)}
 * 
 * @param <E> the type parameter of this list
 * @param <F> the upper bound of the type of the source list
 */
public abstract class TransformationList<E, F> extends ObservableListBase<E> implements ObservableList<E> {

    /**
     * Contains the source list of this transformation list.
     * This is never null and should be used to directly access source list content
     */
    protected List<? extends F> source;
    /**
     * This field contains the result of expression "source instanceof {@link javafx.collections.ObservableList}".
     * If this is true, it is possible to do transforms online.
     */
    protected final boolean observable;
    private ListChangeListener<F> sourceListener;

    /**
     * Creates a new Transformation list wrapped around the source list.
     * @param source the wrapped list 
     */
    @SuppressWarnings("unchecked")
    protected TransformationList(List<? extends F> source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
        if (source instanceof ObservableList) {
            observable = true;
            ((ObservableList<F>)source).addListener(new WeakListChangeListener<F>(getListener()));
        } else {
            observable = false;
        }
    }

    /**
     * The source list specified in the constructor of this transformation list.
     * @return The List that is directly wrapped by this TransformationList
     */
    public final List<? extends F> getDirectSource() {
        return source;
    }

    /**
     * The first non-transformation list in the chain.
     * @return the first wrapped list in the chain of TransformationLists that's
     * not a TransformationList
     * @see #getDirectSource() 
     */
    public final List<?> getBottomMostSource() {
        List<?> currentSource = source;
        while(currentSource instanceof TransformationList) {
            currentSource = ((TransformationList)currentSource).source;
        }
        return currentSource;
    }

    private ListChangeListener<F> getListener() {
        if (sourceListener == null) {
            sourceListener = new ListChangeListener<F>() {

                @Override
                public void onChanged(Change<? extends F> c) {
                    TransformationList.this.onSourceChanged(c);
                }

            };
        }
        return sourceListener;
    }

    /**
     * Called when a change from the source is triggered.
     * @param c the change
     */
    protected abstract void onSourceChanged(Change<? extends F> c);
    
    /**
     * Maps the index of this list's element to an index in the direct source list.
     * @param index the original index
     * @return the index of the element in the original list. 
     * @see #getDirectSource() 
     */
    public abstract int getSourceIndex(int index);
    
    /**
     * Maps the index of list's element to an index in the bottom-most source list.
     * @param index the original index
     * @return the index of the element in the original list.
     * @see #getBottomMostSource() 
     * @see #getSourceIndex(int) 
     */
    public final int getBottomMostSourceIndex(int index) {
        List<?> currentSource = source;
        int idx = getSourceIndex(index);
        while(currentSource instanceof TransformationList) {
            final TransformationList tSource = (TransformationList)currentSource;
            idx = tSource.getSourceIndex(idx);
            currentSource = tSource.source;
        }
        return idx;
    }

}
