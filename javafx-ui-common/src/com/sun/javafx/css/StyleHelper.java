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
import com.sun.javafx.css.StyleHelper.StyleCacheKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.logging.PlatformLogger;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map.Entry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;

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
    private static final Object SKIP = new int[0];

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
    public static StyleHelper create(List<CascadingStyle> styles, long pseudoclassStateMask, int helperIndex) {

        //
        // Note: nhelpers must start at one, not zero, or multiplier in
        // Node.impl_getStyleCacheKey will be zero, causing a divide by zero.
        //
        final StyleHelper helper = new StyleHelper(styles, pseudoclassStateMask, helperIndex);
       return helper;
    }

    private final int helperIndex;
    public int getHelperIndex() { return helperIndex; }

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
     * The key is comprised of this helper's index, plus the
     * indices of all this node's parents' helper's indices.
     * We know that the indices are contiguous so we can
     * construct a unique key from the indices. If the indices
     * are [1, 2, 3], then the unique key will be 123. If
     * the indices are [50, 2, 18], then the unique key will be
     *  50218.
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
     */
    Map<Reference<StyleCacheKey>, List<CacheEntry>> valueCache;

    /**
     * Called from Node.impl_createStyleHelper before a getting a new
     * StyleHelper for a Node. The impl_createStyleHelper method is called
     * when CSS needs to be reapplied. CSS is reapplied when the CSS structure
     * changes - a stylesheet is added, a style is changed, a node is added
     * to a parent, etc. Since the CSS structure has changed, the cached
     * values are invalid.
     */
    public void clearCachedValues(Reference<StyleCacheKey> key) {
        if (key != null && valueCache != null) {
            List<CacheEntry> entries = valueCache.remove(key);
            if (entries == null || entries.isEmpty()) return;
            for(CacheEntry entry : entries) {
                if (entry.values != null) entry.values.clear();
            }
            key.clear();
            entries.clear();
        }
    }

    /** 
     * This method will either create a new StyleCacheKey for the node
     * or will return an existing one. It may return null.
     */
    public Reference<StyleCacheKey> createStyleCacheKey(Node node) {

        int[] indices = StyleCacheKey.getIndices(node, 0);

        // Note: valueCache will not be null. The valueCache is owned
        // by the StylesheetContainer (see StyleManager) and is set when the
        // stylehelper is created.
        assert(valueCache != null);

        //
        // Look to see if there is already a cache for this set of helpers
        //

        // If we encounter a key with a null referent, then the key is added
        // to a list and then removed later in order to avoid concurrent mod.
        List<Reference<StyleCacheKey>> keysToRemove = null;

        Reference<StyleCacheKey> existingKeyRef = null;

        for(Reference<StyleCacheKey> keyRef : valueCache.keySet()) {
            final StyleCacheKey key = keyRef.get();
            if (key == null) {
                if (keysToRemove == null) keysToRemove = new ArrayList<Reference<StyleCacheKey>>();
                keysToRemove.add(keyRef);
                continue;
            }
            if (Arrays.equals(key.indices, indices)) {
                existingKeyRef = keyRef;
                break;
            }
        }

        if (keysToRemove != null) {
            for (Reference<StyleCacheKey> keyRef : keysToRemove) {
                for(CacheEntry entry : valueCache.remove(keyRef)) {
                    entry.values.clear();
                }
            }
        }

        if (existingKeyRef != null) return existingKeyRef;

        // The List<CacheEntry> should only contain entries for those
        // pseudoclass states that have styles. The StyleHelper's
        // pseudoclassStateMask is a bitmask of those pseudoclasses that
        // appear in the node's StyleHelper's smap. This list of
        // pseudoclass masks is held by the StyleCacheKey. When a node is
        // styled, its pseudoclasses and the pseudoclasses of its parents
        // are gotten. By comparing the actual pseudoclass state to the
        // pseudoclass states that apply, a CacheEntry can be created or
        // fetched using only those pseudoclasses that matter.
        long[] pclassMasks = new long[indices.length];
        pclassMasks[0] = pseudoclassStateMask;
        int n = 1;
        Node parent = node.getParent();
        while (parent != null) {
            StyleHelper parentHelper = parent.impl_getStyleHelper();
            pclassMasks[n++] =
                (parentHelper != null) ? parentHelper.pseudoclassStateMask : 0;
            parent = parent.getParent();
        }

        // No existing cache was found for this set of helpers.
        Reference<StyleCacheKey> keyRef =
            new WeakReference<StyleCacheKey>(new StyleCacheKey(indices, pclassMasks));
        valueCache.put(keyRef, new ArrayList<CacheEntry>());
        return keyRef;
    }

    /**
     * There should only ever be one instance of a StyleCacheKey for a given
     * set of indices. See the createStyleCacheKey method.
     */
    public final static class StyleCacheKey {
        private final int[] indices;
        // see comments in createStyleCacheKey
        private final long[] pclassMask;

        private StyleCacheKey(int[] indices, long[] pclassMask) {
            this.indices = indices;
            this.pclassMask = pclassMask;
        }

        private static int[] getIndices(Node node, int count) {
            if (node == null) return new int[count];
            int[] indices = getIndices(node.getParent(), ++count);
            StyleHelper sh = node.impl_getStyleHelper();
            indices[count-1] = (sh != null) ? sh.helperIndex : 0;
            return indices;
        }
    }


    /**
     * An entry in the valueCache. See the valueCache comments for more.
     */
    static class CacheEntry {

        final long[] states;
        final Map<String,CalculatedValue> values;

        CacheEntry(long[] states) {
            this.states = states;
            this.values = new HashMap<String,CalculatedValue>();
        }

    }


    private CacheEntry getCacheEntry(Node node) {
        return getCacheEntry(node, getAllPseudoClassStates(node,0));
    }
    
    private CacheEntry getCacheEntry(Node node, long[] states) {
        //
        // The key is created from this StyleHelper's index
        // and the index of all the StyleHelpers of the Node's parents.
        // The key is unique to this set of StyleHelpers.
        //
        final Reference<StyleCacheKey> keyRef = node.impl_getStyleCacheKey();

        final List<CacheEntry> cachedValues = valueCache.get(keyRef);
        if (cachedValues == null) return null;

        //
        // Find the entry in the list that matches the states
        //
        StyleCacheKey key = keyRef.get();
        if(key == null) {
            for (CacheEntry ce : cachedValues) ce.values.clear();
            valueCache.remove(keyRef);
            return null;
        }
        
        // pclassMask is the set of pseudoclasses that appear in the
        // style maps of this set of StyleHelpers. Calculated values are
        // cached by pseudoclass state, but only the pseudoclass states
        // that mater are used in the search.
        final long[] pclassMask = new long[key.pclassMask.length];
        assert (pclassMask.length == states.length);

        for (int n=0; n<pclassMask.length; n++) {
            pclassMask[n] = key.pclassMask[n] & states[n];
        }

        // the return value...
        CacheEntry cacheEntry = null;

        for (int n=0, max=cachedValues.size(); n<max; n++) {
            final CacheEntry vce = cachedValues.get(n);
            final long[] vceStates = vce.states;
            if (Arrays.equals(pclassMask, vceStates)) {
                cacheEntry = vce;
                break;
            }
        }

        if (cacheEntry == null) {
            cacheEntry = new CacheEntry(pclassMask);
            cachedValues.add(cacheEntry);
        }

        return cacheEntry;
    }

    /* 
     * The lookup function return an Object but also
     * needs to return whether or not the value is cacheable.
     * 
     * isCacheable is true if there are no lookups, or if the resolved lookups
     * did not come from Node.style.
     * 
     */
    private static class CalculatedValue {
        final Object value;
        final Stylesheet.Origin origin;
        final boolean isCacheable;

        CalculatedValue(Object value, Stylesheet.Origin origin, boolean isCacheable) {
            
            this.value = value;            
            this.origin = origin;
            this.isCacheable = isCacheable;            
            
        }
    }
    
    /**
     * A map from Property Name (String) => List of Styles. This shortens
     * the lookup time for each property.
     */
    private final Map<String, List<CascadingStyle>> smap;

    void clearStyleMap() {

        for(List<CascadingStyle> styles : smap.values()) {
            styles.clear();
        }
        smap.clear();
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

    /**
     * Creates a new StyleHelper.
     *
     * @param pseudoclassStateMask A set of bits for each pseudoclass
     *        who's change may impact children of this node. This is used as
     *        an important performance optimization so that pseudoclass state
     *        changes which do not affect any children do not get handled.
     */
    private StyleHelper(List<CascadingStyle> styles, long pseudoclassStateMask, int index) {
        this.pseudoclassStateMask = pseudoclassStateMask;
        this.smap = new HashMap<String, List<CascadingStyle>>();
        this.helperIndex = index;

        final int max = styles != null ? styles.size() : 0;
        for (int i=0; i<max; i++) {
            CascadingStyle style = styles.get(i);
            final String property = style.getProperty();
            // This is carefully written to use the minimal amount of hashing.
            List<CascadingStyle> list = smap.get(property);
            if (list == null) {
                list = new ArrayList<CascadingStyle>(5);
                smap.put(property, list);
            }
            list.add(style);
        }

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
    private Map<String,CascadingStyle> getStyles(Node node) {

        return getInlineStyles(node.getStyle());
    }

    /**
     * Get the mapping of property to style from Node.style for this node.
     */
    private Map<String,CascadingStyle> getStyles(Styleable node) {
        return getInlineStyles(node.getStyle());
    }
    
    private Map<String,CascadingStyle> getInlineStyles(final String style) {

        // If there are no styles for this property then we can just bail
        if ((style == null) || style.isEmpty()) return null;

        Map<String,CascadingStyle> styles = authorStylesCache.get(style);
        
        if (styles == null) {

            Stylesheet authorStylesheet =
                CSSParser.getInstance().parseStyle(style);
            if (authorStylesheet != null) {
                authorStylesheet.setOrigin(Stylesheet.Origin.INLINE);
            }
            styles = getStyles(authorStylesheet);
            
            authorStylesCache.put(style, styles);
        }

        return styles;
    }

    /**
     * Reusable list for pseudoclass states
     */
//    private List<String> pseudoClassStates = null;

    // cache pseudoclass state. cleared on each pulse.
    final static Map<Node,Long> pseudoclassMasksByNode = new HashMap<Node,Long>();

    /** 
     * Get pseudoclass states for the given node
     */
    long getPseudoClassState(Node node) {

        if (pseudoclassMasksByNode.containsKey(node)) {
            Long cachedMask = pseudoclassMasksByNode.get(node);
            return cachedMask.longValue();
    }
    
        long stateMask = 0;
        if (pseudoclassStateMask != 0) {
            stateMask = node.impl_getPseudoClassState();
        }

        pseudoclassMasksByNode.put(node, Long.valueOf(stateMask));

        return stateMask;
    }

    private static long[] getAllPseudoClassStates(Node node, int count) {
        if (node == null) return new long[count];
        long[] states = getAllPseudoClassStates(node.getParent(), ++count);
        StyleHelper sh = node.impl_getStyleHelper();
        states[count-1] = (sh != null) ? sh.getPseudoClassState(node) : 0;
        return states;
    }

    private static StyleableProperty FONT = new StyleableProperty.FONT<Node>("-fx-font", Font.getDefault()) {

        @Override
        public boolean isSettable(Node node) {
            assert(false);
            return false;
        }

        @Override
        public WritableValue<Font> getWritableValue(Node node) {
            assert(false);
            return null;
        }
    };
            
    private Font getInheritedFont(Node node, Map<String,CascadingStyle> userStyles, 
            CacheEntry cacheEntry, List<Style> styleList) {
        if (node == null) return Font.getDefault();
        StyleHelper helper = node.impl_getStyleHelper();
        return (helper != null) 
                ? helper.inheritFont(node, userStyles, cacheEntry, styleList) 
                : Font.getDefault();
    }

    private Font inheritFont(Node node, Map<String,CascadingStyle> userStyles, 
            CacheEntry cacheEntry, List<Style> styleList) {

        StyleableProperty fontKey = FONT;

        final List<StyleableProperty> styleables =
                StyleableProperty.getStyleables(node);

        for(int n=0, max=styleables.size(); n<max; n++) {
            // Instance equality is fine here!
            final StyleableProperty styleable = styleables.get(n);
            if (styleable.getConverter() == FontConverter.getInstance()) {
                fontKey = styleable;
                break;
            }
        }

        //
        // need to distinguish the "inherited" font since it could resolve
        // to Font.getDefault() and we don't want that to look like it was
        // inherited from a style. Without this, if we stick -fx-font into 
        // the cache, then a lookup for some Node will return the default
        // font. The effect of this is seen in RT-15802.
        //
        final String property = fontKey.getProperty();
        final String specialProperty = "*inherited*".concat(property);
        final boolean fastPath = userStyles == null && styleList == null;
        if (fastPath) {
            // maybe the inherited font has already been resolved...
            CalculatedValue cv = cacheEntry.values.get(property);
            if (cv == null) {
                cv = cacheEntry.values.get(specialProperty);
            }
            if (cv != null && cv.value instanceof Font) return (Font)cv.value;
        }

        // Go looking for the font for this node
        // Pass null for originating node to avoid infinite loop
        final CalculatedValue inherited = 
            inherit(node, fontKey, userStyles, null, cacheEntry, styleList);
        // The inherited value might be SKIP for example, so check to make
        // sure that it really is a Font before casting it. If the inherited
        // value isn't a Font, then we will simply use the JavaFX default Font.
        boolean isFont = inherited.value instanceof Font;
        Font inheritedFont = (isFont) ? (Font)inherited.value : Font.getDefault();

        if (fastPath && inherited.isCacheable) {
            final CalculatedValue val =
                new CalculatedValue(inheritedFont, (isFont ? inherited.origin : null), isFont);
            cacheEntry.values.put((isFont ? property : specialProperty), val);
        }
        return inheritedFont;
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

        //
        // The ValueCacheEntry to choose depends on this Node's state and
        // the state of its parents. Without the parent state, the fact that
        // this node in this state matched foo:blah bar { } is lost.
        //
        final long[] allstates = getAllPseudoClassStates(node, 0);

        // allstates[0] is this node's state
        final long states = 
            (allstates != null && allstates.length > 0) ? allstates[0] : 0;

        //
        // userStyles is this node's Node.style. This is passed along and is
        // used in getStyle. Importance being the same, an author style will
        // trump a user style and a user style will trump a user_agent style.
        //
        final Map<String,CascadingStyle> userStyles = this.getStyles(node);

        //
        // Styles that need lookup can be cached provided none of the styles
        // are from Node.style.
        //
        final CacheEntry cacheEntry = getCacheEntry(node, allstates);
        if (cacheEntry == null) return;

        final List<StyleableProperty> styleables =
            StyleableProperty.getStyleables(node);

        //
        // if this node has a style map, then we'll populate it.
        // 
        final Map<WritableValue, List<Style>> styleMap = node.impl_getStyleMap();
        
        // Used in the for loop below, and a convenient place to stop when debugging.
        final int max = styleables.size();

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
            final boolean fastpath = 
                userStyles == null && cacheEntry != null && styleList == null;
            CalculatedValue calculatedValue = null;
            if (fastpath) {

                calculatedValue = cacheEntry.values.get(property);

                // RT-10522:
                // If the user set the property and there is a style and
                // the style came from the user agent stylesheet, then
                // skip the value. A style from a user agent stylesheet should
                // not override the user set style.
                if (calculatedValue != null &&
                    (calculatedValue.origin == Stylesheet.Origin.USER_AGENT) &&
                        isUserSetProperty(node, styleable)) {
                    continue;
                }

            }

            if (calculatedValue == null) {
                calculatedValue = lookup(node, styleable, states, userStyles, 
                        node, cacheEntry, styleList);

                if (fastpath && calculatedValue.isCacheable) {
                    // if userStyles is null and calculatedValue was null,
                    // then the calculatedValue didn't come from the cache
                    cacheEntry.values.put(property, calculatedValue);
                }

            }

            final Object value = calculatedValue.value;
            // If the CalculatedValue value is not SKIP then we will set it.
            if (value == SKIP) continue;
            
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
                    // TODO: use logger here
                    PlatformLogger logger = Logging.getCSSLogger();
                    if (logger.isLoggable(PlatformLogger.WARNING)) {
                        logger.warning(String.format("Failed to set css [%s]\n", styleable), e);
                    }
                }

            }
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
    private CalculatedValue lookup(Node node, StyleableProperty styleable, long states,
            Map<String,CascadingStyle> userStyles, Node originatingNode, 
            CacheEntry cacheEntry, List<Style> styleList) {

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

        if (style == null && numSubProperties == 0) {

            return handleNoStyleFound(node, styleable, userStyles, 
                    originatingNode, cacheEntry, styleList);

        } else if (style != null) {

            // RT-10522:
            // If the user set the property and there is a style and
            // the style came from the user agent stylesheet, then
            // skip the value. A style from a user agent stylesheet should
            // not override the user set style.
            if (style.getOrigin() == Stylesheet.Origin.USER_AGENT
                    && isUserSetProperty(originatingNode, styleable)) {
                return new CalculatedValue(SKIP, style.getOrigin(), true);
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

        // If style is null then it means we didn't successfully find the
        // property we were looking for. However, there might be sub styleables,
        // in which case we should perform a lookup for them. For example,
        // there might not be a style for "font", but there might be one
        // for "font-size" or "font-weight". So if the style is null, then
        // we need to check with the sub-styleables.
        if (style == null && numSubProperties > 0) {

            // Build up a list of all SubProperties which have a constituent part.
            // I default the array to be the size of the number of total
            // sub styleables to avoid having the array grow.
            Map<StyleableProperty,Object> subs = null;
            Stylesheet.Origin origin = null;
            boolean isCacheable = true;
            for (int i=0; i<numSubProperties; i++) {
                StyleableProperty subkey = subProperties.get(i);
                CalculatedValue constituent = 
                    lookup(node, subkey, states, userStyles, 
                        originatingNode, cacheEntry, styleList);
                if (constituent.value != SKIP) {
                    if (subs == null) {
                        subs = new HashMap<StyleableProperty,Object>();
                    }
                    subs.put(subkey, constituent.value);
                    isCacheable = isCacheable && constituent.isCacheable;
                    
                    // origin of this style is the most specific
                    if (origin == null || 
                        (constituent.origin != null && 
                            origin.compareTo(constituent.origin) < 0)) {
                        origin = constituent.origin;
                    }
                }
            }

            // If there are no subkeys which apply...
            if (subs == null || subs.isEmpty()) {
                return handleNoStyleFound(node, styleable, userStyles, 
                        originatingNode, cacheEntry, styleList);
            }

            try {
                final Object ret = keyType.convert(subs);
                return new CalculatedValue(ret, origin, isCacheable);
            } catch (ClassCastException cce) {
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("caught: ", cce);
                    LOGGER.warning("styleable = " + styleable);
                    LOGGER.warning("node = " + node.toString());
                }
                return new CalculatedValue(SKIP, null, true);
            }
        }
//        System.out.println("lookup " + property +
//                ", selector = \'" + style.selector.toString() + "\'" +
//                ", node = " + node.toString());

        if (styleList != null) { 
            styleList.add(style.getStyle());
        }
        
        // If the style was not null, then we need to check its value. The
        // value might not yet have been converted, in which case we need
        // to convert it and cache the converted value in the style.
        final ParsedValue cssValue = style.getParsedValue();
        if (style.value == null && cssValue != null && !("null").equals(cssValue.getValue())) {

            BooleanProperty cacheable = new SimpleBooleanProperty(true);
            final ParsedValue resolved = 
                resolveLookups(node, cssValue, cacheable, states, userStyles, styleList);
            
            try {
                // The computed value
                Object val = null;
                        
                Font font = resolved.isNeedsFont() 
                    ? getInheritedFont(originatingNode, userStyles, cacheEntry, styleList) 
                    : null;
                if (font == null) font = Font.getDefault();
                if (resolved.getConverter() != null)
                    val = resolved.convert(font);
                else
                    val = styleable.getConverter().convert(resolved, font);

                // We cannot store the computed val in the style.value in the
                // case of properties which had lookups
                final ParsedValue parsedValue = style.getParsedValue();
                if (!parsedValue.isLookup() && !parsedValue.isContainsLookups() &&
                    // RT-19192 - if -fx-font-size is relative, do not cache
                    !(resolved.isNeedsFont() && property.endsWith("font-size"))) {
                    style.value = val;
                }
                
                final Stylesheet.Origin origin = style.getOrigin();
                return new CalculatedValue(val, origin, cacheable.get());
                
            } catch (ClassCastException cce) {
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning(formatUnresolvedLookupMessage(node, styleable, style.getStyle(),resolved));
                    LOGGER.fine("node = " + node.toString());
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("styles = " + styleable.getMatchingStyles(node));
                }
                return new CalculatedValue(SKIP, null, true);
            } catch (IllegalArgumentException iae) {
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("caught: ", iae);
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("node = " + node.toString());
                }
                return new CalculatedValue(SKIP, null, true);
            } catch (NullPointerException npe) {
                if (LOGGER.isLoggable(PlatformLogger.WARNING)) {
                    LOGGER.warning("caught: ", npe);
                    LOGGER.fine("styleable = " + styleable);
                    LOGGER.fine("node = " + node.toString());
                }
                return new CalculatedValue(SKIP, null, true);
            } finally {
                resolved.nullResolved();
            }
                
            // We cannot store the computed val in the style.value in the
            // case of properties which had lookups
//            final ParsedValue parsedValue = style.getParsedValue();
//            if (!parsedValue.isLookup() && !parsedValue.isContainsLookups()) {
//                style.value = val;
//            }
//            final Stylesheet.Origin origin = style.getOrigin();
//            return new CalculatedValue(val, (origin != Stylesheet.Origin.INLINE), style.getStyle());
        }
        final Stylesheet.Origin origin = style.getOrigin();
        return new CalculatedValue(style.value, origin, (origin != Stylesheet.Origin.INLINE));
    }

    /** return true if the origin of the property is USER */
    private boolean isUserSetProperty(Node node, StyleableProperty styleable) {
        WritableValue writable = styleable.getWritableValue(node);
        // writable could be null if this is a sub-property
        Stylesheet.Origin origin = writable != null ? ((Property)writable).getOrigin() : null;
        return (origin == Stylesheet.Origin.USER);    
    }
    
    /**
     * Called when there is no style found.
     */
    private CalculatedValue handleNoStyleFound(Node node, StyleableProperty styleable,
            Map<String,CascadingStyle> userStyles, Node originatingNode, 
            CacheEntry cacheEntry, List<Style> styleList) {

        if (styleable.isInherits()) {

            CalculatedValue cv =
                inherit(node, styleable, userStyles, 
                    originatingNode, cacheEntry, styleList);

            if (cv.value == SKIP) return cv;

            // RT-10522
            // Did the user set this? The value is skipped if the user set the
            // property and the style comes from the usear agent stylesheet.
            else if (cv.origin == Stylesheet.Origin.USER_AGENT &&
                    isUserSetProperty(originatingNode, styleable)) {
                    return new CalculatedValue(SKIP, null, true);
            }

            // Not a SKIP, or something that can override a user set value,
            // or the user didn't set the value
            return cv;

        } else if (isUserSetProperty(originatingNode, styleable)) {

            // Not inherited. There is no style but we don't want to
            // set the default value if the user set the property
            return new CalculatedValue(SKIP, null, true);

        } else if (smap.containsKey(styleable.getProperty())) {

            // If there is a style in the stylemap but it just doen't apply,
            // then it may have been set and it needs to be reset to its
            // default value. For example, if there is a style for the hover
            // pseudoclass state, but no style for the default state.
            Object initialValue = styleable.getInitialValue();
            
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

            return new CalculatedValue(SKIP, null, true);

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
            return new CalculatedValue(SKIP, null, true);
        }
        return parentStyleHelper.lookup(parent, styleable,
                parentStyleHelper.getPseudoClassState(parent),
                getStyles(parent), originatingNode, cacheEntry, styleList);
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
                        parentStyleHelper.getPseudoClassState(parent),
                        getStyles(parent));
            }
        }
    }

    // to resolve a lookup, we just need to find the parsed value.
    private ParsedValue resolveLookups(Node node, ParsedValue value, BooleanProperty cacheable, long states,
            Map<String,CascadingStyle> userStyles, List<Style> styleList) {
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
                    
                    if (cacheable.get()) {
                        cacheable.set(resolved.getOrigin() != Stylesheet.Origin.INLINE);
                    }
                    // the resolved value may itself need to be resolved.
                    // For example, if the value "color" resolves to "base",
                    // then "base" will need to be resolved as well.
                    return resolveLookups(node, resolved.getParsedValue(), cacheable, states, userStyles, styleList);
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
                        resolveLookups(node, layers[l][ll], cacheable, states, userStyles, styleList);
                }
            }

        } else if (val instanceof ParsedValue[]) {
        // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layer = (ParsedValue[])val;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                layer[l].resolved =
                    resolveLookups(node, layer[l], cacheable, states, userStyles, styleList);
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
        } else if (stylesheet != null && Stylesheet.Origin.INLINE == stylesheet.getOrigin()) {
            sbuf.append(" from inline style on " )
                .append(node.toString());            
        }
        
        return sbuf.toString();
    }

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
                
                StyleHelper parentHelper = parent.getStyleHelper();
                
                if (parentHelper != null) {
                    
                    Map<String,CascadingStyle> inlineStyles = parentHelper.getStyles(parent);
                    
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
            List<CascadingStyle> styles = smap.get(property);

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
                    StyleHelper parentHelper = parent.getStyleHelper();
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
                    final StyleHelper helper = parent.getStyleHelper();
                    if (helper != null) {
                                             
                        final int start = styleList.size();
                        
                        List<CascadingStyle> styles = helper.smap.get(property);
                        
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
