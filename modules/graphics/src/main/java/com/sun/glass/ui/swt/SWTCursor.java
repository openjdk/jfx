/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

final class SWTCursor extends Cursor {
    org.eclipse.swt.graphics.Cursor cursor;
    protected SWTCursor(int type) {
        super(type);
    }

    protected SWTCursor(int x, int y, Pixels pixels) {
        super(x, y, pixels);
    }

    @Override protected long _createCursor(int x, int y, Pixels pixels) {
        //TODO - dispose custom cursors earlier (ie. when shell closes)
        Display display = Display.getDefault();
        ImageData data = SWTApplication.createImageData(pixels);
        cursor = new org.eclipse.swt.graphics.Cursor(display, data, x, y);
        display.disposeExec(new Runnable() {
            public void run () {
                cursor.dispose();
            }
        });
        return 1L;
    }
}
