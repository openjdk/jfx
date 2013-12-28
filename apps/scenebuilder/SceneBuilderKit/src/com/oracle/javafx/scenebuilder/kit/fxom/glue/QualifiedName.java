/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.fxom.glue;

import java.util.Objects;

/**
 *
 * 
 */
class QualifiedName implements Comparable<QualifiedName> {
    
    private final String qualifier;
    private final String name;

    public QualifiedName(String qualifier, String name) {
        assert name != null;
        this.qualifier = qualifier;
        this.name = name;
    }
    
    public QualifiedName(String qualifiedName) {
        final int dotIndex = qualifiedName.indexOf('.');
        if (dotIndex == -1) {
            this.qualifier = null;
            this.name = qualifiedName;
        } else {
            this.qualifier = qualifiedName.substring(0, dotIndex);
            this.name = qualifiedName.substring(dotIndex+1);
        }
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getName() {
        return name;
    }
    
    /*
     * Object
     */
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.qualifier);
        hash = 71 * hash + Objects.hashCode(this.name);
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
        final QualifiedName other = (QualifiedName) obj;
        if (!Objects.equals(this.qualifier, other.qualifier)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    /*
     * Comparable
     */
    
    @Override
    public int compareTo(QualifiedName o) {
        int result;
        
        if (this == o) {
            result = 0;
        } else if (o == null) {
            result = -1;
        } else {
            if ((this.qualifier == null) && (o.qualifier == null)) {
                result = 0;
            } else if (this.qualifier == null) {
                result = -1;
            } else if (o.qualifier == null) {
                result = +1;
            } else {
                result = this.qualifier.compareTo(o.qualifier);
            }
            if (result == 0) {
                assert this.name != null;
                assert o.name != null;
                result = this.name.compareTo(o.name);
            }
        }
        
        return result;
    }
    
    
}
