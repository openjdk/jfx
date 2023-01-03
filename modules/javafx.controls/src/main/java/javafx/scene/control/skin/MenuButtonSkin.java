/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.behavior.MenuButtonBehavior;
import javafx.event.ActionEvent;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.MenuButton;
import javafx.stage.WindowEvent;

/**
 * Default skin implementation for the {@link MenuButton} control.
 *
 * @see MenuButton
 * @since 9
 */
public class MenuButtonSkin extends MenuButtonSkinBase<MenuButton> {

    /* *************************************************************************
     *                                                                         *
     * Static fields                                                           *
     *                                                                         *
     **************************************************************************/

    static final String AUTOHIDE = "autoHide";



    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final MenuButtonBehavior behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new MenuButtonSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public MenuButtonSkin(final MenuButton control) {
        super(control);

        // install default input map for the MenuButton-like controls
        this.behavior = new MenuButtonBehavior(control);

        // MenuButton's showing does not get updated when autoHide happens,
        // as that hide happens under the covers. So we add to the menuButton's
        // properties map to which the MenuButton can react and update accordingly..
        // JDK-8295426:
        // onAutoHide triggers an Event.ANY, making it impossible to add a listener which dispose() can remove.
        // keeping the existing setOnAutoHide(), making sure to setOnAutoHide(null) later.
        popup.setOnAutoHide(e -> {
            MenuButton menuButton = getSkinnable();
            // work around for the fact autohide happens twice
            // remove this check when that is fixed.
            if (!menuButton.getProperties().containsKey(AUTOHIDE)) {
                menuButton.getProperties().put(AUTOHIDE, Boolean.TRUE);
            }
        });

        ListenerHelper lh = ListenerHelper.get(this);

        // request focus on content when the popup is shown
        lh.addEventHandler(popup, WindowEvent.WINDOW_SHOWN, (ev) -> {
            if (requestFocusOnFirstMenuItem) {
                requestFocusOnFirstMenuItem();
                requestFocusOnFirstMenuItem = false;
            } else {
                ContextMenuContent cmContent = (ContextMenuContent) popup.getSkin().getNode();
                if (cmContent != null) {
                    cmContent.requestFocus();
                }
            }
        });

        lh.addEventHandler(control, ActionEvent.ACTION, (ev) -> {
            control.show();
        });

        label.setLabelFor(control);
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        if (getSkinnable() == null) {
            return;
        }

        popup.setOnAutoHide(null);

        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    @Override MenuButtonBehavior getBehavior() {
        return behavior;
    }



    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case MNEMONIC: return label.queryAccessibleAttribute(AccessibleAttribute.MNEMONIC);
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
