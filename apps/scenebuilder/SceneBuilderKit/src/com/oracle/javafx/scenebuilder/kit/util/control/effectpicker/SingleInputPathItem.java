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

import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;

/**
 * Effect path item for effects defining single input.
 */
public class SingleInputPathItem extends EffectPathItem {

    private EffectPathItem inputPathItem;

    public SingleInputPathItem(EffectPickerController epc, Effect effect, EffectPathItem hostPathItem) {
        super(epc, effect, hostPathItem);
        assert effect instanceof Bloom
                || effect instanceof BoxBlur
                || effect instanceof ColorAdjust
                || effect instanceof DisplacementMap
                || effect instanceof DropShadow
                || effect instanceof GaussianBlur
                || effect instanceof Glow
                || effect instanceof InnerShadow
                || effect instanceof MotionBlur
                || effect instanceof PerspectiveTransform
                || effect instanceof Reflection
                || effect instanceof SepiaTone
                || effect instanceof Shadow;
    }

    @Override
    EffectPathItem getSelectedInputPathItem() {
        return inputPathItem;
    }

    void setInputPathItem(EffectPathItem epi) {
        inputPathItem = epi;
    }

    @Override
    void setSelectedInputEffect(Effect input) {
        if (effect instanceof Bloom) {
            ((Bloom) effect).setInput(input);
        } else if (effect instanceof BoxBlur) {
            ((BoxBlur) effect).setInput(input);
        } else if (effect instanceof ColorAdjust) {
            ((ColorAdjust) effect).setInput(input);
        } else if (effect instanceof DisplacementMap) {
            ((DisplacementMap) effect).setInput(input);
        } else if (effect instanceof DropShadow) {
            ((DropShadow) effect).setInput(input);
        } else if (effect instanceof GaussianBlur) {
            ((GaussianBlur) effect).setInput(input);
        } else if (effect instanceof Glow) {
            ((Glow) effect).setInput(input);
        } else if (effect instanceof InnerShadow) {
            ((InnerShadow) effect).setInput(input);
        } else if (effect instanceof MotionBlur) {
            ((MotionBlur) effect).setInput(input);
        } else if (effect instanceof PerspectiveTransform) {
            ((PerspectiveTransform) effect).setInput(input);
        } else if (effect instanceof Reflection) {
            ((Reflection) effect).setInput(input);
        } else if (effect instanceof SepiaTone) {
            ((SepiaTone) effect).setInput(input);
        } else {
            assert effect instanceof Shadow;
            ((Shadow) effect).setInput(input);
        }
    }

    Effect getInput() {
        if (effect instanceof Bloom) {
            return ((Bloom) effect).getInput();
        } else if (effect instanceof BoxBlur) {
            return ((BoxBlur) effect).getInput();
        } else if (effect instanceof ColorAdjust) {
            return ((ColorAdjust) effect).getInput();
        } else if (effect instanceof DisplacementMap) {
            return ((DisplacementMap) effect).getInput();
        } else if (effect instanceof DropShadow) {
            return ((DropShadow) effect).getInput();
        } else if (effect instanceof GaussianBlur) {
            return ((GaussianBlur) effect).getInput();
        } else if (effect instanceof Glow) {
            return ((Glow) effect).getInput();
        } else if (effect instanceof InnerShadow) {
            return ((InnerShadow) effect).getInput();
        } else if (effect instanceof MotionBlur) {
            return ((MotionBlur) effect).getInput();
        } else if (effect instanceof PerspectiveTransform) {
            return ((PerspectiveTransform) effect).getInput();
        } else if (effect instanceof Reflection) {
            return ((Reflection) effect).getInput();
        } else if (effect instanceof SepiaTone) {
            return ((SepiaTone) effect).getInput();
        } else {
            assert effect instanceof Shadow;
            return ((Shadow) effect).getInput();
        }
    }
}
