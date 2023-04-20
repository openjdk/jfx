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

package com.sun.javafx.collections;

import javafx.collections.ObservableListBase;
import java.util.IdentityHashMap;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.util.Callback;

final class ElementObserver<E> {

    private static class ElementsMapElement {
        InvalidationListener listener;
        int counter;

        public ElementsMapElement(InvalidationListener listener) {
            this.listener = listener;
            this.counter = 1;
        }

        public void increment() {
            counter++;
        }

        public int decrement() {
            return --counter;
        }

        private InvalidationListener getListener() {
            return listener;
        }
    }

    private Callback<E, Observable[]> extractor;
    private final Callback<E, InvalidationListener> listenerGenerator;
    private final ObservableListBase<E> list;
    private IdentityHashMap<E, ElementObserver.ElementsMapElement> elementsMap =
            new IdentityHashMap<>();

    ElementObserver(Callback<E, Observable[]> extractor, Callback<E, InvalidationListener> listenerGenerator, ObservableListBase<E> list) {
        this.extractor = extractor;
        this.listenerGenerator = listenerGenerator;
        this.list = list;
    }


    void attachListener(final E e) {
        if (elementsMap != null && e != null) {
            if (elementsMap.containsKey(e)) {
                elementsMap.get(e).increment();
            } else {
                InvalidationListener listener = listenerGenerator.call(e);
                for (Observable o : extractor.call(e)) {
                    o.addListener(listener);
                }
                elementsMap.put(e, new ElementObserver.ElementsMapElement(listener));
            }
        }
    }

    void detachListener(E e) {
        if (elementsMap != null && e != null) {
            ElementObserver.ElementsMapElement el = elementsMap.get(e);
            for (Observable o : extractor.call(e)) {
                o.removeListener(el.getListener());
            }
            if (el.decrement() == 0) {
                elementsMap.remove(e);
            }
        }
    }

}
