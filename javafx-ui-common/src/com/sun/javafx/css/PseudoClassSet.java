/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
public final class PseudoClassSet implements Set<PseudoClass> {

    /** {@inheritDoc} */
    @Override
    public int size() {

        int size = 0;
        if (bitMask.length > 0) {
            for (int n = 0; n < bitMask.length; n++) {
                final long mask = bitMask[n] & PseudoClassImpl.VALUE_MASK;
                if (mask != 0) {
                    size += Long.bitCount(mask);
                }
            }
        }
        // bitMask.length is zero or all bitMask[n] values are zero
        return size;

    }

    @Override
    public boolean isEmpty() {
        
        if (bitMask.length > 0) {
            for (int n = 0; n < bitMask.length; n++) {
                final long mask = bitMask[n] & PseudoClassImpl.VALUE_MASK;
                if (mask != 0) {
                    return false;
                }
            }
        }
        // bitMask.length is zero or all bitMask[n] values are zero
        return true;

    }

    /**
     * {@inheritDoc} This returned iterator is not fail-fast.
     */
    @Override
    public Iterator<PseudoClass> iterator() {
        return new Iterator<PseudoClass>() {
            Iterator<PseudoClassImpl> iter = PseudoClassImpl.stateMap.values().iterator();
            PseudoClassImpl next = null;

            @Override
            public boolean hasNext() {
                boolean found = false;
                while (!found && iter.hasNext()) {
                    PseudoClassImpl impl = iter.next();
                    found = contains(impl);
                    if (found) {
                        next = impl;
                    }
                }
                if (!found) {
                    next = null;
                }
                return found;
            }

            @Override
            public PseudoClass next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                return next;
            }

            @Override
            public void remove() {
                PseudoClassSet.this.remove(next);
                next = null;
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
        Iterator<PseudoClassImpl> iter = PseudoClassImpl.stateMap.values().iterator();
        while (iter.hasNext()) {
            PseudoClassImpl impl = iter.next();
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
        //
        // When creating the long[] bit set, you get the State bitMask
        // and shift the upper 4 bits to get the index of the State in
        // the long[], then or the value from the bitMask with the
        // mask[index].
        //
        final long m = impl.bitMask;
        final long element = m & ~PseudoClassImpl.VALUE_MASK;
        final int index = (int) (element >>> PseudoClassImpl.VALUE_BITS);
        // need to grow?
        if (index >= bitMask.length) {
            final long[] temp = new long[index + 1];
            System.arraycopy(bitMask, 0, temp, 0, bitMask.length);
            bitMask = temp;
        }
        final Long temp = bitMask[index];
        bitMask[index] = temp | m;
        // if bitMask[index] == temp, then the bit was already set
        // and the add doesn't get counted.
        return (bitMask[index] != temp);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o) {
        
        if (o == null) {
            // this not modified!
            return false;
        }
        
        PseudoClassImpl impl = cast(o);
        final long m = impl.bitMask;
        final long mask = m & PseudoClassImpl.VALUE_MASK;
        final long element = m & ~PseudoClassImpl.VALUE_MASK;
        final int index = (int) (element >>> PseudoClassImpl.VALUE_BITS);
        final Long temp = bitMask[index];
        bitMask[index] = temp & ~mask;
        // if bitMask[index] == temp, then the bit was not there
        // and the remove doesn't get counted.
        return (bitMask[index] != temp);
    }

    
    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        
        final PseudoClassImpl impl = cast(o);
        final long m = impl.bitMask;
        final long element = m & ~PseudoClassImpl.VALUE_MASK;
        final int index = (int) (element >>> PseudoClassImpl.VALUE_BITS);
        return (index < bitMask.length) && (bitMask[index] & m) == m;
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
            if (bitMask.length == 0 && other.bitMask.length == 0) {
                return true;
            }
            // [foo] cannot contain all of [foo bar]
            if (bitMask.length < other.bitMask.length) {
                return false;
            }
            // does [foo bar bang] contain all of [foo bar]?
            for (int n = 0, max = other.bitMask.length; n < max; n++) {
                if ((bitMask[n] & other.bitMask[n]) != other.bitMask[n]) {
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
            
            if (other.isEmpty()) {
                // this not modified!
                return false;
            } 
            
            final long[] maskOne = this.bitMask;
            final long[] maskTwo = other.bitMask;

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
                
                modified |= (union[n] & PseudoClassImpl.VALUE_MASK) != 0;
            }
            if (modified) {
                this.bitMask = union;
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
            
            if (other.isEmpty()) {
                // this not modified!
                return false;
            }

            final long[] maskOne = this.bitMask;
            final long[] maskTwo = other.bitMask;

            final int max = Math.min(maskOne.length, maskTwo.length);
            for(int n = 0; n < max; n++) {
                long temp = maskOne[n] & maskTwo[n];
                modified |= temp != maskOne[n];
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
            
            if (other.isEmpty()) {
                // this was not modified!
                return false;
            }

            final long[] maskOne = bitMask;
            final long[] maskTwo = other.bitMask;

            final int max = Math.min(maskOne.length, maskTwo.length);
            for(int n = 0; n < max; n++) {
                long temp = maskOne[n] & ~maskTwo[n];
                modified |= temp != maskOne[n];
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
        bitMask = new long[1];
    }

    /** @return The list of PseudoClass that are represented by this States object */
    public List<PseudoClass> getPseudoClasses() {
        final List<PseudoClass> pseudoClasses = new ArrayList<PseudoClass>();
        for (PseudoClassImpl impl : PseudoClassImpl.stateMap.values()) {
            if (contains(impl)) {
                pseudoClasses.add(impl);
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
        final PseudoClassSet other = (PseudoClassSet) obj;
        if (!Arrays.equals(this.bitMask, other.bitMask)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final List<PseudoClass> pseudoClasses = getPseudoClasses();
        if (pseudoClasses != null) {
            return pseudoClasses.toString();
        } else {
            return "[]";
        }
    }

    /** Create an empty set of PseudoClass */
    public PseudoClassSet() {
        this(new long[0]);
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

    private PseudoClassSet(long[] bitMask) {
        this.bitMask = bitMask;
    }
    private long[] bitMask;
        
}
