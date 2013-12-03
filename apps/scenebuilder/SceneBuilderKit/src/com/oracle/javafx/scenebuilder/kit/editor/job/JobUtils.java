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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

/**
 * This class defines getter and setter on FXOM objects without using the modify job.
 * Theses methods are used by jobs that creates new FXOM objects and sets some of their properties.
 * No need to use a job in this case as the undo action will remove the new added FXOM object.
 */
public class JobUtils {

    public static void addColumnConstraints(
            final FXOMDocument fxomDocument,
            final FXOMInstance gridPane,
            final FXOMInstance constraints, int index) {
        final PropertyName propertyName = new PropertyName("columnConstraints"); //NOI18N
        FXOMProperty property = gridPane.getProperties().get(propertyName);
        if (property == null) {
            property = new FXOMPropertyC(fxomDocument, propertyName);
        }
        if (property.getParentInstance() == null) {
            property.addToParentInstance(-1, gridPane);
        }
        assert property instanceof FXOMPropertyC;
        constraints.addToParentProperty(index, (FXOMPropertyC) property);
    }

    public static void addRowConstraints(
            final FXOMDocument fxomDocument,
            final FXOMInstance gridPane,
            final FXOMInstance constraints, int index) {
        final PropertyName propertyName = new PropertyName("rowConstraints"); //NOI18N
        FXOMProperty property = gridPane.getProperties().get(propertyName);
        if (property == null) {
            property = new FXOMPropertyC(fxomDocument, propertyName);
        }
        if (property.getParentInstance() == null) {
            property.addToParentInstance(-1, gridPane);
        }
        assert property instanceof FXOMPropertyC;
        constraints.addToParentProperty(index, (FXOMPropertyC) property);
    }

    public static boolean getFillHeight(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "fillHeight"); //NOI18N
        assert propertyMeta instanceof BooleanPropertyMetadata;
        return ((BooleanPropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setFillHeight(
            final FXOMInstance instance, final Class<?> clazz, final boolean value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "fillHeight"); //NOI18N
        assert propertyMeta instanceof BooleanPropertyMetadata;
        ((BooleanPropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static boolean getFillWidth(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "fillWidth"); //NOI18N
        assert propertyMeta instanceof BooleanPropertyMetadata;
        return ((BooleanPropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setFillWidth(
            final FXOMInstance instance, final Class<?> clazz, final boolean value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "fillWidth"); //NOI18N
        assert propertyMeta instanceof BooleanPropertyMetadata;
        ((BooleanPropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static String getHAlignment(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyName propertyName = new PropertyName("halignment"); //NOI18N
        final FXOMProperty property = instance.getProperties().get(propertyName);
        if (property == null) {
            return null;
        } else {
            final PropertyMetadata propertyMeta
                    = getPropertyMetadata(clazz, "halignment"); //NOI18N
            assert propertyMeta instanceof EnumerationPropertyMetadata;
            return ((EnumerationPropertyMetadata) propertyMeta).getValue(instance);
        }
    }

    public static void setHAlignment(
            final FXOMInstance instance, final Class<?> clazz, final String value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "halignment"); //NOI18N
        assert propertyMeta instanceof EnumerationPropertyMetadata;
        if (value != null) {
            ((EnumerationPropertyMetadata) propertyMeta).setValue(instance, value);
        }
    }

    public static String getHGrow(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyName propertyName = new PropertyName("hgrow"); //NOI18N
        final FXOMProperty property = instance.getProperties().get(propertyName);
        if (property == null) {
            return null;
        } else {
            final PropertyMetadata propertyMeta
                    = getPropertyMetadata(clazz, "hgrow"); //NOI18N
            assert propertyMeta instanceof EnumerationPropertyMetadata;
            return ((EnumerationPropertyMetadata) propertyMeta).getValue(instance);
        }
    }

    public static void setHGrow(
            final FXOMInstance instance, final Class<?> clazz, final String value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "hgrow"); //NOI18N
        assert propertyMeta instanceof EnumerationPropertyMetadata;
        if (value != null) {
            ((EnumerationPropertyMetadata) propertyMeta).setValue(instance, value);
        }
    }

    public static void setLayoutX(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "layoutX"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static void setLayoutY(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "layoutY"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getMaxHeight(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "maxHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setMaxHeight(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "maxHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getMaxWidth(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "maxWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setMaxWidth(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "maxWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getMinHeight(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "minHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setMinHeight(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "minHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getMinWidth(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "minWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setMinWidth(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "minWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static void setOrientation(
            final FXOMInstance instance, final Class<?> clazz, final String value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "orientation"); //NOI18N
        assert propertyMeta instanceof EnumerationPropertyMetadata;
        if (value != null) {
            ((EnumerationPropertyMetadata) propertyMeta).setValue(instance, value);
        }
    }

    public static double getPercentHeight(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "percentHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setPercentHeight(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "percentHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getPercentWidth(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "percentWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setPercentWidth(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "percentWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getPrefHeight(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setPrefHeight(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static void setPrefViewportHeight(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefViewportHeight"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static void setPrefViewportWidth(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefViewportWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static double getPrefWidth(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        return ((DoublePropertyMetadata) propertyMeta).getValue(instance);
    }

    public static void setPrefWidth(
            final FXOMInstance instance, final Class<?> clazz, final double value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "prefWidth"); //NOI18N
        assert propertyMeta instanceof DoublePropertyMetadata;
        ((DoublePropertyMetadata) propertyMeta).setValue(instance, value);
    }

    public static String getVAlignment(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyName propertyName = new PropertyName("valignment");  //NOI18N
        final FXOMProperty property = instance.getProperties().get(propertyName);
        if (property == null) {
            return null;
        } else {
            final PropertyMetadata propertyMeta
                    = getPropertyMetadata(clazz, "valignment"); //NOI18N
            assert propertyMeta instanceof EnumerationPropertyMetadata;
            return ((EnumerationPropertyMetadata) propertyMeta).getValue(instance);
        }
    }

    public static void setVAlignment(
            final FXOMInstance instance, final Class<?> clazz, final String value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "valignment"); //NOI18N
        assert propertyMeta instanceof EnumerationPropertyMetadata;
        if (value != null) {
            ((EnumerationPropertyMetadata) propertyMeta).setValue(instance, value);
        }
    }

    public static String getVGrow(
            final FXOMInstance instance, final Class<?> clazz) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyName propertyName = new PropertyName("vgrow");  //NOI18N
        final FXOMProperty property = instance.getProperties().get(propertyName);
        if (property == null) {
            return null;
        } else {
            final PropertyMetadata propertyMeta
                    = getPropertyMetadata(clazz, "vgrow"); //NOI18N
            assert propertyMeta instanceof EnumerationPropertyMetadata;
            return ((EnumerationPropertyMetadata) propertyMeta).getValue(instance);
        }
    }

    public static void setVGrow(
            final FXOMInstance instance, final Class<?> clazz, final String value) {
        assert instance != null && clazz != null;
        assert clazz.isAssignableFrom(instance.getDeclaredClass());
        final PropertyMetadata propertyMeta
                = getPropertyMetadata(clazz, "vgrow"); //NOI18N
        assert propertyMeta instanceof EnumerationPropertyMetadata;
        if (value != null) {
            ((EnumerationPropertyMetadata) propertyMeta).setValue(instance, value);
        }
    }

    /*
     * Private
     */
    private static PropertyMetadata getPropertyMetadata(
            final Class<?> componentClass, final String name) {

        final ComponentClassMetadata componentClassMetadata
                = Metadata.getMetadata().queryComponentMetadata(componentClass);
        final PropertyName propertyName = new PropertyName(name);
        return componentClassMetadata.lookupProperty(propertyName);
    }
}
