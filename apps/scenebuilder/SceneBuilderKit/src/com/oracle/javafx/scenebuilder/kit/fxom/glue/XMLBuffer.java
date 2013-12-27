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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 */
class XMLBuffer {
    
    private final StringBuffer buffer = new StringBuffer();
    private final List<String> elementStack = new ArrayList<>();
    private boolean tagOpened;
    
    /*
     * XMLBuffer
     */
    
    public XMLBuffer() {
        clear();
    }
    
    public final void clear() {
        elementStack.clear();
        tagOpened = false;
        buffer.delete(0, buffer.length());
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //NOI18N
    }
    
    public void addProcessingInstruction(String target, String content) {
        assert target != null;
        assert content != null;
        assert elementStack.isEmpty();
        assert tagOpened == false;
        
        buffer.append("<?"); //NOI18N
        buffer.append(target);
        buffer.append(' ');
        buffer.append(content);
        buffer.append("?>"); //NOI18N
    }
    
    public void beginElement(String elementName) {
        assert elementName != null;
        
        if (tagOpened) {
            buffer.append(">"); //NOI18N
        }
        buffer.append('<');
        buffer.append(elementName);
        elementStack.add(elementName);
        tagOpened = true;
    }
    
    public void addAttribute(String attributeName, String attributeValue) {
        assert attributeName != null;
        assert attributeValue != null;
        assert tagOpened;
        
        buffer.append(' ');
        buffer.append(attributeName);
        buffer.append("=\""); //NOI18N
        buffer.append(encodeToAttributeValue(attributeValue));
        buffer.append('"');
    }
    
    public void endElement() {
        assert elementStack.isEmpty() == false;
        
        final String elementName = elementStack.get(elementStack.size()-1);
        elementStack.remove(elementStack.size()-1);
        if (tagOpened) {
            buffer.append(" />"); //NOI18N
            tagOpened = false;
        } else {
            buffer.append("</"); //NOI18N
            buffer.append(elementName);
            buffer.append(">"); //NOI18N
        }
    }
    
    
    public void addText(String text) {
        if (tagOpened) {
            buffer.append(">"); //NOI18N
            tagOpened = false;
        }
        buffer.append(text);
    }
    
    
    public void addComment(String comment) {
        assert tagOpened == false;
        buffer.append("<!--"); //NOI18N
        buffer.append(comment);
        buffer.append("-->"); //NOI18N
    }
    
    public void addLineSeparator() {
        if (tagOpened) {
            buffer.append(">"); //NOI18N
            tagOpened = false;
        }
        buffer.append('\n');
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        assert elementStack.isEmpty();
        return buffer.toString();
    }
    
    /*
     * Private
     */    
    
    
    private String encodeToAttributeValue(String s) {
        final StringBuffer result = new StringBuffer();
        
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '&': 
                    result.append("&amp;");  //NOI18N
                    break;
                case '<':
                    result.append("&lt;"); //NOI18N
                    break;
                case '>':
                    result.append("&gt;"); //NOI18N
                    break;
                case '"': 
                    result.append("&quot;"); //NOI18N
                    break;
                default :
                    if (Character.isISOControl(c)) {
                        result.append("&#"); //NOI18N
                        result.append((int) c);
                        result.append(';');
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        
        return result.toString();
    }

}
