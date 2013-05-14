/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.theme;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.WindowEvent;

import com.sun.webkit.Invoker;
import com.sun.webkit.graphics.WCFont;
import com.sun.webkit.graphics.WCPoint;
import com.sun.webkit.WebPage;
import com.sun.javafx.webkit.WebPageClientImpl;

public final class PopupMenuImpl extends com.sun.webkit.PopupMenu {

    private final static Logger log = Logger.getLogger(PopupMenuImpl.class.getName());

    private final ContextMenu popupMenu;

    public PopupMenuImpl() {
        popupMenu = new ContextMenu();

        popupMenu.setOnHidden(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent t) {
                log.finer("onHidden");
                // Postpone notification. This is to let webkit
                // to process a mouse event first (in case the
                // event is the trigger of the closing). Otherwise,
                // if this is a click in a drop down list, webkit
                // will reopen the popup assuming it is hidden.
                Invoker.getInvoker().postOnEventThread(new Runnable() {
                    public void run() {
                        log.finer("onHidden: notifying");
                        notifyPopupClosed();
                    }
                });
            }
        });
        popupMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                MenuItem item = (MenuItem) t.getTarget();
                log.log(Level.FINE, "onAction: item={0}", item);
                notifySelectionCommited(popupMenu.getItems().indexOf(item));
            }
        });
    }

    @Override protected void show(WebPage page, final int x, final int y, final int width) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "show at [{0}, {1}], width={2}", new Object[] {x, y, width});
        }
        // TODO: doesn't work
        popupMenu.setPrefWidth(width);
        popupMenu.setPrefHeight(popupMenu.getHeight());
        doShow(popupMenu, page, x, y);
    }

    @Override protected void hide() {
        log.fine("hiding");
        popupMenu.hide();
    }

    @Override protected void appendItem(String itemText, boolean isLabel,
                                        boolean isSeparator, boolean isEnabled,
                                        int bgColor, int fgColor, WCFont font)
    {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "itemText={0}, isLabel={1}, isSeparator={2}, isEnabled={3}, " +
                    "bgColor={4}, fgColor={5}, font={6}", new Object[] {itemText, isLabel,
                    isSeparator, isEnabled, bgColor, fgColor, font});
        }
        MenuItem item;

        if (isSeparator) {
            item = new ContextMenuImpl.SeparatorImpl(null);
        } else {
            item = new MenuItem(itemText);
            item.setDisable(!isEnabled);
            // TODO: set the rest of properties
        }
        popupMenu.getItems().add(item);
    }

    @Override protected void setSelectedItem(int index) {
        log.log(Level.FINEST, "index={0}", index);
        // TODO requestFocus is not supported currently
        //popupMenu.getItems().get(index).requestFocus();
    }

    static void doShow(final ContextMenu popup, WebPage page, int anchorX, int anchorY) {
        WebPageClientImpl client = (WebPageClientImpl)page.getPageClient();
        assert (client != null);
        WCPoint pt = client.windowToScreen(new WCPoint(anchorX, anchorY));
        popup.show(client.getContainer().getScene().getWindow(), pt.getX(), pt.getY());
    }
}
