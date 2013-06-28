/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

public final class ContextMenuItem {
    public static final int ACTION_TYPE = 0;
    public static final int SEPARATOR_TYPE = 1;
    public static final int SUBMENU_TYPE = 2;

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
