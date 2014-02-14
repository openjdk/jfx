/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;

/**
 *
 * 
 */
public class EffectPropertyMetadata extends ComplexPropertyMetadata<Effect> {

    public EffectPropertyMetadata(PropertyName name, boolean readWrite, 
            Effect defaultValue, InspectorPath inspectorPath) {
        super(name, Effect.class, readWrite, defaultValue, inspectorPath);
        
    }

    /*
     * ComplexPropertyMetadata
     */

    @Override
    public FXOMInstance makeFxomInstanceFromValue(Effect value, FXOMDocument fxomDocument) {
        final FXOMInstance result;
        
        if (value instanceof Blend) {
            final BlendPropertyMetadata subclassMetadata 
                    = new BlendPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Blend) value, fxomDocument);
        } else if (value instanceof Bloom) {
            final BloomPropertyMetadata subclassMetadata 
                    = new BloomPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Bloom) value, fxomDocument);
        } else if (value instanceof BoxBlur) {
            final BoxBlurPropertyMetadata subclassMetadata 
                    = new BoxBlurPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((BoxBlur) value, fxomDocument);
        } else if (value instanceof ColorAdjust) {
            final ColorAdjustPropertyMetadata subclassMetadata 
                    = new ColorAdjustPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((ColorAdjust) value, fxomDocument);
        } else if (value instanceof ColorInput) {
            final ColorInputPropertyMetadata subclassMetadata 
                    = new ColorInputPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((ColorInput) value, fxomDocument);
        } else if (value instanceof DisplacementMap) {
            final DisplacementMapPropertyMetadata subclassMetadata 
                    = new DisplacementMapPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((DisplacementMap) value, fxomDocument);
        } else if (value instanceof DropShadow) {
            final DropShadowPropertyMetadata subclassMetadata 
                    = new DropShadowPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((DropShadow) value, fxomDocument);
        } else if (value instanceof GaussianBlur) {
            final GaussianBlurPropertyMetadata subclassMetadata 
                    = new GaussianBlurPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((GaussianBlur) value, fxomDocument);
        } else if (value instanceof Glow) {
            final GlowPropertyMetadata subclassMetadata 
                    = new GlowPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Glow) value, fxomDocument);
        } else if (value instanceof ImageInput) {
            final ImageInputPropertyMetadata subclassMetadata 
                    = new ImageInputPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((ImageInput) value, fxomDocument);
        } else if (value instanceof InnerShadow) {
            final InnerShadowPropertyMetadata subclassMetadata 
                    = new InnerShadowPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((InnerShadow) value, fxomDocument);
        } else if (value instanceof Lighting) {
            final LightingPropertyMetadata subclassMetadata 
                    = new LightingPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Lighting) value, fxomDocument);
        } else if (value instanceof MotionBlur) {
            final MotionBlurPropertyMetadata subclassMetadata 
                    = new MotionBlurPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((MotionBlur) value, fxomDocument);
        } else if (value instanceof PerspectiveTransform) {
            final PerspectiveTransformPropertyMetadata subclassMetadata 
                    = new PerspectiveTransformPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((PerspectiveTransform) value, fxomDocument);
        } else if (value instanceof Reflection) {
            final ReflectionPropertyMetadata subclassMetadata 
                    = new ReflectionPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Reflection) value, fxomDocument);
        } else if (value instanceof SepiaTone) {
            final SepiaTonePropertyMetadata subclassMetadata 
                    = new SepiaTonePropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((SepiaTone) value, fxomDocument);
        } else if (value instanceof Shadow) {
            final ShadowPropertyMetadata subclassMetadata 
                    = new ShadowPropertyMetadata(getName(), isReadWrite(), null, getInspectorPath());
            result = subclassMetadata.makeFxomInstanceFromValue((Shadow) value, fxomDocument);
        } else {
            assert false : "unexpected effect class = " + value.getClass().getSimpleName(); //NOI18N
            result = null;
        }
        
        return result;
    }
    
}
