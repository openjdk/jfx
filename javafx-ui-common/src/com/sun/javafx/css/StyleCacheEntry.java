/*
 *  Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package com.sun.javafx.css;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;

/**
 *
 */
public final class StyleCacheEntry {
    
    
    public StyleCacheEntry(StyleCache.Key sharedCacheKey, StyleCacheEntry.Key sharedEntryKey) {
        this.sharedCacheKey = new StyleCache.Key(sharedCacheKey);
        this.sharedEntryKey = new StyleCacheEntry.Key(sharedEntryKey);
        this.values = new HashMap<String,CalculatedValue>();
    }
    
    private StyleCacheEntry() {
        this.sharedCacheKey = null;
        this.sharedEntryKey = null;
        this.values = new HashMap<String,CalculatedValue>();
    }
    
    public CalculatedValue getFont() {
        return font;
    }
    
    public void setFont(CalculatedValue font) {
        this.font = font;
    }
    
    public CalculatedValue get(String property) {

        CalculatedValue cv = null;
        if (values.isEmpty() == false) {
            cv = values.get(property);
        }
        if (cv == null) {
            final Map<String,CalculatedValue> sharedValues = getSharedValues();
            if (sharedValues != null) cv = sharedValues.get(property);
            else cv = CalculatedValue.SKIP;
        }
        return cv;
    }

    public void put(String property, CalculatedValue cv) {

        // If the origin of the calculated value is inline or user,
        // then use local cache.
        // If the origin of the calculated value is not inline or user,
        // then use local cache if the font origin is inline or user and
        // the value was calculated from a relative size unit.
        final boolean isLocal =
            (cv.getOrigin() == StyleOrigin.INLINE || cv.getOrigin() == StyleOrigin.USER)            
            || (cv.isRelative() &&
                (font.getOrigin() == StyleOrigin.INLINE || 
                 font.getOrigin() == StyleOrigin.USER));
                

        if (isLocal) {
            values.put(property, cv);
        } else {
            final Map<String,CalculatedValue> sharedValues = getSharedValues();
            if (sharedValues != null && sharedValues.containsKey(property) == false) {
                // don't override value already in shared cache.
                sharedValues.put(property, cv);
            }
        }
    }

    private Map<String,CalculatedValue> getSharedValues() {
        
        StyleCacheEntry entry = null;
        
        StyleCache styleCache = StyleManager.getInstance().getStyleCache(sharedCacheKey);
        
        if (styleCache != null) {
            
            entry = styleCache.getStyleCacheEntry(sharedEntryKey);
            
            if (entry == null) {
                entry = new StyleCacheEntry();
                Key key = new Key(sharedEntryKey);
                styleCache.putStyleCacheEntry(key, entry);
            }
        }        
        
        return entry.values;
    }
    
    public final static class Key {

        private final Set<PseudoClass>[] pseudoClassStates;
    
        public Key(Set<PseudoClass>[] pseudoClassStates, int count) {
                        
            this.pseudoClassStates = new PseudoClassState[count];
            
            for (int n=0; n<count; n++) {
                this.pseudoClassStates[n] = new PseudoClassState();
                this.pseudoClassStates[n].addAll(pseudoClassStates[n]);
            }
        }
        
        private Key(Key other) {
            this(other.pseudoClassStates, other.pseudoClassStates != null ? other.pseudoClassStates.length : 0);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            final int iMax = pseudoClassStates != null ? pseudoClassStates.length : 0;
            
            for (int i=0; i<iMax; i++) {
                
                final Set<PseudoClass> states = pseudoClassStates[i];
                if (states != null) {                
                    hash = 67 * hash + states.hashCode();
                }
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
            final Key other = (Key) obj;
            
            // either both must be null or both must be not-null
            if ((pseudoClassStates == null) ^ (other.pseudoClassStates == null)) {
                return false;
            }
            
            // if one is null, the other is too. 
            if (pseudoClassStates == null) {
                return true;
            }
            
            if (pseudoClassStates.length != other.pseudoClassStates.length) {
                return false;
            }
            
            for (int i=0; i<pseudoClassStates.length; i++) {
                
                final Set<PseudoClass> this_pcs = pseudoClassStates[i];
                final Set<PseudoClass> other_pcs = other.pseudoClassStates[i];
                
                // if one is null, the other must be too
                if (this_pcs == null ? other_pcs != null : !this_pcs.equals(other_pcs)) {
                    return false;
                }
            }
            
            return true;
        }

    }
        
    private final StyleCache.Key sharedCacheKey;
    private final StyleCacheEntry.Key sharedEntryKey;
    private final Map<String,CalculatedValue> values;
    private CalculatedValue  font; // for use in converting font relative sizes
}
