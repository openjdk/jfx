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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * 
 */
public class GlueElement extends GlueNode {
    
    private final static int INDENT_STEP = 3;
    
    private GlueDocument document;
    private String tagName;
    private final List<GlueElement> children = new ArrayList<>();

    private final Map<String, String> attributes = new HashMap<>();
    private final List<GlueAuxiliary> front = new ArrayList<>();
    private final List<GlueAuxiliary> tail = new ArrayList<>();
    private final List<GlueAuxiliary> content = new ArrayList<>();
    private int indentDepth;
    
    private GlueElement parent;
    
    public GlueElement(GlueDocument document, String tagName) {
        this(document, tagName, 0, true /* preset */);
    }

    public GlueElement(GlueDocument document, String tagName, int indentDepth, boolean preset) {
        assert document != null;
        assert tagName != null;
        
        this.document = document;
        this.tagName = tagName;
        this.indentDepth = indentDepth;
        if (preset) {
            front.add(new GlueCharacters(document, GlueCharacters.Type.TEXT, "\n")); //NOI18N
            tail.add(new GlueCharacters(document, GlueCharacters.Type.TEXT, "\n")); //NOI18N
        }
    }

    public GlueElement(GlueDocument document, String tagName, GlueElement template) {
        this(document, tagName, template.indentDepth, false);
        
        final int templateIndent = template.guessIndent();
        if (templateIndent != -1) {
            front.add(makeIndentCharacters(templateIndent));
            tail.add(makeIndentCharacters(templateIndent));
        }
    }

    public GlueElement getParent() {
        return parent;
    }

    public GlueDocument getDocument() {
        return document;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        assert tagName != null;
        assert tagName.isEmpty() == false;
        this.tagName = tagName;
    }

    public List<GlueElement> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    public void addToParent(GlueElement newParent) {
        addToParent(-1, newParent);
    }
    
    public void addToParent(int index, GlueElement newParent) {
        assert newParent != null;
        assert newParent != parent;
        assert newParent.getDocument() == document;
        assert -1 <= index;
        assert index <= newParent.children.size();
        
        if (parent != null) {
            this.removeFromParent();
        } else if (this == document.getRootElement()) {
            document.setRootElement(null);
        }
        
        if (index == -1) {
            index = newParent.children.size();
        }
        newParent.children.add(index, this);
        this.parent = newParent;
    }
    
    public void addBefore(GlueElement nextSibling) {
        assert nextSibling != null;
        assert nextSibling.getDocument() == document;
        assert nextSibling.getParent() != null;
        
        final GlueElement siblingParent = nextSibling.getParent();
        final int nextSiblingIndex = siblingParent.getChildren().indexOf(nextSibling);
        assert nextSiblingIndex != -1;
        
        addToParent(nextSiblingIndex, siblingParent);
    }
    
    
    public void removeFromParent() {
        assert parent != null;
        assert parent.children.contains(this);
        
        parent.children.remove(this);
        parent = null;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<GlueAuxiliary> getFront() {
        return front;
    }

    public List<GlueAuxiliary> getTail() {
        return tail;
    }

    public List<GlueAuxiliary> getContent() {
        return content;
    }

    public int getDepth() {
        int result = 0;
        GlueElement ancestor = parent;
        
        while (ancestor != null) {
            result++;
            ancestor = ancestor.getParent();
        }
        
        return result;
    }

    public void updateIndent(int depth) {
        if (indentDepth != depth) {
            final int indentDelta = (depth - indentDepth) * INDENT_STEP;
            if (front.isEmpty()) {
                front.add(makeIndentCharacters(depth * INDENT_STEP));
            } else {
                for (GlueAuxiliary auxiliary : front) {
                    if (auxiliary instanceof GlueCharacters) {
                        final GlueCharacters characters = (GlueCharacters) auxiliary;
                        characters.adjustIndentBy(indentDelta);
                    }
                }
            }
            
            if (tail.isEmpty()) {
                tail.add(makeIndentCharacters(depth * INDENT_STEP));
            } else {
                for (GlueAuxiliary auxiliary : tail) {
                    if (auxiliary instanceof GlueCharacters) {
                        final GlueCharacters characters = (GlueCharacters) auxiliary;
                        characters.adjustIndentBy(indentDelta);
                    }
                }
            }
            indentDepth = depth;
        }
        
        for (GlueElement child : children) {
            child.updateIndent(depth+1);
        }
    }

    public String getContentText() {
        final GlueCharacters contentHolder = getContentHolder();
        final String result;
        
        if (contentHolder == null) {
            result = null;
        } else {
            result = contentHolder.getData();
        }
        
        return result;
    }
    
    
    public void setContentText(String text) {
        final String currentText = getContentText();
        
        if (Objects.equals(currentText, text) == false) {
            GlueCharacters contentHolder = getContentHolder();
            
            if (text == null) {
                assert currentText != null;
                assert contentHolder != null;
                content.remove(contentHolder);
            } else {
                if (contentHolder == null) {
                    contentHolder = new GlueCharacters(document, GlueCharacters.Type.TEXT, text);
                    content.add(contentHolder);
                } else {
                    contentHolder.setData(text);
                }
            }
        }
        
        assert Objects.equals(getContentText(), text);
    }
    
    
    public void moveToDocument(GlueDocument targetDocument) {
        
        assert targetDocument != null;
        assert targetDocument != document;
        
        if (this == document.getRootElement()) {
            // This element is the root of the document
            assert parent == null;
            document.setRootElement(null);
        } 
        
        if ((parent != null) && (parent.getDocument() != targetDocument)) {
            removeFromParent();
        }
        
        document = targetDocument;
        
        for (GlueElement child : children) {
            child.moveToDocument(targetDocument);
        }
        
        assert document == targetDocument;
        assert this != targetDocument.getRootElement();
    }

    
    /*
     * Shortcut
     */
    
    public GlueElement getNextSibling() {
        final GlueElement result;
        
        if (parent == null) {
            result = null;
        } else {
            int index = parent.children.indexOf(this);
            assert index != -1;
            if (index+1 < parent.children.size()) {
                result = parent.children.get(index+1);
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        
        result.append(getClass().getSimpleName());
        result.append(" - "); //NOI18N
        result.append(tagName);
        
        return result.toString();
    }
    
    
    /*
     * Private
     */
    
    private GlueCharacters getContentHolder() {
        /*
         * The content holder is the last TEXT node of the "content". //NOI18N
         * When "content" contains more than one node, it will be the one //NOI18N
         * which is used to get/set the content text.
         * This matches FXMLLoader behavior.
         * 
         * For example, with the following FXML:
         * 
         * <Label>
         *     <text>Hello <!-- Disruptive comment -->World!</text>
         * </Label>
         * 
         * FXMLLoader will produce a Label with text="World!". //NOI18N
         * 
         * <text>                               -> GlueElement
         *      "Hello "                        -> GlueCharacters //NOI18N
         *      " Disruptive comment "          -> GlueCharacters //NOI18N
         *      "World !"                       -> GlueCharacters (HOLDER) //NOI18N
         */
        
        GlueCharacters result = null;
        for (int i = content.size()-1; (i >= 0) && (result == null); i--) {
            final GlueAuxiliary auxiliary = content.get(i);
            if (auxiliary instanceof GlueCharacters) {
                final GlueCharacters c = (GlueCharacters) auxiliary;
                if (c.getType() == GlueCharacters.Type.TEXT) {
                    result = c;
                }
            }
        }
        
        return result;
    }
    
    
    private int guessIndent() {
        final int result;
        
        if (front.isEmpty()) {
            result = -1;
        } else if (front.get(0) instanceof GlueCharacters) {
            final GlueCharacters characters = (GlueCharacters) front.get(0);
            result = characters.guessIndent();
        } else {
            result = -1;
        }
        
        return result;
    }
    
    private GlueCharacters makeIndentCharacters(int indentSize) {
        final StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (int i = 0; i < indentSize; i++) {
            sb.append(' ');
        }
        
        return new GlueCharacters(getDocument(), GlueCharacters.Type.TEXT, sb.toString());
    }
}
