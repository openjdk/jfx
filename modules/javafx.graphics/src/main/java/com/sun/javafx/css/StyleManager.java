/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.util.DataURI;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.css.FontFace;
import javafx.css.PseudoClass;
import javafx.css.Rule;
import javafx.css.Selector;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleConverter;
import javafx.css.Stylesheet;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Window;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
 * but must combine its selectors with the selectors specified in "styles" and perform
 * more work to cascade properly. <p> The 2nd level cache contains a data
 * structure called the Cache. The Cache contains an ordered sequence of Rules,
 * a Long, and a Map. The ordered sequence of selectors are the selectors that *may*
 * match a node with the given classname, id, and style class. For example,
 * selectors which may apply are any selector where the simple selector of the selector
 * contains a reference to the id, style class, or classname of the Node, or a
 * compound selector whose "descendant" part is a simple selector which contains
 * a reference to the id, style class, or classname of the Node. <p> During
 * lookup, we will iterate over all the potential selectors and discover if they
 * apply to this particular node. If so, then we toggle a bit position in the
 * Long corresponding to the position of the selector that matched. This long then
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

    /**
     * Global lock object for the StyleManager. StyleManager is a singleton,
     * which loads CSS stylesheets and manages style caches for all scenes.
     * It needs to be thread-safe since a Node or Scene can be constructed and
     * load its stylesheets on an arbitrary thread, meaning that multiple Scenes
     * can load or apply their stylesheets concurrently. The global lock is used
     * to serialize access to the various state in the StyleManager.
     */
    private static final Object styleLock = new Object();

    private static PlatformLogger LOGGER;
    private static PlatformLogger getLogger() {
        if (LOGGER == null) {
            LOGGER = com.sun.javafx.util.Logging.getCSSLogger();
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

    private StyleManager() {
    }

    /**
     * A map from a parent to its style cache. The parent is either a Scene root, or a
     * Parent with author stylesheets. If a Scene or Parent is removed from the scene,
     * it's cache is annihilated.
     */
    // public for testing
    public static final Map<Parent, CacheContainer> cacheContainerMap = new WeakHashMap<>();

    // package for testing
    CacheContainer getCacheContainer(Styleable styleable, SubScene subScene) {

        if (styleable == null && subScene == null) return null;

        Parent root = null;

        if (subScene != null) {
            root = subScene.getRoot();

        } else if (styleable instanceof Node) {

            Node node = (Node)styleable;
            Scene scene = node.getScene();
            if (scene != null) root = scene.getRoot();

        } else if (styleable instanceof Window) {
            // this catches the PopupWindow case
            Scene scene = ((Window)styleable).getScene();
            if (scene != null) root = scene.getRoot();
        }
        // todo: what other Styleables need to be handled here?

        if (root == null) return null;

        synchronized (styleLock) {
            CacheContainer container = cacheContainerMap.get(root);
            if (container == null) {
                container = new CacheContainer();
                cacheContainerMap.put(root, container);
            }

            return container;
        }
    }

    /**
     * StyleHelper uses this cache but it lives here so it can be cleared
     * when style-sheets change.
     */
    public StyleCache getSharedCache(Styleable styleable, SubScene subScene, StyleCache.Key key) {

        CacheContainer container = getCacheContainer(styleable, subScene);
        if (container == null) return null;

        Map<StyleCache.Key,StyleCache> styleCache = container.getStyleCache();
        if (styleCache == null) return null;

        StyleCache sharedCache = styleCache.get(key);
        if (sharedCache == null) {
            sharedCache = new StyleCache();
            styleCache.put(new StyleCache.Key(key), sharedCache);
        }

        return sharedCache;
    }

    public StyleMap getStyleMap(Styleable styleable, SubScene subScene, int smapId) {

        if (smapId == -1) return StyleMap.EMPTY_MAP;

        CacheContainer container = getCacheContainer(styleable, subScene);
        if (container == null) return StyleMap.EMPTY_MAP;

        return container.getStyleMap(smapId);
    }

    /**
     * A list of user-agent stylesheets from Scene or SubScene.
     * The order of the entries in this list does not matter since a Scene or
     * SubScene will only have zero or one user-agent stylesheets.
     */
    // public for testing
    public final List<StylesheetContainer> userAgentStylesheetContainers = new ArrayList<>();
    /**
     * A list of user-agent stylesheet urls from calling setDefaultUserAgentStylesheet and
     * addUserAgentStylesheet. The order of entries this list matters. The zeroth element is
     * _the_ platform default.
     */
    // public for testing
    public final List<StylesheetContainer> platformUserAgentStylesheetContainers = new ArrayList<>();
    // public for testing
    public boolean hasDefaultUserAgentStylesheet = false;

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
    // package for testing
    static class StylesheetContainer {

        // the stylesheet uri
        final String fname;
        // the parsed stylesheet so we don't reparse for every parent that uses it
        final Stylesheet stylesheet;
        // the parents or scenes that use this stylesheet. Typically, this list
        //  should be very small.
        final SelectorPartitioning selectorPartitioning;

        // who uses this stylesheet?
        final RefList<Parent> parentUsers;

        final int hash;
        final byte[] checksum;
        boolean checksumInvalid = false;

        StylesheetContainer(String fname, Stylesheet stylesheet) {
            this(fname, stylesheet, stylesheet != null ? calculateCheckSum(stylesheet.getUrl()) : new byte[0]);
        }

        StylesheetContainer(String fname, Stylesheet stylesheet, byte[] checksum) {

            this.fname = fname;
            hash = (fname != null) ? fname.hashCode() : 127;

            this.stylesheet = stylesheet;
            if (stylesheet != null) {
                selectorPartitioning = new SelectorPartitioning();
                final List<Rule> rules = stylesheet.getRules();
                final int rMax = rules == null || rules.isEmpty() ? 0 : rules.size();
                for (int r=0; r<rMax; r++) {

                    final Rule rule = rules.get(r);
                    // final List<Selector> selectors = rule.getUnobservedSelectorList();
                    final List<Selector> selectors = rule.getSelectors();
                    final int sMax = selectors == null || selectors.isEmpty() ? 0 : selectors.size();
                    for (int s=0; s < sMax; s++) {

                        final Selector selector = selectors.get(s);
                        selectorPartitioning.partition(selector);

                    }
                }

            } else {
                selectorPartitioning = null;
            }

            this.parentUsers = new RefList<Parent>();

            this.checksum = checksum;
        }

        void invalidateChecksum() {
            // if checksum is byte[0], then it is forever valid.
            checksumInvalid = checksum.length > 0;
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
     */
    // package for testing
    static class RefList<K> {

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

        void remove(K key) {

            for (int n=list.size()-1; 0<=n; --n) {
                final Reference<K> ref = list.get(n);
                final K k = ref.get();
                if (k == null) {
                    // stale reference, remove it.
                    list.remove(n);
                } else {
                    // already have it, bail
                    if (k == key) {
                        list.remove(n);
                        return;
                    }
                }
            }
        }

        // for unit testing
        boolean contains(K key) {
            for (int n=list.size()-1; 0<=n; --n) {
                final Reference<K> ref = list.get(n);
                final K k = ref.get();
                if (k == key) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A map from String => Stylesheet. If a stylesheet for the
     * given URL has already been loaded then we'll simply reuse the stylesheet
     * rather than loading a duplicate.
     * This list is for author stylesheets and not for user-agent stylesheets. User-agent
     * stylesheets are either platformUserAgentStylesheetContainers or userAgentStylesheetContainers
     */
    // public for unit testing
    public final Map<String,StylesheetContainer> stylesheetContainerMap = new HashMap<>();


    /**
     * called from Window when the scene is closed.
     */
    public void forget(final Scene scene) {

        if (scene == null) return;

        forget(scene.getRoot());

        synchronized (styleLock) {
            //
            // if this scene has user-agent stylesheets, clean up the userAgentStylesheetContainers list
            //
            String sceneUserAgentStylesheet = null;
            if ((scene.getUserAgentStylesheet() != null) &&
                    (!(sceneUserAgentStylesheet = scene.getUserAgentStylesheet().trim()).isEmpty())) {

                for(int n=userAgentStylesheetContainers.size()-1; 0<=n; --n) {
                    StylesheetContainer container = userAgentStylesheetContainers.get(n);
                    if (sceneUserAgentStylesheet.equals(container.fname)) {
                        container.parentUsers.remove(scene.getRoot());
                        if (container.parentUsers.list.size() == 0) {
                            userAgentStylesheetContainers.remove(n);
                        }
                    }
                }
            }

            //
            // remove any parents belonging to this scene from the stylesheetContainerMap
            //
            Set<Entry<String,StylesheetContainer>> stylesheetContainers = stylesheetContainerMap.entrySet();
            Iterator<Entry<String,StylesheetContainer>> iter = stylesheetContainers.iterator();

            while(iter.hasNext()) {

                Entry<String,StylesheetContainer> entry = iter.next();
                StylesheetContainer container = entry.getValue();

                Iterator<Reference<Parent>> parentIter = container.parentUsers.list.iterator();
                while (parentIter.hasNext()) {

                    Reference<Parent> ref = parentIter.next();
                    Parent _parent = ref.get();

                    if (_parent == null || _parent.getScene() == scene || _parent.getScene() == null) {
                        ref.clear();
                        parentIter.remove();
                    }
                }

                if (container.parentUsers.list.isEmpty()) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * called from Scene's stylesheets property's onChanged method
     */
    public void stylesheetsChanged(Scene scene, Change<String> c) {

        synchronized (styleLock) {
            // Clear the cache so the cache will be rebuilt.
            Set<Entry<Parent,CacheContainer>> entrySet = cacheContainerMap.entrySet();
            for(Entry<Parent,CacheContainer> entry : entrySet) {
                Parent parent = entry.getKey();
                CacheContainer container = entry.getValue();
                if (parent.getScene() == scene) {
                    container.clearCache();
                }

            }

            c.reset();
            while(c.next()) {
                if (c.wasRemoved()) {
                    for (String fname : c.getRemoved()) {
                        stylesheetRemoved(scene, fname);

                        StylesheetContainer stylesheetContainer = stylesheetContainerMap.get(fname);
                        if (stylesheetContainer != null) {
                            stylesheetContainer.invalidateChecksum();
                        }

                    }
                }
            }
        }
    }

    private void stylesheetRemoved(Scene scene, String fname) {
        stylesheetRemoved(scene.getRoot(), fname);
    }

    /**
     * Called from Parent's scenesChanged method when the Parent's scene is set to null.
     * @param parent The Parent being removed from the scene-graph
     */
    public void forget(Parent parent) {

        if (parent == null) return;

        synchronized (styleLock) {
            // RT-34863 - clean up CSS cache when Parent is removed from scene-graph
            CacheContainer removedContainer = cacheContainerMap.remove(parent);
            if (removedContainer != null) {
                removedContainer.clearCache();
            }

            final List<String> stylesheets = parent.getStylesheets();
            if (stylesheets != null && !stylesheets.isEmpty()) {
                for (String fname : stylesheets) {
                    stylesheetRemoved(parent, fname);
                }
            }

            Iterator<Entry<String,StylesheetContainer>> containerIterator = stylesheetContainerMap.entrySet().iterator();
            while (containerIterator.hasNext()) {
                Entry<String,StylesheetContainer> entry = containerIterator.next();
                StylesheetContainer container = entry.getValue();
                container.parentUsers.remove(parent);
                if (container.parentUsers.list.isEmpty()) {

                    containerIterator.remove();

                    if (container.selectorPartitioning != null) {
                        container.selectorPartitioning.reset();
                    }


                    // clean up image cache by removing images from the cache that
                    // might have come from this stylesheet
                    final String fname = container.fname;
                    imageCache.cleanUpImageCache(fname);
                }
            }

            // Do not iterate over children since this method will be called on each from Parent#scenesChanged
        }
    }

    /**
     * called from Parent's stylesheets property's onChanged method
     */
    public void stylesheetsChanged(Parent parent, Change<String> c) {
        synchronized (styleLock) {
            c.reset();
            while(c.next()) {
                if (c.wasRemoved()) {
                    for (String fname : c.getRemoved()) {
                        stylesheetRemoved(parent, fname);

                        StylesheetContainer stylesheetContainer = stylesheetContainerMap.get(fname);
                        if (stylesheetContainer != null) {
                            stylesheetContainer.invalidateChecksum();
                        }
                    }
                }
            }
        }
    }

    private void stylesheetRemoved(Parent parent, String fname) {

        synchronized (styleLock) {
            StylesheetContainer stylesheetContainer = stylesheetContainerMap.get(fname);

            if (stylesheetContainer == null) return;

            stylesheetContainer.parentUsers.remove(parent);

            if (stylesheetContainer.parentUsers.list.isEmpty()) {
                removeStylesheetContainer(stylesheetContainer);
            }
        }
    }

    /**
     * called from Window when the scene is closed.
     */
    public void forget(final SubScene subScene) {

        if (subScene == null) return;
        final Parent subSceneRoot = subScene.getRoot();

        if (subSceneRoot == null) return;
        forget(subSceneRoot);

        synchronized (styleLock) {
            //
            // if this scene has user-agent stylesheets, clean up the userAgentStylesheetContainers list
            //
            String sceneUserAgentStylesheet = null;
            if ((subScene.getUserAgentStylesheet() != null) &&
                    (!(sceneUserAgentStylesheet = subScene.getUserAgentStylesheet().trim()).isEmpty())) {

                Iterator<StylesheetContainer> iterator = userAgentStylesheetContainers.iterator();
                while(iterator.hasNext()) {
                    StylesheetContainer container = iterator.next();
                    if (sceneUserAgentStylesheet.equals(container.fname)) {
                        container.parentUsers.remove(subScene.getRoot());
                        if (container.parentUsers.list.size() == 0) {
                            iterator.remove();
                        }
                    }
                }
            }

            //
            // remove any parents belonging to this SubScene from the stylesheetContainerMap
            //
            // copy the list to avoid concurrent mod.
            List<StylesheetContainer> stylesheetContainers = new ArrayList<>(stylesheetContainerMap.values());

            Iterator<StylesheetContainer> iter = stylesheetContainers.iterator();

            while(iter.hasNext()) {

                StylesheetContainer container = iter.next();

                Iterator<Reference<Parent>> parentIter = container.parentUsers.list.iterator();
                while (parentIter.hasNext()) {

                    final Reference<Parent> ref = parentIter.next();
                    final Parent _parent = ref.get();

                    if (_parent != null) {
                        // if this stylesheet refererent is a child of this subscene, nuke it.
                        Parent p = _parent;
                        while (p != null) {
                            if (subSceneRoot == p.getParent()) {
                                ref.clear();
                                parentIter.remove();
                                forget(_parent); // _parent, not p!
                                break;
                            }
                            p = p.getParent();
                        }
                    }
                }

                // forget(_parent) will remove the container if the parentUser's list is empty
                // if (container.parentUsers.list.isEmpty()) {
                //    iter.remove();
                // }
            }
        }

    }

    private void removeStylesheetContainer(StylesheetContainer stylesheetContainer) {

        if (stylesheetContainer == null) return;

        synchronized (styleLock) {
            final String fname = stylesheetContainer.fname;

            stylesheetContainerMap.remove(fname);

            if (stylesheetContainer.selectorPartitioning != null) {
                stylesheetContainer.selectorPartitioning.reset();
            }

            // if container has no references, then remove it
            for(Entry<Parent,CacheContainer> entry : cacheContainerMap.entrySet()) {

                CacheContainer container = entry.getValue();
                if (container == null || container.cacheMap == null || container.cacheMap.isEmpty()) {
                    continue;
                }

                List<List<String>> entriesToRemove = new ArrayList<>();

                for (Entry<List<String>, Map<Key,Cache>> cacheMapEntry : container.cacheMap.entrySet()) {
                    List<String> cacheMapKey = cacheMapEntry.getKey();
                    if (cacheMapKey != null ? cacheMapKey.contains(fname) : fname == null) {
                        entriesToRemove.add(cacheMapKey);
                    }
                }

                if (!entriesToRemove.isEmpty()) {
                    for (List<String> cacheMapKey : entriesToRemove) {
                        Map<Key,Cache> cacheEntry = container.cacheMap.remove(cacheMapKey);
                        if (cacheEntry != null) {
                            cacheEntry.clear();
                        }
                    }
                }
            }

            // clean up image cache by removing images from the cache that
            // might have come from this stylesheet
            imageCache.cleanUpImageCache(fname);

            final List<Reference<Parent>> parentList = stylesheetContainer.parentUsers.list;

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
                // NodeHelper.reapplyCSS called on the root.
                //
                NodeHelper.reapplyCSS(parent);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Image caching
    //
    ////////////////////////////////////////////////////////////////////////////

    private final static class ImageCache {
        private Map<String, SoftReference<Image>> imageCache = new HashMap<>();

        Image getCachedImage(String url) {

            synchronized (styleLock) {
                Image image = null;
                if (imageCache.containsKey(url)) {
                    image = imageCache.get(url).get();
                }
                if (image == null) {
                    try {
                        image = new Image(url);
                        // RT-31865
                        if (image.isError()) {
                            final PlatformLogger logger = getLogger();
                            if (logger != null && logger.isLoggable(Level.WARNING)) {
                                // If we have a "data" URL, we should use DataURI.toString() instead
                                // of just logging the entire URL. This truncates the data contained
                                // in the URL and prevents cluttering the log.
                                DataURI dataUri = DataURI.tryParse(url);
                                if (dataUri != null) {
                                    logger.warning("Error loading image: " + dataUri);
                                } else {
                                    logger.warning("Error loading image: " + url);
                                }
                            }
                            image = null;
                        }
                        imageCache.put(url, new SoftReference<>(image));
                    } catch (IllegalArgumentException | NullPointerException ex) {
                        // url was empty!
                        final PlatformLogger logger = getLogger();
                        if (logger != null && logger.isLoggable(Level.WARNING)) {
                            logger.warning(ex.getLocalizedMessage());
                        }
                    } // url was null!

                }
                return image;
            }
        }

        void cleanUpImageCache(String imgFname) {

            synchronized (styleLock) {
                if (imgFname == null || imageCache.isEmpty()) return;

                final String fname = imgFname.trim();
                if (fname.isEmpty()) return;

                int len = fname.lastIndexOf('/');
                final String path = (len > 0) ? fname.substring(0,len) : fname;
                final int plen = path.length();

                final String[] entriesToRemove = new String[imageCache.size()];
                int count = 0;

                final Set<Entry<String, SoftReference<Image>>> entrySet = imageCache.entrySet();
                for (Entry<String, SoftReference<Image>> entry : entrySet) {

                    final String key = entry.getKey();
                    if (entry.getValue().get() == null) {
                        entriesToRemove[count++] = key;
                        continue;
                    }
                    len = key.lastIndexOf('/');
                    final String kpath = (len > 0) ? key.substring(0, len) : key;
                    final int klen = kpath.length();

                    // If the longer path begins with the shorter path,
                    // then assume the image came from this path.
                    boolean match = (klen > plen) ? kpath.startsWith(path) : path.startsWith(kpath);
                    if (match) {
                        entriesToRemove[count++] = key;
                    }
                }

                for (int n = 0; n < count; n++) {
                    imageCache.remove(entriesToRemove[n]);
                }
            }
        }
    }

    private final ImageCache imageCache = new ImageCache();

    public Image getCachedImage(String url) {
        return imageCache.getCachedImage(url);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Stylesheet loading
    //
    ////////////////////////////////////////////////////////////////////////////


    private static final String skinPrefix = "com/sun/javafx/scene/control/skin/";
    private static final String skinUtilsClassName = "com.sun.javafx.scene.control.skin.Utils";

    private static URL getURL(final String str) {

        // Note: this code is duplicated, more or less, in URLConverter

        if (str == null || str.trim().isEmpty()) return null;

        try {

            URI uri =  new URI(str.trim());

            // if url doesn't have a scheme
            if (uri.isAbsolute() == false) {

                // FIXME: JIGSAW -- move this into a utility method, since it will
                // likely be needed elsewhere (e.g., in URLConverter)
                if (str.startsWith(skinPrefix) &&
                        (str.endsWith(".css") || str.endsWith(".bss"))) {

                    try {
                        ClassLoader cl = StyleManager.class.getClassLoader();
                        Class<?> clz = Class.forName(skinUtilsClassName, true, cl);
                        Method m_getResource = clz.getMethod("getResource", String.class);
                        return (URL)m_getResource.invoke(null, str.substring(skinPrefix.length()));
                    } catch (ClassNotFoundException
                            | NoSuchMethodException
                            | IllegalAccessException
                            | InvocationTargetException ex) {
                        ex.printStackTrace();
                        return null;
                    }
                }

                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                final String path = uri.getPath();

                URL resource = null;

                // FIXME: JIGSAW -- The following will only find resources not in a module
                if (path.startsWith("/")) {
                    resource = contextClassLoader.getResource(path.substring(1));
                } else {
                    resource = contextClassLoader.getResource(path);
                }

                return resource;
            }

            // else, url does have a scheme
            return uri.toURL();

        } catch (MalformedURLException malf) {
            // Do not log exception here - caller will handle null return.
            // For example, we might be looking for a .bss that doesn't exist
            return null;
        } catch (URISyntaxException urise) {
            return null;
        }
    }

    // Calculate checksum for stylesheet file. Return byte[0] if checksum could not be calculated.
    static byte[] calculateCheckSum(String fname) {

        if (fname == null || fname.isEmpty()) return new byte[0];

        try {
            final URL url = getURL(fname);

            // We only care about stylesheets from file: URLs.
            if (url != null && "file".equals(url.getProtocol())) {

                // not looking for security, just a checksum. MD5 should be faster than SHA
                try (final InputStream stream = url.openStream();
                    final DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("MD5")); ) {
                    dis.getMessageDigest().reset();
                    byte[] buffer = new byte[4096];
                    while (dis.read(buffer) != -1) { /* empty loop body is intentional */ }
                    return dis.getMessageDigest().digest();
                }

            }

        } catch (IllegalArgumentException | NoSuchAlgorithmException | IOException | SecurityException e) {
            // IOException also covers MalformedURLException
            // SecurityException means some untrusted applet

            // Fall through...
        }
        return new byte[0];
    }

    @SuppressWarnings("removal")
    public static Stylesheet loadStylesheet(final String fname) {
        try {
            return loadStylesheetUnPrivileged(fname);
        } catch (java.security.AccessControlException ace) {

            // FIXME: JIGSAW -- we no longer are in a jar file, so this code path
            // is obsolete and needs to be redone or eliminated. Fortunately, I
            // don't think it is actually needed.
            System.err.println("WARNING: security exception trying to load: " + fname);

            /*
            ** we got an access control exception, so
            ** we could be running with a security manager.
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
                    URI styleManagerJarURI = AccessController.doPrivileged((PrivilegedExceptionAction<URI>) () -> StyleManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());

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
                                jar = AccessController.doPrivileged((PrivilegedExceptionAction<JarFile>) () -> new JarFile(styleManagerJarPath), permsAcc);
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
                                            (PrivilegedAction<Stylesheet>) () -> loadStylesheetUnPrivileged(fname), permsAcc);
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

        synchronized (styleLock) {
            @SuppressWarnings("removal")
            Boolean parse = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {

                final String bss = System.getProperty("binary.css");
                // binary.css is true by default.
                // parse only if the file is not a .bss
                // and binary.css is set to false
                return (!fname.endsWith(".bss") && bss != null) ?
                    !Boolean.valueOf(bss) : Boolean.FALSE;
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

                        try {
                            // RT-36332: if loadBinary throws an IOException, make sure to try .css
                            stylesheet = Stylesheet.loadBinary(url);
                        } catch (IOException ioe) {
                            stylesheet = null;
                        }

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
                    stylesheet = new CssParser().parse(url);
                }

                if (stylesheet == null) {
                    if (errors != null) {
                        CssParser.ParseError error =
                            new CssParser.ParseError(
                                "Resource \""+fname+"\" not found."
                            );
                        errors.add(error);
                    }
                    if (getLogger().isLoggable(Level.WARNING)) {
                        getLogger().warning(
                            String.format("Resource \"%s\" not found.", fname)
                        );
                    }
                }

                // load any fonts from @font-face
                if (stylesheet != null) {
                    faceLoop: for(FontFace fontFace: stylesheet.getFontFaces()) {
                        if (fontFace instanceof FontFaceImpl) {
                            for(FontFaceImpl.FontFaceSrc src: ((FontFaceImpl)fontFace).getSources()) {
                                if (src.getType() == FontFaceImpl.FontFaceSrcType.URL) {
                                    Font loadedFont = Font.loadFont(src.getSrc(),10);
                                    if (loadedFont == null) {
                                        getLogger().info("Could not load @font-face font [" + src.getSrc() + "]");
                                    }
                                    continue faceLoop;
                                }
                            }
                        }
                    }
                }

                return stylesheet;

            } catch (FileNotFoundException fnfe) {
                if (errors != null) {
                    CssParser.ParseError error =
                        new CssParser.ParseError(
                            "Stylesheet \""+fname+"\" not found."
                        );
                    errors.add(error);
                }
                if (getLogger().isLoggable(Level.INFO)) {
                    getLogger().info("Could not find stylesheet: " + fname);//, fnfe);
                }
            } catch (IOException ioe) {
                    if (errors != null) {
                        CssParser.ParseError error =
                            new CssParser.ParseError(
                                "Could not load stylesheet: " + fname
                            );
                        errors.add(error);
                    }
                if (getLogger().isLoggable(Level.INFO)) {
                    getLogger().info("Could not load stylesheet: " + fname);//, ioe);
                }
            }
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // User Agent stylesheet handling
    //
    ////////////////////////////////////////////////////////////////////////////


    /**
     * Set a bunch of user agent stylesheets all at once. The order of the stylesheets in the list
     * is the order of their styles in the cascade. Passing null, an empty list, or a list full of empty
     * strings does nothing.
     *
     * @param urls The list of stylesheet URLs as Strings.
     */
    public void setUserAgentStylesheets(List<String> urls) {

        if (urls == null || urls.size() == 0) return;

        synchronized (styleLock) {
            // Avoid resetting user agent stylesheets if they haven't changed.
            if (urls.size() == platformUserAgentStylesheetContainers.size()) {
                boolean isSame = true;
                for (int n=0, nMax=urls.size(); n < nMax && isSame; n++) {

                    final String url = urls.get(n);
                    final String fname = (url != null) ? url.trim() : null;

                    if (fname == null || fname.isEmpty()) break;

                    StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                    // assignment in this conditional is intentional!
                    if(isSame = fname.equals(container.fname)) {
                        // don't use fname in calculateCheckSum since it is just the key to
                        // find the StylesheetContainer. Rather, use the URL of the
                        // stylesheet that was already loaded. For example, we could have
                        // fname = "com/sun/javafx/scene/control/skin/modena/modena.css, but
                        // the stylesheet URL could be jar:file://some/path/!com/sun/javafx/scene/control/skin/modena/modena.bss
                        String stylesheetUrl = container.stylesheet.getUrl();
                        byte[] checksum = calculateCheckSum(stylesheetUrl);
                        isSame = Arrays.equals(checksum, container.checksum);
                    }
                }
                if (isSame) return;
            }

            boolean modified = false;

            for (int n=0, nMax=urls.size(); n < nMax; n++) {

                final String url = urls.get(n);
                final String fname = (url != null) ? url.trim() : null;

                if (fname == null || fname.isEmpty()) continue;

                if (!modified) {
                    // we have at least one non null or non-empty url
                    platformUserAgentStylesheetContainers.clear();
                    modified = true;
                }

                if (n==0) {
                    _setDefaultUserAgentStylesheet(fname);
                } else {
                    _addUserAgentStylesheet(fname);
                }
            }

            if (modified) {
                userAgentStylesheetsChanged();
            }
        }
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
     * @param url  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643
    public void addUserAgentStylesheet(Scene scene, String url) {

        final String fname = (url != null) ? url.trim() : null;
        if (fname == null || fname.isEmpty()) {
            return;
        }

        synchronized (styleLock) {
            if (_addUserAgentStylesheet(fname)) {
                userAgentStylesheetsChanged();
            }
        }
    }

    // fname is assumed to be non null and non empty
    private boolean _addUserAgentStylesheet(String fname) {

        synchronized (styleLock) {
            // if we already have this stylesheet, bail
            for (int n=0, nMax= platformUserAgentStylesheetContainers.size(); n < nMax; n++) {
                StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                if (fname.equals(container.fname)) {
                    return false;
                }
            }

            final Stylesheet ua_stylesheet = loadStylesheet(fname);

            if (ua_stylesheet == null) return false;

            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            platformUserAgentStylesheetContainers.add(new StylesheetContainer(fname, ua_stylesheet));
            return true;
        }
    }

    /**
     * Add a user agent stylesheet, possibly overriding styles in the default
     * user agent stylesheet.
     * @param scene Only used in CssError for tracking back to the scene that loaded the stylesheet
     * @param ua_stylesheet  The stylesheet to add as a user-agent stylesheet
     */
    public void addUserAgentStylesheet(Scene scene, Stylesheet ua_stylesheet) {

        if (ua_stylesheet == null ) {
            throw new IllegalArgumentException("null arg ua_stylesheet");
        }

        // null url is ok, just means that it is a stylesheet not loaded from a file
        String url = ua_stylesheet.getUrl();
        final String fname = url != null ? url.trim() : "";

        synchronized (styleLock) {
            // if we already have this stylesheet, bail
            for (int n=0, nMax= platformUserAgentStylesheetContainers.size(); n < nMax; n++) {
                StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                if (fname.equals(container.fname)) {
                    return;
                }
            }

            platformUserAgentStylesheetContainers.add(new StylesheetContainer(fname, ua_stylesheet));

            if (ua_stylesheet != null) {
                ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            }
            userAgentStylesheetsChanged();
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
     * @param url  The file URL, either relative or absolute, as a String.
     */
    // For RT-20643
    public void setDefaultUserAgentStylesheet(Scene scene, String url) {

        final String fname = (url != null) ? url.trim() : null;
        if (fname == null || fname.isEmpty()) {
            return;
        }

        synchronized (styleLock) {
            if(_setDefaultUserAgentStylesheet(fname)) {
                userAgentStylesheetsChanged();
            }
        }
    }

    // fname is expected to be non null and non empty
    private boolean _setDefaultUserAgentStylesheet(String fname) {

        synchronized (styleLock) {
            // if we already have this stylesheet, make sure it is the first element
            for (int n=0, nMax= platformUserAgentStylesheetContainers.size(); n < nMax; n++) {
                StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                if (fname.equals(container.fname)) {
                    if (n > 0) {
                        platformUserAgentStylesheetContainers.remove(n);
                        if (hasDefaultUserAgentStylesheet) {
                            platformUserAgentStylesheetContainers.set(0, container);
                        } else {
                            platformUserAgentStylesheetContainers.add(0, container);
                        }
                    }
                    // return true only if platformUserAgentStylesheetContainers was modified
                    return n > 0;
                }
            }

            final Stylesheet ua_stylesheet = loadStylesheet(fname);

            if (ua_stylesheet == null) return false;

            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            final StylesheetContainer sc = new StylesheetContainer(fname, ua_stylesheet);

            if (platformUserAgentStylesheetContainers.size() == 0) {
                platformUserAgentStylesheetContainers.add(sc);
            }
            else if (hasDefaultUserAgentStylesheet) {
                platformUserAgentStylesheetContainers.set(0,sc);
            }
            else {
                platformUserAgentStylesheetContainers.add(0,sc);
            }
            hasDefaultUserAgentStylesheet = true;

            return true;
        }
    }

    /**
     * Removes the specified stylesheet from the application default user agent
     * stylesheet list.
     * @param url  The file URL, either relative or absolute, as a String.
     */
    public void removeUserAgentStylesheet(String url) {

        final String fname = (url != null) ? url.trim() : null;
        if (fname == null || fname.isEmpty()) {
            return;
        }

        synchronized (styleLock) {
            // if we already have this stylesheet, remove it!
            boolean removed = false;
            for (int n = platformUserAgentStylesheetContainers.size() - 1; n >= 0; n--) {
                // don't remove the platform default user agent stylesheet
                if (fname.equals(Application.getUserAgentStylesheet())) {
                    continue;
                }

                StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                if (fname.equals(container.fname)) {
                    platformUserAgentStylesheetContainers.remove(n);
                    removed = true;
                }
            }

            if (removed) {
                userAgentStylesheetsChanged();
            }
        }
    }

    /**
     * Set the user agent stylesheet. This is the base default stylesheet for
     * the platform
     */
    public void setDefaultUserAgentStylesheet(Stylesheet ua_stylesheet) {
        if (ua_stylesheet == null ) {
            return;
        }

        // null url is ok, just means that it is a stylesheet not loaded from a file
        String url = ua_stylesheet.getUrl();
        final String fname = url != null ? url.trim() : "";

        synchronized (styleLock) {
            // if we already have this stylesheet, make sure it is the first element
            for (int n=0, nMax= platformUserAgentStylesheetContainers.size(); n < nMax; n++) {
                StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                if (fname.equals(container.fname)) {
                    if (n > 0) {
                        platformUserAgentStylesheetContainers.remove(n);
                        if (hasDefaultUserAgentStylesheet) {
                            platformUserAgentStylesheetContainers.set(0, container);
                        } else {
                            platformUserAgentStylesheetContainers.add(0, container);
                        }
                    }
                    return;
                }
            }

            StylesheetContainer sc = new StylesheetContainer(fname, ua_stylesheet);
            if (platformUserAgentStylesheetContainers.size() == 0) {
                platformUserAgentStylesheetContainers.add(sc);
            } else if (hasDefaultUserAgentStylesheet) {
                platformUserAgentStylesheetContainers.set(0,sc);
            } else {
                platformUserAgentStylesheetContainers.add(0,sc);
            }
            hasDefaultUserAgentStylesheet = true;

            ua_stylesheet.setOrigin(StyleOrigin.USER_AGENT);
            userAgentStylesheetsChanged();
        }
    }

    /*
     * If the userAgentStylesheets change, then all scenes are updated.
     */
    private void userAgentStylesheetsChanged() {

        List<Parent> parents = new ArrayList<>();

        synchronized (styleLock) {
            for (CacheContainer container : cacheContainerMap.values()) {
                container.clearCache();
            }

            StyleConverter.clearCache();

            for (Parent root : cacheContainerMap.keySet()) {
                if (root == null) {
                    continue;
                }
                parents.add(root);
            }
        }

        for (Parent root : parents) NodeHelper.reapplyCSS(root);
    }

    private List<StylesheetContainer> processStylesheets(List<String> stylesheets, Parent parent) {

        synchronized (styleLock) {
            final List<StylesheetContainer> list = new ArrayList<StylesheetContainer>();
            for (int n = 0, nMax = stylesheets.size(); n < nMax; n++) {
                final String fname = stylesheets.get(n);

                StylesheetContainer container = null;
                if (stylesheetContainerMap.containsKey(fname)) {
                    container = stylesheetContainerMap.get(fname);

                    if (!list.contains(container)) {
                        // minor optimization: if existing checksum in byte[0], then don't bother recalculating
                        if (container.checksumInvalid) {
                            final byte[] checksum = calculateCheckSum(fname);
                            if (!Arrays.equals(checksum, container.checksum)) {
                                removeStylesheetContainer(container);

                                // Stylesheet did change. Re-load the stylesheet and update the container map.
                                Stylesheet stylesheet = loadStylesheet(fname);
                                container = new StylesheetContainer(fname, stylesheet, checksum);
                                stylesheetContainerMap.put(fname, container);
                            } else {
                                container.checksumInvalid = false;
                            }
                        }
                        list.add(container);
                    }

                    // RT-22565: remember that this parent or scene uses this stylesheet.
                    // Later, if the cache is cleared, the parent or scene is told to
                    // reapply css.
                    container.parentUsers.add(parent);

                } else {
                    final Stylesheet stylesheet = loadStylesheet(fname);
                    // stylesheet may be null which would mean that some IOException
                    // was thrown while trying to load it. Add it to the
                    // stylesheetContainerMap anyway as this will prevent further
                    // attempts to parse the file
                    container = new StylesheetContainer(fname, stylesheet);
                    // RT-22565: remember that this parent or scene uses this stylesheet.
                    // Later, if the cache is cleared, the parent or scene is told to
                    // reapply css.
                    container.parentUsers.add(parent);
                    stylesheetContainerMap.put(fname, container);

                    list.add(container);
                }
            }
            return list;
        }
    }

    //
    // recurse so that stylesheets of Parents closest to the root are
    // added to the list first. The ensures that declarations for
    // stylesheets further down the tree (closer to the leaf) have
    // a higher ordinal in the cascade.
    //
    private List<StylesheetContainer> gatherParentStylesheets(final Parent parent) {

        if (parent == null) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<String> parentStylesheets = ParentHelper.getAllParentStylesheets(parent);

        if (parentStylesheets == null || parentStylesheets.isEmpty()) {
            return Collections.<StylesheetContainer>emptyList();
        }

        synchronized (styleLock) {
            return processStylesheets(parentStylesheets, parent);
        }
    }

    //
    //
    //
    private List<StylesheetContainer> gatherSceneStylesheets(final Scene scene) {

        if (scene == null) {
            return Collections.<StylesheetContainer>emptyList();
        }

        final List<String> sceneStylesheets = scene.getStylesheets();

        if (sceneStylesheets == null || sceneStylesheets.isEmpty()) {
            return Collections.<StylesheetContainer>emptyList();
        }

        synchronized (styleLock) {
            return processStylesheets(sceneStylesheets, scene.getRoot());
        }
    }

    // reuse key to avoid creation of numerous small objects
    private Key key = null;

    // Stores weak references to regions which return non-null user agent stylesheets
    private final WeakHashMap<Region, String> weakRegionUserAgentStylesheetMap = new WeakHashMap<>();

    /**
     * Finds matching styles for this Node.
     */
    public StyleMap findMatchingStyles(Node node, SubScene subScene, Set<PseudoClass>[] triggerStates) {

        final Scene scene = node.getScene();
        if (scene == null) {
            return StyleMap.EMPTY_MAP;
        }

        CacheContainer cacheContainer = getCacheContainer(node, subScene);
        if (cacheContainer == null) {
            assert false : node.toString();
            return StyleMap.EMPTY_MAP;
        }

        synchronized (styleLock) {
            final Parent parent =
                (node instanceof Parent)
                    ? (Parent) node : node.getParent();

            final List<StylesheetContainer> parentStylesheets =
                        gatherParentStylesheets(parent);

            final boolean hasParentStylesheets = parentStylesheets.isEmpty() == false;

            final List<StylesheetContainer> sceneStylesheets = gatherSceneStylesheets(scene);

            final boolean hasSceneStylesheets = sceneStylesheets.isEmpty() == false;

            final String inlineStyle = node.getStyle();
            final boolean hasInlineStyles = inlineStyle != null && inlineStyle.trim().isEmpty() == false;

            final String sceneUserAgentStylesheet = scene.getUserAgentStylesheet();
            final boolean hasSceneUserAgentStylesheet =
                    sceneUserAgentStylesheet != null && sceneUserAgentStylesheet.trim().isEmpty() == false;

            final String subSceneUserAgentStylesheet =
                    (subScene != null) ? subScene.getUserAgentStylesheet() : null;
            final boolean hasSubSceneUserAgentStylesheet =
                    subSceneUserAgentStylesheet != null && subSceneUserAgentStylesheet.trim().isEmpty() == false;

            String regionUserAgentStylesheet = null;
            // is this node in a region that has its own stylesheet?
            Node region = node;
            while (region != null) {
                if (region instanceof Region) {
                    regionUserAgentStylesheet = weakRegionUserAgentStylesheetMap.computeIfAbsent(
                            (Region)region, Region::getUserAgentStylesheet);

                    if (regionUserAgentStylesheet != null) {
                        // We want 'region' to be the node that has the user agent stylesheet.
                        // 'region' is used below - look for if (hasRegionUserAgentStylesheet) block
                        break;
                    }
                }
                region = region.getParent();
            }


            final boolean hasRegionUserAgentStylesheet =
                    regionUserAgentStylesheet != null && regionUserAgentStylesheet.trim().isEmpty() == false;

            //
            // Are there any stylesheets at all?
            // If not, then there is nothing to match and the
            // resulting StyleMap is going to end up empty
            //
            if (hasInlineStyles == false
                    && hasParentStylesheets == false
                    && hasSceneStylesheets == false
                    && hasSceneUserAgentStylesheet == false
                    && hasSubSceneUserAgentStylesheet == false
                    && hasRegionUserAgentStylesheet == false
                    && platformUserAgentStylesheetContainers.isEmpty()) {
                return StyleMap.EMPTY_MAP;
            }

            final String cname = node.getTypeSelector();
            final String id = node.getId();
            final List<String> styleClasses = node.getStyleClass();

            if (key == null) {
                key = new Key();
            }

            key.className = cname;
            key.id = id;
            for(int n=0, nMax=styleClasses.size(); n<nMax; n++) {

                final String styleClass = styleClasses.get(n);
                if (styleClass == null || styleClass.isEmpty()) continue;

                key.styleClasses.add(StyleClassSet.getStyleClass(styleClass));
            }

            Map<Key, Cache> cacheMap = cacheContainer.getCacheMap(parentStylesheets,regionUserAgentStylesheet);
            Cache cache = cacheMap.get(key);

            if (cache != null) {
                // key will be reused, so clear the styleClasses for next use
                key.styleClasses.clear();

            } else {

                // If the cache is null, then we need to create a new Cache and
                // add it to the cache map

                // Construct the list of Selectors that could possibly apply
                final List<Selector> selectorData = new ArrayList<>();

                // User agent stylesheets have lowest precedence and go first
                if (hasSubSceneUserAgentStylesheet || hasSceneUserAgentStylesheet) {

                    // if has both, use SubScene
                    final String uaFileName = hasSubSceneUserAgentStylesheet ?
                            subScene.getUserAgentStylesheet().trim() :
                            scene.getUserAgentStylesheet().trim();


                    StylesheetContainer container = null;
                    for (int n=0, nMax=userAgentStylesheetContainers.size(); n<nMax; n++) {
                        container = userAgentStylesheetContainers.get(n);
                        if (uaFileName.equals(container.fname)) {
                            break;
                        }
                        container = null;
                    }

                    if (container == null) {
                        Stylesheet stylesheet = loadStylesheet(uaFileName);
                        if (stylesheet != null) {
                            stylesheet.setOrigin(StyleOrigin.USER_AGENT);
                        }
                        container = new StylesheetContainer(uaFileName, stylesheet);
                        userAgentStylesheetContainers.add(container);
                    }

                    if (container.selectorPartitioning != null) {

                        final Parent root = hasSubSceneUserAgentStylesheet ? subScene.getRoot() : scene.getRoot();
                        container.parentUsers.add(root);

                        final List<Selector> matchingRules =
                                container.selectorPartitioning.match(id, cname, key.styleClasses);
                        selectorData.addAll(matchingRules);
                    }

                } else if (platformUserAgentStylesheetContainers.isEmpty() == false) {
                    for(int n=0, nMax= platformUserAgentStylesheetContainers.size(); n<nMax; n++) {
                        final StylesheetContainer container = platformUserAgentStylesheetContainers.get(n);
                        if (container != null && container.selectorPartitioning != null) {
                            final List<Selector> matchingRules =
                                    container.selectorPartitioning.match(id, cname, key.styleClasses);
                            selectorData.addAll(matchingRules);
                        }
                    }
                }

                if (hasRegionUserAgentStylesheet) {
                    // Unfortunate duplication of code from previous block. No time to refactor.
                    StylesheetContainer container = null;
                    for (int n=0, nMax=userAgentStylesheetContainers.size(); n<nMax; n++) {
                        container = userAgentStylesheetContainers.get(n);
                        if (regionUserAgentStylesheet.equals(container.fname)) {
                            break;
                        }
                        container = null;
                    }

                    if (container == null) {
                        Stylesheet stylesheet = loadStylesheet(regionUserAgentStylesheet);
                        if (stylesheet != null) {
                            stylesheet.setOrigin(StyleOrigin.USER_AGENT);
                        }
                        container = new StylesheetContainer(regionUserAgentStylesheet, stylesheet);
                        userAgentStylesheetContainers.add(container);
                    }

                    if (container.selectorPartitioning != null) {

                        // Depending on RefList add method not allowing duplicates.
                        container.parentUsers.add((Parent)region);

                        final List<Selector> matchingRules =
                                container.selectorPartitioning.match(id, cname, key.styleClasses);
                        selectorData.addAll(matchingRules);
                    }

                }

                // Scene stylesheets come next since declarations from
                // parent stylesheets should take precedence.
                if (sceneStylesheets.isEmpty() == false) {
                    for(int n=0, nMax=sceneStylesheets.size(); n<nMax; n++) {
                        final StylesheetContainer container = sceneStylesheets.get(n);
                        if (container != null && container.selectorPartitioning != null) {
                            final List<Selector> matchingRules =
                                    container.selectorPartitioning.match(id, cname, key.styleClasses);
                            selectorData.addAll(matchingRules);
                        }
                    }
                }

                // lastly, parent stylesheets
                if (hasParentStylesheets) {
                    final int nMax = parentStylesheets == null ? 0 : parentStylesheets.size();
                    for(int n=0; n<nMax; n++) {
                        final StylesheetContainer container = parentStylesheets.get(n);
                        if (container.selectorPartitioning != null) {
                            final List<Selector> matchingRules =
                                    container.selectorPartitioning.match(id, cname, key.styleClasses);
                            selectorData.addAll(matchingRules);
                        }
                    }
                }

                // create a new Cache from these selectors.
                cache = new Cache(selectorData);
                cacheMap.put(key, cache);

                // cause a new Key to be created the next time this method is called
                key = null;
            }

            //
            // Create a style helper for this node from the styles that match.
            //
            StyleMap smap = cache.getStyleMap(cacheContainer, node, triggerStates, hasInlineStyles);

            return smap;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // CssError reporting
    //
    ////////////////////////////////////////////////////////////////////////////

    private static ObservableList<CssParser.ParseError> errors = null;
    /**
     * Errors that may have occurred during css processing.
     * This list is null until errorsProperty() is called.
     *
     * NOTE: this is not thread-safe, and cannot readily be made so given the
     * nature of the API.
     * Currently it is only used by SceneBuilder. If a public API is ever
     * needed, then a new API should be designed.
     *
     * @return
     */
    public static ObservableList<CssParser.ParseError> errorsProperty() {
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
    public static ObservableList<CssParser.ParseError> getErrors() {
        return errors;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Classes and routines for mapping styles to a Node
    //
    ////////////////////////////////////////////////////////////////////////////

    private static List<String> cacheMapKey;

    // Each Scene has its own cache
    // package for testing
    static class CacheContainer {

        private Map<StyleCache.Key,StyleCache> getStyleCache() {
            if (styleCache == null) styleCache = new HashMap<StyleCache.Key, StyleCache>();
            return styleCache;
        }

        private Map<Key,Cache> getCacheMap(List<StylesheetContainer> parentStylesheets, String regionUserAgentStylesheet) {

            if (cacheMap == null) {
                cacheMap = new HashMap<List<String>,Map<Key,Cache>>();
            }

            synchronized (styleLock) {
                if ((parentStylesheets == null || parentStylesheets.isEmpty()) &&
                        (regionUserAgentStylesheet == null || regionUserAgentStylesheet.isEmpty())) {

                    Map<Key,Cache> cmap = cacheMap.get(null);
                    if (cmap == null) {
                        cmap = new HashMap<Key,Cache>();
                        cacheMap.put(null, cmap);
                    }
                    return cmap;

                } else {

                    final int nMax = parentStylesheets.size();
                    if (cacheMapKey == null) {
                        cacheMapKey = new ArrayList<String>(nMax);
                    }
                    for (int n=0; n<nMax; n++) {
                        StylesheetContainer sc = parentStylesheets.get(n);
                        if (sc == null || sc.fname == null || sc.fname.isEmpty()) continue;
                        cacheMapKey.add(sc.fname);
                    }
                    if (regionUserAgentStylesheet != null) {
                        cacheMapKey.add(regionUserAgentStylesheet);
                    }
                    Map<Key,Cache> cmap = cacheMap.get(cacheMapKey);
                    if (cmap == null) {
                        cmap = new HashMap<Key,Cache>();
                        cacheMap.put(cacheMapKey, cmap);
                        // create a new cacheMapKey the next time this method is called
                        cacheMapKey = null;
                    } else {
                        // reuse cacheMapKey, but not the data, the next time this method is called
                        cacheMapKey.clear();
                    }
                    return cmap;

                }
            }

        }

        private List<StyleMap> getStyleMapList() {
            if (styleMapList == null) styleMapList = new ArrayList<StyleMap>();
            return styleMapList;
        }

        private int nextSmapId() {
            styleMapId = baseStyleMapId + getStyleMapList().size();
            return styleMapId;
        }

        private void addStyleMap(StyleMap smap) {
            getStyleMapList().add(smap);
        }

        public StyleMap getStyleMap(int smapId) {

            final int correctedId = smapId - baseStyleMapId;

            if (0 <= correctedId && correctedId < getStyleMapList().size()) {
                return getStyleMapList().get(correctedId);
            }

            return StyleMap.EMPTY_MAP;
        }

        private void clearCache() {

            if (cacheMap != null) cacheMap.clear();
            if (styleCache != null) styleCache.clear();
            if (styleMapList != null) styleMapList.clear();

            baseStyleMapId = styleMapId;
            // 7/8ths is totally arbitrary
            if (baseStyleMapId > Integer.MAX_VALUE/8*7) {
                baseStyleMapId = styleMapId = 0;
            }
        }

        /**
         * Get the mapping of property to style from Node.style for this node.
         */
        private Selector getInlineStyleSelector(String inlineStyle) {

            // If there are no styles for this property then we can just bail
            if ((inlineStyle == null) || inlineStyle.trim().isEmpty()) return null;

            if (inlineStylesCache != null && inlineStylesCache.containsKey(inlineStyle)) {
                // Value of Map entry may be null!
                return inlineStylesCache.get(inlineStyle);
            }

            //
            // inlineStyle wasn't in the inlineStylesCache, or inlineStylesCache was null
            //

            if (inlineStylesCache == null) {
                inlineStylesCache = new HashMap<>();
            }

            final Stylesheet inlineStylesheet =
                    new CssParser().parse("*{"+inlineStyle+"}");

            if (inlineStylesheet != null) {

                inlineStylesheet.setOrigin(StyleOrigin.INLINE);

                List<Rule> rules = inlineStylesheet.getRules();
                Rule rule = rules != null && !rules.isEmpty() ? rules.get(0) : null;

                //List<Selector> selectors = rule != null ? rule.getUnobservedSelectorList() : null;
                List<Selector> selectors = rule != null ? rule.getSelectors() : null;
                Selector selector = selectors != null && !selectors.isEmpty() ? selectors.get(0) : null;

                // selector might be null if parser throws some exception
                if (selector != null) {
                    selector.setOrdinal(-1);

                    inlineStylesCache.put(inlineStyle, selector);
                    return selector;
                }
                // if selector is null, fall through

            }

            // even if selector is null, put it in cache so we don't
            // bother with trying to parse it again.
            inlineStylesCache.put(inlineStyle, null);
            return null;

        }

        private Map<StyleCache.Key,StyleCache> styleCache;

        private Map<List<String>, Map<Key,Cache>> cacheMap;

        private List<StyleMap> styleMapList;

        /**
         * Cache of parsed, inline styles. The key is Node.style.
         * The value is the Selector from the inline stylesheet.
         */
        private Map<String,Selector> inlineStylesCache;

        /*
         * A simple counter used to generate a unique id for a StyleMap.
         * This unique id is used by StyleHelper in figuring out which
         * style cache to use.
         */
        private int styleMapId = 0;

        // When the cache is cleared, styleMapId counting begins here.
        // If a StyleHelper calls getStyleMap with an id less than the
        // baseStyleMapId, then that StyleHelper is working with an old
        // cache and is no longer valid.
        private int baseStyleMapId = 0;

    }

    /**
     * Creates and caches maps of styles, reusing them as often as practical.
     */
    private static class Cache {

        private static class Key {
            final long[] key;
            final String inlineStyle;

            Key(long[] key, String inlineStyle) {
                this.key = key;
                // let inlineStyle be null if it is empty
                this.inlineStyle =  (inlineStyle != null && inlineStyle.trim().isEmpty() ? null : inlineStyle);
            }

            @Override public String toString() {
                return Arrays.toString(key) + (inlineStyle != null ? "* {" + inlineStyle + "}" : "");
            }

            @Override
            public int hashCode() {
                int hash = 3;
                hash = 17 * hash + Arrays.hashCode(this.key);
                if (inlineStyle != null) hash = 17 * hash + inlineStyle.hashCode();
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
                if (inlineStyle == null ? other.inlineStyle != null : !inlineStyle.equals(other.inlineStyle)) {
                    return false;
                }
                if (!Arrays.equals(this.key, other.key)) {
                    return false;
                }
                return true;
            }

        }

        // this must be initialized to the appropriate possible selectors when
        // the helper cache is created by the StylesheetContainer. Note that
        // SelectorPartioning sorts the matched selectors by ordinal, so this
        // list of selectors will be in the same order in which the selectors
        // appear in the stylesheets.
        private final List<Selector> selectors;
        private final Map<Key, Integer> cache;

        Cache(List<Selector> selectors) {
            this.selectors = selectors;
            this.cache = new HashMap<Key, Integer>();
        }

        private StyleMap getStyleMap(CacheContainer cacheContainer, Node node, Set<PseudoClass>[] triggerStates, boolean hasInlineStyle) {

            if ((selectors == null || selectors.isEmpty()) && !hasInlineStyle) {
                return StyleMap.EMPTY_MAP;
            }

            final int selectorDataSize = selectors.size();

            //
            // Since the list of selectors is found by matching only the
            // rightmost selector, the set of selectors may larger than those
            // selectors that actually match the node. The following loop
            // whittles the list down to those selectors that apply.
            //
            //
            // To lookup from the cache, we construct a key from a Long
            // where the selectors that match this particular node are
            // represented by bits on the long[].
            //
            long key[] = new long[selectorDataSize/Long.SIZE + 1];
            boolean nothingMatched = true;

            for (int s = 0; s < selectorDataSize; s++) {

                final Selector sel = selectors.get(s);

                //
                // This particular flavor of applies takes a PseudoClassState[]
                // fills in the pseudo-class states from the selectors where
                // they apply to a node. This is an expedient to looking the
                // applies loopa second time on the matching selectors. This has to
                // be done ahead of the cache lookup since not all nodes that
                // have the same set of selectors will have the same node hierarchy.
                //
                // For example, if I have .foo:hover:focused .bar:selected {...}
                // and the "bar" node is 4 away from the root and the foo
                // node is two away from the root, pseudoclassBits would be
                // [selected, 0, hover:focused, 0]
                // Note that the states run from leaf to root. This is how
                // the code in StyleHelper expects things.
                // Note also that, if the selector does not apply, the triggerStates
                // is unchanged.
                //

                if (sel.applies(node, triggerStates, 0)) {
                    final int index = s / Long.SIZE;
                    final long mask = key[index] | 1l << s;
                    key[index] = mask;
                    nothingMatched = false;
                }
            }

            // nothing matched!
            if (nothingMatched && hasInlineStyle == false) {
                return StyleMap.EMPTY_MAP;
            }

            final String inlineStyle = node.getStyle();
            final Key keyObj = new Key(key, inlineStyle);

            if (cache.containsKey(keyObj)) {
                Integer styleMapId = cache.get(keyObj);
                final StyleMap styleMap = styleMapId != null
                        ? cacheContainer.getStyleMap(styleMapId.intValue())
                        : StyleMap.EMPTY_MAP;
                return styleMap;
            }

            final List<Selector> selectors = new ArrayList<>();

            if (hasInlineStyle) {
                Selector selector = cacheContainer.getInlineStyleSelector(inlineStyle);
                if (selector != null) selectors.add(selector);
            }

            for (int k = 0; k<key.length; k++) {

                if (key[k] == 0) continue;

                final int offset = k * Long.SIZE;

                for (int b = 0; b<Long.SIZE; b++) {

                    // bit at b in key[k] set?
                    final long mask = 1l << b;
                    if ((mask & key[k]) != mask) continue;

                    final Selector pair = this.selectors.get(offset + b);
                    selectors.add(pair);
                }
            }

            int id = cacheContainer.nextSmapId();
            cache.put(keyObj, Integer.valueOf(id));

            final StyleMap styleMap = new StyleMap(id, selectors);
            cacheContainer.addStyleMap(styleMap);
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
        final StyleClassSet styleClasses;

        private Key() {
            styleClasses = new StyleClassSet();
        }

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

                return this.styleClasses.equals(other.styleClasses);
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.className != null ? this.className.hashCode() : 0);
            hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 29 * hash + this.styleClasses.hashCode();
            return hash;
        }

    }


        }
