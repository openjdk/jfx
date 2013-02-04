/*
 * Copyright (c) 2011, 2013, Oracle  and/or its affiliates. All rights reserved.
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

package javafx.embed.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.ImageCursorFrame;

/**
 * An utility class to translate cursor types between embedded
 * application and SWT.
 *
 */
class SWTCursors {

    private static Cursor createCustomCursor(ImageCursorFrame cursorFrame) {
        //TODO - implement custom cursors
        /*
        Toolkit awtToolkit = Toolkit.getDefaultToolkit();

        double imageWidth = cursorFrame.getWidth();
        double imageHeight = cursorFrame.getHeight();
        Dimension nativeSize = awtToolkit.getBestCursorSize((int)imageWidth, (int)imageHeight);

        double scaledHotspotX = cursorFrame.getHotspotX() * nativeSize.getWidth() / imageWidth;
        double scaledHotspotY = cursorFrame.getHotspotY() * nativeSize.getHeight() / imageHeight;
        Point hotspot = new Point((int)scaledHotspotX, (int)scaledHotspotY);

        final com.sun.javafx.tk.Toolkit fxToolkit =
                com.sun.javafx.tk.Toolkit.getToolkit();
        BufferedImage awtImage = 
                (BufferedImage) fxToolkit.toExternalImage(
                                              cursorFrame.getPlatformImage(),
                                              BufferedImage.class);

        return awtToolkit.createCustomCursor(awtImage, hotspot, null);
        */
        return null;
    }

    static Cursor embedCursorToCursor(CursorFrame cursorFrame) {
        int id = SWT.CURSOR_ARROW;
        switch (cursorFrame.getCursorType()) {
            case DEFAULT:   id = SWT.CURSOR_ARROW; break;
            case CROSSHAIR: id = SWT.CURSOR_CROSS; break;
            case TEXT:      id = SWT.CURSOR_IBEAM; break;
            case WAIT:      id = SWT.CURSOR_WAIT; break;
            case SW_RESIZE: id = SWT.CURSOR_SIZESW; break;
            case SE_RESIZE: id = SWT.CURSOR_SIZESE; break;
            case NW_RESIZE: id = SWT.CURSOR_SIZENW; break;
            case NE_RESIZE: id = SWT.CURSOR_SIZENE; break;
            case N_RESIZE:  id = SWT.CURSOR_SIZEN; break;
            case S_RESIZE:  id = SWT.CURSOR_SIZES; break;
            case W_RESIZE:  id = SWT.CURSOR_SIZEW; break;
            case E_RESIZE:  id = SWT.CURSOR_SIZEE; break;
            case OPEN_HAND:
            case CLOSED_HAND:
            case HAND:      id = SWT.CURSOR_HAND; break;
            case MOVE:      id = SWT.CURSOR_SIZEALL; break;
            case DISAPPEAR:
                // NOTE: Not implemented
                break;
            case H_RESIZE:  id = SWT.CURSOR_SIZEWE; break;
            case V_RESIZE:  id = SWT.CURSOR_SIZENS; break;
            case NONE:
                return null;
            case IMAGE:
                // RT-27939: custom cursors are not implemented
                // return createCustomCursor((ImageCursorFrame) cursorFrame);
        }
        Display display = Display.getCurrent();
        return display != null ? display.getSystemCursor(id) : null;
    }
}
