/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import javafx.css.PseudoClass;

/**
 * Implementation details of {@link javafx.css.PseudoClass}
 */
public final class PseudoClassImpl extends PseudoClass {

    /**
     * @see javafx.css.PseudoClass#getPseudoClassName(String)
     */
    public static PseudoClass getPseudoClassImpl(String pseudoClass) {

        if (pseudoClass == null || pseudoClass.trim().isEmpty()) {
            throw new IllegalArgumentException("pseudoClass cannot be null or empty String");
        }

        PseudoClassImpl instance = stateMap.get(pseudoClass);
        if (instance == null) {
            //
            // Convert pseudoClass to a bit mask.
            // The upper 4 bits is an index into the long[] mask representation
            // of the pseudoClass. The remaining bits are the bit mask for this
            // pseudoClass within the long[] at index.
            //
            final int size = stateMap.size();
            final long element = size / VALUE_BITS; // use top bits for element
            if (element > MAX_ELEMENTS) {
                throw new IndexOutOfBoundsException("size of PseudoClass stateMap exceeded");
            }
            final int exp = size % VALUE_BITS; // remaining bits for value
            long mask = Long.valueOf(
                (element << VALUE_BITS) | (1L << exp) // same as Math.pow(2,exp)
            );
            instance = new PseudoClassImpl(pseudoClass, mask);
            stateMap.put(pseudoClass, instance);
        }
        return instance;
    }


    /** @return the pseudo-class state */
    @Override
    public String getPseudoClassName() {
        return pseudoClass;
    }

    /** @return the pseudo-class state */
   @Override public String toString() {
        return pseudoClass;
    }

    /** Cannot create an instance of State except through PseudoClass static methods */
    private PseudoClassImpl(String pseudoClass, long bitMask) {
        this.pseudoClass = pseudoClass;
        this.bitMask = bitMask;
    }

    final String pseudoClass;

    //
    // The long value is a bit mask. The upper 4 bits of the mask are used
    // to hold the index of the mask within a long[] (see States) and the
    // remaining bits are used to hold the mask value. If, for example,
    // "foo" is the 96th string to be entred into stateMap, the upper 4 bits
    // of bitMask will be 0x01 (foo will be at mask[1]) and the remaining
    // bits will have the 36th bit set.
    //
    final long bitMask;
    
    // package private for unit test purposes
    static final Map<String,PseudoClassImpl> stateMap = 
            new HashMap<String,PseudoClassImpl>(64);

    // Number of bits used to represent the index into the long[] of the value.
    // 4 is arbitrary but allows for (2^4+1) * 60 = 1020 string masks.
     static final int MAX_ELEMENT_BITS = 4;

    // The number of elements that you can represent in MAX_ELEMENT_BITS
     static final int MAX_ELEMENTS = 1 << MAX_ELEMENT_BITS;

    // Number of bits in the value part.
     static final int VALUE_BITS = Long.SIZE-MAX_ELEMENT_BITS;

    // Mask for the bits in the value part.
     static final long VALUE_MASK = ~(0xfL << VALUE_BITS);
    
}
