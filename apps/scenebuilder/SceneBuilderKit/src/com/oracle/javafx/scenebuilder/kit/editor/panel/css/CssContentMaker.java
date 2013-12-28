/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState.CssStyle;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.NodeCssState.CssProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Style;
import javafx.css.StyleOrigin;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Skinnable;

/**
 * This class construct the model exposed by the CSS Panel.
 *
 */
public class CssContentMaker {

    private static final List<StyleOrigin> ORDERED_ORIGIN = new ArrayList<>();

    static {
        ORDERED_ORIGIN.add(StyleOrigin.USER_AGENT);// fxTheme : modena/caspian
        ORDERED_ORIGIN.add(StyleOrigin.USER);//Bean API Call

        ORDERED_ORIGIN.add(StyleOrigin.AUTHOR);//CSS files
        ORDERED_ORIGIN.add(StyleOrigin.INLINE);//Style property

    }

    private CssContentMaker() {
        assert false;
    }

    /*
     *
     * Public methods
     *
     */
    public static <N extends Node> PropertyState initialValue(N n, CssMetaData<N, ?> sub) {
        PropertyState val = null;

        try {
            Object fxValue;
            String cssValue;
            Object value = sub.getInitialValue(n);
            if (value == null) {
                cssValue = "none";//NOI18N
                fxValue = cssValue;
            } else {
                fxValue = sub.getInitialValue(n);
                cssValue = CssValueConverter.toCssString(sub.getProperty(), n);
            }
            val = newInitialPropertyState(fxValue, cssValue, n, sub);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage() + " " + ex);
            // Ok no initial value or InitialValue bug.
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    public static <N extends Node> PropertyState initialValue(N n, CssProperty complex,
            CssMetaData<N, ?> sub) {
        PropertyState val = null;

        try {
            Object fxValue;
            String cssValue;
            Object complexInitial = complex.getStyleable().getInitialValue(complex.getTarget());
            if (complexInitial == null) {
                cssValue = "none";//NOI18N
                fxValue = cssValue;
            } else {
                fxValue = sub.getInitialValue(n);
                cssValue = CssValueConverter.toCssString(sub.getProperty(), complexInitial);
            }
            val = newInitialPropertyState(fxValue, cssValue, n, sub);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage() + " " + ex);
            // Ok no initial value or InitialValue bug.
        }
        return val;
    }

    @SuppressWarnings("rawtypes")
    public static <N extends Node> PropertyState modelValue(N node, CssMetaData<?, ?> cssMeta, FXOMObject fxomObject) {
        PropertyState val = null;

        if (fxomObject == null) {
            // In this case, we are handling a sub-component, no model value then.
            return null;
        }
        // First retrieve the java bean property and check if it is overriden by the inspector.
        String beanPropName = CssUtils.getBeanPropertyName(node, cssMeta);
        if (beanPropName == null) {
            // No corresponding java bean property
            return null;
        }
        PropertyName beanPropertyName = new PropertyName(beanPropName);
        assert fxomObject instanceof FXOMInstance;
        FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
        ValuePropertyMetadata propMeta
                = Metadata.getMetadata().queryValueProperty(fxomInstance, beanPropertyName);
        if (propMeta == null) {
            // No corresponding metadata
            return null;
        }
        boolean overriden = false;
        Object defaultValue = propMeta.getDefaultValueObject();
        Object propertyValue = propMeta.getValueObject(fxomInstance);
        if ((propertyValue == null) || (defaultValue == null)) {
            if (propertyValue != defaultValue) {
                overriden = true;
            }
        } else if (!propertyValue.equals(defaultValue)) {
            overriden = true;
        }

        if (overriden) {
            // We have an override.
            val = new BeanPropertyState(propMeta, cssMeta.getProperty(), propertyValue,
                    CssValueConverter.toCssString(cssMeta.getProperty(), propertyValue));
            // An overriden can have sub properties
            if (cssMeta.getSubProperties() != null && !cssMeta.getSubProperties().isEmpty()) {
                for (CssMetaData sub : cssMeta.getSubProperties()) {
                    // Create a virtual sub property
                    PropertyState subProp = new BeanPropertyState(propMeta, sub.getProperty(),
                            propertyValue, CssValueConverter.toCssString(sub.getProperty(), propertyValue));
                    val.getSubProperties().add(subProp);
                }
            }
        }
        return val;
    }

    public static Node getSourceNodeForStyle(Object component, String property) {
        Node ret = null;
        Node n = CssUtils.getNode(component);
        if (n != null) {
            if (n.getStyle() != null && n.getStyle().contains(property)) {
                ret = n;
            } else {
                Parent p = n.getParent();
                while (p != null) {
                    String s = p.getStyle();
                    if (s != null && s.contains(property)) {
                        ret = p;
                        break;
                    }
                    p = p.getParent();
                }
            }
        }
        return ret;
    }

    public static boolean isInlineInherited(Object component, CssPropertyState cssProperty) {
        boolean isInherited = false;
        Node node = CssUtils.getNode(component);

        if (node == null) {
            return false;
        }

        // Not located on this node, must be inherited then
        if (node.getStyle() == null) {
            return true;
        }

        if (!containsInStyle(cssProperty, node.getStyle())) {
            isInherited = true;
        }

        return isInherited;
    }

    public static boolean containsPseudoState(String selector) {
        return selector.contains(":");//NOI18N
    }


    /*
     *
     * Private methods
     *
     */
    private static boolean containsInStyle(CssPropertyState prop, String style) {
        return style.contains(prop.getCssProperty());
    }

    public static NodeCssState getCssState(Object selectedObject) {
        Node node = CssUtils.getSelectedNode(selectedObject);
        if (node == null) {
            return null;
        }
        Parent p = null;
        double current = 1;
        try {
            if (node.getScene() == null) {
                // The node is not visible (ContextMenu, Tooltip, ...)
                // A node MUST be in the scene to allow for CSS content collect,
                // so we add it (temporarily) to the scene. 
                Node inScene = CssUtils.getFirstAncestorWithNonNullScene(node);
                if (inScene == null) {
                    // May happen if the Content Panel is not present
                    return null;
                }
                p = inScene.getParent();
                current = node.getOpacity();
                node.setOpacity(0);
                CssUtils.addToParent(p, node);
            }
            NodeCssState state = new NodeCssState(CssInternal.collectCssState(node), node, getFXOMObject(selectedObject));
            return state;
        } finally {
            if (p != null) {
                CssUtils.removeFromParent(p, node);
                node.setOpacity(current);
            }
        }
    }

    private static FXOMObject getFXOMObject(Object selectedObject) {
        if (selectedObject instanceof FXOMObject) {
            return (FXOMObject) selectedObject;
        } else {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static <N extends Node> InitialPropertyState newInitialPropertyState(
            Object fxValue, String cssValue, N n, CssMetaData<?, ?> cssMeta) {
        InitialPropertyState val
                = new InitialPropertyState(cssMeta.getProperty(), fxValue, cssValue);
        if (cssMeta.getSubProperties() != null && !cssMeta.getSubProperties().isEmpty()) {
            for (CssMetaData sub : cssMeta.getSubProperties()) {
                Object subValue
                        = CssValueConverter.getSubPropertyValue(sub.getProperty(), fxValue);
                String subCssValue = CssValueConverter.toCssString(subValue);
                PropertyState subProp
                        = new InitialPropertyState(sub.getProperty(), subValue, subCssValue);
                val.getSubProperties().add(subProp);
            }
        }
        return val;
    }

    // Retrieve the styles associated to the value. This is the case of lookup (or variable)
    @SuppressWarnings("rawtypes")
    private static CssStyle retrieveStyle(List<Style> styles, Style style) {
        CssStyle st = new CssStyle(style);
        ParsedValue parsedValue = style.getDeclaration().getParsedValue();
        assert parsedValue instanceof ParsedValueImpl;
        ParsedValueImpl parsedValueImpl = (ParsedValueImpl) parsedValue;
        if (parsedValueImpl.isContainsLookups() || parsedValueImpl.isLookup()) {
            retrieveStylesFromParsedValue(styles, st, style.getDeclaration().getParsedValue());
        }
        return st;
    }

    @SuppressWarnings("rawtypes")
    private static void retrieveStylesFromParsedValue(
            List<Style> lst, CssStyle current, ParsedValue<?, ?> parsedValue) {
        final Object val = parsedValue.getValue();
        if (val instanceof ParsedValue[][]) {
            // If ParsedValue is a layered sequence of values, resolve the lookups for each.

            final ParsedValue[][] layers2 = (ParsedValue[][]) val;
            for (ParsedValue[] layers : layers2) {
                for (ParsedValue layer : layers) {
                    if (layer == null) {
                        continue;
                    }
                    retrieveStylesFromParsedValue(lst, current, layer);
                }
            }
        } else if (val instanceof ParsedValue[]) {
            // If ParsedValue is a sequence of values, resolve the lookups for each.
            final ParsedValue[] layers = (ParsedValue[]) val;
            for (ParsedValue layer : layers) {
                if (layer == null) {
                    continue;
                }
                retrieveStylesFromParsedValue(lst, current, layer);
            }
        } else {
            if (val instanceof String) {
                String value = (String) val;
                for (Style info : lst) {
                    if (value.equals(info.getDeclaration().getProperty())) {
                        // Ok matching Style
                        CssStyle cssStyle = retrieveStyle(lst, info);
                        current.getLookupChain().add(cssStyle);
                    }
                }
            }
        }
    }

    private static List<CssStyle> getNotAppliedStyles(
            List<Style> appliedStyles, Node node, CssMetaData<?, ?> cssMeta) {
        List<CssStyle> ret = new ArrayList<>();

        List<Style> allStyles = Deprecation.getMatchingStyles(cssMeta, node);
//        System.out.println("===========================");
//        System.out.println("getNotAppliedStyles() called!");
//        System.out.println("===========================");
//        System.out.println("\n\n");
//        printStyles(allStyles);
        List<Style> matchingStyles = removeUserAgentStyles(allStyles);
        List<Style> notApplied = new ArrayList<>();
        for (Style style : matchingStyles) {
            if (!appliedStyles.contains(style)) {
                notApplied.add(style);
            }
        }
        for (Style style : notApplied) {
            if (style.getDeclaration().getProperty().equals(cssMeta.getProperty())) {
                // We need to retrieve from allStyles, in case a lookup is shared by appliedStyles and notApplied
                CssStyle cssStyle = retrieveStyle(matchingStyles, style);
                ret.add(cssStyle);
            }
        }
        return ret;
    }

    private static List<Style> removeUserAgentStyles(List<Style> allStyles) {
        // With SB 2, we apply explicitly Modena/Caspian theme css on user scene graph.
        // The rules that appear with an AUTHOR origin has already been considered as USER_AGENT.
        // So when an internal css method (such as impl_getMatchingStyles()) is called,
        // we need here to remove all USER_AGENT styles, to avoid doublons.
        List<Style> matchingStyles = new ArrayList<>();
        for (Style style : allStyles) {
            if (!(style.getDeclaration().getRule().getOrigin() == StyleOrigin.USER_AGENT)) {
                matchingStyles.add(style);
            }
        }
        return matchingStyles;
    }
    
//    private static void printStyles(List<Style> styles) {
//        for (Style style : styles) {
//            printStyle(style);
//        }
//
//    }
//    private static void printStyle(Style style) {
//        System.out.println(style.getDeclaration().getRule().getOrigin() + " ==> STYLE " + style.getDeclaration());
//        System.out.println("--> css url = " + style.getDeclaration().getRule().getStylesheet().getUrl());
//    }

    /*
     *
     * Public classes
     *
     */
    public static abstract class PropertyState implements Comparable<PropertyState> {

        protected PropertyState(String cssValue) {
            this.cssValue = cssValue;
        }
        private final List<CssStyle> notAppliedStyles = new ArrayList<>();
        private final List<PropertyState> lst = new ArrayList<>();
        private final String cssValue;

        public abstract String getCssProperty();

        public abstract Object getFxValue();

        public String getCssValue() {
            return cssValue;
        }

        public List<PropertyState> getSubProperties() {
            return lst;
        }

        public List<CssStyle> getNotAppliedStyles() {
            return notAppliedStyles;
        }

        @Override
        public int compareTo(PropertyState t) {
            PropertyState ps = t;
            return getCssProperty().compareTo(ps.getCssProperty());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PropertyState ps = (PropertyState) obj;
            return getCssProperty().compareTo(ps.getCssProperty()) == 0;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.notAppliedStyles);
            hash = 53 * hash + Objects.hashCode(this.lst);
            hash = 53 * hash + Objects.hashCode(this.cssValue);
            return hash;
        }
    }

    public static class BeanPropertyState extends PropertyState {

        PropertyMetadata propMeta;
        private String cssPropName;
        private Object fxValue;

        BeanPropertyState(PropertyMetadata propMeta, String cssPropName, Object fxValue, String cssValue) {
            super(cssValue);
            this.propMeta = propMeta;
            this.cssPropName = cssPropName;
            this.fxValue = fxValue;
        }

        @Override
        public String getCssProperty() {
            return cssPropName;
        }

        public PropertyMetadata getPropertyMeta() {
            return propMeta;
        }

        @Override
        public Object getFxValue() {
            return fxValue;
        }
    }

    public static class CssPropertyState extends PropertyState {

        protected final StyleableProperty<?> value;
        protected final CssMetaData<?, ?> cssMeta;
        private CssStyle style;

        CssPropertyState(StyleableProperty<?> value, CssMetaData<?, ?> cssMeta, String cssValue) {
            super(cssValue);
            this.value = value;
            this.cssMeta = cssMeta;
        }

        @Override
        public String getCssProperty() {
            return cssMeta.getProperty();
        }

        public CssStyle getStyle() {
            return style;
        }

        void setStyle(CssStyle style) {
            this.style = style;
        }

        @Override
        public Object getFxValue() {
            return value.getValue();
        }

        public static class CssStyle {

            private final Style style;
            private boolean used = true;
            private final List<CssStyle> lookupSet = new ArrayList<>();

            private CssStyle(Style style) {
                this.style = style;
            }

            private void setUnused() {
                used = false;
            }

            public boolean isUsed() {
                return used;
            }

            public Style getStyle() {
                return style;
            }

            public String getCssProperty() {
                return style.getDeclaration().getProperty();
            }

            @SuppressWarnings("rawtypes")
            public ParsedValue getParsedValue() {
                return style.getDeclaration().getParsedValue();
            }

            public StyleOrigin getOrigin() {
                return CssInternal.getOrigin(style);
            }

            public String getSelector() {
                String sel = style.getSelector().toString();
                if (sel.startsWith("*")) {//NOI18N
                    sel = sel.substring(1);
                }
                return sel;
            }

            public Rule getCssRule() {
                return style.getDeclaration().getRule();
            }

            public URL getUrl() {
                // Workaround!
                Rule rule = getCssRule();
                if (rule == null) {
                    return null;
                } else {
                    try {
                        return new URL(rule.getStylesheet().getUrl());
                    } catch (MalformedURLException ex) {
                        System.out.println(ex.getMessage() + " " + ex);
                        return null;
                    }
                }
            }

            @Override
            public String toString() {
                return style.toString();
            }

            public List<CssStyle> getLookupChain() {
                return lookupSet;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 47 * hash + (this.style != null ? this.style.hashCode() : 0);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof CssStyle)) {
                    return false;
                }
                CssStyle cssStyle = (CssStyle) obj;
                return style.equals(cssStyle.style);
            }
        }
    }

    /*
     *
     * Private classes
     *
     */
    private static class CssSubPropertyState extends CssPropertyState {

        CssSubPropertyState(StyleableProperty<?> value, CssMetaData<?, ?> cssMeta, String cssValue) {
            super(value, cssMeta, cssValue);
        }

        @Override
        public Object getFxValue() {
            return CssValueConverter.getSubPropertyValue(cssMeta.getProperty(), value.getValue());
        }
    }

    private static class InitialPropertyState extends PropertyState {

        private String name;
        private Object fxValue;

        InitialPropertyState(String name, Object fxValue, String cssValue) {
            super(cssValue);
            this.name = name;
            this.fxValue = fxValue;
        }

        @Override
        public String getCssProperty() {
            return name;
        }

        @Override
        public Object getFxValue() {
            return fxValue;
        }
    }

    public static class NodeCssState {

        @SuppressWarnings("rawtypes")
        private final Map<StyleableProperty, List<Style>> map;
        private final Node node;
        private final FXOMObject fxomObject;
        private Collection<CssPropertyState> author;
        private Collection<CssPropertyState> inline;
        private Collection<CssPropertyState> userAgent;
        private Map<MatchingRule, List<MatchingDeclaration>> matchingRules;
        private List<MatchingRule> sortedMatchingRules = new ArrayList<>();
        private Collection<CssProperty> props;

        @SuppressWarnings("rawtypes")
        private NodeCssState(Map<StyleableProperty, List<Style>> map, Node node, FXOMObject fxomObject) {
            this.map = map;
            this.node = node;
            this.fxomObject = fxomObject;
            getAuthorStyles();
            getInlineStyles();
            getUserAgentStyles();
            getMatchingRules();
            getAllStyleables();
        }

        @SuppressWarnings("rawtypes")
        public static class CssProperty implements Comparable<CssProperty> {

            private final CssMetaData cssMeta;
            private final CssProperty mainProperty;
            private final Node target;
            private final List<CssProperty> sub = new ArrayList<>();
            private final ObjectProperty<String> name = new SimpleObjectProperty<>();
            private final ObjectProperty<PropertyState> builtin = new SimpleObjectProperty<>();
            private final ObjectProperty<CssPropertyState> fxTheme = new SimpleObjectProperty<>();
            private final ObjectProperty<CssPropertyState> authorCss = new SimpleObjectProperty<>();
            private final ObjectProperty<CssPropertyState> inlineCss = new SimpleObjectProperty<>();
            private final ObjectProperty<PropertyState> fxmlModel = new SimpleObjectProperty<>();
            private PropertyState currentState;

            @SuppressWarnings("rawtypes")
            CssProperty(NodeCssState nodeCssState, CssMetaData cssMeta, Node target, FXOMObject fxomObject) {
                this(nodeCssState, null, cssMeta, target, fxomObject);
            }

            CssProperty(NodeCssState nodeCssState, CssProperty mainProperty,
                    CssMetaData cssMeta, Node target, FXOMObject fxomObject) {
                this.mainProperty = mainProperty;
                this.cssMeta = cssMeta;
                this.target = target;
                name.setValue(cssMeta.getProperty());
                CssPropertyState inlineState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getInlineStyles());
                if (inlineState != null) {
                    inlineCss.setValue(inlineState);
                }
                CssPropertyState authorState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getAuthorStyles());
                if (authorState != null) {
                    authorCss.setValue(authorState);
                }
                CssPropertyState fxThemeState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getUserAgentStyles());
                if (fxThemeState != null) {
                    fxTheme.setValue(fxThemeState);
                }
                @SuppressWarnings("unchecked")
                PropertyState builtinState = CssContentMaker.initialValue(target, mainProperty == null ? this : mainProperty, cssMeta);
                assert builtinState != null;
                builtin.setValue(builtinState);

                PropertyState modelState = CssContentMaker.modelValue(target, cssMeta, fxomObject);
                if (modelState != null) {
                    fxmlModel.setValue(modelState);
                }
            }

            public ObjectProperty<PropertyState> builtinState() {
                return builtin;
            }

            public ObjectProperty<PropertyState> modelState() {
                return fxmlModel;
            }

            public ObjectProperty<CssPropertyState> fxThemeState() {
                return fxTheme;
            }

            public ObjectProperty<CssPropertyState> authorState() {
                return authorCss;
            }

            public ObjectProperty<CssPropertyState> inlineState() {
                return inlineCss;
            }

            public ObjectProperty<String> propertyName() {
                return name;
            }

            @SuppressWarnings("rawtypes")
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
            public CssPropertyState getWinner() {
                if (inlineState().get() != null) {
                    return inlineState().get();
                }
                if (authorState().get() != null) {
                    return authorState().get();
                }
                return fxThemeState().get();
            }

            public PropertyState getCurrentStyle() {
                if (currentState == null) {
                    currentState = builtinState().get();
                    PropertyState model = modelState().get();
                    CssPropertyState cssState = getWinner();
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
                PropertyState state = getCurrentStyle();
                if (state instanceof CssPropertyState) {
                    CssPropertyState cssState = (CssPropertyState) state;
                    return cssState.getStyle().getOrigin();
                } else {
                    if (state instanceof BeanPropertyState) {
                        return StyleOrigin.USER;
                    } else {
                        return null;
                    }
                }
            }

            public boolean isInlineInherited() {
                boolean ret = false;
                CssPropertyState css = inlineCss.get();
                if (css != null) {
                    ret = CssContentMaker.isInlineInherited(target, css);
                }
                return ret;
            }

            public Node getSourceNodeForInline() {
                Node ret = null;
                CssPropertyState css = inlineCss.get();
                if (css != null) {
                    ret = CssContentMaker.getSourceNodeForStyle(target, propertyName().get());
                }
                return ret;
            }

            public List<CssStyle> getFxThemeHiddenByModel() {
                List<CssStyle> ret = new ArrayList<>();
                CssPropertyState ps = getWinner();
                List<CssStyle> notAppliedStyles = ps == null ? Collections.<CssStyle>emptyList() : ps.getNotAppliedStyles();
                boolean hasModel = modelState().get() != null;
                if (hasModel) {
                    List<Style> allStyles = Deprecation.getMatchingStyles(getStyleable(), target);
                    List<Style> matchingStyles = removeUserAgentStyles(allStyles);
                    for (Style style : matchingStyles) {
                        CssStyle cssStyle = new CssStyle(style);
                        if (cssStyle.getOrigin() == StyleOrigin.USER_AGENT && !notAppliedStyles.contains(cssStyle)) {
                            if (getStyleable().getProperty().equals(cssStyle.getCssProperty())) {
                                cssStyle = retrieveStyle(matchingStyles, style);
                                ret.add(cssStyle);
                            }
                        }
                    }
                }
                return ret;
            }

        }

        private CssPropertyState retrieveCssStyle(CssMetaData<?, ?> cssMeta, Collection<CssPropertyState> styles) {
            for (CssPropertyState prop : styles) {
                if (prop.getCssProperty().equals(cssMeta.getProperty())) {
                    return prop;
                } else {
                    if (prop.getSubProperties() != null) {
                        for (PropertyState sub : prop.getSubProperties()) {
                            if (sub.getCssProperty().equals(cssMeta.getProperty())) {
                                return (CssPropertyState) sub;
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

        public final Collection<CssPropertyState> getAuthorStyles() {
            if (author == null) {
                author = getAppliedStyles(StyleOrigin.AUTHOR);
            }
            return author;
        }

        public final Collection<CssPropertyState> getInlineStyles() {
            if (inline == null) {
                inline = getAppliedStyles(StyleOrigin.INLINE);
            }
            return inline;
        }

        public final Collection<CssPropertyState> getUserAgentStyles() {
            if (userAgent == null) {
                userAgent = getAppliedStyles(StyleOrigin.USER_AGENT);
            }
            return userAgent;
        }

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

        public static class MatchingDeclaration {

            private final CssStyle style;
            private final CssPropertyState prop;
            private final boolean applied;
            private final boolean lookup;

            MatchingDeclaration(CssStyle style, CssPropertyState prop, boolean applied, boolean lookup) {
                this.style = style;
                this.prop = prop;
                this.applied = applied;
                this.lookup = lookup;
            }

            /**
             * @return the style
             */
            public CssStyle getStyle() {
                return style;
            }

            /**
             * @return the prop
             */
            public CssPropertyState getProp() {
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
        private static void addSubProperties(Collection<CssPropertyState> source, Collection<CssPropertyState> target) {
            for (CssPropertyState p : source) {
                if (p.getSubProperties().isEmpty()) {
                    target.add(p);
                } else {
                    for (PropertyState sub : p.getSubProperties()) {
                        target.add((CssPropertyState) sub);
                    }
                }
            }
        }

        // Sorted according to Author/User Agent and applied / not applied
        public final List<MatchingRule> getMatchingRules() {
            if (matchingRules == null) {
                Collection<CssPropertyState> styledProperties = new TreeSet<>();
                addSubProperties(getUserAgentStyles(), styledProperties);
                addSubProperties(getAuthorStyles(), styledProperties);
                // We need them to have the exhaustive set of styled properties.
                // We compute the rules based on the set of properties.
                addSubProperties(getInlineStyles(), styledProperties);
                matchingRules = new HashMap<>();
                for (CssPropertyState cssP : styledProperties) {
                    List<PropertyState> l = cssP.getSubProperties();
                    if (l.isEmpty()) {
                        addMatchingDeclaration(cssP);
                    } else {
                        for (PropertyState pp : l) {
                            CssPropertyState cssSubP = (CssPropertyState) pp;
                            addMatchingDeclaration(cssSubP);
                        }
                    }
                }
                for (Entry<MatchingRule, List<MatchingDeclaration>> entry : matchingRules.entrySet()) {
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
            for (CssStyle s : cssP.getNotAppliedStyles()) {
                addMatchingDeclaration(cssP, s, false, false);
            }
        }

        private void addMatchingDeclaration(CssPropertyState cssP, CssStyle style, boolean applied, boolean isLookup) {
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
            for (CssStyle lookup : style.getLookupChain()) {
                addMatchingDeclaration(cssP, lookup, applied, true);
            }
        }

        @SuppressWarnings("rawtypes")
        private Set<CssPropertyState> getAppliedStyles(StyleOrigin origin) {
            SortedSet<CssPropertyState> propertyStates = new TreeSet<>();

//            if (origin == StyleOrigin.AUTHOR) {
//                System.out.println("===========================");
//                System.out.println("getAppliedStyles() called!");
//                System.out.println("===========================");
//                for (StyleableProperty sp : map.keySet()) {
//                    System.out.println("---------------------------");
//                    System.out.println("Styleable property: " + sp);
//                    System.out.println("---------------------------");
//                    List<Style> styles = map.get(sp);
//                    printStyles(styles);
//                }
//                System.out.println("\n\n\n");
//            }
            for (Entry<StyleableProperty, List<Style>> entry : map.entrySet()) {//NOI18N
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

                    CssPropertyState pState = new CssPropertyState(value, cssMetaList, cssValue);

                    /* 
                     * Each sub property can be ruled by a specific Origin, 
                     * we need to check if the sub property is in a rule of the passed origin.
                     * For example, we can have background-radius set by fxTheme 
                     * and background-color set by inline or author.
                     */
                    if (cssMetaList.getSubProperties() != null) {
                        for (CssMetaData sub : cssMetaList.getSubProperties()) {
                            List<CssStyle> notApplied = getNotAppliedStyles(entry.getValue(), node, sub);
                            for (Style style : entry.getValue()) {
                                StyleOrigin styleOrigin = CssInternal.getOrigin(style);
                                if (style.getDeclaration().getProperty().equals(sub.getProperty())
                                        && (styleOrigin == origin)) {
                                    CssStyle cssStyle = retrieveStyle(entry.getValue(), style);
                                    String subCssValue = CssValueConverter.toCssString(sub.getProperty(), style.getDeclaration().getRule(), value.getValue());
                                    CssSubPropertyState subCss = new CssSubPropertyState(value, sub, subCssValue);
                                    subCss.setStyle(cssStyle);
                                    subCss.getNotAppliedStyles().addAll(notApplied);
                                    pState.getSubProperties().add(subCss);
                                }
                            }
                        }
                        // eg: -fx-font set
                        CssStyle style = retrieveStyle(entry.getValue(), st);
                        pState.setStyle(style);
                        if (!st.getDeclaration().getProperty().equals(cssMetaList.getProperty())) {
                            style.setUnused();
                        }
                    } else {
                        // Single style for this single property.
                        // Transform the flat list into a chain of lookup.
                        CssStyle style = retrieveStyle(entry.getValue(), st);
                        pState.setStyle(style);
                    }
                    List<Style> applied = new ArrayList<>();
                    applied.add(st);
                    pState.getNotAppliedStyles().addAll(getNotAppliedStyles(applied, node, cssMetaList));
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
}
