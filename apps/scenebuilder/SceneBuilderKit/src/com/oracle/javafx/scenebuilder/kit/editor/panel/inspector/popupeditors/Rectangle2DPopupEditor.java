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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors;

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.DoubleField;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Rectangle2D popup editor. Used for ImageView/MediaView viewPort property.
 */
public class Rectangle2DPopupEditor extends PopupEditor {

    @FXML
    DoubleField minXDf;
    @FXML
    DoubleField minYDf;
    @FXML
    DoubleField widthDf;
    @FXML
    DoubleField heightDf;

    DoubleField[] doubleFields = new DoubleField[4];
    private Parent root;
    private Rectangle2D rectangle2D;

    @SuppressWarnings("LeakingThisInConstructor")
    public Rectangle2DPopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
    }

    @Override
    public Object getValue() {
        Double[] values = new Double[4];
        int index = 0;
        for (DoubleField doubleField : doubleFields) {
            String val = doubleField.getText();
            if (val == null || val.isEmpty()) {
                val = "0"; //NOI18N
            } else {
                try {
                    Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    // should not happen, DoubleField should prevent any error
                    return null;
                }
            }
            values[index] = new Double(val);
            index++;
        }
        rectangle2D = new Rectangle2D(values[0], values[1], values[2], values[3]);
        return rectangle2D;
    }

    //
    // Interface from PopupEditor.
    // Methods called by PopupEditor.
    //
    @Override
    public void initializePopupContent() {
        root = EditorUtils.loadPopupFxml("Rectangle2DPopupEditor.fxml", this); //NOI18N
        doubleFields[0] = minXDf;
        doubleFields[1] = minYDf;
        doubleFields[2] = widthDf;
        doubleFields[3] = heightDf;
        for (DoubleField doubleField : doubleFields) {
            EventHandler<ActionEvent> valueListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    commitValue(getValue());
                }
            };
            setNumericEditorBehavior(this, doubleField, valueListener, false);
        }
    }

    @Override
    public String getPreviewString(Object value) {
        if (value == null) {
            return I18N.getString("inspector.rectangle2D.not.defined");
        }
        assert value instanceof Rectangle2D;
        Rectangle2D rectangle2DVal = (Rectangle2D) value;
        String valueAsString;
        if (isIndeterminate()) {
            valueAsString = "-"; //NOI18N
        } else {
            valueAsString = EditorUtils.valAsStr(rectangle2DVal.getMinX()) + "," //NOI18N
                    + EditorUtils.valAsStr(rectangle2DVal.getMinY())
                    + "  " + EditorUtils.valAsStr(rectangle2DVal.getWidth()) //NOI18N
                    + "x" + EditorUtils.valAsStr(rectangle2DVal.getHeight()); //NOI18N
        }
        return valueAsString;
    }

    @Override
    public void setPopupContentValue(Object value) {
        if (value == null) {
            rectangle2D = null;
            for (DoubleField doubleField : doubleFields) {
                doubleField.setText(""); //NOI18N
            }
        } else {
            assert value instanceof Rectangle2D;
            rectangle2D = (Rectangle2D) value;
            minXDf.setText(EditorUtils.valAsStr(rectangle2D.getMinX()));
            minYDf.setText(EditorUtils.valAsStr(rectangle2D.getMinY()));
            widthDf.setText(EditorUtils.valAsStr(rectangle2D.getWidth()));
            heightDf.setText(EditorUtils.valAsStr(rectangle2D.getHeight()));
        }
    }

    @Override
    public Node getPopupContentNode() {
        return root;
    }
}
