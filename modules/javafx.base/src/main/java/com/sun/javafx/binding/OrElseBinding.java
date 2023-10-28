/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ObservableValue;
import javafx.util.Subscription;

public class OrElseBinding<T> extends LazyObjectBinding<T> {

    private final ObservableValue<T> source;
    private final T constant;

    public OrElseBinding(ObservableValue<T> source, T constant) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.constant = constant;
    }

    @Override
    protected T computeValue() {
        T value = source.getValue();

        return value == null ? constant : value;
    }

    @Override
    protected Subscription observeSources() {
        return source.subscribe(this::invalidate); // start observing source
    }
}
