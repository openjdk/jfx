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

package javafx.scene.control;

import javafx.event.ActionEvent;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.skin.SplitMenuButtonSkin;

/**
 * The SplitMenuButton, like the {@link MenuButton} is closely associated with
 * the concept of selecting a {@link MenuItem} from a menu. Unlike {@link MenuButton},
 * the SplitMenuButton is broken into two pieces, the "action" area and the
 * "menu open" area.
 * <p>
 * If the user clicks in the action area, the SplitMenuButton will act similarly
 * to a {@link javafx.scene.control.Button Button}, firing whatever is
 * associated with the {@link #onAction} property.
 * <p>
 * The menu open area of the control will show a menu if clicked. When the user
 * selects an item from the menu, it is executed.
 * <p>
 * Note that the SplitMenuButton does not automatically assign whatever was last
 * selected in the menu to be the action should the action region be clicked.
 *
 * <p>Example:</p>
 * <pre>
 * {@literal
 * SplitMenuButton m = new SplitMenuButton();
 * m.setText("Shutdown");
 * m.getItems().addAll(new MenuItem("Logout"), new MenuItem("Sleep"));
 * m.setOnAction(new EventHandler<ActionEvent>() {
 *     &#064;Override public void handle(ActionEvent e) {
 *         System.out.println("Shutdown");
 *     }
 * });
 * }
 * </pre>
 *
 * <p>
 * MnemonicParsing is enabled by default for SplitMenuButton.
 * </p>
 *
 * @see MenuItem
 * @see Menu
 * @since JavaFX 2.0
 */

public class SplitMenuButton extends MenuButton {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new empty split menu button. Use {@link #setText(String)},
     * {@link #setGraphic(Node)} and {@link #getItems()} to set the content.
     */
    public SplitMenuButton() {
        this((MenuItem[])null);
    }

    /**
     * Creates a new split menu button with the given list of menu items.
     *
     * @param items The items to show within this button's menu
     */
    public SplitMenuButton(MenuItem... items) {
        if (items != null) {
            getItems().addAll(items);
        }

        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.SPLIT_MENU_BUTTON);
        setMnemonicParsing(true);     // enable mnemonic auto-parsing by default
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * Call the action when button is pressed.
     */
    @Override public void fire() {
        if (!isDisabled()) {
            fireEvent(new ActionEvent());
        }
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new SplitMenuButtonSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "split-menu-button";

    // SplitMenuButton adds no new CSS keys.

    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case EXPANDED: return isShowing();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case FIRE:
                fire();
                break;
            case EXPAND:
                show();
                break;
            case COLLAPSE:
                hide();
                break;
            default: super.executeAccessibleAction(action);
        }
    }

}
