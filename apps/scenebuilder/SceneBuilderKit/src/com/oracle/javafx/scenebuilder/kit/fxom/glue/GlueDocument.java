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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class GlueDocument extends GlueNode {
    
    private GlueElement rootElement;
    private final List<GlueAuxiliary> header = new ArrayList<>();
    
    public GlueDocument() {
    }
    
    public GlueDocument(String xmlText) throws IOException {
        assert xmlText != null;
        if (isEmptyXmlText(xmlText) == false) {
            final GlueLoader loader = new GlueLoader(this);
            loader.load(xmlText);
            adjustRootElementIndentation();
        }
    }
    
    public GlueElement getRootElement() {
        return rootElement;
    }

    public void setRootElement(GlueElement newRootElement) {
        if ((newRootElement != null) && (newRootElement.getParent() != null)) {
            newRootElement.removeFromParent();
        }
        this.rootElement = newRootElement;
    }

    public List<GlueAuxiliary> getHeader() {
        return header;
    }
    
    public void updateIndent() {
        if (rootElement != null) {
            rootElement.updateIndent(0);
        }
    }
    
    
    /*
     * Utilities
     */

    public List<GlueInstruction> collectInstructions(String target) {
        final List<GlueInstruction> result = new ArrayList<>();
        
        assert target != null;
        
        for (GlueAuxiliary auxiliary : header) {
            if (auxiliary instanceof GlueInstruction) {
                final GlueInstruction i = (GlueInstruction) auxiliary;
                if (target.equals(i.getTarget())) {
                    result.add(i);
                }
            }
        }
        
        return result;
    }
    
    public static boolean isEmptyXmlText(String xmlText) {
        assert xmlText != null;
        return xmlText.trim().isEmpty();
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        final String result;
        if (rootElement == null) {
            result = ""; //NOI18N
        } else {
            final GlueSerializer serializer = new GlueSerializer(this);
            result = serializer.toString();
        }
        return result;
    }
    
    
    /*
     * Private
     */
    
    private void adjustRootElementIndentation() {
        /*
         * By default, if a root element is empty and expressed like this:
         *     <AnchorPane />
         * indentation logic would keep the upcoming children on the same line:
         *     <AnchorPane> <children> <Button/> </children> </AnchorPane>.
         * 
         * With the adjustment below, indentation logic will produce:
         *     <AnchorPane> 
         *        <children>
         *           <Button />
         *        </children>
         *     </AnchorPane>
         */
        
        if ((rootElement != null) && rootElement.getChildren().isEmpty()) {
            if (rootElement.getFront().isEmpty()) {
                rootElement.getFront().add(new GlueCharacters(this, GlueCharacters.Type.TEXT, "\n")); //NOI18N
            }
            if (rootElement.getTail().isEmpty()) {
                rootElement.getTail().add(new GlueCharacters(this, GlueCharacters.Type.TEXT, "\n")); //NOI18N
            }
        }
    }
}
