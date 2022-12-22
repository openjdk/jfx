/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.lang.annotation.Native;

public final class ContextMenuItem {
    @Native public static final int ACTION_TYPE = 0;
    @Native public static final int SEPARATOR_TYPE = 1;
    @Native public static final int SUBMENU_TYPE = 2;

    private String title;
    private int action;
    private boolean isEnabled;
    private boolean isChecked;
    private int type;
    private ContextMenu submenu;

    public String getTitle() { return title; }

    public int getAction() { return action; }

    public boolean isEnabled() { return isEnabled; }

    public boolean isChecked() { return isChecked; }

    public int getType() { return type; }

    public ContextMenu getSubmenu() { return submenu; }

    @Override
    public String toString() {
        return String.format(
                "%s[title='%s', action=%d, enabled=%b, checked=%b, type=%d]",
                super.toString(), title, action, isEnabled, isChecked, type);
    }

    private static ContextMenuItem fwkCreateContextMenuItem() {
        return new ContextMenuItem();
    }

    private void fwkSetTitle(String title) {
        this.title = title;
    }

    private String fwkGetTitle() {
        return getTitle();
    }

    private void fwkSetAction(int action) {
        this.action = action;
    }

    private int fwkGetAction() {
        return getAction();
    }

    private void fwkSetEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    private boolean fwkIsEnabled() {
        return isEnabled();
    }

    private void fwkSetChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    private void fwkSetType(int type) {
        this.type = type;
    }

    private int fwkGetType() {
        return getType();
    }

    private void fwkSetSubmenu(ContextMenu submenu) {
        this.submenu = submenu;
    }

    private ContextMenu fwkGetSubmenu() {
        return getSubmenu();
    }
}
