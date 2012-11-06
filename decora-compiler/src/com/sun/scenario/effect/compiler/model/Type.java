/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates. All rights reserved.
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
public enum Type {
    VOID   (BaseType.VOID,  1),
    FLOAT  (BaseType.FLOAT, 1),
    FLOAT2 (BaseType.FLOAT, 2),
    FLOAT3 (BaseType.FLOAT, 3),
    FLOAT4 (BaseType.FLOAT, 4),
    INT    (BaseType.INT,   1),
    INT2   (BaseType.INT,   2),
    INT3   (BaseType.INT,   3),
    INT4   (BaseType.INT,   4),
    BOOL   (BaseType.BOOL,  1),
    BOOL2  (BaseType.BOOL,  2),
    BOOL3  (BaseType.BOOL,  3),
    BOOL4  (BaseType.BOOL,  4),
    SAMPLER(BaseType.SAMPLER, 1),
    LSAMPLER(BaseType.SAMPLER, 1),
    FSAMPLER(BaseType.SAMPLER, 1);
    
    private final BaseType baseType;
    private final int numFields;
    
    private Type(BaseType baseType, int numFields) {
        this.baseType = baseType;
        this.numFields = numFields;
    }
    
    public BaseType getBaseType() {
        return baseType;
    }
    
    public int getNumFields() {
        return numFields;
    }
    
    public boolean isVector() {
        return numFields > 1;
    }
    
    /**
     * Returns a {@code Type} instance given a lowercase token string.
     * For example, given "float3", this method will return {@code Type.FLOAT3}.
     */
    public static Type fromToken(String s) {
        return valueOf(s.toUpperCase(Locale.ENGLISH));
    }
    
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}
