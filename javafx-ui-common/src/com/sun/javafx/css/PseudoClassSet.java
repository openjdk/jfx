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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javafx.css.PseudoClass;

/**
 * States represents a set of State. A {@code Node} may be in more than
 * one pseudo-class state. {@code States} is used to aggregate the active
 * pseudo-class state of a {@code Node}.
 */
class PseudoClassSet implements Set<PseudoClass> {

    /** {@inheritDoc} */
    @Override
    public int size() {

        int size = 0;
        if (pseudoClasses.length > 0) {
            for (int n = 0; n < pseudoClasses.length; n++) {
                final long mask = pseudoClasses[n];
                if (mask != 0) {
                    size += Long.bitCount(mask);
                }
            }
        }
        // index.length is zero or all index[n] values are zero
        return size;

    }

    @Override
    public boolean isEmpty() {
        
        if (pseudoClasses.length > 0) {
            for (int n = 0; n < pseudoClasses.length; n++) {
                final long mask = pseudoClasses[n];
                if (mask != 0) {
                    return false;
                }
            }
        }
        // index.length is zero or all index[n] values are zero
        return true;

    }

    /**
     * {@inheritDoc} This returned iterator is not fail-fast.
     */
    @Override
    public Iterator<PseudoClass> iterator() {
        
        return new Iterator<PseudoClass>() {
            int next = -1;
            int element = 0;
            int index = -1;
            
            @Override
            public boolean hasNext() {
                if (pseudoClasses == null || pseudoClasses.length == 0) {
                    return false;
                }

                boolean found = false;
                
                do {
                    long bit = 0;
                    if (++next >= Long.SIZE) {
                        if (++element < pseudoClasses.length) {
                            next = 0;
                            bit = 1;
                        } else {
                            return false;
                        }
                    }                        
                    
                    bit = 1l << next;
                    found = (bit & pseudoClasses[element]) == bit;
                    
                } while( !found );
                
                if (found) {
                    index = Long.SIZE * element + next;
                }
                return found;
            }

            @Override
            public PseudoClass next() {
                try {
                    return PseudoClassImpl.pseudoClasses.get(index);
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException("["+element+"]["+next+"]");
                }
            }

            @Override
            public void remove() {
                try {
                    PseudoClassImpl impl = PseudoClassImpl.pseudoClasses.get(index);
                    PseudoClassSet.this.remove(impl);
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException("["+element+"]["+next+"]");
                }
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray() {
        return toArray(new PseudoClass[size()]);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size()) {
            a = (T[]) new PseudoClass[size()];
        }
        int index = 0;
        final List<PseudoClassImpl> pseudoClasses = PseudoClassImpl.pseudoClasses;
        for (int n=0, nMax=pseudoClasses.size(); n<nMax; n++) {
            PseudoClassImpl impl = pseudoClasses.get(n);
            if (contains(impl)) {
                a[index++] = (T) impl;
            }
        }
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(PseudoClass e) {
        
        if (e == null) {
            // this not modified!
            return false;
        }

        PseudoClassImpl impl = cast(e);
        
        final int element = impl.index / Long.SIZE;
        final long bit = 1l << (impl.index % Long.SIZE);
        
        // need to grow?
        if (element >= pseudoClasses.length) {
            final long[] temp = new long[element + 1];
            System.arraycopy(pseudoClasses, 0, temp, 0, pseudoClasses.length);
            pseudoClasses = temp;
        }
        
        final Long temp = pseudoClasses[element];
        pseudoClasses[element] = temp | bit;
        
        // if index[element] == temp, then the bit was already set
        final boolean modified = (pseudoClasses[element] != temp);
        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o) {
        
        if (o == null) {
            // this not modified!
            return false;
        }

        PseudoClassImpl impl = cast(o);

        final int element = impl.index / Long.SIZE;
        final long bit = 1l << (impl.index % Long.SIZE);

        if (element >= pseudoClasses.length) {
            // not in this Set!
            return false;
        }
        
        final Long temp = pseudoClasses[element];
        pseudoClasses[element] = temp & ~bit;

        // if index[element] == temp, then the bit was not there
        final boolean modified = (pseudoClasses[element] != temp);
        return modified;
    }

    
    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        
        final PseudoClassImpl impl = cast(o);

        final int element = impl.index / Long.SIZE;
        final long bit = 1l << (impl.index % Long.SIZE);

        return (element < pseudoClasses.length) && (pseudoClasses[element] & bit) == bit;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> c) {

        if (c == null) {
            // this not modified!
            return false;
        }
        
        if (c instanceof PseudoClassSet) {
            
            PseudoClassSet other = (PseudoClassSet)c;
            
            // this contains all of other if both are empty
            if (pseudoClasses.length == 0 && other.pseudoClasses.length == 0) {
                return true;
            }
            // [foo] cannot contain all of [foo bar]
            if (pseudoClasses.length < other.pseudoClasses.length) {
                return false;
            }
            // does [foo bar bang] contain all of [foo bar]?
            for (int n = 0, max = other.pseudoClasses.length; n < max; n++) {
                if ((pseudoClasses[n] & other.pseudoClasses[n]) != other.pseudoClasses[n]) {
                    return false;
                }
            }
            return true;
        }
        
        // [foo] cannot contain all of [foo bar]
        if (size() < c.size()) {
            return false;
        }

        // The hard way...        
        for (Iterator<?> iter = c.iterator(); iter.hasNext();) {
            final PseudoClass pseudoClass = (PseudoClass) iter.next();
            if (!contains(pseudoClass)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends PseudoClass> c) {
        
        if (c == null) {
            // this not modified!
            return false;
        }
        
        boolean modified = false;
        
        if (c instanceof PseudoClassSet) {
            
            PseudoClassSet other = (PseudoClassSet)c;
            boolean triggerTransition = false;
            
            if (other.isEmpty()) {
                // this not modified!
                return false;
            } 
            
            final long[] maskOne = this.pseudoClasses;
            final long[] maskTwo = other.pseudoClasses;

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
                
                final boolean different = (union[n] != maskOne[n]);
                modified |= different;

                if (!triggerTransition && different) {
                    if (n < triggerStates.length) {
                        triggerTransition = (union[n] & triggerStates[n]) != 0;
                    }
                }
            }
            if (modified) {
                this.pseudoClasses = union;
            }
            return modified;
        }
        
        // The hard way...
        for (Iterator<?> iter = c.iterator(); iter.hasNext();) {
            final PseudoClass pseudoClass = (PseudoClass) iter.next();
            modified |= add(pseudoClass);
        }
        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c) {

        if (c == null) {
            // this not modified!
            return false;
        }
        
        boolean modified = false;
        if (c instanceof PseudoClassSet) {
            
            PseudoClassSet other = (PseudoClassSet)c;
            boolean triggerTransition = false;
            
            if (other.isEmpty()) {
                // this not modified!
                return false;
            }

            final long[] maskOne = this.pseudoClasses;
            final long[] maskTwo = other.pseudoClasses;

            final int max = Math.min(maskOne.length, maskTwo.length);
            for(int n = 0; n < max; n++) {
                long temp = maskOne[n] & maskTwo[n];
                
                boolean different = temp != maskOne[n];
                modified |= different;
                
                if (!triggerTransition && different) {
                    if (n < triggerStates.length) {
                        triggerTransition = (temp & triggerStates[n]) != 0;
                    }
                }
                
                maskOne[n] = temp; 
            }
            
            return modified;
        }
        
        for (Iterator<?> iter = iterator(); iter.hasNext();) {
            final PseudoClass pseudoClass = (PseudoClass) iter.next();
            if (!c.contains(pseudoClass)) {
                modified |= remove(pseudoClass);
            }
        }
        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c) {
        
        if (c == null) {
            // this not modified!
            return false;
        }
        
        boolean modified = false;
        
        if (c instanceof PseudoClassSet) {
            
            PseudoClassSet other = (PseudoClassSet)c;
            boolean triggerTransition = false; 
            
            if (other.isEmpty()) {
                // this was not modified!
                return false;
            }

            final long[] maskOne = pseudoClasses;
            final long[] maskTwo = other.pseudoClasses;

            final int max = Math.min(maskOne.length, maskTwo.length);
            for(int n = 0; n < max; n++) {
                long temp = maskOne[n] & ~maskTwo[n];

                boolean different = temp != maskOne[n];
                modified |= different;
                
                if (!triggerTransition && different) {
                    if (n < triggerStates.length) {
                        triggerTransition = (maskOne[n] & triggerStates[n]) != 0;
                    }
                }
                
                maskOne[n] = temp;
            }

            return modified;            
        }
        
        // the hard way...
        if (size() <= c.size()) {
            for (Iterator<?> iter = c.iterator(); iter.hasNext();) {
                final PseudoClass pseudoClass = (PseudoClass) iter.next();
                if (contains(pseudoClass)) {
                    modified |= remove(pseudoClass);
                }
            }
        } else {
            for (Iterator<?> iter = iterator(); iter.hasNext();) {
                final PseudoClass pseudoClass = (PseudoClass) iter.next();
                if (c.contains(pseudoClass)) {
                    modified |= remove(pseudoClass);
                }
            }
        }
        return modified;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        pseudoClasses = new long[1];
    }
    
    boolean isTransition(PseudoClass e) {

        boolean isTransition = false;
        
        PseudoClassImpl impl = cast(e);

        final int element = impl.index / Long.SIZE;
        final long bit = 1l << (impl.index % Long.SIZE);
        
        if (element < triggerStates.length) {
            final long m = triggerStates[element];
            isTransition = (m & bit) == bit;
        }
        
        return isTransition;
        
    }

    /** @return The list of PseudoClass that are represented by this States object */
    public List<PseudoClass> getPseudoClasses() {
        final List<PseudoClass> list = new ArrayList<PseudoClass>();
        final List<PseudoClassImpl> pclases = PseudoClassImpl.pseudoClasses;
        for (int n=0, nMax=pclases.size(); n<nMax; n++) {
            final PseudoClassImpl impl = pclases.get(n);
            if (contains(impl)) {
                list.add(impl);
            }
        }
        return list;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Arrays.hashCode(this.pseudoClasses);
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
        final PseudoClassSet other = (PseudoClassSet) obj;
        if (!Arrays.equals(this.pseudoClasses, other.pseudoClasses)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final List<PseudoClass> list = getPseudoClasses();
        if (list != null) {
            return list.toString();
        } else {
            return "[]";
        }
    }

   static List<PseudoClass> getPseudoClasses(long[] masks) {
        final List<PseudoClass> pseudoClasses = new ArrayList<PseudoClass>();
        for (int n=0; n<masks.length; n++) {
            for (int l=0; l<Long.SIZE; l++) {
                long bit = 1l << l;
                if ((masks[n] & bit) == bit) {
                    int index = n*Long.SIZE + l;
                    if (index < PseudoClassImpl.pseudoClasses.size()) {
                        pseudoClasses.add(PseudoClassImpl.pseudoClasses.get(index));                        
                    }
                }                    
            }
        }
        return pseudoClasses;
   }
    
   static long[] addPseudoClass(long[] pseudoClasses, int pseudoClassIndex) {
       
        final int index = (pseudoClassIndex / Long.SIZE);
        final long bit   = 1l << (pseudoClassIndex % Long.SIZE);
        
        long[] temp = pseudoClasses;
        if (temp == null || temp.length <= index) {
            temp = new long[index+1];
            System.arraycopy(pseudoClasses, 0, temp, 0, pseudoClasses.length);
        }
        
        temp[index] |= bit;       
        return temp;
   }
   
   static boolean containsPseudoClass(long[] pseudoClasses, int pseudoClassIndex) {
       
        final int index = (pseudoClassIndex / Long.SIZE);
        final long bit   = 1l << (pseudoClassIndex % Long.SIZE);
        
        if (pseudoClasses == null || index < pseudoClasses.length) {
            return false;
        }
        
        return (pseudoClasses[index] & bit) == bit;
   }
   
   private long[] EMPTY_SET = new long[0];
    /** Create an empty set of PseudoClass */
    protected PseudoClassSet() {
        this.pseudoClasses = EMPTY_SET;
        this.triggerStates = EMPTY_SET;
    }

    /*
     * Try to cast the arg to a PseudoClassImpl.
     * @throws ClassCastException if the class of the argument is
     *         is not a PseudoClass
     * @throws NullPointerException if the argument is null
     */
    private PseudoClassImpl cast(Object o) {
        if (o == null) {
            throw new NullPointerException("null arg");
        }
        PseudoClass pseudoClass = (PseudoClass) o;
        PseudoClassImpl impl = (pseudoClass instanceof PseudoClassImpl) 
            ? (PseudoClassImpl) pseudoClass 
            : (PseudoClassImpl) PseudoClassImpl.getPseudoClassImpl(pseudoClass.getPseudoClassName());
        return impl;
    }

    // the set
    long[] pseudoClasses;

    // masks of state that, if changed, will trigger a css state transition
    long[] triggerStates;
    
}
