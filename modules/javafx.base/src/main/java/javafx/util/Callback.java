/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.util;

/**
 * The Callback interface is designed to allow for a common, reusable interface
 * to exist for defining APIs that requires a call back in certain situations.
 * <p>
 * Callback is defined with two generic parameters: the first parameter
 * specifies the type of the object passed in to the <code>call</code> method,
 * with the second parameter specifying the return type of the method.
 *
 * @param <P> The type of the argument provided to the <code>call</code> method.
 * @param <R> The type of the return type of the <code>call</code> method.
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface Callback<P,R> {
    /**
     * The <code>call</code> method is called when required, and is given a
     * single argument of type P, with a requirement that an object of type R
     * is returned.
     *
     * @param param The single argument upon which the returned value should be
     *      determined.
     * @return An object of type R that may be determined based on the provided
     *      parameter value.
     */
    public R call(P param);
}
