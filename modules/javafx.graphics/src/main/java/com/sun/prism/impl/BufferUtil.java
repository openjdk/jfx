/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Utility routines for dealing with direct buffers.
 */
public class BufferUtil {

    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_SHORT = 2;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_DOUBLE = 8;

    // NOTE that this work must be done reflectively at the present time
    // because this code must compile and run correctly on both CDC/FP
    // and Java SE.  The ByteOrder class and order() methods are not supported
    // on CDC; on that platform the initial byte order is the native byte order.
    private static boolean isCDCFP;
    private static Class byteOrderClass;
    private static Object nativeOrderObject;
    private static Method orderMethod;

    private BufferUtil() {
    }

    @SuppressWarnings("removal")
    public static void nativeOrder(ByteBuffer buf) {
        if (!isCDCFP) {
            try {
                if (byteOrderClass == null) {
                    byteOrderClass = (Class) AccessController.doPrivileged(
                            (PrivilegedExceptionAction) () -> Class.forName("java.nio.ByteOrder", true, null));
                    orderMethod = ByteBuffer.class.getMethod("order", new Class[]{byteOrderClass});
                    Method nativeOrderMethod = byteOrderClass.getMethod("nativeOrder", (Class[])null);
                    nativeOrderObject = nativeOrderMethod.invoke(null, (Object[])null);
                }
            } catch (Throwable t) {
                // Must be running on CDC / FP
                isCDCFP = true;
            }

            if (!isCDCFP) {
                try {
                    orderMethod.invoke(buf, new Object[]{nativeOrderObject});
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * Allocates a new direct ByteBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static ByteBuffer newByteBuffer(int numElements) {
        ByteBuffer bb = ByteBuffer.allocateDirect(numElements);
        nativeOrder(bb);
        return bb;
    }

    /**
     * Allocates a new direct DoubleBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static DoubleBuffer newDoubleBuffer(int numElements) {
        ByteBuffer bb = newByteBuffer(numElements * SIZEOF_DOUBLE);
        return bb.asDoubleBuffer();
    }

    /**
     * Allocates a new direct FloatBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static FloatBuffer newFloatBuffer(int numElements) {
        ByteBuffer bb = newByteBuffer(numElements * SIZEOF_FLOAT);
        return bb.asFloatBuffer();
    }

    /**
     * Allocates a new direct IntBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static IntBuffer newIntBuffer(int numElements) {
        ByteBuffer bb = newByteBuffer(numElements * SIZEOF_INT);
        return bb.asIntBuffer();
    }

    /**
     * Allocates a new direct ShortBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static ShortBuffer newShortBuffer(int numElements) {
        ByteBuffer bb = newByteBuffer(numElements * SIZEOF_SHORT);
        return bb.asShortBuffer();
    }
}
