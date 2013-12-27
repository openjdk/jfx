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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class InspectorPathComparator implements Comparator<InspectorPath> {
    
    private final List<String> sectionNames;
    private final Map<String, List<String>> subSectionMap;
    
    /*
     * Public
     */
    
    public InspectorPathComparator(List<String> sectionNames, Map<String, List<String>> subSectionMap) {
        this.sectionNames = sectionNames;
        this.subSectionMap = subSectionMap;
    }

    /*
     * Comparator
     */
    
    @Override
    public int compare(InspectorPath p1, InspectorPath p2) {
        assert p1 != null;
        assert p2 != null;

        final int result;

        if (p1 == p2) {
            result = 0;
        } else {
            final int sectionIndex1 = sectionNames.indexOf(p1.getSectionTag());
            final int sectionIndex2 = sectionNames.indexOf(p2.getSectionTag());
            
            assert sectionIndex1 != -1 : "sectionTag=" + p1.getSectionTag();
            assert sectionIndex2 != -1 : "sectionTag=" + p2.getSectionTag();

            if (sectionIndex1 < sectionIndex2) {
                result = -1;
            } else if (sectionIndex1 > sectionIndex2) {
                result = +1;
            } else {
                assert sectionIndex1 == sectionIndex2;
                assert p1.getSectionTag().equals(p2.getSectionTag());
                final List<String> subSections = subSectionMap.get(p1.getSectionTag());
                
                assert subSections != null : "sectionTag=" + p1.getSectionTag();
                
                final int subSectionIndex1 = subSections.indexOf(p1.getSubSectionTag());
                final int subSectionIndex2 = subSections.indexOf(p2.getSubSectionTag());
                
                assert subSectionIndex1 != -1 : "subSectionTag=" + p1.getSubSectionTag();
                assert subSectionIndex2 != -1 : "subSectionTag=" + p2.getSubSectionTag();

                if (subSectionIndex1 < subSectionIndex2) {
                    result = -1;
                } else if (subSectionIndex1 > subSectionIndex2) {
                    result = +1;
                } else {
                    assert subSectionIndex1 == subSectionIndex2;
                    final int propertyIndex1 = p1.getSubSectionIndex();
                    final int propertyIndex2 = p2.getSubSectionIndex();
                    if (propertyIndex1 < propertyIndex2) {
                        result = -1;
                    } else if (propertyIndex1 > propertyIndex2) {
                        result = +1;
                    } else {
                        result = 0;
                    }
                }
            }
        }

        return result;
    }
}
