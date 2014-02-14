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
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.NodeCssState.CssProperty;
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
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * This class construct the model exposed by the CSS Panel.
 *
 */
public class CssContentMaker {

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
        if (!propMeta.isReadWrite()) {
            // R/O : no overridden
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
    protected static CssStyle retrieveStyle(List<Style> styles, Style style) {
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

    protected static List<CssStyle> getNotAppliedStyles(
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

    protected static List<Style> removeUserAgentStyles(List<Style> allStyles) {
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
        private final String cssPropName;
        private final Object fxValue;

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

            public CssStyle(Style style) {
                this.style = style;
            }

            protected void setUnused() {
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
    protected static class CssSubPropertyState extends CssPropertyState {

        CssSubPropertyState(StyleableProperty<?> value, CssMetaData<?, ?> cssMeta, String cssValue) {
            super(value, cssMeta, cssValue);
        }

        @Override
        public Object getFxValue() {
            return CssValueConverter.getSubPropertyValue(cssMeta.getProperty(), value.getValue());
        }
    }

    private static class InitialPropertyState extends PropertyState {

        private final String name;
        private final Object fxValue;

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

}
