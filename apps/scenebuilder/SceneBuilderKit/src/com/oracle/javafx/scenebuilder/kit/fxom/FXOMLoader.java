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

import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.sun.javafx.fxml.LoadListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javafx.fxml.FXMLLoader;


/**
 *
 * 
 */
class FXOMLoader implements LoadListener {
    
    private final FXOMDocument document;
    private TransientNode currentTransientNode;
    private GlueCursor glueCursor;
    
    /*
     * FXOMLoader
     */
    
    public FXOMLoader(FXOMDocument document) {
        assert document != null;
        assert document.getGlue().getRootElement() != null;
        this.document = document;
    }
    
    public void load(String fxmlText) throws java.io.IOException {
        assert fxmlText != null;
        
        final ClassLoader classLoader;
        if (document.getClassLoader() != null) {
            classLoader = document.getClassLoader();
        } else {
            classLoader = FXMLLoader.getDefaultClassLoader();
        }
        
        FXMLLoader fxmlLoader = new FXMLLoader();
        
        fxmlLoader.setLocation(document.getLocation());
        fxmlLoader.setResources(new ResourceKeyCollector(document.getResources()));
        fxmlLoader.setClassLoader(new TransientClassLoader(classLoader));
        fxmlLoader.setBuilderFactory(new FXOMBuilderFactory(classLoader));
        Deprecation.setStaticLoad(fxmlLoader, true);
        Deprecation.setLoadListener(fxmlLoader, this);
        
        final Charset utf8 = Charset.forName("UTF-8");
        try (final InputStream is = new ByteArrayInputStream(fxmlText.getBytes(utf8))) {
            glueCursor = new GlueCursor(document.getGlue());
            currentTransientNode = null;
            assert is.markSupported();
            is.reset();
            document.setSceneGraphRoot(fxmlLoader.load(is));
//            assert document.isConsistent(); // TODO Eric - Returns true on Preview
        } catch(RuntimeException | IOException x) {
            throw new IOException(x);
        }
    }
    
    public FXOMDocument getDocument() {
        return document;
    }
    
    
    /*
     * LoadListener
     */
    
    @Override
    public void readImportProcessingInstruction(String data) {
    }

    @Override
    public void readLanguageProcessingInstruction(String data) {
    }

    @Override
    public void readComment(String string) {
    }

    @Override
    public void beginInstanceDeclarationElement(Class<?> declaredClass) {
        assert declaredClass != null;
        assert glueCursor.getCurrentElement().getTagName().equals(PropertyName.makeClassFullName(declaredClass)) ||
               glueCursor.getCurrentElement().getTagName().equals(declaredClass.getCanonicalName());
        
        final TransientObject transientInstance
                = new TransientObject(currentTransientNode, 
                declaredClass, glueCursor.getCurrentElement());
        
        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginUnknownTypeElement(String unknownClassName) {
        assert unknownClassName != null;
        assert glueCursor.getCurrentElement().getTagName().equals(unknownClassName);
        
        final TransientObject transientInstance
                = new TransientObject(currentTransientNode, 
                unknownClassName, glueCursor.getCurrentElement());
        
        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginIncludeElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:include");
        
        final TransientIntrinsic transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_INCLUDE, glueCursor.getCurrentElement());
        
        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }
    
    @Override
    public void beginReferenceElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:reference");
        
        final TransientIntrinsic transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_REFERENCE, glueCursor.getCurrentElement());
        
        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginCopyElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:copy");
        
        final TransientIntrinsic transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_COPY, glueCursor.getCurrentElement());
        
        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginRootElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:root");
        
        final TransientObject transientInstance
                = new TransientObject(currentTransientNode, 
                glueCursor.getCurrentElement());
        
        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginPropertyElement(String name, Class<?> staticClass) {
        assert name != null;

        final TransientProperty transientProperty
                = new TransientProperty(currentTransientNode,
                    new PropertyName(name, staticClass), 
                    glueCursor.getCurrentElement());
        
        currentTransientNode = transientProperty;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginUnknownStaticPropertyElement(String string) {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginScriptElement() {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginDefineElement() {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void readInternalAttribute(String attrName, String attrValue) {
        assert currentTransientNode instanceof TransientObject ||
               currentTransientNode instanceof TransientIntrinsic;
        
        if (attrName.equals("type")) {
            assert currentTransientNode instanceof TransientObject;
            final TransientObject transientObject = (TransientObject) currentTransientNode;
            transientObject.setFxRootType(attrValue);
        }
    }

    @Override
    public void readPropertyAttribute(String name, Class<?> staticClass, String fxmlValue) {
        assert currentTransientNode instanceof TransientObject
                || currentTransientNode instanceof TransientIntrinsic
                || currentTransientNode instanceof TransientProperty;
        
        assert name != null;

        final PropertyName pname = new PropertyName(name, staticClass);
        final FXOMPropertyT fxomProperty = new FXOMPropertyT(document, pname, null, null, fxmlValue);

        if (currentTransientNode instanceof TransientObject) {
            final TransientObject transientInstance = (TransientObject) currentTransientNode;
            transientInstance.getProperties().add(fxomProperty);
        } else if (currentTransientNode instanceof TransientProperty) {
            final TransientProperty transientProperty = (TransientProperty) currentTransientNode;
            transientProperty.getCollectedProperties().add(fxomProperty);
        } else {
            // TODO(elp): for now, we ignore properties declared in fx:include.
            // To be implemented later.
        }
    }

    @Override
    public void readUnknownStaticPropertyAttribute(String string, String string1) {
        // TODO(elp) : implement FXOMLoader.readUnknownStaticPropertyAttribute.
    }

    @Override
    public void readEventHandlerAttribute(String name, String hashStatement) {
        // Same as readPropertyAttribute() 
        readPropertyAttribute(name, null, hashStatement);
    }

    @Override
    public void endElement(Object sceneGraphObject) {
        
        currentTransientNode.setSceneGraphObject(sceneGraphObject);
        
        if (currentTransientNode instanceof TransientObject) {
            final TransientObject currentInstance = (TransientObject) currentTransientNode;
            final FXOMObject currentFxomObject = currentInstance.makeFxomObject(document);
            final TransientNode currentParent = currentInstance.getParentNode();
            if (currentParent instanceof TransientProperty) {
                final TransientProperty parentProperty = (TransientProperty) currentParent;
                parentProperty.getValues().add(currentFxomObject);
            } else if (currentParent instanceof TransientObject) {
                final TransientObject parentInstance = (TransientObject) currentParent;
                parentInstance.getCollectedItems().add(currentFxomObject);
            } else if (currentParent instanceof TransientIgnored) {
                // currentObject is an object inside an fx:define section
                // Nothing to do for now
            } else {
                assert currentParent == null;
                document.updateRoots(currentFxomObject, currentFxomObject.getSceneGraphObject());
            }
            
        } else if (currentTransientNode instanceof TransientIntrinsic) {
            final TransientIntrinsic currentIntrinsic = (TransientIntrinsic) currentTransientNode;
            final FXOMIntrinsic currentFxomIntrinsic = currentIntrinsic.makeFxomIntrinsic(document);
            final TransientNode currentParent = currentIntrinsic.getParentNode();
            
            if (currentParent instanceof TransientProperty) {
                final TransientProperty parentProperty = (TransientProperty) currentParent;
                parentProperty.getValues().add(currentFxomIntrinsic);
            } else if (currentParent instanceof TransientObject) {
                final TransientObject parentInstance = (TransientObject) currentParent;
                parentInstance.getCollectedItems().add(currentFxomIntrinsic);
            } else if (currentParent instanceof TransientIgnored) {
                // currentObject is an object inside an fx:define section
                // Nothing to do for now
            } else {
                assert currentParent == null;
                document.updateRoots(currentFxomIntrinsic, currentFxomIntrinsic.getSceneGraphObject());
            }
        } else if (currentTransientNode instanceof TransientProperty) {
            final TransientProperty currentProperty = (TransientProperty) currentTransientNode;
            final TransientNode currentParent = currentProperty.getParentNode();
            final FXOMProperty currentFxomProperty = currentProperty.makeFxomProperty(document);
            assert currentParent instanceof TransientObject;
            final TransientObject parentObject = (TransientObject) currentParent;
            parentObject.getProperties().add(currentFxomProperty);
            // We ignore sceneGraphObject
        } else {
                assert currentTransientNode instanceof TransientIgnored;
                // Nothing to do in this case
        }
        
        currentTransientNode = currentTransientNode.getParentNode();
    }
}
