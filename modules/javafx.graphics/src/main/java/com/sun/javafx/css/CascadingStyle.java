/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.Declaration;
import javafx.css.Match;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.Rule;
import javafx.css.Selector;
import javafx.css.Style;
import javafx.css.StyleOrigin;

import java.util.Set;

/** A marriage of pseudo-classes (potentially empty) to property and value
 *
 * @since 9
 */
public class CascadingStyle implements Comparable<CascadingStyle> {

    /** */
    private final Style style;
    public Style getStyle() {
        return style;
    }

    /** State variables, like &quot;hover&quot; or &quot;pressed&quot; */
    private final Set<PseudoClass> pseudoClasses;

    /* specificity of the selector that matched */
    private final int specificity;

    /* order in which this style appeared in the stylesheet */
    private final int ordinal;

    /*
     * True if the property is -fx-skin. We want the skin property to
     * sort less than all other properties.
     */
    private final boolean skinProp;

    public CascadingStyle(final Style style, Set<PseudoClass> pseudoClasses,
            final int specificity, final int ordinal) {
        this.style = style;
        this.pseudoClasses = pseudoClasses;
        this.specificity = specificity;
        this.ordinal = ordinal;
        this.skinProp = "-fx-skin".equals(style.getDeclaration().getProperty());
    }

    public CascadingStyle(final Declaration decl, final Match match, final int ordinal) {
        this(new Style(match.getSelector(), decl),
             match.getPseudoClasses(),
             match.getSpecificity(),
             ordinal);
    }

    // Wrapper to make StyleHelper's life a little easier
    public String getProperty() {
        return style.getDeclaration().getProperty();
    }

    // Wrapper to make StyleHelper's life a little easier
    public Selector getSelector() {
        return style.getSelector();
    }

    // Wrapper to make StyleHelper's life a little easier
    public Rule getRule() {
        return style.getDeclaration().getRule();
    }

    // Wrapper to make StyleHelper's life a little easier
    public StyleOrigin getOrigin() {
        return getRule().getOrigin();
    }

    // Wrapper to make StyleHelper's life a little easier
    public ParsedValue getParsedValue() {
        return style.getDeclaration().getParsedValue();
    }

    @Override public String toString() { return getProperty(); }

    /**
     * When testing equality against another Style, we only care about
     * the property and pseudo-classes. In other words, we only care about
     * where the style is applied, not what is applied.
     */
    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CascadingStyle other = (CascadingStyle)obj;

        final String property = getProperty();
        final String otherProperty = other.getProperty();
        if (property == null ? otherProperty != null : !property.equals(otherProperty)) {
            return false;
        }

        // does [foo bar bang] contain all of [foo bar]?
        if (pseudoClasses == null ? other.pseudoClasses != null : !pseudoClasses.containsAll(other.pseudoClasses)) {
            return false;
        }

        return true;

    }

    /*
     * Hash on property and pseudoclasses since
     * obj1.hashCode() should equal obj2.hashCode() if obj1.equals(obj2)
     */
    @Override public int hashCode() {
        int hash = 7;
        final String property = getProperty();
        hash = 47 * hash + (property != null ? property.hashCode() : 0);
        hash = 47 * hash + (pseudoClasses != null ? pseudoClasses.hashCode() : 0);
        return hash;
    }

    /**
     * Implementation of Comparable such that more specific styles get
     * sorted before less specific ones.
     */
    @Override public int compareTo(CascadingStyle other) {

        //
        // Important styles take the cake
        // Importance being equal, then specificity is considered
        // Specificity being equal, then the order of declaration decides.
        //

        final Declaration decl = style.getDeclaration();
        final boolean important = decl != null ? decl.isImportant() : false;
        final Rule rule = decl != null ? decl.getRule() : null;
        final StyleOrigin source = rule != null ? rule.getOrigin() : null;

        final Declaration otherDecl = other.style.getDeclaration();
        final boolean otherImportant = otherDecl != null ? otherDecl.isImportant() : false;
        final Rule otherRule = otherDecl != null ? otherDecl.getRule() : null;
        final StyleOrigin otherSource = otherRule != null ? otherRule.getOrigin() : null;

        int c = 0;

        if (this.skinProp && !other.skinProp) {
            c = 1;
        } else if (important != otherImportant) {
            c = important ? -1 : 1;
        } else if (source != otherSource) {
            if (source == null) c = -1;
            else if (otherSource == null) c = 1;
            else c = otherSource.compareTo(source);
        } else {
            c = other.specificity - this.specificity;
        };

        if (c == 0) c = other.ordinal - this.ordinal;
        return c;
    }

}

