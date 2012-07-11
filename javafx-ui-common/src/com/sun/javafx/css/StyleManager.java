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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.io.FilePermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Scene;

import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.css.StyleHelper.StyleCacheBucket;
import com.sun.javafx.css.StyleHelper.StyleCacheKey;
import com.sun.javafx.logging.PlatformLogger;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.*;
import java.util.Map.Entry;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Parent;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

public class StyleManager {

    private static class Holder {
        private static StyleManager INSTANCE = new StyleManager();
        private static PlatformLogger LOGGER = com.sun.javafx.Logging.getCSSLogger();
    }

    public static StyleManager getInstance() {
        return Holder.INSTANCE;
    }

    private static PlatformLogger logger() {
        return Holder.LOGGER;
    }

    /**
     * This stylesheet represents the "default" set of styles for the entire
     * platform. This is typically only set by the UI Controls module, and
     * otherwise is generally null. Whenever this variable changes (via the
     * setDefaultUserAgentStylesheet function call) then we will end up clearing all
     * of the caches.
     */
    private Stylesheet defaultUserAgentStylesheet;

    /**
     * A map from String => Stylesheet. If a stylesheet for the given URL has
     * already been loaded then we'll simply reuse the stylesheet rather than
     * loading a duplicate.
     */
    private ObservableMap<String,Stylesheet> authorStylesheetMap = FXCollections.observableHashMap();
    private void addAuthorStylesheet(String fname, Stylesheet stylesheet) {
        
        if (stylesheet != null) {
            stylesheet.setOrigin(Stylesheet.Origin.AUTHOR);            
            authorStylesheetMap.put(fname,stylesheet);
        } else {
            authorStylesheetMap.remove(fname);
        }
        
    }
    private Stylesheet getAuthorStylesheet(String fname) {

        return authorStylesheetMap.get(fname);
    }

    /**
     * A container for Parent stylesheets and the Parents that use them.
     * If a Parent stylesheet is removed by one parent, then all other
     * parents that use that stylesheet should get new styles if the 
     * stylesheet is added back in (typical of SceneBuilder). This container
     * provides the hooks to get back to those parents.
     * 
     * ParentStylesheetContainers are created and added to parentStylesheetMap
     * in the StylesheetContainer method gatherParentStylesheets
     */
    private static class ParentStylesheetContainer {
        // the stylesheet url
        private final String fname;
        // the parsed stylesheet so we don't reparse for every parent that uses it
        private final Stylesheet stylesheet;
        // the parents that use this stylesheet. Typically, this list should
        // be very small.
        private final RefList<Parent> parents;
        // the keys for finding Cache entries that use this stylesheet. 
        // This list should also be fairly small
        private final RefList<Key> keys;

        private ParentStylesheetContainer(String fname, Stylesheet stylesheet) {
            this.fname = fname;
            this.stylesheet = stylesheet;
            this.parents = new RefList<Parent>();
            this.keys = new RefList<Key>();
        }
    }
    
    private static class RefList<K> {
        
        private final List<Reference<K>> list = new ArrayList<Reference<K>>();
        
        private void add(K key) {

            for (int n=list.size()-1; 0<=n; --n) {
                final Reference<K> ref = list.get(n);
                final K k = ref.get();
                if (k == null) {
                    // stale reference, remove it.
                    list.remove(n);
                } else {
                    // already have it, bail
                    if (k == key) return;
                }
            }
            // not found, add it.
            list.add(new WeakReference<K>(key));
        }
        
    }
    
    /**
    * Another map from String => Stylesheet. This one is for stylesheets that
    * hang off a Parent. These are considered "author" stylesheets but are not
    * added to the authorStylesheetMap because we don't want the scene's
    * list of stylesheets in the container to be updated.
    */
    private static Map<String,ParentStylesheetContainer> parentStylesheetMap =
            new HashMap<String,ParentStylesheetContainer>();

    

    /**
     * called from Parent's stylesheets property's onChanged method
     */
    public void parentStylesheetsChanged(Parent parent, Change<String> c) {
        
        final Scene scene = parent.getScene();
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
                    ParentStylesheetContainer psc = parentStylesheetMap.remove(fname);
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
    private void clearParentCache(ParentStylesheetContainer psc) {
        
        final List<Reference<Parent>> parentList = psc.parents.list;
        final List<Reference<Key>>    keyList    = psc.keys.list;
        for (int n=parentList.size()-1; 0<=n; --n) {
            
            final Reference<Parent> ref = parentList.get(n);
            final Parent parent = ref.get();
            if (parent == null) continue;
            
            final Scene scene = parent.getScene();
            if (scene == null) continue;

            StylesheetContainer container = null;            
            if (containerMap != null 
                && (container = get(containerMap, scene)) != null) {
                clearParentCache(container, keyList);
            }

            if (defaultContainer != null) {
                clearParentCache(defaultContainer, keyList);
            }
            
            // tell parent it needs to reapply css
            parent.impl_reapplyCSS();
        }
    }
    
    // RT-22565: Called from clearParentCache to clear the cache entries.
    private void clearParentCache(StylesheetContainer container, List<Reference<Key>> keyList) {
        
        if (container.cacheMap.isEmpty()) return;
        
        for (int n=keyList.size()-1; 0<=n; --n) {
            
            final Reference<Key> ref = keyList.get(n);
            final Key key = ref.get();
            if (key == null) continue;
            
            final Cache cache = container.cacheMap.remove(key);
        }
    }
    
    /**
     * A map from Scene => StylesheetContainer. This provides us a way to find
     * the stylesheets which apply to any given scene.
     */
    private Map<WeakReference<Scene>, StylesheetContainer> containerMap;

    /**
     * Remove mappings for keys with null referents.
     * Should only be called from get and put.
     * @param map
     */
    private void expunge(Map<WeakReference<Scene>, StylesheetContainer> map) {

        // avoid concurrent modification
        List<WeakReference<Scene>> mappingsToRemove = null;

        for (WeakReference<Scene> key : map.keySet()) {
            if (key.get() == null) {
                if (mappingsToRemove == null) mappingsToRemove =
                    new ArrayList<WeakReference<Scene>>();
                mappingsToRemove.add(key);
            }
        }

        if (mappingsToRemove != null) {
            for (int n=mappingsToRemove.size()-1; 0 <= n; --n) {
                StylesheetContainer container =
                    map.remove(mappingsToRemove.remove(n));
                container.destroy();
            }
        }
    }

    /**
     * Rather than create a new class just to handle the expunge, this helper
     * function will expunge the containerMap and then put the new key and value
     * into the map.
     * @param map The map to operate on
     * @param scene The scene to add as the referent in the key
     * @param container The container to add as the value
     */
    private void put(Map<WeakReference<Scene>, StylesheetContainer> map,
            Scene scene, StylesheetContainer container) {

        expunge(map);
        map.put(new WeakReference(scene), container);

    }

    /**
     * Rather than create a new class just to handle the expunge, this helper
     * function will expunge the containerMap and then get the container
     * for the scene.
     * @param map The map to operate on
     * @param scene The scene which will be a referent in the key
     * @return container The container to associated with the scene or null.
     */
    private StylesheetContainer get(Map<WeakReference<Scene>, StylesheetContainer> map,
            Scene scene) {

        expunge(map);

        for(Entry<WeakReference<Scene>, StylesheetContainer> entry : map.entrySet()) {
            if (scene.equals(entry.getKey().get())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Rather than create a new class just to handle the expunge, this helper
     * function will expunge the containerMap and then remove the container
     * for the scene. It is the responsibility of the caller to destroy the
     * returned container.
     * @param map The map to operate on
     * @param scene The scene which will be a referent in the key
     * @return container The container to associated with the scene or null.
     */
    private StylesheetContainer remove(Map<WeakReference<Scene>, StylesheetContainer> map,
            Scene scene) {

        expunge(map);

        // avoid concurrent modification
        WeakReference<Scene> keyToRemove = null;
        for(WeakReference<Scene> key : map.keySet()) {
            if (scene.equals(key.get())) {
                keyToRemove = key;
                break;
            }
        }

        StylesheetContainer container = null;

        if (keyToRemove != null) {
            container = map.remove(keyToRemove);
            // There is no call to container.destroy() here since the caller
            // might want to do something with the container. In the JavaFX 
            // 2.0.2 implementation, the only place that remove is called is 
            // from updateStylesheets which removes the container and then
            // destroys it.
            keyToRemove.clear();
        }

        return container;
    }

    /**
     * Used for retrieving for nodes which have a "style" defined, and which are
     * rooted in a Scene, but who's Scene doesn't have any stylesheets of its
     * own and therefore doesn't have an entry in the containerMap.
     */
    private StylesheetContainer defaultContainer;

    private StyleManager() {
    }

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

    private Stylesheet loadStylesheet(final String fname) {
        try {
            return loadStylesheetUnPrivileged(fname);
        } catch (java.security.AccessControlException ace) {
            if (logger().isLoggable(PlatformLogger.INFO)) {
                logger().info("Could not load the stylesheet, trying with FilePermissions : " + fname);
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
                    if (styleManagerJarPath.equals(requestedFileJarPart.toString())) {
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


    private Stylesheet loadStylesheetUnPrivileged(final String fname) {

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

            final String name =
                (fname.endsWith(".css") || fname.endsWith(".bss")) ?
                    fname.substring(0, fname.length() - 4) : fname;

            java.net.URL url = getURL(name+ext);
            if (url == null && (parse = !parse)) {
                // If we failed to get the URL for the .bss file,
                // fall back to the .css file.
                // Note that 'parse' is toggled in the test.
                url = getURL(name+".css");
            }

            Stylesheet stylesheet = null;
            if ((url != null) && !parse) {
                stylesheet = Stylesheet.loadBinary(url);

                if (stylesheet == null && (parse = !parse)) {
                    // If we failed to load the .bss file,
                    // fall back to the .css file.
                    // Note that 'parse' is toggled in the test.
                    url = getURL(name+".css");
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
                if (logger().isLoggable(PlatformLogger.WARNING)) {
                    logger().warning(
                        String.format("Resource \"%s\" not found.", fname)
                    );
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
            if (logger().isLoggable(PlatformLogger.INFO)) {
                logger().info("Could not find stylesheet: " + fname);//, fnfe);
            }
        } catch (IOException ioe) {
                if (errors != null) {
                    CssError error = 
                        new CssError(
                            "Could not load stylesheet: " + fname
                        );
                    errors.add(error);
                }
            if (logger().isLoggable(PlatformLogger.INFO)) {
                logger().info("Could not load stylesheet: " + fname);//, ioe);
            }
        }
        return null;
    }

    
    /**
     * User agent stylesheets from Control.getUserAgentStylesheet.
     */
    private Map<String,Stylesheet> userAgentStylesheetMap = new HashMap<String,Stylesheet>();

    
    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public void addUserAgentStylesheet(String fname) {
        addUserAgentStylesheet(null, fname);
    }
    
    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param fname  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643    
    public void addUserAgentStylesheet(Scene scene, String fname) {
                
        // nothing to add
        if (fname == null ||  fname.trim().isEmpty()) return;

        if (userAgentStylesheetMap.containsKey(fname) == false) {

            // RT-20643
            CssError.setCurrentScene(scene);
            
            Stylesheet ua_stylesheet = loadStylesheet(fname);
            ua_stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
            userAgentStylesheetMap.put(fname, ua_stylesheet);
            
            if (ua_stylesheet != null) {
                userAgentStylesheetsChanged();                
            }
            
            // RT-20643
            CssError.setCurrentScene(null);
        }

    }

    /**
     * Set the default user agent stylesheet.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public void setDefaultUserAgentStylesheet(String fname) {
        setDefaultUserAgentStylesheet(null, fname);
    }
    
    /**
     * Set the default user agent stylesheet
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param fname  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643
    public void setDefaultUserAgentStylesheet(Scene scene, String fname) {

        if (fname == null || fname.trim().isEmpty())
            throw new IllegalArgumentException("null arg fname");

        // RT-20643
        CssError.setCurrentScene(scene);
        
        Stylesheet ua_stylesheet = loadStylesheet(fname);
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
            setDefaultUserAgentStylesheet(ua_stylesheet);
        } 
        
        // RT-20643
        CssError.setCurrentScene(null);

    }
    
    /**
     * Set the user agent stylesheet. This is the base default stylesheet for
     * the platform
     */
    public void setDefaultUserAgentStylesheet(Stylesheet stylesheet) {
        defaultUserAgentStylesheet = stylesheet;
        if (defaultUserAgentStylesheet != null) {
            defaultUserAgentStylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        }
        userAgentStylesheetsChanged();
    }
    
    private void userAgentStylesheetsChanged() {
        if (defaultContainer != null) {
            //
            // RT-21563 if default ua stylesheet is set, then the default 
            // container must be destroyed so it will be rebuilt. If this is 
            // the first time the ua stylesheet is set, then the defaultContainer
            // won't have any rules so it must be destroyed so it will be rebuilt. 
            // 
            defaultContainer.destroy();
            defaultContainer = null;
        }
        if (containerMap != null) {
            Iterator<StylesheetContainer> iter = containerMap.values().iterator();
            while(iter.hasNext()) {
                StylesheetContainer sc = iter.next();
                sc.destroy();
            }
            containerMap.clear();
        }

        for (Iterator<Window> it = Window.impl_getWindows(); it.hasNext(); ) {
            Scene scene = it.next().getScene();
            if (scene != null) {
                updateStylesheets(scene);
                scene.getRoot().impl_reapplyCSS();
            }            
        }        
    }

    /** 
     * Force the author stylesheets associated with the given scene to be re-parsed.
     * The author stylesheets are the stylesheets specified in
     * Scene.stylesheets. User agent stylesheets are not re-parsed.
     * 
     * To avoid the overhead of parsing, the StyleManager maintains a cache of 
     * parsed stylesheets. Calling this method flushes the cache and then 
     * calls updateStylesheets. CSS is then reapplied from the root of the
     * scene on down. This is an expensive method!
     * 
     * @param scene
     */
    public void reloadStylesheets(Scene scene) {
        this.authorStylesheetMap.clear();
        updateStylesheets(scene);
        scene.getRoot().impl_reapplyCSS();
    }

    /**
     *
     * @param scene
     * @param stylesheet
     * @deprecated Adding or removing a stylesheet to the scene is sufficient
     */
    public void replaceStylesheet(Scene scene, Stylesheet stylesheet) {

        StylesheetContainer container =
            (containerMap != null) ? get(containerMap, scene) : null;

        if (container != null) {
            int index = container.stylesheets.indexOf(stylesheet);
            if (index > -1) {
                container.stylesheets.set(index, stylesheet);
            } else {
                container.stylesheets.add(stylesheet);
            }
            container.clearCaches();
        } else {
            if (containerMap == null) containerMap = 
                new HashMap<WeakReference<Scene>,StylesheetContainer>();

            container = new StylesheetContainer(null);
            container.stylesheets.add(stylesheet);
            put(containerMap, scene, container);
        }

        scene.getRoot().impl_reapplyCSS();
    }

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
    public void updateStylesheets(Scene scene) {
        // RT-7788
        // If there is no stage, then we're dealing with something like a popup
        // or a scene that hasn't been added to a stage yet. While there may
        // be a stylesheet for this scene, it is possible that the user agent
        // stylesheet will get set some time after this call to updateStylesheets
        // and the containerMap will get reset. A scene with no stage won't get added
        // back into the containerMap unless its stylesheet changes (which
        // is how it got there in the first place
        if (scene.getWindow() == null) return;

        //
        // Here we remove the Scene's container from the container map.
        // This ensures that the Scene's nodes will either get their
        // styles from the default container (if Scene.stylesheets is empty)
        // or that a new cachMap will be computed for the nodes from the
        // new set of styles.
        //
        // This should happen infrequently since adding and removing stylesheets
        // on the fly should be the exception rather than the rule.
        //
        if (containerMap != null) {

            final StylesheetContainer container = remove(containerMap, scene);

            // annihilate the container
            if (container != null) {
                container.destroy();
            }
        }

        // if there are no stylesheets, then they were probably all removed.
        if (scene.getStylesheets().size() == 0) return;

        // RT-20643
        CssError.setCurrentScene(scene);
        
        // create the stylesheets, one per URL supplied
        final Collection<Stylesheet> stylesheets = new ArrayList<Stylesheet>();
        for (int i = 0; i < scene.getStylesheets().size(); i++) {
            String stringUrl = scene.getStylesheets().get(i);
            stringUrl = stringUrl.trim();
            try {
                
                Stylesheet ss = loadStylesheet(stringUrl);
                addAuthorStylesheet(stringUrl, ss);
                if (ss != null) stylesheets.add(ss);

            } catch (Exception e) {
                // If an exception occurred while loading one of the stylesheets
                // then we will simply print warning into system.err and skip the
                // stylesheet, allowing other stylesheets to attempt to load
                System.err.printf("Cannot add stylesheet. %s\n", e.getLocalizedMessage());
            }
        }

        // RT-20643
        CssError.setCurrentScene(scene);
        
        // Look up existing user stylesheets and add new stylesheets.
        // We defer creation of the containerMap until needed
        if (containerMap == null) containerMap =
            new HashMap<WeakReference<Scene>,StylesheetContainer>();

        StylesheetContainer container = new StylesheetContainer(stylesheets);
        put(containerMap, scene, container);
    }

    private final Map<String,Long> pseudoclassMasks = new HashMap<String,Long>();

    public long getPseudoclassMask(String pclass) {
        Long mask = pseudoclassMasks.get(pclass);
        if (mask == null) {
            final int exp = pseudoclassMasks.size();
            mask = Long.valueOf(1L << exp); // same as Math.pow(2,exp)
            pseudoclassMasks.put(pclass,mask);
        }
        return mask.longValue();
    }

    long getPseudoclassMask(List<String> pseudoclasses) {
        long mask = 0;
        
        final int max = pseudoclasses != null ? pseudoclasses.size() : -1;
        for (int n=0; n<max; n++) {
            long m = getPseudoclassMask(pseudoclasses.get(n));
            mask = mask | m;
        }
        return mask;
    }

    public List<String> getPseudoclassStrings(long mask) {
        if (mask == 0) return Collections.EMPTY_LIST;

        Map<Long,String> stringMap = new HashMap<Long,String>();
        for (Entry<String,Long> entry : pseudoclassMasks.entrySet()) {
            stringMap.put(entry.getValue(), entry.getKey());
        }
        List<String> strings = new ArrayList<String>();
        for (long exp=0; exp < Long.SIZE; exp++) {
            long key = (1L << exp) & mask;
            if (key != 0) {
                String value = stringMap.get(key);
                if (value != null) strings.add(value);
            }
        }
        return strings;
    }

    /*
     * Find the matching styles for this node and return them in order of
     * ascending specificity
     */
    public StyleHelper getStyleHelper(Node node) {

        assert(node != null && node.getScene() != null);

        // if there is no container map then we can take a fast path which
        // simply uses the default container
        boolean fastPath = containerMap == null;

        // We have special handling for the case of a node which is in a popup
        // scene. The problem is that the root scene used for the purpose of style
        // sheet support isn't the scene of the popup (which is a hidden API
        // member), but rather the main stage's scene. So we need the scene of the
        // root stage.
        StylesheetContainer container = null;
        if (! fastPath) {
            Window window = null;
            if (node != null && node.getScene() != null) {
                window = node.getScene().getWindow();
                while (window instanceof PopupWindow) {
                    PopupWindow popup = (PopupWindow) window;
                    window = popup.getOwnerWindow();
                }
            }
            Scene scene;
            if (window == null) {
                scene = node.getScene();
            } else {
                scene = window.getScene();
            }

            if (scene != null) {
                // get the container from the containerMap. If it is null, then we
                // will end up using the fast path
                container = get(containerMap,scene);
            }

            fastPath = container == null;
        }

        if (fastPath) {
            if (defaultContainer == null) defaultContainer = new StylesheetContainer(null);
            return defaultContainer.getStyleHelper(node);
        }

        // Now that we have the container we can ask the container for the
        // StyleHelper. The container implements some additional caching and
        // fast paths.
        return container.getStyleHelper(node);
    }

    /**
     * Contains the stylesheet state for a single scene. This includes both the
     * Stylesheets defined on the Scene itself as well as a map of stylesheets
     * for "style"s defined on the Node itself. These containers are kept in the
     * containerMap, key'd by the Scene to which they belong.
     * <p>
     * One of the key responsibilities of the StylesheetContainer is to create and
     * maintain an admittedly elaborate series of caches so as to minimize the
     * amount of time it takes to match a Node to its eventual StyleHelper, and
     * to reuse the StyleHelper as much as possible.
     * <p>
     * Initially, the cache is empty. It is recreated whenever the
     * userStylesheets on the container change, or whenever the userAgentStylesheet
     * changes. The cache is built up as nodes are looked for, and thus there is
     * some overhead associated with the first lookup but which is then not
     * repeated for subsequent lookups.
     * <p>
     * The cache system used is a two level cache. The first level cache simply
     * maps the classname/id/styleclass combination of the request node to a 2nd
     * level cache. If the node has "styles" specified then we still use this 2nd
     * level cache, but must combine its rules with the rules specified in "styles"
     * and perform more work to cascade properly.
     * <p>
     * The 2nd level cache contains a data structure called the Cache. The
     * Cache contains an ordered sequence of Rules, a Long, and a Map.
     * The ordered sequence of rules are the rules that *may* match a node with
     * the given classname, id, and style class. For example, rules which may
     * apply are any rule where the simple selector of the rule contains a reference
     * to the id, style class, or classname of the Node, or a compound selector
     * who's "descendant" part is a simple selector which contains a reference to
     * the id, style class, or classname of the Node.
     * <p>
     * During lookup, we will iterate over all the potential rules and discover
     * if they apply to this particular node. If so, then we toggle a bit position
     * in the Long corresponding to the position of the rule that matched. This
     * long then becomes our key into the final map.
     * <p>
     * Once we have established our key, we will visit the map and look for an
     * existing StyleHelper. If we find a StyleHelper, then we will return it. If
     * not, then we will take the Rules that matched and construct a new
     * StyleHelper from their various parts.
     * <p>
     * This system, while elaborate, also provides for numerous fast paths and
     * sharing of data structures which should dramatically reduce the memory and
     * runtime performance overhead associated with CSS by reducing the matching
     * overhead and caching as much as possible. We make no attempt to use weak
     * references here, so if memory issues result one work around would be to
     * toggle the root user agent stylesheet or stylesheets on the scene to cause
     * the cache to be flushed.
     */
    static int hits =0;
    static int misses = 0;
    static int puts = 0;
    static int tries = 0;
    private static class StylesheetContainer {

        private final List<Stylesheet> stylesheets;

        /**
         * The map of Caches, key'd by a combination of class name, style class,
         * and id.
         */
        private final Map<Key, Cache> cacheMap;

        /**
         * This instance is reused for each lookup in the cache so as to avoid
         * creating temporary objects as much as reasonable.
         */
        private final Key key;

        // The StyleHelper's cached values are relevant only for a given scene.
        // Since a StylesheetContainer is created for a Scene with stylesheets,
        // it makes sense that the container should own the valueCache. This
        // way, each scene gets its own valueCache.
        private final Map<StyleCacheKey, StyleCacheBucket> styleCache;
        
        private int smapCount;

        private StylesheetContainer(final Collection<Stylesheet> authorStylesheets) {

            cacheMap = new HashMap<Key, Cache>();
            key = new Key();

            stylesheets = new ArrayList<Stylesheet>();

            if (StyleManager.getInstance().defaultUserAgentStylesheet != null) {
                stylesheets.add(StyleManager.getInstance().defaultUserAgentStylesheet);
            }            

            if (StyleManager.getInstance().userAgentStylesheetMap != null &&
                   StyleManager.getInstance().userAgentStylesheetMap.isEmpty() == false) {
                stylesheets.addAll(StyleManager.getInstance().userAgentStylesheetMap.values());
            }            
            
            if (authorStylesheets != null) stylesheets.addAll(authorStylesheets);
            
            styleCache = new HashMap<StyleCacheKey, StyleCacheBucket>();
            smapCount = 0;
        }

        private void destroy() {
            stylesheets.clear();
            clearCaches();
        }

        private void clearCaches() {
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
            if (parent == null) return new int[count];
            final int[] indices = getIndicesOfParentsWithStylesheets(parent.getParent(), ++count);
            // logic elsewhere depends on indices of parents
            // with no stylesheets being -1
            if (parent.getStylesheets().isEmpty() == false) {
                indices[indices.length-count] = parent.hashCode();
            } else {
                indices[indices.length-count] = -1;
            }
            return indices;
        }
        
        //
        // recurse so that stylesheets of Parents closest to the root are 
        // added to the list first. The ensures that declarations for 
        // stylesheets further down the tree (closer to the leaf) have
        // a higer ordinal in the cascade.
        //
        private List<ParentStylesheetContainer> gatherParentStylesheets(Parent parent) {
            
            if (parent == null) return null; 
            
            final List<String> parentStylesheets = parent.impl_getAllParentStylesheets();
            
            if (parentStylesheets == null || parentStylesheets.isEmpty()) return null;
            
            final List<ParentStylesheetContainer> list = new ArrayList<ParentStylesheetContainer>();
            
            for (int n=0, nMax=parentStylesheets.size(); n<nMax; n++) {
                final String fname = parentStylesheets.get(n);
                ParentStylesheetContainer container = null;
                if (parentStylesheetMap.containsKey(fname)) {
                    container = parentStylesheetMap.get(fname);
                    // RT-22565: remember that this parent uses this stylesheet.
                    // Later, if the cache is cleared, the parent is told to 
                    // reapply css.
                    container.parents.add(parent);
                } else {
                    final Stylesheet stylesheet = 
                        StyleManager.getInstance().loadStylesheet(fname);
                    // stylesheet may be null which would mean that some IOException
                    // was thrown while trying to load it. Add it to the 
                    // parentStylesheetMap anyway as this will prevent further
                    // attempts to parse the file
                    container =
                            new ParentStylesheetContainer(fname, stylesheet);
                    // RT-22565: remember that this parent uses this stylesheet.
                    // Later, if the cache is cleared, the parent is told to 
                    // reapply css.
                    container.parents.add(parent);
                    parentStylesheetMap.put(fname, container);
                }
                if (container != null) list.add(container);
            }
            
            return list;
        }
        
        /**
         * Returns a StyleHelper for the given Node, or null if there are no
         * styles for this node.
         */
        private StyleHelper getStyleHelper(Node node) {
            
            // Populate our helper key with the class name, id, and style class
            // of the node and lookup the associated Cache in the cacheMap
            final String className = node.getClass().getName();
            final String id = node.getId();
            final List<String> styleClass = node.getStyleClass();
            
            final int[] indicesOfParentsWithStylesheets =  
                getIndicesOfParentsWithStylesheets(
                    ((node instanceof Parent) ? (Parent)node : node.getParent()), 0
                );
            //
            // avoid calling gatherParentStylesheets later if we know there
            // aren't any parent stylesheets. If there are parent stylesheets,
            // then at least one element of indicesOfParentsWithStylesheets
            // will be other than -1
            //
            boolean hasParentStylesheets = false;
            for (int n=0; n<indicesOfParentsWithStylesheets.length; n++) {
                // assignment, not equality here!
                if (hasParentStylesheets = (indicesOfParentsWithStylesheets[n] != -1)) break;
            }
            
            key.className = className;
            key.id = id;
            key.styleClass = styleClass;
            key.indices = hasParentStylesheets ? indicesOfParentsWithStylesheets : null;
            
            Cache cache = cacheMap.get(key);

            // the key is an instance variable and so we need to null the
            // key.styleClass to prevent holding a hard reference to the
            // styleClass (and its Node)
            key.styleClass = null;
            
            // If the cache is null, then we need to create a new Cache and
            // add it to the cache map
            if (cache == null) {
                // Construct the list of Rules that could possibly apply
                final List<Rule> rules = new ArrayList<Rule>();

                /*
                 * A Set of all the pseudoclass states which, if they change, need
                 * to cause the Node to be set to UPDATE its CSS styles on the next
                 * pulse. For a full explanation, see the StyleHelper's
                 * mayImpactChildren variable.
                 */
                long pseudoclassStateMask = 0;
                
                // If the node can have children, look at the ancestors
                // in a compound selector for pseudoclassBits
                final boolean canHaveChildren = node instanceof Parent;

                // It may be that there are no pseudoclassBits in the ancestor
                // selectors, but if one of the ancestor selectors might apply,
                // then the selector impacts its children.
                boolean impactsChildren = false;
                
                final List<ParentStylesheetContainer> parentStylesheets = 
                    hasParentStylesheets 
                        ? gatherParentStylesheets(
                            ((node instanceof Parent) ? (Parent)node : node.getParent())
                        )
                        : null;
                
                //
                List<Stylesheet> stylesheetsToProcess = null;
                
                if (parentStylesheets == null || parentStylesheets.isEmpty()) {
                    stylesheetsToProcess = stylesheets;
                } else {
                    
                    //
                    // if bypassing shared cache, then the node is a Parent
                    // with stylesheets or has a Parent with stylesheets. 
                    // Gather up all of the Parent stylesheets for procesing.
                    //
                    
                    // scene stylesheets come first since declarations from
                    // Parent stylesheets should take precedence.
                    stylesheetsToProcess = new ArrayList<Stylesheet>(parentStylesheets.size());
                    for (int n=0, nMax=parentStylesheets.size(); n<nMax; n++) {
                        final ParentStylesheetContainer psc = parentStylesheets.get(n);
                        stylesheetsToProcess.add(psc.stylesheet);
                    }
                    stylesheetsToProcess.addAll(0,stylesheets);
                }                    

                for (int i = 0, imax = stylesheetsToProcess.size(); i < imax; i++) {
                    final Stylesheet ss = stylesheetsToProcess.get(i); 
                    
                    final List<Rule> stylesheetRules = ss != null ? ss.getRules() : null;
                    if (stylesheetRules == null || stylesheetRules.isEmpty()) continue;

                    for (int j = 0, jmax = stylesheetRules.size(); j < jmax; j++) {
                        Rule rule = stylesheetRules.get(j);
                        boolean mightApply = rule.mightApply(className, id, styleClass);
                        if (mightApply) {
                            rules.add(rule);
                        }

                        //
                        // The following logic used to be in a separate routine,
                        // but the need to flag compound selectors with no
                        // pseudoclass state that might impact the node's
                        // children encouraged moving it here.
                        //

                        // The following loop creates the pseudoclassStateMask.
                        // For a leaf node, the loop will just gather the
                        // pseudoclass state bits. For a parent node, this loop
                        // looks at the ancestor selectors to see if the parent
                        // matches. If so, then the pseudoclass state from that
                        // parent will affect the styling of its child nodes.
                        // For example, in the selector A:foo B { }, whenever the
                        // "foo" pseudoclass of A changes it must notify child
                        // nodes of the change so that if one of them happens to
                        // be a B it can update itself accordingly. Since this
                        // can be quite expensive, we want to make sure we
                        // support this capability only for nodes that need it.
                        // This function determines whether this support is
                        // necessary (setting impactsChildren to true),
                        // and what pseudoclasses on the parent (A) that matter.

                        // The node has to have rules that apply or be a Parent
                        // in order for the pseudoclass state to matter.
                        if (!mightApply && !canHaveChildren) continue;

                        // For each selector, rule, stylesheet look for whether this Node
                        // is referenced in the ancestor part of the selector, and whether or
                        // not it also has pseudoclasses specified
                        final int smax = rule.selectors != null ? rule.selectors.size() : 0;
                        for (int s = 0; s < smax; s++) {
                            final Selector selector = rule.selectors.get(s);
                            if (selector instanceof CompoundSelector) {

                                final CompoundSelector cs = (CompoundSelector)selector;
                                final List<SimpleSelector> csSelectors = cs.getSelectors();

                                // if mightApply is true, then the right-most selector
                                // was matched and we just need the pseudoclass state
                                // from that selector.
                                if (mightApply) {
                                    final SimpleSelector simple =
                                        csSelectors.get(csSelectors.size()-1);
                                    pseudoclassStateMask = pseudoclassStateMask |
                                        StyleManager.getInstance().getPseudoclassMask(simple.getPseudoclasses());


                                // Otherwise, the rule did not match but we need to check
                                // to see if this node matches one of the ancestor selectors.
                                // This only matters for nodes that can have children. You
                                // wouldn't find a Rectangle, for example, as an ancestor
                                // in a compound selector (you might, but the selector
                                // wouldn't match anything)
                                } else if (canHaveChildren) {
                                    // only check the ancestor selectors. If we are here,
                                    // then we know mightApply is false, meaning the rule
                                    // does not apply to this node because this node does
                                    // not match the right-most selector.
                                    for (int sctr = 0, max_sctr = csSelectors.size()-1; sctr < max_sctr; sctr++) {
                                        final SimpleSelector simple = csSelectors.get(sctr);
                                        if (simple.mightApply(className, id, styleClass)) {
                                            pseudoclassStateMask = pseudoclassStateMask |
                                                StyleManager.getInstance().getPseudoclassMask(simple.getPseudoclasses());
                                            impactsChildren = true;
                                        }
                                    }
                                }
                                // Not a compound selector. If the selector might apply,
                                // then save off the pseudoclass state. StyleHelper
                                // checks this to see if a pseudoclass matters or not.
                            } else if (mightApply) {
                                SimpleSelector simple = (SimpleSelector)selector;
                                pseudoclassStateMask = pseudoclassStateMask |
                                    StyleManager.getInstance().getPseudoclassMask(simple.getPseudoclasses());
                            }
                        }
                    }
                }

                // 
                // regardless of whether or not the cache is shared, a Cache
                // object is still needed in order to do the lookup.
                //
                final Key newKey = new Key();
                newKey.className = className;
                newKey.id = id;
                // Copy the list.
                // If the contents of the Node's styleClass changes,
                // the cacheMap lookup should miss.
                final int nElements = styleClass.size();
                newKey.styleClass = new ArrayList<String>(nElements);
                for(int n=0; n<nElements; n++) newKey.styleClass.add(styleClass.get(n));
                newKey.indices = hasParentStylesheets ? indicesOfParentsWithStylesheets : null;
                
                cache = new Cache(this, rules, pseudoclassStateMask, impactsChildren);
                cacheMap.put(newKey, cache);
                
                // RT-22565: remember where this cache is used if there are 
                // parent stylesheets involve so the cache can be cleared later
                // from the parentStylesheetsChanged method.
                final int nMax = parentStylesheets != null ? parentStylesheets.size() : 0;
                for (int n=0; n<nMax; n++) {
                    final ParentStylesheetContainer psc = parentStylesheets.get(n);
                    psc.keys.add(newKey);
                }
                
            }
            // Return the style helper looked up by the cache. The cache will
            // create a style helper if necessary (and possible), so we don't
            // have to worry about that part.
            StyleMap smap = cache.getStyleMap(node);
            if (smap == null) return null;
            
            StyleHelper helper = 
                StyleHelper.create(
                    node, 
                    smap.map, 
                    styleCache, 
                    cache.pseudoclassStateMask, 
                    smap.uniqueId
                );
            return helper;
        }
    }

    private ObservableList<CssError> errors = null;
    /** 
     * Errors that may have occurred during css processing. 
     * This list is null until errorsProperty() is called.
     * @return 
     */
    public ObservableList<CssError> errorsProperty() {
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
    public ObservableList<CssError> getErrors() {
        return errors;
    }
    
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
        
    }
    
    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    private static class Cache {
        private final StylesheetContainer owner;
        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final long pseudoclassStateMask;
        private final boolean impactsChildren;
        private final Map<Long, StyleMap> cache;
        
        Cache(StylesheetContainer owner, List<Rule> rules, long pseudoclassStateMask, boolean impactsChildren) {
            this.owner = owner;
            this.rules = rules;
            this.pseudoclassStateMask = pseudoclassStateMask;
            this.impactsChildren = impactsChildren;
            this.cache = new HashMap<Long, StyleMap>();
        }

        private StyleMap getStyleMap(Node node) {

            // If this set of rules (which may be empty) impacts children,
            // then this node will get a StyleHelper.
            if (!impactsChildren) {

                // Since this set of rules does not impact children, this node
                // will only get a StyleHelper if there are styles that might
                // apply. There are styles that might apply if there are rules
                // or if the node has an in-line style.
                final String nodeStyle = node.getStyle();
                final boolean hasStyle = nodeStyle != null && !nodeStyle.isEmpty();

                if (rules.isEmpty() && pseudoclassStateMask == 0 && hasStyle == false) {
                    boolean hasInheritAsDefault = false;
                    // TODO This is questionable as what happens when a node has no style helper and inherits styles
                    final List<StyleableProperty> styleables = node.impl_getStyleableProperties();
                    final int max = styleables != null ? styleables.size() : 0;
                    for (int i = 0; i < max; i++) {
                        if (styleables.get(i).isInherits()) {
                            hasInheritAsDefault = true;
                            break;
                        }
                    }
                    // if we have no rules and no inherited properties we don't need a StyleHelper
                    if (! hasInheritAsDefault) return null;
                }
            }
            
            // To lookup from the cache, we construct a key from a Long
            // where the rules that apply to this particular node are
            // represented by bits on the Long.
            long key = 0;
            long mask = 1;
            for (int i = 0, imax = rules.size(); i < imax; i++) {
                Rule rule = rules.get(i);
                if (rule.applies(node)) {
                    key = key | mask;
                }
                mask = mask << 1;
            }
            
            final Long keyObj = Long.valueOf(key);            
            if (cache.containsKey(keyObj)) {
                final StyleMap styleMap = cache.get(keyObj);
                return styleMap;
            } 
            
            // We need to create a new StyleHelper, add it to the cache,
            // and then return it.
            final List<CascadingStyle> styles = getStyles(node);
            final Map<String, List<CascadingStyle>> smap = 
                new HashMap<String, List<CascadingStyle>>();
            final int max = styles != null ? styles.size() : 0;
            for (int i=0; i<max; i++) {
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

        /**
         * Looks up all the styles for this Node. This function takes no advantage
         * of any caches but simply does a complete fresh lookup. The styles
         * returned are sorted such that the most specific style is first.
         */
        private List<CascadingStyle> getStyles(Node node) {
            // The priority of stylesheets from the CSS spec is
            // (1) user agent declarations
            // (2) user normal declarations
            // (3) author normal declarations
            // (4) author important declarations
            // (5) user important declarations
            // we have the correct order here for 1,2,3 and the 4 & 5
            // we handle when sorting the styles

            if (rules == null || rules.isEmpty()) return null;

            List<CascadingStyle> styles = new ArrayList<CascadingStyle>(rules.size());
                // Order in which the declaration of a Style appears (see below)
                int ordinal = 0;
                // For each declaration in the matching rules, create a Style object
                //final List<Style> styles = new ArrayList<Style>();
                for (int i = 0, imax = rules.size(); i < imax; i++) {
                    final Rule rule = rules.get(i);
                    List<Match> matches = rule.matches(node);
                    for (int j = 0, jmax = matches.size(); j < jmax; j++) {
                        final Match match = matches.get(j);
                        if (match == null) continue;
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
            return styles;
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
        List<String> styleClass;
        
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
            if (this == o) return true;
            if (o instanceof Key) {
                Key other = (Key)o;
                boolean eq =
                        className.equals(other.className)
                    && (   (indices == null && other.indices == null)
                        || (indices != null && other.indices != null)
                       )
                    && (   (id == null && other.id == null)
                        || (id != null && id.equals(other.id))
                       )
                    && (   (styleClass == null && other.styleClass == null)
                        || (styleClass != null && styleClass.containsAll(other.styleClass))
                       );
                
                if (eq && indices != null) {
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
                }
                return eq;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = className.hashCode();
            hash = 31 * (hash + ((id == null || id.isEmpty()) ? 1231 : id.hashCode()));
            hash = 31 * (hash + ((styleClass == null || styleClass.isEmpty()) ? 1237 : styleClass.hashCode()));
            if (indices != null) hash = 31 * (hash + Arrays.hashCode(indices));
            return hash;
        }

        @Override
        public String toString() {
            return "Key ["+className+", "+String.valueOf(id)+", "+String.valueOf(styleClass)+"]";
        }
        
    }

}
