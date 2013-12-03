/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @treatAsPrivate
 */
public class BuiltinSectionComparator implements Comparator<String> {
    
    private static final List<String> orderedSections = new ArrayList<>();
    
    static {
        orderedSections.add(BuiltinLibrary.TAG_CONTAINERS);
        orderedSections.add(BuiltinLibrary.TAG_CONTROLS);
        orderedSections.add(BuiltinLibrary.TAG_MENU);
        orderedSections.add(BuiltinLibrary.TAG_MISCELLANEOUS);
        orderedSections.add(BuiltinLibrary.TAG_SHAPES);
        orderedSections.add(BuiltinLibrary.TAG_CHARTS);
        orderedSections.add(BuiltinLibrary.TAG_3D);
    }
    
    
    /*
     * Comparator
     */

    @Override
    public int compare(String section1, String section2) {
        assert section1 != null;
        assert section2 != null;
        
        final int index1 = orderedSections.indexOf(section1);
        final int index2 = orderedSections.indexOf(section2);
        final int result;
        
        if ((index1 != -1) && (index2 != -1)) {
            // section1 and section2 are both predefined names
            result = Integer.compare(index1, index2);
        } else if (index1 != -1) {
            // only section1 is predefined -> goes before section2
            result = +1;
        } else if (index2 != -1) {
            // only section2 is predefined -> goes before section1
            result = -1;
        } else {
            // section1 and section2 are both custom
            result = section1.compareTo(section2);
        }
        
        return result;
    }
    
}
