/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.css;

import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Style;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.Skinnable;

/**
 *
 * @treatAsPrivate
 */
public class NodeCssState {

    private static final List<StyleOrigin> ORDERED_ORIGIN = new ArrayList<>();

    static {
        ORDERED_ORIGIN.add(StyleOrigin.USER_AGENT);// fxTheme : modena/caspian
        ORDERED_ORIGIN.add(StyleOrigin.USER);//Bean API Call
        ORDERED_ORIGIN.add(StyleOrigin.AUTHOR);//CSS files
        ORDERED_ORIGIN.add(StyleOrigin.INLINE);//Style property

    }

    @SuppressWarnings("rawtypes")
    private final Map<StyleableProperty, List<Style>> map;
    private final Node node;
    private final FXOMObject fxomObject;
    private Collection<CssContentMaker.CssPropertyState> author;
    private Collection<CssContentMaker.CssPropertyState> inline;
    private Collection<CssContentMaker.CssPropertyState> userAgent;
    private Map<MatchingRule, List<MatchingDeclaration>> matchingRules;
    private List<MatchingRule> sortedMatchingRules = new ArrayList<>();
    private Collection<CssProperty> props;

    @SuppressWarnings("rawtypes")
    protected NodeCssState(Map<StyleableProperty, List<Style>> map, Node node, FXOMObject fxomObject) {
        this.map = map;
        this.node = node;
        this.fxomObject = fxomObject;
        getAuthorStyles();
        getInlineStyles();
        getUserAgentStyles();
        getMatchingRules();
        getAllStyleables();
    }

    /**
     *
     * @treatAsPrivate
     */
    @SuppressWarnings("rawtypes")
    public static class CssProperty implements Comparable<CssProperty> {

        private final CssMetaData cssMeta;
        private final CssProperty mainProperty;
        private final Node target;
        private final List<CssProperty> sub = new ArrayList<>();
        private final ObjectProperty<String> name = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.PropertyState> builtin = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> fxTheme = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> authorCss = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> inlineCss = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.PropertyState> fxmlModel = new SimpleObjectProperty<>();
        private CssContentMaker.PropertyState currentState;

        CssProperty(NodeCssState nodeCssState, CssMetaData cssMeta, Node target, FXOMObject fxomObject) {
            this(nodeCssState, null, cssMeta, target, fxomObject);
        }

        CssProperty(NodeCssState nodeCssState, CssProperty mainProperty,
                CssMetaData cssMeta, Node target, FXOMObject fxomObject) {
            this.mainProperty = mainProperty;
            this.cssMeta = cssMeta;
            this.target = target;
            name.setValue(cssMeta.getProperty());
            CssContentMaker.CssPropertyState inlineState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getInlineStyles());
            if (inlineState != null) {
                inlineCss.setValue(inlineState);
            }
            CssContentMaker.CssPropertyState authorState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getAuthorStyles());
            if (authorState != null) {
                authorCss.setValue(authorState);
            }
            CssContentMaker.CssPropertyState fxThemeState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getUserAgentStyles());
            if (fxThemeState != null) {
                fxTheme.setValue(fxThemeState);
            }
            @SuppressWarnings("unchecked")
            CssContentMaker.PropertyState builtinState = CssContentMaker.initialValue(target, mainProperty == null ? this : mainProperty, cssMeta);
            assert builtinState != null;
            builtin.setValue(builtinState);

            CssContentMaker.PropertyState modelState = CssContentMaker.modelValue(target, cssMeta, fxomObject);
            if (modelState != null) {
                fxmlModel.setValue(modelState);
            }
        }

        public ObjectProperty<CssContentMaker.PropertyState> builtinState() {
            return builtin;
        }

        public ObjectProperty<CssContentMaker.PropertyState> modelState() {
            return fxmlModel;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> fxThemeState() {
            return fxTheme;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> authorState() {
            return authorCss;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> inlineState() {
            return inlineCss;
        }

        public ObjectProperty<String> propertyName() {
            return name;
        }

        public CssMetaData getStyleable() {
            return cssMeta;
        }

        public Node getTarget() {
            return target;
        }

        public List<CssProperty> getSubProperties() {
            return sub;
        }

        public CssProperty getMainProperty() {
            return mainProperty;
        }

        @Override
        public int compareTo(CssProperty cssProperty) {
            return cssMeta.getProperty().compareTo(cssProperty.cssMeta.getProperty());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CssProperty cssProperty = (CssProperty) obj;
            return cssMeta.getProperty().compareTo(cssProperty.cssMeta.getProperty()) == 0;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 31 * hash + Objects.hashCode(this.cssMeta);
            hash = 31 * hash + Objects.hashCode(this.mainProperty);
            hash = 31 * hash + Objects.hashCode(this.target);
            return hash;
        }

        public boolean isBuiltinSource() {
            return inlineState().get() == null
                    && authorState().get() == null
                    && modelState().get() == null
                    && fxThemeState().get() == null;
        }

        public boolean isFxThemeSource() {
            return fxThemeState().get() != null
                    && inlineState().get() == null
                    && authorState().get() == null
                    && modelState().get() == null;
        }

        public boolean isModelSource() {
            return modelState().get() != null
                    && inlineState().get() == null
                    && authorState().get() == null;
        }

        public boolean isAuthorSource() {
            return authorState().get() != null
                    && inlineState().get() == null;
        }

        public boolean isInlineSource() {
            return inlineState().get() != null;
        }

        // CSS only, model and builtin doesn't make sense there
        public CssContentMaker.CssPropertyState getWinner() {
            if (inlineState().get() != null) {
                return inlineState().get();
            }
            if (authorState().get() != null) {
                return authorState().get();
            }
            return fxThemeState().get();
        }

        public CssContentMaker.PropertyState getCurrentStyle() {
            if (currentState == null) {
                currentState = builtinState().get();
                CssContentMaker.PropertyState model = modelState().get();
                CssContentMaker.CssPropertyState cssState = getWinner();
                if (cssState == null) {
                    if (model != null) {
                        currentState = model;
                    }
                } else {
                    if (model != null && cssState.getStyle() != null
                            && cssState.getStyle().getOrigin() == StyleOrigin.USER_AGENT) {
                        currentState = model;
                    } else {
                        currentState = cssState;
                    }
                }
            }
            return currentState;
        }

        public StyleOrigin getCurrentStyleOrigin() {
            CssContentMaker.PropertyState state = getCurrentStyle();
            if (state instanceof CssContentMaker.CssPropertyState) {
                CssContentMaker.CssPropertyState cssState = (CssContentMaker.CssPropertyState) state;
                return cssState.getStyle().getOrigin();
            } else {
                if (state instanceof CssContentMaker.BeanPropertyState) {
                    return StyleOrigin.USER;
                } else {
                    return null;
                }
            }
        }

        public boolean isInlineInherited() {
            boolean ret = false;
            CssContentMaker.CssPropertyState css = inlineCss.get();
            if (css != null) {
                ret = CssContentMaker.isInlineInherited(target, css);
            }
            return ret;
        }

        public Node getSourceNodeForInline() {
            Node ret = null;
            CssContentMaker.CssPropertyState css = inlineCss.get();
            if (css != null) {
                ret = CssContentMaker.getSourceNodeForStyle(target, propertyName().get());
            }
            return ret;
        }

        public List<CssContentMaker.CssPropertyState.CssStyle> getFxThemeHiddenByModel() {
            List<CssContentMaker.CssPropertyState.CssStyle> ret = new ArrayList<>();
            CssContentMaker.CssPropertyState ps = getWinner();
            List<CssContentMaker.CssPropertyState.CssStyle> notAppliedStyles = 
                    ps == null ? 
                    Collections.<CssContentMaker.CssPropertyState.CssStyle>emptyList() : 
                    ps.getNotAppliedStyles();
            boolean hasModel = modelState().get() != null;
            if (hasModel) {
                List<Style> allStyles = Deprecation.getMatchingStyles(getStyleable(), target);
                List<Style> matchingStyles = CssContentMaker.removeUserAgentStyles(allStyles);
                for (Style style : matchingStyles) {
                    CssContentMaker.CssPropertyState.CssStyle cssStyle = new CssContentMaker.CssPropertyState.CssStyle(style);
                    if (cssStyle.getOrigin() == StyleOrigin.USER_AGENT && !notAppliedStyles.contains(cssStyle)) {
                        if (getStyleable().getProperty().equals(cssStyle.getCssProperty())) {
                            cssStyle = CssContentMaker.retrieveStyle(matchingStyles, style);
                            ret.add(cssStyle);
                        }
                    }
                }
            }
            return ret;
        }

    }

    private CssContentMaker.CssPropertyState retrieveCssStyle(
            CssMetaData<?, ?> cssMeta, Collection<CssContentMaker.CssPropertyState> styles) {
        for (CssContentMaker.CssPropertyState prop : styles) {
            if (prop.getCssProperty().equals(cssMeta.getProperty())) {
                return prop;
            } else {
                if (prop.getSubProperties() != null) {
                    for (CssContentMaker.PropertyState sub : prop.getSubProperties()) {
                        if (sub.getCssProperty().equals(cssMeta.getProperty())) {
                            return (CssContentMaker.CssPropertyState) sub;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Node getNode() {
        return node;
    }

    @SuppressWarnings("rawtypes")
    public final Collection<CssProperty> getAllStyleables() {
        if (props == null) {
            props = new TreeSet<>();
            List<CssMetaData<? extends Styleable, ?>> cssMetaList = node.getCssMetaData();
            for (CssMetaData<? extends Styleable, ?> cssMeta : cssMetaList) {
                CssProperty mainProp = new CssProperty(this, cssMeta, node, fxomObject);
                props.add(mainProp);
                if (cssMeta.getSubProperties() != null) {
                    for (CssMetaData sub : cssMeta.getSubProperties()) {
                        CssProperty subProp = new CssProperty(this, mainProp, sub, node, fxomObject);
                        mainProp.getSubProperties().add(subProp);
                    }
                }
            }

            if (node instanceof Skinnable) {
                Skinnable skinnable = (Skinnable) node;
                Node skinNode = skinnable.getSkin().getNode();
                List<CssMetaData<? extends Styleable, ?>> skinList = skinNode.getCssMetaData();
                for (CssMetaData<? extends Styleable, ?> skinCssMeta : skinList) {
                    boolean found = false;
                    for (CssMetaData cssMeta : cssMetaList) {
                        if (skinCssMeta.getProperty().equals(cssMeta.getProperty())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        CssProperty mainProp = new CssProperty(this, skinCssMeta, skinNode, fxomObject);
                        props.add(mainProp);
                        if (skinCssMeta.getSubProperties() != null) {
                            for (CssMetaData<? extends Styleable, ?> sub : skinCssMeta.getSubProperties()) {
                                CssProperty subProp = new CssProperty(this, sub, node, fxomObject);
                                mainProp.getSubProperties().add(subProp);
                            }
                        }
                    }
                }
            }
        }
        return props;
    }

    public final Collection<CssContentMaker.CssPropertyState> getAuthorStyles() {
        if (author == null) {
            author = getAppliedStyles(StyleOrigin.AUTHOR);
        }
        return author;
    }

    public final Collection<CssContentMaker.CssPropertyState> getInlineStyles() {
        if (inline == null) {
            inline = getAppliedStyles(StyleOrigin.INLINE);
        }
        return inline;
    }

    public final Collection<CssContentMaker.CssPropertyState> getUserAgentStyles() {
        if (userAgent == null) {
            userAgent = getAppliedStyles(StyleOrigin.USER_AGENT);
        }
        return userAgent;
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class RuleComparator implements Comparator<MatchingRule> {

        @Override
        public int compare(MatchingRule t, MatchingRule t1) {
            int originComparaison = compareOrigin(
                    CssInternal.getOrigin(t.getRule()), CssInternal.getOrigin(t1.rule));
            int tnotApplied = countNotApplied(t.declarations);
            int t1notApplied = countNotApplied(t1.declarations);
            int notAppliedComparaisons = tnotApplied - t1notApplied;

            if (originComparaison == 0) {// Same origin, not Applied count is what is important. The less, the stronger
                return notAppliedComparaisons;
            } else {
                return originComparaison;
            }
        }

    }

    private static int compareOrigin(StyleOrigin toCompare, StyleOrigin other) {
        int index1 = ORDERED_ORIGIN.indexOf(toCompare);
        int index2 = ORDERED_ORIGIN.indexOf(other);
        return index2 - index1;

    }

    private static int countNotApplied(List<MatchingDeclaration> declarations) {
        int count = 0;
        for (MatchingDeclaration decl : declarations) {
            if (!decl.isApplied()) {
                count += 1;
            }
        }
        return count;
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class MatchingRule {

        private final Rule rule;
        private final String selector;
        private final List<MatchingDeclaration> declarations = new ArrayList<>();

        private MatchingRule(Rule rule, String selector) {
            this.rule = rule;
            this.selector = selector;
        }

        public Rule getRule() {
            return rule;
        }

        public String getSelector() {
            return selector;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + (this.rule != null ? this.rule.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MatchingRule)) {
                return false;
            }
            MatchingRule mr = (MatchingRule) obj;
            return rule.equals(mr.rule);
        }

        private void addDeclarations(List<MatchingDeclaration> values) {
            declarations.addAll(values);
        }

        public List<MatchingDeclaration> getDeclarations() {
            return Collections.unmodifiableList(declarations);
        }

        @Override
        public String toString() {
            return rule.getSelectors().toString();
        }
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class MatchingDeclaration {

        private final CssContentMaker.CssPropertyState.CssStyle style;
        private final CssContentMaker.CssPropertyState prop;
        private final boolean applied;
        private final boolean lookup;

        MatchingDeclaration(CssContentMaker.CssPropertyState.CssStyle style, 
                CssContentMaker.CssPropertyState prop, boolean applied, boolean lookup) {
            this.style = style;
            this.prop = prop;
            this.applied = applied;
            this.lookup = lookup;
        }

        /**
         * @return the style
         */
        public CssContentMaker.CssPropertyState.CssStyle getStyle() {
            return style;
        }

        /**
         * @return the prop
         */
        public CssContentMaker.CssPropertyState getProp() {
            return prop;
        }

        /**
         * @return the applied
         */
        public boolean isApplied() {
            return applied;
        }

        public boolean isLookup() {
            return lookup;
        }
    }

    // This method add sub properties instead of compund property.
    private static void addSubProperties(
            Collection<CssContentMaker.CssPropertyState> source, 
            Collection<CssContentMaker.CssPropertyState> target) {
        for (CssContentMaker.CssPropertyState p : source) {
            if (p.getSubProperties().isEmpty()) {
                target.add(p);
            } else {
                for (CssContentMaker.PropertyState sub : p.getSubProperties()) {
                    target.add((CssContentMaker.CssPropertyState) sub);
                }
            }
        }
    }

    // Sorted according to Author/User Agent and applied / not applied
    public final List<MatchingRule> getMatchingRules() {
        if (matchingRules == null) {
            Collection<CssContentMaker.CssPropertyState> styledProperties = new TreeSet<>();
            addSubProperties(getUserAgentStyles(), styledProperties);
            addSubProperties(getAuthorStyles(), styledProperties);
                // We need them to have the exhaustive set of styled properties.
            // We compute the rules based on the set of properties.
            addSubProperties(getInlineStyles(), styledProperties);
            matchingRules = new HashMap<>();
            for (CssContentMaker.CssPropertyState cssP : styledProperties) {
                List<CssContentMaker.PropertyState> l = cssP.getSubProperties();
                if (l.isEmpty()) {
                    addMatchingDeclaration(cssP);
                } else {
                    for (CssContentMaker.PropertyState pp : l) {
                        CssContentMaker.CssPropertyState cssSubP = (CssContentMaker.CssPropertyState) pp;
                        addMatchingDeclaration(cssSubP);
                    }
                }
            }
            for (Map.Entry<MatchingRule, List<MatchingDeclaration>> entry : matchingRules.entrySet()) {
                MatchingRule rule = entry.getKey();
                // Filterout the Inline
                if (CssInternal.getOrigin(rule.getRule()) != StyleOrigin.INLINE) {
                    rule.addDeclarations(entry.getValue());
                    sortedMatchingRules.add(rule);
                }
            }
            Collections.<MatchingRule>sort(sortedMatchingRules, new RuleComparator());
        }
        return sortedMatchingRules;
    }

    private void addMatchingDeclaration(CssPropertyState cssP) {
        addMatchingDeclaration(cssP, cssP.getStyle(), true, false);
        for (CssPropertyState.CssStyle s : cssP.getNotAppliedStyles()) {
            addMatchingDeclaration(cssP, s, false, false);
        }
    }

    private void addMatchingDeclaration(
            CssPropertyState cssP, CssPropertyState.CssStyle style, boolean applied, boolean isLookup) {
        MatchingRule mr = new MatchingRule(style.getCssRule(), style.getSelector());
        List<MatchingDeclaration> lst = matchingRules.get(mr);
        if (lst == null) {
            lst = new ArrayList<>();
            matchingRules.put(mr, lst);
        }
        MatchingDeclaration pmr = new MatchingDeclaration(style, cssP, applied, isLookup);
        boolean found = false;
        for (MatchingDeclaration d : lst) {
            if (d.style.getCssProperty().equals(style.getCssProperty())) {
                found = true;
                break;
            }
        }

        if (!found) {
            lst.add(pmr);
        }
        for (CssContentMaker.CssPropertyState.CssStyle lookup : style.getLookupChain()) {
            addMatchingDeclaration(cssP, lookup, applied, true);
        }
    }

    @SuppressWarnings("rawtypes")
    private Set<CssContentMaker.CssPropertyState> getAppliedStyles(StyleOrigin origin) {
        SortedSet<CssContentMaker.CssPropertyState> propertyStates = new TreeSet<>();

//            if (origin == StyleOrigin.USER_AGENT) {
//                System.out.println("===========================");
//                System.out.println("getAppliedStyles() called!");
//                System.out.println("===========================");
//                for (StyleableProperty sp : map.keySet()) {
//                    System.out.println("---------------------------");
//                    System.out.println("Styleable property: " + sp);
//                    System.out.println("---------------------------");
//                    List<Style> styles = map.get(sp);
//                    CssContentMaker.printStyles(styles);
//                }
//                System.out.println("\n\n\n");
//            }
        for (Map.Entry<StyleableProperty, List<Style>> entry : map.entrySet()) {//NOI18N
            StyleableProperty<?> value = entry.getKey();
//                System.out.println("\nStyleable property: " + value);
            assert entry.getValue() != null;
            assert !entry.getValue().isEmpty();
            Style st = entry.getValue().get(0);
            StyleOrigin o = CssInternal.getOrigin(st);
//                printStyle(st);
                /* If this origin is equals to the passed one, this is the nominal case.
             * If this property contains sub properties (eg:background-fills), then we need to check
             * each sub property.
             */
            CssMetaData<? extends Styleable, ?> cssMetaList = value.getCssMetaData();
            if (o == origin || cssMetaList.getSubProperties() != null) {
                    // Need the first style to compute the value
                // We have at least a style. The first one is the winner.
                String cssValue = CssValueConverter.toCssString(cssMetaList.getProperty(),
                        st.getDeclaration().getRule(), value.getValue());

                CssContentMaker.CssPropertyState pState = new CssContentMaker.CssPropertyState(value, cssMetaList, cssValue);

                /* 
                 * Each sub property can be ruled by a specific Origin, 
                 * we need to check if the sub property is in a rule of the passed origin.
                 * For example, we can have background-radius set by fxTheme 
                 * and background-color set by inline or author.
                 */
                if (cssMetaList.getSubProperties() != null) {
                    for (CssMetaData sub : cssMetaList.getSubProperties()) {
                        List<CssContentMaker.CssPropertyState.CssStyle> notApplied = CssContentMaker.getNotAppliedStyles(entry.getValue(), node, sub);
                        for (Style style : entry.getValue()) {
                            StyleOrigin styleOrigin = CssInternal.getOrigin(style);
                            if (style.getDeclaration().getProperty().equals(sub.getProperty())
                                    && (styleOrigin == origin)) {
                                CssContentMaker.CssPropertyState.CssStyle cssStyle = CssContentMaker.retrieveStyle(entry.getValue(), style);
                                String subCssValue = CssValueConverter.toCssString(sub.getProperty(), style.getDeclaration().getRule(), value.getValue());
                                CssContentMaker.CssSubPropertyState subCss = new CssContentMaker.CssSubPropertyState(value, sub, subCssValue);
                                subCss.setStyle(cssStyle);
                                subCss.getNotAppliedStyles().addAll(notApplied);
                                pState.getSubProperties().add(subCss);
                            }
                        }
                    }
                    // eg: -fx-font set
                    CssContentMaker.CssPropertyState.CssStyle style = CssContentMaker.retrieveStyle(entry.getValue(), st);
                    pState.setStyle(style);
                    if (!st.getDeclaration().getProperty().equals(cssMetaList.getProperty())) {
                        style.setUnused();
                    }
                } else {
                        // Single style for this single property.
                    // Transform the flat list into a chain of lookup.
                    CssContentMaker.CssPropertyState.CssStyle style = CssContentMaker.retrieveStyle(entry.getValue(), st);
                    pState.setStyle(style);
                }
                List<Style> applied = new ArrayList<>();
                applied.add(st);
                pState.getNotAppliedStyles().addAll(CssContentMaker.getNotAppliedStyles(applied, node, cssMetaList));
                /*
                 * In case the origin is not the same and no sub properties have been found for 
                 * the passed origin, then the property is not taken into consideration
                 */
                if (o == origin || !pState.getSubProperties().isEmpty()) {
                    propertyStates.add(pState);
                }
            }
        }
        return propertyStates;
    }
}
