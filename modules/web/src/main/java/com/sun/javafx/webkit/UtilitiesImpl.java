/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit;

import com.sun.webkit.ContextMenu;
import com.sun.webkit.Pasteboard;
import com.sun.webkit.PopupMenu;
import com.sun.webkit.Utilities;
import com.sun.javafx.webkit.theme.ContextMenuImpl;
import com.sun.javafx.webkit.theme.PopupMenuImpl;


public final class UtilitiesImpl extends Utilities {
    
    @Override protected Pasteboard createPasteboard() {
        return new PasteboardImpl();
    }
    
    @Override protected PopupMenu createPopupMenu() {
        return new PopupMenuImpl();
    }

    @Override protected ContextMenu createContextMenu() {
        return new ContextMenuImpl();
    }
}
