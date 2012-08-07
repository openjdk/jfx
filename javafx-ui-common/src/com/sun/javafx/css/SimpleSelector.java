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
 */

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * A simple selector which behaves according to the CSS standard.
 *
 */
final public class SimpleSelector extends Selector {
    static final private Object MAX_CLASS_DEPTH = 255;
    
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
     * @return The name of the java class to which this selector is applied, or *.
     */
    public String getName() {
        return name;
    }
    
    final private List<String> styleClasses;
    
    /**
     * @return Immutable List&lt;String&gt; of style-classes of the selector
     */
    public List<String> getStyleClasses() {
        return styleClasses;
    }

    final private List<String> pseudoclasses;
    /**
     * @return Immutable List&lt;String&gt; of pseudo-classes of the selector
     */
    public List<String> getPseudoclasses() {
        return pseudoclasses;
    }
    

    final private String id;
    /*
     * @return The value of the selector id, which may be an empty string.
     */
    public String getId() {
        return id;
    }
    
    // a mask of bits corresponding to the pseudoclasses
    final private long pclassMask;

    // true if name is not a wildcard
    final private boolean matchOnName;

    // true if id given
    final private boolean matchOnId;

    // true if style class given
    final private boolean matchOnStyleClass;

    // TODO: The parser passes styleClasses as a List. Should be array?
    public SimpleSelector(final String name, final List<String> styleClasses,
            final List<String> pseudoclasses, final String id)
    {
        this.name = name == null ? "*" : name;
        // if name is not null and not empty or wildcard, 
        // then match needs to check name
        this.matchOnName = (name != null && !("".equals(name)) && !("*".equals(name)));

        this.styleClasses = 
                (styleClasses != null) 
                ? Collections.unmodifiableList(styleClasses)  
                : Collections.EMPTY_LIST;
        this.matchOnStyleClass = (this.styleClasses.size() > 0);

        this.pseudoclasses = 
                (pseudoclasses != null) 
                ? Collections.unmodifiableList(pseudoclasses)  
                : Collections.EMPTY_LIST;
        pclassMask = StyleManager.getInstance().getPseudoclassMask(pseudoclasses);

        this.id = id == null ? "" : id;
        // if id is not null and not empty, then match needs to check id
        this.matchOnId = (id != null && !("".equals(id)));

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
        if (applies(node)) {
            final int idCount = (matchOnId) ? 1 : 0;
            return new Match(this, pseudoclasses, idCount, styleClasses.size());
        }
        return null;
    }

    @Override 
    Match matches(final Scene scene) {
        // Scene should match wildcard or specific java class name only.
        if (!matchOnStyleClass && !matchOnId && pseudoclasses.isEmpty()) {

            final String className = scene.getClass().getName();
            final boolean classMatch =
                "".equals(name) || nameMatchesAtEnd(className);

            if (classMatch) {
                // we know idCount and styleClassCount are zero from
                // the condition for entering the outer block
                return new Match(this, pseudoclasses, 0, 0 );
            }
        }
        return null;
    }

    @Override 
    public boolean applies(final Node node) {
        // if the selector has an id,
        // then bail if it doesn't match the node's id
        // (do this first since it is potentially the cheapest check)
        if (matchOnId) {
            boolean idMatch = id.equals(node.getId());
            if (!idMatch) return false;
        }

        // If name is not a wildcard,
        // then bail if it doesn't match the node's class name
        // if not wildcard, then match name with node's class name
        if (matchOnName) {
            final String className = node.getClass().getName();
            boolean classMatch = nameMatchesAtEnd(className);
            if (!classMatch) return false;
        }

        if (matchOnStyleClass) {
            boolean styleClassMatch = matchStyleClasses(node.getStyleClass());
            if (!styleClassMatch) return false;
        }
        return true;
    }

    @Override
    boolean mightApply(final String className, final String id, final List<String> styleClasses) {
        if (matchOnName && nameMatchesAtEnd(className)) return true;
        if (matchOnId   && this.id.equals(id)) return true;
        if (matchOnStyleClass) return matchStyleClasses(styleClasses);
        return false;
    }

    @Override
    boolean stateMatches(final Node node, long states) {
        return ((pclassMask & states) == pclassMask);
    }

    /*
     * Optimized className.equals(name) || className.endsWith(".".concat(name)).
     */
    private boolean nameMatchesAtEnd(final String className) {
        // if name is null or empty, why bother?
        if (!matchOnName) return false;

        final int nameLen = this.name.length();

        // take a guess that '.name' starts at this offset in className
        final int dotPos = className.length() - nameLen - 1;

        // If dotPos is -1, then className and name are
        // equal length and may match. Anything less than -1 and
        // className is shorter than name and no match is possible.
        if ((dotPos == -1) || (dotPos > -1 && className.charAt(dotPos) == '.')) {
            return className.regionMatches(dotPos+1, this.name, 0, nameLen);
        }
        return false;
    }

    // Are the Selector's style classes a subset of the Node's style classes?
    //
    // http://www.w3.org/TR/css3-selectors/#class-html
    // The following rule matches any P element whose class attribute has been
    // assigned a list of whitespace-separated values that includes both
    // pastoral and marine:
    //
    //     p.pastoral.marine { color: green }
    //
    // This rule matches when class="pastoral blue aqua marine" but does not
    // match for class="pastoral blue".
    private boolean matchStyleClasses(final List<String> nodeStyleClasses) {
        return isSubsetOf(styleClasses, nodeStyleClasses);
    }

    /**
     * A set of current strings. Used to match states and also match styleclasses.
     * We reuse this set from run to run to help reduce garbage a bit.
     */
    static final private Set<String> strSet = new HashSet<String>();

    /** 
      * return true if seq1 is a subset of seq2. That is, all the strings
      * in seq1 are contained in seq2
      */
    boolean isSubsetOf(final List<String> seq1, final List<String> seq2) {
        // if one or the other is null, then they are a subset if both are null
        if (seq1 == null || seq2 == null) return seq1 == null && seq2 == null;
        
        // they are a subset if both are empty
        if (seq1.isEmpty() && seq2.isEmpty()) return true;

        // [foo bar] cannot be a subset of [foo]
        if (seq1.size() > seq2.size()) return false;

        // is [foo] a subset of [foo bar bang]?
        // Just need to find the first string in seq2 that equals seq1[0]
        if (seq1.size() == 1) {
            final String otherString = seq1.get(0);
            if (otherString == null) return false;

            for (int n=0, max=seq2.size(); n<max; n++) {
                String item = seq2.get(n);
                if (item == null) continue;
                if (item.equals(otherString)) return true;
            }
            return false;
        }

        // is [foo bar] a subset of [foo bar bang]?
        // Check if each string in seq1 is in seq2
        strSet.clear();
        for (int n=0, max=seq2.size(); n<max; n++) strSet.add(seq2.get(n));
        for (int n=0, max=seq1.size(); n<max; n++) {
            if (! strSet.contains(seq1.get(n))) return false;
        }
        return true;

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleSelector other = (SimpleSelector) obj;
        if (this.pclassMask != other.pclassMask) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if (this.styleClasses != other.styleClasses && (this.styleClasses == null || !this.styleClasses.equals(other.styleClasses))) {
            return false;
        }
        return true;
    }

    private  int hash = -1;
    /* Hash code is used in Style's hash code and Style's hash 
       code is used by StyleHelper */
    @Override public int hashCode() {
        if (hash == -1) {
            hash = name.hashCode();
            hash = 31 * (hash + styleClasses.hashCode());
            hash = 31 * (hash + (id != null ? id.hashCode() : 1229));
            hash = 31 * (int)(pclassMask ^ (pclassMask >>> 32));
        }
        return hash;
    }

    /** Converts this object to a string. */
    @Override public String toString() {
        StringBuilder sbuf = new StringBuilder();
        if (name != null && name.isEmpty() == false) sbuf.append(name);
        else sbuf.append("*");
        for (int n=0; n<styleClasses.size(); n++) {
            sbuf.append('.');
            sbuf.append(styleClasses.get(n));
        }
        if (id != null && id.isEmpty() == false) {
            sbuf.append('#');
            sbuf.append(id);
        }
        for (int n=0; n<pseudoclasses.size(); n++) {
            sbuf.append(':');
            sbuf.append(pseudoclasses.get(n));
        }
        return sbuf.toString();
    }

    public void writeBinary(final DataOutputStream os, final StringStore stringStore)
        throws IOException
    {
        super.writeBinary(os, stringStore);
        os.writeShort(stringStore.addString(name));
        os.writeShort(styleClasses.size());
        for (String sc  : styleClasses) os.writeShort(stringStore.addString(sc));
        os.writeShort(stringStore.addString(id));
        os.writeShort(pseudoclasses.size());
        for (String p : pseudoclasses)  os.writeShort(stringStore.addString(p));
    }

    static SimpleSelector readBinary(final DataInputStream is, final String[] strings)
        throws IOException
    {
        final String name = strings[is.readShort()];
        final int nStyleClasses = is.readShort();
        final List<String> styleClasses = new ArrayList<String>();
        for (int n=0; n < nStyleClasses; n++) {
            styleClasses.add(strings[is.readShort()]);
        }
        final String id = strings[is.readShort()];
        final int nPseudoclasses = is.readShort();
        final List<String> pseudoclasses = new ArrayList<String>();
        for(int n=0; n < nPseudoclasses; n++) {
            pseudoclasses.add(strings[is.readShort()]);
        }
        return new SimpleSelector(name, styleClasses, pseudoclasses, id);
    }
}
