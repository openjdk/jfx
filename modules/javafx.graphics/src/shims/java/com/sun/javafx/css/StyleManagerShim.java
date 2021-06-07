package com.sun.javafx.css;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.Stylesheet;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;



/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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


public class StyleManagerShim {

    private final StyleManager sm;

    private StyleManagerShim(StyleManager sm) {
        this.sm = sm;
    }

    public static StyleManagerShim getInstance() {
        return new StyleManagerShim(StyleManager.getInstance());

    }

    public boolean isCacheContainerNull(
            Styleable styleable, SubScene subScene) {
        StyleManager.CacheContainer cc = sm.getCacheContainer(styleable, subScene);
        return cc == null;
    }

    public void cacheContainerMap_clear() {
        sm.cacheContainerMap.clear();
    }

    public int platformUserAgentStylesheetContainers_indexOf(String fname) {
        return indexOf(sm.platformUserAgentStylesheetContainers, fname);
    }

    public int userAgentStylesheetContainers_indexOf(String fname) {
        return indexOf(sm.userAgentStylesheetContainers, fname);
    }

    private int indexOf(List<StyleManager.StylesheetContainer> list, String fname) {
        for (int n = 0, nMax = list.size(); n < nMax; n++) {
            StyleManager.StylesheetContainer container = list.get(n);
            if (fname.equals(container.fname)) {
                return n;
            }
        }

        return -1;
    }

    public int userAgentStylesheetContainers_size() {
        return sm.userAgentStylesheetContainers.size();
    }

    public void userAgentStylesheetContainers_clear() {
        sm.userAgentStylesheetContainers.clear();
    }

    public boolean userAgentStylesheetContainers_isEmpty() {
        return sm.userAgentStylesheetContainers.isEmpty();
    }

    public void platformUserAgentStylesheetContainers_clear() {
        sm.platformUserAgentStylesheetContainers.clear();
    }

    public int platformUserAgentStylesheetContainers_size() {
        return sm.platformUserAgentStylesheetContainers.size();
    }

    public String platformUserAgentStylesheetContainers_getfname(int dex) {
        return sm.platformUserAgentStylesheetContainers.get(dex).fname;
    }

    public void stylesheetContainerMap_clear() {
        sm.stylesheetContainerMap.clear();
    }

    public boolean get_hasDefaultUserAgentStylesheet() {
        return sm.hasDefaultUserAgentStylesheet;
    }

    public void set_hasDefaultUserAgentStylesheet(boolean value) {
        sm.hasDefaultUserAgentStylesheet = value;
    }

    public void setDefaultUserAgentStylesheet(String fname) {
        sm.setDefaultUserAgentStylesheet(fname);
    }

    public void setDefaultUserAgentStylesheet(Scene scene, String url) {
        sm.setDefaultUserAgentStylesheet(scene, url);
    }

    public void setDefaultUserAgentStylesheet(Stylesheet ua_stylesheet) {
        sm.setDefaultUserAgentStylesheet(ua_stylesheet);
    }

    public void setUserAgentStylesheets(List<String> urls) {
        sm.setUserAgentStylesheets(urls);
    }

    public void addUserAgentStylesheet(String fname) {
        sm.addUserAgentStylesheet(fname);
    }

    public void addUserAgentStylesheet(Scene scene, String url) {
        sm.addUserAgentStylesheet(scene, url);
    }

    public void addUserAgentStylesheet(Scene scene, Stylesheet ua_stylesheet) {
        sm.addUserAgentStylesheet(scene, ua_stylesheet);
    }

    public void forget(final Scene scene) {
        sm.forget(scene);
    }

    public void forget(final Parent parent) {
        sm.forget(parent);
    }

    public void forget(final SubScene subScene) {
        sm.forget(subScene);
    }

    public StyleMap findMatchingStyles(Node node, SubScene subScene, Set<PseudoClass>[] triggerStates) {
        return sm.findMatchingStyles(node, subScene, triggerStates);
    }

    public byte[] calculateCheckSum(String fname) {
        return sm.calculateCheckSum(fname);
    }

    public boolean stylesheetContainerMap_containsKey(String k) {
        return sm.stylesheetContainerMap.containsKey(k);
    }

    public StylesheetContainer stylesheetContainerMap_get(String k) {
        return new StylesheetContainer(
                sm.stylesheetContainerMap.get(k));
    }

    public static class StylesheetContainer {

         private final StyleManager.StylesheetContainer sc;

         StylesheetContainer(StyleManager.StylesheetContainer sc) {
             this.sc = sc;
         }

        public String get_fname() {
            return sc.fname;
        }

        public int parentUsers_list_size() {
            return sc.parentUsers.list.size();
        }

        public boolean parentUsers_contains(Parent k) {
            return sc.parentUsers.contains(k);
        }

        public StyleManagerShim.RefList<Parent> get_parentUsers() {
            return new RefList<Parent>(sc.parentUsers);
        }
    }

    static class RefList<T> {
         private StyleManager.RefList ref;

         RefList(StyleManager.RefList ref) {
             this.ref = ref;
         }
    }

}
