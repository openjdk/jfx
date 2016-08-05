/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx;

import javafx.util.FXPermission;

/**
 * Constants used for permission checks.
 */
public final class FXPermissions {

    // Prevent instantiation
    private FXPermissions() {
    }

    public static final FXPermission ACCESS_CLIPBOARD_PERMISSION =
            new FXPermission("accessClipboard");

    public static final FXPermission ACCESS_WINDOW_LIST_PERMISSION =
            new FXPermission("accessWindowList");

    public static final FXPermission CREATE_ROBOT_PERMISSION =
            new FXPermission("createRobot");

    public static final FXPermission CREATE_TRANSPARENT_WINDOW_PERMISSION =
            new FXPermission("createTransparentWindow");

    public static final FXPermission UNRESTRICTED_FULL_SCREEN_PERMISSION =
            new FXPermission("unrestrictedFullScreen");

    public static final FXPermission LOAD_FONT_PERMISSION =
            new FXPermission("loadFont");

    public static final FXPermission MODIFY_FXML_CLASS_LOADER_PERMISSION =
            new FXPermission("modifyFXMLClassLoader");

    public static final FXPermission SET_WINDOW_ALWAYS_ON_TOP_PERMISSION =
            new FXPermission("setWindowAlwaysOnTop");

}
