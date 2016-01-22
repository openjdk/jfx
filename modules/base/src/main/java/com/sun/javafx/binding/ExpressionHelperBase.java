/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Predicate;
import javafx.beans.WeakListener;

public class ExpressionHelperBase {

    protected static int trim(int size, Object[] listeners) {
        Predicate<Object> p = t -> t instanceof WeakListener &&
                ((WeakListener)t).wasGarbageCollected();
        int index = 0;
        for (; index < size; index++) {
            if (p.test(listeners[index])) {
                break;
            }
        }
        if (index < size) {
            for (int src = index + 1; src < size; src++) {
                if (!p.test(listeners[src])) {
                    listeners[index++] = listeners[src];
                }
            }
            int oldSize = size;
            size = index;
            for (; index < oldSize; index++) {
                listeners[index] = null;
            }
        }

        return size;
    }

}
