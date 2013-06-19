/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.converters.BooleanConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import com.sun.javafx.scene.control.skin.MenuBarSkin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;

/**
 * <p>
 * A MenuBar control traditionally is placed at the very top of the user
 * interface, and embedded within it are {@link Menu Menus}. To add a menu to
 * a menubar, you add it to the {@link #getMenus() menus} ObservableList.
 * By default, for each menu added to the menubar, it will be
 * represented as a button with the Menu {@link MenuItem#textProperty() text} value displayed.
 * <p>
 * MenuBar sets focusTraversable to false.
 * </p>
 *
 * To create and populate a {@code MenuBar}, you may do what is shown below.
 * Please refer to the {@link Menu} API page for more information on how to
 * configure it.
 * <pre><code>
 * final Menu menu1 = new Menu("File");
 * final Menu menu2 = new Menu("Options");
 * final Menu menu3 = new Menu("Help");
 * 
 * MenuBar menuBar = new MenuBar();
 * menuBar.getMenus().addAll(menu1, menu2, menu3);
 * </code></pre>
 *
 * @see Menu
 * @see MenuItem
 * @since JavaFX 2.0
 */
@DefaultProperty("menus")
public class MenuBar extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuBar() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling applyStyle with null
        // StyleOrigin ensures that css will be able to override the value.
        ((StyleableProperty)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);
    }



    /***************************************************************************
     *                                                                         *
     * Instance variables                                                      *
     *                                                                         *
     **************************************************************************/
    private ObservableList<Menu> menus = FXCollections.<Menu>observableArrayList();


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Use the system menu bar if the current platform supports it.
     * @since JavaFX 2.1
     */
    public final BooleanProperty useSystemMenuBarProperty() {
        if (useSystemMenuBar == null) {
            useSystemMenuBar = new StyleableBooleanProperty() {

                @Override
                public CssMetaData<MenuBar,Boolean> getCssMetaData() {
                    return StyleableProperties.USE_SYSTEM_MENU_BAR;
                }

                @Override
                public Object getBean() {
                    return MenuBar.this;
                }

                @Override
                public String getName() {
                    return "useSystemMenuBar";
                }
            };
        }
        return useSystemMenuBar;
    }
    private BooleanProperty useSystemMenuBar;
    public final void setUseSystemMenuBar(boolean value) {
        useSystemMenuBarProperty().setValue(value);
    }
    public final boolean isUseSystemMenuBar() {
        return useSystemMenuBar == null ? false : useSystemMenuBar.getValue();
    }


    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The menus to show within this MenuBar. If this ObservableList is modified at
     * runtime, the MenuBar will update as expected.
     * @see Menu
     */
    public final ObservableList<Menu> getMenus() {
        return menus;
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new MenuBarSkin(this);
    }
    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "menu-bar";

    private static class StyleableProperties {
        private static final CssMetaData<MenuBar, Boolean> USE_SYSTEM_MENU_BAR =
                new CssMetaData<MenuBar, Boolean>("-fx-use-system-menu-bar",
                                                        BooleanConverter.getInstance(),
                                                        false) {
            @Override public boolean isSettable(MenuBar n) {
                return n.useSystemMenuBar == null || !n.useSystemMenuBar.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(MenuBar n) {
                return (StyleableProperty<Boolean>)n.useSystemMenuBarProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(USE_SYSTEM_MENU_BAR);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    /**
      * Most Controls return true for focusTraversable, so Control overrides
      * this method to return true, but MenuBar returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }
}

