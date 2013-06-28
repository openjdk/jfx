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

package com.sun.javafx.font.coretext;

import java.lang.ref.WeakReference;

import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.FontStrikeDesc;

class CTStrikeDisposer implements DisposerRecord {

    private FontResource fontResource;
    private FontStrikeDesc desc;
    private long fontRef = 0L;
    private boolean disposed = false;

    public CTStrikeDisposer(FontResource font,
                            FontStrikeDesc desc,
                            long fontRef) {

        this.fontResource = font;
        this.desc = desc;
        this.fontRef = fontRef;
    }

    public synchronized void dispose() {
        if (!disposed) {
            // Careful here. The original strike we are collecting
            // may now be superseded in the map, so only remove
            // the desc if the value reference has been cleared
            WeakReference<FontStrike> ref = fontResource.getStrikeMap().get(desc);
            if (ref != null) {
                Object o = ref.get();
                if (o == null) {
                    fontResource.getStrikeMap().remove(desc);
                }
            }
            if (fontRef != 0) {
                OS.CFRelease(fontRef);
                fontRef = 0;
            }
            disposed = true;
        }
    }
}
