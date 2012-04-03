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
import com.sun.javafx.logging.PlatformLogger;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
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
     * User agent stylesheets from Control.getUserAgentStylesheet.
     * This does not include the default ua styleheet.
     */
    private ObservableMap<String,Stylesheet> userAgentStylesheetMap = FXCollections.observableHashMap();
    private void addUserAgentStylesheet(String fname, Stylesheet stylesheet) {

        if (stylesheet != null) {
            stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
            userAgentStylesheetMap.put(fname, stylesheet);
        } else {
            userAgentStylesheetMap.remove(fname);
        }
        
    }
    private Stylesheet getUserAgentStylesheet(String fname) {

            return userAgentStylesheetMap.get(fname);
            
    }

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
     * Another map from String => Stylesheet. This one is for stylesheets that
     * hang off a Parent. These are considered "author" stylesheets but are not
     * added to the authorStylesheetMap because we don't want the scene's
     * list of stylesheets in the container to be updated.
     */
    private static Map<String,Stylesheet> parentStylesheetMap =
            new HashMap<String,Stylesheet>();

    /**
     * called from Parent's stylesheets onChanged method
     */
    public void parentStylesheetsChanged(Change<String> c) {
        while (c.next()) {
            
            List<String> list = null;
            // If the String was removed, remove it from parentStylesheetMap since
            // it isn't referenced any more. It may be referenced by some other
            // parent, in which case the stylesheet will be reparsed and added
            // back to parentStylesheetMap when the StyleHelper is recreated.
            if (c.wasRemoved()) list = c.getRemoved();
            // If the String was added, remove it from parentStylesheetMap. When
            // the StyleHelper is recreated, the stylesheet will be reparsed
            // and added back to the parentStyleMap.
            else if (c.wasAdded()) list = c.getAddedSubList();
            
            // must have been a permutation. continue on to the next change.
            if (list == null) continue;
            
            for (int n=0, nMax=list.size(); n<nMax; n++) {
                parentStylesheetMap.remove(list.get(n));
            }                
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
                    Boolean.valueOf(bss) : false;
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
                errorsProperty().addAll("Resource \"%s\" not found.", fname);
                if (logger().isLoggable(PlatformLogger.WARNING)) {
                    logger().warning(
                        String.format("Resource \"%s\" not found.", fname)
                    );
                }
            }
            return stylesheet;

        } catch (FileNotFoundException fnfe) {
            errorsProperty().addAll("Could not find stylesheet: " + fname);
            if (logger().isLoggable(PlatformLogger.INFO)) {
                logger().info("Could not find stylesheet: " + fname);//, fnfe);
            }
        } catch (IOException ioe) {
            errorsProperty().addAll("Could not load stylesheet: " + fname);
            if (logger().isLoggable(PlatformLogger.INFO)) {
                logger().info("Could not load stylesheet: " + fname);//, ioe);
            }
        }
        return null;
    }

    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet. The node argument must be an instance of Control.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public void addUserAgentStylesheet(String fname) {
                
        // nothing to add
        if (fname == null ||  fname.trim().isEmpty()) return;

        Stylesheet ua_stylesheet = loadStylesheet(fname);

        addUserAgentStylesheet(fname, ua_stylesheet);

    }

    /**
     * Set the default user agent stylesheet.
     *
     * @param fname The file URL, either relative or absolute, as a String.
     */
    public void setDefaultUserAgentStylesheet(String fname) {

        if (fname == null || fname.trim().isEmpty())
            throw new IllegalArgumentException("null arg fname");

        Stylesheet ua_stylesheet = loadStylesheet(fname);
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
            setDefaultUserAgentStylesheet(ua_stylesheet);
        } 

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
        if (defaultContainer != null) defaultContainer.destroy();
        defaultContainer = null;
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
                scene.getRoot().impl_processCSS(true);
            }            
        }        
    }

    public void clearCachedValues(Scene scene) {

        // Psuedoclass state for the nodes is only valid for a pulse. 
        StyleHelper.pseudoclassMasksByNode.clear();
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

    List<String> getPseudoclassStrings(long mask) {
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
    public Reference<StyleHelper> getStyleHelper(Node node) {

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

    /** Called from Node to apply styles to the node */
    public List<CascadingStyle> getStyles(Scene scene) {
        // Either the userAgentStylesheet or stylesheets defined on the Scene
        // might apply. If there is an entry in the containerMap we use that,
        // otherwise we use the defaultContainer.
        StylesheetContainer container = get(containerMap,scene);
        if (container == null) {
            if (defaultContainer == null) defaultContainer = new StylesheetContainer(null);
            return defaultContainer.getStyles(scene);
        }
        // There exists a more specific container, so use that
        return container.getStyles(scene);
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

        private final MapChangeListener<String,Stylesheet> mapChangeListener;

        // The StyleHelper's cached values are relevant only for a given scene.
        // Since a StylesheetContainer is created for a Scene with stylesheets,
        // it makes sense that the container should own the valueCache. This
        // way, each scene gets its own valueCache.
        private final Map<Reference<StyleHelper.StyleCacheKey>, List<StyleHelper.CacheEntry>> valueCache;
        // ditto
        private int helperCount;

        private StylesheetContainer(final Collection<Stylesheet> authorStylesheets) {

            cacheMap = new HashMap<Key, Cache>();
            key = new Key();

            stylesheets = new ArrayList<Stylesheet>();

            if (StyleManager.getInstance().defaultUserAgentStylesheet != null) {
                stylesheets.add(StyleManager.getInstance().defaultUserAgentStylesheet);
            }            
            stylesheets.addAll(StyleManager.getInstance().userAgentStylesheetMap.values());
            if (authorStylesheets != null) stylesheets.addAll(authorStylesheets);
            
            mapChangeListener = new MapChangeListener<String,Stylesheet>() {

                @Override
                public void onChanged(Change<? extends String,? extends Stylesheet> change) {
                    if (change.wasAdded()) {
                        stylesheets.add(change.getValueAdded());
                        clearCaches();
                    } else if (change.wasRemoved()) {
                        stylesheets.remove(change.getValueRemoved());
                        clearCaches();
                    }
                }
            };

            StyleManager.getInstance().userAgentStylesheetMap.addListener(mapChangeListener);

            valueCache = new HashMap<Reference<StyleHelper.StyleCacheKey>, List<StyleHelper.CacheEntry>>();
            helperCount = 0;
        }

        private void destroy() {

            StyleManager.getInstance().userAgentStylesheetMap.removeListener(mapChangeListener);
            stylesheets.clear();
            clearCaches();
        }

        private void clearCaches() {

            for (Entry<Reference<StyleHelper.StyleCacheKey>, List<StyleHelper.CacheEntry>> entry : valueCache.entrySet()) {
                for (StyleHelper.CacheEntry cacheEntry : entry.getValue()) {
                    cacheEntry.values.clear();
                }
                entry.getKey().clear();
                entry.getValue().clear();
            }
            valueCache.clear();

            for(Cache cache : cacheMap.values()) {
                cache.clear();
            }
            cacheMap.clear();

            helperCount = 0;
        }

        //
        // If this Node is a Parent and has stylesheets, or if any Parent 
        // above this has stylesheets, then a cached StyleHelper cannot be used.
        //
        private boolean useCacheMap(Node node) {
            boolean useCacheMap = true;
            Parent parent = (node instanceof Parent) ? (Parent)node : node.getParent();
            while (parent != null && useCacheMap) {
                useCacheMap = parent.getStylesheets().isEmpty() ;
                parent = parent.getParent();
            }
            return useCacheMap;
        }
        
        //
        // recurse so that stylesheets of Parents closest to the root are 
        // added to the list first. The ensures that declarations for 
        // stylesheets further down the tree (closer to the leaf) have
        // a higer ordinal in the cascade.
        //
        private List<Stylesheet> gatherParentStylesheets(Parent parent) {
            
            if (parent == null) return null; 
            
            final List<String> parentStylesheets = parent.impl_getAllParentStylesheets();
            
            if (parentStylesheets == null || parentStylesheets.isEmpty()) return null;
            
            final List<Stylesheet> list = new ArrayList<Stylesheet>();
            
            for (int n=0, nMax=parentStylesheets.size(); n<nMax; n++) {
                final String fname = parentStylesheets.get(n);
                Stylesheet stylesheet = null;
                if (parentStylesheetMap.containsKey(fname)) {
                    stylesheet = parentStylesheetMap.get(fname);
                } else {
                    stylesheet = StyleManager.getInstance().loadStylesheet(fname);
                    // stylesheet may be null which would mean that some IOException
                    // was thrown while trying to load it. Add it to the 
                    // parentStylesheetMap anyway as this will prevent further
                    // attempts to parse the file. 
                    parentStylesheetMap.put(fname, stylesheet);
                }
                if (stylesheet != null) list.add(stylesheet);
            }
            
            return list;
        }
        /**
         * Returns a StyleHelper for the given Node, or null if there are no
         * styles for this node.
         */
        private Reference<StyleHelper> getStyleHelper(Node node) {
            
            // If this node has no Parent stylesheets, then shared cache can be used.
            final List<Stylesheet> parentStylesheets = 
                gatherParentStylesheets(((node instanceof Parent) ? (Parent)node : node.getParent()));
            final boolean useCacheMap = parentStylesheets == null || parentStylesheets.isEmpty();
                    
            // Populate our helper key with the class name, id, and style class
            // of the node and lookup the associated Cache in the cacheMap
            final String className = node.getClass().getName();
            final String id = node.getId();
            final List<String> styleClass = node.getStyleClass();
            key.className = className;
            key.id = id;
            key.styleClass = styleClass;
            // bypass cacheMap if the node cannot use shared cache
            Cache cache = useCacheMap ? cacheMap.get(key) : null;

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

                //
                List<Stylesheet> stylesheetsToProcess = null;
                
                if (useCacheMap) {
                    stylesheetsToProcess = stylesheets;
                } else {
                    
                    //
                    // if bypassing shared cache, then the node is a Parent
                    // with stylesheets or has a Parent with stylesheets. 
                    // Gather up all of the Parent stylesheets for procesing.
                    //
                    
                    // scene stylesheets come first since declarations from
                    // Parent stylesheets should take precedence.
                    stylesheetsToProcess = parentStylesheets;
                    stylesheetsToProcess.addAll(0,stylesheets);
                }                    

                for (int i = 0, imax = stylesheetsToProcess.size(); i < imax; i++) {
                    final Stylesheet ss = stylesheetsToProcess.get(i);
                    final List<Rule> stylesheetRules = ss.getRules();
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
                        for (int s = 0, smax = rule.selectors.size(); s < smax; s++) {
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
                cache = new Cache(rules, pseudoclassStateMask, impactsChildren);
                if (useCacheMap) {
                    final Key newKey = new Key();
                    newKey.className = className;
                    newKey.id = id;
                    // Copy the list.
                    // If the contents of the Node's styleClass changes,
                    // the cacheMap lookup should miss.
                    final int nElements = styleClass.size();
                    newKey.styleClass = new ArrayList<String>(nElements);
                    for(int n=0; n<nElements; n++) newKey.styleClass.add(styleClass.get(n));
                    cacheMap.put(newKey, cache);
                }
            }
            // Return the style helper looked up by the cache. The cache will
            // create a style helper if necessary (and possible), so we don't
            // have to worry about that part. 
            return cache.lookup(node, this);
        }

        /**
         * Get a style helper for the scene
         */
        StyleHelper getStyleHelper(Scene scene) {
            StyleHelper helper = StyleHelper.create(getStyles(scene), 0, ++helperCount);
            helper.valueCache = valueCache;
            return helper;
        }

        /*
         * Find the matching styles for this node and return them in order such
         * that the most specific style is first.
         */
        List<CascadingStyle> getStyles(Scene scene) {
            // stylesheets belong to the scene

            // FIXME return default empty sequence here, and create actual sequence
            // after this test
            if (stylesheets.isEmpty()) { return Collections.EMPTY_LIST; }

            // Order in which the declaration of a Style appears (see below)
            int ordinal = 0;

            // Rip through all the stylesheets and find the matching rules.
            // For each declaration in the matching rules, create a Style object
            final List<CascadingStyle> styles = new ArrayList<CascadingStyle>();
            for (int i = 0; i < stylesheets.size(); i++) {
                final Stylesheet stylesheet = stylesheets.get(i);
                for (int j = 0; j < stylesheet.getRules().size(); j++) {
                    final Rule rule = stylesheet.getRules().get(j);
                    final List<Match> matches = rule.matches(scene);
                    for (int k = 0; k < matches.size(); k++) {
                        Match match = matches.get(k);
                        for (int m = 0; m < rule.declarations.size(); m++) {
                            final Declaration decl = rule.declarations.get(m);

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
            }

            // return sorted styles
            Collections.sort(styles);
            return styles;
        }
    }

    private ObservableList<String> errors = null;
    public ObservableList<String> errorsProperty() {
        if (errors == null) {
            errors = FXCollections.observableArrayList();
        }
        return errors;
    }
    public ObservableList<String> getErrors() {
        return errors;
    }

    private static class StyleHelperCacheContainer {
        private final StyleHelper styleHelper;
        private final Reference<StyleHelper> styleHelperRef;
        private StyleHelperCacheContainer(StyleHelper styleHelper, 
                Reference<StyleHelper> styleHelperRef) {
            this.styleHelper = styleHelper;
            this.styleHelperRef = styleHelperRef;
        }        
    }
    
    /**
     * Creates and caches StyleHelpers, reusing them as often as practical.
     */
    private static class Cache {
        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final long pseudoclassStateMask;
        private final boolean impactsChildren;
        private final Map<Long, StyleHelperCacheContainer> cache;

        Cache(List<Rule> rules, long pseudoclassStateMask, boolean impactsChildren) {
            this.rules = rules;
            this.pseudoclassStateMask = pseudoclassStateMask;
            this.impactsChildren = impactsChildren;
            cache = new HashMap<Long, StyleHelperCacheContainer>();
        }

        private void clear() {

            for(StyleHelperCacheContainer helperContainer : cache.values()) {
                
                final StyleHelper helper = (helperContainer != null)
                        ? helperContainer.styleHelper
                        : null;
                
                if (helper == null) {
                    continue;
                }
                helper.valueCache = null;
                helper.clearStyleMap();
                helperContainer.styleHelperRef.clear();
                
            }

            cache.clear();
            rules.clear();
        }

        private Reference<StyleHelper> lookup(Node node, StylesheetContainer container) {

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

            if (cache.containsKey(key)) {
                final StyleHelperCacheContainer helperContainer = cache.get(key);
                if (helperContainer != null) {
                    return helperContainer.styleHelperRef;
                }
                cache.remove(key);
            } 
            
            // We need to create a new StyleHelper, add it to the cache,
            // and then return it.
            final List<CascadingStyle> styles = getStyles(node);
            final StyleHelper helper =
                StyleHelper.create(styles, pseudoclassStateMask,
                    ++(container.helperCount));

            helper.valueCache = container.valueCache;
            
            final Reference<StyleHelper> helperRef =
                new WeakReference<StyleHelper>(helper);            
            final StyleHelperCacheContainer helperContainer = 
                new StyleHelperCacheContainer(helper, helperRef);
            cache.put(key, helperContainer);

            return helperRef;
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

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                return className.equals(other.className)
                    && (   (id == null && other.id == null)
                        || (id != null && id.equals(other.id))
                       )
                    && (   (styleClass == null && other.styleClass == null)
                        || (styleClass != null && styleClass.containsAll(other.styleClass))
                       );
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = className.hashCode();
            hash = 31 * (hash + ((id == null || id.isEmpty()) ? 1231 : id.hashCode()));
            hash = 31 * (hash + ((styleClass == null || styleClass.isEmpty()) ? 1237 : styleClass.hashCode()));
            return hash;
        }

        @Override
        public String toString() {
            return "Key ["+className+", "+String.valueOf(id)+", "+String.valueOf(styleClass)+"]";
        }
    }

}
