/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.control.ContextMenuContent;
import com.sun.javafx.scene.control.EmbeddedTextContextMenuContent;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.WindowEvent;
import com.sun.javafx.scene.control.behavior.TwoLevelFocusPopupBehavior;

/**
 * Default Skin implementation for ContextMenu. Several controls use ContextMenu in
 * order to display items in a drop down. This class mostly deals mostly with
 * show / hide logic - the actual content of the context menu is contained within
 * the {@link #getNode() root node}.
 *
 * @since 9
 * @see ContextMenu
 */
public class ContextMenuSkin implements Skin<ContextMenu> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /* need to hold a reference to popupMenu here because getSkinnable() deliberately
     * returns null in PopupControlSkin. */
    private ContextMenu popupMenu;

    private final Region root;
    private TwoLevelFocusPopupBehavior tlFocus;

    // used to handle the situation where CSS is applied to the popup
    // after it is displayed, and we need to modify the position of the
    // popup to account for this.
    private double prefHeight;
    private double shiftY;
    private double prefWidth;
    private double shiftX;



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    // Fix for RT-18247
    private final EventHandler<KeyEvent> keyListener = new EventHandler<KeyEvent>() {
        @Override public void handle(KeyEvent event) {
            if (event.getEventType() != KeyEvent.KEY_PRESSED) return;

            // We only care if the root container still has focus
            if (! root.isFocused()) return;

            final KeyCode code = event.getCode();
            switch (code) {
                case ENTER:
                case SPACE: popupMenu.hide(); return;
                default:    return;
            }
        }
    };



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ContextMenuSkin instance.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ContextMenuSkin(final ContextMenu control) {
        this.popupMenu = control;

        popupMenu.addEventHandler(Menu.ON_SHOWING, new EventHandler<Event>() {
            @Override public void handle(Event event) {
                prefHeight = root.prefHeight(-1);
                prefWidth = root.prefWidth(-1);
            }
        });

        // When a contextMenu is shown, requestFocus on its content to enable
        // keyboard navigation.
        popupMenu.addEventHandler(Menu.ON_SHOWN, new EventHandler<Event>() {
            @Override public void handle(Event event) {
                Node cmContent = popupMenu.getSkin().getNode();
                if (cmContent != null) {
//                    cmContent.requestFocus();
                    if (cmContent instanceof ContextMenuContent) {
                        Node accMenu = ((ContextMenuContent)cmContent).getItemsContainer();
                        accMenu.notifyAccessibleAttributeChanged(AccessibleAttribute.VISIBLE);
                    }
                }

                root.addEventHandler(KeyEvent.KEY_PRESSED, keyListener);

                performPopupShifts();
            }
        });
        popupMenu.addEventHandler(Menu.ON_HIDDEN, new EventHandler<Event>() {
            @Override public void handle(Event event) {
                Node cmContent = popupMenu.getSkin().getNode();
                if (cmContent != null) cmContent.requestFocus();

                root.removeEventHandler(KeyEvent.KEY_PRESSED, keyListener);
            }
        });

        // For accessibility Menu.ON_HIDING does not work because isShowing is true
        // during the event, Menu.ON_HIDDEN does not work because the Window (in glass)
        // has already being disposed. The fix is to use WINDOW_HIDING (WINDOW_HIDDEN).
        popupMenu.addEventFilter(WindowEvent.WINDOW_HIDING, new EventHandler<Event>() {
            @Override public void handle(Event event) {
                Node cmContent = popupMenu.getSkin().getNode();
                if (cmContent instanceof ContextMenuContent) {
                    Node accMenu = ((ContextMenuContent)cmContent).getItemsContainer();
                    accMenu.notifyAccessibleAttributeChanged(AccessibleAttribute.VISIBLE);
                }
            }
        });

        if (Properties.IS_TOUCH_SUPPORTED &&
            popupMenu.getStyleClass().contains("text-input-context-menu")) {
            root = new EmbeddedTextContextMenuContent(popupMenu);
        } else {
            root = new ContextMenuContent(popupMenu);
        }
        root.idProperty().bind(popupMenu.idProperty());
        root.styleProperty().bind(popupMenu.styleProperty());
        root.getStyleClass().addAll(popupMenu.getStyleClass()); // TODO needs to handle updates

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusPopupBehavior(popupMenu); // needs to be last.
        }
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public ContextMenu getSkinnable() {
        return popupMenu;
    }

    /** {@inheritDoc} */
    @Override public Node getNode() {
        return root;
    }

    /** {@inheritDoc} */
    @Override public void dispose() {
        root.idProperty().unbind();
        root.styleProperty().unbind();
        if (tlFocus != null) tlFocus.dispose();
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void performPopupShifts() {
        final ContextMenu contextMenu = getSkinnable();
        final Node ownerNode = contextMenu.getOwnerNode();
        if (ownerNode == null) return;

        final Bounds ownerBounds = ownerNode.localToScreen(ownerNode.getLayoutBounds());
        if (ownerBounds == null) return;

        // shifting vertically
        final double rootPrefHeight = root.prefHeight(-1);
        shiftY = prefHeight - rootPrefHeight;
        if (shiftY > 0 && (contextMenu.getY() + rootPrefHeight) < ownerBounds.getMinY()) {
            contextMenu.setY(contextMenu.getY() + shiftY);
        }

        // shifting horizontally
        final double rootPrefWidth = root.prefWidth(-1);
        shiftX = prefWidth - rootPrefWidth;
        if (shiftX > 0 && (contextMenu.getX() + rootPrefWidth) < ownerBounds.getMinX()) {
            contextMenu.setX(contextMenu.getX() + shiftX);
        }
    }
}
