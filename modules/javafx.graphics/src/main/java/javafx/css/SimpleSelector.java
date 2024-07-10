/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.javafx.css.FixedCapacitySet;
import com.sun.javafx.css.ImmutablePseudoClassSetsCache;
import com.sun.javafx.css.PseudoClassState;

import static javafx.geometry.NodeOrientation.INHERIT;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.geometry.NodeOrientation.RIGHT_TO_LEFT;

/**
 * A simple selector which behaves according to the CSS standard.
 *
 * @since 9
 * @deprecated This class was exposed erroneously and will be removed in a future version
 */
@Deprecated(since = "23", forRemoval = true)
@SuppressWarnings("removal")
final public class SimpleSelector extends Selector {

    /**
     * If specified in the CSS file, the name of the java class to which
     * this selector is applied. For example, if the CSS file had:
     * <code><pre>
     *   Rectangle { }
     * </pre></code>
     * then name would be "Rectangle".
     */
    final private String name;

    /**
     * Gets the name of the java class to which this selector is applied, or *.
     * @return the name of the java class
     */
    public String getName() {
        return name;
    }

    /**
     * Gets an immutable list of style-classes of the {@code Selector}.
     * @return an immutable list of style-classes of the {@code Selector}
     */
    public List<String> getStyleClasses() {
        return List.copyOf(selectorStyleClassNames);
    }

    /**
     * Gets the immutable {@code Set} of {@code StyleClass}es of the {@code Selector}.
     * @return the {@code Set} of {@code StyleClass}es
     */
    public Set<StyleClass> getStyleClassSet() {
        if (cachedStyleClasses == null) {
            cachedStyleClasses = getStyleClassNames().stream().map(SimpleSelector::getStyleClass).collect(Collectors.toUnmodifiableSet());
        }

        return cachedStyleClasses;
    }

    /*
     * Copied from removed StyleClassSet to give StyleClasses a fixed index when
     * first encountered. No longer needed once StyleClass is removed.
     */

    private static final Map<String, Integer> styleClassMap = new HashMap<>(64);
    private static final List<StyleClass> styleClasses = new ArrayList<>();

    private static StyleClass getStyleClass(String styleClass) {

        if (styleClass == null || styleClass.trim().isEmpty()) {
            throw new IllegalArgumentException("styleClass cannot be null or empty String");
        }

        StyleClass instance = null;

        final Integer value = styleClassMap.get(styleClass);
        final int index = value != null ? value.intValue() : -1;

        final int size = styleClasses.size();
        assert index < size;

        if (index != -1 && index < size) {
            instance = styleClasses.get(index);
        }

        if (instance == null) {
            instance = new StyleClass(styleClass, size);
            styleClasses.add(instance);
            styleClassMap.put(styleClass, Integer.valueOf(size));
        }

        return instance;
    }

    /**
     * Style class names (immutable).
     */
    private final FixedCapacitySet<String> selectorStyleClassNames;

    /**
     * Cache to avoid having to recreate this set on each call.
     */
    private transient Set<StyleClass> cachedStyleClasses;

    private final String id;

    /**
     * Gets the value of the selector id.
     * @return the value of the selector id, which may be an empty string
     */
    public String getId() {
        return id;
    }

    // a mask of bits corresponding to the pseudoclasses (immutable)
    private final Set<PseudoClass> pseudoClassState;

    // for test purposes
    Set<PseudoClass> getPseudoClassStates() {
        return pseudoClassState;
    }

    // true if name is not a wildcard
    final private boolean matchOnName;

    // true if id given
    final private boolean matchOnId;

    // true if style class given
    final private boolean matchOnStyleClass;

    // dir(ltr) or dir(rtl), otherwise inherit
    final private NodeOrientation nodeOrientation;

    // Used in Match. If nodeOrientation is ltr or rtl,
    // then count it as a pseudoclass
    /**
     * Gets the {@code NodeOrientation} of this {@code Selector}.
     * @return the {@code NodeOrientation}
     */
    public NodeOrientation getNodeOrientation() {
        return nodeOrientation;
    }

    // TODO: The parser passes styleClasses as a List. Should be array?
    SimpleSelector(final String name, final List<String> styleClasses,
            final List<String> pseudoClasses, final String id)
    {
        this.name = name == null ? "*" : name;
        // if name is not null and not empty or wildcard,
        // then match needs to check name
        this.matchOnName = (name != null && !("".equals(name)) && !("*".equals(name)));

        this.selectorStyleClassNames = styleClasses == null ? FixedCapacitySet.of(0) : convertStyleClassNamesToSet(styleClasses);
        this.selectorStyleClassNames.freeze();  // turns it read only without having to wrap it

        this.matchOnStyleClass = (this.selectorStyleClassNames.size() > 0);

        PseudoClassState pcs = new PseudoClassState();
        NodeOrientation dir = NodeOrientation.INHERIT;

        if (pseudoClasses != null) {
            for (int n = 0; n < pseudoClasses.size(); n++) {

                final String pclass = pseudoClasses.get(n);
                if (pclass == null || pclass.isEmpty()) continue;

                // TODO: This is not how we should handle functional pseudo-classes in the long-run!
                if ("dir(".regionMatches(true, 0, pclass, 0, 4)) {
                    final boolean rtl = "dir(rtl)".equalsIgnoreCase(pclass);
                    dir = rtl ? RIGHT_TO_LEFT : LEFT_TO_RIGHT;
                    continue;
                }

                pcs.add(PseudoClassState.getPseudoClass(pclass));
            }
        }

        this.pseudoClassState = ImmutablePseudoClassSetsCache.of(pcs);
        this.nodeOrientation = dir;
        this.id = id == null ? "" : id;
        // if id is not null and not empty, then match needs to check id
        this.matchOnId = (id != null && !("".equals(id)));

    }

    @Override
    public Set<String> getStyleClassNames() {
        return selectorStyleClassNames;
    }

    private FixedCapacitySet<String> convertStyleClassNamesToSet(List<String> styleClasses) {
        FixedCapacitySet<String> scs = FixedCapacitySet.of(styleClasses.size());

        for (int n = 0, nMax = styleClasses.size(); n < nMax; n++) {
            String styleClassName = styleClasses.get(n);

            if (styleClassName == null || styleClassName.isEmpty()) {
                continue;
            }

            scs.add(styleClassName);
        }

        return scs;
    }

    @Override public Match createMatch() {
        final int idCount = (matchOnId) ? 1 : 0;
        int styleClassCount = selectorStyleClassNames.size();
        return new Match(this, pseudoClassState, idCount, styleClassCount);
    }

    @Override public boolean applies(Styleable styleable) {

        // handle functional pseudo-class :dir()
        // INHERIT applies to both :dir(rtl) and :dir(ltr)
        if (nodeOrientation != INHERIT && styleable instanceof Node) {
            final Node node = (Node)styleable;
            final NodeOrientation orientation = node.getNodeOrientation();

            if (orientation == INHERIT
                    ? node.getEffectiveNodeOrientation() != nodeOrientation
                    : orientation != nodeOrientation)
            {
                return false;
            }
        }

        // if the selector has an id,
        // then bail if it doesn't match the node's id
        // (do this first since it is potentially the cheapest check)
        if (matchOnId) {
            final String otherId = styleable.getId();
            final boolean idMatch = id.equals(otherId);
            if (!idMatch) return false;
        }

        // If name is not a wildcard,
        // then bail if it doesn't match the node's class name
        // if not wildcard, then match name with node's class name
        if (matchOnName) {
            final String otherName = styleable.getTypeSelector();
            final boolean classMatch = this.name.equals(otherName);
            if (!classMatch) return false;
        }

        if (matchOnStyleClass) {
            if (!matchesStyleClasses(styleable.getStyleClass())) {
                return false;
            }
        }

        return true;
    }

    @Override public boolean applies(Styleable styleable, Set<PseudoClass>[] pseudoClasses, int depth) {


        final boolean applies = applies(styleable);

        //
        // We only need the pseudo-classes if the selector applies to the node.
        //
        if (applies && pseudoClasses != null && depth < pseudoClasses.length) {

            if (pseudoClasses[depth] == null) {
                pseudoClasses[depth] = new PseudoClassState();
            }

            pseudoClasses[depth].addAll(pseudoClassState);

        }
        return applies;
    }

    @Override public boolean stateMatches(final Styleable styleable, Set<PseudoClass> states) {
        // [foo bar] matches [foo bar bang],
        // but [foo bar bang] doesn't match [foo bar]
        return states != null ? states.containsAll(pseudoClassState) : false;
    }

    // Are the Selector's style classes a subset of the Node's style classes?
    //
    // http://www.w3.org/TR/css3-selectors/#class-html
    // The following selector matches any P element whose class attribute has been
    // assigned a list of whitespace-separated values that includes both
    // pastoral and marine:
    //
    //     p.pastoral.marine { color: green }
    //
    // This selector matches when class="pastoral blue aqua marine" but does not
    // match for class="pastoral blue".
    private boolean matchesStyleClasses(List<String> styleClassNames) {

        /*
         * Exit early if the input list is too small to possibly match all the styles
         * of this selector:
         */

        if (styleClassNames.size() < selectorStyleClassNames.size()) {
            return false;
        }

        return selectorStyleClassNames.isSuperSetOf(styleClassNames);
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleSelector other = (SimpleSelector) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if (this.selectorStyleClassNames.equals(other.selectorStyleClassNames) == false) {
            return false;
        }
        if (this.pseudoClassState.equals(other.pseudoClassState) == false) {
            return false;
        }

        return true;
    }

    /* Hash code is used in Style's hash code and Style's hash
       code is used by StyleHelper */
    @Override public int hashCode() {
        int hash = 7;
        hash = 31 * (hash + name.hashCode());
        hash = 31 * (hash + selectorStyleClassNames.hashCode());
        hash = 31 * (hash + selectorStyleClassNames.hashCode());
        hash = (id != null) ? 31 * (hash + id.hashCode()) : 0;
        hash = 31 * (hash + pseudoClassState.hashCode());
        return hash;
    }

    /** Converts this object to a string. */
    @Override public String toString() {

        StringBuilder sbuf = new StringBuilder();
        if (name != null && name.isEmpty() == false) sbuf.append(name);
        else sbuf.append("*");
        Iterator<String> iter1 = selectorStyleClassNames.iterator();
        while(iter1.hasNext()) {
            final String styleClass = iter1.next();
            sbuf.append('.').append(styleClass);
        }
        if (id != null && id.isEmpty() == false) {
            sbuf.append('#');
            sbuf.append(id);
        }
        Iterator<PseudoClass> iter2 = pseudoClassState.iterator();
        while(iter2.hasNext()) {
            final PseudoClass pseudoClass = iter2.next();
            sbuf.append(':').append(pseudoClass.getPseudoClassName());
        }

        return sbuf.toString();
    }

    @Override protected final void writeBinary(final DataOutputStream os, final StyleConverter.StringStore stringStore)
        throws IOException
    {
        super.writeBinary(os, stringStore);
        os.writeShort(stringStore.addString(name));
        os.writeShort(selectorStyleClassNames.size());
        Iterator<String> iter1 = selectorStyleClassNames.iterator();
        while(iter1.hasNext()) {
            final String sc = iter1.next();
            os.writeShort(stringStore.addString(sc));
        }
        os.writeShort(stringStore.addString(id));
        int pclassSize = pseudoClassState.size()
                + (nodeOrientation == RIGHT_TO_LEFT || nodeOrientation == LEFT_TO_RIGHT ? 1 : 0);
        os.writeShort(pclassSize);
        Iterator<PseudoClass> iter2 = pseudoClassState.iterator();
        while(iter2.hasNext()) {
            final PseudoClass pc = iter2.next();
            os.writeShort(stringStore.addString(pc.getPseudoClassName()));
        }
        if (nodeOrientation == RIGHT_TO_LEFT) {
            os.writeShort(stringStore.addString("dir(rtl)"));
        } else if (nodeOrientation == LEFT_TO_RIGHT) {
            os.writeShort(stringStore.addString("dir(ltr)"));
        }
    }

    static SimpleSelector readBinary(int bssVersion, final DataInputStream is, final String[] strings)
        throws IOException
    {
        final String name = strings[is.readShort()];
        final int nStyleClasses = is.readShort();
        final List<String> styleClasses = new ArrayList<>();
        for (int n=0; n < nStyleClasses; n++) {
            styleClasses.add(strings[is.readShort()]);
        }
        final String id = strings[is.readShort()];
        final int nPseudoclasses = is.readShort();
        final List<String> pseudoclasses = new ArrayList<>();
        for(int n=0; n < nPseudoclasses; n++) {
            pseudoclasses.add(strings[is.readShort()]);
        }
        return new SimpleSelector(name, styleClasses, pseudoclasses, id);
    }
}
