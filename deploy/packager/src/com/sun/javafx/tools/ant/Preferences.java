/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.ant;

import org.apache.tools.ant.types.DataType;

/**
 * Deployment preferences of the application.
 *
 * Examples:
 * <pre>
 *    &lt;fx:preferences id="p1" shortcut="true">
 * </pre>
 * Request to create desktop shortcut.
 *
 * <pre>
 *    &lt;fx:preferences shortcut="false" install="true" menu="true">
 * </pre>
 * Request to add reference to the start menu and mark application as installed
 * (in particular, it will be added to Add/Remove programs).
 *
 * <pre>
 *    &lt;fx:resource refid="p1"/>
 * </pre>
 * Same as first example - request to create shortcut.
 *
 * @ant.type name="preferences" category="javafx"
 */
public class Preferences extends DataType {
    private boolean installRequested = false;
    private boolean shortcutRequested = false;
    private boolean menuRequested = false;
    private Boolean systemWide = null;

    Boolean getSystemInstall() {
        return systemWide;
    }

    /**
     * For Web applications "true" is request for app to be installed,
     * i.e. stay in the cache permanently.
     *
     * For native bundles "true" is request to install into system wide location.
     * Specific bundler may ignore this preference if it is not supported.
     *
     * If not specified then default is
     *    - not to install for web apps (i.e. same as "false")
     *    - bundler-specific for native bundlers
     *
     * @ant.not-required    Default is false.
     */
    public void setInstall(boolean b) {
        installRequested = b;
        systemWide = b;
    }

    /**
     * If true then application requests desktop shortcut to be created.
     *
     * @ant.not-required    Default is false.
     */
        public void setShortcut(boolean b) {
        shortcutRequested = b;
    }

    /**
     * If true then application requests to add entry to the system Start Menu.
     *
     * @ant.not-required    Default is false.
     */
    public void setMenu(boolean b) {
        menuRequested = b;
    }

    private Preferences get() {
        if (isReference()) {
            return (Preferences) getRefid().getReferencedObject();
        }
        return this;
    }

    boolean getMenu() {
        return get().menuRequested;
    }

    boolean getShortcut() {
        return get().shortcutRequested;
    }

    boolean getInstall() {
        return get().installRequested;
    }
}
