/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import javafx.beans.NamedArg;


/**
 * This class describes features of a Web popup window as specified by
 * JavaScript {@code window.open} function. Instances are passed into popup
 * handlers registered on a {@code WebEngine} using
 * {@link WebEngine#setCreatePopupHandler} method.
 * 
 * @see WebEngine
 * @see WebEngine#setCreatePopupHandler
 * @since JavaFX 2.0
 */
public final class PopupFeatures {

    private final boolean menu, status, toolbar, resizable;

    /**
     * Creates a new instance.
     *
     * @param menu whether menu bar should be present
     * @param status whether status bar should be present
     * @param toolbar whether tool bar should be present
     * @param resizable whether popup window should be resizable
     */
    public PopupFeatures(
            @NamedArg("menu") boolean menu, @NamedArg("status") boolean status, @NamedArg("toolbar") boolean toolbar, @NamedArg("resizable") boolean resizable) {
        this.menu = menu;
        this.status = status;
        this.toolbar = toolbar;
        this.resizable = resizable;
    }

    /**
     * Returns whether menu bar should be present.
     */
    public final boolean hasMenu() {
        return menu;
    }

    /**
     * Returns whether status bar should be present.
     */
    public final boolean hasStatus() {
        return status;
    }

    /**
     * Returns whether tool bar should be present.
     */
    public final boolean hasToolbar() {
        return toolbar;
    }

    /**
     * Returns whether popup window should be resizable.
     */
    public final boolean isResizable() {
        return resizable;
    }
}
