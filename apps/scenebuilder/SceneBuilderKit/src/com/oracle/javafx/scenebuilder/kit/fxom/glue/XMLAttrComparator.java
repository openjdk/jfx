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
package com.oracle.javafx.scenebuilder.kit.fxom.glue;

import java.util.Comparator;
import java.util.Map;

/**
 *
 * 
 */
class XMLAttrComparator implements Comparator<Map.Entry<String,String>> {

    @Override
    public int compare(Map.Entry<String,String> attr1, Map.Entry<String,String> attr2) {
        assert attr1 != null;
        assert attr2 != null;
        
        final int aoi1 = getAttrOrderIndex(attr1);
        final int aoi2 = getAttrOrderIndex(attr2);
        final int result;
        
        if ((aoi1 == 2) && (aoi2 == 2)) {
            final QualifiedName qn1 = new QualifiedName(attr1.getKey());
            final QualifiedName qn2 = new QualifiedName(attr2.getKey());
            result = qn1.compareTo(qn2);
        } else {
            result = Integer.compare(aoi1, aoi2);
        }
        
        return result;
    }
    
    
    /*
     * Private
     */
    
    private int getAttrOrderIndex(Map.Entry<String,String> attr) {
        assert attr != null;
        
        /*
         * fx:id < id < other-attr < fx:controller
         */
        
        final int result;
        switch(attr.getKey()) {
            case "id": //NOI18N
                result = 0;
                break;
            case "fx:id": //NOI18N
                if (attr.getValue().startsWith("x")) { //NOI18N
                    // Auto-generated fx:id goes at the end
                    result = 10000;
                } else {
                    result = 1;
                }
                break;
            default:
                result = 2;
                break;
            case "fx:value": //NOI18N
                result = 3;
                break;
            case "fx:factory": //NOI18N
                result = 4;
                break;
            case "xmlns": //NOI18N
                result = 5;
                break;
            case "xmlns:fx": //NOI18N
                result = 5;
                break;
            case "fx:controller": //NOI18N
                result = 6;
                break;
        }
        
        return result;
    }
}
