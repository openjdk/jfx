/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * A compound selector which behaves according to the CSS standard. The selector is 
 * composed of one or more <code>Selectors</code>, along with an array of 
 * <code>CompoundSelectorRelationships</code> indicating the required relationship at each 
 * stage.  There must be exactly one less <code>Combinator</code> than 
 * there are selectors.
 * <p>
 * For example, the paramters <code>[selector1, selector2, selector3]</code> 
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
    /**
     * The relationships between the selectors
     * @return Immutable List&lt;Combinator&gt;
     */
    public List<Combinator> getRelationships() {
        return relationships;
    }

    public CompoundSelector(List<SimpleSelector> selectors, List<Combinator> relationships)
    {
        this.selectors = 
            (selectors != null) 
                ? Collections.unmodifiableList(selectors) 
                : Collections.EMPTY_LIST;
        this.relationships =
            (relationships != null) 
                ? Collections.unmodifiableList(relationships) 
                : Collections.EMPTY_LIST;
    }
        
    private CompoundSelector()
    {
        this(null, null);
    }
    
    /**
     * Returns a {@link Match} if this selector matches the specified object, or 
     * <code>null</code> otherwise.
     *
     *@param node the object to check for a match
     *@return a {@link Match} if the selector matches, or <code>null</code> 
     *      otherwise
     */
    @Override
    Match matches(final Node node) {
        return matches(node, selectors.size()-1);
    }

    private Match matches(final Node node, final int index) {
            
        final Match descendantMatch = selectors.get(index).matches(node);
        if (descendantMatch == null || index == 0) {
            return descendantMatch;
        }

        Node parent = node.getParent();
        while (parent != null) {
            final Match ancestorMatch = matches(parent, index-1);
            if (ancestorMatch != null) {

                List<String> ancestorPseudoclasses = ancestorMatch.pseudoclasses;
                List<String> descendantPseudoclasses = descendantMatch.pseudoclasses;
                List<String> pseudoclasses = new ArrayList<String>();
                pseudoclasses.addAll(ancestorMatch.pseudoclasses);
                pseudoclasses.addAll(descendantMatch.pseudoclasses);

                return new Match(this, pseudoclasses,
                        ancestorMatch.idCount + descendantMatch.idCount,
                        ancestorMatch.styleClassCount + descendantMatch.styleClassCount);
            }
            // Combinator.CHILD will cause this loop to exit after the first iteration
            if ( relationships.get(index-1) == Combinator.CHILD ) break;
            parent = parent.getParent();
        }
        
        return null;
    }

    @Override
    Match matches(final Scene scene) {
        // TBD: do compound selectors make sense for Scene?
        return null;
    }

    @Override
    public boolean applies(final Node node) {
        return applies(node, selectors.size()-1);
    }

    private boolean applies(final Node node, final int index) {
        // If the index is < 0 then we know we don't apply
        if (index < 0) return false;

        // Simply check the selector associated with this index and see if it
        // applies to the Node
        if (! selectors.get(index).applies(node)) return false;

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
            final Node parent = node.getParent();
            if (parent == null) return false;
            // If this call succeeds, then all preceding selectors will have
            // matched due to the recursive nature of the call
            return applies(parent, index - 1);
        } else {
             Node parent = node.getParent();
            while (parent != null) {
                boolean answer = applies(parent, index - 1);
                // If a call to stateMatches succeeded, then we know that
                // all preceding selectors will have also matched.
                if (answer) return true;
                // Otherwise we need to get the next parent and try again
                parent = parent.getParent();
            }
        }
        return false;
    }

    @Override
    boolean mightApply(final String className, final String id, final long[] styleClasses) {
        return selectors.get(selectors.size()-1).mightApply(className, id, styleClasses);
    }

    @Override
    boolean stateMatches(final Node node, long states) {
        return stateMatches(node, states, selectors.size()-1);
    }

    private boolean stateMatches(Node node, long states, int index) {
        // If the index is < 0 then we know we don't match
        if (index < 0) return false;

        // Simply check the selector associated with this index and see if it
        // matches the Node and states provided.
        if (! selectors.get(index).stateMatches(node, states)) return false;

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
            final Node parent = node.getParent();
            if (parent == null) return false;
            if (selectors.get(index-1).applies(parent)) {
                final StyleHelper parentStyleHelper = parent.impl_getStyleHelper();
                if (parentStyleHelper == null) return false;
                long parentStates = parentStyleHelper.getPseudoClassState(parent);
                // If this call succeeds, then all preceding selectors will have
                // matched due to the recursive nature of the call
                return stateMatches(parent, parentStates, index - 1);
            }
        } else {
            Node parent = node.getParent();
            while (parent != null) {
                if (selectors.get(index-1).applies(parent)) { 
                    final StyleHelper parentStyleHelper = parent.impl_getStyleHelper();
                    if (parentStyleHelper != null) {
                        long parentStates = parentStyleHelper.getPseudoClassState(parent);
                        return stateMatches(parent, parentStates, index - 1);
                    } else {
                        // What does it mean for a parent to have a null StyleHelper? 
                        // In node, StyleHelper is held as a Reference, so if
                        // the Node's StyleHelper is GC'd, impl_getStyleHelper()
                        // is going to return null. This can happen if the 
                        // StyleManager's StyleHelper cache is cleared. 
                        return false;
                    }
                }
                // Otherwise we need to get the next parent and try again
                parent = parent.getParent();
            }
        }

        return false;
    }

    private  int hash = -1;

    /* Hash code is used in Style's hash code and Style's hash
       code is used by StyleHelper */
    @Override
    public int hashCode() {
        if (hash == -1) {
            for (int i = 0, max=selectors.size(); i<max; i++) 
                hash = 31 * (hash + selectors.get(i).hashCode());
            for (int i = 0, max=relationships.size(); i<max; i++) 
                hash = 31 * (hash + relationships.get(i).hashCode());
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
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

    public void writeBinary(final DataOutputStream os, final StringStore stringStore)
            throws IOException
    {
        super.writeBinary(os, stringStore);
        os.writeShort(selectors.size());
        for (int n=0; n< selectors.size(); n++) selectors.get(n).writeBinary(os,stringStore);
        os.writeShort(relationships.size());
        for (int n=0; n< relationships.size(); n++) os.writeByte(relationships.get(n).ordinal());
    }

    public static CompoundSelector readBinary(final DataInputStream is, final String[] strings)
            throws IOException
    {

        final int nSelectors = is.readShort();
        final List<SimpleSelector> selectors = new ArrayList<SimpleSelector>();
        for (int n=0; n<nSelectors; n++) {
            selectors.add((SimpleSelector)Selector.readBinary(is,strings));
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

