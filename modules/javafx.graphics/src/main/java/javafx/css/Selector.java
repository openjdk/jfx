/*
 * Copyright (c) 2008, 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import com.sun.javafx.css.Combinator;
import com.sun.javafx.css.CompoundSelector;
import com.sun.javafx.css.SimpleSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Used by {@code CSSRule} to determine whether or not the {@code Selector} applies to a
 * given object.
 *
 * @since 9
 */
public abstract sealed class Selector permits SimpleSelector, CompoundSelector {

    /**
     * Constructor for subclasses to call.
     *
     * @since 24
     */
    protected Selector() {
    }

    private static class UniversalSelector {
        private static final Selector INSTANCE =
            new SimpleSelector("*", null, null, null);
    }

    static Selector getUniversalSelector() {
        return UniversalSelector.INSTANCE;
    }

    private Rule rule;
    /**
     * Sets the {@code Rule} of this Selector.
     * @param rule the {@code Rule} of this Selector
     */
    void setRule(Rule rule) {
        this.rule = rule;
    }

    /**
     * Gets the {@code Rule} of this Selector.
     * @return rule
     */
    public Rule getRule() {
        return rule;
    }

    private int ordinal = -1;
    /**
     * Sets the ordinal of this Selector.
     * @param ordinal the ordinal of this Selector
     */
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    /**
     * Gets the ordinal of this Selector.
     * @return the ordinal of this Selector
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Gets the immutable set of style class names of this Selector.
     *
     * @return an immutable set with style class names, never {@code null},
     *     or contains {@code null}s, but can be empty
     * @since 23
     */
    public abstract Set<String> getStyleClassNames();

    /**
     * Creates a {@code Match}.
     *
     * @return a match, never {@code null}
     */
    public final Match createMatch() {
        return Match.of(this);
    }

    /**
     * Gets whether this {@code Selector} applies to the given {@code Styleable}.
     * @param styleable the {@code Styleable} to match
     * @return {@code true} if this {@code Selector} applies to the given {@code Styleable}
     */
    public abstract boolean applies(Styleable styleable);

    /**
     * Gets whether this {@code Selector} applies to the given {@code Styleable}.
     * It is the same as the {@link applies(Styleable)} method except it also returns
     * {@code PseudoClass} state that it finds along the way.
     * @param styleable the {@code Styleable} to match
     * @param triggerStates a set of {@code PseudoClass} states
     * @param depth depth of the {@code Node} hierarchy to look for
     * @return {@code true} if this {@code Selector} and a set of {@code PseudoClass}
     * applies to the given {@code Styleable}
     */
    public abstract boolean applies(Styleable styleable, Set<PseudoClass>[] triggerStates, int depth);

    /**
     * Determines whether the current state of the {@code Node} and its parents
     * matches the pseudo-classes defined (if any) for this selector.
     * @param styleable the styleable
     * @param state the state
     * @return {@code true} if the current state of the node and its parents
     * matches the pseudo-classes defined (if any) for this selector
     */
    public abstract boolean stateMatches(Styleable styleable, Set<PseudoClass> state);

    /**
     * Creates a {@code Selector} object.
     * @param cssSelector CSS selector string
     * @return a {@code Selector}
     */
    public static Selector createSelector(final String cssSelector) {
        if (cssSelector == null || cssSelector.length() == 0) {
            return null; // actually return a default no-match selector
        }

        // A very primitive parser
        List<SimpleSelector> selectors = new ArrayList<>();
        List<Combinator> combinators = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        int start = 0;
        int end = -1;
        char combinator = '\0';
        for (int i=0; i<cssSelector.length(); i++) {
            char ch = cssSelector.charAt(i);
            if (ch == ' ') {
                if (combinator == '\0') {
                    combinator = ch;
                    end = i;
                }
            } else if (ch == '>') {
                if (combinator == '\0') end = i;
                combinator = ch;
            } else if (combinator != '\0'){
                parts.add(cssSelector.substring(start, end));
                start = i;
                combinators.add(combinator == ' ' ? Combinator.DESCENDANT : Combinator.CHILD);
                combinator = '\0';
            }
        }
        parts.add(cssSelector.substring(start));

        for (int i=0; i<parts.size(); i++) {
            final String part = parts.get(i);
            if (part != null && !part.equals("")) {
                // Now we have the parts, we can split off the pseudo classes
                String[] pseudoClassParts = part.split(":");
                List<String> pseudoClasses = new ArrayList<>();
                for (int j=1; j<pseudoClassParts.length; j++) {
                    if (pseudoClassParts[j] != null && !pseudoClassParts[j].equals("")) {
                        pseudoClasses.add(pseudoClassParts[j].trim());
                    }
                }

                // Now that we've read off the pseudo classes, we can go ahead and pull
                // apart the beginning.
                final String selector = pseudoClassParts[0].trim();
                // There might be style classes, so lets peel those off next
                String[] styleClassParts = selector.split("\\.");
                List<String> styleClasses = new ArrayList<>();

                // If the first one is an empty string, then it started with a pseudo class
                // If the first one starts with a #, it was an id
                // Otherwise, it was a name
                for (int j=1; j<styleClassParts.length; j++) {
                    if (styleClassParts[j] != null && !styleClassParts[j].equals("")) {
                        styleClasses.add(styleClassParts[j].trim());
                    }
                }
                String name = null, id = null;
                if (styleClassParts[0].equals("")) {
                    // Do nothing!
                } else if (styleClassParts[0].charAt(0) == '#') {
                    id = styleClassParts[0].substring(1).trim();
                } else {
                    name = styleClassParts[0].trim();
                }

                selectors.add(new SimpleSelector(name, styleClasses, pseudoClasses, id));
            }
        }

        if (selectors.size() == 1) {
            return selectors.get(0);
        } else {
            return new CompoundSelector(selectors, combinators);
        }
    }

}
