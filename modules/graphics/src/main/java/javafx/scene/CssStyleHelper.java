/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.javafx.scene.CssFlags;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import com.sun.javafx.Logging;
import com.sun.javafx.Utils;
import com.sun.javafx.css.CalculatedValue;
import com.sun.javafx.css.CascadingStyle;
import com.sun.javafx.css.CssError;
import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Selector;
import com.sun.javafx.css.Style;
import com.sun.javafx.css.StyleCache;
import com.sun.javafx.css.StyleCacheEntry;
import com.sun.javafx.css.StyleConverterImpl;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleMap;
import com.sun.javafx.css.Stylesheet;
import com.sun.javafx.css.converters.FontConverter;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

import static com.sun.javafx.css.CalculatedValue.*;

/**
 * The StyleHelper is a helper class used for applying CSS information to Nodes.
 */
final class CssStyleHelper {

    private static final PlatformLogger LOGGER = com.sun.javafx.Logging.getCSSLogger();

    private CssStyleHelper() {
        this.triggerStates = new PseudoClassState();
    }

    /**
     * Creates a new StyleHelper.
     */
    static CssStyleHelper createStyleHelper(final Node node, WritableValue<Boolean> clearCacheOnReuse, ObservableMap<StyleableProperty<?>, List<Style>> styleObserver) {

        // need to know how far we are to root in order to init arrays.
        // TODO: should we hang onto depth to avoid this nonsense later?
        // TODO: is there some other way of knowing how far from the root a node is?
        Styleable parent = node;
        int depth = 0;
        while(parent != null) {
            depth++;
            parent = parent.getStyleableParent();
        }

        // The List<CacheEntry> should only contain entries for those
        // pseudo-class states that have styles. The StyleHelper's
        // pseudoclassStateMask is a bitmask of those pseudoclasses that
        // appear in the node's StyleHelper's smap. This list of
        // pseudo-class masks is held by the StyleCacheKey. When a node is
        // styled, its pseudoclasses and the pseudoclasses of its parents
        // are gotten. By comparing the actual pseudo-class state to the
        // pseudo-class states that apply, a CacheEntry can be created or
        // fetched using only those pseudoclasses that matter.
        final PseudoClassState[] triggerStates = new PseudoClassState[depth];

        final StyleMap styleMap =
                StyleManager.getInstance().findMatchingStyles(node, triggerStates);

        //
        // reuse the existing styleHelper if possible.
        //
        if ( canReuseStyleHelper(node, styleMap) ) {

            //
            // RT-33080
            //
            // If we're reusing a style helper, clear the fontSizeCache in case either this node or some parent
            // node has changed font from a user calling setFont.
            //
            // It may be the case that the node's font has changed from a call to setFont, which will
            // trigger a REAPPLY. If the REAPPLY comes because of a change in font, then the fontSizeCache
            // needs to be invalidated (cleared) so that new values will be looked up for all transition states.
            //
            if (node.styleHelper.cacheContainer != null &&
                    (isTrue(clearCacheOnReuse) || isUserSetFont(node, node.styleHelper.cacheContainer.fontProp))) {
                setTrue(clearCacheOnReuse);
                node.styleHelper.cacheContainer.fontSizeCache.clear();
            }
            return node.styleHelper;

        }

        if (styleMap == null || styleMap.isEmpty()) {

            boolean mightInherit = false;

            final List<CssMetaData<? extends Styleable, ?>> props = node.getCssMetaData();

            final int pMax = props != null ? props.size() : 0;
            for (int p=0; p<pMax; p++) {

                final CssMetaData<? extends Styleable, ?> prop = props.get(p);
                if (prop.isInherits()) {
                    mightInherit = true;
                    break;
                }
            }

            if (mightInherit == false) {

                //
                // If there is an observer listening for css style changes, then that listener needs to be
                // preserved, even if there aren't any changes. At some point in the future, this node might
                // have styles and the observer should start notifying of changes.
                //
                // So, if there isn't an observer listening for css style changes, we can just return null here.
                // If there is, then we'll fall through and create a new StyleHelper which will be assigned
                // the current StyleHelper's observer.
                //
                if ((styleObserver == null) || (FXCollections.emptyObservableMap() == (ObservableMap)styleObserver)) {

                    // If this node had a style helper, then reset properties to their initial value
                    // since the node won't have a style helper after this call
                    if (node.styleHelper != null) {
                        node.styleHelper.resetToInitialValues(node);
                    }

                    //
                    // This node didn't have a StyleHelper before and it doesn't need one now since there are
                    // no styles in the StyleMap and no inherited styles.
                    return null;
                }
            }

        }

        final CssStyleHelper helper = new CssStyleHelper();
        helper.triggerStates.addAll(triggerStates[0]);

        // make sure parent's transition states include the pseudo-classes
        // found when matching selectors
        parent = node.getStyleableParent();
        for(int n=1; n<depth; n++) {

            // TODO: this means that a style like .menu-item:hover won't work. Need to separate CssStyleHelper tree from scene-graph tree
            if (parent instanceof Node == false) {
                parent=parent.getStyleableParent();
                continue;
            }
            Node parentNode = (Node)parent;

            final PseudoClassState triggerState = triggerStates[n];

            // if there is nothing in triggerState, then continue since there
            // isn't any pseudo-class state that might trigger a state change
            if (triggerState != null && triggerState.size() > 0) {

                // Create a StyleHelper for the parent, if necessary.
                if (parentNode.styleHelper == null) {
                    parentNode.styleHelper = new CssStyleHelper();
                }
                parentNode.styleHelper.triggerStates.addAll(triggerState);

            }

            parent=parent.getStyleableParent();
        }

        helper.cacheContainer = new CacheContainer(node, styleMap, depth);

        // If this node had a style helper, then reset properties to their initial value
        // since the style map might now be different
        if (node.styleHelper != null) {
            node.styleHelper.resetToInitialValues(node);
        }

        // Don't lose track of the observer, if any.
        helper.observableStyleMap = styleObserver;

        return helper;
    }

    //
    // return true if the fontStyleableProperty's origin is USER
    //
    private static boolean isUserSetFont(Styleable node, CssMetaData<Styleable, Font> fontCssMetaData) {
        StyleableProperty<Font> fontStyleableProperty = fontCssMetaData != null ? fontCssMetaData.getStyleableProperty(node) : null;
        return fontStyleableProperty != null && fontStyleableProperty.getStyleOrigin() == StyleOrigin.USER;
    }

    //
    // return the value of the property
    //
    private static boolean isTrue(WritableValue<Boolean> booleanProperty) {
        return booleanProperty != null && booleanProperty.getValue();
    }

    //
    // set the value of the property to true
    //
    private static void setTrue(WritableValue<Boolean> booleanProperty) {
        if (booleanProperty != null) booleanProperty.setValue(true);
    }

    //
    // return true if the Node's current styleHelper can be reused.
    //
    private static boolean canReuseStyleHelper(final Node node, final StyleMap styleMap) {

        // Obviously, we cannot reuse the node's style helper if it doesn't have one.
        if (node == null || node.styleHelper == null) {
            return false;
        }

        // If we have a styleHelper but the new styleMap is null, then we don't need a styleHelper at all
        if (styleMap == null) {
            return false;
        }

        StyleMap currentMap = node.styleHelper.getStyleMap(node);

        // We cannot reuse the style helper if the styleMap is not the same instance as the current one
        // Note: check instance equality!
        if (currentMap != styleMap) {
            return false;
        }

        // If the style maps are the same instance, we can re-use the current styleHelper if the cacheContainer is null.
        // Under this condition, there are no styles for this node _and_ no styles inherit.
        if (node.styleHelper.cacheContainer == null) {
            return true;
        }

        //
        // The current map might be the same, but one of the node's parent's maps might have changed which
        // might cause some calculated values to change. To see if we can re-use the style-helper, we need to
        // check if the StyleMap id's have changed, which we can do by inspecting the cacheContainer's styleCacheKey
        // since it is made up of the current set of StyleMap ids.
        //

        CssStyleHelper parentHelper = null;
        Styleable parent = node.getStyleableParent();

        // if the node's parent is null and the style maps are the same, then we can certainly reuse the style-helper
        if (parent == null) {
            return true;
        }

        while (parent != null) {
            if (parent instanceof Node) {
                parentHelper = ((Node) parent).styleHelper;
                if (parentHelper != null) break;
            }
            parent = parent.getStyleableParent();
        }

        if (parentHelper != null && parentHelper.cacheContainer != null) {

            int[] parentIds = parentHelper.cacheContainer.styleCacheKey.getStyleMapIds();
            int[] nodeIds = node.styleHelper.cacheContainer.styleCacheKey.getStyleMapIds();

            if (parentIds.length == nodeIds.length - 1) {

                boolean isSame = true;

                // check that all of the style map ids are the same.
                for (int i = 0; i < parentIds.length; i++) {
                    if (nodeIds[i + 1] != parentIds[i]) {
                        isSame = false;
                        break;
                    }
                }

                return isSame;

            }
        }

        return false;
    }

    private CacheContainer cacheContainer;

    private final static class CacheContainer {

        // Set internal internalState structures
        private CacheContainer(
                Node node,
                final StyleMap styleMap,
                int depth) {

            int ctr = 0;
            int[] smapIds = new int[depth];
            smapIds[ctr++] = this.smapId = styleMap.getId();

            //
            // Create a set of StyleMap id's from the parent's smapIds.
            // The resulting smapIds array may have less than depth elements.
            // If a parent doesn't have a styleHelper or the styleHelper's
            // internal state is null, then that parent doesn't contribute
            // to the selection of a style. Any Node that has the same
            // set of smapId's can potentially share previously calculated
            // values.
            //
            Styleable parent = node.getStyleableParent();
            for(int d=1; d<depth; d++) {

                // TODO: won't work for something like .menu-item:hover. Need to separate CssStyleHelper tree from scene-graph tree
                if ( parent instanceof Node) {
                    Node parentNode = (Node)parent;
                final CssStyleHelper helper = parentNode.styleHelper;
                    if (helper != null && helper.cacheContainer != null) {
                        smapIds[ctr++] = helper.cacheContainer.smapId;
                    }
                }
                parent = parent.getStyleableParent();

            }

            this.styleCacheKey = new StyleCache.Key(smapIds, ctr);

            CssMetaData<Styleable,Font> styleableFontProperty = null;

            final List<CssMetaData<? extends Styleable, ?>> props = node.getCssMetaData();
            final int pMax = props != null ? props.size() : 0;
            for (int p=0; p<pMax; p++) {
                final CssMetaData<? extends Styleable, ?> prop = props.get(p);

                if ("-fx-font".equals(prop.getProperty())) {
                    // unchecked!
                    styleableFontProperty = (CssMetaData<Styleable, Font>) prop;
                    break;
                }
            }

            this.fontProp = styleableFontProperty;
            this.fontSizeCache = new HashMap<>();

            this.cssSetProperties = new HashMap<>();

        }

        private StyleMap getStyleMap(Styleable styleable) {
            if (styleable != null) {
                return StyleManager.getInstance().getStyleMap(styleable, smapId);
            } else {
                return StyleMap.EMPTY_MAP;
            }

        }

        // This is the key we use to find the shared cache
        private final StyleCache.Key styleCacheKey;

        // If the node has a fontProperty, we hang onto the CssMetaData for it
        // so we can get at it later.
        // TBD - why not the fontProperty itself?
        private final CssMetaData<Styleable,Font> fontProp;

        // The id of StyleMap that contains the styles that apply to this node
        private final int smapId;

        // All nodes with the same set of styles share the same cache of
        // calculated values. But one node might have a different font-size
        // than another so the values are stored in cache by font-size.
        // This map associates a style cache entry with the font to use when
        // getting a value from or putting a value into cache.
        private final Map<StyleCacheEntry.Key, CalculatedValue> fontSizeCache;

        // Any properties that have been set by this style helper are tracked
        // here so the property can be reset without expanding properties that
        // were not set by css.
        private Map<CssMetaData, CalculatedValue> cssSetProperties;
    }

    private void resetToInitialValues(final Styleable styleable) {

        if (cacheContainer == null ||
                cacheContainer.cssSetProperties == null ||
                cacheContainer.cssSetProperties.isEmpty()) return;

        // RT-31714 - make a copy of the entry set and clear the cssSetProperties immediately.
        Set<Entry<CssMetaData, CalculatedValue>> entrySet = new HashSet<>(cacheContainer.cssSetProperties.entrySet());
        cacheContainer.cssSetProperties.clear();

        for (Entry<CssMetaData, CalculatedValue> resetValues : entrySet) {

            final CssMetaData metaData = resetValues.getKey();
            final StyleableProperty styleableProperty = metaData.getStyleableProperty(styleable);

            final StyleOrigin styleOrigin = styleableProperty.getStyleOrigin();
            if (styleOrigin != null && styleOrigin != StyleOrigin.USER) {
                final CalculatedValue calculatedValue = resetValues.getValue();
                styleableProperty.applyStyle(calculatedValue.getOrigin(), calculatedValue.getValue());
            }
        }
    }


    private StyleMap getStyleMap(Styleable styleable) {
        if (cacheContainer == null || styleable == null) return null;
        return cacheContainer.getStyleMap(styleable);
    }

    /**
     * A Set of all the pseudo-class states which, if they change, need to
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
     * button and all children to be update. Other pseudo-class state changes
     * that are not in this hash set are ignored.
     * *
     * Called "triggerStates" since they would trigger a CSS update.
     */
    private PseudoClassState triggerStates = new PseudoClassState();

    boolean pseudoClassStateChanged(PseudoClass pseudoClass) {
        return triggerStates.contains(pseudoClass);
    }

    /**
     * Dynamic pseudo-class state of the node and its parents.
     * Only valid during a pulse.
     *
     * The StyleCacheEntry to choose depends on the Node's pseudo-class state
     * and the pseudo-class state of its parents. Without the parent
     * pseudo-class state, the fact that the the node in this pseudo-class state
     * matched foo:blah bar { } is lost.
     */
    // TODO: this should work on Styleable, not Node
    private Set<PseudoClass>[] getTransitionStates(final Node node) {

        // if cacheContainer is null, then CSS just doesn't apply to this node
        if (cacheContainer == null) return null;

        int depth = 0;
        Node parent = node;
        while (parent != null) {
            depth += 1;
            parent = parent.getParent();
        }

        //
        // StyleHelper#triggerStates is the set of pseudo-classes that appear
        // in the style maps of this StyleHelper. Calculated values are
        // cached by pseudo-class state, but only the pseudo-class states
        // that mater are used in the search. So we take the transition states
        // and intersect them with triggerStates to remove the
        // transition states that don't matter when it comes to matching states
        // on a  selector. For example if the style map contains only
        // .foo:hover { -fx-fill: red; } then only the hover state matters
        // but the transtion state could be [hover, focused]
        //
        final Set<PseudoClass>[] retainedStates = new PseudoClassState[depth];

        //
        // Note Well: The array runs from leaf to root. That is,
        // retainedStates[0] is the pseudo-class state for node and
        // retainedStates[1..(states.length-1)] are the retainedStates for the
        // node's parents.
        //

        int count = 0;
        parent = node;
        while (parent != null) {
            final CssStyleHelper helper = (parent instanceof Node) ? ((Node)parent).styleHelper : null;
            if (helper != null) {
                final Set<PseudoClass> pseudoClassState = ((Node)parent).pseudoClassStates;
                retainedStates[count] = new PseudoClassState();
                retainedStates[count].addAll(pseudoClassState);
                // retainAll method takes the intersection of pseudoClassState and helper.triggerStates
                retainedStates[count].retainAll(helper.triggerStates);
                count += 1;
            }
            parent = parent.getParent();
        }

        final Set<PseudoClass>[] transitionStates = new PseudoClassState[count];
        System.arraycopy(retainedStates, 0, transitionStates, 0, count);

        return transitionStates;

    }

    ObservableMap<StyleableProperty<?>, List<Style>> observableStyleMap;
     /**
      * RT-17293
      */
     ObservableMap<StyleableProperty<?>, List<Style>> getObservableStyleMap() {
         return (observableStyleMap != null)
             ? observableStyleMap
             : FXCollections.<StyleableProperty<?>, List<Style>>emptyObservableMap();
     }

     /**
      * RT-17293
      */
     void setObservableStyleMap(ObservableMap<StyleableProperty<?>, List<Style>> observableStyleMap) {
         this.observableStyleMap = observableStyleMap;
     }

    /**
     * Called by the Node whenever it has transitioned from one set of
     * pseudo-class states to another. This function will then lookup the
     * new values for each of the styleable variables on the Node, and
     * then either set the value directly or start an animation based on
     * how things are specified in the CSS file. Currently animation support
     * is disabled until the new parser comes online with support for
     * animations and that support is detectable via the API.
     */
    void transitionToState(final Node node, final CssFlags cssFlag) {

        if (cacheContainer == null) {
            return;
        }

        //
        // If styleMap is null, then StyleManager has blown it away and we need to reapply CSS.
        //
        final StyleMap styleMap = getStyleMap(node);
        if (styleMap == null) {
            cacheContainer = null;
            node.impl_reapplyCSS();
            return;
        }

        // if the style-map is empty, then we are only looking for inherited styles.
        final boolean inheritOnly = styleMap.isEmpty();

        //
        // Styles that need lookup can be cached provided none of the styles
        // are from Node.style.
        //
        final StyleCache sharedCache = StyleManager.getInstance().getSharedCache(node, cacheContainer.styleCacheKey);

        if (sharedCache == null) {
            // Shared cache was blown away by StyleManager.
            // Therefore, this CssStyleHelper is no good.
            cacheContainer = null;
            node.impl_reapplyCSS();
            return;

        }

        final Set<PseudoClass>[] transitionStates = getTransitionStates(node);

        final StyleCacheEntry.Key fontCacheKey = new StyleCacheEntry.Key(transitionStates, Font.getDefault());
        CalculatedValue cachedFont = cacheContainer.fontSizeCache.get(fontCacheKey);

        if (cachedFont == null) {

            cachedFont = lookupFont(node, "-fx-font", styleMap, cachedFont);

            if (cachedFont == SKIP) cachedFont = getCachedFont(node.getStyleableParent());
            if (cachedFont == null) cachedFont = new CalculatedValue(Font.getDefault(), null, false);

            cacheContainer.fontSizeCache.put(fontCacheKey,cachedFont);

        }

        final Font fontForRelativeSizes = (Font)cachedFont.getValue();

        final StyleCacheEntry.Key cacheEntryKey = new StyleCacheEntry.Key(transitionStates, fontForRelativeSizes);
        StyleCacheEntry cacheEntry = sharedCache.getStyleCacheEntry(cacheEntryKey);

        // if the cacheEntry already exists, take the fastpath
        final boolean fastpath = cacheEntry != null;

        if (cacheEntry == null) {
            cacheEntry = new StyleCacheEntry();
            sharedCache.addStyleCacheEntry(cacheEntryKey, cacheEntry);
        }

        final List<CssMetaData<? extends Styleable,  ?>> styleables = node.getCssMetaData();

        // Used in the for loop below, and a convenient place to stop when debugging.
        final int max = styleables.size();

        // RT-20643
        CssError.setCurrentScene(node.getScene());

        // For each property that is settable, we need to do a lookup and
        // transition to that value.
        for(int n=0; n<max; n++) {

            @SuppressWarnings("unchecked") // this is a widening conversion
            final CssMetaData<Styleable,Object> cssMetaData =
                    (CssMetaData<Styleable,Object>)styleables.get(n);

            if (observableStyleMap != null) {
                final StyleableProperty styleableProperty = cssMetaData.getStyleableProperty(node);
                if (styleableProperty != null && observableStyleMap.containsKey(styleableProperty)) {
                    observableStyleMap.remove(styleableProperty);
                }
            }

            // Don't bother looking up styles that don't inherit.
            if (inheritOnly && cssMetaData.isInherits() == false) {
                continue;
            }

            // Skip the lookup if we know there isn't a chance for this property
            // to be set (usually due to a "bind").
            if (!cssMetaData.isSettable(node)) continue;

            final String property = cssMetaData.getProperty();

            CalculatedValue calculatedValue = cacheEntry.get(property);

            // If there is no calculatedValue and we're on the fast path,
            // take the slow path if cssFlags is REAPPLY (RT-31691)
            final boolean forceSlowpath =
                    fastpath && calculatedValue == null && cssFlag == CssFlags.REAPPLY;

            final boolean addToCache =
                    (!fastpath && calculatedValue == null) || forceSlowpath;

            if (fastpath && !forceSlowpath) {

                // calculatedValue may be null,
                // but we should never put SKIP in cache.
                if (calculatedValue == SKIP) {
                    assert false : "cache returned SKIP for " + property;
                    continue;
                }

            } else if (calculatedValue == null) {

                // slowpath!
                calculatedValue = lookup(node, cssMetaData, styleMap, transitionStates[0],
                        node, cachedFont);

                // lookup is not supposed to return null.
                if (calculatedValue == null) {
                    assert false : "lookup returned null for " + property;
                    continue;
                }

            }

            // StyleableProperty#applyStyle might throw an exception and it is called
            // from two places in this try block.
            try {

                //
                // RT-19089
                // If the current value of the property was set by CSS
                // and there is no style for the property, then reset this
                // property to its initial value. If it was not set by CSS
                // then leave the property alone.
                //
                if (calculatedValue == null || calculatedValue == SKIP) {

                    // cssSetProperties keeps track of the StyleableProperty's that were set by CSS in the previous state.
                    // If this property is not in cssSetProperties map, then the property was not set in the previous state.
                    // This accomplishes two things. First, it lets us know if the property was set in the previous state
                    // so it can be reset in this state if there is no value for it. Second, it calling
                    // CssMetaData#getStyleableProperty which is rather expensive as it may cause expansion of lazy
                    // properties.
                    CalculatedValue initialValue = cacheContainer.cssSetProperties.get(cssMetaData);

                    // if the current value was set by CSS and there
                    // is no calculated value for the property, then
                    // there was no style for the property in the current
                    // state, so reset the property to its initial value.
                    if (initialValue != null) {

                        StyleableProperty styleableProperty = cssMetaData.getStyleableProperty(node);
                        if (styleableProperty.getStyleOrigin() != StyleOrigin.USER) {
                            styleableProperty.applyStyle(initialValue.getOrigin(), initialValue.getValue());
                        }
                    }

                    continue;

                }

                if (addToCache) {

                    // If we're not on the fastpath, then add the calculated
                    // value to cache.
                    cacheEntry.put(property, calculatedValue);
                }

                StyleableProperty styleableProperty = cssMetaData.getStyleableProperty(node);

                // need to know who set the current value - CSS, the user, or init
                final StyleOrigin originOfCurrentValue = styleableProperty.getStyleOrigin();


                // RT-10522:
                // If the user set the property and there is a style and
                // the style came from the user agent stylesheet, then
                // skip the value. A style from a user agent stylesheet should
                // not override the user set style.
                //
                final StyleOrigin originOfCalculatedValue = calculatedValue.getOrigin();

                // A calculated value should never have a null style origin since that would
                // imply the style didn't come from a stylesheet or in-line style.
                if (originOfCalculatedValue == null) {
                    assert false : styleableProperty.toString();
                    continue;
                }

                if (originOfCurrentValue == StyleOrigin.USER) {
                    if (originOfCalculatedValue == StyleOrigin.USER_AGENT) {
                        continue;
                    }
                }

                final Object value = calculatedValue.getValue();
                final Object currentValue = styleableProperty.getValue();

                // RT-21185: Only apply the style if something has changed.
                if ((originOfCurrentValue != originOfCalculatedValue)
                        || (currentValue != null
                        ? currentValue.equals(value) == false
                        : value != null)) {

                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer(property + ", call applyStyle: " + styleableProperty + ", value =" +
                                String.valueOf(value) + ", originOfCalculatedValue=" + originOfCalculatedValue);
                    }

                    styleableProperty.applyStyle(originOfCalculatedValue, value);

                    if (cacheContainer.cssSetProperties.containsKey(cssMetaData) == false) {
                        // track this property
                        CalculatedValue initialValue = new CalculatedValue(currentValue, originOfCurrentValue, false);
                        cacheContainer.cssSetProperties.put(cssMetaData, initialValue);
                    }

                }

                if (observableStyleMap != null) {

                    List<Style> styleList = getMatchingStyles(node, cssMetaData, true);
                    observableStyleMap.put(styleableProperty, styleList);
                }

            } catch (Exception e) {

                // RT-27155: if setting value raises exception, reset value
                // the value to initial and thereafter skip setting the property
                cacheEntry.put(property, null);

                List<CssError> errors = null;
                if ((errors = StyleManager.getErrors()) != null) {
                    final String msg = String.format("Failed to set css [%s] due to %s\n", cssMetaData, e.getMessage());
                    final CssError error = new CssError.PropertySetError(cssMetaData, node, msg);
                    errors.add(error);
                }
                // TODO: use logger here
                PlatformLogger logger = Logging.getCSSLogger();
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning(String.format("Failed to set css [%s]\n", cssMetaData), e);
                }
            }

        }

        // RT-20643
        CssError.setCurrentScene(null);

    }

    /**
     * Gets the CSS CascadingStyle for the property of this node in these pseudo-class
     * states. A null style may be returned if there is no style information
     * for this combination of input parameters.
     *
     *
     * @param styleable
     * @param property
     * @param styleMap
     * @param states   @return
     * */
    private CascadingStyle getStyle(final Styleable styleable, final String property, final StyleMap styleMap, final Set<PseudoClass> states){

        if (styleMap == null || styleMap.isEmpty()) return null;

        final Map<String, List<CascadingStyle>> cascadingStyleMap = styleMap.getCascadingStyles();
        if (cascadingStyleMap == null || cascadingStyleMap.isEmpty()) return null;

        // Get all of the Styles which may apply to this particular property
        List<CascadingStyle> styles = cascadingStyleMap.get(property);

        // If there are no styles for this property then we can just bail
        if ((styles == null) || styles.isEmpty()) return null;

        // Go looking for the style. We do this by visiting each CascadingStyle in
        // order finding the first that matches the current node & set of
        // pseudo-class states. We use an iteration style that avoids creating
        // garbage iterators (and wish javac did it for us...)
       CascadingStyle style = null;
        final int max = (styles == null) ? 0 : styles.size();
        for (int i=0; i<max; i++) {
            final CascadingStyle s = styles.get(i);
            final Selector sel = s == null ? null : s.getSelector();
            if (sel == null) continue; // bail if the selector is null.
//System.out.println(node.toString() + "\n\tstates=" + PseudoClassSet.getPseudoClasses(states) + "\n\tstateMatches? " + sel.stateMatches(node, states) + "\n\tsel=" + sel.toString());
            if (sel.stateMatches(styleable, states)) {
                style = s;
                break;
            }
        }

        return style;
    }

    /**
     * The main workhorse of this class, the lookup method walks up the CSS
     * style tree looking for the style information for the Node, the
     * property associated with the given styleable, in these states for this font.
     *
     *
     *
     *
     * @param styleable
     * @param states
     * @param originatingStyleable
     * @return
     */
    private CalculatedValue lookup(final Styleable styleable,
                                   final CssMetaData cssMetaData,
                                   final StyleMap styleMap,
                                   final Set<PseudoClass> states,
                                   final Styleable originatingStyleable,
                                   final CalculatedValue cachedFont) {

        if (cssMetaData.getConverter() == FontConverter.getInstance()) {
            return lookupFont(styleable, cssMetaData.getProperty(), styleMap, cachedFont);
        }

        final String property = cssMetaData.getProperty();

        // Get the CascadingStyle which may apply to this particular property
        CascadingStyle style = getStyle(styleable, property, styleMap, states);

        // If no style was found and there are no sub styleables, then there
        // are no matching styles for this property. We will then either SKIP
        // or we will INHERIT. We will inspect the default value for the styleable,
        // and if it is INHERIT then we will inherit otherwise we just skip it.
        final List<CssMetaData<? extends Styleable, ?>> subProperties = cssMetaData.getSubProperties();
        final int numSubProperties = (subProperties != null) ? subProperties.size() : 0;
        if (style == null) {

            if (numSubProperties == 0) {

                return handleNoStyleFound(styleable, cssMetaData,
                        styleMap, states, originatingStyleable, cachedFont);

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
                StyleOrigin origin = null;

                boolean isRelative = false;

                for (int i=0; i<numSubProperties; i++) {
                    CssMetaData subkey = subProperties.get(i);
                    CalculatedValue constituent =
                        lookup(styleable, subkey, styleMap, states,
                                originatingStyleable, cachedFont);
                    if (constituent != SKIP) {
                        if (subs == null) {
                            subs = new HashMap<CssMetaData,Object>();
                        }
                        subs.put(subkey, constituent.getValue());

                        // origin of this style is the most specific
                        if ((origin != null && constituent.getOrigin() != null)
                                ? origin.compareTo(constituent.getOrigin()) < 0
                                : constituent.getOrigin() != null) {
                            origin = constituent.getOrigin();
                        }

                        // if the constiuent uses relative sizes, then
                        // isRelative is true;
                        isRelative = isRelative || constituent.isRelative();

                    }
                }

                // If there are no subkeys which apply...
                if (subs == null || subs.isEmpty()) {
                    return handleNoStyleFound(styleable, cssMetaData,
                            styleMap, states, originatingStyleable, cachedFont);
                }

                try {
                    final StyleConverter keyType = cssMetaData.getConverter();
                    if (keyType instanceof StyleConverterImpl) {
                        Object ret = ((StyleConverterImpl)keyType).convert(subs);
                        return new CalculatedValue(ret, origin, isRelative);
                    } else {
                        assert false; // TBD: should an explicit exception be thrown here?
                        return SKIP;
                    }
                } catch (ClassCastException cce) {
                    final String msg = formatExceptionMessage(styleable, cssMetaData, null, cce);
                    List<CssError> errors = null;
                    if ((errors = StyleManager.getErrors()) != null) {
                        final CssError error = new CssError.PropertySetError(cssMetaData, styleable, msg);
                        errors.add(error);
                    }
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(msg);
                        LOGGER.fine("caught: ", cce);
                        LOGGER.fine("styleable = " + cssMetaData);
                        LOGGER.fine("node = " + styleable.toString());
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
            if (style.getOrigin() == StyleOrigin.USER_AGENT) {

                StyleableProperty styleableProperty = cssMetaData.getStyleableProperty(originatingStyleable);
                // if styleableProperty is null, then we're dealing with a sub-property.
                if (styleableProperty != null && styleableProperty.getStyleOrigin() == StyleOrigin.USER) {
                    return SKIP;
                }
            }

            // If there was a style found, then we want to check whether the
            // value was "inherit". If so, then we will simply inherit.
            final ParsedValueImpl cssValue = style.getParsedValueImpl();
            if (cssValue != null && "inherit".equals(cssValue.getValue())) {
                style = getInheritedStyle(styleable, property);
                if (style == null) return SKIP;
            }
        }

//        System.out.println("lookup " + property +
//                ", selector = \'" + style.selector.toString() + "\'" +
//                ", node = " + node.toString());

        return calculateValue(style, styleable, cssMetaData, styleMap, states,
                originatingStyleable, cachedFont);
    }

    /**
     * Called when there is no style found.
     */
    private CalculatedValue handleNoStyleFound(final Styleable styleable,
                                               final CssMetaData cssMetaData,
                                               final StyleMap styleMap, Set<PseudoClass> pseudoClassStates, Styleable originatingStyleable,
                                               final CalculatedValue cachedFont) {

        if (cssMetaData.isInherits()) {


            StyleableProperty styleableProperty = cssMetaData.getStyleableProperty(styleable);
            StyleOrigin origin = styleableProperty != null ? styleableProperty.getStyleOrigin() : null;

            // RT-16308: if there is no matching style and the user set
            // the property, do not look for inherited styles.
            if (origin == StyleOrigin.USER) {

                    return SKIP;

            }

            CascadingStyle style = getInheritedStyle(styleable, cssMetaData.getProperty());
            if (style == null) return SKIP;

            CalculatedValue cv =
                    calculateValue(style, styleable, cssMetaData,
                            styleMap, pseudoClassStates, originatingStyleable,
                                   cachedFont);

            return cv;

        } else {

            // Not inherited. There is no style
            return SKIP;

        }
    }
    /**
     * Called when we must getInheritedStyle a value from a parent node in the scenegraph.
     */
    private CascadingStyle getInheritedStyle(
            final Styleable styleable,
            final String property) {

        Styleable parent = styleable != null ? styleable.getStyleableParent() : null;

        while (parent != null) {

            CssStyleHelper parentStyleHelper = parent instanceof Node ? ((Node)parent).styleHelper : null;
            if (parentStyleHelper != null) {

                StyleMap parentStyleMap = parentStyleHelper.getStyleMap(parent);
                Set<PseudoClass> transitionStates = ((Node)parent).pseudoClassStates;
                CascadingStyle cascadingStyle = parentStyleHelper.getStyle(parent, property, parentStyleMap, transitionStates);

                if (cascadingStyle != null) {

                    final ParsedValueImpl cssValue = cascadingStyle.getParsedValueImpl();

                    if ("inherit".equals(cssValue.getValue())) {
                        return getInheritedStyle(parent, property);
                    }
                    return cascadingStyle;
                }

                return null;
            }

            parent = parent.getStyleableParent();

        }

        return null;
    }


    // helps with self-documenting the code
    static final Set<PseudoClass> NULL_PSEUDO_CLASS_STATE = null;

    /**
     * Find the property among the styles that pertain to the Node
     */
    private CascadingStyle resolveRef(final Styleable styleable, final String property, final StyleMap styleMap, final Set<PseudoClass> states) {

        final CascadingStyle style = getStyle(styleable, property, styleMap, states);
        if (style != null) {
            return style;
        } else {
            // if style is null, it may be because there isn't a style for this
            // node in this state, or we may need to look up the parent chain
            if (states != null && states.size() > 0) {
                // if states > 0, then we need to check this node again,
                // but without any states.
                return resolveRef(styleable,property, styleMap, NULL_PSEUDO_CLASS_STATE);
            } else {
                // TODO: This block was copied from inherit. Both should use same code somehow.
                Styleable styleableParent = styleable.getStyleableParent();
                CssStyleHelper parentStyleHelper = null;
                if (styleableParent != null && styleableParent instanceof Node) {
                    parentStyleHelper = ((Node)styleableParent).styleHelper;
                }
                while (styleableParent != null && parentStyleHelper == null) {
                    styleableParent = styleableParent.getStyleableParent();
                    if (styleableParent != null && styleableParent instanceof Node) {
                        parentStyleHelper = ((Node)styleableParent).styleHelper;
                    }
                }

                if (styleableParent == null || parentStyleHelper == null) {
                    return null;
                }

                StyleMap parentStyleMap = parentStyleHelper.getStyleMap(styleableParent);
                Set<PseudoClass> styleableParentPseudoClassStates =
                    styleableParent instanceof Node
                        ? ((Node)styleableParent).pseudoClassStates
                        : styleable.getPseudoClassStates();

                return parentStyleHelper.resolveRef(styleableParent, property,
                        parentStyleMap, styleableParentPseudoClassStates);
            }
        }
    }

    // to resolve a lookup, we just need to find the parsed value.
    private ParsedValueImpl resolveLookups(
            final Styleable styleable,
            final ParsedValueImpl parsedValue,
            final StyleMap styleMap, Set<PseudoClass> states,
            final ObjectProperty<StyleOrigin> whence,
            Set<ParsedValue> resolves) {

        //
        // either the value itself is a lookup, or the value contain a lookup
        //
        if (parsedValue.isLookup()) {

            // The value we're looking for should be a Paint, one of the
            // containers for linear, radial or ladder, or a derived color.
            final Object val = parsedValue.getValue();
            if (val instanceof String) {

                final String sval = (String)val;

                CascadingStyle resolved =
                    resolveRef(styleable, sval, styleMap, states);

                if (resolved != null) {

                    if (resolves.contains(resolved.getParsedValueImpl())) {

                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.warning("Loop detected in " + resolved.getRule().toString() + " while resolving '" + sval + "'");
                        }
                        throw new IllegalArgumentException("Loop detected in " + resolved.getRule().toString() + " while resolving '" + sval + "'");

                    } else {
                        resolves.add(parsedValue);
                    }

                    // The origin of this parsed value is the greatest of
                    // any of the resolved reference. If a resolved reference
                    // comes from an inline style, for example, then the value
                    // calculated from the resolved lookup should have inline
                    // as its origin. Otherwise, an inline style could be
                    // stored in shared cache.
                    final StyleOrigin wOrigin = whence.get();
                    final StyleOrigin rOrigin = resolved.getOrigin();
                    if (rOrigin != null && (wOrigin == null ||  wOrigin.compareTo(rOrigin) < 0)) {
                        whence.set(rOrigin);
                    }

                    // the resolved value may itself need to be resolved.
                    // For example, if the value "color" resolves to "base",
                    // then "base" will need to be resolved as well.
                    ParsedValueImpl pv = resolveLookups(styleable, resolved.getParsedValueImpl(), styleMap, states, whence, resolves);

                    if (resolves != null) {
                        resolves.remove(parsedValue);
                    }

                    return pv;

                }
            }
        }

        // If the value doesn't contain any values that need lookup, then bail
        if (!parsedValue.isContainsLookups()) {
            return parsedValue;
        }

        final Object val = parsedValue.getValue();

        if (val instanceof ParsedValueImpl[][]) {

            // If ParsedValueImpl is a layered sequence of values, resolve the lookups for each.
            final ParsedValueImpl[][] layers = (ParsedValueImpl[][])val;
            ParsedValueImpl[][] resolved = new ParsedValueImpl[layers.length][0];
            for (int l=0; l<layers.length; l++) {
                resolved[l] = new ParsedValueImpl[layers[l].length];
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                    resolved[l][ll] =
                        resolveLookups(styleable, layers[l][ll], styleMap, states, whence, resolves);
                }
            }

            resolves.clear();

            return new ParsedValueImpl(resolved, parsedValue.getConverter(), false);

        } else if (val instanceof ParsedValueImpl[]) {

            // If ParsedValueImpl is a sequence of values, resolve the lookups for each.
            final ParsedValueImpl[] layer = (ParsedValueImpl[])val;
            ParsedValueImpl[] resolved = new ParsedValueImpl[layer.length];
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                resolved[l] =
                    resolveLookups(styleable, layer[l], styleMap, states, whence, resolves);
            }

            resolves.clear();

            return new ParsedValueImpl(resolved, parsedValue.getConverter(), false);

        }

        return parsedValue;

    }

    private String getUnresolvedLookup(final ParsedValueImpl resolved) {

        Object value = resolved.getValue();

        if (resolved.isLookup() && value instanceof String) {
            return (String)value;
        }

        if (value instanceof ParsedValueImpl[][]) {
            final ParsedValueImpl[][] layers = (ParsedValueImpl[][])value;
            for (int l=0; l<layers.length; l++) {
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                    String unresolvedLookup = getUnresolvedLookup(layers[l][ll]);
                    if (unresolvedLookup != null) return unresolvedLookup;
                }
            }

        } else if (value instanceof ParsedValueImpl[]) {
        // If ParsedValueImpl is a sequence of values, resolve the lookups for each.
            final ParsedValueImpl[] layer = (ParsedValueImpl[])value;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                String unresolvedLookup = getUnresolvedLookup(layer[l]);
                if (unresolvedLookup != null) return unresolvedLookup;
            }
        }

        return null;
    }

    private String formatUnresolvedLookupMessage(Styleable styleable, CssMetaData cssMetaData, Style style, ParsedValueImpl resolved) {

        // find value that could not be looked up
        String missingLookup = resolved != null ? getUnresolvedLookup(resolved) : null;
        if (missingLookup == null) missingLookup = "a lookup value";

        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Could not resolve '")
            .append(missingLookup)
            .append("'")
            .append(" while resolving lookups for '")
            .append(cssMetaData.getProperty())
            .append("'");

        final Rule rule = style != null ? style.getDeclaration().getRule(): null;
        final Stylesheet stylesheet = rule != null ? rule.getStylesheet() : null;
        final String url = stylesheet != null ? stylesheet.getUrl() : null;
        if (url != null) {
            sbuf.append(" from rule '")
                .append(style.getSelector())
                .append("' in stylesheet ").append(url);
        } else if (stylesheet != null && StyleOrigin.INLINE == stylesheet.getOrigin()) {
            sbuf.append(" from inline style on " )
                .append(styleable.toString());
        }

        return sbuf.toString();
    }

    private String formatExceptionMessage(Styleable styleable, CssMetaData cssMetaData, Style style, Exception e) {

        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Caught ")
            .append(e.toString())
            .append("'")
            .append(" while calculating value for '")
            .append(cssMetaData.getProperty())
            .append("'");

        final Rule rule = style != null ? style.getDeclaration().getRule(): null;
        final Stylesheet stylesheet = rule != null ? rule.getStylesheet() : null;
        final String url = stylesheet != null ? stylesheet.getUrl() : null;
        if (url != null) {
            sbuf.append(" from rule '")
                .append(style.getSelector())
                .append("' in stylesheet ").append(url);
        } else if (stylesheet != null && StyleOrigin.INLINE == stylesheet.getOrigin()) {
            sbuf.append(" from inline style on " )
                .append(styleable.toString());
        }

        return sbuf.toString();
    }


    private CalculatedValue calculateValue(
            final CascadingStyle style,
            final Styleable styleable,
            final CssMetaData cssMetaData,
            final StyleMap styleMap, final Set<PseudoClass> states,
            final Styleable originatingStyleable,
            final CalculatedValue fontFromCacheEntry) {

        final ParsedValueImpl cssValue = style.getParsedValueImpl();
        if (cssValue != null && !("null").equals(cssValue.getValue())) {

            ParsedValueImpl resolved = null;
            try {

                ObjectProperty<StyleOrigin> whence = new SimpleObjectProperty<StyleOrigin>(style.getOrigin());
                resolved = resolveLookups(styleable, cssValue, styleMap, states, whence, new HashSet<ParsedValue>());

                final String property = cssMetaData.getProperty();

                // The computed value
                Object val = null;
                boolean isFontProperty =
                        "-fx-font".equals(property) ||
                        "-fx-font-size".equals(property);

                boolean isRelative = ParsedValueImpl.containsFontRelativeSize(resolved, isFontProperty);

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
                Font fontForFontRelativeSizes = null;

                if (isRelative && isFontProperty &&
                    (fontFromCacheEntry == null || fontFromCacheEntry.isRelative())) {

                    Styleable parent = styleable;
                    CalculatedValue childsCachedFont = fontFromCacheEntry;
                    do {

                        CalculatedValue parentsCachedFont = getCachedFont(parent.getStyleableParent());

                        if (parentsCachedFont != null)  {

                            if (parentsCachedFont.isRelative()) {

                                //
                                // If the cached fonts are the same, then the cached font came from the same
                                // style and we need to keep looking. Otherwise, use the font we found.
                                //
                                if (childsCachedFont == null || parentsCachedFont.equals(childsCachedFont)) {
                                    childsCachedFont = parentsCachedFont;
                                } else {
                                    fontForFontRelativeSizes = (Font)parentsCachedFont.getValue();
                                }

                            } else  {
                                // fontValue.isRelative() == false!
                                fontForFontRelativeSizes = (Font)parentsCachedFont.getValue();
                            }

                        }

                    } while(fontForFontRelativeSizes == null &&
                            (parent = parent.getStyleableParent()) != null);
                }

                // did we get a fontValue from the preceding block?
                // if not, get it from our cacheEntry or choose the default
                if (fontForFontRelativeSizes == null) {
                    if (fontFromCacheEntry != null && fontFromCacheEntry.isRelative() == false) {
                        fontForFontRelativeSizes = (Font)fontFromCacheEntry.getValue();
                    } else {
                        fontForFontRelativeSizes = Font.getDefault();
                    }
                }

                if (resolved.getConverter() != null)
                    val = resolved.convert(fontForFontRelativeSizes);
                else
                    val = cssMetaData.getConverter().convert(resolved, fontForFontRelativeSizes);

                final StyleOrigin origin = whence.get();
                return new CalculatedValue(val, origin, isRelative);

            } catch (ClassCastException cce) {
                final String msg = formatUnresolvedLookupMessage(styleable, cssMetaData, style.getStyle(),resolved);
                List<CssError> errors = null;
                if ((errors = StyleManager.getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(cssMetaData, styleable, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(msg);
                    LOGGER.fine("node = " + styleable.toString());
                    LOGGER.fine("cssMetaData = " + cssMetaData);
                    LOGGER.fine("styles = " + getMatchingStyles(styleable, cssMetaData));
                }
                return SKIP;
            } catch (IllegalArgumentException iae) {
                final String msg = formatExceptionMessage(styleable, cssMetaData, style.getStyle(), iae);
                List<CssError> errors = null;
                if ((errors = StyleManager.getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(cssMetaData, styleable, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(msg);
                    LOGGER.fine("caught: ", iae);
                    LOGGER.fine("styleable = " + cssMetaData);
                    LOGGER.fine("node = " + styleable.toString());
                }
                return SKIP;
            } catch (NullPointerException npe) {
                final String msg = formatExceptionMessage(styleable, cssMetaData, style.getStyle(), npe);
                List<CssError> errors = null;
                if ((errors = StyleManager.getErrors()) != null) {
                    final CssError error = new CssError.PropertySetError(cssMetaData, styleable, msg);
                    errors.add(error);
                }
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(msg);
                    LOGGER.fine("caught: ", npe);
                    LOGGER.fine("styleable = " + cssMetaData);
                    LOGGER.fine("node = " + styleable.toString());
                }
                return SKIP;
            }

        }
        // either cssValue was null or cssValue's value was "null"
        return new CalculatedValue(null, style.getOrigin(), false);

    }

    private static final CssMetaData dummyFontProperty =
            new FontCssMetaData<Node>("-fx-font", Font.getDefault()) {

        @Override
        public boolean isSettable(Node node) {
            return true;
        }

        @Override
        public StyleableProperty<Font> getStyleableProperty(Node node) {
            return null;
        }
    };

    private CalculatedValue getCachedFont(final Styleable styleable) {

        if (styleable instanceof Node == false) return null;

        CalculatedValue cachedFont = null;

        Node parent = (Node)styleable;

        final CssStyleHelper parentHelper = parent.styleHelper;

        // if there is no parentHelper,
        // or there is a parentHelper but no cacheContainer,
        // then look to the next parent
        if (parentHelper == null || parentHelper.cacheContainer == null) {

            cachedFont = getCachedFont(parent.getStyleableParent());

        // there is a parent helper and a cacheContainer,
        } else  {

            CacheContainer parentCacheContainer = parentHelper.cacheContainer;
            if ( parentCacheContainer != null
                    && parentCacheContainer.fontSizeCache != null
                    && parentCacheContainer.fontSizeCache.isEmpty() == false) {

                Set<PseudoClass>[] transitionStates = parentHelper.getTransitionStates(parent);
                StyleCacheEntry.Key parentCacheEntryKey = new StyleCacheEntry.Key(transitionStates, Font.getDefault());
                cachedFont = parentCacheContainer.fontSizeCache.get(parentCacheEntryKey);

            } else {

                Set<PseudoClass> pseudoClassState = parent.getPseudoClassStates();
                StyleMap smap = parentHelper.getStyleMap(parent);
                cachedFont = parentHelper.lookup(parent, dummyFontProperty, smap, pseudoClassState, parent, null);
            }
        }

        return cachedFont;
    }

    /*package access for testing*/ FontPosture getFontPosture(Font font) {
        if (font == null) return FontPosture.REGULAR;

        String fontName = font.getName().toLowerCase(Locale.ROOT);

        if (fontName.contains("italic")) {
            return FontPosture.ITALIC;
        }

        return FontPosture.REGULAR;
    }

    /*package access for testing*/ FontWeight getFontWeight(Font font) {
        if (font == null) return FontWeight.NORMAL;

        String fontName = font.getName().toLowerCase(Locale.ROOT);

        if (fontName.contains("bold")) {
            if (fontName.contains("extra")) return FontWeight.EXTRA_BOLD;
            if (fontName.contains("ultra")) return FontWeight.EXTRA_BOLD;
            else if (fontName.contains("semi")) return FontWeight.SEMI_BOLD;
            else if (fontName.contains("demi")) return FontWeight.SEMI_BOLD;
            else return FontWeight.BOLD;

        } else if (fontName.contains("light")) {
            if (fontName.contains("extra")) return FontWeight.EXTRA_LIGHT;
            if (fontName.contains("ultra")) return FontWeight.EXTRA_LIGHT;
            else return FontWeight.LIGHT;

        } else if (fontName.contains("black")) {
            return FontWeight.BLACK;

        } else if (fontName.contains("heavy")) {
            return FontWeight.BLACK;

        } else if (fontName.contains("medium")) {
            return FontWeight.MEDIUM;
        }

        return FontWeight.NORMAL;

    }

    /*package access for testing*/ String getFontFamily(Font font) {
        if (font == null) return Font.getDefault().getFamily();
        return font.getFamily();
    }


    /*package access for testing*/ Font deriveFont(
            Font font,
            String fontFamily,
            FontWeight fontWeight,
            FontPosture fontPosture,
            double fontSize) {

        if (font != null && fontFamily == null) fontFamily = getFontFamily(font);
        else if (fontFamily != null) fontFamily = Utils.stripQuotes(fontFamily);

        if (font != null && fontWeight == null) fontWeight = getFontWeight(font);
        if (font != null && fontPosture == null) fontPosture = getFontPosture(font);
        if (font != null && fontSize <= 0) fontSize = font.getSize();

        return  Font.font(
                fontFamily,
                fontWeight,
                fontPosture,
                fontSize);
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
     */
     /*package access for testing*/ CalculatedValue lookupFont(
            final Styleable styleable,
            final String property,
            final StyleMap styleMap,
            final CalculatedValue cachedFont)
    {

        StyleOrigin origin = null;

        // How far from this node did we travel to find a font shorthand?
        // Don't look past this distance for other font properties.
        int distance = 0;

        // Did we find a style?
        boolean foundStyle = false;

        String family = null;
        double size = -1;
        FontWeight weight = null;
        FontPosture posture = null;

        CalculatedValue cvFont = cachedFont;

        Set<PseudoClass> states = styleable instanceof Node ? ((Node)styleable).pseudoClassStates : styleable.getPseudoClassStates();

        // RT-20145 - if looking for font size and the node has a font,
        // use the font property's value if it was set by the user and
        // there is not an inline or author style.

        if (cacheContainer.fontProp != null) {
            StyleableProperty<Font> styleableProp = cacheContainer.fontProp.getStyleableProperty(styleable);
            StyleOrigin fpOrigin = styleableProp.getStyleOrigin();
            if (fpOrigin == StyleOrigin.USER) {
                origin = fpOrigin;
                Font font = styleableProp.getValue();
                if (font == null) font = Font.getDefault();
                family = getFontFamily(font);
                size = font.getSize();
                weight = getFontWeight(font);
                posture = getFontPosture(font);
                cvFont = new CalculatedValue(font, fpOrigin, false);
            }
        }

        //
        // Look up the font- properties
        //
        CascadingStyle fontShorthand = getStyle(styleable, property, styleMap, states);

        // don't look past current node for font shorthand if user set the font
        if (fontShorthand == null && origin != StyleOrigin.USER) {

            Styleable parent = styleable != null ? styleable.getStyleableParent() : null;

            while (parent != null) {

                CssStyleHelper parentStyleHelper = parent instanceof Node ? ((Node)parent).styleHelper : null;
                if (parentStyleHelper != null) {

                    distance += 1;

                    StyleMap parentStyleMap = parentStyleHelper.getStyleMap(parent);
                    Set<PseudoClass> transitionStates = ((Node)parent).pseudoClassStates;
                    CascadingStyle cascadingStyle = parentStyleHelper.getStyle(parent, property, parentStyleMap, transitionStates);

                    if (cascadingStyle != null) {

                        final ParsedValueImpl cssValue = cascadingStyle.getParsedValueImpl();

                        if ("inherit".equals(cssValue.getValue()) == false) {
                            fontShorthand = cascadingStyle;
                            break;
                        }
                    }

                }

                parent = parent.getStyleableParent();

            }

        }

        if (fontShorthand != null) {

            //
            // If we don't have an existing font, or if the origin of the
            // existing font is less than that of the shorthand, then
            // take the shorthand. If the origins compare equals, then take
            // the shorthand since the fontProp value will not have been
            // updated yet.
            //
            if (origin == null || origin.compareTo(fontShorthand.getOrigin()) <= 0) {

                final CalculatedValue cv =
                        calculateValue(fontShorthand, styleable, dummyFontProperty,
                                styleMap, states, styleable, cvFont);

                // cv could be SKIP
                if (cv.getValue() instanceof Font) {
                    origin = cv.getOrigin();
                    Font font = (Font)cv.getValue();
                    family = getFontFamily(font);
                    size = font.getSize();
                    weight = getFontWeight(font);
                    posture = getFontPosture(font);
                    cvFont = cv;
                    foundStyle = true;
                }

            }
        }

        CascadingStyle fontSize = getStyle(styleable, property.concat("-size"), styleMap, states);
        if (fontSize != null) {
            // if we have a font shorthand and it is more specific than font-size, then don't use the font-size style
            if (fontShorthand != null && fontShorthand.compareTo(fontSize) < 0) {
                fontSize = null;
            }

        } else if (origin != StyleOrigin.USER) {
            //
            // If we don't have a font-size, see if there is an inherited font-size.
            // If lookupInheritedFontProperty returns other than null, then we know that font-size is closer (more specific)
            // than the font shorthand
            //
            fontSize = lookupInheritedFontProperty(styleable, property.concat("-size"), styleMap, distance, fontShorthand);
        }

        if (fontSize != null) {

            // The logic above ensures that, if fontSize is not null, then it is either
            // 1) a style matching this node and is more specific than the font shorthand or
            // 2) an inherited style that is more specific than the font shorthand
            // and, therefore, we can use the fontSize style
            final CalculatedValue cv =
                    calculateValue(fontSize, styleable, dummyFontProperty,
                            styleMap, states, styleable, cvFont);

            if (cv.getValue() instanceof Double) {
                if (origin == null || origin.compareTo(fontSize.getOrigin()) <= 0) {

                    origin = cv.getOrigin();
                }
                size = ((Double) cv.getValue()).doubleValue();

                if (cvFont != null) {
                    boolean isRelative = cvFont.isRelative() || cv.isRelative();
                    Font font = deriveFont((Font) cvFont.getValue(), family, weight, posture, size);
                    cvFont = new CalculatedValue(font, origin, isRelative);
                } else {
                    boolean isRelative = cv.isRelative();
                    Font font = deriveFont(Font.getDefault(), family, weight, posture, size);
                    cvFont = new CalculatedValue(font, origin, isRelative);
                }
                foundStyle = true;
            }

        }

        // if cachedFont is null, then we're in this method to look up a font for the CacheContainer's fontSizeCache
        // and we only care about font-size or the size from font shorthand.
        if (cachedFont == null) {
            return (cvFont != null) ? cvFont : SKIP;
        }

        CascadingStyle fontWeight = getStyle(styleable, property.concat("-weight"), styleMap, states);
        if (fontWeight != null) {
            // if we have a font shorthand and it is more specific than font-weight, then don't use the font-weight style
            if (fontShorthand != null && fontShorthand.compareTo(fontWeight) < 0) {
                fontWeight = null;
            }

        } else if (origin != StyleOrigin.USER) {
            //
            // If we don't have a font-weight, see if there is an inherited font-weight.
            // If lookupInheritedFontProperty returns other than null, then we know that font-weight is closer (more specific)
            // than the font shorthand
            //
            fontWeight = lookupInheritedFontProperty(styleable, property.concat("-weight"), styleMap, distance, fontShorthand);
        }

        if (fontWeight != null) {

            // The logic above ensures that, if fontWeight is not null, then it is either
            // 1) a style matching this node and is more specific than the font shorthand or
            // 2) an inherited style that is more specific than the font shorthand
            // and, therefore, we can use the fontWeight style

            final CalculatedValue cv =
                    calculateValue(fontWeight, styleable, dummyFontProperty,
                            styleMap, states, styleable, null);

            if (cv.getValue() instanceof FontWeight) {
                if (origin == null || origin.compareTo(fontWeight.getOrigin()) <= 0) {
                    origin = cv.getOrigin();
                }
                weight = (FontWeight)cv.getValue();
                foundStyle = true;
            }
        }


        CascadingStyle fontStyle = getStyle(styleable, property.concat("-style"), styleMap, states);
        if (fontStyle != null) {
            // if we have a font shorthand and it is more specific than font-style, then don't use the font-style style
            if (fontShorthand != null && fontShorthand.compareTo(fontStyle) < 0) {
                fontStyle = null;
            }

        } else if (origin != StyleOrigin.USER) {
            //
            // If we don't have a font-style, see if there is an inherited font-style.
            // If lookupInheritedFontProperty returns other than null, then we know that font-style is closer (more specific)
            // than the font shorthand
            //
            fontStyle = lookupInheritedFontProperty(styleable, property.concat("-style"), styleMap, distance, fontShorthand);
        }

        if (fontStyle != null) {

            // The logic above ensures that, if fontStyle is not null, then it is either
            // 1) a style matching this node and is more specific than the font shorthand or
            // 2) an inherited style that is more specific than the font shorthand
            // and, therefore, we can use the fontStyle style

            final CalculatedValue cv =
                    calculateValue(fontStyle, styleable, dummyFontProperty,
                            styleMap, states, styleable, null);

            if (cv.getValue() instanceof FontPosture) {
                if (origin == null || origin.compareTo(fontStyle.getOrigin()) <= 0) {
                    origin = cv.getOrigin();
                }
                posture = (FontPosture)cv.getValue();
                foundStyle = true;
            }

        }

        CascadingStyle fontFamily = getStyle(styleable, property.concat("-family"), styleMap, states);
        if (fontFamily != null) {
            // if we have a font shorthand and it is more specific than font-family, then don't use the font-family style
            if (fontShorthand != null && fontShorthand.compareTo(fontFamily) < 0) {
                fontFamily = null;
            }

        } else if (origin != StyleOrigin.USER) {
            //
            // If we don't have a font-family, see if there is an inherited font-family.
            // If lookupInheritedFontProperty returns other than null, then we know that font-family is closer (more specific)
            // than the font shorthand
            //
            fontFamily = lookupInheritedFontProperty(styleable, property.concat("-family"), styleMap, distance, fontShorthand);
        }

        if (fontFamily != null) {

            // The logic above ensures that, if fontFamily is not null, then it is either
            // 1) a style matching this node and is more specific than the font shorthand or
            // 2) an inherited style that is more specific than the font shorthand
            // and, therefore, we can use the fontFamily style

            final CalculatedValue cv =
                    calculateValue(fontFamily, styleable, dummyFontProperty,
                            styleMap, states, styleable, null);

            if (cv.getValue() instanceof String) {
                if (origin == null || origin.compareTo(fontFamily.getOrigin()) <= 0) {
                    origin = cv.getOrigin();
                }
                family = (String)cv.getValue();
                foundStyle = true;
            }

        }

        if (foundStyle) {

            Font font = cvFont != null ? (Font)cvFont.getValue() : Font.getDefault();
            Font derivedFont = deriveFont(font, family, weight, posture, size);
            return new CalculatedValue(derivedFont,origin,false);

        }

        return SKIP;
    }

    private CascadingStyle lookupInheritedFontProperty(
            final Styleable styleable,
            final String property,
            final StyleMap styleMap,
            final int distance,
            CascadingStyle fontShorthand) {

        Styleable parent = styleable != null ? styleable.getStyleableParent() : null;

        int nlooks = distance;
        while (parent != null && nlooks > 0) {

            CssStyleHelper parentStyleHelper = parent instanceof Node ? ((Node)parent).styleHelper : null;
            if (parentStyleHelper != null) {

                nlooks -= 1;

                StyleMap parentStyleMap = parentStyleHelper.getStyleMap((parent));
                Set<PseudoClass> transitionStates = ((Node)parent).pseudoClassStates;
                CascadingStyle cascadingStyle = parentStyleHelper.getStyle(parent, property, parentStyleMap, transitionStates);

                if (cascadingStyle != null) {

                    // If we are closer to the node than the font shorthand, then font shorthand doesn't matter.
                    // If the font shorthand and this style are the same distance, then we need to compare.
                    if (fontShorthand != null && nlooks == 0) {
                        if (fontShorthand.compareTo(cascadingStyle) < 0) {
                            return null;
                        }
                    }

                    final ParsedValueImpl cssValue = cascadingStyle.getParsedValueImpl();

                    if ("inherit".equals(cssValue.getValue()) == false) {
                        return cascadingStyle;
                    }
                }

            }

            parent = parent.getStyleableParent();

        }

        return null;
    }


    /**
     * Called from CssMetaData getMatchingStyles
     * @param node
     * @param styleableProperty
     * @return
     */
    List<Style> getMatchingStyles(final Styleable node, final CssMetaData styleableProperty) {
        return getMatchingStyles(node, styleableProperty, false);
    }

    List<Style> getMatchingStyles(final Styleable node, final CssMetaData styleableProperty, boolean matchState) {

        final List<CascadingStyle> styleList = new ArrayList<CascadingStyle>();

        getMatchingStyles(node, styleableProperty, styleList, matchState);

        List<CssMetaData<? extends Styleable, ?>> subProperties = styleableProperty.getSubProperties();
        if (subProperties != null) {
            for (int n=0,nMax=subProperties.size(); n<nMax; n++) {
                final CssMetaData subProperty = subProperties.get(n);
                getMatchingStyles(node, subProperty, styleList, matchState);
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

    private void getMatchingStyles(final Styleable node, final CssMetaData styleableProperty, final List<CascadingStyle> styleList, boolean matchState) {

        if (node != null) {

            String property = styleableProperty.getProperty();
            Node _node = node instanceof Node ? (Node)node : null;
            final StyleMap smap = getStyleMap(_node);
            if (smap == null) return;

            if (matchState) {
                CascadingStyle cascadingStyle = getStyle(node, styleableProperty.getProperty(), smap, _node.pseudoClassStates);
                if (cascadingStyle != null) {
                    styleList.add(cascadingStyle);
                    final ParsedValueImpl parsedValue = cascadingStyle.getParsedValueImpl();
                    getMatchingLookupStyles(node, parsedValue, styleList, matchState);
                }
            }  else {

                Map<String, List<CascadingStyle>> cascadingStyleMap = smap.getCascadingStyles();
                // StyleMap.getCascadingStyles() does not return null
                List<CascadingStyle> styles = cascadingStyleMap.get(property);

                if (styles != null) {
                    styleList.addAll(styles);
                    for (int n=0, nMax=styles.size(); n<nMax; n++) {
                        final CascadingStyle style = styles.get(n);
                        final ParsedValueImpl parsedValue = style.getParsedValueImpl();
                        getMatchingLookupStyles(node, parsedValue, styleList, matchState);
                    }
                }
            }

            if (styleableProperty.isInherits()) {
                Styleable parent = node.getStyleableParent();
                while (parent != null) {
                    CssStyleHelper parentHelper = parent instanceof Node
                            ? ((Node)parent).styleHelper
                            : null;
                    if (parentHelper != null) {
                        parentHelper.getMatchingStyles(parent, styleableProperty, styleList, matchState);
                    }
                    parent = parent.getStyleableParent();
                }
            }

        }

    }

    // Pretty much a duplicate of resolveLookups, but without the state
    private void getMatchingLookupStyles(final Styleable node, final ParsedValueImpl parsedValue, final List<CascadingStyle> styleList, boolean matchState) {

        if (parsedValue.isLookup()) {

            Object value = parsedValue.getValue();

            if (value instanceof String) {

                final String property = (String)value;
                // gather up any and all styles that contain this value as a property
                Styleable parent = node;
                do {

                    StyleMap styleMap = getStyleMap(parent);
                    if (styleMap == null || styleMap.isEmpty()) continue;

                    final Node _parent = parent instanceof Node ? (Node)parent : null;
                    final CssStyleHelper helper = _parent != null
                            ? _parent.styleHelper
                            : null;
                    if (helper != null) {

                        final int start = styleList.size();

                        if (matchState) {
                            CascadingStyle cascadingStyle = helper.resolveRef(_parent, property, styleMap, _parent.pseudoClassStates);
                            if (cascadingStyle != null) {
                                styleList.add(cascadingStyle);
                            }
                        } else {
                            final Map<String, List<CascadingStyle>> smap = styleMap.getCascadingStyles();
                            // getCascadingStyles does not return null
                            List<CascadingStyle> styles = smap.get(property);

                            if (styles != null) {
                                styleList.addAll(styles);
                            }

                        }

                        final int end = styleList.size();

                        for (int index=start; index<end; index++) {
                            final CascadingStyle style = styleList.get(index);
                            getMatchingLookupStyles(parent, style.getParsedValueImpl(), styleList, matchState);
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
        if (val instanceof ParsedValueImpl[][]) {
        // If ParsedValueImpl is a layered sequence of values, resolve the lookups for each.
            final ParsedValueImpl[][] layers = (ParsedValueImpl[][])val;
            for (int l=0; l<layers.length; l++) {
                for (int ll=0; ll<layers[l].length; ll++) {
                    if (layers[l][ll] == null) continue;
                        getMatchingLookupStyles(node, layers[l][ll], styleList, matchState);
                }
            }

        } else if (val instanceof ParsedValueImpl[]) {
        // If ParsedValueImpl is a sequence of values, resolve the lookups for each.
            final ParsedValueImpl[] layer = (ParsedValueImpl[])val;
            for (int l=0; l<layer.length; l++) {
                if (layer[l] == null) continue;
                    getMatchingLookupStyles(node, layer[l], styleList, matchState);
            }
        }

    }

}
