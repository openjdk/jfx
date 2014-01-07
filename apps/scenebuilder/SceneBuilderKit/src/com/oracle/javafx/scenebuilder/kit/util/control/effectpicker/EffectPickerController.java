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

import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.CheckBoxEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.EnumEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.ImageEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.NumFieldEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.SliderEditor;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPickerController.Mode;
import java.net.URL;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BlurType;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Controller class for the effects editor.
 */
public class EffectPickerController {

    @FXML
    private VBox root_vbox;
    @FXML
    private VBox props_vbox;
    @FXML
    private HBox effects_path_hbox;

    private final ToggleGroup effectToggleGroup = new ToggleGroup();
    private final Image selectionChevronImage;

    private final ObjectProperty<Effect> rootEffect = new SimpleObjectProperty<>();

    public EffectPickerController() {
        // Initialize selection chevron image
        final URL selectionChevronURL = EffectPickerController.class.getResource("images/selection-chevron.png"); //NOI18N
        assert selectionChevronURL != null;
        selectionChevronImage = new Image(selectionChevronURL.toExternalForm());
    }

    public final ObjectProperty<Effect> rootEffectProperty() {
        return rootEffect;
    }

    public final Effect getRootEffectProperty() {
        return rootEffect.get();
    }

    public final void setRootEffectProperty(Effect value) {
        rootEffect.setValue(value);
    }

    public void reset() {
    }

    /**
     * Update the effect path items starting from the root effect path item.
     */
    public void updateUI() {
        effects_path_hbox.getChildren().clear();
        if (getRootEffectProperty() != null) {
            final EffectPathItem rootEffectPathItem = makeEffectPathItem(getRootEffectProperty(), null);
            assert rootEffectPathItem != null;
            EffectPathItem epi = rootEffectPathItem;
            while (epi != null) {
                effects_path_hbox.getChildren().add(epi);
                if (epi.getSelectedInputPathItem() != null) {
                    final ImageView img = new ImageView(selectionChevronImage);
                    effects_path_hbox.getChildren().add(img);
                    epi = epi.getSelectedInputPathItem();
                } else {
                    epi = null;
                }
            }
            selectEffectPathItem(rootEffectPathItem);
        }
    }

    /**
     * Update the effect path items starting from the specified effect path item.
     *
     * @param effectPathItem
     */
    public void updateUI(EffectPathItem effectPathItem) {
        assert effectPathItem != null;
        int index = effects_path_hbox.getChildren().indexOf(effectPathItem);
        // Remove sub items if any
        if (index + 1 < effects_path_hbox.getChildren().size()) {
            effects_path_hbox.getChildren().remove(index + 1, effects_path_hbox.getChildren().size());
        }
        // Add new sub items if any
        EffectPathItem epi = effectPathItem.getSelectedInputPathItem();
        while (epi != null) {
            effects_path_hbox.getChildren().add(epi);
            if (epi.getSelectedInputPathItem() != null) {
                final ImageView img = new ImageView(selectionChevronImage);
                effects_path_hbox.getChildren().add(img);
                epi = epi.getSelectedInputPathItem();
            } else {
                epi = null;
            }
        }
    }

    public ToggleGroup getEffectToggleGroup() {
        return effectToggleGroup;
    }

    public String getEffectPath() {
        final EffectPathItem rootEffectPathItem = makeEffectPathItem(getRootEffectProperty(), null);
        if (rootEffectPathItem == null) {
            return "+"; //NOI18N
        } else {
            final StringBuilder sb = new StringBuilder();
            EffectPathItem epi = rootEffectPathItem;
            while (epi != null) {
                sb.append(epi.getSimpleName());
                if (epi.getSelectedInputPathItem() != null) {
                    sb.append(", "); //NOI18N
                    epi = epi.getSelectedInputPathItem();
                } else {
                    epi = null;
                }
            }
            return sb.toString();
        }
    }

    @FXML
    void initialize() {
        assert root_vbox != null;
        assert effects_path_hbox != null;
        assert props_vbox != null;
    }

    private EffectPathItem makeEffectPathItem(Effect effect, EffectPathItem hostPathItem) {
        final EffectPathItem epi;
        if (effect == null) {
            return null;
        }
        if (effect instanceof Blend) {
            epi = makeBlendPathItem(effect, hostPathItem);
        } else if (effect instanceof ColorInput) {
            epi = makeColorInputPathItem(effect, hostPathItem);
        } else if (effect instanceof ImageInput) {
            epi = makeImageInputPathItem(effect, hostPathItem);
        } else if (effect instanceof Lighting) {
            epi = makeLightingPathItem(effect, hostPathItem);
        } else {
            epi = makeSingleInputPathItem(effect, hostPathItem);
        }
        return epi;
    }

    private EffectPathItem makeBlendPathItem(Effect effect, EffectPathItem hostPathItem) {
        assert effect != null;
        final BlendPathItem epi = new BlendPathItem(this, effect, hostPathItem);
        final Effect topInput = epi.getTopInput();
        final EffectPathItem topInputPathItem
                = topInput == null ? null : makeEffectPathItem(topInput, epi);
        epi.setTopInputPathItem(topInputPathItem);
        final Effect bottomInput = epi.getBottomInput();
        final EffectPathItem bottomInputPathItem
                = bottomInput == null ? null : makeEffectPathItem(bottomInput, epi);
        epi.setBottomInputPathItem(bottomInputPathItem);
        return epi;
    }

    private EffectPathItem makeColorInputPathItem(Effect effect, EffectPathItem hostPathItem) {
        assert effect != null;
        final ColorInputPathItem epi = new ColorInputPathItem(this, effect, hostPathItem);
        return epi;
    }

    private EffectPathItem makeImageInputPathItem(Effect effect, EffectPathItem hostPathItem) {
        assert effect != null;
        final ImageInputPathItem epi = new ImageInputPathItem(this, effect, hostPathItem);
        return epi;
    }

    private EffectPathItem makeLightingPathItem(Effect effect, EffectPathItem hostPathItem) {
        assert effect != null;
        final LightingPathItem epi = new LightingPathItem(this, effect, hostPathItem);
        final Effect bumpInput = epi.getBumpInput();
        final EffectPathItem bumpInputPathItem
                = bumpInput == null ? null : makeEffectPathItem(bumpInput, epi);
        epi.setBumpInputPathItem(bumpInputPathItem);
        final Effect contentInput = epi.getContentInput();
        final EffectPathItem contentInputPathItem
                = contentInput == null ? null : makeEffectPathItem(contentInput, epi);
        epi.setContentInputPathItem(contentInputPathItem);
        return epi;
    }

    private EffectPathItem makeSingleInputPathItem(Effect effect, EffectPathItem hostPathItem) {
        assert effect != null;
        final SingleInputPathItem epi = new SingleInputPathItem(this, effect, hostPathItem);
        final Effect input = epi.getInput();
        final EffectPathItem inputPathItem
                = input == null ? null : makeEffectPathItem(input, epi);
        epi.setInputPathItem(inputPathItem);
        return epi;
    }

    public void selectEffectPathItem(EffectPathItem epi) {
        assert epi != null;
        final ToggleButton tb = epi.getToggleButton();
        final Effect effect = epi.getValue();
        tb.setSelected(true);
        props_vbox.getChildren().clear();
        if (effect != null) {
            makeEffectUI(effect);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Effects UI ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private void makeEffectUI(Effect effect) {
        if (effect instanceof Blend) {
            makeBlendUI(effect);
        } else if (effect instanceof Bloom) {
            makeBloomUI(effect);
        } else if (effect instanceof BoxBlur) {
            makeBoxBlurUI(effect);
        } else if (effect instanceof ColorAdjust) {
            makeColorAdjustUI(effect);
        } else if (effect instanceof ColorInput) {
            makeColorInputUI(effect);
        } else if (effect instanceof DisplacementMap) {
            makeDisplacementMapUI(effect);
        } else if (effect instanceof DropShadow) {
            makeDropShadowUI(effect);
        } else if (effect instanceof GaussianBlur) {
            makeGaussianBlurUI(effect);
        } else if (effect instanceof Glow) {
            makeGlowUI(effect);
        } else if (effect instanceof ImageInput) {
            makeImageInputUI(effect);
        } else if (effect instanceof InnerShadow) {
            makeInnerShadowUI(effect);
        } else if (effect instanceof Lighting) {
            makeLightingUI(effect);
        } else if (effect instanceof MotionBlur) {
            makeMotionBlurUI(effect);
        } else if (effect instanceof PerspectiveTransform) {
            makePerspectiveTransformUI(effect);
        } else if (effect instanceof Reflection) {
            makeReflectionUI(effect);
        } else if (effect instanceof SepiaTone) {
            makeSepiaToneUI(effect);
        } else {
            assert effect instanceof Shadow;
            makeShadowUI(effect);
        }
    }

    private void makeBlendUI(Effect effect) {
        assert effect instanceof Blend;
        final Blend blend = (Blend) effect;
        final VBox vBox = new VBox(8.0);

        final EnumEditor<BlendMode> modeEditor = new EnumEditor<>(
                "mode", BlendMode.values(), blend.getMode()); //NOI18N
        blend.modeProperty().bind(modeEditor.getValueProperty());
        vBox.getChildren().add(modeEditor);

        final SliderEditor opacityEditor = new SliderEditor(
                "opacity", 0, 1.0, blend.getOpacity(), 1.0, false); //NOI18N
        blend.opacityProperty().bind(opacityEditor.getValueProperty());
        vBox.getChildren().add(opacityEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeBloomUI(Effect effect) {
        assert effect instanceof Bloom;
        final Bloom bloom = (Bloom) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor thresholdEditor = new SliderEditor(
                "threshold", 0, 1.0, bloom.getThreshold(), 0.1, false); //NOI18N
        bloom.thresholdProperty().bind(thresholdEditor.getValueProperty());
        vBox.getChildren().add(thresholdEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeBoxBlurUI(Effect effect) {
        assert effect instanceof BoxBlur;
        final BoxBlur boxBlur = (BoxBlur) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor widthEditor = new SliderEditor(
                "width", 0, 255.0, boxBlur.getWidth(), 1.0, false); //NOI18N
        boxBlur.widthProperty().bind(widthEditor.getValueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderEditor heightEditor = new SliderEditor(
                "height", 0, 255.0, boxBlur.getHeight(), 1.0, false); //NOI18N
        boxBlur.heightProperty().bind(heightEditor.getValueProperty());
        vBox.getChildren().add(heightEditor);

        final SliderEditor iterationsEditor = new SliderEditor(
                "iterations", 0, 3.0, boxBlur.getIterations(), 1.0, true); //NOI18N
        final Slider slider = iterationsEditor.getSlider();
        slider.setBlockIncrement(1.0);
        slider.setMajorTickUnit(1.0);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        boxBlur.iterationsProperty().bind(iterationsEditor.getValueProperty());
        vBox.getChildren().add(iterationsEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeColorAdjustUI(Effect effect) {
        assert effect instanceof ColorAdjust;
        final ColorAdjust colorAdjust = (ColorAdjust) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor brightnessEditor = new SliderEditor(
                "brightness", -1.0, 1.0, colorAdjust.getBrightness(), 0.1, false); //NOI18N
        colorAdjust.brightnessProperty().bind(brightnessEditor.getValueProperty());
        vBox.getChildren().add(brightnessEditor);

        final SliderEditor contrastEditor = new SliderEditor(
                "contrast", -1.0, 1.0, colorAdjust.getContrast(), 0.1, false); //NOI18N
        colorAdjust.contrastProperty().bind(contrastEditor.getValueProperty());
        vBox.getChildren().add(contrastEditor);

        final SliderEditor hueEditor = new SliderEditor(
                "hue", -1.0, 1.0, colorAdjust.getHue(), 0.1, false); //NOI18N
        colorAdjust.hueProperty().bind(hueEditor.getValueProperty());
        vBox.getChildren().add(hueEditor);

        final SliderEditor saturationEditor = new SliderEditor(
                "saturation", -1.0, 1.0, colorAdjust.getSaturation(), 0.1, false); //NOI18N
        colorAdjust.saturationProperty().bind(saturationEditor.getValueProperty());
        vBox.getChildren().add(saturationEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeColorInputUI(Effect effect) {
        assert effect instanceof ColorInput;
        final ColorInput colorInput = (ColorInput) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor widthEditor = new SliderEditor(
                "width", 0, 255.0, colorInput.getWidth(), 1.0, false); //NOI18N
        colorInput.widthProperty().bind(widthEditor.getValueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderEditor heightEditor = new SliderEditor(
                "height", 0, 255.0, colorInput.getHeight(), 1.0, false); //NOI18N
        colorInput.heightProperty().bind(heightEditor.getValueProperty());
        vBox.getChildren().add(heightEditor);

        final NumFieldEditor xEditor = new NumFieldEditor(
                "x", -10.0, 10.0, colorInput.getX(), 1.0, false); //NOI18N
        colorInput.xProperty().bind(xEditor.getValueProperty());
        vBox.getChildren().add(xEditor);

        final NumFieldEditor yEditor = new NumFieldEditor(
                "y", -10.0, 10.0, colorInput.getY(), 1.0, false); //NOI18N
        colorInput.yProperty().bind(yEditor.getValueProperty());
        vBox.getChildren().add(yEditor);

        final PaintPicker colorPicker = new PaintPicker();
        colorPicker.setPaintProperty(colorInput.getPaint());
        colorPicker.paintProperty().addListener(new PaintChangeListener(colorInput));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeDisplacementMapUI(Effect effect) {
        assert effect instanceof DisplacementMap;
        final DisplacementMap displacementMap = (DisplacementMap) effect;
        VBox vBox = new VBox(8.0);

        final NumFieldEditor offsetXEditor = new NumFieldEditor(
                "offsetX", -10.0, 10.0, displacementMap.getOffsetX(), 1.0, false); //NOI18N
        displacementMap.offsetXProperty().bind(offsetXEditor.getValueProperty());
        vBox.getChildren().add(offsetXEditor);

        final NumFieldEditor offsetYEditor = new NumFieldEditor(
                "offsetY", -10.0, 10.0, displacementMap.getOffsetY(), 1.0, false); //NOI18N
        displacementMap.offsetYProperty().bind(offsetYEditor.getValueProperty());
        vBox.getChildren().add(offsetYEditor);

        final NumFieldEditor scaleXEditor = new NumFieldEditor(
                "scaleX", -10.0, 10.0, displacementMap.getScaleX(), 1.0, false); //NOI18N
        displacementMap.scaleXProperty().bind(scaleXEditor.getValueProperty());
        vBox.getChildren().add(scaleXEditor);

        final NumFieldEditor scaleYEditor = new NumFieldEditor(
                "scaleY", -10.0, 10.0, displacementMap.getScaleY(), 1.0, false); //NOI18N
        displacementMap.scaleYProperty().bind(scaleYEditor.getValueProperty());
        vBox.getChildren().add(scaleYEditor);

        final CheckBoxEditor wrapEditor = new CheckBoxEditor(
                "wrap", displacementMap.isWrap()); //NOI18N
        displacementMap.wrapProperty().bind(wrapEditor.getValueProperty());
        vBox.getChildren().add(wrapEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeDropShadowUI(Effect effect) {
        assert effect instanceof DropShadow;
        final DropShadow dropShadow = (DropShadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumEditor<BlurType> blurTypeEditor = new EnumEditor<>(
                "blurType", BlurType.values(), dropShadow.getBlurType()); //NOI18N
        dropShadow.blurTypeProperty().bind(blurTypeEditor.getValueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderEditor widthEditor = new SliderEditor(
                "width", 0, 255.0, dropShadow.getWidth(), 1.0, false); //NOI18N
        dropShadow.widthProperty().bind(widthEditor.getValueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderEditor heightEditor = new SliderEditor(
                "height", 0, 255.0, dropShadow.getHeight(), 1.0, false); //NOI18N
        dropShadow.heightProperty().bind(heightEditor.getValueProperty());
        vBox.getChildren().add(heightEditor);

        final NumFieldEditor offsetXEditor = new NumFieldEditor(
                "offsetX", -10.0, 10.0, dropShadow.getOffsetX(), 1.0, false); //NOI18N
        dropShadow.offsetXProperty().bind(offsetXEditor.getValueProperty());
        vBox.getChildren().add(offsetXEditor);

        final NumFieldEditor offsetYEditor = new NumFieldEditor(
                "offsetY", -10.0, 10.0, dropShadow.getOffsetY(), 1.0, false); //NOI18N
        dropShadow.offsetYProperty().bind(offsetYEditor.getValueProperty());
        vBox.getChildren().add(offsetYEditor);

        // setting radius equivalent to setting both width and height attributes to value of (2 * radius + 1)
//        SliderEditor e6 = (new SliderEditor("radius", 0, 127.0 , dropShadow.getRadius(), 1.0, false ) );
//        dropShadow.radiusProperty().bind(e6.value);
//        vBox.getChildren().add(e6);
        final SliderEditor spreadEditor = new SliderEditor(
                "spread", 0, 1.0, dropShadow.getSpread(), 0.1, false); //NOI18N
        dropShadow.spreadProperty().bind(spreadEditor.getValueProperty());
        vBox.getChildren().add(spreadEditor);

        final PaintPicker colorPicker = new PaintPicker(Mode.COLOR);
        colorPicker.setPaintProperty(dropShadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(dropShadow));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeGaussianBlurUI(Effect effect) {
        assert effect instanceof GaussianBlur;
        final GaussianBlur gaussianBlur = (GaussianBlur) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor radiusEditor = new SliderEditor(
                "radius", 0, 63.0, gaussianBlur.getRadius(), 0.1, false); //NOI18N
        gaussianBlur.radiusProperty().bind(radiusEditor.getValueProperty());
        vBox.getChildren().add(radiusEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeGlowUI(Effect effect) {
        assert effect instanceof Glow;
        final Glow glow = (Glow) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor levelEditor = new SliderEditor(
                "level", 0, 1.0, glow.getLevel(), 0.1, false); //NOI18N
        glow.levelProperty().bind(levelEditor.getValueProperty());
        vBox.getChildren().add(levelEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeImageInputUI(Effect effect) {
        assert effect instanceof ImageInput;
        final ImageInput imageInput = (ImageInput) effect;
        VBox vBox = new VBox(8.0);

        final NumFieldEditor xEditor = new NumFieldEditor(
                "x", -10.0, 10.0, imageInput.getX(), 1.0, false); //NOI18N
        imageInput.xProperty().bind(xEditor.getValueProperty());
        vBox.getChildren().add(xEditor);

        final NumFieldEditor yEditor = new NumFieldEditor(
                "y", -10.0, 10.0, imageInput.getY(), 1.0, false); //NOI18N
        imageInput.yProperty().bind(yEditor.getValueProperty());
        vBox.getChildren().add(yEditor);

        final ImageEditor imageEditor = new ImageEditor(
                "source", imageInput.getSource()); //NOI18N
        imageInput.sourceProperty().bind(imageEditor.getValueProperty());
        vBox.getChildren().add(imageEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeInnerShadowUI(Effect effect) {
        assert effect instanceof InnerShadow;
        final InnerShadow innerShadow = (InnerShadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumEditor<BlurType> blurTypeEditor = new EnumEditor<>(
                "blurType", BlurType.values(), innerShadow.getBlurType()); //NOI18N
        innerShadow.blurTypeProperty().bind(blurTypeEditor.getValueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderEditor chokeEditor = new SliderEditor(
                "choke", 0, 1.0, innerShadow.getChoke(), 0.1, false); //NOI18N
        innerShadow.chokeProperty().bind(chokeEditor.getValueProperty());
        vBox.getChildren().add(chokeEditor);

        final SliderEditor widthEditor = new SliderEditor(
                "width", 0, 255.0, innerShadow.getWidth(), 0.1, false); //NOI18N
        innerShadow.widthProperty().bind(widthEditor.getValueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderEditor heightEditor = new SliderEditor(
                "height", 0, 255.0, innerShadow.getHeight(), 0.1, false); //NOI18N
        innerShadow.heightProperty().bind(heightEditor.getValueProperty());
        vBox.getChildren().add(heightEditor);

        final NumFieldEditor offsetXEditor = new NumFieldEditor(
                "offsetX", -10.0, 10.0, innerShadow.getOffsetX(), 1.0, false); //NOI18N
        innerShadow.offsetXProperty().bind(offsetXEditor.getValueProperty());
        vBox.getChildren().add(offsetXEditor);

        final NumFieldEditor offsetYEditor = new NumFieldEditor(
                "offsetY", -10.0, 10.0, innerShadow.getOffsetY(), 1.0, false); //NOI18N
        innerShadow.offsetYProperty().bind(offsetYEditor.getValueProperty());
        vBox.getChildren().add(offsetYEditor);

        // setting radius equivalent to setting both width and height attributes to value of (2 * radius + 1)
//        SliderEditor e7 = (new SliderEditor("radius", 0, 127.0 , innerShadow.getRadius(), 1.0, false ) );
//        innerShadow.radiusProperty().bind(e7.value);
//        vBox.getChildren().add(e7);
        final PaintPicker colorPicker = new PaintPicker(Mode.COLOR);
        colorPicker.setPaintProperty(innerShadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(innerShadow));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeLightingUI(Effect effect) {
        assert effect instanceof Lighting;
        final Lighting lighting = (Lighting) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor diffuseConstantEditor = new SliderEditor(
                "diffuseConstant", 0, 2.0, lighting.getDiffuseConstant(), 0.1, false); //NOI18N
        lighting.diffuseConstantProperty().bind(diffuseConstantEditor.getValueProperty());
        vBox.getChildren().add(diffuseConstantEditor);

        final SliderEditor specularConstantEditor = new SliderEditor(
                "specularConstant", 0, 2.0, lighting.getSpecularConstant(), 0.1, false); //NOI18N
        lighting.specularConstantProperty().bind(specularConstantEditor.getValueProperty());
        vBox.getChildren().add(specularConstantEditor);

        final SliderEditor specularExponentEditor = new SliderEditor(
                "specularExponent", 0, 40.0, lighting.getSpecularExponent(), 0.1, false); //NOI18N
        lighting.specularExponentProperty().bind(specularExponentEditor.getValueProperty());
        vBox.getChildren().add(specularExponentEditor);

        final SliderEditor surfaceScaleEditor = new SliderEditor(
                "surfaceScale", 0, 10.0, lighting.getSurfaceScale(), 0.1, false); //NOI18N
        lighting.surfaceScaleProperty().bind(surfaceScaleEditor.getValueProperty());
        vBox.getChildren().add(surfaceScaleEditor);

        // need editor for this
        vBox.getChildren().add(new Label("Light Editor here")); //NOI18N

        props_vbox.getChildren().add(vBox);
    }

    private void makeMotionBlurUI(Effect effect) {
        assert effect instanceof MotionBlur;
        final MotionBlur motionBlur = (MotionBlur) effect;
        VBox vBox = new VBox(8.0);

        // need editor for this
        vBox.getChildren().add(new Label("Angle editor here")); //NOI18N

        final SliderEditor radiusEditor = new SliderEditor(
                "radius", 0, 63.0, motionBlur.getRadius(), 0.1, false); //NOI18N
        motionBlur.radiusProperty().bind(radiusEditor.getValueProperty());
        vBox.getChildren().add(radiusEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makePerspectiveTransformUI(Effect effect) {
        assert effect instanceof PerspectiveTransform;
        final PerspectiveTransform perspectiveTransform = (PerspectiveTransform) effect;
        VBox vBox = new VBox(8.0);

        final NumFieldEditor llxEditor = new NumFieldEditor(
                "llx", -10.0, 10.0, perspectiveTransform.getLlx(), 1.0, false); //NOI18N
        perspectiveTransform.llxProperty().bind(llxEditor.getValueProperty());
        vBox.getChildren().add(llxEditor);

        final NumFieldEditor llyEditor = new NumFieldEditor(
                "lly", -10.0, 10.0, perspectiveTransform.getLly(), 1.0, false); //NOI18N
        perspectiveTransform.llyProperty().bind(llyEditor.getValueProperty());
        vBox.getChildren().add(llyEditor);

        final NumFieldEditor lrxEditor = new NumFieldEditor(
                "lrx", -10.0, 10.0, perspectiveTransform.getLrx(), 1.0, false); //NOI18N
        perspectiveTransform.lrxProperty().bind(lrxEditor.getValueProperty());
        vBox.getChildren().add(lrxEditor);

        final NumFieldEditor lryEditor = new NumFieldEditor(
                "lry", -10.0, 10.0, perspectiveTransform.getLry(), 1.0, false); //NOI18N
        perspectiveTransform.lryProperty().bind(lryEditor.getValueProperty());
        vBox.getChildren().add(lryEditor);

        final NumFieldEditor ulxEditor = new NumFieldEditor(
                "ulx", -10.0, 10.0, perspectiveTransform.getUlx(), 1.0, false); //NOI18N
        perspectiveTransform.ulxProperty().bind(ulxEditor.getValueProperty());
        vBox.getChildren().add(ulxEditor);

        final NumFieldEditor ulyEditor = new NumFieldEditor(
                "uly", -10.0, 10.0, perspectiveTransform.getUly(), 1.0, false); //NOI18N
        perspectiveTransform.ulyProperty().bind(ulyEditor.getValueProperty());
        vBox.getChildren().add(ulyEditor);

        final NumFieldEditor urxEditor = new NumFieldEditor(
                "urx", -10.0, 10.0, perspectiveTransform.getUrx(), 1.0, false); //NOI18N
        perspectiveTransform.urxProperty().bind(urxEditor.getValueProperty());
        vBox.getChildren().add(urxEditor);

        final NumFieldEditor uryEditor = new NumFieldEditor(
                "ury", -10.0, 10.0, perspectiveTransform.getUry(), 1.0, false); //NOI18N
        perspectiveTransform.uryProperty().bind(uryEditor.getValueProperty());
        vBox.getChildren().add(uryEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeReflectionUI(Effect effect) {
        assert effect instanceof Reflection;
        final Reflection reflection = (Reflection) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor bottomOpacityEditor = new SliderEditor(
                "bottomOpacity", 0, 1.0, reflection.getBottomOpacity(), 0.1, false); //NOI18N
        reflection.bottomOpacityProperty().bind(bottomOpacityEditor.getValueProperty());
        vBox.getChildren().add(bottomOpacityEditor);

        final SliderEditor topOpacityEditor = new SliderEditor(
                "topOpacity", 0, 1.0, reflection.getTopOpacity(), 0.1, false); //NOI18N
        reflection.topOpacityProperty().bind(topOpacityEditor.getValueProperty());
        vBox.getChildren().add(topOpacityEditor);

        final NumFieldEditor topOffsetEditor = new NumFieldEditor(
                "topOffset", -10.0, 10.0, reflection.getTopOffset(), 1.0, false); //NOI18N
        reflection.topOffsetProperty().bind(topOffsetEditor.getValueProperty());
        vBox.getChildren().add(topOffsetEditor);

        final SliderEditor fractionEditor = new SliderEditor(
                "fraction", 0, 1.0, reflection.getFraction(), 0.1, false); //NOI18N
        reflection.fractionProperty().bind(fractionEditor.getValueProperty());
        vBox.getChildren().add(fractionEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeSepiaToneUI(Effect effect) {
        assert effect instanceof SepiaTone;
        final SepiaTone sepiaTone = (SepiaTone) effect;
        VBox vBox = new VBox(8.0);

        final SliderEditor levelEditor = new SliderEditor(
                "level", 0, 1.0, sepiaTone.getLevel(), 0.1, false); //NOI18N
        sepiaTone.levelProperty().bind(levelEditor.getValueProperty());
        vBox.getChildren().add(levelEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeShadowUI(Effect effect) {
        assert effect instanceof Shadow;
        final Shadow shadow = (Shadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumEditor<BlurType> blurTypeEditor = new EnumEditor<>(
                "blurType", BlurType.values(), shadow.getBlurType()); //NOI18N
        shadow.blurTypeProperty().bind(blurTypeEditor.getValueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderEditor widthEditor = new SliderEditor(
                "width", 0, 255.0, shadow.getWidth(), 0.1, false); //NOI18N
        shadow.widthProperty().bind(widthEditor.getValueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderEditor heightEditor = new SliderEditor(
                "height", 0, 255.0, shadow.getHeight(), 0.1, false); //NOI18N
        shadow.heightProperty().bind(heightEditor.getValueProperty());
        vBox.getChildren().add(heightEditor);

        final SliderEditor radiusEditor = new SliderEditor(
                "radius", 0, 127.0, shadow.getRadius(), 0.1, false); //NOI18N
        shadow.radiusProperty().bind(radiusEditor.getValueProperty());
        vBox.getChildren().add(radiusEditor);

        final PaintPicker colorPicker = new PaintPicker(Mode.COLOR);
        colorPicker.setPaintProperty(shadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(shadow));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class ColorChangeListener implements ChangeListener<Paint> {

        private final Effect effect;

        public ColorChangeListener(Effect effect) {
            assert effect instanceof DropShadow
                    || effect instanceof InnerShadow
                    || effect instanceof Shadow;
            this.effect = effect;
        }

        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            assert newValue instanceof Color;
            final Color color = (Color) newValue;
            if (effect instanceof DropShadow) {
                ((DropShadow) effect).setColor(color);
            } else if (effect instanceof InnerShadow) {
                ((InnerShadow) effect).setColor(color);
            } else {
                assert effect instanceof Shadow;
                ((Shadow) effect).setColor(color);
            }
        }
    }

    private static class PaintChangeListener implements ChangeListener<Paint> {

        private final ColorInput colorInput;

        public PaintChangeListener(ColorInput colorInput) {
            this.colorInput = colorInput;
        }

        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            colorInput.setPaint(newValue);
        }
    }
}
