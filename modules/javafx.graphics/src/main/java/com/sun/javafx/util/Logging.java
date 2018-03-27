/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import com.sun.javafx.logging.PlatformLogger;

/**
 * Holds PlatformLoggers to use for logging javafx-ui-common related things.
 */
public class Logging {

    /**
     * A PlatformLogger to use for logging layout-related activities.  Created
     * lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit() so that
     * it will not intefere with the build.
     */
    private static PlatformLogger layoutLogger = null;

    /**
     * Returns the PlatformLogger for logging layout-related activities.
     */
    public static final PlatformLogger getLayoutLogger() {
        if (layoutLogger == null) {
            layoutLogger = PlatformLogger.getLogger("javafx.scene.layout");
        }
        return layoutLogger;
    }

    /**
     * A PlatformLogger to use for logging focus-related activities.  Created
     * lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit() so that
     * it will no intefere with the build.
     */
    private static PlatformLogger focusLogger = null;

    /**
     * Returns the PlatformLogger for logging focus-related activities.
     */
    public static final PlatformLogger getFocusLogger() {
        if (focusLogger == null) {
            focusLogger = PlatformLogger.getLogger("javafx.scene.focus");
        }
        return focusLogger;
    }

    /**
     * A PlatformLogger to use for logging input-related activities.  Created
     * lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit() so that
     * it will no intefere with the build.
     */
    private static PlatformLogger inputLogger = null;

    /**
     * Returns the PlatformLogger for logging input-related activities.
     */
    public static final PlatformLogger getInputLogger() {
        if (inputLogger == null) {
            inputLogger = PlatformLogger.getLogger("javafx.scene.input");
        }
        return inputLogger;
    }

    /**
     * A PlatformLogger to use for logging CSS-related activities.  Created
     * lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit() so that
     * it will no intefere with the build.
     */
    private static PlatformLogger cssLogger = null;

    /**
     * Returns the PlatformLogger for logging CSS-related activities.
     */
    public static final PlatformLogger getCSSLogger() {
        if (cssLogger == null) {
            cssLogger = PlatformLogger.getLogger("javafx.css");
        }
        return cssLogger;
    }

     /**
     * A PlatformLogger to use for logging general javafx related activities.
     * Created lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit()
     * so that it will not interfere with the build.
     */
    private static PlatformLogger javafxLogger = null;

    /**
     * Returns the PlatformLogger for logging javafx general activities.
     */
    public static final PlatformLogger getJavaFXLogger() {
        if (javafxLogger == null) {
            javafxLogger = PlatformLogger.getLogger("javafx");
        }
        return javafxLogger;
    }

    /**
     * A PlatformLogger to use for logging layout-related activities.  Created
     * lazily to delay calls to com.sun.javafx.tk.Toolkit.getToolkit() so that
     * it will not intefere with the build.
     */
    private static PlatformLogger accessibilityLogger = null;

    /**
     * Returns the PlatformLogger for logging layout-related activities.
     */
    public static final PlatformLogger getAccessibilityLogger() {
        if (accessibilityLogger == null) {
            accessibilityLogger = PlatformLogger.getLogger("javafx.accessibility");
        }
        return accessibilityLogger;
    }

}
