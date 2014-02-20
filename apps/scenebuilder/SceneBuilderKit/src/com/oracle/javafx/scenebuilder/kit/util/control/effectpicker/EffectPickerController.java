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

import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.CheckBoxControl;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.EnumControl;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.ImageControl;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.DoubleTextFieldControl;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.LightControl;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors.SliderControl;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker.Mode;
import java.net.URL;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
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
    private EffectPicker.Delegate effectPickerDelegate;
    private PaintPicker.Delegate paintPickerDelegate;

    private final ObjectProperty<Effect> rootEffect = new SimpleObjectProperty<>();
    // The revision property is used when a change occurs on the root effect inputs
    private final SimpleIntegerProperty revision = new SimpleIntegerProperty();
    private final BooleanProperty liveUpdate = new SimpleBooleanProperty();

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

    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }

    public final BooleanProperty liveUpdateProperty() {
        return liveUpdate;
    }

    public boolean isLiveUpdate() {
        return liveUpdate.get();
    }
    
    public void setLiveUpdate(boolean value) {
        liveUpdate.setValue(value);
    }
    
    public EffectPicker.Delegate getEffectPickerDelegate() {
        return effectPickerDelegate;
    }
    
    void setEffectPickerDelegate(EffectPicker.Delegate delegate) {
        this.effectPickerDelegate = delegate;
    }

    public PaintPicker.Delegate getPaintPickerDelegate() {
        return paintPickerDelegate;
    }
    
    void setPaintPickerDelegate(PaintPicker.Delegate delegate) {
        this.paintPickerDelegate = delegate;
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

    public void incrementRevision() {
        revision.set(revision.get() + 1);
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

        final EnumControl<BlendMode> modeEditor = new EnumControl<>(
                this, "mode", BlendMode.values(), blend.getMode()); //NOI18N
        blend.modeProperty().bind(modeEditor.valueProperty());
        vBox.getChildren().add(modeEditor);

        final SliderControl opacityEditor = new SliderControl(
                this, "opacity", 0, 1.0, blend.getOpacity(), 0.1, false); //NOI18N
        blend.opacityProperty().bind(opacityEditor.valueProperty());
        vBox.getChildren().add(opacityEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeBloomUI(Effect effect) {
        assert effect instanceof Bloom;
        final Bloom bloom = (Bloom) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl thresholdEditor = new SliderControl(
                this, "threshold", 0, 1.0, bloom.getThreshold(), 0.1, false); //NOI18N
        bloom.thresholdProperty().bind(thresholdEditor.valueProperty());
        vBox.getChildren().add(thresholdEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeBoxBlurUI(Effect effect) {
        assert effect instanceof BoxBlur;
        final BoxBlur boxBlur = (BoxBlur) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl widthEditor = new SliderControl(
                this, "width", 0, 255.0, boxBlur.getWidth(), 1.0, false); //NOI18N
        boxBlur.widthProperty().bind(widthEditor.valueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderControl heightEditor = new SliderControl(
                this, "height", 0, 255.0, boxBlur.getHeight(), 1.0, false); //NOI18N
        boxBlur.heightProperty().bind(heightEditor.valueProperty());
        vBox.getChildren().add(heightEditor);

        final SliderControl iterationsEditor = new SliderControl(
                this, "iterations", 0, 3.0, boxBlur.getIterations(), 1.0, true); //NOI18N
        final Slider slider = iterationsEditor.getSlider();
        slider.setBlockIncrement(1.0);
        slider.setMajorTickUnit(1.0);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        boxBlur.iterationsProperty().bind(iterationsEditor.valueProperty());
        vBox.getChildren().add(iterationsEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeColorAdjustUI(Effect effect) {
        assert effect instanceof ColorAdjust;
        final ColorAdjust colorAdjust = (ColorAdjust) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl brightnessEditor = new SliderControl(
                this, "brightness", -1.0, 1.0, colorAdjust.getBrightness(), 0.1, false); //NOI18N
        colorAdjust.brightnessProperty().bind(brightnessEditor.valueProperty());
        vBox.getChildren().add(brightnessEditor);

        final SliderControl contrastEditor = new SliderControl(
                this, "contrast", -1.0, 1.0, colorAdjust.getContrast(), 0.1, false); //NOI18N
        colorAdjust.contrastProperty().bind(contrastEditor.valueProperty());
        vBox.getChildren().add(contrastEditor);

        final SliderControl hueEditor = new SliderControl(
                this, "hue", -1.0, 1.0, colorAdjust.getHue(), 0.1, false); //NOI18N
        colorAdjust.hueProperty().bind(hueEditor.valueProperty());
        vBox.getChildren().add(hueEditor);

        final SliderControl saturationEditor = new SliderControl(
                this, "saturation", -1.0, 1.0, colorAdjust.getSaturation(), 0.1, false); //NOI18N
        colorAdjust.saturationProperty().bind(saturationEditor.valueProperty());
        vBox.getChildren().add(saturationEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeColorInputUI(Effect effect) {
        assert effect instanceof ColorInput;
        final ColorInput colorInput = (ColorInput) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl widthEditor = new SliderControl(
                this, "width", 0, 255.0, colorInput.getWidth(), 1.0, false); //NOI18N
        colorInput.widthProperty().bind(widthEditor.valueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderControl heightEditor = new SliderControl(
                this, "height", 0, 255.0, colorInput.getHeight(), 1.0, false); //NOI18N
        colorInput.heightProperty().bind(heightEditor.valueProperty());
        vBox.getChildren().add(heightEditor);

        final DoubleTextFieldControl xEditor = new DoubleTextFieldControl(
                this, "x", -10.0, 10.0, colorInput.getX(), 1.0); //NOI18N
        colorInput.xProperty().bind(xEditor.valueProperty());
        vBox.getChildren().add(xEditor);

        final DoubleTextFieldControl yEditor = new DoubleTextFieldControl(
                this, "y", -10.0, 10.0, colorInput.getY(), 1.0); //NOI18N
        colorInput.yProperty().bind(yEditor.valueProperty());
        vBox.getChildren().add(yEditor);

        final PaintPicker colorPicker = new PaintPicker(paintPickerDelegate);
        colorPicker.setPaintProperty(colorInput.getPaint());
        colorPicker.paintProperty().addListener(new PaintChangeListener(this, colorInput));
        colorPicker.liveUpdateProperty().addListener(new PaintPickerLiveUpdateListener(colorPicker, this));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeDisplacementMapUI(Effect effect) {
        assert effect instanceof DisplacementMap;
        final DisplacementMap displacementMap = (DisplacementMap) effect;
        VBox vBox = new VBox(8.0);

        final DoubleTextFieldControl offsetXEditor = new DoubleTextFieldControl(
                this, "offsetX", -10.0, 10.0, displacementMap.getOffsetX(), 1.0); //NOI18N
        displacementMap.offsetXProperty().bind(offsetXEditor.valueProperty());
        vBox.getChildren().add(offsetXEditor);

        final DoubleTextFieldControl offsetYEditor = new DoubleTextFieldControl(
                this, "offsetY", -10.0, 10.0, displacementMap.getOffsetY(), 1.0); //NOI18N
        displacementMap.offsetYProperty().bind(offsetYEditor.valueProperty());
        vBox.getChildren().add(offsetYEditor);

        final DoubleTextFieldControl scaleXEditor = new DoubleTextFieldControl(
                this, "scaleX", -10.0, 10.0, displacementMap.getScaleX(), 1.0); //NOI18N
        displacementMap.scaleXProperty().bind(scaleXEditor.valueProperty());
        vBox.getChildren().add(scaleXEditor);

        final DoubleTextFieldControl scaleYEditor = new DoubleTextFieldControl(
                this, "scaleY", -10.0, 10.0, displacementMap.getScaleY(), 1.0); //NOI18N
        displacementMap.scaleYProperty().bind(scaleYEditor.valueProperty());
        vBox.getChildren().add(scaleYEditor);

        final CheckBoxControl wrapEditor = new CheckBoxControl(
                this, "wrap", displacementMap.isWrap()); //NOI18N
        displacementMap.wrapProperty().bind(wrapEditor.valueProperty());
        vBox.getChildren().add(wrapEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeDropShadowUI(Effect effect) {
        assert effect instanceof DropShadow;
        final DropShadow dropShadow = (DropShadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumControl<BlurType> blurTypeEditor = new EnumControl<>(
                this, "blurType", BlurType.values(), dropShadow.getBlurType()); //NOI18N
        dropShadow.blurTypeProperty().bind(blurTypeEditor.valueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderControl widthEditor = new SliderControl(
                this, "width", 0, 255.0, dropShadow.getWidth(), 1.0, false); //NOI18N
        dropShadow.widthProperty().bind(widthEditor.valueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderControl heightEditor = new SliderControl(
                this, "height", 0, 255.0, dropShadow.getHeight(), 1.0, false); //NOI18N
        dropShadow.heightProperty().bind(heightEditor.valueProperty());
        vBox.getChildren().add(heightEditor);

        // setting radius equivalent to setting both width and height attributes to value of (2 * radius + 1)
        final SliderControl radiusEditor = new SliderControl(
                this, "radius", 0, 127.0, dropShadow.getRadius(), 1.0, false); //NOI18N
        dropShadow.radiusProperty().bind(radiusEditor.valueProperty());
        vBox.getChildren().add(radiusEditor);

        final DoubleTextFieldControl offsetXEditor = new DoubleTextFieldControl(
                this, "offsetX", -10.0, 10.0, dropShadow.getOffsetX(), 1.0); //NOI18N
        dropShadow.offsetXProperty().bind(offsetXEditor.valueProperty());
        vBox.getChildren().add(offsetXEditor);

        final DoubleTextFieldControl offsetYEditor = new DoubleTextFieldControl(
                this, "offsetY", -10.0, 10.0, dropShadow.getOffsetY(), 1.0); //NOI18N
        dropShadow.offsetYProperty().bind(offsetYEditor.valueProperty());
        vBox.getChildren().add(offsetYEditor);

        final SliderControl spreadEditor = new SliderControl(
                this, "spread", 0, 1.0, dropShadow.getSpread(), 0.1, false); //NOI18N
        dropShadow.spreadProperty().bind(spreadEditor.valueProperty());
        vBox.getChildren().add(spreadEditor);

        final PaintPicker colorPicker = new PaintPicker(paintPickerDelegate, Mode.COLOR);
        colorPicker.setPaintProperty(dropShadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(this, dropShadow));
        colorPicker.liveUpdateProperty().addListener(new PaintPickerLiveUpdateListener(colorPicker, this));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeGaussianBlurUI(Effect effect) {
        assert effect instanceof GaussianBlur;
        final GaussianBlur gaussianBlur = (GaussianBlur) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl radiusEditor = new SliderControl(
                this, "radius", 0, 63.0, gaussianBlur.getRadius(), 0.1, false); //NOI18N
        gaussianBlur.radiusProperty().bind(radiusEditor.valueProperty());
        vBox.getChildren().add(radiusEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeGlowUI(Effect effect) {
        assert effect instanceof Glow;
        final Glow glow = (Glow) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl levelEditor = new SliderControl(
                this, "level", 0, 1.0, glow.getLevel(), 0.1, false); //NOI18N
        glow.levelProperty().bind(levelEditor.valueProperty());
        vBox.getChildren().add(levelEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeImageInputUI(Effect effect) {
        assert effect instanceof ImageInput;
        final ImageInput imageInput = (ImageInput) effect;
        VBox vBox = new VBox(8.0);

        final DoubleTextFieldControl xEditor = new DoubleTextFieldControl(
                this, "x", -10.0, 10.0, imageInput.getX(), 1.0); //NOI18N
        imageInput.xProperty().bind(xEditor.valueProperty());
        vBox.getChildren().add(xEditor);

        final DoubleTextFieldControl yEditor = new DoubleTextFieldControl(
                this, "y", -10.0, 10.0, imageInput.getY(), 1.0); //NOI18N
        imageInput.yProperty().bind(yEditor.valueProperty());
        vBox.getChildren().add(yEditor);

        final ImageControl imageEditor = new ImageControl(
                this, "source", imageInput.getSource()); //NOI18N
        imageInput.sourceProperty().bind(imageEditor.valueProperty());
        vBox.getChildren().add(imageEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeInnerShadowUI(Effect effect) {
        assert effect instanceof InnerShadow;
        final InnerShadow innerShadow = (InnerShadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumControl<BlurType> blurTypeEditor = new EnumControl<>(
                this, "blurType", BlurType.values(), innerShadow.getBlurType()); //NOI18N
        innerShadow.blurTypeProperty().bind(blurTypeEditor.valueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderControl chokeEditor = new SliderControl(
                this, "choke", 0, 1.0, innerShadow.getChoke(), 0.1, false); //NOI18N
        innerShadow.chokeProperty().bind(chokeEditor.valueProperty());
        vBox.getChildren().add(chokeEditor);

        final SliderControl widthEditor = new SliderControl(
                this, "width", 0, 255.0, innerShadow.getWidth(), 1.0, false); //NOI18N
        innerShadow.widthProperty().bind(widthEditor.valueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderControl heightEditor = new SliderControl(
                this, "height", 0, 255.0, innerShadow.getHeight(), 1.0, false); //NOI18N
        innerShadow.heightProperty().bind(heightEditor.valueProperty());
        vBox.getChildren().add(heightEditor);

        // setting radius equivalent to setting both width and height attributes to value of (2 * radius + 1)
        final SliderControl radiusEditor = new SliderControl(
                this, "radius", 0, 127.0, innerShadow.getRadius(), 1.0, false); //NOI18N
        innerShadow.radiusProperty().bind(radiusEditor.valueProperty());
        vBox.getChildren().add(radiusEditor);

        final DoubleTextFieldControl offsetXEditor = new DoubleTextFieldControl(
                this, "offsetX", -10.0, 10.0, innerShadow.getOffsetX(), 1.0); //NOI18N
        innerShadow.offsetXProperty().bind(offsetXEditor.valueProperty());
        vBox.getChildren().add(offsetXEditor);

        final DoubleTextFieldControl offsetYEditor = new DoubleTextFieldControl(
                this, "offsetY", -10.0, 10.0, innerShadow.getOffsetY(), 1.0); //NOI18N
        innerShadow.offsetYProperty().bind(offsetYEditor.valueProperty());
        vBox.getChildren().add(offsetYEditor);

        final PaintPicker colorPicker = new PaintPicker(paintPickerDelegate, Mode.COLOR);
        colorPicker.setPaintProperty(innerShadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(this, innerShadow));
        colorPicker.liveUpdateProperty().addListener(new PaintPickerLiveUpdateListener(colorPicker, this));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    private void makeLightingUI(Effect effect) {
        assert effect instanceof Lighting;
        final Lighting lighting = (Lighting) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl diffuseConstantEditor = new SliderControl(
                this, "diffuseConstant", 0, 2.0, lighting.getDiffuseConstant(), 1.0, false); //NOI18N
        lighting.diffuseConstantProperty().bind(diffuseConstantEditor.valueProperty());
        vBox.getChildren().add(diffuseConstantEditor);

        final SliderControl specularConstantEditor = new SliderControl(
                this, "specularConstant", 0, 2.0, lighting.getSpecularConstant(), 1.0, false); //NOI18N
        lighting.specularConstantProperty().bind(specularConstantEditor.valueProperty());
        vBox.getChildren().add(specularConstantEditor);

        final SliderControl specularExponentEditor = new SliderControl(
                this, "specularExponent", 0, 40.0, lighting.getSpecularExponent(), 1.0, false); //NOI18N
        lighting.specularExponentProperty().bind(specularExponentEditor.valueProperty());
        vBox.getChildren().add(specularExponentEditor);

        final SliderControl surfaceScaleEditor = new SliderControl(
                this, "surfaceScale", 0, 10.0, lighting.getSurfaceScale(), 1.0, false); //NOI18N
        lighting.surfaceScaleProperty().bind(surfaceScaleEditor.valueProperty());
        vBox.getChildren().add(surfaceScaleEditor);

        final LightControl lightControl = new LightControl(
                this, "light", lighting.getLight()); //NOI18N
        lighting.lightProperty().bind(lightControl.valueProperty());
        lightControl.liveUpdateProperty().addListener(new LightControlLiveUpdateListener(lightControl, this));
        vBox.getChildren().add(lightControl);

        props_vbox.getChildren().add(vBox);
    }

    private void makeMotionBlurUI(Effect effect) {
        assert effect instanceof MotionBlur;
        final MotionBlur motionBlur = (MotionBlur) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl angleEditor = new SliderControl(
                this, "angle", 0, 360.0, motionBlur.getAngle(), 1.0, false); //NOI18N
        motionBlur.angleProperty().bind(angleEditor.valueProperty());
        vBox.getChildren().add(angleEditor);

        final SliderControl radiusEditor = new SliderControl(
                this, "radius", 0, 63.0, motionBlur.getRadius(), 1.0, false); //NOI18N
        motionBlur.radiusProperty().bind(radiusEditor.valueProperty());
        vBox.getChildren().add(radiusEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makePerspectiveTransformUI(Effect effect) {
        assert effect instanceof PerspectiveTransform;
        final PerspectiveTransform perspectiveTransform = (PerspectiveTransform) effect;
        VBox vBox = new VBox(8.0);

        final DoubleTextFieldControl llxEditor = new DoubleTextFieldControl(
                this, "llx", -10.0, 10.0, perspectiveTransform.getLlx(), 1.0); //NOI18N
        perspectiveTransform.llxProperty().bind(llxEditor.valueProperty());
        vBox.getChildren().add(llxEditor);

        final DoubleTextFieldControl llyEditor = new DoubleTextFieldControl(
                this, "lly", -10.0, 10.0, perspectiveTransform.getLly(), 1.0); //NOI18N
        perspectiveTransform.llyProperty().bind(llyEditor.valueProperty());
        vBox.getChildren().add(llyEditor);

        final DoubleTextFieldControl lrxEditor = new DoubleTextFieldControl(
                this, "lrx", -10.0, 10.0, perspectiveTransform.getLrx(), 1.0); //NOI18N
        perspectiveTransform.lrxProperty().bind(lrxEditor.valueProperty());
        vBox.getChildren().add(lrxEditor);

        final DoubleTextFieldControl lryEditor = new DoubleTextFieldControl(
                this, "lry", -10.0, 10.0, perspectiveTransform.getLry(), 1.0); //NOI18N
        perspectiveTransform.lryProperty().bind(lryEditor.valueProperty());
        vBox.getChildren().add(lryEditor);

        final DoubleTextFieldControl ulxEditor = new DoubleTextFieldControl(
                this, "ulx", -10.0, 10.0, perspectiveTransform.getUlx(), 1.0); //NOI18N
        perspectiveTransform.ulxProperty().bind(ulxEditor.valueProperty());
        vBox.getChildren().add(ulxEditor);

        final DoubleTextFieldControl ulyEditor = new DoubleTextFieldControl(
                this, "uly", -10.0, 10.0, perspectiveTransform.getUly(), 1.0); //NOI18N
        perspectiveTransform.ulyProperty().bind(ulyEditor.valueProperty());
        vBox.getChildren().add(ulyEditor);

        final DoubleTextFieldControl urxEditor = new DoubleTextFieldControl(
                this, "urx", -10.0, 10.0, perspectiveTransform.getUrx(), 1.0); //NOI18N
        perspectiveTransform.urxProperty().bind(urxEditor.valueProperty());
        vBox.getChildren().add(urxEditor);

        final DoubleTextFieldControl uryEditor = new DoubleTextFieldControl(
                this, "ury", -10.0, 10.0, perspectiveTransform.getUry(), 1.0); //NOI18N
        perspectiveTransform.uryProperty().bind(uryEditor.valueProperty());
        vBox.getChildren().add(uryEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeReflectionUI(Effect effect) {
        assert effect instanceof Reflection;
        final Reflection reflection = (Reflection) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl bottomOpacityEditor = new SliderControl(
                this, "bottomOpacity", 0, 1.0, reflection.getBottomOpacity(), 0.1, false); //NOI18N
        reflection.bottomOpacityProperty().bind(bottomOpacityEditor.valueProperty());
        vBox.getChildren().add(bottomOpacityEditor);

        final SliderControl topOpacityEditor = new SliderControl(
                this, "topOpacity", 0, 1.0, reflection.getTopOpacity(), 0.1, false); //NOI18N
        reflection.topOpacityProperty().bind(topOpacityEditor.valueProperty());
        vBox.getChildren().add(topOpacityEditor);

        final DoubleTextFieldControl topOffsetEditor = new DoubleTextFieldControl(
                this, "topOffset", -10.0, 10.0, reflection.getTopOffset(), 1.0); //NOI18N
        reflection.topOffsetProperty().bind(topOffsetEditor.valueProperty());
        vBox.getChildren().add(topOffsetEditor);

        final SliderControl fractionEditor = new SliderControl(
                this, "fraction", 0, 1.0, reflection.getFraction(), 0.1, false); //NOI18N
        reflection.fractionProperty().bind(fractionEditor.valueProperty());
        vBox.getChildren().add(fractionEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeSepiaToneUI(Effect effect) {
        assert effect instanceof SepiaTone;
        final SepiaTone sepiaTone = (SepiaTone) effect;
        VBox vBox = new VBox(8.0);

        final SliderControl levelEditor = new SliderControl(
                this, "level", 0, 1.0, sepiaTone.getLevel(), 0.1, false); //NOI18N
        sepiaTone.levelProperty().bind(levelEditor.valueProperty());
        vBox.getChildren().add(levelEditor);

        props_vbox.getChildren().add(vBox);
    }

    private void makeShadowUI(Effect effect) {
        assert effect instanceof Shadow;
        final Shadow shadow = (Shadow) effect;
        VBox vBox = new VBox(8.0);

        final EnumControl<BlurType> blurTypeEditor = new EnumControl<>(
                this, "blurType", BlurType.values(), shadow.getBlurType()); //NOI18N
        shadow.blurTypeProperty().bind(blurTypeEditor.valueProperty());
        vBox.getChildren().add(blurTypeEditor);

        final SliderControl widthEditor = new SliderControl(
                this, "width", 0, 255.0, shadow.getWidth(), 1.0, false); //NOI18N
        shadow.widthProperty().bind(widthEditor.valueProperty());
        vBox.getChildren().add(widthEditor);

        final SliderControl heightEditor = new SliderControl(
                this, "height", 0, 255.0, shadow.getHeight(), 1.0, false); //NOI18N
        shadow.heightProperty().bind(heightEditor.valueProperty());
        vBox.getChildren().add(heightEditor);

        // setting radius equivalent to setting both width and height attributes to value of (2 * radius + 1)
        final SliderControl radiusEditor = new SliderControl(
                this, "radius", 0, 127.0, shadow.getRadius(), 1.0, false); //NOI18N
        shadow.radiusProperty().bind(radiusEditor.valueProperty());
        vBox.getChildren().add(radiusEditor);

        final PaintPicker colorPicker = new PaintPicker(paintPickerDelegate, Mode.COLOR);
        colorPicker.setPaintProperty(shadow.getColor());
        colorPicker.paintProperty().addListener(new ColorChangeListener(this, shadow));
        colorPicker.liveUpdateProperty().addListener(new PaintPickerLiveUpdateListener(colorPicker, this));
        vBox.getChildren().add(colorPicker);

        props_vbox.getChildren().add(vBox);
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class PaintPickerLiveUpdateListener implements ChangeListener<Boolean> {

        private final PaintPicker paintPicker;
        private final EffectPickerController effectPickerController;

        public PaintPickerLiveUpdateListener(
                PaintPicker paintPicker,
                EffectPickerController effectPickerController) {
            this.paintPicker = paintPicker;
            this.effectPickerController = effectPickerController;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
            effectPickerController.setLiveUpdate(paintPicker.isLiveUpdate());
        }
    }
    
    private static class LightControlLiveUpdateListener implements ChangeListener<Boolean> {

        private final LightControl lightControl;
        private final EffectPickerController effectPickerController;

        public LightControlLiveUpdateListener(
                LightControl lightControl,
                EffectPickerController effectPickerController) {
            this.lightControl = lightControl;
            this.effectPickerController = effectPickerController;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
            effectPickerController.setLiveUpdate(lightControl.isLiveUpdate());
        }
    }
    
    private static class ColorChangeListener implements ChangeListener<Paint> {

        private final EffectPickerController effectPickerController;
        private final Effect effect;

        public ColorChangeListener(EffectPickerController effectPickerController, Effect effect) {
            assert effect instanceof DropShadow
                    || effect instanceof InnerShadow
                    || effect instanceof Shadow;
            this.effectPickerController = effectPickerController;
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
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        }
    }

    private static class PaintChangeListener implements ChangeListener<Paint> {

        private final EffectPickerController effectPickerController;
        private final ColorInput colorInput;

        public PaintChangeListener(EffectPickerController effectPickerController, ColorInput colorInput) {
            this.effectPickerController = effectPickerController;
            this.colorInput = colorInput;
        }

        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            colorInput.setPaint(newValue);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        }
    }
}
