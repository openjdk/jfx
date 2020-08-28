/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Used by CSSRule to determine whether or not the selector applies to a
 * given object.
 *
 * @since 9
 */
abstract public class Selector {

    /**
     * @deprecated This constructor was exposed erroneously and will be removed in the next version. Use {@link #createSelector(String)} instead.
     */
    @Deprecated(since="16", forRemoval=true)
    public Selector() {
    }

    private static class UniversalSelector {
        private static final Selector INSTANCE =
            new SimpleSelector("*", null, null, null);
    }

    static Selector getUniversalSelector() {
        return UniversalSelector.INSTANCE;
    }

    private Rule rule;
    void setRule(Rule rule) {
        this.rule = rule;
    }
    public Rule getRule() {
        return rule;
    }

    private int ordinal = -1;
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }
    public int getOrdinal() {
        return ordinal;
    }

    public abstract Match createMatch();

    // same as the matches method expect return true/false rather than a match
    public abstract boolean applies(Styleable styleable);

    // same as applies, but will return pseudoclass state that it finds along the way
    public abstract boolean applies(Styleable styleable, Set<PseudoClass>[] triggerStates, int bit);

    /**
     * Determines whether the current state of the node and its parents
     * matches the pseudo-classes defined (if any) for this selector.
     * @param styleable the styleable
     * @param state the state
     * @return {@code true} if the current state of the node and its parents
     * matches the pseudo-classes defined (if any) for this selector
     */
    public abstract boolean stateMatches(Styleable styleable, Set<PseudoClass> state);

    private static final int TYPE_SIMPLE = 1;
    private static final int TYPE_COMPOUND = 2;

    protected void writeBinary(DataOutputStream os, StyleConverter.StringStore stringStore)
        throws IOException {
        if (this instanceof SimpleSelector) {
            os.writeByte(TYPE_SIMPLE);
        } else {
            os.writeByte(TYPE_COMPOUND);
        }
    }

    static Selector readBinary(int bssVersion, DataInputStream is, String[] strings)
        throws IOException {
        final int type = is.readByte();
        if (type == TYPE_SIMPLE)
            return SimpleSelector.readBinary(bssVersion, is,strings);
        else
            return CompoundSelector.readBinary(bssVersion, is,strings);
    }

    public static Selector createSelector(final String cssSelector) {
        if (cssSelector == null || cssSelector.length() == 0) {
            return null; // actually return a default no-match selector
        }

        // A very primitive parser
        List<SimpleSelector> selectors = new ArrayList<SimpleSelector>();
        List<Combinator> combinators = new ArrayList<Combinator>();
        List<String> parts = new ArrayList<String>();
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
                List<String> pseudoClasses = new ArrayList<String>();
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
                List<String> styleClasses = new ArrayList<String>();

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
