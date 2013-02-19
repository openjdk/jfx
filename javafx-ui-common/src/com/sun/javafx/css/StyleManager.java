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

import javafx.css.Styleable;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Window;
import com.sun.javafx.css.parser.CSSParser;
import java.util.Map.Entry;
import java.util.Set;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
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

    private static class InstanceHolder {
        final static StyleManager INSTANCE = new StyleManager();
    }
    /**
     * Return the StyleManager instance.
     */
    public static StyleManager getInstance() {   
        return InstanceHolder.INSTANCE;
    }
        
    /**
     * @return  The Styles that match this CSS property for the given Node. The 
     * list is sorted by descending specificity. 
     */
    public static List<Style> getMatchingStyles(CssMetaData cssMetaData, Node node) {
        if (node != null && cssMetaData != null) {
            return getMatchingStyles(cssMetaData, node);
        }
        return Collections.<Style>emptyList();
    }

    /**
     * @return  The Styles that match this CSS property for the given Styleable. The 
     * list is sorted by descending specificity. 
     */
    public static List<Style> getMatchingStyles(CssMetaData cssMetaData, Styleable styleable) {
        if (styleable != null && cssMetaData != null) {
            StyleHelper helper = styleable instanceof Node 
                    ? ((Node)styleable).impl_getStyleHelper()
                    : null;
            return (helper != null) 
                ? helper.getMatchingStyles(styleable, cssMetaData) 
                : Collections.<Style>emptyList(); 
        }
        return Collections.<Style>emptyList();
    }    
 
    /**
     * 
     * @param node
     * @return 
     */
    // TODO: is this used anywhere?
    public static List<CssMetaData<? extends Styleable, ?>> getStyleables(final Styleable styleable) {
        
        return styleable != null 
            ? styleable.getCssMetaData() 
            : Collections.<CssMetaData<? extends Styleable, ?>>emptyList();
    }

    /**
     * 
     * @param node
     * @return 
     */
    // TODO: is this used anywhere?
    public static List<CssMetaData<? extends Styleable, ?>> getStyleables(final Node node) {
        
        return node != null 
            ? node.getCssMetaData() 
            : Collections.<CssMetaData<? extends Styleable, ?>>emptyList();
    }
         
    private StyleManager() {                
    }
            
    /**
     * The map of Caches, key'd by a combination of class name, style class, and
     * id.
     */
    private static final Map<List<String>, Map<Key, Cache>> 
            masterCacheMap = new HashMap<List<String>, Map<Key, Cache>>();
    
    // for testing
    Map<List<String>, Map<Key, Cache>> getCacheMap() { return masterCacheMap; }

    /*
     */
    private final Map<StyleCache.Key, StyleCache> styleCache =
            new HashMap<StyleCache.Key, StyleCache>();
    
    /** StyleHelper uses this cache. */
    StyleCache getStyleCache(StyleCache.Key key) { 

        StyleCache cache = styleCache.get(key);
        if (cache == null) {
            cache = new StyleCache();
            styleCache.put(new StyleCache.Key(key), cache);
        }
        return cache;
    }
    
   /**
     * This stylesheet represents the "default" set of styles for the entire
     * platform. This is typically only set by the UI Controls module, and
     * otherwise is generally null. Whenever this variable changes (via the
     * setDefaultUserAgentStylesheet function call) then we will end up clearing
     * all of the caches.
     */
    private final List<StylesheetContainer> userAgentStylesheets = 
            new ArrayList<StylesheetContainer>();
   
    ////////////////////////////////////////////////////////////////////////////
    //
    // stylesheet handling
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
     * StylesheetContainer<Parent> are created and added to stylesheetContainerMap
     * in the method gatherParentStylesheets.
     * 
     * StylesheetContainer<Scene> are created and added to sceneStylesheetMap in
     * the method updateStylesheets
     */
    private static class StylesheetContainer {
        // the stylesheet url
        final String fname;
        // the parsed stylesheet so we don't reparse for every parent that uses it
        final Stylesheet stylesheet;
        // the parents or scenes that use this stylesheet. Typically, this list
        //  should be very small.
        final SelectorPartitioning selectorPartitioning;
        // who uses this stylesheet?        
        final RefList<Scene> sceneUsers;
        final RefList<Parent> parentUsers;
        // the keys for finding Cache entries that use this stylesheet.
        // This list should also be fairly small
        final RefList<Key> keys;
        
        final int hash;

        StylesheetContainer(String fname, Stylesheet stylesheet) {
            
            this.fname = fname;
            hash = (fname != null) ? fname.hashCode() : 127;
            
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
            
            this.sceneUsers = new RefList<Scene>();
            this.parentUsers = new RefList<Parent>();
            this.keys = new RefList<Key>();
        }

        @Override
        public int hashCode() {
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
            final StylesheetContainer other = (StylesheetContainer) obj;
            if ((this.fname == null) ? (other.fname != null) : !this.fname.equals(other.fname)) {
                return false;
            }
            return true;
        }
        
        @Override public String toString() {
            return fname;
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
    private final Map<String,StylesheetContainer> stylesheetContainerMap 
        = new HashMap<String,StylesheetContainer>();
    
    /**
     * called from Parent's or Scene's stylesheets property's onChanged method
     */
    public void stylesheetsChanged(Scene scene, Change<String> c) {

        // annihilate the cache
        clearCache();
        processChange(c);
        
    }
        
    /**
     * called from Parent's or Scene's stylesheets property's onChanged method
     */
    public void stylesheetsChanged(Parent parent, Change<String> c) {
        processChange(c);
    }
    
    /**
     * called from Parent's or Scene's stylesheets property's onChanged method
     */
    private void processChange(Change<String> c) {
        
        // make sure we start from the beginning should this Change
        // have been used before entering this method.
        c.reset(); 
        
        while (c.next()) {

            // RT-22565
            // If the String was removed, remove it from stylesheetContainerMap
            // and remove all Caches that got styles from the stylesheet.
            // The stylesheet may still be referenced by some other parent or
            // scene, in which case the stylesheet will be reparsed and added
            // back to stylesheetContainerMap when the StyleHelper is updated
            // with new styles.
            //
            // This incurs a some overhead since the stylesheet has to be
            // reparsed, but this keeps the other scenes and parents that use
            // the stylesheet in sync. For example, SceneBuilder will remove
            // and then add a stylesheet after it has been edited and all
            // parents that use that stylesheet should get the new values.
            //
            if (c.wasRemoved()) {

                final List<String> list = c.getRemoved();
                int nMax = list != null ? list.size() : 0;
                for (int n=0; n<nMax; n++) {
                    final String fname = list.get(n);

                    // remove this stylesheet from the container and clear
                    // all caches used by it
                    StylesheetContainer sc = stylesheetContainerMap.remove(fname);

                    if (sc != null) {
                        clearCache(sc);
                        if (sc.selectorPartitioning != null) {
                            sc.selectorPartitioning.reset();
                        }
                    }
                }
            }

            // RT-22565: only wasRemoved matters. If the logic was applied to
            // wasAdded, then the stylesheet would be reparsed each time a
            // a Parent added it.
    
        }
    }

    // RT-22565: Called from parentStylesheetsChanged to clear the cache entries
    // for parents and scenes that use the same stylesheet
    private void clearCache(StylesheetContainer sc) {

        removeFromCacheMap(sc);
            
        final List<Reference<Scene>> sceneList = sc.sceneUsers.list;
        final List<Reference<Parent>> parentList = sc.parentUsers.list;
                        
        for (int n=sceneList.size()-1; 0<=n; --n) {

            final Reference<Scene> ref = sceneList.remove(n);
            final Scene scene = ref.get();
            ref.clear();
            if (scene == null) {
                continue;
            }
            
            scene.getRoot().impl_reapplyCSS();
        }
        
        for (int n=parentList.size()-1; 0<=n; --n) {

            final Reference<Parent> ref = parentList.remove(n);
            final Parent parent = ref.get();
            ref.clear();
            if (parent == null || parent.getScene() == null) {
                continue;
            }
        
            //
            // tell parent it needs to reapply css
            // No harm is done if parent is in a scene that has had 
            // impl_reapplyCSS called on the root.
            // 
            parent.impl_reapplyCSS();
        }
    }

    // RT-22565: Called from clearParentCache to clear the cache entries.
    private void removeFromCacheMap(StylesheetContainer sc) {

        if (masterCacheMap.isEmpty()) {
            return;
        }

        final Set<Entry<List<String>, Map<Key, Cache>>> entries = 
                masterCacheMap.entrySet();

        // If this StylesheetContainer is used by other entries, then those
        // caches need to be invalidated, too. But we need to go through all
        // of the entries first and gather up the ones that need invalidated
        // to avoid concurrent modification due to this method being re-entered.         
        final List<String> containerKeys = new ArrayList<String>();
        
        final Iterator<Entry<List<String>, Map<Key, Cache>>> iter =
                entries.iterator();
            
        while (iter.hasNext()) {
            
            Entry<List<String>, Map<Key, Cache>> entry = iter.next();
            List<String> containerList = entry.getKey();
            if (containerList == null) continue;
            
            if (containerList.contains(sc.fname)) {
                containerList.remove(sc.fname);
                
                if (containerList.isEmpty()) {
                    entry.getValue().clear();
                } else {
                    for (String container : containerList) {
                        if (containerKeys.contains(container) == false) {
                            containerKeys.add(container);                            
                        }
                    }
                }
            }
        }
        
        // invalidate other caches that used this Stylesheet.
        for (String key : containerKeys) {
            StylesheetContainer container = stylesheetContainerMap.get(key);
            clearCache(container);
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
    
    private int getIndex(String fname) {
        
        for(int n=0,nMax = userAgentStylesheets.size(); n<nMax; n++) {
            StylesheetContainer sc = userAgentStylesheets.get(n);
            String scFname = sc.fname;
            if (scFname == null ? fname == null : scFname.equals(fname)) {
                return n;
            }
        }
        return -1;
    }
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
        if (fname == null ||  fname.trim().isEmpty()) {
            return;
        }

        int index = getIndex(fname);
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
        
        Stylesheet ua_stylesheet = loadStylesheet(fname);
        userAgentStylesheets.add(new StylesheetContainer(fname, ua_stylesheet));

        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            userAgentStylesheetsChanged();
        }

        // RT-20643
        CssError.setCurrentScene(null);

    }
    
    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param fname  The file URL, either relative or absolute, as a String.
     */
    public void addUserAgentStylesheet(Scene scene, Stylesheet ua_stylesheet) {

        // RT-20643
        CssError.setCurrentScene(scene);

        if (userAgentStylesheets.isEmpty()) {
            // default UA stylesheet is always index 0
            // but a default hasn't been set, so leave room for it.
            userAgentStylesheets.add(null);
        }
        
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
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

        if (fname == null || fname.trim().isEmpty()) {
            throw new IllegalArgumentException("null arg fname");
        }

        // if this stylesheet has been added already, move it to first element
        int index = getIndex(fname);
        if (index != -1) {
            
            if (index > 0) {
                StylesheetContainer sc = userAgentStylesheets.get(index);
                userAgentStylesheets.remove(index);
                userAgentStylesheets.set(0, sc);
            }
            return;
        }
        
        // RT-20643
        CssError.setCurrentScene(scene);

        Stylesheet ua_stylesheet = loadStylesheet(fname);
        StylesheetContainer sc = new StylesheetContainer(fname, ua_stylesheet);
        
        if (userAgentStylesheets.isEmpty()) {
            userAgentStylesheets.add(sc);
        } else {
            userAgentStylesheets.set(0,sc);
        }
        
        if (ua_stylesheet != null) {
            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            userAgentStylesheetsChanged();
        }

        // RT-20643
        CssError.setCurrentScene(null);

    }

    /**
     * Set the user agent stylesheet. This is the base default stylesheet for
     * the platform
     */
    public void setDefaultUserAgentStylesheet(Stylesheet stylesheet) {
        
        if (stylesheet == null ) {
            throw new IllegalArgumentException("null arg ua_stylesheet");
        }
        
        final URL url = stylesheet.getUrl();
        final String fname = url != null ? url.toExternalForm() : "";
        
        // if this stylesheet has been added already, move it to first element
        int index = getIndex(fname);
        if (index != -1) {
            
            if (index > 0) {
                StylesheetContainer sc = userAgentStylesheets.get(index);
                userAgentStylesheets.remove(index);
                userAgentStylesheets.set(0, sc);
            }
            return;
        }
        
        StylesheetContainer sc = new StylesheetContainer(fname, stylesheet);
        
        if (userAgentStylesheets.isEmpty()) {
            userAgentStylesheets.add(sc);
        } else {
            userAgentStylesheets.set(0,sc);
        }
        
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        userAgentStylesheetsChanged();
    }

    /*
     * If the userAgentStylesheets change, then all scenes are updated.
     */
    private void userAgentStylesheetsChanged() {

        clearCache();
        
        final Iterator<Window> windows = Window.impl_getWindows();
        while (windows.hasNext()) {
            final Window window = windows.next();
            final Scene scene = window.getScene();
            if (scene == null) {
                continue;
            }
            scene.getRoot().impl_reapplyCSS();
        }

    }
    
    private void clearCache() {
        
        masterCacheMap.clear();
        styleCache.clear();
        
        styleMapList.clear();
        baseStyleMapId = styleMapId;
        // totally arbitrary
        if (baseStyleMapId > Integer.MAX_VALUE*7/8) {
            baseStyleMapId = styleMapId = 0;
        }
    }


    //
    // recurse so that stylesheets of Parents closest to the root are
    // added to the list first. The ensures that declarations for
    // stylesheets further down the tree (closer to the leaf) have
    // a higer ordinal in the cascade.
    //
    private List<StylesheetContainer> gatherParentStylesheets(Parent parent) {

        if (parent == null) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<String> parentStylesheets = parent.impl_getAllParentStylesheets();

        if (parentStylesheets == null || parentStylesheets.isEmpty()) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<StylesheetContainer> list = new ArrayList<StylesheetContainer>();

        // RT-20643
        CssError.setCurrentScene(parent.getScene());

        for (int n = 0, nMax = parentStylesheets.size(); n < nMax; n++) {
            final String fname = parentStylesheets.get(n);
            StylesheetContainer container = null;
            if (stylesheetContainerMap.containsKey(fname)) {
                container = stylesheetContainerMap.get(fname);
                // RT-22565: remember that this parent uses this stylesheet.
                // Later, if the cache is cleared, the parent is told to
                // reapply css.
                container.parentUsers.add(parent);
            } else {
                final Stylesheet stylesheet = loadStylesheet(fname);
                // stylesheet may be null which would mean that some IOException
                // was thrown while trying to load it. Add it to the
                // stylesheetContainerMap anyway as this will prevent further
                // attempts to parse the file
                container =
                        new StylesheetContainer(fname, stylesheet);
                // RT-22565: remember that this parent uses this stylesheet.
                // Later, if the cache is cleared, the parent is told to
                // reapply css.
                container.parentUsers.add(parent);
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
    
    //
    // 
    //
    private List<StylesheetContainer> gatherSceneStylesheets(Scene scene) {

        if (scene == null) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<String> sceneStylesheets = scene.getStylesheets();

        if (sceneStylesheets == null || sceneStylesheets.isEmpty()) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<StylesheetContainer> list = 
                new ArrayList<StylesheetContainer>(sceneStylesheets.size());

        // RT-20643
        CssError.setCurrentScene(scene);

        for (int n = 0, nMax = sceneStylesheets.size(); n < nMax; n++) {
            
            final String fname = sceneStylesheets.get(n);
            
            StylesheetContainer container = null;
            if (stylesheetContainerMap.containsKey(fname)) {
                container = stylesheetContainerMap.get(fname);
                container.sceneUsers.add(scene);
            } else {
                final Stylesheet stylesheet = loadStylesheet(fname);
                // stylesheet may be null which would mean that some IOException
                // was thrown while trying to load it. Add it to the
                // stylesheetContainerMap anyway as this will prevent further
                // attempts to parse the file
                container =
                        new StylesheetContainer(fname, stylesheet);
                container.sceneUsers.add(scene);
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
    private final Key key = new Key();
    private final List<String> masterKey = new ArrayList<String>();
    
    /**
     * Finds matching styles for this Node.
     */
    StyleMap findMatchingStyles(Node node, long[][] pseudoclassBits) {
        
        final Scene scene = node.getScene();
        if (scene == null) {
            return StyleMap.EMPTY_MAP;
        }

        final Parent parent = 
            (node instanceof Parent) 
                ? (Parent) node : node.getParent();
        
        final List<StylesheetContainer> parentStylesheets =
                    gatherParentStylesheets(parent);
        
        final boolean hasParentStylesheets = parentStylesheets.isEmpty() == false;
        
        final List<StylesheetContainer> sceneStylesheets =
                    gatherSceneStylesheets(scene);
        
        boolean hasSceneStylesheets = sceneStylesheets.isEmpty() == false;
        
        //
        // Are there any stylesheets at all?
        // If not, then there is nothing to match and the
        // resulting StyleMap is going to end up empty
        // 
        if (hasParentStylesheets == false 
                && hasSceneStylesheets == false
                && userAgentStylesheets.isEmpty()) {
            return StyleMap.EMPTY_MAP;
        }
                
        if (hasSceneStylesheets) {
            for (StylesheetContainer container : sceneStylesheets) {
                masterKey.add(container.fname);
            };
        }
        if (hasParentStylesheets) {
            for (StylesheetContainer container : parentStylesheets) {
                masterKey.add(container.fname);
            };
        }
        
        Map<Key, Cache> cacheMap = masterCacheMap.get(
                masterKey.isEmpty() ? null : masterKey
        );
                
        final String name = node.getClass().getName();
        final int dotPos = name.lastIndexOf('.');
        final String cname = name.substring(dotPos+1);  // want Foo, not bada.bing.Foo
        final String id = node.getId();
        final long[] styleClassBits = node.impl_getStyleHelper().getStyleClassBits();
        
        key.className = cname;
        key.id = id;
        key.styleClass = styleClassBits;

        Cache cache = cacheMap != null ? cacheMap.get(key) : null;
        
        if (cacheMap == null) {
            
            cacheMap = new HashMap<Key, Cache>();
            
            List<String> newMasterKey = null;
            if (masterKey.isEmpty() == false) {
                newMasterKey = new ArrayList<String>(masterKey);
            } else {
                newMasterKey = null;
            }
            
            masterCacheMap.put(newMasterKey, cacheMap);
        }
        masterKey.clear();
        
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
            
            // Construct the list of Rules that could possibly apply
            final List<Rule> rules = new ArrayList<Rule>();

            // User agent stylesheets have lowest precedence and go first
            if (userAgentStylesheets.isEmpty() == false) {
                for(int n=0, nMax=userAgentStylesheets.size(); n<nMax; n++) {
                    final StylesheetContainer container = userAgentStylesheets.get(n);
                    
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
                    final StylesheetContainer container = sceneStylesheets.get(n);
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
                final int nMax = parentStylesheets == null ? 0 : parentStylesheets.size();
                for(int n=0; n<nMax; n++) {
                    final StylesheetContainer container = parentStylesheets.get(n);
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

    /*
     * A simple counter used to generate a unique id for a StyleMap. 
     * This unique id is used by StyleHelper in figuring out which 
     * style cache to use.
     */
    private int styleMapId = 0;
    
    private List<StyleMap> styleMapList = new ArrayList<StyleMap>();
    private int baseStyleMapId = 0;

    private int nextSmapId() {
        return ++styleMapId;
    }
       
    private void addStyleMap(StyleMap smap) {
        assert ((smap.id - baseStyleMapId - 1) == styleMapList.size());
        styleMapList.add(smap);
    }
    
    StyleMap getStyleMap(int smapId) {
        
        if (smapId == StyleMap.EMPTY_MAP.id) return StyleMap.EMPTY_MAP;
        
        final int smapCount = styleMapList.size();
        final int correctedId = smapId - baseStyleMapId - 1;
        
        if (correctedId < 0 || smapCount <= correctedId) return null;
        return styleMapList.get(correctedId);
    }
    
    //
    // Used by StyleHelper. The key uniquely identifies this style map.
    // These keys are used by StyleHelper in creating its StyleCacheKey.
    // The StyleCacheKey is used to lookup calculated values in cache.
    //
    static class StyleMap {
        
        private StyleMap(int id, Map<String, List<CascadingStyle>> map) {
            this.id = id;
            this.map  = map;
        }
        
        static final StyleMap EMPTY_MAP = 
            new StyleMap(0, Collections.<String,List<CascadingStyle>>emptyMap());

        final int id; // unique per container
        final Map<String, List<CascadingStyle>> map;
        
    }

    
    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    private static class Cache {

        private static class Key {
            final long[] key;

            Key(long[] key) {
                this.key = key;
            }
            
            @Override
            public int hashCode() {
                int hash = 3;
                hash = 97 * hash + Arrays.hashCode(this.key);
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
                final Key other = (Key) obj;
                if (!Arrays.equals(this.key, other.key)) {
                    return false;
                }
                return true;
            }
            
        }
        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final Map<Key, Integer> cache;

        Cache(List<Rule> rules) {
            this.rules = rules;
            this.cache = new HashMap<Key, Integer>();
        }

        private StyleMap getStyleMap(StyleManager owner, Node node, long[][] pseudoclassBits) {
            
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
            // where the selectors that match this particular node are
            // represented by bits on the Long.
            //
            long key[] = new long[1];
            int count = 0;
            int index = 0;
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
                
                final int nSelectors = rule.getSelectors().size();
                if ((count + nSelectors) > Long.SIZE) {
                    final long[] temp = new long[key.length+1];
                    System.arraycopy(key, 0, temp, 0, key.length);
                    key = temp;
                    ++index;
                    count = 0;
                }
                
                long mask = rule.applies(node, pseudoclassBits);
                if (mask != 0) {
                    key[index] |= mask << count;
                    applicableRules[r] = rule;
                } else {
                    applicableRules[r] = null;
                }
                
                count += rule.getSelectors().size(); 
                
            }
            
            // nothing matched!
            if (key.length == 1 && key[0] == 0) {
                return StyleMap.EMPTY_MAP;
            }
            
            final Key keyObj = new Key(key);
            if (cache.containsKey(keyObj)) {
                Integer id = cache.get(keyObj);
                final StyleMap styleMap = id != null ? owner.getStyleMap(id.intValue()) : null;
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
                            match.pseudoClasses,
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

            final int id = owner.nextSmapId();
            final StyleMap styleMap = new StyleMap(id, smap);
            owner.addStyleMap(styleMap);
            cache.put(keyObj, Integer.valueOf(id));
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
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.className != null ? this.className.hashCode() : 0);
            hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 29 * hash + Arrays.hashCode(this.styleClass);
            return hash;
        }

        @Override
        public String toString() {
            return "Key ["+className+", "+String.valueOf(id)+", "+String.valueOf(SimpleSelector.getStyleClassStrings(styleClass))+"]";
        }

    }
    

}
