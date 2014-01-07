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

    static final Effect newInstance(Class<?> clazz) {
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

    static void setDefaultInput(Effect effect, Effect input) {
        assert effect != null;
        assert input != null;
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
