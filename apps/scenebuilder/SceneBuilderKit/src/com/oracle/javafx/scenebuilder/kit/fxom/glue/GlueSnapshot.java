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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 */
public class GlueSnapshot {
    
    private final GlueElement topElement;
    private final Map<GlueCharacters, String> textMap = new HashMap<>();
    
    public GlueSnapshot(GlueElement element) {
        this.topElement = element;
        
        snapshot(this.topElement);
    }

    public GlueElement getTopElement() {
        return topElement;
    }
    
    public void restore() {
        for (Map.Entry<GlueCharacters, String> e : textMap.entrySet()) {
            e.getKey().setData(e.getValue());
        }
    }
    
    static void indent(GlueElement element, int delta) {
        for (GlueAuxiliary auxiliary : element.getFront()) {
            if (auxiliary instanceof GlueCharacters) {
                GlueSnapshot.indent((GlueCharacters)auxiliary, delta);
            }
        }
        for (GlueAuxiliary auxiliary : element.getTail()) {
            if (auxiliary instanceof GlueCharacters) {
                GlueSnapshot.indent((GlueCharacters)auxiliary, delta);
            }
        }
        for (GlueElement child : element.getChildren()) {
            indent(child, delta);
        }
    }
    
    static void indent(GlueCharacters characters, int delta) {
        assert characters != null;

        /*
         * characters.getData()
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
        
        final String currentValue = characters.getData();
        final StringBuilder newValue = new StringBuilder();
        
        if (delta > 0) {
            for (int i = 0, length = currentValue.length(); i < length; i++) {
                final char ch = currentValue.charAt(i);
                newValue.append(ch);
                if (ch == '\n') {
                    for (int n = 0; n < delta; n++) {
                        newValue.append(' ');
                    }
                }
            }
        } else {
            for (int i = 0, length = currentValue.length(); i < length; i++) {
                final char ch = currentValue.charAt(i);
                newValue.append(ch);
                if (ch == '\n') {
                    while ((i+1 < length) 
                            && (currentValue.charAt(i+1) == ' ')
                            && (delta < 0)) {
                        i++;
                        delta++;
                    }
                }
            }
        }
        
        characters.setData(newValue.toString());
    }
    
    /*
     * Private
     */
    
    private void snapshot(GlueElement element) {
        for (GlueAuxiliary f : element.getFront()) {
            snapshot(f);
        }
        for (GlueAuxiliary t : element.getTail()) {
            snapshot(t);
        }
        for (GlueElement child : element.getChildren()) {
            snapshot(child);
        }
    }
    
    private void snapshot(GlueAuxiliary auxiliary) {
        if (auxiliary instanceof GlueCharacters) {
            final GlueCharacters characters = (GlueCharacters) auxiliary;
            textMap.put(characters, characters.getData());
        }
    }
}
