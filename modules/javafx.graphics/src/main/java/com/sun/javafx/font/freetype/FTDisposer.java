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

package com.sun.javafx.font.freetype;

import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.PrismFontFactory;

class FTDisposer implements DisposerRecord  {
    long library;
    long face;

    FTDisposer(long library, long face) {
        this.library = library;
        this.face = face;
    }

    public synchronized void dispose() {
        if (face != 0) {
            OSFreetype.FT_Done_Face(face);
            if (PrismFontFactory.debugFonts) {
                System.err.println("Done Face=" + face);
            }
            face = 0;
        }
        if (library != 0) {
            OSFreetype.FT_Done_FreeType(library);
            if (PrismFontFactory.debugFonts) {
                System.err.println("Done Library=" + library);
            }
            library = 0;
        }
    }
}