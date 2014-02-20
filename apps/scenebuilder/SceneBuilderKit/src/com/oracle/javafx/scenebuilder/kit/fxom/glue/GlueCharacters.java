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

/**
 *
 * 
 */
public class GlueCharacters extends GlueAuxiliary {
    
    public enum Type {
        TEXT,
        COMMENT
    }
    
    private final Type type;
    private String data;
    
    public GlueCharacters(GlueDocument document, Type type, String data) {
        super(document);
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
    
    public void adjustIndentBy(int delta) {
        /*
         * data
         * 
         * 'xxxxxxx\nbbbbbbxxxxxxx\nbbbbbbbbxxxxxxxxxx\nbbbbbxxxxx....'
         * 
         *  b : white space
         *  x : any other char
         * 
         * Indenting means:
         * - when delta > 0, inserting 'delta' spaces after each '\n' char
         * - when delta < 0, removing 'delta' spaces after each '\n' char *when possible*
         */
        
        final StringBuilder newValue = new StringBuilder();
        
        if (delta > 0) {
            for (int i = 0, length = data.length(); i < length; i++) {
                final char ch = data.charAt(i);
                newValue.append(ch);
                if (ch == '\n') {
                    for (int n = 0; n < delta; n++) {
                        newValue.append(' ');
                    }
                }
            }
        } else {
            for (int i = 0, length = data.length(); i < length; i++) {
                final char ch = data.charAt(i);
                newValue.append(ch);
                if (ch == '\n') {
                    while ((i+1 < length) 
                            && (data.charAt(i+1) == ' ')
                            && (delta < 0)) {
                        i++;
                        delta++;
                    }
                }
            }
        }
        
        data = newValue.toString();
    }
    
    public int guessIndent() {
        int result;
        
        /*
         * If data match the following pattern
         * 
         * 'xxxxxxx\nbbbbbbxxxxxxx....'
         * 
         *  b : white space
         *  x : any other char
         * 
         * then returns number of b characters else returns -1.
         */
        
        int i = 0;
        final int count = data.length();
        while ((i < count) && data.charAt(i) != '\n') {
            i++;
        }
        
        if (i < count) {
            i++;
            result = 0;
            while ((i < count) && data.charAt(i) == ' ') {
                result++;
                i++;
            }
        } else {
            result = -1;
        }
        
        return result;
    }
}
