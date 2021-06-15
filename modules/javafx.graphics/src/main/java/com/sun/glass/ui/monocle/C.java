/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.nio.ByteBuffer;
import java.security.Permission;

/**
 * The C class provides ways to wrap pointers to native C structures in Java
 * objects.
 *
 * C is a singleton. Its instance is obtained by calling C.getC(). This
 * requires the RuntimePermission "loadLibrary.*".
 */
class C {

    private static Permission permission = new RuntimePermission("loadLibrary.*");

    private static C instance = new C();

    /**
     * Obtains the single instance of LinuxSystem. Calling this method requires
     * the RuntimePermission "loadLibrary.*".
     */
    static C getC() {
        checkPermissions();
        return instance;
    }

    private static void checkPermissions() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(permission);
        }
    }

    private C() {
    }

    /**
     * Structure is used for wrapping C structs in Java objects. A subclass of
     * Structure must implement the method sizeof() to define the size of the
     * struct it wraps.
     */
    static abstract class Structure {
        final ByteBuffer b;
        final long p;

        /** Create a new Structure wrapping a new C struct */
        protected Structure() {
            b = ByteBuffer.allocateDirect(sizeof());
            p = getC().GetDirectBufferAddress(b);
        }

        /**
         * Create a new Structure wrapping the C struct at the given location
         * in memory
         * @param ptr The memory address of the C struct to wrap
         */
        protected Structure(long ptr) {
            b = getC().NewDirectByteBuffer(ptr, sizeof());
            p = ptr;
        }

        /** The size of the C struct in bytes. Must be overridden by subclasses. */
        abstract int sizeof();
    }

    /** Create a new ByteBuffer that provides access to the memory at a given
     *  address
     *
     * @param ptr The memory address for which to create a ByteBuffer.
     * @param size The byte length of memory to be wrapped in the ByteBuffer.
     * @return a new ByteBuffer providing direct access to the requested
     * memory region
     */
    native ByteBuffer NewDirectByteBuffer(long ptr, int size);

    /**
     * Finds the memory address pointed to by a direct ByteBuffer
     *
     * @param b a direct ByteBuffer
     * @return the memory address referenced by b
     */
    native long GetDirectBufferAddress(ByteBuffer b);

}
