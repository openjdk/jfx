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
package com.oracle.javafx.scenebuilder.kit.editor.report;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNode;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;

/**
 *
 * 
 */
public class ErrorReportEntry {
    
    public enum Type {
        UNRESOLVED_CLASS,
        UNRESOLVED_LOCATION,
        UNRESOLVED_RESOURCE,
        INVALID_CSS_CONTENT,
        UNSUPPORTED_EXPRESSION
    }
    
    private final FXOMNode fxomNode;
    private final Type type;
    private final CSSParsingReport cssParsingReport; // relevant for INVALID_CSS_CONTENT
    
    public ErrorReportEntry(FXOMNode fxomNode, Type type, CSSParsingReport cssParsingReport) {
        assert fxomNode != null;
        assert (type == Type.INVALID_CSS_CONTENT) == (cssParsingReport != null);
        
        this.fxomNode = fxomNode;
        this.type = type;
        this.cssParsingReport = cssParsingReport;
    }

    public ErrorReportEntry(FXOMNode fxomNode, Type type) {
        this(fxomNode, type, null);
    }

    public FXOMNode getFxomNode() {
        return fxomNode;
    }

    public Type getType() {
        return type;
    }

    public CSSParsingReport getCssParsingReport() {
        return cssParsingReport;
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        
        result.append(getClass().getSimpleName());
        result.append("(fxomNode="); //NOI18N
        result.append(fxomNode.getClass().getSimpleName());
        result.append(",type="); //NOI18N
        result.append(type.toString());
        switch(type) {
            case UNRESOLVED_CLASS:
                break;
            case UNRESOLVED_LOCATION:
                result.append(",location="); //NOI18N
                break;
            case UNRESOLVED_RESOURCE:
                result.append(",resource="); //NOI18N
                break;
            case INVALID_CSS_CONTENT:
                result.append(",css file="); //NOI18N
                break;
            case UNSUPPORTED_EXPRESSION:
                result.append(",expression="); //NOI18N
                break;
        }
        if (fxomNode instanceof FXOMPropertyT) {
            final FXOMPropertyT fxomProperty = (FXOMPropertyT) fxomNode;
            result.append(fxomProperty.getValue());
        } else if (fxomNode instanceof FXOMIntrinsic) {
            final FXOMIntrinsic fxomIntrinsic = (FXOMIntrinsic) fxomNode;
            result.append(fxomIntrinsic.getSource());
        } else {
            result.append("?"); //NOI18N
        }
        result.append(")"); //NOI18N
        
        return result.toString();
    }
    
}
