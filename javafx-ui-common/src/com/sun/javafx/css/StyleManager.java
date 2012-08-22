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

        cacheMap = new HashMap<Key, Cache>();

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
    protected final Map<Key, Cache> cacheMap;
    // for testing
    Map<Key, Cache> getCacheMap() { return cacheMap; }

    /*
     * The StyleHelper's cached values are relevant only for a given scene.
     * Since a StylesheetContainer is created for a Scene with stylesheets,
     * it makes sense that the container should own the valueCache. This
     * way, each scene gets its own valueCache.
     */
    private final Map<StyleCacheKey, StyleCacheBucket> styleCache;
    // for testing
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
        final RefList<Key> keys;

        StylesheetContainer(String fname, Stylesheet stylesheet) {
            this.fname = fname;
            this.stylesheet = stylesheet;
            this.users = new RefList<T>();
            this.keys = new RefList<Key>();
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

    public abstract void updateStylesheets();
    
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
        return strings;
    }

    protected final void clearCache() {
        styleCache.clear();
        cacheMap.clear();
        smapCount = 0;
    }

    private long nextSmapId() {
        return ++smapCount;
    }

    abstract protected Key getKey(Node node);

    /**
     * Returns a StyleHelper for the given Node, or null if there are no styles
     * for this node.
     */
    public final StyleHelper getStyleHelper(Node node) {

        final Key key = getKey(node);
        Cache cache = cacheMap.get(key);

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
            final String className = node.getClass().getName();
            final String id = node.getId();
            final List<String> styleClass = node.getStyleClass();
            
            List<Stylesheet> stylesheetsToProcess = referencedStylesheets;

            for (int i = 0, imax = stylesheetsToProcess.size(); i < imax; i++) {
                final Stylesheet ss = stylesheetsToProcess.get(i);

                final List<Rule> stylesheetRules = ss != null ? ss.getRules() : null;
                if (stylesheetRules == null || stylesheetRules.isEmpty()) {
                    continue;
                }

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
                    if (!mightApply && !canHaveChildren) {
                        continue;
                    }

                    // For each selector, rule, stylesheet look for whether this Node
                    // is referenced in the ancestor part of the selector, and whether or
                    // not it also has pseudoclasses specified
                    final int smax = rule.selectors != null ? rule.selectors.size() : 0;
                    for (int s = 0; s < smax; s++) {
                        final Selector selector = rule.selectors.get(s);
                        if (selector instanceof CompoundSelector) {

                            final CompoundSelector cs = (CompoundSelector) selector;
                            final List<SimpleSelector> csSelectors = cs.getSelectors();

                            // if mightApply is true, then the right-most selector
                            // was matched and we just need the pseudoclass state
                            // from that selector.
                            if (mightApply) {
                                final SimpleSelector simple =
                                        csSelectors.get(csSelectors.size() - 1);
                                pseudoclassStateMask = pseudoclassStateMask
                                        | getPseudoclassMask(simple.getPseudoclasses());


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
                                for (int sctr = 0, max_sctr = csSelectors.size() - 1; sctr < max_sctr; sctr++) {
                                    final SimpleSelector simple = csSelectors.get(sctr);
                                    if (simple.mightApply(className, id, styleClass)) {
                                        pseudoclassStateMask = pseudoclassStateMask
                                                | getPseudoclassMask(simple.getPseudoclasses());
                                        impactsChildren = true;
                                    }
                                }
                            }
                            // Not a compound selector. If the selector might apply,
                            // then save off the pseudoclass state. StyleHelper
                            // checks this to see if a pseudoclass matters or not.
                        } else if (mightApply) {
                            SimpleSelector simple = (SimpleSelector) selector;
                            pseudoclassStateMask = pseudoclassStateMask
                                    | getPseudoclassMask(simple.getPseudoclasses());
                        }
                    }
                }
            }

            //
            // regardless of whether or not the cache is shared, a Cache
            // object is still needed in order to do the lookup.
            //
            final Key newKey = key.dup();

            cache = new Cache(rules, pseudoclassStateMask, impactsChildren);
            cacheMap.put(newKey, cache);

        }

        // the key is an instance variable and so we need to null the
        // key.styleClass to prevent holding a hard reference to the
        // styleClass (and its Node)
        key.styleClass = null;
        
        // Return the style helper looked up by the cache. The cache will
        // create a style helper if necessary (and possible), so we don't
        // have to worry about that part.
        StyleMap smap = cache.getStyleMap(this, node);
        if (smap == null) {
            return null;
        }

        StyleHelper helper =
                StyleHelper.create(
                node,
                smap.map,
                styleCache,
                cache.pseudoclassStateMask,
                smap.uniqueId);
        
        return helper;
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
    // Inner classes
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

    }

    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    protected static class Cache {

        // this must be initialized to the appropriate possible rules when
        // the helper cache is created by the StylesheetContainer
        private final List<Rule> rules;
        private final long pseudoclassStateMask;
        private final boolean impactsChildren;
        private final Map<Long, StyleMap> cache;

        Cache(List<Rule> rules, long pseudoclassStateMask, boolean impactsChildren) {
            this.rules = rules;
            this.pseudoclassStateMask = pseudoclassStateMask;
            this.impactsChildren = impactsChildren;
            this.cache = new HashMap<Long, StyleMap>();
        }

        private StyleMap getStyleMap(StyleManager owner, Node node) {

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
    static class Key {
        
        public Key() {
        }

        protected Key dup() {
            
            Key key = new Key();
            key.className  = className;
            key.id = id;
            final int nElements = styleClass.size();
            key.styleClass = new ArrayList<String>(nElements);
            for (int n = 0; n < nElements; n++) {
                key.styleClass.add(styleClass.get(n));
            }
            
            return key;
        }
        
        // note that the class name here is the *full* class name, such as
        // javafx.scene.control.Button. We only bother parsing this down to the
        // last part when doing matching against selectors, and so want to avoid
        // having to do a bunch of preliminary parsing in places where it isn't
        // necessary.
        protected String className;
        protected String id;
        protected List<String> styleClass;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Key) {
                Key other = (Key)o;
                boolean eq =
                        className.equals(other.className)
                    && (   (id == null && other.id == null)
                        || (id != null && id.equals(other.id))
                       )
                    && (   (styleClass == null && other.styleClass == null)
                        || (styleClass != null && styleClass.containsAll(other.styleClass))
                       );
                return eq;
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
