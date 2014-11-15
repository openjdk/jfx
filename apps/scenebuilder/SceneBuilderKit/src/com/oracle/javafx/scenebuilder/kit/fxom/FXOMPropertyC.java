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
package com.oracle.javafx.scenebuilder.kit.fxom;

import com.oracle.javafx.scenebuilder.kit.fxom.glue.GlueElement;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * 
 */
public class FXOMPropertyC extends FXOMProperty {
    
    private final List<FXOMObject> values = new ArrayList<>();
    private final GlueElement glueElement;

    public FXOMPropertyC(
            FXOMDocument document, 
            PropertyName name,
            List<FXOMObject> values,
            GlueElement glueElement) {
        super(document, name);
        
        assert values != null;
        assert values.isEmpty() == false;
        assert glueElement != null;
        assert glueElement.getTagName().equals(getName().toString());
        
        this.glueElement = glueElement;

        // Adds values to this property.
        // Note we don't use addValue() because
        // here Glue is already up to date.
        for (FXOMObject v : values) {
            this.values.add(v);
            v.setParentProperty(this);
        }
    }
    
    
    public FXOMPropertyC(FXOMDocument document, PropertyName name) {
        super(document, name);
        
        this.glueElement = new GlueElement(document.getGlue(), name.toString());
    }
    
    
    public FXOMPropertyC(FXOMDocument document, PropertyName name, FXOMObject value) {
        super(document, name);
        
        assert value != null;
        
        this.glueElement = new GlueElement(document.getGlue(), name.toString());
        value.addToParentProperty(-1, this);
    }

    public FXOMPropertyC(FXOMDocument document, PropertyName name, List<FXOMObject> values) {
        super(document, name);
        
        assert values != null;
        assert values.isEmpty() == false;
        
        this.glueElement = new GlueElement(document.getGlue(), name.toString());
        for (FXOMObject value : values) {
            value.addToParentProperty(-1, this);
        }
    }

    public List<FXOMObject> getValues() {
        return Collections.unmodifiableList(values);
    }

    public GlueElement getGlueElement() {
        return glueElement;
    }
    
    
    
    /*
     * FXOMProperty
     */
    
    @Override
    public void addToParentInstance(int index, FXOMInstance newParentInstance) {
        
        if (getParentInstance() != null) {
            removeFromParentInstance();
        }
        
        setParentInstance(newParentInstance);
        newParentInstance.addProperty(this);
        
        final GlueElement newParentElement = newParentInstance.getGlueElement();
        glueElement.addToParent(index, newParentElement);
    }

    @Override
    public void removeFromParentInstance() {
        
        assert getParentInstance() != null;
        
        final FXOMInstance currentParentInstance = getParentInstance();
        
        assert glueElement.getParent() == currentParentInstance.getGlueElement();
        glueElement.removeFromParent();
        
        setParentInstance(null);
        currentParentInstance.removeProperty(this);
    }
 
    @Override
    public int getIndexInParentInstance() {
        final int result;
        
        if (getParentInstance() == null) {
            result = -1;
        } else {
            final GlueElement parentElement = getParentInstance().getGlueElement();
            result = parentElement.getChildren().indexOf(glueElement);
            assert result != -1;
        }
        
        return result;
    }
    
    
    /*
     * FXOMNode
     */
    
    @Override
    public void moveToFxomDocument(FXOMDocument destination) {
        assert destination != null;
        assert destination != getFxomDocument();
        
        documentLocationWillChange(destination.getLocation());
        
        if (getParentInstance() != null) {
            assert getParentInstance().getFxomDocument() == getFxomDocument();
            removeFromParentInstance();
        }
        
        assert getParentInstance() == null;
        assert glueElement.getParent() == null;
        
        glueElement.moveToDocument(destination.getGlue());
        changeFxomDocument(destination);
    }

    
    @Override
    protected void changeFxomDocument(FXOMDocument destination) {
        assert destination != null;
        assert destination != getFxomDocument();
        assert destination.getGlue() == glueElement.getDocument();
        
        super.changeFxomDocument(destination);
        for (FXOMObject v : values) {
            v.changeFxomDocument(destination);
        }
    }

    @Override
    public void documentLocationWillChange(URL newLocation) {
        for (FXOMObject v : values) {
            v.documentLocationWillChange(newLocation);
        }
    }
    
  
    /*
     * Package
     */
    
    /* Reserved to FXOMObject.addToParentProperty() private use */
    void addValue(int index, FXOMObject value) {
        assert value != null;
        assert value.getParentProperty() == this;
        assert values.contains(value) == false;
        if (index == -1) {
            values.add(value);
        } else {
            values.add(index, value);
        }
    }
    
    /* Reserved to FXOMObject.removeFromParentProperty() private use */
    void removeValue(FXOMObject value) {
        assert value != null;
        assert value.getParentProperty() == null;
        assert values.contains(value);
        values.remove(value);
    }
}
