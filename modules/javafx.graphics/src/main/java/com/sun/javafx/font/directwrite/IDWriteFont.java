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

class IDWriteFont extends IUnknown {
    IDWriteFont(long ptr) {
        super(ptr);
    }

    IDWriteFontFace CreateFontFace() {
        long result = OS.CreateFontFace(ptr);
        return result != 0 ? new IDWriteFontFace(result) : null;
    }

    IDWriteLocalizedStrings GetFaceNames() {
        long result = OS.GetFaceNames(ptr);
        return result != 0 ? new IDWriteLocalizedStrings(result) : null;
    }

    IDWriteFontFamily GetFontFamily() {
        long result = OS.GetFontFamily(ptr);
        return result != 0 ? new IDWriteFontFamily(result) : null;
    }

    IDWriteLocalizedStrings GetInformationalStrings(int informationalStringID) {
        long result = OS.GetInformationalStrings(ptr, informationalStringID);
        return result != 0 ? new IDWriteLocalizedStrings(result) : null;
    }

    int GetSimulations() {
        return OS.GetSimulations(ptr);
    }

    int GetStretch() {
        return OS.GetStretch(ptr);
    }

    int GetStyle() {
        return OS.GetStyle(ptr);
    }

    int GetWeight() {
        return OS.GetWeight(ptr);
    }
}
