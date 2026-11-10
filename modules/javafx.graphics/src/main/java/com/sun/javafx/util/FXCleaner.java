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

package com.sun.javafx.util;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;

/**
 * A module-wide Cleaner utility for registering cleanup actions on objects
 * that become phantom-reachable. This class maintains a single shared
 * {@link Cleaner} instance for the module, avoiding multiple daemon threads.
 * <p>
 * The cleanup action must not reference the object it is registered for, or that
 * object can never become unreachable. Capture a separate object that holds the
 * state to clean up instead:
 * <pre>
 *     NativeHandle handle = new NativeHandle(address);
 *     FXCleaner.register(owner, handle::release);  // the action captures handle, not owner
 * </pre>
 */
public class FXCleaner {
    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * Registers a cleanup action to be run when {@code obj} becomes
     * phantom-reachable.
     *
     * @param obj the object to monitor, cannot be {@code null}
     * @param action the cleanup action to run, cannot be {@code null}
     * @return a {@link Cleanable} that can be used to cancel the cleanup, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public static Cleanable register(Object obj, Runnable action) {
        return CLEANER.register(obj, action);
    }
}
