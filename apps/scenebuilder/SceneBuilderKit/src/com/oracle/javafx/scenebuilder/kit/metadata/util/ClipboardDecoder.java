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

package com.oracle.javafx.scenebuilder.kit.metadata.util;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMArchive;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ImagePropertyMetadata;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

/**
 *
 */
public class ClipboardDecoder {
    
    final Clipboard clipboard;

    public ClipboardDecoder(Clipboard clipboard) {
        this.clipboard = clipboard;
    }
    
    public List<FXOMObject> decode(FXOMDocument targetDocument) {
        assert targetDocument != null;
        
        List<FXOMObject> result = null;
        
        // SB_DATA_FORMAT
        if (clipboard.hasContent(ClipboardEncoder.SB_DATA_FORMAT)) {
            final Object content = clipboard.getContent(ClipboardEncoder.SB_DATA_FORMAT);
            if (content instanceof FXOMArchive) {
                final FXOMArchive archive = (FXOMArchive) content;
                try {
                    result = archive.decode(targetDocument);
                } catch(IOException x) {
                    result = null;
                }
            }
        }
        
        // FXML_DATA_FORMAT
        if ((result == null) 
                && clipboard.hasContent(ClipboardEncoder.FXML_DATA_FORMAT)) {
            final Object content = clipboard.getContent(ClipboardEncoder.FXML_DATA_FORMAT);
            if (content instanceof String) {
                final String fxmlText = (String) content;
                try {
                    final URL location = targetDocument.getLocation();
                    final ClassLoader classLoader = targetDocument.getClassLoader();
                    final ResourceBundle resources = targetDocument.getResources();
                    final FXOMDocument transientDoc
                            = new FXOMDocument(fxmlText, location, classLoader, resources);
                    result = Arrays.asList(transientDoc.getFxomRoot());
                } catch(IOException x) {
                    result = null;
                }
            }
        }
        
        // DataFormat.IMAGE
        if ((result == null) 
                && clipboard.hasContent(DataFormat.IMAGE)) {
            final Object content = clipboard.getContent(DataFormat.IMAGE);
            if (content instanceof Image) {
                final Image image = (Image) content;
                try {
                    final FXOMDocument transientDoc = 
                            makeFxomDocumentFromImageURL(image, 200.0);
                    result = Arrays.asList(transientDoc.getFxomRoot());
                } catch(IOException x) {
                    result = null;
                }
            }
        }
        
        // If nothing is exploitable, we return a list.
        if (result == null) {
            result = Collections.emptyList();
        }
        
        return result;
    }
    
    /*
     * Utilities that should probably go somewhere else.
     */
    
    static FXOMDocument makeFxomDocumentFromImageURL(Image image, 
            double fitSize) throws IOException {

        assert image != null;
        assert fitSize > 0.0;
        
        final double imageWidth = image.getWidth();
        final double imageHeight = image.getHeight();
        
        final double fitWidth, fitHeight;
        final double imageSize = Math.max(imageWidth, imageHeight);
        if (imageSize < fitSize) {
            fitWidth = 0;
            fitHeight = 0;
        } else {
            final double widthScale  = fitSize / imageSize;
            final double heightScale = fitSize / imageHeight;
            final double scale = Math.min(widthScale, heightScale);
            fitWidth = Math.floor(imageWidth * scale);
            fitHeight = Math.floor(imageHeight * scale);
        }
        
        return makeFxomDocumentFromImageURL(image, fitWidth, fitHeight);
    }
    
    static final PropertyName imageName = new PropertyName("image"); //NOI18N
    static final PropertyName fitWidthName = new PropertyName("fitWidth"); //NOI18N
    static final PropertyName fitHeightName = new PropertyName("fitHeight"); //NOI18N
    
    static FXOMDocument makeFxomDocumentFromImageURL(Image image, double fitWidth, double fitHeight) {
        final FXOMDocument result = new FXOMDocument();
        final FXOMInstance imageView = new FXOMInstance(result, ImageView.class);
        
        final ComponentClassMetadata imageViewMeta 
                = Metadata.getMetadata().queryComponentMetadata(ImageView.class);
        final PropertyMetadata imagePropMeta
                = imageViewMeta.lookupProperty(imageName);
        final PropertyMetadata fitWidthPropMeta
                = imageViewMeta.lookupProperty(fitWidthName);
        final PropertyMetadata fitHeightPropMeta
                = imageViewMeta.lookupProperty(fitHeightName);
        
        assert imagePropMeta instanceof ImagePropertyMetadata;
        assert fitWidthPropMeta instanceof DoublePropertyMetadata;
        assert fitHeightPropMeta instanceof DoublePropertyMetadata;
        
        final ImagePropertyMetadata imageMeta
                = (ImagePropertyMetadata) imagePropMeta;
        final DoublePropertyMetadata fitWidthMeta
                = (DoublePropertyMetadata) fitWidthPropMeta;
        final DoublePropertyMetadata fitHeightMeta
                = (DoublePropertyMetadata) fitHeightPropMeta;

        imageMeta.setValue(imageView, image);
        fitWidthMeta.setValue(imageView, fitWidth);
        fitHeightMeta.setValue(imageView, fitHeight);
        
        result.setFxomRoot(imageView);
        
        return result;
    }
}
