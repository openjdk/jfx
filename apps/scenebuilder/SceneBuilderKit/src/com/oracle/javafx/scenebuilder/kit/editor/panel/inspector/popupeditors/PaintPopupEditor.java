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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors;

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Rectangle;

/**
 * Popup editor for the Paint property.
 */
public class PaintPopupEditor extends PopupEditor {

    private final PaintPicker paintEditor = new PaintPicker();
    private final Rectangle graphic = new Rectangle(20, 10);

    private final ChangeListener<Paint> paintChangeListener = new ChangeListener<Paint>() {
        @Override
        public void changed(ObservableValue<? extends Paint> ov, Paint oldValue, Paint newValue) {
            final String valueAsString = getValueAsString(newValue);
            commitValue(newValue, valueAsString);
            displayValueAsString(valueAsString);
            graphic.setFill(newValue);
        }
    };

    @SuppressWarnings("LeakingThisInConstructor")
    public PaintPopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        plugEditor(this, paintEditor);
        popupMb.setGraphic(graphic);
    }

    @Override
    public void setPopupContentValue(Object value) {
//        if (value == null) {
//            paintEditor.setPaintProperty(Color.WHITE);
//        } else {
//            assert value instanceof Paint;
//            paintEditor.setPaintProperty((Paint) value);
//        }
        assert value == null || value instanceof Paint;
        paintEditor.paintProperty().removeListener(paintChangeListener);
        if (value != null) {
            final Paint paint = (Paint) value;
            paintEditor.setPaintProperty(paint);
        }
        paintEditor.paintProperty().addListener(paintChangeListener);
        graphic.setFill((Paint) value);
        // Update the menu button string
        final String valueAsString = getValueAsString((Paint) value);
        displayValueAsString(valueAsString);
    }

    @Override
    public void resetPopupContent() {
//        paintEditor.setPaintProperty(null);
        paintEditor.reset();
    }

    private String getValueAsString(final Paint paint) {
        if (paint == null) {
            return null;
        }
        if (paint instanceof LinearGradient
                || paint instanceof RadialGradient
                || paint instanceof ImagePattern) {
            return paint.getClass().getSimpleName();
        }
        assert paint instanceof Color;
        return ColorEncoder.encodeColor((Color) paint);
    }
}
