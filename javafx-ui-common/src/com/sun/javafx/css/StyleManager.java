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
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.*;
import java.util.Map.Entry;
import javafx.collections.*;
import javafx.scene.Parent;
import javafx.stage.Window;

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

abstract public class StyleManager<T> {

    private static PlatformLogger LOGGER;
    protected static PlatformLogger getLogger() {
        if (LOGGER == null) {
            LOGGER = com.sun.javafx.Logging.getCSSLogger();
        }
        return LOGGER;
    }

    /**
     * Return the StyleManager for the given Scene, or null if the scene
     * is no longer referenced.
     */
    public static <T> StyleManager getStyleManager(T key) {
        final Reference<StyleManager> styleManagerRef = 
                key != null ? managerReferenceMap.get(key) : null;
        return styleManagerRef != null ? styleManagerRef.get() : null;
    }
    
    public static StyleManager createStyleManager(Scene scene) {
        return new SceneStyleManager(scene);
    }

    public static StyleManager createStyleManager(Parent parent, StyleManager ancestorStyleManager) {
        return new ParentStyleManager(parent, ancestorStyleManager);
    }
    
    /**
     *
     * @param owner
     */
    protected StyleManager(T owner) {

        if (owner == null) throw new IllegalArgumentException("scene cannot be null");

        this.owner = owner;

        cacheMap = new HashMap<SimpleSelector, Cache>();

        referencedStylesheets = new ArrayList<Stylesheet>();
        if (defaultUserAgentStylesheet != null) {
            referencedStylesheets.add(defaultUserAgentStylesheet);
        }

        styleCache = new HashMap<StyleCacheKey, StyleCacheBucket>();
        
        smapCount = 0;
        
        // A Reference is needed here since Scene is both in the key and value
        managerReferenceMap.put(owner, new WeakReference<StyleManager>(this));        
        
    }
        
    /*
     * The scene or parent that created this StyleManager. 
     */
    protected final T owner;
    T getOwner() { return owner; } // for testing
    
    /*
     * Stylesheets that are referenced by this Scene. The parsed stylesheets
     * are held in the StylesheetContainer and could be looked up from there,
     * but that lookup would be frequent so we hang onto the parsed stylesheets
     */
    protected final List<Stylesheet> referencedStylesheets;
    // for testing
    List<Stylesheet> getReferencedStylesheets() { return referencedStylesheets; }
    
       /*
     * Scene does not have a inline style, but an applet might. This is
     * the applet's stylesheet from a 'style' attribute. 
     */
    protected Stylesheet inlineStylesheet;
    // for testing
    Stylesheet getInlineStylesheet() { return inlineStylesheet; }
    
    /**
     * The map of Caches, key'd by a combination of class name, style class, and
     * id.
     */
    protected final Map<SimpleSelector, Cache> cacheMap;
    // for testing
    Map<SimpleSelector, Cache> getCacheMap() { return cacheMap; }

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
    protected static final Map<Object, Reference<StyleManager>> managerReferenceMap =
            new WeakHashMap<Object, Reference<StyleManager>>();
        
   /**
     * This stylesheet represents the "default" set of styles for the entire
     * platform. This is typically only set by the UI Controls module, and
     * otherwise is generally null. Whenever this variable changes (via the
     * setDefaultUserAgentStylesheet function call) then we will end up clearing
     * all of the caches.
     */
    static Stylesheet defaultUserAgentStylesheet;


    /**
     * User agent stylesheets from Control.getUserAgentStylesheet.
     */
    protected static final Map<String,Stylesheet> userAgentStylesheetMap =
            new HashMap<String,Stylesheet>();

    
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
    protected static class StylesheetContainer<T> {
        // the stylesheet url
        final String fname;
        // the parsed stylesheet so we don't reparse for every parent that uses it
        final Stylesheet stylesheet;
        // the parents or scenes that use this stylesheet. Typically, this list
        //  should be very small.
        final RefList<T> users;
        // the keys for finding Cache entries that use this stylesheet.
        // This list should also be fairly small
        final RefList<SimpleSelector> keys;

        StylesheetContainer(String fname, Stylesheet stylesheet) {
            this.fname = fname;
            this.stylesheet = stylesheet;
            this.users = new RefList<T>();
            this.keys = new RefList<SimpleSelector>();
        }
    }

    /*
     * A list that holds references. Used by StylesheetContainer. 
     * We only ever add to this list, or the whole container gets thrown
     * away. 
     */
    protected static class RefList<K> {

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
                    if (k == key) return;
                }
            }
            // not found, add it.
            list.add(new WeakReference<K>(key));
        }

    }

    public void updateStylesheets() {
     
        selectorPartitioning.reset();

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

        updateStylesheetsImpl();
        
        for(int n=0, nMax=referencedStylesheets.size(); n<nMax; n++) {
            
            final Stylesheet stylesheet = referencedStylesheets.get(n);
            final List<Rule> rules = stylesheet.getRules();
            for (int r=0, rMax=rules.size(); r<rMax; r++) {
                final Rule rule = rules.get(r);
                final List<Selector> selectors = rule.getSelectors();
                for (int s=0, sMax=selectors.size(); s<sMax; s++) {
                    Selector selector = selectors.get(s);
                    selectorPartitioning.partition(selector, rule);
                }
            }
        }
        
    }
    
    protected abstract void updateStylesheetsImpl();
    
    public void stylesheetsChanged(ListChangeListener.Change<String> c) {
        // for now, just call updateStylesheets
        updateStylesheets();
    }    

    /**
     * Set a stylesheet for the scene from the given stylesheet string. This
     * stylesheet comes after user agent stylesheets but before author
     * stylesheets and applies only to this scene.
     */
    public static void setInlineStylesheet(Scene scene, Stylesheet stylesheet) {
        
        if (scene == null) return;
        
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

    protected static Stylesheet loadStylesheet(final String fname) {
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
                if (getLogger().isLoggable(PlatformLogger.WARNING)) {
                    getLogger().warning(
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
        if (fname == null ||  fname.trim().isEmpty()) return;

        if (userAgentStylesheetMap.containsKey(fname) == false) {
            
            // RT-20643
            CssError.setCurrentScene(scene);

            Stylesheet ua_stylesheet = loadStylesheet(fname);

            if (ua_stylesheet != null) {
                ua_stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
                userAgentStylesheetMap.put(fname, ua_stylesheet);
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
    public static void setDefaultUserAgentStylesheet(Stylesheet stylesheet) {
        defaultUserAgentStylesheet = stylesheet;
        if (defaultUserAgentStylesheet != null) {
            defaultUserAgentStylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        }
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
            if (scene == null) continue;
            
            final Reference<StyleManager> styleManagerRef = managerReferenceMap.get(scene);
            final StyleManager styleManager = styleManagerRef.get();
            if (styleManager == null) continue;
            
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
        // even though the list returned could be modified without causing 
        // harm, returning an unmodifiableList is consistent with 
        // SimpleSelector.getStyleClasses()
        return Collections.unmodifiableList(strings);
    }

    protected final void clearCache() {
        styleCache.clear();
        cacheMap.clear();
        smapCount = 0;
    }

    private long nextSmapId() {
        return ++smapCount;
    }

    /**
     * Finds matching styles for this Node.
     */
    StyleMap findMatchingStyles(Node node, SimpleSelector key) {
        
        //
        // Are there any stylesheets at all?
        // If not, then there is nothing to match and the
        // resulting StyleMap is going to end up empty
        // 
        if (referencedStylesheets.isEmpty()) {
            return StyleMap.EMPTY_MAP;
        }
        
        Cache cache = cacheMap.get(key);

        // If the cache is null, then we need to create a new Cache and
        // add it to the cache map
        if (cache == null) {
            
            final String id = key.getId();
            final String name = key.getName();
            final long[] styleClassBits = key.getStyleClassMasks();
            
            // Construct the list of Rules that could possibly apply
            final List<Rule> rules =
                selectorPartitioning.match(
                        key.getId(), 
                        key.getName(), 
                        key.getStyleClassMasks()
                    );
            
            // create a new Cache from these rules.
            cache = new Cache(rules);
            
            final SimpleSelector newKey = new SimpleSelector(key);                    
            cacheMap.put(newKey, cache);

        }

        //
        // Create a style helper for this node from the styles that match. 
        //
        StyleMap smap = cache.getStyleMap(this, node);
        
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

    private final SelectorPartitioning selectorPartitioning = new SelectorPartitioning();
    
    
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
        
        private static final StyleMap EMPTY_MAP = 
            new StyleMap(0, Collections.EMPTY_MAP);

    }

    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    protected static class Cache {

        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final Map<Long, StyleMap> cache;

        Cache(List<Rule> rules) {
            this.rules = rules;
            this.cache = new HashMap<Long, StyleMap>();
        }

        private StyleMap getStyleMap(StyleManager owner, Node node) {
            
            if (rules == null || rules.isEmpty()) {                
                return StyleMap.EMPTY_MAP;
            }


            //
            // Since the list of rules is found by matching only the
            // rightmost selector, the set of rules may larger than those 
            // rules that actually match the node. The following loop
            // whittles the list down to those rules that actually match.
            //
            // listOfMatches is dual purpose. First, it keeps track
            // of what rules match the node. Second, it keeps track
            // of the indices of the matching rules. These indices
            // are used to create the bit mask for the cache key. If
            // the cache misses, then a new entry is created by looping
            // through listOfMatches and creating CascadingStyles for
            // entries that are not null. Ok, so that's three.
            //
            final List<Match>[] listOfMatches = new List[rules.size()];
            
            //
            // do we have a cache for the set of matching rules?
            //
            // To lookup from the cache, we construct a key from a Long
            // where the rules that match this particular node are
            // represented by bits on the Long.
            //
            long key = 0;
            long mask = 1;
            for (int r = 0, rMax = rules.size(); r < rMax; r++) {
                
                final Rule rule = rules.get(r);
                final List<Match> matches = rule.matches(node);
                if (matches  != null && matches.isEmpty() == false) {
                    
                    listOfMatches[r] = matches;
                    key = key | mask;
                    
                } else {
                    
                    listOfMatches[r] = null;
                    
                }
                mask = mask << 1;
            }
            
            // nothing matched!
            if (key == 0) return null;
            
            final Long keyObj = Long.valueOf(key);
            if (cache.containsKey(keyObj)) {
                final StyleMap styleMap = cache.get(keyObj);
                return styleMap;
            }
                        
            final List<CascadingStyle> styles = new ArrayList<CascadingStyle>();
            int ordinal = 0;
            
            for (int m = 0, mMax = listOfMatches.length; m<mMax; m++) {
                
                final List<Match> matches = listOfMatches[m];
                if (matches == null) continue; 
                
                // the rule that matched
                Rule rule = rules.get(m);
                
                for (int n=0, nMax = matches.size(); n<nMax; n++) {
                    
                    final Match match = matches.get(n);
                    // TODO: should never get nulls in this list. Fix Rule#matches
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
    

}
