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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities to handle CSS pseudo-classes. There can be at most 1020
 * unique pseudo-classes. Introducing a pseudo-class into
 * a JavaFX class requires implementing {@link javafx.scene.Node#getPseudoClassStates()}
 * and calling the {@link javafx.scene.Node#pseudoClassStateChanged(PseudoClass.State)}
 * method when the corresponding property changes value. Typically, the
 * {@code pseudoClassStateChanged} method is called from the
 * {@code protected void invalidated()} method of a {@code javafx.beans.property}
 * class.
 * <pre>
 * <b>Example:</b>
 *
 * <code>
 *  public boolean isMagic() {
 *       return magic.get();
 *   }
 *
 *   public BooleanProperty magicProperty() {
 *       return magic;
 *   }
 *
 *   public BooleanProperty magic =
 *       new BooleanPropertyBase(false) {
 *
 *       {@literal @}Override protected void invalidated() {
 *           pseudoClassStateChanged(MAGIC_PSEUDO_CLASS);
 *       }
 *
 *       {@literal @}Override public Object getBean() {
 *           return MyControl.this;
 *       }
 *
 *       {@literal @}Override public String getName() {
 *           return "magic";
 *       }
 *   }
 *
 *   private static final PseudoClass.State
 *       MAGIC_PSEUDO_CLASS = PseudoClass.getState("xyzzy");
 *
 *   {@literal @}Override public PseudoClass.States getPseudoClassStates() {
 *         PseudoClass.States states = super.getPseudoClassStates();
 *         if (isMagic()) states.addState(MAGIC_PSEUDO_CLASS);
 *         return states;
 *    }
 * </code></pre>
 */
public final class PseudoClass {

    /**
     * There is only one PseudoClass.State instance for a given pseudoClass.
     * There can be at most 1020 unique pseudo-classes.
     * @return The PseudoClass.State for the given pseudoClass. Will not return null.
     * @throws IllegalArgumentException if pseudoClass parameter is null or an empty String
     * @throws IndexOutOfBoundsException if adding the pseudoClass would exceed the
     *         maximum number of allowable pseudo-classes.
     */
    public static PseudoClass.State getState(String pseudoClass) {

        if (pseudoClass == null || pseudoClass.trim().isEmpty()) {
            throw new IllegalArgumentException("pseudoClass cannot be null or empty String");
        }

        State state = stateMap.get(pseudoClass);
        if (state == null) {
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
            state = new State(pseudoClass, mask);
            stateMap.put(pseudoClass, state);
        }
        return state;
    }

    /**
     * @return A new PseudoClass.States instance with no pseudo-classes
     */
    public static PseudoClass.States createStatesInstance() {
        return new States();
    }

    /**
     * Use the list of pseudo-classes to initialize a newly created
     * {@link PseudoClass.States} instance.
     * @param pseudoClasses The pseudo-classes used to initialize this instance. 
     * The list may be null or empty.
     * @return A new {@link PseudoClass.States} instance
     */
    public static PseudoClass.States createStatesInstance(List<String> pseudoClasses) {
        return new States(pseudoClasses);
    }

    /**
     * State represents one unique pseudo-class state.
     */
    public static final class State {

        /** @return the pseudo-class state */
        public String getPseudoClass() {
            return pseudoClass;
        }

        /** @return the pseudo-class state */
       @Override public String toString() {
            return pseudoClass;
        }

        /** Cannot create an instance of State except through PseudoClass static methods */
        private State(String pseudoClass, long bitMask) {
            this.pseudoClass = pseudoClass;
            this.bitMask = bitMask;
        }

        private final String pseudoClass;

        //
        // The long value is a bit mask. The upper 4 bits of the mask are used
        // to hold the index of the mask within a long[] (see States) and the
        // remaining bits are used to hold the mask value. If, for example,
        // "foo" is the 96th string to be entred into stateMap, the upper 4 bits
        // of bitMask will be 0x01 (foo will be at mask[1]) and the remaining
        // bits will have the 36th bit set.
        //
        private final long bitMask;

    }

    /**
     * States represents a set of State. A {@code Node} may be in more than
     * one pseudo-class state. {@code States} is used to aggregate the active
     * pseudo-class state of a {@code Node}.
     */
    public static final class States {

        /**
         * Add the state to the current set of states. The result of the
         * operation will be the union of the existing set of states and
         * the added state.
         * @param state The {@link PseudoClass.State} to add
         */
        public void addState(PseudoClass.State state) {

            if (state == null) {
                return;
            }

            //
            // When creating the long[] bit set, you get the State bitMask
            // and shift the upper 4 bits to get the index of the State in
            // the long[], then or the value from the bitMask with the
            // mask[index].
            //
            final long m = state.bitMask;
            final long element = (m & ~VALUE_MASK);
            final int  index = (int)(element >>> VALUE_BITS);

            // need to grow?
            if (index >= bitMask.length) {
                final long[] temp = new long[index+1];
                System.arraycopy(bitMask, 0, temp, 0, bitMask.length);
                bitMask = temp;
            }

            final Long temp = bitMask[index];
            bitMask[index] = temp | m;
            
            // if bitMask[index] == temp, then the bit was already set
            // and the add doesn't get counted.
            if (bitMask[index] != temp) {
                count += 1;
            }
        }

        /**
         * Remove the state to the current states. The result of the operation
         * will be the relative complement of the removed state in the existing
         * set of states. This implies that removing a state that is not in the
         * existing set of states had no effect.
         * @param state The {@link PseudoClass.State} state to remove
         */
        public void removeState(PseudoClass.State state) {

            if (state == null) {
                return;
            }

            final long m = state.bitMask;
            final long mask = m & VALUE_MASK;
            final long element = (m & ~VALUE_MASK);
            final int  index = (int)(element >>> VALUE_BITS);

            final Long temp = bitMask[index];
            bitMask[index] = temp & ~mask;
            
            // if bitMask[index] == temp, then the bit was not there
            // and the remove doesn't get counted.
            if (bitMask[index] != temp) {
                count -= 1;
            }
        }

        /**
         * Test if the given state is in the set of states represented by
         * this States object.
         * @param state The {@link PseudoClass.State} state to test
         * @return true if the given state is present.
         */
        public boolean containsState(PseudoClass.State state) {

            if (state == null) {
                return false;
            }

            final long m = state.bitMask;
            final long element = (m & ~VALUE_MASK);
            final int  index = (int)(element >>> VALUE_BITS);

            return (index < bitMask.length) && (bitMask[index] & m) == m;
        }
        
        /**
         * Test that all the bits of this key are present in the other. 
         * Note that {@code x.isSubsetOf(y) == false } does not imply
         * that {@code y.isSuperSetOf(x) == true } since <i>x</i> and <i>y</i>
         * might be disjoint.
         * @return true if set of pseudo-classes represented by this 
         * {@code States} object is a subset of the other. 
         */
        public boolean isSubsetOf(States other) {
        
            if (other == null) {
                return false;
            }
            
            // they are a subset if both are empty
            if (bitMask.length == 0 && other.bitMask.length == 0) {
                return true;
            }

            // [foo bar] cannot be a subset of [foo]
            if (bitMask.length > other.bitMask.length) {
                return false;
            }

            // is [foo bar] a subset of [foo bar bang]?
            for (int n=0, max=bitMask.length; n<max; n++) {
                if ((bitMask[n] & other.bitMask[n]) != bitMask[n]) {
                    return false;
                }
            }
            return true;

        }

        /**
         * Test that all the pseudo-classes the other {@code States} object
         * are present in this. 
         * Note that {@code x.isSubsetOf(y) == false } does not imply
         * that {@code y.isSuperSetOf(x) == true } since <i>x</i> and <i>y</i>
         * might be disjoint.
         * @return true if set of pseudo-classes represented by this 
         * {@code States} object is a superset of the other. 
         */
        public boolean isSupersetOf(States other) {
            
            if (other == null) {
                return true;
            }
            
            // [foo] cannot be a superset of [foo  bar]
            // nor can [foo] be a superset of [foo]
            if (bitMask.length < other.bitMask.length) {
                return false;
            }

            // is [foo bar bang] a superset of [foo bar]?
            for (int n=0, max=other.bitMask.length; n<max; n++) {
                if ((bitMask[n] & other.bitMask[n]) != other.bitMask[n]) {
                    return false;
                }
            }
            return true;

        }
        
        /**
         * @return The number of pseudo-class states represented 
         *         by this {@code States} object
         */
        public int getCount() {
            return count;
        }
        /**
         * Return true if this States object contains no {@link PseudoClass.State}.
         */
        public boolean isEmpty() {
            if (bitMask.length > 0) {
                for(int n = 0; n < bitMask.length; n++) {
                    final long mask = bitMask[n] & VALUE_MASK;
                    if (mask != 0) {
                        return false;
                    }
                }
                
            } 
            // bitMask.length is zero or all bitMask[n] values are zero
            return true;
        }
        /**
         * Take the intersection of statesOne and statesTwo.
         * @return A new States object the has the common State between
         *         the input parameters.
         */
        public static States intersectionOf(States statesOne, States statesTwo) {

            if (statesOne == null || statesTwo == null) {
                // empty set
                return new States(new long[0]);
            }
            
            final long[] maskOne = statesOne.bitMask;
            final long[] maskTwo = statesTwo.bitMask;

            final int max = Math.min(maskOne.length, maskTwo.length);
            final long[] intersection = new long[max];

            for(int n = 0; n < max; n++) {
                intersection[n] = maskOne[n] & maskTwo[n];
            }

            return new States(intersection);
        }

        /**
         * Take the union of statesOne and statesTwo.
         * @return A new States object the has the all the State of both of
         *         the input parameters.
         */
        public static States unionOf(States statesOne, States statesTwo) {

            if (statesOne == null && statesTwo == null) {
                // empty set
                return new States(new long[0]);
            } else if (statesOne == null) {
                final long[] bitMask = statesTwo.bitMask;
                return new States(Arrays.copyOf(bitMask, bitMask.length));
            } else if (statesTwo == null) {
                final long[] bitMask = statesOne.bitMask;
                return new States(Arrays.copyOf(bitMask, bitMask.length));
            }
            
            final long[] maskOne = statesOne.bitMask;
            final long[] maskTwo = statesTwo.bitMask;

            final int max = Math.max(maskOne.length, maskTwo.length);
            final long[] union = new long[max];

            for(int n = 0; n < max; n++) {
                if (n < maskOne.length && n < maskTwo.length) {
                    union[n] = maskOne[n] | maskTwo[n];
                } else if (n < maskOne.length) {
                    union[n] = maskOne[n];
                } else {
                    union[n] = maskTwo[n];
                }
            }

            return new States(union);
        }

        /**
         * Reset the set of states to the empty set.
         */
        public void clear() {
            bitMask = new long[1];
        }

        /** @return The list of PseudoClass.State that are represented by this States object */
        public List<PseudoClass.State> getStates() {

            final List<State> states = new ArrayList<State>();
            for (Map.Entry<String,State> entry : stateMap.entrySet()) {
                final State state = entry.getValue();
                if (containsState(state)) {
                    states.add(state);
                }
            }
            return states;

        }

        /** @return The list of pseudo-classes that are represented by this States object */
        public List<String> getPseudoClasses() {

            final List<String> pseudoClasses = new ArrayList<String>(count);
            for (Map.Entry<String,State> entry : stateMap.entrySet()) {
                final State state = entry.getValue();
                if (containsState(state)) {
                    pseudoClasses.add(state.getPseudoClass());
                }
            }
            return pseudoClasses;

        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Arrays.hashCode(this.bitMask);
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
            final States other = (States) obj;
            if (!Arrays.equals(this.bitMask, other.bitMask)) {
                return false;
            }
            return true;
        }


        @Override public String toString() {
            final List<String> pseudoClasses = getPseudoClasses();
            if (pseudoClasses != null) {
                return pseudoClasses.toString();
            } else {
                return "[]";
            }
        }

        /** Cannot create an instance of States except through PseudoClass static methods */
        private States() {
            this(new long[0]);
        }
        
        private States(List<String> pseudoClasses) {
            this();
            if (pseudoClasses != null && pseudoClasses.isEmpty() == false) {
                final int nMax = pseudoClasses.size();
                for (int n=0; n<nMax; n++) {
                    State state = PseudoClass.getState(pseudoClasses.get(n));
                    addState(state);
                }
            }
        }

        private States(long[] bitMask) {
            this.bitMask = bitMask;
        }

        private long[] bitMask;
        private int count;
    }


    // package private for unit test purposes
    static final Map<String,State> stateMap = new HashMap<String,State>(64);

    // Number of bits used to represent the index into the long[] of the value.
    // 4 is arbitrary but allows for (2^4+1) * 60 = 1020 string masks.
    private static final int MAX_ELEMENT_BITS = 4;

    // The number of elements that you can represent in MAX_ELEMENT_BITS
    private static final int MAX_ELEMENTS = 1 << MAX_ELEMENT_BITS;

    // Number of bits in the value part.
    private static final int VALUE_BITS = Long.SIZE-MAX_ELEMENT_BITS;

    // Mask for the bits in the value part.
    private static final long VALUE_MASK = ~(0xfL << VALUE_BITS);

}