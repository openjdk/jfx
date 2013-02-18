/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
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

        PseudoClassImpl instance = null;
        
        final Integer value = pseudoClassMap.get(pseudoClass);
        final int index = value != null ? value.intValue() : -1;
        
        final int size = pseudoClasses.size();
        assert index < size;
        
        if (index != -1 && index < size) {
            instance = pseudoClasses.get(index);
        }
        
        if (instance == null) {
            instance = new PseudoClassImpl(pseudoClass, size);
            pseudoClasses.add(instance);
            pseudoClassMap.put(pseudoClass, Integer.valueOf(size));
        }
        
        return instance;
    }


    /** @return the pseudo-class state */
    @Override
    public String getPseudoClassName() {
        return pseudoClassName;
    }

    /** @return the pseudo-class state */
   @Override public String toString() {
        return pseudoClassName;
    }

    /** Cannot create an instance of State except through PseudoClass static methods */
    private PseudoClassImpl(String pseudoClassName, int index) {
        this.pseudoClassName = pseudoClassName;
        this.index = index;
    }

    final String pseudoClassName;

    // index of this PseudoClass in pseudoClasses list.
    final int index;
    
    // package private for unit test purposes
    static final Map<String,Integer> pseudoClassMap = 
            new HashMap<String,Integer>(64);

    static final List<PseudoClassImpl> pseudoClasses =
            new ArrayList<PseudoClassImpl>();
    
}
