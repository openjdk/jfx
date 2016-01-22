/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.css.Declaration;
import javafx.css.Match;
import javafx.css.Rule;
import javafx.css.Selector;


/**
 * A map of property name to the cascading styles that match a node.
 */
public final class StyleMap {

    public static final StyleMap EMPTY_MAP =
        new StyleMap(-1, Collections.<Selector>emptyList());

    /** Only StyleManager creates StyleMap */
    public StyleMap(int id, List<Selector> selectors) {
        this.id = id;
        this.selectors = selectors;
    }

    public int getId() {
        return id;
    }

    public boolean isEmpty() {
        if (selectors != null) return selectors.isEmpty();
        else if (cascadingStyles != null) return cascadingStyles.isEmpty();
        else return true;
    }

    public Map<String, List<CascadingStyle>> getCascadingStyles() {

        if (cascadingStyles == null) {

            if (selectors == null || selectors.isEmpty()) {
                cascadingStyles = Collections.emptyMap();
                return cascadingStyles;
            }

            //
            // Creating the map is a three step process. First, create
            // a list of CascadingStyles. Second, sort the CascadingStyles.
            // And, finally, loop through the CascadingStyles to put them
            // into the Map by property name.
            //
            List<CascadingStyle> cascadingStyleList = new ArrayList<>();

            int ordinal = 0;
            for (int i=0, iMax=selectors.size(); i<iMax; i++) {

                final Selector selector = selectors.get(i);

                final Match match = selector.createMatch();

                final Rule rule = selector.getRule();

                for (int d = 0, dmax = rule.getDeclarations().size(); d < dmax; d++) {
                    final Declaration decl = rule.getDeclarations().get(d);

                    // ordinal increments at declaration level since
                    // there may be more than one declaration for the
                    // same attribute within a selector or within a stylesheet
                    final CascadingStyle s = new CascadingStyle(decl, match, ordinal++);

                    cascadingStyleList.add(s);

                }
            }

            if (cascadingStyleList.isEmpty()) {
                cascadingStyles = Collections.emptyMap();
                return cascadingStyles;
            }

            // apply the cascade. CascadingStyle's primary sort key is the
            // property name, so the same properties should be in sequence.
            Collections.sort(cascadingStyleList, cascadingStyleComparator);

            // there may be more entries in this HashMap than we need if there
            // is more than one CascadingStyle per property. But this is
            // still better than
            final int nCascadingStyles = cascadingStyleList.size();
            cascadingStyles = new HashMap<>(nCascadingStyles);

            CascadingStyle cascadingStyle = cascadingStyleList.get(0);
            String property = cascadingStyle.getProperty();


            for (int fromIndex=0; fromIndex<nCascadingStyles; /*increment is in code*/) {

                List<CascadingStyle> value = cascadingStyles.get(property);
                if (value == null)  {

                    int toIndex = fromIndex;
                    final String currentProperty = property;

                    while (++toIndex < nCascadingStyles) {
                        cascadingStyle = cascadingStyleList.get(toIndex);
                        property = cascadingStyle.getProperty();
                        if (property.equals(currentProperty) == false) break;
                    }

                    cascadingStyles.put(currentProperty, cascadingStyleList.subList(fromIndex, toIndex));

                    fromIndex = toIndex;


                } else {
                    // logic is broken!
                    assert(false);
                }
            }

            selectors.clear();
            selectors = null;

        }

        return cascadingStyles;
    }

    private static final Comparator<CascadingStyle> cascadingStyleComparator =
            (o1, o2) -> {

                //
                // primary sort is on property. If the property names are the same,
                // then go through the cascade logic. Otherwise, sort by property
                // name since the cascade doesn't matter for dissimilar properties.
                //
                // What we want to end up with is a list where all the same properties
                // are together in the list.
                //
                String thisProperty = o1.getProperty();
                String otherProperty = o2.getProperty();

                int c = thisProperty.compareTo(otherProperty);
                if (c != 0) return c;

                return o1.compareTo(o2);

            };

    private final int id; // unique per container
    private List<Selector> selectors;
    private Map<String, List<CascadingStyle>> cascadingStyles;
}
