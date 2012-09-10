/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.logging.PlatformLogger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Scene;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

/**
 * Contains the stylesheet state for a single scene.
 */
final class SceneStyleManager extends StyleManager<Scene> {
    
    /**
     *
     * @param scene
     */
    public SceneStyleManager(Scene scene) {

        super(scene);

        this.popupOwnerScene = scene;
        
        this.key = new StyleManager.Key();
        
        // We have special handling for the case of a node which is in a popup
        // scene. The problem is that the root scene used for the purpose of style
        // sheet support isn't the scene of the popup (which is a hidden API
        // member), but rather the main stage's scene. So we need the scene of the
        // root stage.
        Window window = scene.getWindow();
        while (window instanceof PopupWindow) {
            window = ((PopupWindow)window).getOwnerWindow();
        }

        // A popup owner's window might not be set yet.
        if (window == null) scene.windowProperty().addListener(windowListener);
    }
    
    private final ChangeListener<Window> windowListener = new ChangeListener<Window>() {
        
        @Override
        public void changed(ObservableValue<? extends Window> observable, Window oldValue, Window newValue) {
            
            if (newValue != null) {
                
                observable.removeListener(this);
                
                if (newValue instanceof PopupWindow) {

                    // 
                    // Find the owner window of this popup
                    //
                    Window window = newValue;
                    PopupWindow popup = null;
                    while (window instanceof PopupWindow) {
                        popup = (PopupWindow)window;
                        window = popup.getOwnerWindow();
                    } 

                    Scene popupOwnerScene = null; 
                    if (window != null) {
                        popupOwnerScene = window.getScene();
                    } 

                    if (popupOwnerScene != null) {
                        setPopupOwnerScene(popupOwnerScene);
                    } else {
                        // 
                        // Still don't have the root Scene. Keep listening.
                        //
                        popup.ownerWindowProperty().addListener(this);
                    }
                    
                } else {
                    
                    // if the newWindow isn't a popup window, then 
                    // we've found the owner window
                    setPopupOwnerScene(newValue.getScene());
                }                
            }            
        }        
    };
    
    private void setPopupOwnerScene(Scene popupOwnerScene) {

        assert(popupOwnerScene != null);

        if (popupOwnerScene != this.owner) {

            // If the root Scene's stylesheets change, then this popup's styles
            // need to be updated.
            popupOwnerScene.getStylesheets().addListener(new ListChangeListener<String>() {

                @Override
                public void onChanged(Change<? extends String> c) {
                    updateStylesheets();
                }
            });
            this.popupOwnerScene = popupOwnerScene;
            owner.getRoot().impl_reapplyCSS();

        }
        
    }

    private final StyleManager.Key key;
    
    /*
     * In the case of a popup, this is the Scene of the ownerWindow. If
     * the Scene's window is not a PopupWindow, then the rootScene == scene.
     */
    private Scene popupOwnerScene; // TODO: potential leak?
    Scene getPopupOwnerScene() { return popupOwnerScene; } // for testing

    /**
     * A map from String => Stylesheet for a Scene. If a stylesheet for the 
     * given URL has already been loaded then we'll simply reuse the stylesheet
     * rather than loading a duplicate.
     */
    private static final Map<String,StylesheetContainer<Scene>> sceneStylesheetMap 
        = new HashMap<String,StylesheetContainer<Scene>>();
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // Author stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates stylesheets and associates them with the given scene.
     *
     * Called from Scene.stylesheets on replace. Attempts to reuse existing
     * stylesheets for duplicate urls (ie: if two stages are loaded with the same
     * stylesheet urls, then the same stylesheet instance will be reused).
     *
     * All applied styles and caches are deleted, and complete clean application
     * of styles is started with new stylesheets.
     *
     * @param scene     The scene to update stylesheets for
     */
    public void updateStylesheets() {
        
        // The list of referenced stylesheets is about to be recalculated, 
        // so clear the current list. The style maps are no longer valid, so
        // annihilate the cache
        referencedStylesheets.clear();
        clearCache();

        if (defaultUserAgentStylesheet != null) {
            referencedStylesheets.add(defaultUserAgentStylesheet);
        }
        
        if (userAgentStylesheetMap.isEmpty() == false) {
            // TODO: This isn't right. They should be in the same order that
            // they were first added.
            referencedStylesheets.addAll(userAgentStylesheetMap.values());
        }
        
        // RT-20643
        CssError.setCurrentScene(owner);

        // create the stylesheets, one per URL supplied
        List<String> stylesheetURLs = popupOwnerScene == owner                 
                ? owner.getStylesheets()
                : popupOwnerScene.getStylesheets();

        for (int i = 0; i < stylesheetURLs.size(); i++) {

            String url = stylesheetURLs.get(i);
            url = url != null ? url.trim() : null;
            if (url == null || url.isEmpty()) continue;

            // Has someone already parsed this for us?
            StylesheetContainer container = sceneStylesheetMap.get(url);
            if (container != null) {
                
                // let it be known that this Scene uses this stylesheet
                container.users.add(owner);
                
                final Stylesheet stylesheet = container.stylesheet;
                if (stylesheet != null) {
                    referencedStylesheets.add(stylesheet); 
                }
                
            } else {
                
                Stylesheet stylesheet = null;
                try {

                    stylesheet = loadStylesheet(url);                    

                } catch (Exception e) {
                    // If an exception occurred while loading one of the stylesheets
                    // then we will simply print warning into system.err and skip the
                    // stylesheet, allowing other stylesheets to attempt to load
                    if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                        LOGGER.warning("Cannot add stylesheet. %s\n", e.getLocalizedMessage());
                    }
                    // no telling what condition stylesheet is in. make it null
                    stylesheet = null;
                    
                } finally {

                    // stylesheet is added to the container even if it is null
                    // in order to prevent further attempts at parsing it.
                    container = new StylesheetContainer<Scene>(url, stylesheet);
                    container.users.add(owner);
                    if (stylesheet != null) {
                        referencedStylesheets.add(stylesheet); 
                    }
                    
                }
            }
        }
                            
        // RT-20643
        CssError.setCurrentScene(null);
            
    }
    
    @Override
    protected Key getKey(Node node) {
        
        // Populate our helper key with the class name, id, and style class
        // of the node and lookup the associated Cache in the cacheMap
        key.className = node.getClass().getName();
        key.id = node.getId();
        key.styleClass = node.impl_cssGetStyleClassBits();

        return key;
        
    }

}
