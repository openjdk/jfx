/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.application;

import com.sun.javafx.application.PlatformImpl;

/**
 * Application menu class that provides support for interacting with the platform provided
 * application menu. This is currently only supported on macOS.
 *
 * @author David Kopp
 *
 * @since JavaFX 24.0
 */
public final class ApplicationMenu
{
    ApplicationMenu() {
    }

    /**
     * Installs a handler to show a custom About window for your application.
     * <p>
     * Setting the handler to null reverts it to the default behavior.
     *
     * @param handler - the handler to respond to the About menu item being selected
     */
    public void setAboutHandler(Runnable handler) {
        PlatformImpl.setAboutHandler(handler);
    }

    /**
     * Installs a handler to show a custom Settings window for your application.
     * <p>
     * Setting the handler to null reverts it to the default behavior.
     *
     * @param handler - the handler to respond to the Settings menu item being selected
     */
    public void setSettingsHandler(Runnable handler) {
        PlatformImpl.setSettingsHandler(handler);
    }
}
