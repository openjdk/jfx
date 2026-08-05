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
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Wrapper class for weak references.
 * TODO: describe the purpose of this class.
 *
 * NOTE: In the case of value objects, the referent is never going to be
 * collected!
 */
public class WeakReferenceWrapper<T> {

    private static final Method hasIdentityMethod;
    private final T obj;
    private final WeakReference<T> ref;

    static {
        Method meth;
        try {
            meth = Objects.class.getDeclaredMethod("hasIdentity", Object.class);
        } catch (NoSuchMethodException ex) {
            meth = null;
        }
        hasIdentityMethod = meth;
        System.err.println("hasIdentityMethod = " + hasIdentityMethod);
    }

    /**
     * Helper function that calls Object.hasIdentity via reflection.
     * If `obj` is null, treat is as having no identity (matching JDK 28's
     * behavior with or without --enable-preview) and return false.
     * If `obj` is not null, check whether Objects.hasIdentity exists: if the
     * method doesn't exist or cannot be invoked, return true; otherwise, call
     * Objects.hasIdentity(obj) reflectively and return its value
     */
    private static boolean hasIdentity(Object obj) {
        if (obj == null) {
            System.err.println("[1] null");
            return false;
        } else if (hasIdentityMethod == null) {
            System.err.println("[2] hasIdentityMethod == null");
            return true;
        } else {
            try {
                System.err.println("[3] Calling Ojbects.hasIdentityMethod(obj)");
                return (Boolean)hasIdentityMethod.invoke(null, obj);
            } catch (Exception ex) {
                return true;
            }
        }
    }

    public WeakReferenceWrapper(T obj) {
        if (hasIdentity(obj)) {
            System.err.println("obj has identity : " + obj);
            this.obj = null;
            this.ref = new WeakReference<>(obj);
        } else {
            System.err.println("obj is a value object : " + obj);
            this.obj = obj;
            this.ref = null;
        }

        System.err.println("WeakReferenceWrapper: this.obj = " + this.obj + ", this.ref = " + this.ref);
    }

    public T get() {
        return ref != null ? ref.get() : obj;
    }
}
