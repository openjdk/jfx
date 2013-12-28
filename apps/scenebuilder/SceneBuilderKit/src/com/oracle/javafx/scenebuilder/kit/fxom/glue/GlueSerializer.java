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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * 
 */
class GlueSerializer {
    
    private static final XMLAttrComparator attrComparator
            = new XMLAttrComparator();
    
    private final GlueDocument document;
    
    public GlueSerializer(GlueDocument document) {
        assert document.getRootElement() != null;
        this.document = document;
    }
    
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        final XMLBuffer result = new XMLBuffer();
        
        result.addLineSeparator();
        result.addLineSeparator();
        
        Class<? extends GlueAuxiliary> lastAuxiliaryClass = null;
        for (GlueAuxiliary auxiliary : document.getHeader()) {
            if ((lastAuxiliaryClass != null) && (lastAuxiliaryClass != auxiliary.getClass())) {
                // We insert an extra empty line to separate
                // sequences of processing instructions, comments ...
                result.addLineSeparator();
            }
            serializeAuxiliary(auxiliary, result);
            result.addLineSeparator();
            lastAuxiliaryClass = auxiliary.getClass();
        }
        
        if (lastAuxiliaryClass != null) {
            result.addLineSeparator();
        }
        
        serializeElement(document.getRootElement(), result);
        result.addLineSeparator();
        
        return result.toString();
    }
    
    private void serializeElement(GlueElement element, XMLBuffer xmlBuffer) {
        for (GlueAuxiliary auxiliary : element.getFront()) {
            serializeAuxiliary(auxiliary, xmlBuffer);
        }
        xmlBuffer.beginElement(element.getTagName());
        serializeAttributes(element, xmlBuffer);
        if (element.getChildren().isEmpty()) {
            for (GlueAuxiliary auxiliary : element.getContent()) {
                serializeAuxiliary(auxiliary, xmlBuffer);
            }
        } else {
            for (GlueElement child : element.getChildren()) {
                serializeElement(child, xmlBuffer);
            }
            for (GlueAuxiliary auxiliary : element.getTail()) {
                serializeAuxiliary(auxiliary, xmlBuffer);
            }
        }
        xmlBuffer.endElement();
    }
    
    private void serializeAuxiliary(GlueAuxiliary auxiliary, XMLBuffer xmlBuffer) {
        if (auxiliary instanceof GlueCharacters) {
            final GlueCharacters characters = (GlueCharacters) auxiliary;
            switch(characters.getType()) {
                case TEXT:
                    xmlBuffer.addText(characters.getData());
                    break;
                case COMMENT:
                    xmlBuffer.addComment(characters.getData());
                    break;
                default:
                    assert false;
                    break;
            }
        } else {
            assert auxiliary instanceof GlueInstruction;
            final GlueInstruction instruction = (GlueInstruction) auxiliary;
            xmlBuffer.addProcessingInstruction(instruction.getTarget(), instruction.getData());
        }
    }
    
    
    private void serializeAttributes(GlueElement element, XMLBuffer xmlBuffer) {
        
        final Map<String, String> attributes = element.getAttributes();
        final List<Map.Entry<String,String>> attrNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attrNames.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(attrNames, attrComparator);
        
        for (Map.Entry<String,String> e : attrNames) {
            xmlBuffer.addAttribute(e.getKey(), e.getValue());
        }
    }
}
