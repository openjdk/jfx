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
    
    //
    // The Long value is a bit mask. The upper 4 bits of the mask are used to
    // hold the index of the mask within the long[] and the remaining bits are
    // used to hold the mask value. If, for example, "foo" is the 96th entry in
    // styleClassMask, the upper 4 bits will be 0x01 (foo will be at mask[1]) 
    // and the remaining bits will have the 36th bit set. 
    //
    // When creating the long[] bit set, you get the value from styleClassMask,
    // mask and shift the upper 4 bits to get the index of the style class in 
    // the long[], then or the value from styleClassMask with the mask[index]. 
    // In our example, "foo" will always be at mask[1]
    //
    private static final Map<String,Long> styleClassMask = new HashMap<String,Long>();
    
    
    // 4 is arbitrary but allows for 960 style classes. Well, actually 
    // less since the first bit of each mask[index>0] is wasted since
    // 61 % 60 is 1, even though the 61st entry is the zeroth value in mask[1]. 
    // So, 945 style classes.
    private static final int VALUE_BITS = Long.SIZE-4;
    
    // 0x0fffffffffffffff
    private static final long VALUE_MASK = ~(0xfL << VALUE_BITS);
        
    /** 
     * Convert styleClass string to a bit mask
     * @param styleClass
     * @return The upper 4 bits is an index into the long[] mask representation 
     * of styleClasses. The remaining bits are the bit mask for this styleClass
     * within the mask[index]
     */
    public static long getStyleClassMask(String styleClass) {
        Long mask = styleClassMask.get(styleClass);
        if (mask == null) {
            final int size = styleClassMask.size();
            final long element = size / VALUE_BITS; // use top bits for element
            final int exp = size % VALUE_BITS; // remaining bits for value
            mask = Long.valueOf(
                (element << VALUE_BITS) | (1L << exp) // same as Math.pow(2,exp)
            ); 
            styleClassMask.put(styleClass, mask);
        }
        return mask.longValue();
    }

    /**
     * Convert a list of style class strings to an array of bit masks.
     * @param styleClasses
     * @return The upper 4 bits of each element is an index into the long[] mask
     * representation of styleClasses. The remaining bits of each element are 
     * the bit mask for this styleClass within the mask[index]
     */
    public static long[] getStyleClassMasks(List<String> styleClasses) {
        
        long[] mask = new long[0]; // return zero length if styleClasses is null
        
        final int max = styleClasses != null ? styleClasses.size() : -1;
        for (int n=0; n<max; n++) {
            final String styleClass = styleClasses.get(n);
            final long m = getStyleClassMask(styleClass);
            final long element = (m & ~VALUE_MASK);
            final int  index = (int)(element >> VALUE_BITS);
            // need to grow?
            if (index >= mask.length) {
                final long[] temp = new long[index+1];
                System.arraycopy(mask, 0, temp, 0, mask.length);
                mask = temp;
            }
            mask[index] = mask[index] | m;
        }
        return mask;
    }

    public static List<String> getStyleClassStrings(long[] mask) {
        
        if (mask == null || mask.length == 0) return Collections.EMPTY_LIST;

        final Map<Long,String> stringMap = new HashMap<Long,String>();
        for (Map.Entry<String,Long> entry : styleClassMask.entrySet()) {
            stringMap.put(entry.getValue(), entry.getKey());
        }
        final List<String> strings = new ArrayList<String>();
        for(int index=0; index<mask.length; index++) {
            final long m = mask[index];
            final long element = (m & ~VALUE_MASK);
            for (int exp=0; exp < VALUE_BITS; exp++) {
                final long key = element | ((1L << exp) & m);
                if (key != 0) {
                    final String value = stringMap.get(key);
                    if (value != null) strings.add(value);
                }
            }
        }
        // even though the list returned could be modified without causing 
        // harm, returning an unmodifiableList is consistent with 
        // SimpleSelector.getStyleClasses()         
        return Collections.unmodifiableList(strings);
    }    
    
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
    
    /**
     * @return Immutable List&lt;String&gt; of style-classes of the selector
     */
    public List<String> getStyleClasses() {
        return getStyleClassStrings(styleClassMasks);
    }

    long[] getStyleClassMasks() {
        return styleClassMasks;
    }
    
    /** styleClasses converted to a set of bit masks */
    final private long[] styleClassMasks;
    
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

        this.styleClassMasks = 
                (styleClasses != null && styleClasses.isEmpty() == false)
                ? getStyleClassMasks(styleClasses)
                : new long[0];
        this.matchOnStyleClass = (this.styleClassMasks.length > 0);

        this.pseudoclasses = 
                (pseudoclasses != null) 
                ? Collections.unmodifiableList(pseudoclasses)  
                : Collections.EMPTY_LIST;
        pclassMask = StyleManager.getPseudoclassMask(pseudoclasses);

        this.id = id == null ? "" : id;
        // if id is not null and not empty, then match needs to check id
        this.matchOnId = (id != null && !("".equals(id)));

    }
    
    /** copy constructor used by StyleManager */
    SimpleSelector(final SimpleSelector other) {
        
        this.name = other.name;
        this.matchOnName = other.matchOnName;
        
        if (other.matchOnStyleClass) {
            this.styleClassMasks = new long[other.styleClassMasks.length];
        } else {
            // other is long[0]
            this.styleClassMasks = other.styleClassMasks;
        }
        this.matchOnStyleClass = other.matchOnStyleClass;
        
        if (other.pseudoclasses != null && other.pseudoclasses.isEmpty() == false) {
            final List<String> temp = new ArrayList<String>(other.pseudoclasses.size());
            for(int p=0, pMax=other.pseudoclasses.size(); p<pMax; p++) {
                temp.add(other.pseudoclasses.get(p));
            }
            this.pseudoclasses = Collections.unmodifiableList(temp);
        } else {
            this.pseudoclasses = Collections.EMPTY_LIST;
        }
        this.pclassMask = other.pclassMask;
        
        this.id = other.id;
        this.matchOnId = other.matchOnId;
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
            int styleClassCount = 0;
            for (int n=0; n<styleClassMasks.length; n++) {
                styleClassCount += Long.bitCount(styleClassMasks[n] & VALUE_MASK);
            }
            return new Match(this, pseudoclasses, idCount, styleClassCount);
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
    public boolean applies(Node node) {
        
        final StyleHelper styleHelper = node.impl_getStyleHelper();
        final SimpleSelector selector = styleHelper.getSelector();
        
        // if the selector has an id,
        // then bail if it doesn't match the node's id
        // (do this first since it is potentially the cheapest check)
        if (matchOnId) {
            boolean idMatch = id.equals(selector.id);
            if (!idMatch) return false;
        }

        // If name is not a wildcard,
        // then bail if it doesn't match the node's class name
        // if not wildcard, then match name with node's class name
        if (matchOnName) {
            boolean classMatch = nameMatchesAtEnd(selector.name);
            if (!classMatch) return false;
        }

        if (matchOnStyleClass) {
            boolean styleClassMatch = matchStyleClasses(selector.styleClassMasks);                
            if (!styleClassMatch) return false;
        }
        return true;
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
    private boolean matchStyleClasses(long[] nodeStyleClasses) {
        return isSubsetOf(styleClassMasks, nodeStyleClasses);
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
    boolean isSubsetOf(long[] seq1, long[] seq2) {
        
        // if one or the other is null, then they are a subset if both are null
        if (seq1 == null || seq2 == null) return seq1 == null && seq2 == null;
        
        // they are a subset if both are empty
        if (seq1.length == 0 && seq2.length == 0) return true;

        // [foo bar] cannot be a subset of [foo]
        if (seq1.length > seq2.length) return false;

        // is [foo bar] a subset of [foo bar bang]?
        for (int n=0, max=seq1.length; n<max; n++) {
            if ((seq1[n] & seq2[n]) != seq1[n]) return false;
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
        if (this.styleClassMasks != other.styleClassMasks && 
                (this.styleClassMasks == null || 
                    !Arrays.equals(this.styleClassMasks, other.styleClassMasks))) {
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
            hash = 31 * (hash + (styleClassMasks != null ? Arrays.hashCode(styleClassMasks) : 37));
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
        if (styleClassMasks != null && styleClassMasks.length > 0) {
            List<String> strings = getStyleClassStrings(styleClassMasks);
            for(String styleClass : strings) {
                sbuf.append('.');
                sbuf.append(styleClass);
            }
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
        final List<String> styleClasses = getStyleClasses();
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

    @Override public String writeJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("new SimpleSelector(\"");
        sb.append(getName());
        sb.append("\", ");
        
        // styleclasses
        if (getStyleClasses().isEmpty()) {
            sb.append("null");
        } else {
            sb.append("Arrays.<String>asList(");
            for (int i = 0, max = getStyleClasses().size(); i < max; i++) {
                sb.append("\"");
                sb.append(getStyleClasses().get(i));
                sb.append("\"");

                if (i < (max - 1)) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        
        sb.append(", ");
        
        // pseudoclasses
        if (getPseudoclasses().isEmpty()) {
            sb.append("null");
        } else {
            sb.append("Arrays.<String>asList(");
            for (int i = 0, max = getPseudoclasses().size(); i < max; i++) {
                sb.append("\"");
                sb.append(getPseudoclasses().get(i));
                sb.append("\"");

                if (i < (max - 1)) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        
        sb.append(", \"");
        sb.append(getId());
        sb.append("\")");
        
        return sb.toString();
    }
}
