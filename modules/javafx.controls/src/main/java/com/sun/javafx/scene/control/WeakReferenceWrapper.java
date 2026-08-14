/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Wrapper class that holds a reference to an object with or without identity.
 * If the object is non-null and has identity, a weak reference is created and
 * stored; otherwise it holds a reference to the object itself.
 * <p>
 * In the case of a value object, the referent is never collected, so it is only
 * suitable for uses that do not rely on the object being placed onto a reference
 * queue.
 *
 * @param <T> the type of the referent
 */
public class WeakReferenceWrapper<T> {

    private static final Method hasIdentityMethod;
    private final T obj;
    private final WeakReference<T> ref;

    static {
        Method meth;
        try {
            meth = Objects.class.getMethod("hasIdentity", Object.class);
        } catch (NoSuchMethodException ex) {
            meth = null;
        }
        hasIdentityMethod = meth;
    }

    /**
     * Helper method that reflectively calls Objects.hasIdentity to determine
     * whether we should create and hold a weak reference to the given object.
     * If {@code obj} is null, treat it as having no identity (matching JDK 28's
     * behavior with or without {@code --enable-preview}) and return false.
     * If {@code obj} is not null, check whether {@code Objects.hasIdentity} exists:
     * if the method doesn't exist or cannot be invoked, return true; otherwise,
     * call {@code Objects.hasIdentity(obj)} reflectively and return its value.
     */
    private static boolean useWeakRef(Object obj) {
        if (obj == null) {
            return false;
        } else if (hasIdentityMethod == null) {
            return true;
        } else {
            try {
                return (Boolean)hasIdentityMethod.invoke(null, obj);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                return true;
            }
        }
    }

    /**
     * Creates a new reference that refers to the given object.
     * If the object is non-null and has identity, a weak reference is created and
     * stored; otherwise it holds a reference to the object itself.
     *
     * @param obj the object this reference will refer to
     */
    public WeakReferenceWrapper(T obj) {
        if (useWeakRef(obj)) {
            this.obj = null;
            this.ref = new WeakReference<>(obj);
        } else {
            this.obj = obj;
            this.ref = null;
        }
    }

    /**
     * Returns this reference object's referent. If the referent is held in a weak reference,
     * and this reference object has been cleared by the garbage collector, then this method
     * returns null.
     *
     * @return the object to which this reference refers, or null if this reference object has been cleared
     */
    public T get() {
        return ref != null ? ref.get() : obj;
    }
}
