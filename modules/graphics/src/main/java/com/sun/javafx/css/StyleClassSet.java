/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import javafx.css.StyleClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * States represents a set of State. A {@code Node} may be in more than
 * one pseudo-class state. {@code States} is used to aggregate the active
 * pseudo-class state of a {@code Node}.
 */
public final class StyleClassSet  extends BitSet<StyleClass> {

    /** Create an empty set of StyleClass */
    public StyleClassSet() {
        super();
    }

    StyleClassSet(List<String> styleClassNames) {

        int nMax = styleClassNames != null ? styleClassNames.size() : 0;
        for(int n=0; n<nMax; n++) {
            final String styleClass = styleClassNames.get(n);
            if (styleClass == null || styleClass.isEmpty()) continue;

            final StyleClass sc = getStyleClass(styleClass);
            add(sc);
        }

    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray() {
        return toArray(new StyleClass[size()]);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size()) {
            a = (T[]) new StyleClass[size()];
        }
        int index = 0;
        while(index < getBits().length) {
            final long state = getBits()[index];
            for(int bit=0; bit<Long.SIZE; bit++) {
                long mask = 1l << bit;
                if ((state & mask) == mask) {
                    int n = index * Long.SIZE + bit;
                    StyleClass impl = getStyleClass(n);
                    a[index++] = (T) impl;
                }

            }
        }
        return a;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("style-classes: [");
        Iterator<StyleClass> iter = iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().getStyleClassName());
            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    protected StyleClass cast(Object o) {
        if (o == null) {
            throw new NullPointerException("null arg");
        }
        StyleClass styleClass = (StyleClass) o;
        return styleClass;
    }

    @Override
    protected StyleClass getT(int index) {
        return getStyleClass(index);
    }

    @Override
    protected int getIndex(StyleClass t) {
        return t.getIndex();
    }


    /**
     */
    public static StyleClass getStyleClass(String styleClass) {

        if (styleClass == null || styleClass.trim().isEmpty()) {
            throw new IllegalArgumentException("styleClass cannot be null or empty String");
        }

        StyleClass instance = null;

        final Integer value = styleClassMap.get(styleClass);
        final int index = value != null ? value.intValue() : -1;

        final int size = styleClasses.size();
        assert index < size;

        if (index != -1 && index < size) {
            instance = styleClasses.get(index);
        }

        if (instance == null) {
            instance = new StyleClass(styleClass, size);
            styleClasses.add(instance);
            styleClassMap.put(styleClass, Integer.valueOf(size));
        }

        return instance;
    }

   static StyleClass getStyleClass(int index) {
       if (0 <= index && index < styleClasses.size()) {
           return styleClasses.get(index);
       }
       return null;
   }

    // package private for unit test purposes
    static final Map<String,Integer> styleClassMap =
            new HashMap<String,Integer>(64);

    static final List<StyleClass> styleClasses =
            new ArrayList<StyleClass>();

}

