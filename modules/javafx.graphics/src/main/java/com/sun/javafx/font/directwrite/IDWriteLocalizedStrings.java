/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

class IDWriteLocalizedStrings extends IUnknown {
    IDWriteLocalizedStrings(long ptr) {
        super(ptr);
    }

    int FindLocaleName(String locale) {
        return OS.FindLocaleName(ptr, (locale+'\0').toCharArray());
    }

    int GetStringLength(int index) {
        return OS.GetStringLength(ptr, index);
    }

    String GetString(int index, int size) {
        char[] buffer = OS.GetString(ptr, index, size + 1);//length must include space for null-terminator
        return buffer != null ? new String(buffer, 0, size) : null;
    }
}
