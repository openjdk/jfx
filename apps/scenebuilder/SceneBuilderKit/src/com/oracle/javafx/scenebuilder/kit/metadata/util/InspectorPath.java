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
package com.oracle.javafx.scenebuilder.kit.metadata.util;

import java.util.Objects;

/**
 *
 */
public class InspectorPath implements Comparable<InspectorPath> {
    
    private final String sectionTag;
    private final String subSectionTag;
    private final int subSectionIndex;

    public static final String CUSTOM_SECTION = "Properties";
    public static final String CUSTOM_SUB_SECTION = "Custom";
    
    public static final InspectorPath UNUSED = new InspectorPath("", "", 0);
    
    public InspectorPath(String sectionTag, String subSectionTag, int subSectionIndex) {
        assert sectionTag != null;
        assert subSectionTag != null;
        assert subSectionIndex >= 0;
        
        this.sectionTag = sectionTag;
        this.subSectionTag = subSectionTag;
        this.subSectionIndex = subSectionIndex;
    }

    public String getSectionTag() {
        return sectionTag;
    }

    public String getSubSectionTag() {
        return subSectionTag;
    }

    public int getSubSectionIndex() {
        return subSectionIndex;
    }
    
    
    /*
     * Object
     */

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.sectionTag);
        hash = 97 * hash + Objects.hashCode(this.subSectionTag);
        hash = 97 * hash + this.subSectionIndex;
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
        final InspectorPath other = (InspectorPath) obj;
        if (!Objects.equals(this.sectionTag, other.sectionTag)) {
            return false;
        }
        if (!Objects.equals(this.subSectionTag, other.subSectionTag)) {
            return false;
        }
        if (this.subSectionIndex != other.subSectionIndex) {
            return false;
        }
        return true;
    }
    
    
    /*
     * Comparable
     */
    @Override
    public int compareTo(InspectorPath o) {
        int result;
        
        result = this.sectionTag.compareTo(o.sectionTag);
        
        if (result == 0) {
            result = this.subSectionTag.compareTo(o.subSectionTag);
        }
        
        if (result == 0) {
            if (this.subSectionIndex < o.subSectionIndex) {
                result = -1;
            } else if (this.subSectionIndex > o.subSectionIndex) {
                result = +1;
            } else {
                assert this.subSectionIndex == o.subSectionIndex;
                assert result == 0;
            }
        }
        
        return result;
    }
}
