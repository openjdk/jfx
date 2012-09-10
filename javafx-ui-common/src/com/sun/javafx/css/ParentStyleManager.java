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
import java.lang.ref.Reference;
import java.util.*;
import javafx.scene.Scene;

import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Contains the stylesheet state for a single parent.
 */
final class ParentStyleManager extends StyleManager<Parent> {
    
    /**
     *
     * @param scene
     */
    public ParentStyleManager(Parent parent, StyleManager ancestorStyleManager) {

        super(parent);
        this.ancestorStyleManager = ancestorStyleManager;
        this.key = new ParentKey();
        this.indices = getIndicesOfParentsWithStylesheets(parent, 0);
        updateStylesheets();
    }

    /** Either the Scene styleManager or the styleManager of a Parent */
    private final StyleManager ancestorStyleManager;
    
    /**
     * This instance is reused for each lookup in the cache so as to avoid
     * creating temporary objects as much as reasonable.
     */
    private final ParentKey key;    
    
    private final int[] indices;
    
    /**
     * Another map from String => Stylesheet for a Parent. If a stylesheet for the 
     * given URL has already been loaded then we'll simply reuse the stylesheet
     * rather than loading a duplicate.
     */
    private static final Map<String,StylesheetContainer<Parent>> parentStylesheetMap 
        = new HashMap<String,StylesheetContainer<Parent>>();
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // abstract method implementation
    //
    ////////////////////////////////////////////////////////////////////////////
    
    @Override
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
        
        referencedStylesheets.addAll(ancestorStyleManager.referencedStylesheets);
        
        // RT-20643
        CssError.setCurrentScene(owner.getScene());

        // create the stylesheets, one per URL supplied
        List<String> stylesheetURLs = owner.getStylesheets();
        
        for (int i = 0; i < stylesheetURLs.size(); i++) {

            String url = stylesheetURLs.get(i);
            url = url != null ? url.trim() : null;
            if (url == null || url.isEmpty()) continue;

            Stylesheet stylesheet = null;
            try {
                
                stylesheet = loadParentStylesheet(url);                    
                if (stylesheet != null) {
                    referencedStylesheets.add(stylesheet); 
                }

            } catch (Exception e) {
                // If an exception occurred while loading one of the stylesheets
                // then we will simply print warning into system.err and skip the
                // stylesheet, allowing other stylesheets to attempt to load
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("Cannot add stylesheet. %s\n", e.getLocalizedMessage());
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

        key.indices = indices;
        
        return key;
        
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Parent stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * called from Parent's stylesheets property's onChanged method
     */
    @Override
    public void stylesheetsChanged(Change<String> c) {

        final Scene scene = owner.getScene();
        if (scene == null) return;

        boolean wasRemoved = false;

        while (c.next()) {

            // RT-22565
            // If the String was removed, remove it from parentStylesheetMap
            // and remove all Caches that got styles from the stylesheet.
            // The stylesheet may still be referenced by some other parent,
            // in which case the stylesheet will be reparsed and added
            // back to parentStylesheetMap when the StyleHelper is recreated.
            // This incurs a some overhead since the stylesheet has to be
            // reparsed, but this keeps the other Parents that use this
            // in sync. For example, SceneBuilder will remove and then add
            // a stylesheet after it has been edited and all Parents that use
            // that stylesheet should get the new values.
            //
            if (c.wasRemoved()) {

                final List<String> list = c.getRemoved();
                int nMax = list != null ? list.size() : 0;
                for (int n=0; n<nMax; n++) {
                    final String fname = list.get(n);

                    // remove this parent from the container and clear
                    // all caches used by this stylesheet
                    StylesheetContainer<Parent> psc = parentStylesheetMap.remove(fname);
                    if (psc != null) {
                        clearParentCache(psc);
                    }
                }
            }

            // RT-22565: only wasRemoved matters. If the logic was applied to
            // wasAdded, then the stylesheet would be reparsed each time a
            // a Parent added it.

        }
        // parent uses change also
        c.reset();
    }

    // RT-22565: Called from parentStylesheetsChanged to clear the cache entries
    // for parents that use the same stylesheet
    private void clearParentCache(StylesheetContainer<Parent> psc) {

        final List<Reference<Parent>> parentList = psc.users.list;
        final List<Reference<StyleManager.Key>>    keyList    = psc.keys.list;
        for (int n=parentList.size()-1; 0<=n; --n) {

            final Reference<Parent> ref = parentList.get(n);
            final Parent parent = ref.get();
            if (parent == null) continue;

            Reference<StyleManager> styleManagerRef = managerReferenceMap.get(parent);
            if (styleManagerRef == null) continue;
            
            StyleManager styleManager = styleManagerRef.get();
            if (styleManager == null) continue;
            
            ((ParentStyleManager)styleManager).clearParentCache(keyList);
            
            // tell parent it needs to reapply css
            parent.impl_reapplyCSS();
        }
    }

    // RT-22565: Called from clearParentCache to clear the cache entries.
    private void clearParentCache(List<Reference<StyleManager.Key>> keyList) {

        if (cacheMap.isEmpty()) return;

        for (int n=keyList.size()-1; 0<=n; --n) {

            final Reference<StyleManager.Key> ref = keyList.get(n);
            final StyleManager.Key key = ref.get();
            if (key == null) continue;

            final StyleManager.Cache cache = cacheMap.remove(key);
        }
    }
    

    //
    // This int array represents the set of Parent's that have stylesheets.
    // The indice is that of the Parent's hash code or -1 if
    // the Parent has no stylesheet. See also the comments in the Key class.
    // Note that the array is ordered from root at index zero to the
    // leaf Node's parent at index length-1.
    //

    private static int[] getIndicesOfParentsWithStylesheets(Parent parent, int count) {
        if (parent == null) {
            return new int[count];
        }
        final int[] indices = getIndicesOfParentsWithStylesheets(parent.getParent(), ++count);
        // logic elsewhere depends on indices of parents
        // with no stylesheets being -1
        if (parent.getStylesheets().isEmpty() == false) {
            indices[indices.length - count] = parent.hashCode();
        } else {
            indices[indices.length - count] = -1;
        }
        return indices;
    }

    //
    // recurse so that stylesheets of Parents closest to the root are
    // added to the list first. The ensures that declarations for
    // stylesheets further down the tree (closer to the leaf) have
    // a higer ordinal in the cascade.
    //
    private Stylesheet loadParentStylesheet(String url) {

        // RT-20643
        CssError.setCurrentScene(owner.getScene());

        Stylesheet stylesheet = null;

        StyleManager.StylesheetContainer<Parent> container = null;
        if (parentStylesheetMap.containsKey(url)) {
            container = parentStylesheetMap.get(url);
            // RT-22565: remember that this parent uses this stylesheet.
            // Later, if the cache is cleared, the parent is told to
            // reapply css.
            container.users.add(owner);
            stylesheet = container.stylesheet;
        } else {
            stylesheet = loadStylesheet(url);
            // stylesheet may be null which would mean that some IOException
            // was thrown while trying to load it. Add it to the
            // parentStylesheetMap anyway as this will prevent further
            // attempts to parse the file
            container =
                    new StyleManager.StylesheetContainer<Parent>(url, stylesheet);
            // RT-22565: remember that this parent uses this stylesheet.
            // Later, if the cache is cleared, the parent is told to
            // reapply css.
            container.users.add(owner);
            parentStylesheetMap.put(url, container);
        }
        
        // RT-20643
        CssError.setCurrentScene(null);

        return stylesheet;
    }
    
    
    /**
     * The key used in the cacheMap of the StylesheetContainer
     */
    private static class ParentKey extends StyleManager.Key {
        
        ParentKey() {
            super();
            indices = null;
        }

        protected Key dup() {
            
            ParentKey key = new ParentKey();
            key.className  = className;
            key.id = id;
            final int nElements = styleClass.length;
            key.styleClass = new long[nElements];
            System.arraycopy(styleClass, 0, key.styleClass, 0, nElements);
            key.indices = indices;
            
            return key;
        }
        
        // this will be initialized if a Parent has a stylesheet and will
        // hold the indices of those Parents with stylesheets (the Parent's
        // hash code is used). If the parent does not have a stylesheet, the
        // indice will be -1. When finding the equals Key in the cache lookup,
        // we only need to check up to the last parent that has a stylesheet.
        // In other words, if one node is at level n and nearest parent with
        // a stylesheet is at level m, then the indices match provided
        // indices[x] == other.indices[x] for 0 <= x <= m and all other
        // indices are -1. Thus, [1, -1, 2] and [1, -1, 2, -1, -1] are equivalent
        // whereas [1, -1, 2] and [1, -1, 2, -1, 3] are not.
        private int[] indices;

        public int[] getIndices() {
            return indices;
        }

        public void setIndices(int[] indices) {
            this.indices = indices;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ParentKey && super.equals(o)) {
                ParentKey other = (ParentKey)o;
                if(indices != null && other.indices != null) {
                    boolean eq = true;
                    final int max = Math.min(indices.length, other.indices.length);
                    for (int x = 0; eq && (x < max); x++) {
                        eq = indices[x] == other.indices[x];
                    }
                    if (eq) {
                        // ensure the remainder are -1
                        for(int x=max; eq && x<indices.length; x++) {
                            eq = indices[x] == -1;
                        }
                        for(int x=max; eq && x<other.indices.length; x++) {
                            eq = other.indices[x] == -1;
                        }
                    }
                    return eq;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            if (indices != null) hash = 31 * (hash + Arrays.hashCode(indices));
            return hash;
        }

        @Override
        public String toString() {
            final String istr = indices != null ? Arrays.toString(indices) : "[]";
            return "ParentKey {"+className+", "+id+", "+String.valueOf(styleClass)+", "+istr+"}";
        }

    }
    
}
