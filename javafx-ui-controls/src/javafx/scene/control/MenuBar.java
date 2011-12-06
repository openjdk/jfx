/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.DefaultProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
        setFocusTraversable(false);
    }



    /***************************************************************************
     *                                                                         *
     * Instance variables                                                      *
     *                                                                         *
     **************************************************************************/
    private ObservableList<Menu> menus = FXCollections.<Menu>observableArrayList();



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


    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "menu-bar";
}

