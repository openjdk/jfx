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

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import com.sun.javafx.css.StyleHelper.StyleCacheBucket;
import com.sun.javafx.css.StyleHelper.StyleCacheKey;
import com.sun.javafx.css.parser.CSSParser;
import sun.util.logging.PlatformLogger;

/**
 * Contains the stylesheet state for a single scene. This includes both the
 * Stylesheets defined on the Scene itself as well as a map of stylesheets for
 * "style"s defined on the Node itself. These containers are kept in the
 * containerMap, key'd by the Scene to which they belong. <p> One of the key
 * responsibilities of the StylesheetContainer is to create and maintain an
 * admittedly elaborate series of caches so as to minimize the amount of time it
 * takes to match a Node to its eventual StyleHelper, and to reuse the
 * StyleHelper as much as possible. <p> Initially, the cache is empty. It is
 * recreated whenever the userStylesheets on the container change, or whenever
 * the userAgentStylesheet changes. The cache is built up as nodes are looked
 * for, and thus there is some overhead associated with the first lookup but
 * which is then not repeated for subsequent lookups. <p> The cache system used
 * is a two level cache. The first level cache simply maps the
 * classname/id/styleclass combination of the request node to a 2nd level cache.
 * If the node has "styles" specified then we still use this 2nd level cache,
 * but must combine its rules with the rules specified in "styles" and perform
 * more work to cascade properly. <p> The 2nd level cache contains a data
 * structure called the Cache. The Cache contains an ordered sequence of Rules,
 * a Long, and a Map. The ordered sequence of rules are the rules that *may*
 * match a node with the given classname, id, and style class. For example,
 * rules which may apply are any rule where the simple selector of the rule
 * contains a reference to the id, style class, or classname of the Node, or a
 * compound selector who's "descendant" part is a simple selector which contains
 * a reference to the id, style class, or classname of the Node. <p> During
 * lookup, we will iterate over all the potential rules and discover if they
 * apply to this particular node. If so, then we toggle a bit position in the
 * Long corresponding to the position of the rule that matched. This long then
 * becomes our key into the final map. <p> Once we have established our key, we
 * will visit the map and look for an existing StyleHelper. If we find a
 * StyleHelper, then we will return it. If not, then we will take the Rules that
 * matched and construct a new StyleHelper from their various parts. <p> This
 * system, while elaborate, also provides for numerous fast paths and sharing of
 * data structures which should dramatically reduce the memory and runtime
 * performance overhead associated with CSS by reducing the matching overhead
 * and caching as much as possible. We make no attempt to use weak references
 * here, so if memory issues result one work around would be to toggle the root
 * user agent stylesheet or stylesheets on the scene to cause the cache to be
 * flushed.
 */

final public class StyleManager {

    private static PlatformLogger LOGGER;
    private static PlatformLogger getLogger() {
        if (LOGGER == null) {
            LOGGER = com.sun.javafx.Logging.getCSSLogger();
        }
        return LOGGER;
    }

    /**
     * Return the StyleManager for the given Scene, or null if the scene
     * is no longer referenced.
     */
    public static StyleManager getStyleManager(Scene scene) {
        final Reference<StyleManager> styleManagerRef = 
                scene != null ? managerReferenceMap.get(scene) : null;
        return styleManagerRef != null ? styleManagerRef.get() : null;
    }
    
    public StyleManager(Scene scene) {

        if (scene == null) {
            throw new IllegalArgumentException("scene cannot be null");
        }

        this.scene = scene;

        cacheMap = new HashMap<Key, Cache>();

        styleCache = new HashMap<StyleCacheKey, StyleCacheBucket>();
        
        smapCount = 0;
        
        // A Reference is needed here since Scene is both in the key and value
        managerReferenceMap.put(scene, new WeakReference<StyleManager>(this));        
        
        this.popupOwnerScene = scene;
        
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
        if (window == null) { 
            scene.windowProperty().addListener(windowListener); 
        }
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

        if (popupOwnerScene != this.scene) {

            // If the root Scene's stylesheets change, then this popup's styles
            // need to be updated.
            popupOwnerScene.getStylesheets().addListener(new ListChangeListener<String>() {

                @Override
                public void onChanged(ListChangeListener.Change<? extends String> c) {
                    updateStylesheets();
                }
            });
            this.popupOwnerScene = popupOwnerScene;
            scene.getRoot().impl_reapplyCSS();

        }
        
    }

    /*
     * In the case of a popup, this is the Scene of the ownerWindow. If
     * the Scene's window is not a PopupWindow, then the rootScene == scene.
     */
    private Scene popupOwnerScene; // TODO: potential leak?
    Scene getPopupOwnerScene() { return popupOwnerScene; } // for testing
        
    /*
     * The scene or parent that created this StyleManager. 
     */
    private final Scene scene;
    Scene getOwner() { return scene; } // for testing
    
    /*
     * Scene does not have a inline style, but an applet might. This is
     * the applet's stylesheet from a 'style' attribute. 
     */
    private Stylesheet inlineStylesheet;
    // for testing
    Stylesheet getInlineStylesheet() { return inlineStylesheet; }
    
    /**
     * The map of Caches, key'd by a combination of class name, style class, and
     * id.
     */
    private final Map<Key, Cache> cacheMap;
    // for testing
    Map<Key, Cache> getCacheMap() { return cacheMap; }

    /*
     * The StyleHelper's cached values are relevant only for a given scene.
     * Since a StylesheetContainer is created for a Scene with stylesheets,
     * it makes sense that the container should own the valueCache. This
     * way, each scene gets its own valueCache.
     */
    private final Map<StyleCacheKey, StyleCacheBucket> styleCache;
    /** StyleHelper uses this cache. */
    Map<StyleCacheKey, StyleCacheBucket> getStyleCache() { return styleCache; }
    
    /*
     * A simple counter used to generate a unique id for a StyleMap. 
     * This unique id is used by StyleHelper in figuring out which 
     * style cache to use.
     */
    private int smapCount;

    /**
     * A map from Scene => StyleManager. This provides us a way to find
     * the StyleManager which is owned by a given Scene without having to 
     * add a getStyleManager public API to Scene. This map is used primarily
     * for getting the StyleManager of the owning Scene for a popup. 
     */
    private static final Map<Object, Reference<StyleManager>> managerReferenceMap =
            new WeakHashMap<Object, Reference<StyleManager>>();
        
   /**
     * This stylesheet represents the "default" set of styles for the entire
     * platform. This is typically only set by the UI Controls module, and
     * otherwise is generally null. Whenever this variable changes (via the
     * setDefaultUserAgentStylesheet function call) then we will end up clearing
     * all of the caches.
     */
    private static final List<String> userAgentStylesheets = new ArrayList<String>();
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // Scene stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////    
    public void updateStylesheets() {
     
        // The list of referenced stylesheets is about to be recalculated, 
        // so clear the current list. The style maps are no longer valid, so
        // annihilate the cache
        clearCache();

        // RT-20643 
        CssError.setCurrentScene(scene);

        // create the stylesheets, one per URL supplied
        final List<String> stylesheetURLs = popupOwnerScene == scene                 
                ? scene.getStylesheets()
                : popupOwnerScene.getStylesheets();
        
        for (int i = 0, iMax = stylesheetURLs.size(); i < iMax; i++) {

            String url = stylesheetURLs.get(i);
            url = url != null ? url.trim() : null;
            if (url == null || url.isEmpty()) {
                continue;
            }
            
            
            // Has someone already parsed this for us?
            StylesheetContainer container = stylesheetContainerMap.get(url);
            if (container != null) {
                
                // let it be known that this Scene uses this stylesheet
                container.users.add(scene);
                
            } else {
                
                Stylesheet stylesheet = null;
                try {

                    stylesheet = loadStylesheet(url);                    

                } catch (Exception e) {
                    // If an exception occurred while loading one of the stylesheets
                    // then we will simply print warning into system.err and skip the
                    // stylesheet, allowing other stylesheets to attempt to load
                    if (getLogger().isLoggable(PlatformLogger.WARNING)) {
                        getLogger().warning("Cannot add stylesheet. %s\n", e.getLocalizedMessage());
                    }
                    // no telling what condition stylesheet is in. make it null
                    stylesheet = null;
                    
                } finally {

                    // stylesheet is added to the container even if it is null
                    // in order to prevent further attempts at parsing it.
                    container = new StylesheetContainer<Scene>(url, stylesheet);
                    stylesheetContainerMap.put(url, container);

                    container.users.add(scene);
                    
                }
            }
            
        }

    }

    /**
     * Called from Scene's stylesheets property's onChanged method. If
     * a stylesheet is removed, remove it from the stylesheetMap so it will
     * be re-parsed if it is added back in.
     */
    public void stylesheetsChanged(Change<String> c) {

        while (c.next()) {

            if (c.wasRemoved()) {

                final List<String> list = c.getRemoved();
                int nMax = list != null ? list.size() : 0;
                for (int n=0; n<nMax; n++) {
                    final String fname = list.get(n);
                    StylesheetContainer container = stylesheetContainerMap.remove(fname);
                    if (container != null && container.selectorPartitioning != null) {
                        container.selectorPartitioning.reset();
                    }
                }
            }

        }
        
        updateStylesheets();

    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // Parent stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /*
     * A container for stylesheets and the Parents or Scenes that use them.
     * If a stylesheet is removed, then all other Parents or Scenes
     * that use that stylesheet should get new styles if the
     * stylesheet is added back in since the stylesheet may have been
     * removed and re-added because it was edited (typical of SceneBuilder). 
     * This container provides the hooks to get back to those Parents or Scenes.
     *
     * StylesheetContainer<Parent> are created and added to parentStylesheetMap
     * in the method gatherParentStylesheets.
     * 
     * StylesheetContainer<Scene> are created and added to sceneStylesheetMap in
     * the method updateStylesheets
     */
    private static class StylesheetContainer<T> {
        // the stylesheet url
        final String fname;
        // the parsed stylesheet so we don't reparse for every parent that uses it
        final Stylesheet stylesheet;
        // the parents or scenes that use this stylesheet. Typically, this list
        //  should be very small.
        final SelectorPartitioning selectorPartitioning;
        final RefList<T> users;
        // the keys for finding Cache entries that use this stylesheet.
        // This list should also be fairly small
        final RefList<Key> keys;

        StylesheetContainer(String fname, Stylesheet stylesheet) {
            this.fname = fname;
            this.stylesheet = stylesheet;
            
            if (stylesheet != null) {
                selectorPartitioning = new SelectorPartitioning();
                final List<Rule> rules = stylesheet.getRules();
                final int rMax = rules == null || rules.isEmpty() ? 0 : rules.size();
                for (int r=0; r<rMax; r++) {

                    final Rule rule = rules.get(r);
                    final List<Selector> selectors = rule.getSelectors();
                    final int sMax = selectors == null || selectors.isEmpty() ? 0 : selectors.size();
                    for (int s=0; s < sMax; s++) {

                        final Selector selector = selectors.get(s);
                        selectorPartitioning.partition(selector, rule);

                    }
                }
                
            } else {
                selectorPartitioning = null;
            }
            
            this.users = new RefList<T>();
            this.keys = new RefList<Key>();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + (this.fname != null ? this.fname.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StylesheetContainer<T> other = (StylesheetContainer<T>) obj;
            if ((this.fname == null) ? (other.fname != null) : !this.fname.equals(other.fname)) {
                return false;
            }
            return true;
        }
        
    }

    /*
     * A list that holds references. Used by StylesheetContainer. 
     * We only ever add to this list, or the whole container gets thrown
     * away. 
     */
    private static class RefList<K> {

        final List<Reference<K>> list = new ArrayList<Reference<K>>();

        void add(K key) {

            for (int n=list.size()-1; 0<=n; --n) {
                final Reference<K> ref = list.get(n);
                final K k = ref.get();
                if (k == null) {
                    // stale reference, remove it.
                    list.remove(n);
                } else {
                    // already have it, bail
                    if (k == key) {
                        return;
                    }
                }
            }
            // not found, add it.
            list.add(new WeakReference<K>(key));
        }

    }

    /**
     * A map from String => Stylesheet. If a stylesheet for the 
     * given URL has already been loaded then we'll simply reuse the stylesheet
     * rather than loading a duplicate.
     */
    private static final Map<String,StylesheetContainer> stylesheetContainerMap 
        = new HashMap<String,StylesheetContainer>();
     
    /**
     * called from Parent's stylesheets property's onChanged method
     */
    public void stylesheetsChanged(Parent parent, Change<String> c) {

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
                    StylesheetContainer<Parent> psc = stylesheetContainerMap.remove(fname);
                    if (psc != null) {
                        clearParentCache(psc);
                        if (psc.selectorPartitioning != null) {
                            psc.selectorPartitioning.reset();
                        }
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
        final List<Reference<Key>>    keyList    = psc.keys.list;
        for (int n=parentList.size()-1; 0<=n; --n) {

            final Reference<Parent> ref = parentList.get(n);
            final Parent parent = ref.get();
            if (parent == null) {
                continue;
            }

            final Scene parentScene = parent.getScene();
            if (parentScene == null) {
                continue;
            }

            Reference<StyleManager> styleManagerRef = managerReferenceMap.get(parentScene);
            if (styleManagerRef == null) {
                continue;
            }
            
            StyleManager styleManager = styleManagerRef.get();
            if (styleManager == null) {
                continue;
            }
            
            styleManager.clearParentCache(keyList);
            
            // tell parent it needs to reapply css
            parent.impl_reapplyCSS();
        }
    }

    // RT-22565: Called from clearParentCache to clear the cache entries.
    private void clearParentCache(List<Reference<Key>> keyList) {

        if (cacheMap.isEmpty()) {
            return;
        }

        for (int n=keyList.size()-1; 0<=n; --n) {

            final Reference<Key> ref = keyList.get(n);
            final Key key = ref.get();
            if (key == null) {
                continue;
            }

            final Cache cache = cacheMap.remove(key);
        }
    }
    
    /**
     * Set a stylesheet for the scene from the given stylesheet string. This
     * stylesheet comes after user agent stylesheets but before author
     * stylesheets and applies only to this scene.
     */
    public static void setInlineStylesheet(Scene scene, Stylesheet stylesheet) {
        
        if (scene == null) {
            return;
        }
        
        StyleManager styleManager = StyleManager.getStyleManager(scene);
        if (styleManager != null) {
            styleManager.inlineStylesheet = stylesheet;
            styleManager.updateStylesheets();
        }
        
    }    
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // Stylesheet loading
    //
    ////////////////////////////////////////////////////////////////////////////
    private static URL getURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException malf) {
            // This may be a relative URL, so try resolving
            // it using the application classloader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                String path = (urlStr != null && urlStr.startsWith("/"))
                        ? urlStr.substring(1) : urlStr;
                return cl.getResource(path);
            }
            return null;
        }
    }

    private static Stylesheet loadStylesheet(final String fname) {
        try {
            return loadStylesheetUnPrivileged(fname);
        } catch (java.security.AccessControlException ace) {
            if (getLogger().isLoggable(PlatformLogger.INFO)) {
                getLogger().info("Could not load the stylesheet, trying with FilePermissions : " + fname);
            }

            /*
            ** we got an access control exception, so
            ** we could be running from an applet/jnlp/or with a security manager.
            ** we'll allow the app to read a css file from our runtime jar,
            ** and give it one more chance.
            */

            /*
            ** check that there are enough chars after the !/ to have a valid .css or .bss file name
            */
            if ((fname.length() < 7) && (fname.indexOf("!/") < fname.length()-7)) {
                return null;
            }

            /*
            **
            ** first check that it's actually looking for the same runtime jar
            ** that we're running from, and not some other file.
            */
            try {
                URI requestedFileUrI = new URI(fname);

                /*
                ** is the requested file in a jar
                */
                if ("jar".equals(requestedFileUrI.getScheme())) {
                    /*
                    ** let's check that the css file is being requested from our
                    ** runtime jar
                    */
                    URI styleManagerJarURI = AccessController.doPrivileged(new PrivilegedExceptionAction<URI>() {
                            @Override
                            public URI run() throws java.net.URISyntaxException, java.security.PrivilegedActionException {
                            return StyleManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                        }
                    });

                    final String styleManagerJarPath = styleManagerJarURI.getSchemeSpecificPart();
                    String requestedFilePath = requestedFileUrI.getSchemeSpecificPart();
                    String requestedFileJarPart = requestedFilePath.substring(requestedFilePath.indexOf('/'), requestedFilePath.indexOf("!/"));
                    /*
                    ** it's the correct jar, check it's a file access
                    ** strip off the leading jar
                    */
                    if (styleManagerJarPath.equals(requestedFileJarPart)) {
                        /*
                        ** strip off the leading "jar",
                        ** the css file name is past the last '!'
                        */
                        String requestedFileJarPathNoLeadingSlash = fname.substring(fname.indexOf("!/")+2);
                        /*
                        ** check that it's looking for a css file in the runtime jar
                        */
                        if (fname.endsWith(".css") || fname.endsWith(".bss")) {
                            /*
                            ** set up a read permission for the jar
                            */
                            FilePermission perm = new FilePermission(styleManagerJarPath, "read");

                            PermissionCollection perms = perm.newPermissionCollection();
                            perms.add(perm);
                            AccessControlContext permsAcc = new AccessControlContext(
                                new ProtectionDomain[] {
                                    new ProtectionDomain(null, perms)
                                });
                            /*
                            ** check that the jar file exists, and that we're allowed to
                            ** read it.
                            */
                            JarFile jar = null;
                            try {
                                jar = AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                                        @Override
                                        public JarFile run() throws FileNotFoundException, IOException {
                                            return new JarFile(styleManagerJarPath);
                                        }
                                    }, permsAcc);
                            } catch (PrivilegedActionException pae) {
                                /*
                                ** we got either a FileNotFoundException or an IOException
                                ** in the privileged read. Return the same error as we
                                ** would have returned if the css file hadn't of existed.
                                */
                                return null;
                            }
                            if (jar != null) {
                                /*
                                ** check that the file is in the jar
                                */
                                JarEntry entry = jar.getJarEntry(requestedFileJarPathNoLeadingSlash);
                                if (entry != null) {
                                    /*
                                    ** allow read access to the jar
                                    */
                                    return AccessController.doPrivileged(
                                        new PrivilegedAction<Stylesheet>() {
                                            @Override public Stylesheet run() {
                                                return loadStylesheetUnPrivileged(fname);
                                            }}, permsAcc);
                                }
                            }
                        }
                    }
                }
                /*
                ** no matter what happen, we return the same error that would
                ** be returned if the css file hadn't of existed.
                ** That way there in no information leaked.
                */
                return null;
            }
            /*
            ** no matter what happen, we return the same error that would
            ** be returned if the css file hadn't of existed.
            ** That way there in no information leaked.
            */
            catch (java.net.URISyntaxException e) {
                return null;
            }
            catch (java.security.PrivilegedActionException e) {
                return null;
            }
       }
    }


    private static Stylesheet loadStylesheetUnPrivileged(final String fname) {

        Boolean parse = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {

                final String bss = System.getProperty("binary.css");
                // binary.css is true by default.
                // parse only if the file is not a .bss
                // and binary.css is set to false
                return (!fname.endsWith(".bss") && bss != null) ?
                    !Boolean.valueOf(bss) : Boolean.FALSE;
            }
        });

        try {
            final String ext = (parse) ? (".css") : (".bss");
            java.net.URL url = null;
            Stylesheet stylesheet = null;
            // check if url has extension, if not then just url as is and always parse as css text
            if (!(fname.endsWith(".css") || fname.endsWith(".bss"))) {
                url = getURL(fname);
                parse = true;
            } else {
                final String name = fname.substring(0, fname.length() - 4);

                url = getURL(name+ext);
                if (url == null && (parse = !parse)) {
                    // If we failed to get the URL for the .bss file,
                    // fall back to the .css file.
                    // Note that 'parse' is toggled in the test.
                    url = getURL(name+".css");
                }

                if ((url != null) && !parse) {
                    stylesheet = Stylesheet.loadBinary(url);

                    if (stylesheet == null && (parse = !parse)) {
                        // If we failed to load the .bss file,
                        // fall back to the .css file.
                        // Note that 'parse' is toggled in the test.
                        url = getURL(fname);
                    }
                }
            }

            // either we failed to load the .bss file, or parse
            // was set to true.
            if ((url != null) && parse) {
                stylesheet = CSSParser.getInstance().parse(url);
            }

            if (stylesheet == null) {
                if (errors != null) {
                    CssError error =
                        new CssError(
                            "Resource \""+fname+"\" not found."
                        );
                    errors.add(error);
                }
                if (getLogger().isLoggable(PlatformLogger.WARNING)) {
                    getLogger().warning(
                        String.format("Resource \"%s\" not found.", fname)
                    );
                }
            }

            // load any fonts from @font-face
            if (stylesheet != null) {
                faceLoop: for(FontFace fontFace: stylesheet.getFontFaces()) {
                    for(FontFace.FontFaceSrc src: fontFace.getSources()) {
                        if (src.getType() == FontFace.FontFaceSrcType.URL) {
                            Font loadedFont = Font.loadFont(src.getSrc(),10);
                            getLogger().info("Loaded @font-face font [" + (loadedFont == null ? "null" : loadedFont.getName()) + "]");
                            continue faceLoop;
                        }
                    }
                }
            }

            return stylesheet;

        } catch (FileNotFoundException fnfe) {
            if (errors != null) {
                CssError error =
                    new CssError(
                        "Stylesheet \""+fname+"\" not found."
                    );
                errors.add(error);
            }
            if (getLogger().isLoggable(PlatformLogger.INFO)) {
                getLogger().info("Could not find stylesheet: " + fname);//, fnfe);
            }
        } catch (IOException ioe) {
                if (errors != null) {
                    CssError error =
                        new CssError(
                            "Could not load stylesheet: " + fname
                        );
                    errors.add(error);
                }
            if (getLogger().isLoggable(PlatformLogger.INFO)) {
                getLogger().info("Could not load stylesheet: " + fname);//, ioe);
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // User Agent stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public static void addUserAgentStylesheet(String fname) {
        addUserAgentStylesheet(null, fname);
    }

    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param fname  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643
    public static void addUserAgentStylesheet(Scene scene, String fname) {

        // nothing to add
        if (fname == null ||  fname.trim().isEmpty()) {
            return;
        }

        int index = userAgentStylesheets.indexOf(fname);
        if (index > -1) {
            // already have it
            return;
        }
        
        // RT-20643
        CssError.setCurrentScene(scene);

        if (userAgentStylesheets.isEmpty()) {
            // default UA stylesheet is always index 0
            // but a default hasn't been set, so leave room for it.
            userAgentStylesheets.add(null);
        }
        userAgentStylesheets.add(fname);
        
        Stylesheet ua_stylesheet = loadStylesheet(fname);
        StylesheetContainer container = new StylesheetContainer(fname, ua_stylesheet);
        stylesheetContainerMap.put(fname, container);
        
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(Origin.USER_AGENT);
            userAgentStylesheetsChanged();
        }

        // RT-20643
        CssError.setCurrentScene(null);

    }

    /**
     * Set the default user agent stylesheet.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public static void setDefaultUserAgentStylesheet(String fname) {
        setDefaultUserAgentStylesheet(null, fname);
    }

    /**
     * Set the default user agent stylesheet
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param fname  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643
    public static void setDefaultUserAgentStylesheet(Scene scene, String fname) {

        if (fname == null || fname.trim().isEmpty()) {
            throw new IllegalArgumentException("null arg fname");
        }

        // RT-20643
        CssError.setCurrentScene(scene);

        // if this stylesheet has been added already, move it to first element
        int index = userAgentStylesheets.indexOf(fname);
        if (index != -1) {
            
            if (index > 0) {
                userAgentStylesheets.remove(index);
                userAgentStylesheets.set(0, fname);
            }
            return;
        }
        
        // RT-20643
        CssError.setCurrentScene(scene);
        if (userAgentStylesheets.isEmpty()) {
            userAgentStylesheets.add(fname);
        } else {
            userAgentStylesheets.set(0,fname);
        }

        Stylesheet ua_stylesheet = loadStylesheet(fname);
        StylesheetContainer container = new StylesheetContainer(fname, ua_stylesheet);
        stylesheetContainerMap.put(fname, container);
        
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(Origin.USER_AGENT);
            userAgentStylesheetsChanged();
        }

        // RT-20643
        CssError.setCurrentScene(null);

    }

    /**
     * Set the user agent stylesheet. This is the base default stylesheet for
     * the platform
     */
    public static void setDefaultUserAgentStylesheet(Stylesheet stylesheet) {
        
        if (stylesheet == null ) {
            throw new IllegalArgumentException("null arg ua_stylesheet");
        }
        
        final URL url = stylesheet.getUrl();
        final String fname = url != null ? url.toExternalForm() : "";
        
        // if this stylesheet has been added already, move it to first element
        int index = userAgentStylesheets.indexOf(fname);
        if (index != -1) {
            
            if (index > 0) {
                userAgentStylesheets.remove(index);
                userAgentStylesheets.set(0, fname);
            }
            return;
        }
        
        if (userAgentStylesheets.isEmpty()) {
            userAgentStylesheets.add(fname);
        } else {
            userAgentStylesheets.set(0,fname);
        }
        
        StylesheetContainer container = new StylesheetContainer(fname, stylesheet);
        stylesheetContainerMap.put(fname, container);

        stylesheet.setOrigin(Origin.USER_AGENT);
        userAgentStylesheetsChanged();
    }

    /*
     * If the userAgentStylesheets change, then all scenes are updated.
     */
    private static void userAgentStylesheetsChanged() {

        final Iterator<Window> windows = Window.impl_getWindows();
        while (windows.hasNext()) {
            final Window window = windows.next();
            final Scene scene = window.getScene();
            if (scene == null) {
                continue;
            }
            
            final Reference<StyleManager> styleManagerRef = managerReferenceMap.get(scene);
            final StyleManager styleManager = styleManagerRef.get();
            if (styleManager == null) {
                continue;
            }
            
            styleManager.updateStylesheets();
            scene.getRoot().impl_reapplyCSS();
        }

    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // Pseudo-class state bit map handling
    //
    ////////////////////////////////////////////////////////////////////////////
    
    private static final Map<String,Long> pseudoclassMasks = new HashMap<String,Long>();

    public static long getPseudoclassMask(String pclass) {
        Long mask = pseudoclassMasks.get(pclass);
        if (mask == null) {
            final int exp = pseudoclassMasks.size();
            mask = Long.valueOf(1L << exp); // same as Math.pow(2,exp)
            pseudoclassMasks.put(pclass,mask);
        }
        return mask.longValue();
    }

    static long getPseudoclassMask(List<String> pseudoclasses) {
        long mask = 0;

        final int max = pseudoclasses != null ? pseudoclasses.size() : -1;
        for (int n=0; n<max; n++) {
            long m = getPseudoclassMask(pseudoclasses.get(n));
            mask = mask | m;
        }
        return mask;
    }

    public static List<String> getPseudoclassStrings(long mask) {
        if (mask == 0) {
            return Collections.EMPTY_LIST;
        }

        Map<Long,String> stringMap = new HashMap<Long,String>();
        for (Entry<String,Long> entry : pseudoclassMasks.entrySet()) {
            stringMap.put(entry.getValue(), entry.getKey());
        }
        List<String> strings = new ArrayList<String>();
        for (long exp=0; exp < Long.SIZE; exp++) {
            long key = (1L << exp) & mask;
            if (key != 0) {
                String value = stringMap.get(key);
                if (value != null) {
                    strings.add(value);
                }
            }
        }
        // even though the list returned could be modified without causing 
        // harm, returning an unmodifiableList is consistent with 
        // SimpleSelector.getStyleClasses()
        return Collections.unmodifiableList(strings);
    }

    private void clearCache() {
        styleCache.clear();
        cacheMap.clear();
        smapCount = 0;
    }

    private long nextSmapId() {
        return ++smapCount;
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
    private List<StylesheetContainer<Parent>> gatherParentStylesheets(Parent parent) {

        if (parent == null) {
            return null;
        }

        final List<String> parentStylesheets = parent.impl_getAllParentStylesheets();

        if (parentStylesheets == null || parentStylesheets.isEmpty()) {
            return null;
        }

        final List<StylesheetContainer<Parent>> list = new ArrayList<StylesheetContainer<Parent>>();

        // RT-20643
        CssError.setCurrentScene(parent.getScene());

        for (int n = 0, nMax = parentStylesheets.size(); n < nMax; n++) {
            final String fname = parentStylesheets.get(n);
            StylesheetContainer<Parent> container = null;
            if (stylesheetContainerMap.containsKey(fname)) {
                container = stylesheetContainerMap.get(fname);
                // RT-22565: remember that this parent uses this stylesheet.
                // Later, if the cache is cleared, the parent is told to
                // reapply css.
                container.users.add(parent);
            } else {
                final Stylesheet stylesheet = loadStylesheet(fname);
                // stylesheet may be null which would mean that some IOException
                // was thrown while trying to load it. Add it to the
                // parentStylesheetMap anyway as this will prevent further
                // attempts to parse the file
                container =
                        new StylesheetContainer<Parent>(fname, stylesheet);
                // RT-22565: remember that this parent uses this stylesheet.
                // Later, if the cache is cleared, the parent is told to
                // reapply css.
                container.users.add(parent);
                stylesheetContainerMap.put(fname, container);
            }
            if (container != null) {
                list.add(container);
            }
        }
        // RT-20643
        CssError.setCurrentScene(null);

        return list;
    }
    
    // reuse key to avoid creation of numerous small objects
    private static final Key key = new Key();
    
    /**
     * Finds matching styles for this Node.
     */
    StyleMap findMatchingStyles(Node node, long[] pseudoclassBits) {

        final int[] indicesOfParentsWithStylesheets =
                getIndicesOfParentsWithStylesheets(
                ((node instanceof Parent) ? (Parent) node : node.getParent()), 0);
        //
        // avoid calling gatherParentStylesheets later if we know there
        // aren't any parent stylesheets. If there are parent stylesheets,
        // then at least one element of indicesOfParentsWithStylesheets
        // will be other than -1
        //
        boolean hasParentStylesheets = false;
        for (int n = 0; n < indicesOfParentsWithStylesheets.length; n++) {
            // assignment, not equality here!
            if (hasParentStylesheets = (indicesOfParentsWithStylesheets[n] != -1)) {
                break;
            }
        }
                
        //
        // Are there any stylesheets at all?
        // If not, then there is nothing to match and the
        // resulting StyleMap is going to end up empty
        // 
        final List<String> sceneStylesheets = scene.getStylesheets();
        if (hasParentStylesheets == false && sceneStylesheets.isEmpty() && userAgentStylesheets.isEmpty()) {
            return StyleMap.EMPTY_MAP;
        }
        
        final String name = node.getClass().getName();
        final int dotPos = name.lastIndexOf('.');
        final String cname = name.substring(dotPos+1);  // want Foo, not bada.bing.Foo
        final String id = node.getId();
        final long[] styleClassBits = node.impl_getStyleHelper().getStyleClassBits();
        
        key.className = cname;
        key.id = id;
        key.styleClass = styleClassBits;
        key.indices = hasParentStylesheets ? indicesOfParentsWithStylesheets : null;

        Cache cache = cacheMap.get(key);
        
        // the key is an instance variable and so we need to null the
        // key.styleClass to prevent holding a hard reference to the
        // styleClass (and its StyleHelper)
        key.styleClass = null;

        // If the cache is null, then we need to create a new Cache and
        // add it to the cache map
        if (cache == null) {

            final Key newKey = new Key();
            newKey.className = cname;
            newKey.id = id;
            newKey.styleClass = 
                styleClassBits != null 
                    ? Arrays.copyOf(styleClassBits, styleClassBits.length) 
                    : null;
            newKey.indices = hasParentStylesheets ? indicesOfParentsWithStylesheets : null;

            
            // Construct the list of Rules that could possibly apply
            final List<Rule> rules = new ArrayList<Rule>();

            // User agent stylesheets have lowest precedence and go first
            if (userAgentStylesheets.isEmpty() == false) {
                for(int n=0, nMax=userAgentStylesheets.size(); n<nMax; n++) {
                    final String fname = userAgentStylesheets.get(n);
                    
                    final StylesheetContainer container = stylesheetContainerMap.get(fname);
                    if (container != null && container.selectorPartitioning != null) {
                        final List<Rule> matchingRules = 
                                container.selectorPartitioning.match(id, cname, styleClassBits);
                        rules.addAll(matchingRules);
                    }
                }
            }
            
            // Scene stylesheets come next since declarations from
            // parent stylesheets should take precedence.
            if (sceneStylesheets.isEmpty() == false) {
                for(int n=0, nMax=sceneStylesheets.size(); n<nMax; n++) {
                    final String fname = sceneStylesheets.get(n);
                    final StylesheetContainer<Parent> container = stylesheetContainerMap.get(fname);
                    if (container != null && container.selectorPartitioning != null) {
                        container.keys.add(newKey); // remember that this stylesheet was used in this cache
                        final List<Rule> matchingRules = 
                                container.selectorPartitioning.match(id, cname, styleClassBits);
                        rules.addAll(matchingRules);
                    }
                }
            }

            // lastly, parent stylesheets
            if (hasParentStylesheets) {
                final List<StylesheetContainer<Parent>> parentStylesheets =
                    gatherParentStylesheets(((node instanceof Parent) ? (Parent) node : node.getParent()));
                final int nMax = parentStylesheets == null ? 0 : parentStylesheets.size();
                for(int n=0; n<nMax; n++) {
                    final StylesheetContainer<Parent> container = parentStylesheets.get(n);
                    container.keys.add(newKey); // remember that this stylesheet was used in this cache
                    if (container.selectorPartitioning != null) {
                        final List<Rule> matchingRules = 
                                container.selectorPartitioning.match(id, cname, styleClassBits);
                        rules.addAll(matchingRules);
                    }
                }
            }
            
            // create a new Cache from these rules.
            cache = new Cache(rules);            
            cacheMap.put(newKey, cache);
            
        }

        //
        // Create a style helper for this node from the styles that match. 
        //
        StyleMap smap = cache.getStyleMap(this, node, pseudoclassBits);
        
        return smap;        
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // CssError reporting
    //
    ////////////////////////////////////////////////////////////////////////////
    
    private static ObservableList<CssError> errors = null;
    /**
     * Errors that may have occurred during css processing.
     * This list is null until errorsProperty() is called.
     * @return
     */
    public static ObservableList<CssError> errorsProperty() {
        if (errors == null) {
            errors = FXCollections.observableArrayList();
        }
        return errors;
    }

    /**
     * Errors that may have occurred during css processing.
     * This list is null until errorsProperty() is called and is used
     * internally to figure out whether or  not anyone is interested in
     * receiving CssError.
     * Not meant for general use - call errorsProperty() instead.
     * @return
     */
    public static ObservableList<CssError> getErrors() {
        return errors;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // Classes and routines for mapping styles to a Node
    //
    ////////////////////////////////////////////////////////////////////////////

    //
    // Used by StyleHelper. The key uniquely identifies this style map.
    // These keys are used by StyleHelper in creating its StyleCacheKey.
    // The StyleCacheKey is used to lookup calculated values in cache.
    //
    static class StyleMap {
        final long uniqueId; // unique per container
        final Map<String, List<CascadingStyle>> map;
        private StyleMap(long key, Map<String, List<CascadingStyle>> map) {
            this.uniqueId = key;
            this.map  = map;
        }
        
        static final StyleMap EMPTY_MAP = 
            new StyleMap(0, Collections.EMPTY_MAP);

    }

    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    private static class Cache {

        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final Map<Long, StyleMap> cache;

        Cache(List<Rule> rules) {
            this.rules = rules;
            this.cache = new HashMap<Long, StyleMap>();
        }

        private StyleMap getStyleMap(StyleManager owner, Node node, long[] pseudoclassBits) {
            
            if (rules == null || rules.isEmpty()) {                
                return StyleMap.EMPTY_MAP;
            }


            //
            // Since the list of rules is found by matching only the
            // rightmost selector, the set of rules may larger than those 
            // rules that actually match the node. The following loop
            // whittles the list down to those rules that apply.
            //
            final Rule[] applicableRules = new Rule[rules.size()];
            
            //
            // To lookup from the cache, we construct a key from a Long
            // where the rules that match this particular node are
            // represented by bits on the Long.
            //
            long key = 0;
            long mask = 1;
            for (int r = 0, rMax = rules.size(); r < rMax; r++) {
                
                final Rule rule = rules.get(r);

                //
                // This particular flavor of applies takes a long[] and fills 
                // in the pseudoclass states from the selectors where they apply
                // to a node. This is an expedient to looking the applies loop
                // a second time on the matching rules. This has to be done 
                // ahead of the cache lookup since not all nodes that have the 
                // same set of rules will have the same node hierarchy. 
                // 
                // For example, if I have .foo:hover:focused .bar:selected {...}
                // and the "bar" node is 4 away from the root and the foo
                // node is two away from the root, pseudoclassBits would be
                // [selected, 0, hover:focused, 0]
                // Note that the states run from leaf to root. This is how
                // the code in StyleHelper expects things. 
                // Note also that, if the rule does not apply, the pseudoclassBits
                // is unchanged. 
                //
                if (rule.applies(node, pseudoclassBits)) {
                    
                    applicableRules[r] = rule;
                    key = key | mask;
                    
                } else {
                    
                    applicableRules[r] = null;
                    
                }
                mask = mask << 1;
            }
            
            // nothing matched!
            if (key == 0) {
                return StyleMap.EMPTY_MAP;
            }
            
            final Long keyObj = Long.valueOf(key);
            if (cache.containsKey(keyObj)) {
                final StyleMap styleMap = cache.get(keyObj);
                return styleMap;
            }

            int ordinal = 0;
            
            // if there isn't a map in cache already, create one. 
            // 
            // We know the rules apply, so they should also match. A rule
            // might have more than one selector and match will return the
            // selector that matches. Matches is more expensive than applies, 
            // so we pay for it here, but only for the first time the 
            // cache is created.
            //
            final List<CascadingStyle> styles = new ArrayList<CascadingStyle>();
            
            for (int r = 0, rMax = applicableRules.length; r<rMax; r++) {
                
                final Rule rule = applicableRules[r];
                
                // if the rule didn't apply, then applicableRules[r] will be null
                if (rule == null) {
                    continue;
                }
                
                final List<Match> matches = rule.matches(node);
                if (matches == null || matches.isEmpty()) {
                    continue;
                }
                
                for (int m=0, mMax=matches.size(); m<mMax; m++) {
                
                    final Match match = matches.get(m);
                    // TODO: should never get nulls in this list. Fix Rule#matches
                    if (match == null) {
                        continue;
                    }
                    
                    for (int k = 0, kmax = rule.declarations.size(); k < kmax; k++) {
                        final Declaration decl = rule.declarations.get(k);

                        final CascadingStyle s = new CascadingStyle(
                            new Style(match.selector, decl),
                            match.pseudoclasses,
                            match.specificity,
                            // ordinal increments at declaration level since
                            // there may be more than one declaration for the
                            // same attribute within a rule or within a stylesheet
                            ordinal++
                        );

                        styles.add(s);
                    }
                }
            }

                // return sorted styles
            Collections.sort(styles);

            
            // We need to create a new style map, add it to the cache,
            // and then return it.
            final Map<String, List<CascadingStyle>> smap =
                new HashMap<String, List<CascadingStyle>>();
            
            for (int i=0, max=styles.size(); i<max; i++) {
                
                final CascadingStyle style = styles.get(i);
                final String property = style.getProperty();
                
                // This is carefully written to use the minimal amount of hashing.
                List<CascadingStyle> list = smap.get(property);
                if (list == null) {
                    list = new ArrayList<CascadingStyle>(5);
                    smap.put(property, list);
                }
                list.add(style);
            }

            final StyleMap styleMap = new StyleMap(owner.nextSmapId(), smap);
            cache.put(keyObj, styleMap);
            return styleMap;
        }

    }
    
    /**
     * The key used in the cacheMap of the StylesheetContainer
     */
    private static class Key {
        // note that the class name here is the *full* class name, such as
        // javafx.scene.control.Button. We only bother parsing this down to the
        // last part when doing matching against selectors, and so want to avoid
        // having to do a bunch of preliminary parsing in places where it isn't
        // necessary.
        String className;
        String id;
        long[] styleClass;

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
        int[] indices;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Key) {
                Key other = (Key)o;
                
                if (className == null ? other.className != null : (className.equals(other.className) == false)) {
                    return false;
                }

                if (id == null ? other.id != null : (id.equals(other.id) == false)) {
                    return false;
                }

                if (styleClass == null ? other.styleClass != null : other.styleClass == null) {
                    
                    return false;
                    
                } else if (styleClass != null) {
        
                    if (styleClass.length != other.styleClass.length) {
                        return false;
                    }

                    for (int n=0, max=styleClass.length; n<max; n++) {
                        if (styleClass[n] != other.styleClass[n]) {
                            return false;
                        }
                    }
                    
                }
                
                if (indices == null ? other.indices != null : other.indices == null) {
                    
                    return false;
                    
                } else if (indices != null) {
                    
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
                
                // indices were null
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.className != null ? this.className.hashCode() : 0);
            hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 29 * hash + Arrays.hashCode(this.styleClass);
            hash = 29 * hash + Arrays.hashCode(this.indices);
            return hash;
        }

        @Override
        public String toString() {
            return "Key ["+className+", "+String.valueOf(id)+", "+String.valueOf(SimpleSelector.getStyleClassStrings(styleClass))+"]";
        }

    }
    

}
