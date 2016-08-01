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

package javafx.css;

import com.sun.javafx.css.Combinator;
import com.sun.javafx.css.PseudoClassState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * A compound selector which behaves according to the CSS standard. The selector is
 * composed of one or more <code>Selectors</code>, along with an array of
 * <code>CompoundSelectorRelationships</code> indicating the required relationship at each
 * stage.  There must be exactly one less <code>Combinator</code> than
 * there are selectors.
 * <p>
 * For example, the parameters <code>[selector1, selector2, selector3]</code>
 * and <code>[Combinator.CHILD, Combinator.DESCENDANT]</code> will match
 * a component when all of the following conditions hold:
 * <ol>
 * <li>The component itself is matched by selector3
 * <li>The component has an ancestor which is matched by selector2
 * <li>The ancestor matched in step 2 is a direct CHILD of a component
 * matched by selector1
 * </ol>
 * In other words, the compound selector specified above is (in CSS syntax)
 * <code>selector1 &gt; selector2 selector3</code>.  The greater-than (&gt;)
 * between selector1 and selector2 specifies a direct CHILD, whereas the
 * whitespace between selector2 and selector3 corresponds to
 * <code>Combinator.DESCENDANT</code>.
 *
 * @since 9
 */
final public class CompoundSelector extends Selector {

    private final List<SimpleSelector> selectors;
    /**
     * The selectors that make up this compound selector
     * @return Immutable List&lt;SimpleSelector&gt;
     */
    public List<SimpleSelector> getSelectors() {
        return selectors;
    }

    private final List<Combinator> relationships;
    // /**
    //  * The relationships between the selectors
    //  * @return Immutable List&lt;Combinator&gt;
    //  */
    // public List<Combinator> getRelationships() {
    //     return relationships;
    // }

    /**
     * Creates a <code>CompoundSelector</code> from a list of selectors and a
     * list of <code>Combinator</code> relationships.  There must be exactly one
     * less <code>Combinator</code> than there are selectors.
     */
    CompoundSelector(List<SimpleSelector> selectors, List<Combinator> relationships) {
        this.selectors =
            (selectors != null)
                ? Collections.unmodifiableList(selectors)
                : Collections.EMPTY_LIST;
        this.relationships =
            (relationships != null)
                ? Collections.unmodifiableList(relationships)
                : Collections.EMPTY_LIST;
    }

    private CompoundSelector() {
        this(null, null);
    }


    @Override public Match createMatch() {
        final PseudoClassState allPseudoClasses = new PseudoClassState();
        int idCount = 0;
        int styleClassCount = 0;

        for(int n=0, nMax=selectors.size(); n<nMax; n++) {
            Selector sel = selectors.get(n);
            Match match = sel.createMatch();
            allPseudoClasses.addAll(match.pseudoClasses);
            idCount += match.idCount;
            styleClassCount += match.styleClassCount;
        }

        return new Match(this, allPseudoClasses, idCount, styleClassCount);
    }

    @Override public boolean applies(final Styleable styleable) {
        return applies(styleable, selectors.size()-1, null, 0);
    }

    @Override public boolean applies(final Styleable styleable, Set<PseudoClass>[] triggerStates, int depth) {

        assert (triggerStates == null || depth < triggerStates.length);
        if (triggerStates != null && triggerStates.length <= depth) {
            return false;
        }

        //
        // We only care about pseudo-class if the selector applies. But in
        // the case of a compound selector, we don't know whether it applies
        // until all the selectors have been checked (in the worse case). So
        // the setting of pseudo-class has to be deferred until we know
        // that this compound selector applies. So we'll send a new
        // PseudoClassSet[] and if the compound selector applies,
        // just copy the state back.
        //
        final Set<PseudoClass>[] tempStates = triggerStates != null
                ? new PseudoClassState[triggerStates.length] : null;

        final boolean applies = applies(styleable, selectors.size()-1, tempStates, depth);

        if (applies && tempStates != null) {

            for(int n=0; n<triggerStates.length; n++) {

                final Set<PseudoClass> pseudoClassOut = triggerStates[n];
                final Set<PseudoClass> pseudoClassIn = tempStates[n];

                if (pseudoClassOut != null) {
                    pseudoClassOut.addAll(pseudoClassIn);
                } else {
                    triggerStates[n] = pseudoClassIn;
                }

            }
        }
        return applies;
    }

    private boolean applies(final Styleable styleable, final int index, Set<PseudoClass>[] triggerStates, int depth) {
        // If the index is < 0 then we know we don't apply
        if (index < 0) return false;

        // Simply check the selector associated with this index and see if it
        // applies to the Node
        if (! selectors.get(index).applies(styleable, triggerStates, depth)) return false;

        // If there are no more selectors to check (ie: index == 0) then we
        // know we know we apply
        if (index == 0) return true;

        // We have not yet checked all the selectors in this CompoundSelector,
        // so now we need to find the next parent and try again. If the
        // relationship between this selector and its ancestor selector is
        // "CHILD" then it is required that the parent scenegraph node match
        // the ancestor selector. Otherwise, we just walk up the scenegraph
        // until we find an ancestor node that matches the selector. If we
        // manage to walk all the way to the top without having satisfied all
        // of the selectors, then we know it doesn't apply.
        final Combinator relationship = relationships.get(index-1);
        if (relationship == Combinator.CHILD) {
            final Styleable parent = styleable.getStyleableParent();
            if (parent == null) return false;
            // If this call succeeds, then all preceding selectors will have
            // matched due to the recursive nature of the call
            return applies(parent, index - 1, triggerStates, ++depth);
        } else {
             Styleable parent = styleable.getStyleableParent();
            while (parent != null) {
                boolean answer = applies(parent, index - 1, triggerStates, ++depth);
                // If a call to stateMatches succeeded, then we know that
                // all preceding selectors will have also matched.
                if (answer) return true;
                // Otherwise we need to get the next parent and try again
                parent = parent.getStyleableParent();
            }
        }
        return false;
    }

    @Override public boolean stateMatches(final Styleable styleable, Set<PseudoClass> states) {
        return stateMatches(styleable, states, selectors.size()-1);
    }

    private boolean stateMatches(Styleable styleable, Set<PseudoClass> states, int index) {
        // If the index is < 0 then we know we don't match
        if (index < 0) return false;

        // Simply check the selector associated with this index and see if it
        // matches the Node and states provided.
        if (! selectors.get(index).stateMatches(styleable, states)) return false;

        // If there are no more selectors to match (ie: index == 0) then we
        // know we have successfully matched
        if (index == 0) return true;

        // We have not yet checked all the selectors in this CompoundSelector,
        // so now we need to find the next parent and try again. If the
        // relationship between this selector and its ancestor selector is
        // "CHILD" then it is required that the parent scenegraph node match
        // the ancestor selector. Otherwise, we just walk up the scenegraph
        // until we find an ancestor node that matches the selector. If we
        // manage to walk all the way to the top without having satisfied all
        // of the selectors, then we know it doesn't match.
        final Combinator relationship = relationships.get(index - 1);
        if (relationship == Combinator.CHILD) {
            final Styleable parent = styleable.getStyleableParent();
            if (parent == null) return false;
            if (selectors.get(index-1).applies(parent)) {
                // If this call succeeds, then all preceding selectors will have
                // matched due to the recursive nature of the call
                Set<PseudoClass> parentStates = parent.getPseudoClassStates();
                return stateMatches(parent, parentStates, index - 1);
            }
        } else {
            Styleable parent = styleable.getStyleableParent();
            while (parent != null) {
                if (selectors.get(index-1).applies(parent)) {
                    Set<PseudoClass> parentStates = parent.getPseudoClassStates();
                    return stateMatches(parent, parentStates, index - 1);
                }
                // Otherwise we need to get the next parent and try again
                parent = parent.getStyleableParent();
            }
        }

        return false;
    }

    private  int hash = -1;

    /* Hash code is used in Style's hash code and Style's hash
       code is used by StyleHelper */
    @Override public int hashCode() {
        if (hash == -1) {
            for (int i = 0, max=selectors.size(); i<max; i++)
                hash = 31 * (hash + selectors.get(i).hashCode());
            for (int i = 0, max=relationships.size(); i<max; i++)
                hash = 31 * (hash + relationships.get(i).hashCode());
        }
        return hash;
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompoundSelector other = (CompoundSelector) obj;
        if (other.selectors.size() != selectors.size()) return false;
        // Avoid ArrayList equals since it uses enhanced for loop
        for (int i = 0, max=selectors.size(); i<max; i++) {
            if (!other.selectors.get(i).equals(selectors.get(i))) return false;
        }
        // Avoid ArrayList equals since it uses enhanced for loop
        if (other.relationships.size() != relationships.size()) return false;
        for (int i = 0, max=relationships.size(); i<max; i++) {
            if (!other.relationships.get(i).equals(relationships.get(i))) return false;
        }
        return true;
    }

    @Override public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(selectors.get(0));
        for(int n=1; n<selectors.size(); n++) {
            sbuf.append(relationships.get(n-1));
            sbuf.append(selectors.get(n));
        }
        return sbuf.toString();
    }

    @Override protected final void writeBinary(final DataOutputStream os, final StyleConverter.StringStore stringStore)
            throws IOException
    {
        super.writeBinary(os, stringStore);
        os.writeShort(selectors.size());
        for (int n=0; n< selectors.size(); n++) selectors.get(n).writeBinary(os,stringStore);
        os.writeShort(relationships.size());
        for (int n=0; n< relationships.size(); n++) os.writeByte(relationships.get(n).ordinal());
    }

    static CompoundSelector readBinary(int bssVersion, final DataInputStream is, final String[] strings)
            throws IOException
    {

        final int nSelectors = is.readShort();
        final List<SimpleSelector> selectors = new ArrayList<SimpleSelector>();
        for (int n=0; n<nSelectors; n++) {
            selectors.add((SimpleSelector)Selector.readBinary(bssVersion, is,strings));
        }

        final int nRelationships = is.readShort();

        final List<Combinator> relationships = new ArrayList<Combinator>();
        for (int n=0; n<nRelationships; n++) {
            final int ordinal = is.readByte();
            if (ordinal == Combinator.CHILD.ordinal())
                relationships.add(Combinator.CHILD);
            else if (ordinal == Combinator.DESCENDANT.ordinal())
                relationships.add(Combinator.DESCENDANT);
            else {
                assert false : "error deserializing CompoundSelector: Combinator = " + ordinal;
                relationships.add(Combinator.DESCENDANT);
            }
        }
        return new CompoundSelector(selectors, relationships);
    }
}
