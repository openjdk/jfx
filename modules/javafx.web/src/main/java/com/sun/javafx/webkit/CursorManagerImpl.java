/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit;

import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.CursorManager;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

public final class CursorManagerImpl extends CursorManager<Cursor> {

    private final Map<String, Cursor> map = new HashMap<String, Cursor>();
    private ResourceBundle bundle;

    @Override protected Cursor getCustomCursor(WCImage image, int hotspotX, int hotspotY) {
        return new ImageCursor(
                Toolkit.getImageAccessor().fromPlatformImage(
                    WCGraphicsManager.getGraphicsManager().toPlatformImage(image)),
                hotspotX, hotspotY);
    }

    @Override protected Cursor getPredefinedCursor(int type) {
        switch (type) {
            default:
            case POINTER:                      return                                   Cursor.DEFAULT;
            case CROSS:                        return                                   Cursor.CROSSHAIR;
            case HAND:                         return                                   Cursor.HAND;
            case MOVE:                         return                                   Cursor.MOVE;
            case TEXT:                         return                                   Cursor.TEXT;
            case WAIT:                         return                                   Cursor.WAIT;
            case HELP:                         return getCustomCursor("help",           Cursor.DEFAULT);
            case EAST_RESIZE:                  return                                   Cursor.E_RESIZE;
            case NORTH_RESIZE:                 return                                   Cursor.N_RESIZE;
            case NORTH_EAST_RESIZE:            return                                   Cursor.NE_RESIZE;
            case NORTH_WEST_RESIZE:            return                                   Cursor.NW_RESIZE;
            case SOUTH_RESIZE:                 return                                   Cursor.S_RESIZE;
            case SOUTH_EAST_RESIZE:            return                                   Cursor.SE_RESIZE;
            case SOUTH_WEST_RESIZE:            return                                   Cursor.SW_RESIZE;
            case WEST_RESIZE:                  return                                   Cursor.W_RESIZE;
            case NORTH_SOUTH_RESIZE:           return                                   Cursor.V_RESIZE;
            case EAST_WEST_RESIZE:             return                                   Cursor.H_RESIZE;
            case NORTH_EAST_SOUTH_WEST_RESIZE: return getCustomCursor("resize.nesw",    Cursor.DEFAULT);
            case NORTH_WEST_SOUTH_EAST_RESIZE: return getCustomCursor("resize.nwse",    Cursor.DEFAULT);
            case COLUMN_RESIZE:                return getCustomCursor("resize.column",  Cursor.H_RESIZE);
            case ROW_RESIZE:                   return getCustomCursor("resize.row",     Cursor.V_RESIZE);
            case MIDDLE_PANNING:               return getCustomCursor("panning.middle", Cursor.DEFAULT);
            case EAST_PANNING:                 return getCustomCursor("panning.east",   Cursor.DEFAULT);
            case NORTH_PANNING:                return getCustomCursor("panning.north",  Cursor.DEFAULT);
            case NORTH_EAST_PANNING:           return getCustomCursor("panning.ne",     Cursor.DEFAULT);
            case NORTH_WEST_PANNING:           return getCustomCursor("panning.nw",     Cursor.DEFAULT);
            case SOUTH_PANNING:                return getCustomCursor("panning.south",  Cursor.DEFAULT);
            case SOUTH_EAST_PANNING:           return getCustomCursor("panning.se",     Cursor.DEFAULT);
            case SOUTH_WEST_PANNING:           return getCustomCursor("panning.sw",     Cursor.DEFAULT);
            case WEST_PANNING:                 return getCustomCursor("panning.west",   Cursor.DEFAULT);
            case VERTICAL_TEXT:                return getCustomCursor("vertical.text",  Cursor.DEFAULT);
            case CELL:                         return getCustomCursor("cell",           Cursor.DEFAULT);
            case CONTEXT_MENU:                 return getCustomCursor("context.menu",   Cursor.DEFAULT);
            case NO_DROP:                      return getCustomCursor("no.drop",        Cursor.DEFAULT);
            case NOT_ALLOWED:                  return getCustomCursor("not.allowed",    Cursor.DEFAULT);
            case PROGRESS:                     return getCustomCursor("progress",       Cursor.WAIT);
            case ALIAS:                        return getCustomCursor("alias",          Cursor.DEFAULT);
            case ZOOM_IN:                      return getCustomCursor("zoom.in",        Cursor.DEFAULT);
            case ZOOM_OUT:                     return getCustomCursor("zoom.out",       Cursor.DEFAULT);
            case COPY:                         return getCustomCursor("copy",           Cursor.DEFAULT);
            case NONE:                         return                                   Cursor.NONE;
            case GRAB:                         return getCustomCursor("grab",           Cursor.OPEN_HAND);
            case GRABBING:                     return getCustomCursor("grabbing",       Cursor.CLOSED_HAND);
        }
    }

    private Cursor getCustomCursor(String name, Cursor predefined) {
        Cursor cursor = this.map.get(name);
        if (cursor == null) {
            try {
                if (bundle == null) {
                    bundle = ResourceBundle.getBundle("com.sun.javafx.webkit.Cursors", Locale.getDefault());
                }
                if (bundle != null) {
                    String resource = bundle.getString(name + ".file");
                    Image image = new Image(CursorManagerImpl.class.getResourceAsStream(resource));

                    resource = bundle.getString(name + ".hotspotX");
                    int hotspotX = Integer.parseInt(resource);

                    resource = bundle.getString(name + ".hotspotY");
                    int hotspotY = Integer.parseInt(resource);

                    cursor = new ImageCursor(image, hotspotX, hotspotY);
                }
            } catch (MissingResourceException e) {
                // ignore, treat cursor as missing, use predefined instead
            }
            if (cursor == null) {
                cursor = predefined;
            }
            this.map.put(name, cursor);
        }
        return cursor;
    }
}
