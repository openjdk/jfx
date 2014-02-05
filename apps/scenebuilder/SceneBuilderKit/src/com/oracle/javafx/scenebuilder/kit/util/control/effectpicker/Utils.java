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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker;

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

public abstract class Utils {

    public static final Effect newInstance(Class<? extends Effect> clazz) {
        assert clazz != null;
        return newInstance(clazz.getSimpleName());
    }

    static final Effect newInstance(String text) {
        assert text != null;
        assert text.isEmpty() == false;
        if (text.equals(Blend.class.getSimpleName())) {
            return new Blend();
        } else if (text.equals(Bloom.class.getSimpleName())) {
            return new Bloom();
        } else if (text.equals(BoxBlur.class.getSimpleName())) {
            return new BoxBlur();
        } else if (text.equals(ColorAdjust.class.getSimpleName())) {
            return new ColorAdjust();
        } else if (text.equals(ColorInput.class.getSimpleName())) {
            return new ColorInput();
        } else if (text.equals(DisplacementMap.class.getSimpleName())) {
            return new DisplacementMap();
        } else if (text.equals(DropShadow.class.getSimpleName())) {
            return new DropShadow();
        } else if (text.equals(GaussianBlur.class.getSimpleName())) {
            return new GaussianBlur();
        } else if (text.equals(Glow.class.getSimpleName())) {
            return new Glow();
        } else if (text.equals(ImageInput.class.getSimpleName())) {
            return new ImageInput();
        } else if (text.equals(InnerShadow.class.getSimpleName())) {
            return new InnerShadow();
        } else if (text.equals(Lighting.class.getSimpleName())) {
            return new Lighting();
        } else if (text.equals(MotionBlur.class.getSimpleName())) {
            return new MotionBlur();
        } else if (text.equals(PerspectiveTransform.class.getSimpleName())) {
            return new PerspectiveTransform();
        } else if (text.equals(Reflection.class.getSimpleName())) {
            return new Reflection();
        } else if (text.equals(SepiaTone.class.getSimpleName())) {
            return new SepiaTone();
        } else if (text.equals(Shadow.class.getSimpleName())) {
            return new Shadow();
        } else {
            assert false;
            return null;
        }
    }

    public static Effect clone(Effect effect) {
        final Effect clone;
        if (effect == null) {
            clone = null;
        } else if (effect instanceof Blend) {
            final Blend blend = (Blend) effect;
            clone = new Blend(blend.getMode());
            ((Blend) clone).setOpacity(blend.getOpacity());
            ((Blend) clone).setBottomInput(clone(blend.getBottomInput()));
            ((Blend) clone).setTopInput(clone(blend.getTopInput()));
        } else if (effect instanceof Bloom) {
            final Bloom bloom = (Bloom) effect;
            clone = new Bloom(bloom.getThreshold());
            ((Bloom) clone).setInput(clone(bloom.getInput()));
        } else if (effect instanceof BoxBlur) {
            final BoxBlur boxBlur = (BoxBlur) effect;
            clone = new BoxBlur(
                    boxBlur.getWidth(),
                    boxBlur.getHeight(),
                    boxBlur.getIterations());
            ((BoxBlur) clone).setInput(clone(boxBlur.getInput()));
        } else if (effect instanceof ColorAdjust) {
            final ColorAdjust colorAdjust = (ColorAdjust) effect;
            clone = new ColorAdjust(
                    colorAdjust.getHue(),
                    colorAdjust.getSaturation(),
                    colorAdjust.getBrightness(),
                    colorAdjust.getContrast());
            ((ColorAdjust) clone).setInput(clone(colorAdjust.getInput()));
        } else if (effect instanceof ColorInput) {
            final ColorInput colorInput = (ColorInput) effect;
            clone = new ColorInput(
                    colorInput.getX(),
                    colorInput.getY(),
                    colorInput.getWidth(),
                    colorInput.getHeight(),
                    colorInput.getPaint());
        } else if (effect instanceof DisplacementMap) {
            final DisplacementMap displacementMap = (DisplacementMap) effect;
            clone = new DisplacementMap(
                    displacementMap.getMapData(),
                    displacementMap.getOffsetX(),
                    displacementMap.getOffsetY(),
                    displacementMap.getScaleX(),
                    displacementMap.getScaleY());
            ((DisplacementMap) clone).setWrap(displacementMap.isWrap());
            ((DisplacementMap) clone).setInput(clone(displacementMap.getInput()));
        } else if (effect instanceof DropShadow) {
            final DropShadow dropShadow = (DropShadow) effect;
            clone = new DropShadow(
                    dropShadow.getBlurType(),
                    dropShadow.getColor(),
                    dropShadow.getRadius(),
                    dropShadow.getSpread(),
                    dropShadow.getOffsetX(),
                    dropShadow.getOffsetY());
            ((DropShadow) clone).setHeight(dropShadow.getHeight());
            ((DropShadow) clone).setWidth(dropShadow.getWidth());
            ((DropShadow) clone).setInput(clone(dropShadow.getInput()));
        } else if (effect instanceof GaussianBlur) {
            final GaussianBlur gaussianBlur = (GaussianBlur) effect;
            clone = new GaussianBlur(gaussianBlur.getRadius());
            ((GaussianBlur) clone).setInput(clone(gaussianBlur.getInput()));
        } else if (effect instanceof Glow) {
            final Glow glow = (Glow) effect;
            clone = new Glow(glow.getLevel());
            ((Glow) clone).setInput(clone(glow.getInput()));
        } else if (effect instanceof ImageInput) {
            final ImageInput imageInput = (ImageInput) effect;
            clone = new ImageInput(
                    imageInput.getSource(),
                    imageInput.getX(),
                    imageInput.getY());
        } else if (effect instanceof InnerShadow) {
            final InnerShadow innerShadow = (InnerShadow) effect;
            clone = new InnerShadow(
                    innerShadow.getBlurType(),
                    innerShadow.getColor(),
                    innerShadow.getRadius(),
                    innerShadow.getChoke(),
                    innerShadow.getOffsetX(),
                    innerShadow.getOffsetY());
            ((InnerShadow) clone).setHeight(innerShadow.getHeight());
            ((InnerShadow) clone).setWidth(innerShadow.getWidth());
            ((InnerShadow) clone).setInput(clone(innerShadow.getInput()));
        } else if (effect instanceof Lighting) {
            final Lighting lighting = (Lighting) effect;
            clone = new Lighting(lighting.getLight());
            ((Lighting) clone).setDiffuseConstant(lighting.getDiffuseConstant());
            ((Lighting) clone).setSpecularConstant(lighting.getSpecularConstant());
            ((Lighting) clone).setSpecularExponent(lighting.getSpecularExponent());
            ((Lighting) clone).setSurfaceScale(lighting.getSurfaceScale());
            ((Lighting) clone).setBumpInput(clone(lighting.getBumpInput()));
            ((Lighting) clone).setContentInput(clone(lighting.getContentInput()));
        } else if (effect instanceof MotionBlur) {
            final MotionBlur motionBlur = (MotionBlur) effect;
            clone = new MotionBlur(
                    motionBlur.getAngle(),
                    motionBlur.getRadius());
            ((MotionBlur) clone).setInput(clone(motionBlur.getInput()));
        } else if (effect instanceof PerspectiveTransform) {
            final PerspectiveTransform perspectiveTransform = (PerspectiveTransform) effect;
            clone = new PerspectiveTransform(
                    perspectiveTransform.getUlx(),
                    perspectiveTransform.getUly(),
                    perspectiveTransform.getUrx(),
                    perspectiveTransform.getUry(),
                    perspectiveTransform.getLrx(),
                    perspectiveTransform.getLry(),
                    perspectiveTransform.getLlx(),
                    perspectiveTransform.getLly());
            ((PerspectiveTransform) clone).setInput(clone(perspectiveTransform.getInput()));
        } else if (effect instanceof Reflection) {
            final Reflection reflection = (Reflection) effect;
            clone = new Reflection(
                    reflection.getTopOffset(),
                    reflection.getFraction(),
                    reflection.getTopOpacity(),
                    reflection.getBottomOpacity());
            ((Reflection) clone).setInput(clone(reflection.getInput()));
        } else if (effect instanceof SepiaTone) {
            final SepiaTone sepiaTone = (SepiaTone) effect;
            clone = new SepiaTone(sepiaTone.getLevel());
            ((SepiaTone) clone).setInput(clone(sepiaTone.getInput()));
        } else if (effect instanceof Shadow) {
            final Shadow shadow = (Shadow) effect;
            clone = new Shadow(
                    shadow.getBlurType(),
                    shadow.getColor(),
                    shadow.getRadius());
            ((Shadow) clone).setHeight(shadow.getHeight());
            ((Shadow) clone).setWidth(shadow.getWidth());
            ((Shadow) clone).setInput(clone(shadow.getInput()));
        } else {
            assert false;
            clone = null;
        }
        return clone;
    }

    public static Effect getDefaultInput(Effect effect) {
        final Effect input;
        assert effect != null;
        if (effect instanceof Blend) {
            input = ((Blend) effect).getTopInput();
        } else if (effect instanceof Bloom) {
            input = ((Bloom) effect).getInput();
        } else if (effect instanceof BoxBlur) {
            input = ((BoxBlur) effect).getInput();
        } else if (effect instanceof ColorAdjust) {
            input = ((ColorAdjust) effect).getInput();
        } else if (effect instanceof ColorInput) {
            // No input
            input = null;
        } else if (effect instanceof DisplacementMap) {
            input = ((DisplacementMap) effect).getInput();
        } else if (effect instanceof DropShadow) {
            input = ((DropShadow) effect).getInput();
        } else if (effect instanceof GaussianBlur) {
            input = ((GaussianBlur) effect).getInput();
        } else if (effect instanceof Glow) {
            input = ((Glow) effect).getInput();
        } else if (effect instanceof ImageInput) {
            // No input
            input = null;
        } else if (effect instanceof InnerShadow) {
            input = ((InnerShadow) effect).getInput();
        } else if (effect instanceof Lighting) {
            input = ((Lighting) effect).getBumpInput();
        } else if (effect instanceof MotionBlur) {
            input = ((MotionBlur) effect).getInput();
        } else if (effect instanceof PerspectiveTransform) {
            input = ((PerspectiveTransform) effect).getInput();
        } else if (effect instanceof Reflection) {
            input = ((Reflection) effect).getInput();
        } else if (effect instanceof SepiaTone) {
            input = ((SepiaTone) effect).getInput();
        } else if (effect instanceof Shadow) {
            input = ((Shadow) effect).getInput();
        } else {
            assert false;
            input = null;
        }
        return input;
    }

    static void setDefaultInput(Effect effect, Effect input) {
        assert effect != null;
        if (effect instanceof Blend) {
            ((Blend) effect).setTopInput(input);
        } else if (effect instanceof Bloom) {
            ((Bloom) effect).setInput(input);
        } else if (effect instanceof BoxBlur) {
            ((BoxBlur) effect).setInput(input);
        } else if (effect instanceof ColorAdjust) {
            ((ColorAdjust) effect).setInput(input);
        } else if (effect instanceof ColorInput) {
            // No input
        } else if (effect instanceof DisplacementMap) {
            ((DisplacementMap) effect).setInput(input);
        } else if (effect instanceof DropShadow) {
            ((DropShadow) effect).setInput(input);
        } else if (effect instanceof GaussianBlur) {
            ((GaussianBlur) effect).setInput(input);
        } else if (effect instanceof Glow) {
            ((Glow) effect).setInput(input);
        } else if (effect instanceof ImageInput) {
            // No input
        } else if (effect instanceof InnerShadow) {
            ((InnerShadow) effect).setInput(input);
        } else if (effect instanceof Lighting) {
            ((Lighting) effect).setBumpInput(input);
        } else if (effect instanceof MotionBlur) {
            ((MotionBlur) effect).setInput(input);
        } else if (effect instanceof PerspectiveTransform) {
            ((PerspectiveTransform) effect).setInput(input);
        } else if (effect instanceof Reflection) {
            ((Reflection) effect).setInput(input);
        } else if (effect instanceof SepiaTone) {
            ((SepiaTone) effect).setInput(input);
        } else if (effect instanceof Shadow) {
            ((Shadow) effect).setInput(input);
        } else {
            assert false;
        }
    }
}
