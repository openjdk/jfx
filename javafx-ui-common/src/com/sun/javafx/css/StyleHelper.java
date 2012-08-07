/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.Logging;
import com.sun.javafx.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import com.sun.javafx.css.Stylesheet.Origin;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.logging.PlatformLogger;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import javafx.beans.value.WritableValue;
import javafx.scene.Parent;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * The StyleHelper is a helper class used for applying CSS information to Nodes.
 * In theory a single StyleHelper can be reused among all Nodes which share the
 * same id/styleClass combination (ie: if the same exact set of styles apply
 * to the same nodes, then they should be able to use the same StyleHelper).
 */
public class StyleHelper {

    private static final PlatformLogger LOGGER = com.sun.javafx.Logging.getCSSLogger();

    /**
     * It is very important that the "default" value of a attribute which is not
     * specified as INHERIT defaults to SKIP, meaning, don't set it. Otherwise
     * we end up in a situation where the minute you use CSS, you lose the
     * ability to use translate, scale, rotate, etc on the FX level because CSS
     * fights for those values and sets them to their defaults. An Object is
     * used to represent SKIP because if a String constant were used it might
     * accidentally match some actual string value specified in the CSS.
     */
    private static final CalculatedValue SKIP = new CalculatedValue(new int[0], null, false);

    /**
     * Constructs and returns a StyleHelper for the given sequence of styles.
     * This function is mandatory for creation of a StyleHelper because we want
     * to reduce the memory usage of the helper and so don't want to specify the
     * styles directly on the helper, but also need to process the styles into a
     * lookup table and want that code to be localized to the StyleHelper
     * script file.
     * <p>
     * The pseudoclassStateMask contains the set of pseudoclass states
     * which, if they change on the nodes that use this StyleHelper, will cause
     * that the node and all children be updated on the next CSS pass. For
     * example, if you have a Button, then when the "hover" changes on the
     * button you want the Button to be marked as needing an UPDATE. However, if
     * the "hover" state changes on the root of the scene graph you don't want
     * to update any CSS styles at all (unless of course you had a rule that
     * indicated that a change in the hover state of the root node should affect
     * everything). This is computed in the StyleManager and passed here.
     * @param styles List of the styles. Use an ArrayList for good performance.
     */
    public static StyleHelper create(
            Node node,
            Map<String, List<CascadingStyle>> smap, 
            Map<StyleCacheKey, StyleCacheBucket> containerStyleCache,  
            long pseudoclassStateMask, 
            long uniqueId) {

        // since node doesn't have a stylehelper yet and getKeys
        // needs the stylehelper uniqueId, we get the keys for 
        // the parent chain, but will start counting at 1 to 
        // accomodate for this stylehelper that we are creating.
        final long[] keys = getKeys(node.getParent(), 1);
        keys[0] = uniqueId;
        
        // The List<CacheEntry> should only contain entries for those
        // pseudoclass states that have styles. The StyleHelper's
        // pseudoclassStateMask is a bitmask of those pseudoclasses that
        // appear in the node's StyleHelper's smap. This list of
        // pseudoclass masks is held by the StyleCacheKey. When a node is
        // styled, its pseudoclasses and the pseudoclasses of its parents
        // are gotten. By comparing the actual pseudoclass state to the
        // pseudoclass states that apply, a CacheEntry can be created or
        // fetched using only those pseudoclasses that matter.
        final long[] pclassMasks = new long[keys.length];
        pclassMasks[0] = pseudoclassStateMask;
        int n = 1;
        Node parent = node.getParent();
        while (parent != null) {
            final StyleHelper parentHelper = parent.impl_getStyleHelper();
            pclassMasks[n++] =
                (parentHelper != null) ? parentHelper.pseudoclassStateMask : 0;
            parent = parent.getParent();
        }
        
        // TODO: move initialization of localStyleCache somewhere else.
        //       It should not be hidden down here. Maybe to getCacheEntry
        final StyleCacheBucket localStyleCache = new StyleCacheBucket(pclassMasks);
        
        final StyleCacheKey styleCacheKey = new StyleCacheKey(keys);        
        //
        // Look to see if there is already a cache for this set of helpers
        //
        StyleCacheBucket sharedStyleCache = containerStyleCache.get(styleCacheKey);
        if (sharedStyleCache == null) {
            sharedStyleCache = new StyleCacheBucket(pclassMasks);
            containerStyleCache.put(styleCacheKey, sharedStyleCache);
        }
             
        WritableValue fontProp = null;
        final List<StyleableProperty> props = node.impl_getStyleableProperties();
        final int pMax = props != null ? props.size() : 0;
        for (int p=0; p<pMax; p++) {
            final StyleableProperty prop = props.get(p);
            if ("-fx-font".equals(prop.getProperty())) {                
                fontProp = prop.getWritableValue(node);
                break;
            }
        }
        final StyleHelper helper = 
            new StyleHelper(
                smap, 
                fontProp,
                localStyleCache,
                sharedStyleCache,
                pseudoclassStateMask, 
                uniqueId);

        if (LOGGER.isLoggable(PlatformLogger.FINE)) {
            LOGGER.fine(node + " " + uniqueId);
        }
        return helper;
    }

    private final long key;

    /*
     * A cache to store values from lookup.
     * Consider that there are some number of StyleHelpers and that the
     * StyleHelpers are shared. For a particular node, a style might come
     * from its StyleHelper or the StyleHelper of one of its parents (ignoring
     * Node.style for now). What makes a style unique is the set of StyleHelpers
     * that go into its calculation. So, if node N has StyleHelper A and its
     * parents have StyleHelper B and C, the opacity style (say) for N is going
     * to be unique to the set of StyleHelpers [A B C]. Because StyleHelpers
     * are chosen by the rules they match, and because StyleHelpers are shared,
     * every node that has the set of StyleHelpers [A B C] will match the same
     * rule for opacity (for a given pseudoclass state). Further, the value for
     * opacity (in the given pseudoclass state) will not change for the given
     * set of StyleHelpers. Therefore, rather than trying to cache a calculated
     * value with an individual StyleHelper, the value can be cached with a key
     * that uniquely identifies the set [A B C]. Subsequent lookups for the
     * property do not need to be recalculated even if there are lookups in the
     * value. Incidentally, resolved references will also be unique to a set of
     * StyleHelpers and would only need to be resolved once (for a given
     * pseudoclass state).
     *
     * Node.style puts a slight wrinkle in that the style might come from the
     * Node rather than the cache. This can be handled in a relatively
     * straight-forward manner. If Node.style is not empty or null and it
     * contains the property, then that style should be used if the style
     * compares less than the style that would have been applied. If there is
     * some parent with Node.style that would affect the child Node's style,
     * then the cached value can be used.
     *
     * The key is comprised of this helper's key, plus the
     * keys of all this node's parents' helpers.
     *
     * The values in the cache styles that apply are determined
     * by the node's state and the state of its parents. This unique combination
     * of states reflects the state of the node and its parents at the time the
     * style was first determined. Provided the node and its parents are in the
     * same state, then the styles applied will be the same as what is in the
     * cache.
     *
     * The value could be a Map, but there should not be a large number of
     * entries. Computing the key from the long[], doing the lookup and
     * resolving collisions is probably just as bad, if not worse, than
     * finding the matching set of states by comparing the long[].
     *
     * Since all StyleHelpers are relevant to a Scene, valueCache is
     * created by StyleManager.StylesheetContainer and is passed in.
     * Note that all StyleHelper instances within a given Scene all 
     * share the same valueCache! 
     * 
     * Note that the Map is created as a WeakHashMap in StyleManager. 
     */
    private final Reference<StyleCacheBucket> sharedStyleCacheRef;
    
    private StyleCacheBucket getSharedStyleCache() {
        final StyleCacheBucket styleCache = sharedStyleCacheRef.get();
        return styleCache;
    }
    
    /**
     * A place to store calculated values that cannot be stored in shared cache
     */
    private final StyleCacheBucket localStyleCache;
    
    public static class StyleCacheKey {
        private final long[] keys;
        
        private StyleCacheKey(long[] keys) {
            this.keys = keys;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StyleCacheKey other = (StyleCacheKey) obj;
            if (!Arrays.equals(this.keys, other.keys)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + Arrays.hashCode(this.keys);
            return hash;
        }
        
    }
    
    //
    // The key is created from the index of this StyleHelper
    // and the hash of all the Node's parents.
    // The key is unique to the path to the Node that owns this .
    //    
    private static long[] getKeys(Node node, int count) {
        if (node == null) return new long[count];
        long[] keys = getKeys(node.getParent(), ++count);
        final StyleHelper sh = node.impl_getStyleHelper();        
        keys[count-1] = sh != null ? sh.key : 0;
        return keys;
    }
        
    /**
     * There should only ever be one instance of a StyleCache for a given
     * set of keys. See the createStyleCacheKey method.
     */
    final static class StyleCacheBucket {
        
        // see comments in createStyleCacheKey
        private final long[] pclassMask;
        
        private final List<CacheEntry> entries;

        private StyleCacheBucket(long[] pclassMask) {
            this.pclassMask = pclassMask;
            this.entries = new ArrayList<CacheEntry>();
        }
        
    }

    /**
     * A drop in the StyleCacheBucket.
     */
    static class CacheEntry {

        private final long[] states;
        private final Map<String,CalculatedValue> values;
        private CalculatedValue  font; // for use in converting relative sizes
        
        //
        // if this CacheEntry is an entry in the shared cache, then this
        // member will be null. If this is a local cache, then this member
        // will point to the shared cache for the same states.
        //
        // RT-23079 - weakly reference the shared CacheEntry
        private final Reference<CacheEntry> sharedCacheRef;

        private CacheEntry(long[] states) {
            this(states, null);
        }
        
        private CacheEntry(long[] states, CacheEntry sharedCache) {
            this.states = states;
            this.values = new HashMap<String,CalculatedValue>();
            this.sharedCacheRef = new WeakReference<CacheEntry>(sharedCache);
        }

        private CalculatedValue get(String property) {
            
            CalculatedValue cv = null;
            if (values.isEmpty() == false) {
                cv = values.get(property);
            }
            if (cv == null && sharedCacheRef != null) {
                final CacheEntry ce = sharedCacheRef.get();
                if (ce != null) cv = ce.values.get(property);
                // if referent is null, we should skip the value. 
                else cv = SKIP; 
            }
            return cv;
        }
        
        private void put(String property, CalculatedValue cv) {
            
            // If the origin of the calculated value is inline or user, 
            // then use local cache. 
            // If the origin of the calculated value is not inline or user,
            // then use local cache if the font origin is inline or user and
            // the value was calculated from a relative size unit. 
            final boolean isLocal = 
                (sharedCacheRef == null
                    || cv.origin == Origin.INLINE
                    || cv.origin == Origin.USER)
                || (cv.isRelative &&
                    (font.origin == Origin.INLINE
                    || font.origin == Origin.USER));
            
            if (isLocal) {
                values.put(property, cv);
            } else {
                // if isLocal is false, then sharedCacheRef cannot be null. 
                final CacheEntry ce = sharedCacheRef.get();
                if (ce != null && ce.values.containsKey(property) == false) {
                    // don't override value already in shared cache.
                    ce.values.put(property, cv);
                }
            }
        }

    }

    private CacheEntry getCacheEntry(Node node, long[] states) {

        //
        // Find the entry in local cache that matches the states
        //
       
        //
        // pclassMask is the set of pseudoclasses that appear in the
        // style maps of this set of StyleHelpers. Calculated values are
        // cached by pseudoclass state, but only the pseudoclass states
        // that mater are used in the search.
        //
        // localStyleCache is created with the same pclassMask as 
        // the shared cache - see createStyleCacheKey
        //
        final long[] pclassMask = new long[localStyleCache.pclassMask.length];
        assert (pclassMask.length == states.length) : (pclassMask.length + " != " + states.length); 

        for (int n=0; n<pclassMask.length; n++) {
            pclassMask[n] = localStyleCache.pclassMask[n] & states[n];
        }

        // find the entry in the list with the same pclassMask
        CacheEntry localCacheEntry = null;

        for (int n=0, max=localStyleCache.entries.size(); n<max; n++) {
            final CacheEntry vce = localStyleCache.entries.get(n);
            final long[] vceStates = vce.states;
            if (Arrays.equals(pclassMask, vceStates)) {
                localCacheEntry = vce;
                break;
            }
        }

        if (localCacheEntry != null) {
            return localCacheEntry;
        }

        //
        // Failed to find existing entry.
        // There may already be an existing entry in the shared cache
        // for this pclassMask, so look for that first. 
        //

        //
        // The key is created from this StyleHelper's index
        // and the index of all the StyleHelpers of the Node's parents.
        // The key is unique to this set of StyleHelpers.
        //
        
        final StyleCacheBucket sharedStyleCache = getSharedStyleCache();
        // if sharedStyleCache is null, then this StyleHelper comes from
        // a null referent.
        if (sharedStyleCache == null) return null;
                        
        // find the shared entry in the list with the same pclassMask
        CacheEntry sharedCacheEntry = null;

        // TODO: same block of code as above.
        for (int n=0, max=sharedStyleCache.entries.size(); n<max; n++) {
            final CacheEntry vce = sharedStyleCache.entries.get(n);
            final long[] vceStates = vce.states;
            if (Arrays.equals(pclassMask, vceStates)) {
                sharedCacheEntry = vce;
                break;
            }
        }

        if (sharedCacheEntry == null) {
            sharedCacheEntry = new CacheEntry(pclassMask);
            sharedStyleCache.entries.add(sharedCacheEntry);
        }
        
        localCacheEntry = new CacheEntry(pclassMask, sharedCacheEntry);
        localStyleCache.entries.add(localCacheEntry);

        return localCacheEntry;
    }

    public void clearLocalCache() {
        final List<CacheEntry> entries = localStyleCache.entries;
        final int max = entries.size();
        for (int n=0; n<max; n++) {
            CacheEntry entry = entries.get(n);
            assert (entry.sharedCacheRef != null);
            entry.values.clear();
            entry.font = null;
        }
    }
    /**
     * A map from Property Name (String) => List of Styles. This shortens
     * the lookup time for each property. The smap is shared and is owned
     * by the StyleManager Cache.
     */
    private final Reference<Map<String, List<CascadingStyle>>> smapRef;

    private Map<String, List<CascadingStyle>> getStyleMap() {
        final Map<String, List<CascadingStyle>> smap = smapRef.get();
        return smap;
    }
    
    /**
     * A Set of all the pseudoclass states which, if they change, need to
     * cause the Node to be set to UPDATE its CSS styles on the next pulse.
     * For example, your stylesheet might have:
     * <pre><code>
     * .button { ... }
     * .button:hover { ... }
     * .button *.label { text-fill: black }
     * .button:hover *.label { text-fill: blue }
     * </code></pre>
     * In this case, the first 2 rules apply to the Button itself, but the
     * second two rules apply to the label within a Button. When the hover
     * changes on the Button, however, we must mark the Button as needing
     * an UPDATE. StyleHelper though only contains styles for the first two
     * rules for Button. The pseudoclassStateMask would in this case have
     * only a single bit set for "hover". In this way the StyleHelper associated
     * with the Button would know whether a change to "hover" requires the
     * button and all children to be update. Other pseudoclass state changes
     * that are not in this hash set are ignored.
     */
    private final long pseudoclassStateMask;

    private final WritableValue fontProp;
    /**
     * Creates a new StyleHelper.
     *
     * @param pseudoclassStateMask A set of bits for each pseudoclass
     *        who's change may impact children of this node. This is used as
     *        an important performance optimization so that pseudoclass state
     *        changes which do not affect any children do not get handled.
     */
    private StyleHelper(
            Map<String, List<CascadingStyle>> smap, 
            WritableValue fontProp,
            StyleCacheBucket localStyleCache,
            StyleCacheBucket sharedStyleCache,
            long pseudoclassStateMask, 
            long key) {
        this.key = key;
        this.smapRef = new WeakReference<Map<String, List<CascadingStyle>>>(smap);
        this.sharedStyleCacheRef = new WeakReference<StyleCacheBucket>(sharedStyleCache);
        this.localStyleCache = localStyleCache;
        this.pseudoclassStateMask = pseudoclassStateMask;
        this.fontProp = fontProp;
    }

    /**
     * Invoked by Node to determine whether a change to a specific pseudoclass
     * state should result in the Node being marked dirty with an UPDATE flag.
     */
    public boolean isPseudoclassUsed(String pseudoclass) {
        final long mask = StyleManager.getInstance().getPseudoclassMask(pseudoclass);
        return ((pseudoclassStateMask & mask) == mask);
    }

    /**
     * A convenient place for holding default values for populating the
     * List&lt;Style&gt; that is populated if the Node has a 
     * Map&lt;WritableValue&gt;, List&lt;Style&gt;. 
     * See handleNoStyleFound
     */
    private static final Map<StyleableProperty,Style> stylesFromDefaults = 
            new HashMap<StyleableProperty,Style>();
    
    /**
     * The List to which Declarations fabricated from StyleablePropeerty 
     * defaults are added.
     */
    private static final List<Declaration> declarationsFromDefaults;
    
    /**
     * The Styles in defaultsStyles need to belong to a stylesheet. 
     */
    private static final Stylesheet defaultsStylesheet;
    static {
        final Rule defaultsRule = 
            new Rule(new ArrayList<Selector>(), Collections.EMPTY_LIST);
        defaultsRule.getSelectors().add(Selector.getUniversalSelector());
        declarationsFromDefaults = defaultsRule.getDeclarations();
        defaultsStylesheet = new Stylesheet();
        defaultsStylesheet.setOrigin(null);
        defaultsStylesheet.getRules().add(defaultsRule);
    };
        
    /** 
     * The key is Node.style. The value is the set of property styles
     * from parsing Node.style.
     */
    private static final Map<String,Map<String,CascadingStyle>> authorStylesCache =
        new HashMap<String,Map<String,CascadingStyle>>();

    /**
     * Get the map of property to style from the rules and declarations
     * in the stylesheet. There is no need to do selector matching here since
     * the stylesheet is from parsing Node.style.
     */
    private Map<String,CascadingStyle> getStyles(Stylesheet authorStylesheet) {

        final Map<String,CascadingStyle> authorStyles = new HashMap<String,CascadingStyle>();
        if (authorStylesheet != null) {
            // Order in which the declaration of a CascadingStyle appears (see below)
            int ordinal = 0;
            // For each declaration in the matching rules, create a CascadingStyle object
            final List<Rule> authorStylesheetRules = authorStylesheet.getRules();
            for (int i = 0, imax = authorStylesheetRules.size(); i < imax; i++) {
                final Rule rule = authorStylesheetRules.get(i);
                final List<Declaration> declarations = rule.getDeclarations();
                for (int k = 0, kmax = declarations.size(); k < kmax; k++) {
                    Declaration decl = declarations.get(k);

                    CascadingStyle s = new CascadingStyle(
                        new Style(Selector.getUniversalSelector(), decl),
                        null, // no pseudo classes
                        0, // specificity is zero
                        // ordinal increments at declaration level since
                        // there may be more than one declaration for the
                        // same attribute within a rule or within a stylesheet
                        ordinal++
                    );

                    authorStyles.put(decl.property,s);
                }
            }
        }
        return authorStyles;
    }

    /**
     * Get the mapping of property to style from Node.style for this node.
     */
    private Map<String,CascadingStyle> getInlineStyleMap(Node node) {

        return getInlineStyleMap(node.impl_getStyleable());
    }

    /**
     * Get the mapping of property to style from Node.style for this node.
     */
    private Map<String,CascadingStyle> getInlineStyleMap(Styleable styleable) {
        
        final String inlineStyles = styleable.getStyle();

        // If there are no styles for this property then we can just bail
        if ((inlineStyles == null) || inlineStyles.isEmpty()) return null;

        Map<String,CascadingStyle> styles = authorStylesCache.get(inlineStyles);
        
        if (styles == null) {

            Stylesheet authorStylesheet =
                CSSParser.getInstance().parseInlineStyle(styleable);
            if (authorStylesheet != null) {
                authorStylesheet.setOrigin(Origin.INLINE);
            }
            styles = getStyles(authorStylesheet);
            
            authorStylesCache.put(inlineStyles, styles);
        }

        return styles;
    }

    /**
     * dynamic pseudoclass state for this helper - only valid during a pulse 
     * Set in setPseudoClassStatesForTransition which is called upon entering 
     * transitionToState. 
     */
    private long[] pseudoClassStates;

    // cache pseudoclass state. cleared on each pulse.
    final static Map<Node,Long> pseudoclassMasksByNode = new HashMap<Node,Long>();

    // get pseudoclass state for the node (set at StyleHelper creation)
    long getPseudoClassState() {
        return pseudoClassStates != null && pseudoClassStates.length > 0 ? pseudoClassStates[0] : 0;
    }
    
    private long[] getPseudoClassStates() {
        return pseudoClassStates;
    }
    
    /* 
     * Called from transitionToState to set the pseudoClassStates member. This method
     * gets the pseudoClassStates from the first non-null StyleHelper of a Parent and
     * fills in the gaps with zeros (no state) for those parents that have no StyleHelper.
     * This works because styles are applied from the top-down. So the StyleHelper of
     * the parent will already have pseudoClassStates set to the correct value.
     * 
     * Note Well: The array runs from leaf to root. That is 
     * pseudoClassStates[0] is the states for node and 
     * pseudoClassStates[1..(pseudoClassStates.length-1)] is the states for the 
     * node's parents. This is how the code was written when the pseudoClassState
     * was looked up recursively, so we'll leave it that way for now.
     */ 
    private static void setPseudoClassStatesForTransition(Node node, StyleHelper styleHelper) {
    
        
        long[] parentStates = null;
        // count up parents that don't have a stylehelper until we get to 
        // the first parent with a stylehelper. The state for the missing
        // stylehelpers will be zero. 
        int nMissing = 0; 
        
        Parent parent = node.getParent();
        while (parent != null && parentStates == null) {
            final StyleHelper helper = parent.impl_getStyleHelper();
            if (helper != null) {
                parentStates = helper.pseudoClassStates; 
            } else {
                parent = parent.getParent();
                nMissing += 1;
            }
        }
        
        final long nodeState = node.impl_getPseudoClassState();
        final int nParents = parentStates != null ? parentStates.length + nMissing : nMissing;
        // do we need to init pseudoClassStates or can we reuse?
        if (styleHelper.pseudoClassStates == null || styleHelper.pseudoClassStates.length < nParents) {
            styleHelper.pseudoClassStates = new long[nParents + 1];            
        }
        Arrays.fill(styleHelper.pseudoClassStates, 0);
        styleHelper.pseudoClassStates[0] = nodeState;
        
        if (parentStates != null) {
            System.arraycopy(parentStates, 0, styleHelper.pseudoClassStates, nMissing+1, parentStates.length);
        }
        
    }
    
    /* 
     * The lookup function return an Object but also
     * needs to return whether or not the value is cacheable.
     * 
     * isShared is true if the value is not specific to a node. 
     * 
     */
    private static class CalculatedValue {
        final Object value;
        final Origin origin;
        final boolean isRelative;

        CalculatedValue(Object value, Origin origin, boolean isRelative) {
            
            this.value = value;            
            this.origin = origin;
            this.isRelative = isRelative;
            
        }
        
    }
        
    /**
     * Called by the Node whenever it has transitioned from one set of
     * pseudoclass states to another. This function will then lookup the
     * new values for each of the styleable variables on the Node, and
     * then either set the value directly or start an animation based on
     * how things are specified in the CSS file. Currently animation support
     * is disabled until the new parser comes online with support for
     * animations and that support is detectable via the API.
     */
    public void transitionToState(Node node) {

        if (smapRef.get() == null) return;
        
        //
        // The ValueCacheEntry to choose depends on this Node's state and
        // the state of its parents. Without the parent state, the fact that
        // this node in this state matched foo:blah bar { } is lost.
        //
        setPseudoClassStatesForTransition(node, this);

        // allstates[0] is this node's state
        long states = pseudoClassStates[0];
        //
        // inlineStyles is this node's Node.style. This is passed along and is
        // used in getStyle. Importance being the same, an author style will
        // trump a user style and a user style will trump a user_agent style.
        //
        final Map<String,CascadingStyle> inlineStyles = this.getInlineStyleMap(node);

        //
        // Styles that need lookup can be cached provided none of the styles
        // are from Node.style.
        //
                
        final CacheEntry cacheEntry = getCacheEntry(node, pseudoClassStates);
        if (cacheEntry == null 
            || (cacheEntry.sharedCacheRef != null 
                && cacheEntry.sharedCacheRef.get() == null)) {
            // If cacheEntry is null, then the StyleManager Cache from which
            // this StyleHelper was created has been blown away and this
            // StyleHelper is no good. If this is the case, we need to tell
            // this node to reapply CSS
            //
            // RT-23079 - if this is local cache, then the sharedCacheRef 
            // will not be null. If sharedCacheRef is not null, but its 
            // referent is null, then the styleMap in the StylesheetContainer
            // has been cleared and we're working with a cache that is no good.
            // 
            node.impl_reapplyCSS();
            return;
        }
        
        //
        // if this node has a style map, then we'll populate it.
        // 
        final Map<WritableValue, List<Style>> styleMap = node.impl_getStyleMap();

        //
        // If someone is watching the styles, then we have to take the slow path.
        //
        boolean fastpath = styleMap == null && inlineStyles == null;
        
        if (cacheEntry.font == null) {
            final CalculatedValue font = 
                getFontForUseInConvertingRelativeSize(node, cacheEntry, states, inlineStyles);
            
            cacheEntry.font = font;
            assert(cacheEntry.font != null);
            //
            // If the cacheEntry's font is null, then this is a new cache entry.
            // If the font is set by the user or there are inline styles, 
            // take the slow path to ensure that the local values are updated. 
            // The next time transitionToState is called, we'll take the fast
            // path since all the values will have been calculated for this entry.
            //
            fastpath = 
                fastpath && 
                cacheEntry.font.origin != Origin.USER && 
                cacheEntry.font.origin != Origin.INLINE && 
                inlineStyles == null;
        } 
        
        final List<StyleableProperty> styleables = StyleableProperty.getStyleables(node);
        
        // Used in the for loop below, and a convenient place to stop when debugging.
        final int max = styleables.size();

        // RT-20643
        CssError.setCurrentScene(node.getScene());
        
        // For each property that is settable, we need to do a lookup and
        // transition to that value.
        for(int n=0; n<max; n++) {

            final StyleableProperty styleable = styleables.get(n);
            
            if (styleMap != null) {
                WritableValue writable = styleable.getWritableValue(node);
                if (writable != null && styleMap.containsKey(writable)) {
                    styleMap.remove(writable);
                }
            }
            

            // Skip the lookup if we know there isn't a chance for this property
            // to be set (usually due to a "bind").
            if (!styleable.isSettable(node)) continue;

            final String property = styleable.getProperty();
            
            // Create a List to hold the Styles if the node has 
            // a Map<WritableValue, List<Style>>
            final List<Style> styleList = (styleMap != null) 
                    ? new ArrayList<Style>() 
                    : null;

            //
            // if there are userStyles, then we cannot (at this time) use
            // the cached value since the userStyles may contain a mapping for
            // the lookup. At some point, I'd like to compare the userStyles
            // with the lookups in the cached value. If userStyles doesn't
            // have mappings for lookups, then the cached value could be used.
            //
            // cacheEntry may be null if the calculation of the style cache
            // key would overflow long. In this case, the style simply cannot
            // be cached.
            //
            // If the node has a style map, then we'll take the slow path. 
            // This is so we don't have to cache the List<Style> that went 
            // into figuring out the calculated value. The style map incurs a 
            // performance penalty, but that is preferable to the memory hit
            // if we had to cache the List<Style> - especially since asking
            // for the List<Style> will be the exception rather than the rule.
            //
//            final boolean fastpath = styleList == null;
            CalculatedValue calculatedValue = null;
            if (fastpath) {

                calculatedValue = cacheEntry.get(property);

            }

            if (calculatedValue == null) {

                boolean isUserSet = isUserSetProperty(node, styleable);            

                calculatedValue = lookup(node, styleable, isUserSet, states, 
                        inlineStyles, node, cacheEntry, styleList);

                if (fastpath) {
                    // if userStyles is null and calculatedValue was null,
                    // then the calculatedValue didn't come from the cache
                    cacheEntry.put(property, calculatedValue);
                }

            }

            // RT-10522:
            // If the user set the property and there is a style and
            // the style came from the user agent stylesheet, then
            // skip the value. A style from a user agent stylesheet should
            // not override the user set style.
            //
            // RT-21894: the origin might be null if the calculatedValue 
            // comes from reverting back to the initial value. In this case,
            // make sure the initial value doesn't overwrite the user set value.
            // Also moved this condition from the fastpath block to here since
            // the check needs to be done on any calculated value, not just
            // calculatedValues from cache
            //
            if (calculatedValue == SKIP
                || (   calculatedValue != null
                    && (   calculatedValue.origin == Origin.USER_AGENT
                        || calculatedValue.origin == null) 
                    && isUserSetProperty(node, styleable)
                    )
                ) {
                continue;
            }
            
                final Object value = calculatedValue.value;
                if (LOGGER.isLoggable(PlatformLogger.FINER)) {
                    LOGGER.finer("call " + node + ".impl_cssSet(" +
                                    property + ", " + value + ")");
                }

                try {
                    styleable.set(node, value, calculatedValue.origin);

                    if (styleMap != null) {
                        WritableValue writable = styleable.getWritableValue(node);                            
                        styleMap.put(writable, styleList);
                    }
                    
                } catch (Exception e) {
                    List<CssError> errors = null;
                    if ((errors = StyleManager.getInstance().getErrors()) != null) {
                        final String msg = String.format("Failed to set css [%s] due to %s\n", styleable, e.getMessage());
                        final CssError error = new CssError.PropertySetError(styleable, node, msg);
                        errors.add(error);
                    }
                    // TODO: use logger here
                    PlatformLogger logger = Logging.getCSSLogger();
                    if (logger.isLoggable(PlatformLogger.WARNING)) {
                        logger.warning(String.format("Failed to set css [%s]\n", styleable), e);
                    }
                }

            }
        
        // RT-20643
        CssError.setCurrentScene(null);

        // If the list weren't empty, we'd worry about animations at this
        // point. TODO need to implement animation trickery here
    }

    /**
     * Called by the Scene whenever it has transitioned from one set of
     * pseudoclass states to another. This function will then lookup the
     * new values for each of the styleable variables on the Scene, and
     * then either set the value directly or start an animation based on
     * how things are specified in the CSS file. Currently animation support
     * is disabled until the new parser comes online with support for
     * animations and that support is detectable via the API.
     */
    public void transitionToState(Scene scene, List<String> states) {
        // TODO the majority of this function is exactly the same as the
        // Node variant. We also need to implement the lookup for the
        // scene, but don't have to worry about inherit. If only Scene were
        // an actual Parent node... might be a good idea except that its
        // scene variable then makes no sense. I guess it would be null??
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Gets the CSS CascadingStyle for the property of this node in these pseudoclass
     * states. A null style may be returned if there is no style information
     * for this combination of input parameters.
     *
     * @param node 
     * @param property
     * @param states
     * @return
     */
    private CascadingStyle getStyle(Node node, String property, long states, Map<String,CascadingStyle> userStyles){

        assert node != null && property != null: String.valueOf(node) + ", " + String.valueOf(property);

        final CascadingStyle userStyle = (userStyles != null) ? userStyles.get(property) : null;

        // Get all of the Styles which may apply to this particular property
        final Map<String, List<CascadingStyle>> smap = getStyleMap();
        if (smap == null) return null;

        final List<CascadingStyle> styles = smap.get(property);

        // If there are no styles for this property then we can just bail
        if ((styles == null || styles.isEmpty())&& (userStyle == null)) return null;

        // Go looking for the style. We do this by visiting each CascadingStyle in
        // order finding the first that matches the current node & set of
        // pseudoclass states. We use an iteration style that avoids creating
        // garbage iterators (and wish javac did it for us...)
        CascadingStyle style = null;
        final int max = (styles == null) ? 0 : styles.size();
        for (int i=0; i<max; i++) {
            final CascadingStyle s = styles.get(i);
            final Selector sel = s == null ? null : s.getSelector();
            if (sel == null) continue; // bail if the selector is null.
            if (sel.stateMatches(node, states)) {
                style = s;
                break;
            }
        }
        
        if (userStyle != null) {

            if (style == null || userStyle.compareTo(style) < 0) {
                style = userStyle;
            }

        }
        return style;
    }

    /**
     * The main workhorse of this class, the lookup method walks up the CSS
     * style tree looking for the style information for the Node, the
     * property associated with the given styleable, in these states for this font.
     *
     * @param node
     * @param styleable
     * @param states
     * @param font
     * @return
     */
    private CalculatedValue lookup(Node node, StyleableProperty styleable, 
            boolean isUserSet, long states,
            Map<String,CascadingStyle> userStyles, Node originatingNode, 
            CacheEntry cacheEntry, List<Style> styleList) {

        if (styleable.getConverter() == FontConverter.getInstance()) {
        
            return lookupFont(node, styleable, isUserSet, 
                    originatingNode, cacheEntry, styleList);
        }
        
        final String property = styleable.getProperty();

        // Get the CascadingStyle which may apply to this particular property
        CascadingStyle style = getStyle(node, property, states, userStyles);

        // If no style was found and there are no sub styleables, then there
        // are no matching styles for this property. We will then either SKIP
        // or we will INHERIT. We will inspect the default value for the styleable,
        // and if it is INHERIT then we will inherit otherwise we just skip it.
        final List<StyleableProperty> subProperties = styleable.getSubProperties();
        final int numSubProperties = (subProperties != null) ? subProperties.size() : 0;
        final StyleConverter keyType = styleable.getConverter();
        if (style == null) {
            

            if (numSubProperties == 0) {
                
                return handleNoStyleFound(node, styleable, isUserSet, userStyles, 
                        originatingNode, cacheEntry, styleList);
                
            } else {

                // If style is null then it means we didn't successfully find the
                // property we were looking for. However, there might be sub styleables,
                // in which case we should perform a lookup for them. For example,
                // there might not be a style for "font", but there might be one
                // for "font-size" or "font-weight". So if the style is null, then
                // we need to check with the sub-styleables.

                // Build up a list of all SubProperties which have a constituent part.
                // I default the array to be the size of the number of total
                // sub styleables to avoid having the array grow.
                Map<StyleableProperty,Object> subs = null;
                Origin origin = null;
                
                boolean isRelative = false;
                
                for (int i=0; i<numSubProperties; i++) {
                    StyleableProperty subkey = subProperties.get(i);
                    CalculatedValue constituent = 
                        lookup(node, subkey, isUserSet, states, userStyles, 
                            originatingNode, cacheEntry, styleList);
                    if (constituent != SKIP) {
                        if (subs == null) {
                            subs = new HashMap<StyleableProperty,Object>();
                        }
                        subs.put(subkey, constituent.value);

                        // origin of this style is the most specific
                        if ((origin != null && constituent.origin != null)
                                ? origin.compareTo(constituent.origin) < 0
                                : constituent.origin != null) {
                            origin = constituent.origin;
                        }
                        
                        // if the constiuent uses relative sizes, then 
                        // isRelative is true;
                        isRelative = isRelative || constituent.isRelative;
                            
                    }
                }

                // If there are no subkeys which apply...
                if (subs == null || subs.isEmpty()) {
                    return handleNoStyleFound(node, styleable, isUserSet, userStyles, 
                            originatingNode, cacheEntry, styleList);
                }

                try {
                    final Object ret = keyType.convert(subs);
                    return new CalculatedValue(ret, origin, isRelative);
                } catch (ClassCastException cce) {
                    final String msg = formatExceptionMessage(node, styleable, style.getStyle(), cce);
                    List<CssError> errors = null;
                    if ((errors = StyleManager.getInstance().getErrors()) != null) {
                        final CssError error = new CssError.PropertySetError(styleable, node, msg);
                        errors.add(error);
                    }
                    if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                        LOGGER.warning("caught: ", cce);
                        LOGGER.warning("styleable = " + styleable);
                        LOGGER.warning("node = " + node.toString());
                    }
                    return SKIP;
                }
            }                
            
        } else { // style != null

            // RT-10522:
            // If the user set the property and there is a style and
            // the style came from the user agent stylesheet, then
            // skip the value. A style from a user agent stylesheet should
            // not override the user set style.
            if (isUserSet && style.getOrigin() == Origin.USER_AGENT) {
                return SKIP;
            }

            // If there was a style found, then we want to check whether the
            // value was "inherit". If so, then we will simply inherit.
            final ParsedValue cssValue = style.getParsedValue();
            if (cssValue != null && "inherit".equals(cssValue.getValue())) {
                if (styleList != null) styleList.add(style.getStyle());
                return inherit(node, styleable, userStyles, 
                        originatingNode, cacheEntry, styleList);
            }
        }

//        System.out.println("lookup " + property +
//                ", selector = \'" + style.selector.toString() + "\'" +
//                ", node = " + node.toString());

        if (styleList != null) { 
            styleList.add(style.getStyle());
        }
        
        return calculateValue(style, node, styleable, states, userStyles, 
                    originatingNode, cacheEntry, styleList);
    }
    
    /**
     * Called when there is no style found.
     */
    private CalculatedValue handleNoStyleFound(Node node, StyleableProperty styleable,
            boolean isUserSet, Map<String,CascadingStyle> userStyles, 
            Node originatingNode, CacheEntry cacheEntry, List<Style> styleList) {

        if (styleable.isInherits()) {


            // RT-16308: if there is no matching style and the user set 
            // the property, do not look for inherited styles.
            if (isUserSet) {

                    return SKIP;
                    
            }

            CalculatedValue cv =
                inherit(node, styleable, userStyles, 
                    originatingNode, cacheEntry, styleList);

            return cv;

        } else if (isUserSet) {

            // Not inherited. There is no style but we don't want to
            // set the default value if the user set the property
            return SKIP;

        } else {
            
            final Map<String, List<CascadingStyle>> smap = getStyleMap();
            if (smap == null) return SKIP;
            
            if (smap.containsKey(styleable.getProperty())) {

                // If there is a style in the stylemap but it just doen't apply,
                // then it may have been set and it needs to be reset to its
                // default value. For example, if there is a style for the hover
                // pseudoclass state, but no style for the default state.
                Object initialValue = styleable.getInitialValue(node);

                if (styleList != null) {

                    Style initialStyle = stylesFromDefaults.get(styleable);
                    if (initialStyle != null) {
                        if (!declarationsFromDefaults.contains(initialStyle.getDeclaration())) {
                            declarationsFromDefaults.add(initialStyle.getDeclaration());
                        }
                    } else {
                        initialStyle = new Style( 
                            Selector.getUniversalSelector(),
                            new Declaration(styleable.getProperty(), 
                                        new ParsedValue(initialValue, null), false));
                        stylesFromDefaults.put(styleable, initialStyle);
                        declarationsFromDefaults.add(initialStyle.getDeclaration());
                    }

                    styleList.add(initialStyle);
                }

                return new CalculatedValue(initialValue, null, false);

            } else {

                return SKIP;

            }
        }
    }
    /**
     * Called when we must inherit a value from a parent node in the scenegraph.
     */
    private CalculatedValue inherit(Node node, StyleableProperty styleable,
            Map<String,CascadingStyle> userStyles, 
            Node originatingNode, CacheEntry cacheEntry, List<Style> styleList) {
        
        // Locate the first parentStyleHelper in the hierarchy
        Node parent = node.getParent();        
        StyleHelper parentStyleHelper = parent == null ? null : parent.impl_getStyleHelper();
        while (parent != null && parentStyleHelper == null) {
            parent = parent.getParent();
            if (parent != null) {
                parentStyleHelper = parent.impl_getStyleHelper();
            }
        }

        if (parent == null) {
            return SKIP;
        }
        return parentStyleHelper.lookup(parent, styleable, false,
                parentStyleHelper.getPseudoClassState(),
                getInlineStyleMap(parent), originatingNode, cacheEntry, styleList);
    }


    /**
     * Find the property among the styles that pertain to the Node
     */
    private CascadingStyle resolveRef(Node node, String property, long states,
            Map<String,CascadingStyle> userStyles) {

        final CascadingStyle style = getStyle(node, property, states, userStyles);
        if (style != null) {
            return style;
        } else {
            // if style is null, it may be because there isn't a style for this
            // node in this state, or we may need to look up the parent chain
            if (states > 0) {
                // if states > 0, then we need to check this node again,
                // but without any states.
                return resolveRef(node,property,0,userStyles);
            } else {
                // TODO: This block was copied from inherit. Both should use same code somehow.
                Node parent = node.getParent();
                StyleHelper parentStyleHelper = parent == null ? null : parent.impl_getStyleHelper();
                while (parent != null && parentStyleHelper == null) {
                    parent = parent.getParent();
                    if (parent != null) {
                        parentStyleHelper = parent.impl_getStyleHelper();
                    }
                }

                if (parent == null || parentStyleHelper == null) {
                    return null;
                }
                return parentStyleHelper.resolveRef(parent, property,
                        parentStyleHelper.getPseudoClassState(),
                        getInlineStyleMap(parent));
            }
        }
    }

    // to resolve a lookup, we just need to find the parsed value.
    private ParsedValue resolveLookups(
            Node node, 
            ParsedValue value, 
            long states,
            Map<String,CascadingStyle> userStyles, 
            List<Style> styleList) {
        
        
        //
        // either the value itself is a lookup, or the value contain a lookup
        //
        if (value.isLookup()) {

            // The value we're looking for should be a Paint, one of the
            // containers for linear, radial or ladder, or a derived color.
            final Object val = value.getValue();
            if (val instanceof String) {

                final String sval = (String)val;
                
                CascadingStyle resolved = 
                    resolveRef(node, sval, states, userStyles);

                if (resolved != null) {
                    
                    if (styleList != null) {
                        final Style style = resolved.getStyle();
                        if (style != null && !styleList.contains(style)) {
                            styleList.add(style);
                        }
                    }
                    
                    // the resolved value may itself need to be resolved.
                    // For example, if the value "color" resolves to "base",
                    // then "base" will need to be resolved as well.
                    return resolveLookups(node, resolved.getParsedValue(), states, userStyles, styleList);
                }
            }
        }

        // If the value doesn't contain any values that need lookup, then bail
        if (!value.isContainsLookups()) {
            return value;
        }

        final Object val = value.getValue();
        if (val instanceof ParsedValue[][]) {
        // If ParsedValue is a layered sequence of values, resolve the lookups for each.
            final ParsedValue[][] layers = (ParsedValue[][])val;
            for (int l=0; l<layers.length; l++) {
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                    layers[l][ll].resolved = 
                        resolveLookups(node, layers[l][ll], states, userStyles, styleList);
                }
            }

        } else if (val instanceof ParsedValue[]) {
        // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layer = (ParsedValue[])val;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                layer[l].resolved =
                    resolveLookups(node, layer[l], states, userStyles, styleList);
            }
        }

        return value;

    }
    
    private String getUnresolvedLookup(ParsedValue resolved) {

        Object value = resolved.getValue();

        if (resolved.isLookup() && value instanceof String) {
            return (String)value;
        } 

        if (value instanceof ParsedValue[][]) {
            final ParsedValue[][] layers = (ParsedValue[][])value;
            for (int l=0; l<layers.length; l++) {
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                    String unresolvedLookup = getUnresolvedLookup(layers[l][ll]);
                    if (unresolvedLookup != null) return unresolvedLookup;
                }
            }

        } else if (value instanceof ParsedValue[]) {
        // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layer = (ParsedValue[])value;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                String unresolvedLookup = getUnresolvedLookup(layer[l]);
                if (unresolvedLookup != null) return unresolvedLookup;
            }
        }        
        
        return null;
    }
    
    private String formatUnresolvedLookupMessage(Node node, StyleableProperty styleable, Style style, ParsedValue resolved) {
        
        // find value that could not be looked up
        String missingLookup = resolved != null ? getUnresolvedLookup(resolved) : null;
        if (missingLookup == null) missingLookup = "a lookup value";
        
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Could not resolve '")
            .append(missingLookup)
            .append("'")
            .append(" while resolving lookups for '")
            .append(styleable.getProperty())
            .append("'");
        
        final Rule rule = style != null ? style.getDeclaration().getRule(): null;
        final Stylesheet stylesheet = rule != null ? rule.getStylesheet() : null;
        final java.net.URL url = stylesheet != null ? stylesheet.getUrl() : null;
        if (url != null) {
            sbuf.append(" from rule '")
                .append(style.getSelector())
                .append("' in stylesheet ").append(url.toExternalForm());
        } else if (stylesheet != null && Origin.INLINE == stylesheet.getOrigin()) {
            sbuf.append(" from inline style on " )
                .append(node.toString());            
        }
        
        return sbuf.toString();
    }

    private String formatExceptionMessage(Node node, StyleableProperty styleable, Style style, Exception e) {
        
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Caught ")
            .append(e.toString())
            .append("'")
            .append(" while calculating value for '")
            .append(styleable.getProperty())
            .append("'");
        
        final Rule rule = style != null ? style.getDeclaration().getRule(): null;
        final Stylesheet stylesheet = rule != null ? rule.getStylesheet() : null;
        final java.net.URL url = stylesheet != null ? stylesheet.getUrl() : null;
        if (url != null) {
            sbuf.append(" from rule '")
                .append(style.getSelector())
                .append("' in stylesheet ").append(url.toExternalForm());
        } else if (stylesheet != null && Origin.INLINE == stylesheet.getOrigin()) {
            sbuf.append(" from inline style on " )
                .append(node.toString());            
        }
        
        return sbuf.toString();
    }
    
    private CalculatedValue calculateValue(
            final CascadingStyle style, 
            final Node node, 
            final StyleableProperty styleable, 
            final long states,
            final Map<String,CascadingStyle> inlineStyles, 
            final Node originatingNode, 
            final CacheEntry cacheEntry, 
            final List<Style> styleList) {

        final ParsedValue cssValue = style.getParsedValue();
        if (cssValue != null && !("null").equals(cssValue.getValue())) {
        
            final ParsedValue resolved = 
                resolveLookups(node, cssValue, states, inlineStyles, styleList);
            
            try {
                // The computed value
                Object val = null;
                CalculatedValue fontValue = null;

                //
                // Avoid using a font calculated from a relative size
                // to calculate a font with a relative size. 
                // For example:
                // Assume the default font size is 13 and we have a style with
                // -fx-font-size: 1.5em, then the cacheEntry font value will 
                // have a size of 13*1.5=19.5. 
                // Now, when converting that same font size again in response
                // to looking up a value for -fx-font, we do not want to use
                // 19.5 as the font for relative size conversion since this will
                // yield a font 19.5*1.5=29.25 when really what we want is
                // a font size of 19.5.
                // In this situation, then, we use the font from the parent's
                // cache entry.
                final String property = styleable.getProperty();
                if (resolved.isNeedsFont() &&
                    (cacheEntry.font == null || cacheEntry.font.isRelative) &&
                    ("-fx-font".equals(property) ||
                     "-fx-font-size".equals(property)))  {
                    
                    Node parent = node;
                    CalculatedValue cachedFont = cacheEntry.font;
                    while(parent != null) {
                        
                        final CacheEntry parentCacheEntry = 
                            getParentCacheEntry(parent);
                        
                        if (parentCacheEntry != null) {
                            fontValue = parentCacheEntry.font;
                        
                            if (fontValue != null && fontValue.isRelative) {

                                // If cacheEntry.font is null, then we're here by 
                                // way of getFontForUseInConvertingRelativeSize
                                // If  the fontValue is not relative, then the
                                // cacheEntry.font needs to be calculated. If
                                // fontValue is relative, then we can use it as
                                // is - this is a hack for Control and SkinBase
                                // which share the same styles (hence the check
                                // for parent == node).
                                // TBD - should check that they are the same styles
    //                                if (parent == node && childCacheEntry.font == null) {
    //                                    return fontValue;
    //                                } 

                                if (cachedFont != null) {
                                    final Font ceFont = (Font)cachedFont.value;
                                    final Font fvFont = (Font)fontValue.value;
                                    if (ceFont.getSize() != fvFont.getSize()) {
                                        // if the font sizes don't match, then
                                        // the fonts came from distinct styles,
                                        // otherwise, we need another level of
                                        // indirection
                                        break;
                                    }
                                } 

                                // cachedFont is null or the sizes match
                                // (implies the fonts came from the same style)
                                cachedFont = fontValue;

                            } else if (fontValue != null) {
                                // fontValue.isRelative == false
                                break;
                            }
                        }
                        // try again
                        parent = parent.getParent();
                    }
                }

                // did we get a fontValue from the preceding block (from the hack)?
                // if not, get it from our cachEntry or choose the default.cd
                if (fontValue == null && cacheEntry != null) {
                    fontValue = cacheEntry != null ? cacheEntry.font : null;
                }
                final Font font = (fontValue != null) ? (Font)fontValue.value : Font.getDefault();


                if (resolved.getConverter() != null)
                    val = resolved.convert(font);
                else
                    val = styleable.getConverter().convert(resolved, font);

                final Origin origin = style.getOrigin();
                return new CalculatedValue(val, origin, resolved.isNeedsFont());
                
            } catch (ClassCastException cce) {
                final String msg = formatUnresolvedLookupMessage(node, styleable, style.getStyle(),resolved);
                List<CssError> errors = null;
                if ((errors = StyleManager.getInstance().getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(styleable, node, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning(msg);
                    LOGGER.fine("node = " + node.toString());
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("styles = " + styleable.getMatchingStyles(node));
                }
                return SKIP;
            } catch (IllegalArgumentException iae) {
                final String msg = formatExceptionMessage(node, styleable, style.getStyle(), iae);
                List<CssError> errors = null;
                if ((errors = StyleManager.getInstance().getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(styleable, node, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("caught: ", iae);
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("node = " + node.toString());
                }
                return SKIP;
            } catch (NullPointerException npe) {
                final String msg = formatExceptionMessage(node, styleable, style.getStyle(), npe);
                List<CssError> errors = null;
                if ((errors = StyleManager.getInstance().getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(styleable, node, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("caught: ", npe);
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("node = " + node.toString());
                }
                return SKIP;
            } finally {
                resolved.nullResolved();
            }
                
        }
        // either cssValue was null or cssValue's value was "null"
        return new CalculatedValue(null, style.getOrigin(), false);
           
    }
    
    /** return true if the origin of the property is USER */
    private boolean isUserSetProperty(Node node, StyleableProperty styleable) {
        WritableValue writable = node != null ? styleable.getWritableValue(node) : null;
        // writable could be null if this is a sub-property
        Origin origin = writable != null ? ((Property)writable).getOrigin() : null;
        return (origin == Origin.USER);    
    }    
            
    private static final StyleableProperty dummyFontProperty =
            new StyleableProperty.FONT<Node>("-fx-font", Font.getDefault()) {

        @Override
        public boolean isSettable(Node node) {
            return true;
        }

        @Override
        public WritableValue<Font> getWritableValue(Node node) {
            return null;
        }
    };
   
    private CacheEntry getParentCacheEntry(Node child) {

        if (child == null) return null;
        
        Node parent = child;
        StyleHelper parentHelper = null;
        do {
            parent = parent.getParent();
        } while (parent != null &&
                    (parentHelper = parent.impl_getStyleHelper()) == null);

        CacheEntry parentCacheEntry = null;
        if (parent != null && parentHelper != null) {
            final long[] pstates = parentHelper.getPseudoClassStates();
            parentCacheEntry = parentHelper.getCacheEntry(parent, pstates);
//            assert(parentCacheEntry.font != null);
        }
        return parentCacheEntry;
    }
   
    private CalculatedValue getFontForUseInConvertingRelativeSize(
             final Node node,
             final CacheEntry cacheEntry,
            long pseudoclassState,
            Map<String,CascadingStyle> inlineStyles)
     {
        
                            
        Origin origin = null;
        Font foundFont = null;
        CalculatedValue foundSize = null;
        CalculatedValue foundShorhand = null;
        boolean isRelative = false;
         
        // RT-20145 - if looking for font size and the node has a font, 
        // use the font property's value if it was set by the user and
        // there is not an inline or author style.
        if (fontProp != null) {
 
            Origin fpOrigin = StyleableProperty.getOrigin(fontProp);
            if (fpOrigin == Origin.USER) {                
                origin = fpOrigin;
                foundFont = (Font)fontProp.getValue();
            }
        } 
 
        final CascadingStyle fontShorthand =
            getStyle(node, "-fx-font", pseudoclassState, inlineStyles);

        if (fontShorthand != null) {
            
            final CalculatedValue cv = 
                calculateValue(fontShorthand, node, dummyFontProperty, 
                    pseudoclassState, inlineStyles, 
                    node, cacheEntry, null);
            
            // If we don't have an existing font, or if the origin of the existing font is
            // less than that of the shorthand, then take the shorthand.
            // If the origins compare equals, then take the shorthand since 
            // the fontProp value will not have been updated yet. Man, this
            // drove me nuts!
            if (origin == null || origin.compareTo(cv.origin) <= 0) {
                               
                // cv could be SKIP
                if (cv.value instanceof Font) {
                    origin = cv.origin;
                    foundShorhand = cv;
                    foundFont = null;
                }

            }  
         }
 
        final boolean isUserSet = 
                origin == Origin.USER || origin == Origin.INLINE;
         
        // now look for -fx-size, but don't look past the current node (hence 
        // distance == 0)
        CascadingStyle fontSize = lookupFontSubPropertyStyle(node, "-fx-font-size",
                isUserSet, fontShorthand, 0);
        
        if (fontSize != null &&
                (fontShorthand == null || fontShorthand.compareTo(fontSize) < 0)) {

            final CalculatedValue cv = 
                calculateValue(fontSize, node, dummyFontProperty, pseudoclassState, inlineStyles, 
                    node, cacheEntry, null);
            
            if (cv.value instanceof Double) {
                if (origin == null || origin.compareTo(cv.origin) <= 0) {  
                    origin = cv.origin;
                    foundSize = cv;
                    foundShorhand = null;
                    foundFont = null;
                }
             }

         }
         
         
        if (foundShorhand != null) {
            
            return foundShorhand;
            
        } else if (foundSize != null) {
            
            Font font = Font.font("system", ((Double)foundSize.value).doubleValue());
            return new CalculatedValue(font, foundSize.origin, foundSize.isRelative);
            
        } else if (foundFont != null) {
         
            // Did this found font come from a style? 
            // If so, then the font used to convert it from a relative size
            // (if conversion was needed) comes from the parent.
            if (origin != null && origin != Origin.USER) {
                
                CacheEntry parentCacheEntry = getParentCacheEntry(node);

                if (parentCacheEntry != null && parentCacheEntry.font != null) {
                    isRelative = parentCacheEntry.font.isRelative;
                } 
            }
            return new CalculatedValue(foundFont, origin, isRelative);
            
        } else {
            
            // no font, font-size or fontProp. 
            // inherit by taking the parent's cache entry font.
            CacheEntry parentCacheEntry = getParentCacheEntry(node);
            
            if (parentCacheEntry != null && parentCacheEntry.font != null) {
                return parentCacheEntry.font;                    
            } 
        } 
        // last ditch effort -  take the default font.
        return new CalculatedValue(Font.getDefault(), null, false);
     }

    
    private CascadingStyle lookupFontSubPropertyStyle(final Node node, 
            final String subProperty, final boolean isUserSet,
            final CascadingStyle csShorthand, 
            final int distance) {
    
        Node parent = node;
        StyleHelper helper = this;
        int nlooks = 0;
        CascadingStyle cascadingStyle = null;
        
        while (parent != null && nlooks <= distance) {
            
            final long states = helper.getPseudoClassState();
            final Map<String,CascadingStyle> inlineStyles = helper.getInlineStyleMap(parent);
            
            cascadingStyle = 
                helper.getStyle(parent, subProperty, states, inlineStyles);
            
            if (isUserSet) {
                //
                // Don't look past this node if the user set the property.
                //
                if (cascadingStyle != null) {
                    
                    final Origin origin = cascadingStyle.getOrigin();

                    // if the user set the property and the origin is the
                    // user agent stylesheet, then we can't use the style
                    // since ua styles shouldn't override user set values
                    if (origin == Origin.USER_AGENT) {
                        cascadingStyle = null;
                    }
                }    
                
                break;
                
            } else if (cascadingStyle != null) {
                // Take the first non-null  
                break;
                
            } else {
                
                // 
                // haven't found it yet, keep looking up the parent chain.
                //
                do {
                    parent = parent.getParent();
                    nlooks += 1;
                    helper = parent != null ? parent.impl_getStyleHelper() : null;
                } while (parent != null && helper == null);
            }           
        }
        
        if (csShorthand != null && cascadingStyle != null) {
       
            final boolean shorthandImportant = 
                    csShorthand.getStyle().getDeclaration().isImportant();
            
            final Style style = cascadingStyle.getStyle();
            final boolean familyImportant = 
                style.getDeclaration().isImportant();

            if (nlooks < distance) {
                //
                // if we found a font sub-property closer to the node than 
                // the font shorthand, then the sub-property style
                // wins provided the fontShorthand isn't important.
                // If font shorthand is important and sub-property style is 
                // important, then sub-property style wins (since, being closer
                // to the node, it is more specific)
                //                
                if (shorthandImportant == true && familyImportant == false) {
                    cascadingStyle = null;
                } 

            } else if (cascadingStyle.compareTo(csShorthand) < 0) {
                //
                // if we found font sub-property at the same distance from the
                // node as the font shortand, then do a normal compare
                // to see if the sub-property is more specific. If it isn't
                // then return null.
                // 
                cascadingStyle = null;
                        
            }

        }
        
        return cascadingStyle;
        
    }
    
    /**
     * Look up a font property. This is handled separately from lookup since
     * font is inherited and has sub-properties. One should expect that the 
     * text font for the following would be 16px Arial. The lookup method would
     * give 16px system since it would look <em>only</em> for font-size, 
     * font-family, etc <em>only</em> if the lookup on font failed.
     * <pre>
     * Text text = new Text("Hello World");
     * text.setStyle("-fx-font-size: 16px;");
     * Group group = new Group();
     * group.setStyle("-fx-font: 12px Arial;");
     * group.getChildren().add(text);
     * </pre>
     * @param node
     * @param styleable
     * @param isUserSet
     * @param states
     * @param userStyles
     * @param originatingNode
     * @param cacheEntry
     * @param styleList
     * @return 
     */
    private CalculatedValue lookupFont(Node node, StyleableProperty styleable, 
            boolean isUserSet, Node originatingNode, 
            CacheEntry cacheEntry, List<Style> styleList) {

        Origin origin = null;
        boolean isRelative = false;
            
        // distance keeps track of how far up the parent chain we had to go
        // to find a font shorthand. We'll look no further than this
        // for the missing pieces. nlooks is used to keep track of how 
        // many parents we've looked at. nlooks should never exceed distance. 
        int distance = 0, nlooks = 0;
        
        Node parent = node;
        StyleHelper helper = this;
        String property = styleable.getProperty();
        CascadingStyle csShorthand = null;
        while (parent != null) {
            
            final long states = helper.getPseudoClassState();
            final Map<String,CascadingStyle> inlineStyles = helper.getInlineStyleMap(parent);
            
            final CascadingStyle cascadingStyle =
                helper.getStyle(parent, property, states, inlineStyles);
            
            if (isUserSet) {
                //
                // If isUserSet, then we don't look beyond the current node. 
                // Only if the user did not set the font will we inherit.
                //
                if (cascadingStyle != null) {
                
                    origin = cascadingStyle.getOrigin();

                    // if the user set font and the origin of the font shorthand
                    // is the user agent stylesheet, then we can't use the style
                    // since ua styles shouldn't override setFont
                    if (origin != Origin.USER_AGENT) {
                        csShorthand = cascadingStyle;
                    }                    
                }    
                
                break;
                
            } else if (cascadingStyle != null) {
                //
                // if isUserSet is false, then take the style if we found one
                //
                origin = cascadingStyle.getOrigin();
                csShorthand = cascadingStyle;
                break;
                
            } else {
                //
                // Otherwise, isUserSet is false and style is null, so look
                // up the parent chain for the next -fx-font.
                //
                do {
                    parent = parent.getParent();
                    distance += 1;
                    helper = parent != null ? parent.impl_getStyleHelper() : null;
                } while (parent != null && helper == null);
                
            }
        }
        
        final long states = pseudoClassStates[0];
        final Map<String,CascadingStyle> inlineStyles = getInlineStyleMap(node);

        String family = null;
        double size = -1;
        FontWeight weight = null;
        FontPosture style = null;
        
        if (csShorthand != null) {
            
            if (styleList != null) {
                styleList.add(csShorthand.getStyle());
            }
            
            // pull out the pieces. 
            final CalculatedValue cv = 
                calculateValue(csShorthand, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);
            
            if (origin == null || origin.compareTo(cv.origin) <= 0) {
                
                if (cv.value instanceof Font) {
                    Font f = (Font)cv.value;
                    isRelative = cv.isRelative;
                    origin = cv.origin;
                
                    // what did font shorthand specify? 
                    ParsedValue[] vals = 
                            (ParsedValue[])csShorthand.getParsedValue().getValue();
                    // Use family and size from converted font since the actual 
                    // values may have been resolved. The weight and posture, 
                    // however, are hard to get reliably from the font so use 
                    // the parsed value instead. 
                    if (vals[0] != null) family = f.getFamily();
                    if (vals[1] != null) size   = f.getSize();
                    if (vals[2] != null) weight = (FontWeight)vals[2].convert(null);
                    if (vals[3] != null) style  = (FontPosture)vals[3].convert(null);

                }
            }
            
        }
        
        CascadingStyle csFamily = null; 
        if ((csFamily = lookupFontSubPropertyStyle(node, property+"-family",
                isUserSet, csShorthand, distance)) != null &&
               (csShorthand == null || csShorthand.compareTo(csFamily) < 0)) {

            if (styleList != null) {
                styleList.add(csFamily.getStyle());
            }
            
            final CalculatedValue cv = 
                calculateValue(csFamily, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            if (origin == null || origin.compareTo(cv.origin) <= 0) {                        
                if (cv.value instanceof String) {
                    family = Utils.stripQuotes((String)cv.value);
                    origin = cv.origin;
                }
            }
                
        }
        
        CascadingStyle csSize = null;
        if ((csSize = lookupFontSubPropertyStyle(node, property+"-size",
                isUserSet, csShorthand, distance))!= null &&
               (csShorthand == null || csShorthand.compareTo(csSize) < 0)) {
       
            if (styleList != null) {
                styleList.add(csSize.getStyle());
            }

            final CalculatedValue cv = 
                calculateValue(csSize, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            if (origin == null || origin.compareTo(cv.origin) <= 0) {                        
                if (cv.value instanceof Double) {
                    size = ((Double)cv.value).doubleValue();
                    isRelative = cv.isRelative;
                    origin = cv.origin;
                }
            }

        }
        
        CascadingStyle csWeight = null;
        if ((csWeight = lookupFontSubPropertyStyle(node, property+"-weight",
                isUserSet, csShorthand, distance))!= null &&
               (csShorthand == null || csShorthand.compareTo(csWeight) < 0)) {

            if (styleList != null) {
                styleList.add(csWeight.getStyle());
            }
            
            final CalculatedValue cv = 
                calculateValue(csWeight, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            if (origin == null || origin.compareTo(cv.origin) <= 0) {                        
                if (cv.value instanceof FontWeight) {
                    weight = (FontWeight)cv.value;
                    origin = cv.origin;
                }
            }
                
        }

        CascadingStyle csStyle = null;
        if ((csStyle = lookupFontSubPropertyStyle(node, property+"-style",
                isUserSet, csShorthand, distance))!= null &&
               (csShorthand == null || csShorthand.compareTo(csStyle) < 0)) {
            
            if (styleList != null) {
                styleList.add(csStyle.getStyle());
            }
            
            final CalculatedValue cv = 
                calculateValue(csStyle, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            if (origin == null || origin.compareTo(cv.origin) <= 0) {                        
                if (cv.value instanceof FontPosture) {
                    style = (FontPosture)cv.value;
                    origin = cv.origin;
                }
            }                

        }
        
        // if no styles were found, then skip...
        if (family == null &&
            size   == -1   &&
            weight == null &&
            style  == null) {
            return SKIP;
        }
        
        // Now we have all the pieces from the stylesheet
        // still be some missing. We'll grab those from the node. 
        WritableValue writable = styleable != null 
                ? styleable.getWritableValue(node) 
                : null;
        Font f = null;
        if (writable != null) {
            f = (Font)writable.getValue();
        }
        if (f == null) f = Font.getDefault();

        if (family == null) {
            family = f.getFamily();
        }

        if (size == -1) {
            size = f.getSize();                
        }

        Font val = null;
        if (weight != null && style != null) {
            val = Font.font(family, weight, style, size);            
        } else if (weight != null) {
            val = Font.font(family, weight, size);
        } else if (style != null) {
            val = Font.font(family, style, size);
        } else {
            val = Font.font(family, size);            
        }

        return new CalculatedValue(val, origin, isRelative);
    }    
    
    /**
     * Called from StyleableProperty getMatchingStyles
     * @param node
     * @param styleableProperty
     * @return 
     */
    List<Style> getMatchingStyles(Styleable node, StyleableProperty styleableProperty) {
        
        final List<CascadingStyle> styleList = new ArrayList<CascadingStyle>();

        getMatchingStyles(node, styleableProperty, styleList);

        List<StyleableProperty> subProperties = styleableProperty.getSubProperties();
        if (subProperties != null) {
            for (int n=0,nMax=subProperties.size(); n<nMax; n++) {
                final StyleableProperty subProperty = subProperties.get(n);
                getMatchingStyles(node, subProperty, styleList);                    
            }
        }

        Collections.sort(styleList);

        final List<Style> matchingStyles = new ArrayList<Style>(styleList.size());
        for (int n=0,nMax=styleList.size(); n<nMax; n++) {
            final Style style = styleList.get(n).getStyle();
            if (!matchingStyles.contains(style)) matchingStyles.add(style);
        }

        return matchingStyles;
    }
    
    private void getMatchingStyles(Styleable node, StyleableProperty styleableProperty, List<CascadingStyle> styleList) {
        
        if (node != null) {
            
            Map<String,List<CascadingStyle>> inlineStyleMap = null;
            
            // create a map of inline styles.
            Styleable parent = node;
            while (parent != null) {
                
                StyleHelper parentHelper = parent.getNode() != null
                        ? parent.getNode().impl_getStyleHelper()
                        : null;
                
                if (parentHelper != null) {
                    
                    Map<String,CascadingStyle> inlineStyles = parentHelper.getInlineStyleMap(parent);
                    
                    if (inlineStyles != null) {
                        
                        if (inlineStyleMap == null) {
                            inlineStyleMap = new HashMap<String,List<CascadingStyle>>();
                        }
                        
                        for(Entry<String,CascadingStyle> entry : inlineStyles.entrySet()) {                            
                            String key = entry.getKey();
                            
                            List<CascadingStyle> inlineStyleList = inlineStyleMap.get(key);
                            if (inlineStyleList == null) {
                                inlineStyleList = new ArrayList<CascadingStyle>();
                                inlineStyleMap.put(key, inlineStyleList);
                            }
                            inlineStyleList.add(entry.getValue());
                        }
                    }
                }
                parent = parent.getStyleableParent();
            }
                    
            String property = styleableProperty.getProperty();
            final Map<String, List<CascadingStyle>> smap = getStyleMap();
            if (smap == null) return;
            
             List<CascadingStyle> styles = smap.get(property);            
            
//            if (inlineStyleMap != null) {
//               if (inlineStyleMap.containsKey(property)) {
//                    List<CascadingStyle> inlineStyleList = inlineStyleMap.get(property);
//                    if (styles == null) styles = new ArrayList<CascadingStyle>();
//                    styles.addAll(inlineStyleList);
//                }
//            }

            if (styles != null) {
                styleList.addAll(styles);
                for (int n=0, nMax=styles.size(); n<nMax; n++) {
                    final CascadingStyle style = styles.get(n);
                    final ParsedValue parsedValue = style.getParsedValue();
                    getMatchingLookupStyles(node, parsedValue, inlineStyleMap, styleList);
                }
            }
            
            if (styleableProperty.isInherits()) {
                parent = node.getStyleableParent();
                while (parent != null) {
                    StyleHelper parentHelper = parent.getNode() != null 
                            ? parent.getNode().impl_getStyleHelper()
                            : null;
                    if (parentHelper != null) {
                        parentHelper.getMatchingStyles(parent, styleableProperty, styleList); 
                    }
                    parent = parent.getStyleableParent();
                }
            }
            
        }

    }
    
    // Pretty much a duplicate of resolveLookups, but without the state
    private void getMatchingLookupStyles(Styleable node, ParsedValue parsedValue, Map<String,List<CascadingStyle>> inlineStyleMap, List<CascadingStyle> styleList) {
                
        if (parsedValue.isLookup()) {
            
            Object value = parsedValue.getValue();
            
            if (value instanceof String) {
                
                final String property = (String)value;
                // gather up any and all styles that contain this value as a property
                Styleable parent = node;
                do {
                    final StyleHelper helper = parent.getNode() != null 
                            ? parent.getNode().impl_getStyleHelper()
                            : null;
                    if (helper != null) {
                                             
                        final int start = styleList.size();
                        
                        final Map<String, List<CascadingStyle>> smap = getStyleMap();
                        if (smap == null) return;

                        List<CascadingStyle> styles = smap.get(property);
                        
                        if (styles != null) {
                            styleList.addAll(styles);
                        }
                        
                        List<CascadingStyle> inlineStyles = (inlineStyleMap != null) 
                            ? inlineStyleMap.get(property) 
                            : null;
                        
                        if (inlineStyles != null) {
                            styleList.addAll(inlineStyles);
                        }
                        
                        final int end = styleList.size();
                        
                        for (int index=start; index<end; index++) {
                            final CascadingStyle style = styleList.get(index);
                            getMatchingLookupStyles(parent, style.getParsedValue(), inlineStyleMap, styleList);
                        }
                    }
                                                                                    
                } while ((parent = parent.getStyleableParent()) != null);
            
            }
        }
        
        // If the value doesn't contain any values that need lookup, then bail
        if (!parsedValue.isContainsLookups()) {
            return;
        }

        final Object val = parsedValue.getValue();
        if (val instanceof ParsedValue[][]) {
        // If ParsedValue is a layered sequence of values, resolve the lookups for each.
            final ParsedValue[][] layers = (ParsedValue[][])val;
            for (int l=0; l<layers.length; l++) {
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                        getMatchingLookupStyles(node, layers[l][ll], inlineStyleMap, styleList);
                }
            }

        } else if (val instanceof ParsedValue[]) {
        // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layer = (ParsedValue[])val;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                    getMatchingLookupStyles(node, layer[l], inlineStyleMap, styleList);
            }
        }

    }
    
}
