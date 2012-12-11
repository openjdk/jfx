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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import com.sun.javafx.Logging;
import com.sun.javafx.Utils;
import com.sun.javafx.css.Origin;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.logging.PlatformLogger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;

/**
 * The StyleHelper is a helper class used for applying CSS information to Nodes.
 * In theory a single StyleHelper can be reused among all Nodes which share the
 * same id/styleClass combination (ie: if the same exact set of styles apply
 * to the same nodes, then they should be able to use the same StyleHelper).
 */
final public class StyleHelper {

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
     * Creates a new StyleHelper.
     */
    public StyleHelper(Node node) {
        this.node = node;
        // these will get set in postinit. See comments there for why.
        this.inited = false;
    }
    
    private boolean inited = false;    
    private void postinit() {
    
        // I'd like to be able to do this in the constructor and make 
        // fontProp and mightInherit invariants, but doing so causes this
        // exception:
        //
        // java.lang.ClassCastException: javafx.scene.control.PopupControl$CSSBridge cannot be cast to javafx.scene.control.Tooltip$CSSBridge
        // at javafx.scene.control.Tooltip$CSSProperties$1.getWritableValue(Tooltip.java:322)
        // at com.sun.javafx.css.StyleHelper.<init>(StyleHelper.java:99)
        // at javafx.scene.Node.<init>(Node.java:2183)
        // at javafx.scene.Parent.<init>(Parent.java:1206)
        // at javafx.scene.Group.<init>(Group.java:80)
        // at javafx.scene.control.PopupControl$CSSBridge.<init>(PopupControl.java:998)
        // at javafx.scene.control.PopupControl.<init>(PopupControl.java:100)
        // at javafx.scene.control.Tooltip.<init>(Tooltip.java:143)
        // at helloworld.HelloTooltip.start(HelloTooltip.java:32)    
        //
        CssMetaData styleableFontProperty = null;
        CssMetaData styleableThatInherits = null;
        
        final List<CssMetaData> props = node.getCssMetaData();
        final int pMax = props != null ? props.size() : 0;
        for (int p=0; p<pMax; p++) {
            final CssMetaData prop = props.get(p);
            
            if (styleableFontProperty == null && "-fx-font".equals(prop.getProperty())) {                
                styleableFontProperty = prop;
            }
            
            if (styleableThatInherits == null && prop.isInherits()) {
                styleableThatInherits = prop;
            }
            
            if (styleableFontProperty != null && styleableThatInherits != null) {
                break;
            }
        }
        
        this.fontProp = (styleableFontProperty != null) 
                ?  styleableFontProperty.getWritableValue(node) 
                : null;
        
        this.mightInherit = styleableThatInherits != null;
        
        inited = true;
    }
    
    /**
     * Called from StyleManager when a Node needs to reapply styles, 
     * this method causes the StyleHelper to adopt the style map and 
     * reinitialize its internals.
     */
    public void setStyles(StyleManager styleManager) {

        // Reset to initial state. If we end up with
        // no styles, inline or otherwise, then transitionToState 
        // will be a no-op.
        resetToInitialState();
        
        // styleManager is not expected to be null
        assert(styleManager != null);
        if (styleManager == null) return;

        if (!inited) postinit();
        
        // need to know how far we are to root in order to init arrays.
        Node parent = node;
        int depth = 0;
        while(parent != null) {
            depth++;
            parent = parent.getParent();
        }
        
        // The List<CacheEntry> should only contain entries for those
        // pseudoclass states that have styles. The StyleHelper's
        // pseudoclassStateMask is a bitmask of those pseudoclasses that
        // appear in the node's StyleHelper's smap. This list of
        // pseudoclass masks is held by the StyleCacheKey. When a node is
        // styled, its pseudoclasses and the pseudoclasses of its parents
        // are gotten. By comparing the actual pseudoclass state to the
        // pseudoclass states that apply, a CacheEntry can be created or
        // fetched using only those pseudoclasses that matter.
        final long[] pclassMasks = new long[depth];
        
        final StyleManager.StyleMap styleMap = styleManager != null
                ? styleManager.findMatchingStyles(node, pclassMasks)
                : null;

        
        final Map<String, List<CascadingStyle>> smap = styleMap != null ? styleMap.map : null;
        if (smap == null || smap.isEmpty()) {
            
            // If there are no styles at all, then return
            final String inlineStyle = node.getStyle();
            if (mightInherit == false && (inlineStyle == null || inlineStyle.trim().isEmpty())) {
                return;
            }
        }   
        
        setInternalState(styleManager, styleMap, pclassMasks);
    }
    
    //
    // Clear internal data structures, resetting this StyleHelper
    // to its initial state with no mapped styles. If the style 
    // map is not reestablished after this call, then transitionToState
    // becomes a no-op.
    // 
    private void resetToInitialState() {
        
        styleClassBits = null;
        key = 0;
        if (smapRef != null) {
            smapRef.clear();
            smapRef = null;
        }
        if (sharedStyleCacheRef != null) {
            sharedStyleCacheRef.clear();
            sharedStyleCacheRef = null;            
        }
        if (localStyleCache != null) {
            localStyleCache.entries.clear();
            localStyleCache = null;
        }
        pseudoclassStateMask = 0;
        transitionStates = null;
                
    }
    
    // Set internal data structures
    private void setInternalState(
            StyleManager styleManager, 
            StyleManager.StyleMap styleMap, 
            long[] pclassMasks) {
        
        final int depth = pclassMasks.length;

        final Map<String, List<CascadingStyle>> smap = styleMap != null ? styleMap.map : null;        
        smapRef = new WeakReference<Map<String, List<CascadingStyle>>>(smap);
        
        pseudoclassStateMask = pclassMasks[0];

        Parent parent = node.getParent();
        for(int n=1; n<depth; n++) {
            final StyleHelper parentHelper = parent.impl_getStyleHelper();
            parentHelper.pseudoclassStateMask = parentHelper.pseudoclassStateMask | pclassMasks[n];
            parent = parent.getParent();
        } 
        
        // TODO: move initialization of localStyleCache somewhere else.
        //       It should not be hidden down here. Maybe to getCacheEntry
        localStyleCache = new StyleCacheBucket(pclassMasks);

        long[] keys = new long[depth];
        key = keys[0] = styleMap.uniqueId;
        parent = node.getParent();
        for(int k=1; k<depth; k++) {
            final StyleHelper helper = parent.impl_getStyleHelper();            
            keys[k] = helper.key;
            parent = parent.getParent();
        }
        
        final StyleCacheKey styleCacheKey = new StyleCacheKey(keys); 
        //
        // Look to see if there is already a cache for this set of helpers
        //
        Map<StyleCacheKey, StyleCacheBucket> styleCache = styleManager.getStyleCache();
        StyleCacheBucket sharedStyleCache = styleCache.get(styleCacheKey);
        if (sharedStyleCache == null) {
            sharedStyleCache = new StyleCacheBucket(pclassMasks);
            styleCache.put(styleCacheKey, sharedStyleCache);
        }
        sharedStyleCacheRef = new WeakReference<StyleCacheBucket>(sharedStyleCache);
                     
    }

    private long key;

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
    private Reference<StyleCacheBucket> sharedStyleCacheRef;
    
    private StyleCacheBucket getSharedStyleCache() {
        final StyleCacheBucket styleCache = sharedStyleCacheRef.get();
        return styleCache;
    }

    /**
     * A place to store calculated values that cannot be stored in shared cache
     */
    private StyleCacheBucket localStyleCache;
    
    final static class StyleCacheKey {
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
    final static class CacheEntry {

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
        if (localStyleCache == null) return null;
        
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

    public void inlineStyleChanged(StyleManager styleManager) {

        // styleManager is not expected to be null
        assert(styleManager != null);
        if (styleManager == null) return;
        
        // Clear local cache so styles will be recalculated. 
        // Since we're clearing the cache and getting (potentially) 
        // new styles, reset the properties to initial values.
        if (localStyleCache != null) {
            
            node.impl_cssResetInitialValues();
            localStyleCache.entries.clear();
            
            // do we have any styles at all now?
            final String inlineStyle = node.getStyle();
            if(inlineStyle == null && inlineStyle.isEmpty()) {

                final Map<String, List<CascadingStyle>> smap = getStyleMap();            
                if (smap == null || smap.isEmpty()) {
                    // We have no styles! Reset this StyleHelper to its
                    // initial state so that calls to transitionToState 
                    // become a no-op.
                    resetToInitialState();
                }
                
                // If smap isn't empty, then there are styles that
                // apply to the node. There isn't a need to remap the styles
                // since we've only removed an inline style and inline styles
                // aren't part of the style map.
            }

            
            
            
        } else {
            final String inlineStyle = node.getStyle();
            if (inlineStyle == null || inlineStyle.isEmpty()) {
                return;
            }
            // if we don't have a localStyleCache, that means this 
            // node doesn't have any applicable styles and it didn't
            // have an inline style before. If it did have an inline
            // style before, then there would be an smap, albeit an
            // an empty one. See setStyles for this bit of logic. 
            // But now the  node does have an inline style and so it
            // needs to have an smap and localStyleCache for the logic
            // in transitionToState to work. 
            Node parent = node;
            int depth = 0;
            while(parent != null) {
                depth++;
                parent = parent.getParent();
            }

            final long[] pclassMasks = new long[depth];
            Arrays.fill(pclassMasks, 0);
            
            setInternalState(styleManager, StyleManager.StyleMap.EMPTY_MAP, pclassMasks);
        }
        
    }
    
    /**
     * A map from StyleableProperty Name (String) => List of Styles. This shortens
     * the lookup time for each property. The smap is shared and is owned
     * by the StyleManager Cache.
     */
    private Reference<Map<String, List<CascadingStyle>>> smapRef;

    private Map<String, List<CascadingStyle>> getStyleMap() {
        final Map<String, List<CascadingStyle>> smap = smapRef != null ? smapRef.get() : null;
        return smap;
    }
    
    /** The node's font property, if there is one */
    private WritableValue fontProp;

    /** 
     * True if the node has any property that might inherit its value. It
     * might be that the smap is empty but some upstream parent has an
     * inline style that this node should inherit. 
     */
    private boolean mightInherit;
    
    /** 
     * The node to which this selector belongs. StyleHelper is a final in Node.
     */
    private final Node node;
    
    /** 
     * The Node's style-class as bits.
     */
    private long[] styleClassBits;
            
    long[] getStyleClassBits() {

        if (styleClassBits == null) {

            styleClassBits = SimpleSelector.getStyleClassMasks(node.getStyleClass());
        }            
        
        return styleClassBits;
    }
    
    /**
     * Invoked by Node to determine whether a change to a specific pseudoclass
     * state should result in the Node being marked dirty with an UPDATE flag.
     */
    public boolean isPseudoclassUsed(String pseudoclass) {
        final long mask = StyleManager.getPseudoclassMask(pseudoclass);
        return ((pseudoclassStateMask & mask) == mask);
    }

    /**
     * A convenient place for holding default values for populating the
     * List&lt;Style&gt; that is populated if the Node has a 
     * Map&lt;WritableValue&gt;, List&lt;Style&gt;. 
     * See handleNoStyleFound
     */
    private static final Map<CssMetaData,Style> stylesFromDefaults = 
            new HashMap<CssMetaData,Style>();
    
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
     * Cache of parsed, inline styles. The key is Node.style. 
     * The value is the set of property styles from parsing Node.style.
     */
    private static final Map<String,Map<String,CascadingStyle>> inlineStylesCache =
        new HashMap<String,Map<String,CascadingStyle>>();

    /**
     * Get the map of property to style from the rules and declarations
     * in the stylesheet. There is no need to do selector matching here since
     * the stylesheet is from parsing Node.style.
     */
    private static Map<String,CascadingStyle> getInlineStyleMap(Stylesheet inlineStylesheet) {

        final Map<String,CascadingStyle> inlineStyleMap = new HashMap<String,CascadingStyle>();
        if (inlineStylesheet != null) {
            // Order in which the declaration of a CascadingStyle appears (see below)
            int ordinal = 0;
            // For each declaration in the matching rules, create a CascadingStyle object
            final List<Rule> stylesheetRules = inlineStylesheet.getRules();
            for (int i = 0, imax = stylesheetRules.size(); i < imax; i++) {
                final Rule rule = stylesheetRules.get(i);
                final List<Declaration> declarations = rule.getDeclarations();
                for (int k = 0, kmax = declarations.size(); k < kmax; k++) {
                    Declaration decl = declarations.get(k);

                    CascadingStyle s = new CascadingStyle(
                        new Style(Selector.getUniversalSelector(), decl),
                        0, // no pseudo classes
                        0, // specificity is zero
                        // ordinal increments at declaration level since
                        // there may be more than one declaration for the
                        // same attribute within a rule or within a stylesheet
                        ordinal++
                    );

                    inlineStyleMap.put(decl.property,s);
                }
            }
        }
        return inlineStyleMap;
    }

    /**
     * Get the mapping of property to style from Node.style for this node.
     */
    private static Map<String,CascadingStyle> getInlineStyleMap(Node node) {

        return getInlineStyleMap(node.impl_getStyleable());
    }

    /**
     * Get the mapping of property to style from Node.style for this node.
     */
    private static Map<String,CascadingStyle> getInlineStyleMap(Styleable styleable) {
        
        final String inlineStyles = styleable.getStyle();

        // If there are no styles for this property then we can just bail
        if ((inlineStyles == null) || inlineStyles.isEmpty()) return null;

        Map<String,CascadingStyle> styles = inlineStylesCache.get(inlineStyles);
        
        if (styles == null) {

            Stylesheet inlineStylesheet =
                CSSParser.getInstance().parseInlineStyle(styleable);
            if (inlineStylesheet != null) {
                inlineStylesheet.setOrigin(Origin.INLINE);
            }
            styles = getInlineStyleMap(inlineStylesheet);
            
            inlineStylesCache.put(inlineStyles, styles);
        }

        return styles;
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
    private long pseudoclassStateMask;

    /**
     * dynamic pseudoclass state for this helper - only valid during a pulse. 
     * Note Well: The array runs from leaf to root. That is, 
     * transitionStates[0] is the pseudoclassState for node and 
     * transitionStates[1..(states.length-1)] are the pseudoclassStates for the 
     * node's parents.
     */ 
    private long[] transitionStates;

    
    // get pseudoclass state for the node 
    long getPseudoClassState() {
        // setTransitionState should have been called on this StyleHelper
        assert (transitionStates != null && transitionStates.length > 0);        
        return transitionStates != null && transitionStates.length > 0 ? transitionStates[0] : 0;
    }
    
    public void setTransitionState(long state) {
        
        Parent parent = node.getParent();
        if (parent != null) {
            
            // My transitionStates include those of my parents
            final StyleHelper helper = parent.impl_getStyleHelper();
            long[] parentTransitionStates = helper.getTransitionStates();
            
            // setTransitionState should have been called on parent
            assert (parentTransitionStates != null && parentTransitionStates.length > 0);

            // 
            // Fake the transition states if assertions are disabled and
            // the assert conditions hold true.
            // 
            if (parentTransitionStates == null || parentTransitionStates.length == 0) {
                int n=0;
                while(parent != null) {
                    n += 1;
                    parent = parent.getParent();
                }
                parentTransitionStates = new long[n];
            }
                        
            final int nStates = 
                (parentTransitionStates != null && parentTransitionStates.length > 0) 
                    ? parentTransitionStates.length+1 
                    : 1;
            
            if (transitionStates == null || transitionStates.length != nStates) {
                transitionStates = new long[nStates];
            }
            
            // transtitionStates[0] is my state, so copy parent states to 
            // transitionStates[1..nStates-1]
            if (nStates > 1) {
                // if nStates <= 1, then parentTransitionStates must be null or length zero.
                System.arraycopy(parentTransitionStates, 0, transitionStates, 1, parentTransitionStates.length);
            }
            
        } else {
            if (transitionStates == null || transitionStates.length != 1) {
                transitionStates = new long[1];
            }            
        }
        transitionStates[0] = state;
    }
    
    private long[] getTransitionStates() {
        return transitionStates;
    }

    /* 
     * The lookup function return an Object but also
     * needs to return whether or not the value is cacheable.
     * 
     * isShared is true if the value is not specific to a node. 
     * 
     */
    final private static class CalculatedValue {
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
    public void transitionToState() {
       
        if (smapRef == null || smapRef.get() == null) return;
        

        //
        // The CacheEntry to choose depends on this Node's state and
        // the state of its parents. Without the parent state, the fact that
        // this node in this state matched foo:blah bar { } is lost.
        //
        long[] states = getTransitionStates();
        
        //
        // Styles that need lookup can be cached provided none of the styles
        // are from Node.style.
        //
                
        final CacheEntry cacheEntry = getCacheEntry(node, states);
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
        // inlineStyles is this node's Node.style. This is passed along and is
        // used in getStyle. Importance being the same, an author style will
        // trump a user style and a user style will trump a user_agent style.
        //
        final Map<String,CascadingStyle> inlineStyles = StyleHelper.getInlineStyleMap(node);
        
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
                getFontForUseInConvertingRelativeSize(node, cacheEntry, getPseudoClassState(), inlineStyles);
            
            cacheEntry.font = font;
            assert(cacheEntry.font != null);
            //
            // If the cacheEntry's font is null, then this is a new cache entry.
            // If the font is set by the user or there are inline styles, 
            // take the slow path to ensure that the local values are updated. 
            // The next time transitionToState is called, we'll take the fast
            // path since all the values will have been calculated for this entry.
            //
            fastpath = false;
//                fastpath && 
//                cacheEntry.font.origin != Origin.USER && 
//                cacheEntry.font.origin != Origin.INLINE && 
//                inlineStyles == null;
        } 
        
        final List<CssMetaData> styleables = CssMetaData.getStyleables(node);
        
        // Used in the for loop below, and a convenient place to stop when debugging.
        final int max = styleables.size();

        // RT-20643
        CssError.setCurrentScene(node.getScene());
        
        // For each property that is settable, we need to do a lookup and
        // transition to that value.
        for(int n=0; n<max; n++) {

            final CssMetaData styleable = styleables.get(n);
            
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

                calculatedValue = lookup(node, styleable, isUserSet, getPseudoClassState(), 
                        inlineStyles, node, cacheEntry, styleList);

                cacheEntry.put(property, calculatedValue);

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
                    if ((errors = StyleManager.getErrors()) != null) {
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
     * Gets the CSS CascadingStyle for the property of this node in these pseudoclass
     * states. A null style may be returned if there is no style information
     * for this combination of input parameters.
     *
     * @param node 
     * @param property
     * @param states
     * @return
     */
    private CascadingStyle getStyle(Node node, String property, long states, Map<String,CascadingStyle> inlineStyles){

        assert node != null && property != null: String.valueOf(node) + ", " + String.valueOf(property);

        final CascadingStyle inlineStyle = (inlineStyles != null) ? inlineStyles.get(property) : null;

        // Get all of the Styles which may apply to this particular property
        final Map<String, List<CascadingStyle>> smap = getStyleMap();
        if (smap == null) return inlineStyle;

        final List<CascadingStyle> styles = smap.get(property);

        // If there are no styles for this property then we can just bail
        if ((styles == null) || styles.isEmpty()) return inlineStyle;

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
        
        if (inlineStyle != null) {

            // is inlineStyle more specific than style?    
            if (style == null || inlineStyle.compareTo(style) < 0) {
                style = inlineStyle;
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
    private CalculatedValue lookup(Node node, CssMetaData styleable, 
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
        final List<CssMetaData> subProperties = styleable.getSubProperties();
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
                Map<CssMetaData,Object> subs = null;
                Origin origin = null;
                
                boolean isRelative = false;
                
                for (int i=0; i<numSubProperties; i++) {
                    CssMetaData subkey = subProperties.get(i);
                    CalculatedValue constituent = 
                        lookup(node, subkey, isUserSet, states, userStyles, 
                            originatingNode, cacheEntry, styleList);
                    if (constituent != SKIP) {
                        if (subs == null) {
                            subs = new HashMap<CssMetaData,Object>();
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
                    final String msg = formatExceptionMessage(node, styleable, null, cce);
                    List<CssError> errors = null;
                    if ((errors = StyleManager.getErrors()) != null) {
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
    private CalculatedValue handleNoStyleFound(Node node, CssMetaData styleable,
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
    private CalculatedValue inherit(Node node, CssMetaData styleable,
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
            Map<String,CascadingStyle> inlineStyles) {
        
        final CascadingStyle style = getStyle(node, property, states, inlineStyles);
        if (style != null) {
            return style;
        } else {
            // if style is null, it may be because there isn't a style for this
            // node in this state, or we may need to look up the parent chain
            if (states > 0) {
                // if states > 0, then we need to check this node again,
                // but without any states.
                return resolveRef(node,property,0,inlineStyles);
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
            Map<String,CascadingStyle> inlineStyles,
            ObjectProperty<Origin> whence,
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
                    resolveRef(node, sval, states, inlineStyles);

                if (resolved != null) {
                    
                    if (styleList != null) {
                        final Style style = resolved.getStyle();
                        if (style != null && !styleList.contains(style)) {
                            styleList.add(style);
                        }
                    }
                    
                    // The origin of this parsed value is the greatest of 
                    // any of the resolved reference. If a resolved reference
                    // comes from an inline style, for example, then the value
                    // calculated from the resolved lookup should have inline
                    // as its origin. Otherwise, an inline style could be 
                    // stored in shared cache.
                    final Origin wOrigin = whence.get();
                    final Origin rOrigin = resolved.getOrigin();
                    if (rOrigin != null && (wOrigin == null ||  wOrigin.compareTo(rOrigin) < 0)) {
                        whence.set(rOrigin);
                    } 
                    
                    // the resolved value may itself need to be resolved.
                    // For example, if the value "color" resolves to "base",
                    // then "base" will need to be resolved as well.
                    return resolveLookups(node, resolved.getParsedValue(), states, inlineStyles, whence, styleList);
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
                        resolveLookups(node, layers[l][ll], states, inlineStyles, whence, styleList);
                }
            }

        } else if (val instanceof ParsedValue[]) {
        // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layer = (ParsedValue[])val;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                layer[l].resolved =
                    resolveLookups(node, layer[l], states, inlineStyles, whence, styleList);
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
    
    private String formatUnresolvedLookupMessage(Node node, CssMetaData styleable, Style style, ParsedValue resolved) {
        
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

    private String formatExceptionMessage(Node node, CssMetaData styleable, Style style, Exception e) {
        
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
            final CssMetaData styleable, 
            final long states,
            final Map<String,CascadingStyle> inlineStyles, 
            final Node originatingNode, 
            final CacheEntry cacheEntry, 
            final List<Style> styleList) {

        final ParsedValue cssValue = style.getParsedValue();
        if (cssValue != null && !("null").equals(cssValue.getValue())) {

            ObjectProperty<Origin> whence = new SimpleObjectProperty<Origin>(style.getOrigin());
            final ParsedValue resolved = 
                resolveLookups(node, cssValue, states, inlineStyles, whence, styleList);
            
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
                    fontValue = cacheEntry.font;
                }
                final Font font = (fontValue != null) ? (Font)fontValue.value : Font.getDefault();


                if (resolved.getConverter() != null)
                    val = resolved.convert(font);
                else
                    val = styleable.getConverter().convert(resolved, font);

                final Origin origin = whence.get();
                return new CalculatedValue(val, origin, resolved.isNeedsFont());
                
            } catch (ClassCastException cce) {
                final String msg = formatUnresolvedLookupMessage(node, styleable, style.getStyle(),resolved);
                List<CssError> errors = null;
                if ((errors = StyleManager.getErrors()) != null) {
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
                if ((errors = StyleManager.getErrors()) != null) {
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
                if ((errors = StyleManager.getErrors()) != null) {
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
    private boolean isUserSetProperty(Node node, CssMetaData styleable) {
        WritableValue writable = node != null ? styleable.getWritableValue(node) : null;
        // writable could be null if this is a sub-property
        Origin origin = writable != null ? ((StyleableProperty)writable).getOrigin() : null;
        return (origin == Origin.USER);    
    }    
            
    private static final CssMetaData dummyFontProperty =
            new CssMetaData.FONT<Node>("-fx-font", Font.getDefault()) {

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
        
        CacheEntry parentCacheEntry = null;
        Node parent = child;
        StyleHelper parentHelper = null;
        do {
            parent = parent.getParent();
            if (parent != null) {
                parentHelper = parent.impl_getStyleHelper();
                final long[] pstates = parentHelper.getTransitionStates();
                parentCacheEntry = parentHelper.getCacheEntry(parent, pstates);                
            }
        } while (parent != null && parentCacheEntry == null);

        return parentCacheEntry;
    }
   
    private CalculatedValue getFontForUseInConvertingRelativeSize(
             final Node node,
             final CacheEntry cacheEntry,
            long pseudoclassState,
            Map<String,CascadingStyle> inlineStyles)
     {
        
        // To make the code easier to read, define CONSIDER_INLINE_STYLES as true. 
        // Passed to lookupFontSubProperty to tell it whether or not 
        // it should look for inline styles
        final boolean CONSIDER_INLINE_STYLES = true;
        
        Origin origin = null;
        Font foundFont = null;
        CalculatedValue foundSize = null;
        CalculatedValue foundShorthand = null;
        boolean isRelative = false;
         
        // RT-20145 - if looking for font size and the node has a font, 
        // use the font property's value if it was set by the user and
        // there is not an inline or author style.
        if (fontProp != null) {
 
            Origin fpOrigin = CssMetaData.getOrigin(fontProp);
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
            
            // If we don't have an existing font, or if the origin of the
            // existing font is less than that of the shorthand, then
            // take the shorthand. If the origins compare equals, then take 
            // the shorthand since the fontProp value will not have been
            // updated yet.
            // Man, this drove me nuts!
            if (origin == null || origin.compareTo(cv.origin) <= 0) {
                               
                // cv could be SKIP
                if (cv.value instanceof Font) {
                    origin = cv.origin;
                    foundShorthand = cv;
                    foundFont = null;
                }

            }  
         }
 
        final boolean isUserSet = 
                origin == Origin.USER || origin == Origin.INLINE;

        // now look for -fx-size, but don't look past the current node (hence 
        // distance == 0 and negation of CONSIDER_INLINE_STYLES)        
        CascadingStyle fontSize = lookupFontSubPropertyStyle(node, "-fx-font-size",
                isUserSet, fontShorthand, 0, !CONSIDER_INLINE_STYLES);
        
        if (fontSize != null) {

            final CalculatedValue cv = 
                calculateValue(fontSize, node, dummyFontProperty, pseudoclassState, inlineStyles, 
                    node, cacheEntry, null);
            
            if (cv.value instanceof Double) {
                if (origin == null || origin.compareTo(cv.origin) <= 0) {  
                    origin = cv.origin;
                    foundSize = cv;
                    foundShorthand = null;
                    foundFont = null;
                }
             }

         }
         
         
        if (foundShorthand != null) {
            
            return foundShorthand;
            
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
            final int distance,
            boolean considerInlineStyles) {
    
        Node parent = node;
        StyleHelper helper = this;
        int nlooks = 0;
        CascadingStyle returnValue = null;
        Origin origin = null;
        boolean consideringInline = false;
        
        while (parent != null && (consideringInline || nlooks <= distance)) {
            
            final long states = helper.getPseudoClassState();
            final Map<String,CascadingStyle> inlineStyles = StyleHelper.getInlineStyleMap(parent);
            
            CascadingStyle cascadingStyle = 
                helper.getStyle(parent, subProperty, states, inlineStyles);
            
            if (isUserSet) {
                //
                // Don't look past this node if the user set the property.
                //
                if (cascadingStyle != null) {
                    
                    origin = cascadingStyle.getOrigin();

                    // if the user set the property and the origin is the
                    // user agent stylesheet, then we can't use the style
                    // since ua styles shouldn't override user set values
                    if (origin == Origin.USER_AGENT) {
                        returnValue = null;
                    }
                }    
                
                break;
                
            } else if (cascadingStyle != null) {

                //
                // If isUserSet is false, then take the style if we found one.
                // If csShorthand is not null, then were looking for an inline
                // style. In this case, if the origin is INLINE, we'll take it.
                // If it isn't INLINE we'll keep looking for an INLINE style.
                //
                final boolean isInline = cascadingStyle.getOrigin() == Origin.INLINE;
                if (returnValue == null || isInline) {
                    origin = cascadingStyle.getOrigin();
                    returnValue = cascadingStyle;
                    
                    // If we found and inline style, then we don't need to look 
                    // further. Also, if the style is not an inline style and we
                    // don't want to consider inline styles, then look no further.
                    if (isInline || considerInlineStyles == false) {
                        
                        break;
                        
                    } else {
                        // 
                        // If we are here, then the style is not an inline style
                        // and we do want to consider inline styles. Setting
                        // this flag will cause the code to look beyond nlooks
                        // to see if there is an inline style
                        //
                        consideringInline = true;
                    }
                } 
                
            }
                
            // 
            // haven't found it yet, or we're looking for an inline style
            // so keep looking up the parent chain.
            //
            do {
                parent = parent.getParent();
                nlooks += 1;
                helper = parent != null ? parent.impl_getStyleHelper() : null;
            } while (parent != null && helper == null);
        }
        
        if (csShorthand != null && returnValue != null) {
       
            final boolean shorthandImportant = 
                    csShorthand.getStyle().getDeclaration().isImportant();
            
            final Style style = returnValue.getStyle();
            final boolean returnValueIsImportant = 
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
                if (shorthandImportant == true && returnValueIsImportant == false) {
                    returnValue = null;
                } 

            } else if (csShorthand.compareTo(returnValue) < 0) {
                //
                // CascadingStyle.compareTo is such that a return value less
                // than zero means the style is more specific than the arg.
                // So if csShorthand is more specific than the return value,
                // return null.
                // 
                // if we found font sub-property at the same distance from the
                // node as the font shortand, then do a normal compare
                // to see if the sub-property is more specific. If it isn't
                // then return null.
                //
                returnValue = null;
                        
            }

        }
        
        return returnValue;
        
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
    private CalculatedValue lookupFont(Node node, CssMetaData styleable, 
            boolean isUserSet, Node originatingNode, 
            CacheEntry cacheEntry, List<Style> styleList) {

        // To make the code easier to read, define CONSIDER_INLINE_STYLES as true. 
        // Passed to lookupFontSubProperty to tell it whether or not 
        // it should look for inline styles
        final boolean CONSIDER_INLINE_STYLES = true; 
        
        Origin origin = null;
        boolean isRelative = false;
            
        // distance keeps track of how far up the parent chain we had to go
        // to find a font shorthand. We'll look no further than this
        // for the missing pieces. nlooks is used to keep track of how 
        // many parents we've looked at. nlooks should never exceed distance. 
        int distance = 0, nlooks = 0;
        
        Node parent = node;
        StyleHelper helper = this;
        String property = styleable == null ? null : styleable.getProperty();
        CascadingStyle csShorthand = null;
        while (parent != null) {
            
            final long states = helper.getPseudoClassState();
            final Map<String,CascadingStyle> inlineStyles = StyleHelper.getInlineStyleMap(parent);
            
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
                // If isUserSet is false, then take the style if we found one.
                // If csShorthand is not null, then were looking for an inline
                // style. In this case, if the origin is INLINE, we'll take it.
                // If it isn't INLINE we'll keep looking for an INLINE style.
                //
                final boolean isInline = cascadingStyle.getOrigin() == Origin.INLINE;
                if (csShorthand == null || isInline) {
                    origin = cascadingStyle.getOrigin();
                    csShorthand = cascadingStyle;
                    distance = nlooks;
                    
                    if (isInline) {
                        break;
                    }
                } 
                
                
            } 
            
            //
            // If were here, then we either didn't find a style or we did find
            // one but it wasn't inline. Either way, we need to keep looking
            // up the parent chain for the next -fx-font.
            //
            do {
                parent = parent.getParent();
                nlooks += 1;
                helper = parent != null ? parent.impl_getStyleHelper() : null;
            } while (parent != null && helper == null);

        }

        if (csShorthand == null) {
            distance = nlooks;
        }
        nlooks = 0;
        
        final long states = getPseudoClassState();
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
            
            // UA < AUTHOR < INLINE
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
                isUserSet, csShorthand, distance, CONSIDER_INLINE_STYLES)) != null) {

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
                isUserSet, csShorthand, distance, CONSIDER_INLINE_STYLES))!= null) {
       
            if (styleList != null) {
                styleList.add(csSize.getStyle());
            }

            final CalculatedValue cv = 
                calculateValue(csSize, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            // UA < AUTHOR < INLINE
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
                isUserSet, csShorthand, distance, CONSIDER_INLINE_STYLES))!= null) {

            if (styleList != null) {
                styleList.add(csWeight.getStyle());
            }
            
            final CalculatedValue cv = 
                calculateValue(csWeight, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            // UA < AUTHOR < INLINE
            if (origin == null || origin.compareTo(cv.origin) <= 0) {                        
                if (cv.value instanceof FontWeight) {
                    weight = (FontWeight)cv.value;
                    origin = cv.origin;
                }
            }
                
        }

        CascadingStyle csStyle = null;
        if ((csStyle = lookupFontSubPropertyStyle(node, property+"-style",
                isUserSet, csShorthand, distance, CONSIDER_INLINE_STYLES))!= null) {
            
            if (styleList != null) {
                styleList.add(csStyle.getStyle());
            }
            
            final CalculatedValue cv = 
                calculateValue(csStyle, node, styleable, states, inlineStyles, 
                    originatingNode, cacheEntry, styleList);

            // UA < AUTHOR < INLINE
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
     * Called from CssMetaData getMatchingStyles
     * @param node
     * @param styleableProperty
     * @return 
     */
    List<Style> getMatchingStyles(Styleable node, CssMetaData styleableProperty) {
        
        final List<CascadingStyle> styleList = new ArrayList<CascadingStyle>();

        getMatchingStyles(node, styleableProperty, styleList);

        List<CssMetaData> subProperties = styleableProperty.getSubProperties();
        if (subProperties != null) {
            for (int n=0,nMax=subProperties.size(); n<nMax; n++) {
                final CssMetaData subProperty = subProperties.get(n);
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
    
    private void getMatchingStyles(Styleable node, CssMetaData styleableProperty, List<CascadingStyle> styleList) {
        
        if (node != null) {
            
            Map<String,List<CascadingStyle>> inlineStyleMap = null;
            
            // create a map of inline styles.
            Styleable parent = node;
            while (parent != null) {
                
                StyleHelper parentHelper = parent.getNode() != null
                        ? parent.getNode().impl_getStyleHelper()
                        : null;
                
                if (parentHelper != null) {
                    
                    Map<String,CascadingStyle> inlineStyles = StyleHelper.getInlineStyleMap(parent);
                    
                    if (inlineStyles != null) {
                        
                        if (inlineStyleMap == null) {
                            inlineStyleMap = new HashMap<String,List<CascadingStyle>>();
                        }
                        
                        for(Entry<String,CascadingStyle> entry : inlineStyles.entrySet()) {                            
                            String kee = entry.getKey();
                            
                            List<CascadingStyle> inlineStyleList = inlineStyleMap.get(kee);
                            if (inlineStyleList == null) {
                                inlineStyleList = new ArrayList<CascadingStyle>();
                                inlineStyleMap.put(kee, inlineStyleList);
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
                        
                        final Map<String, List<CascadingStyle>> smap = helper.getStyleMap();
                        if (smap != null) {

                            List<CascadingStyle> styles = smap.get(property);

                            if (styles != null) {
                                styleList.addAll(styles);
                            }

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
