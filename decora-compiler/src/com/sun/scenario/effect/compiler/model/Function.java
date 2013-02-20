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

import java.util.Collections;
import java.util.List;

/**
 */
public class Function {

    private final String name;
    private final Type returnType;
    private final List<Param> params;

    Function(String name, Type returnType, List<Param> params) {
        this.name = name;
        this.returnType = returnType;
        if (params != null) {
            this.params = params;
        } else {
            this.params = Collections.emptyList();
        }
    }

    public String getName() {
        return name;
    }
    
    public Type getReturnType() {
        return returnType;
    }
    
    public List<Param> getParams() {
        return params;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Function other = (Function) obj;
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        if (this.returnType != other.returnType) {
            return false;
        }
        if (this.params != other.params && (this.params == null || !this.params.equals(other.params))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 71 * hash + (this.returnType != null ? this.returnType.hashCode() : 0);
        hash = 71 * hash + (this.params != null ? this.params.hashCode() : 0);
        return hash;
    }
}
