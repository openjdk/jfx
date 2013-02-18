/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.model;

import java.util.Locale;

/**
 */
public class Variable {

    private final String name;
    private final Type type;
    private final Qualifier qual;
    private final Precision precision;
    private final int reg;
    private final int arraySize;
    private final Object constValue;
    private final boolean isParam;
    private int refCount;

    Variable(String name, Type type) {
        this(name, type, null, null, -1, -1, null, false);
    }
    
    Variable(String name, Type type, Qualifier qual, Precision precision,
             int reg, int arraySize, Object constValue, boolean isParam)
    {
        if (name == null) {
            throw new IllegalArgumentException("Name must be non-null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must be non-null");
        }
        this.name = name;
        this.type = type;
        this.qual = qual;
        this.precision = precision;
        this.reg = reg;
        this.arraySize = arraySize;
        this.constValue = constValue;
        this.isParam = isParam;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
    
    public Qualifier getQualifier() {
        return qual;
    }

    public Precision getPrecision() {
        return precision;
    }

    public int getReg() {
        return reg;
    }
    
    public boolean isArray() {
        return arraySize > 0;
    }
    
    public int getArraySize() {
        return arraySize;
    }
    
    public Object getConstValue() {
        return constValue;
    }
    
    public boolean isParam() {
        return isParam;
    }
    
    /**
     * Returns the JavaBean-style accessor name for this variable.  For
     * example, if variable.getName() returns "someVariable", this method
     * will return "getSomeVariable".
     */
    public String getAccessorName() {
        return "get" + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
    }

    public void incrementRefCount() {
        refCount++;
    }

    public boolean isReferenced() {
        return refCount > 0;
    }
}
