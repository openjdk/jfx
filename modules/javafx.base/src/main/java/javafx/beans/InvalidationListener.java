/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans;

import javafx.beans.value.ObservableValue;

/**
 * An {@code InvalidationListener} is notified whenever an
 * {@link Observable} becomes invalid. It can be registered and
 * unregistered with {@link Observable#addListener(InvalidationListener)}
 * respectively {@link Observable#removeListener(InvalidationListener)}.
 * <p>
 * For an in-depth explanation of invalidation events and how they differ from
 * change events, see the documentation of {@code ObservableValue}.
 * <p>
 * The same instance of {@code InvalidationListener} can be registered to listen
 * to multiple {@code Observables}.
 *
 * @see Observable
 * @see ObservableValue
 *
 *
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface InvalidationListener {

    /**
     * This method needs to be provided by an implementation of
     * {@code InvalidationListener}. It is called if an {@link Observable}
     * becomes invalid.
     * <p>
     * In general, it is considered bad practice to modify the observed value in
     * this method.
     *
     * @param observable
     *            The {@code Observable} that became invalid
     */
    public void invalidated(Observable observable);
}
