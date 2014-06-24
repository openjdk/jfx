/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.css;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.css.converters.EnumConverter;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Methods for creating instances of StyleableProperty with corresponding CssMetaData created behind the scenes.
 * These methods greatly reduce the amount of boiler-plate code needed to implement the StyleableProperty
 * and CssMetaData.
 * These methods take a Function&lt;? extends Styleable, StyleableProperty&lt;?&gt;&gt; which returns a
 * reference to the property itself. See the example below.
 * <p>StyleablePropertyFactory cannot be constructed. Use the {@link #getInstance()} method to get the singleton instance.</p>
 * <code><pre>
 public class MyButton extends Button {

     MyButton(String labelText) {
         super(labelText);
         getStyleClass().add("my-button");
     }

     // Typical JavaFX property implementation
     public ObservableValue{@literal <}Boolean{@literal >} selectedProperty() { return (ObservableValue{@literal <}Boolean{@literal >})selected; }
     public boolean isSelected() { return selected.getValue(); }
     public void setSelected(boolean isSelected) { selected.setValue(isSelected); }

     // StyleableProperty implementation reduced to one line
     private final StyleableProperty{@literal <}Boolean{@literal >} selected =
         StyleablePropertyFactory.getInstance().createStyleableBooleanProperty(this, "selected", "-my-selected", s -{@literal >} ((MyButton) s).selected);

     @Override
     public List{@literal <}CssMetaData{@literal <}? extends Styleable, ?{@literal >}{@literal >} getControlCssMetaData() {
         return StyleablePropertyFactory.getInstance().getCssMetaData(this);
     }

 }
 * </pre></code>
 * <p><span class="simpleTagLabel">Caveats:</span></p>
 * The only option for creating a StyleableProperty with a number value is to create a StyleableProperty&lt;Number&gt;</Number>.
 * The return value from the <code>getValue()</code> method of the StyleableProperty is a Number. Therefore,
 * the <code>get</code> method of the JavaFX property needs to call the correct value method for the return type.
 * For example,
 * <code><pre>
     public ObservableValue<Double> offsetProperty() { return (ObservableValue<Double>)offset; }
     public Double getOffset() { return offset.getValue().doubleValue(); }
     public void setOffset(Double value) { offset.setValue(value); }
     private final StyleableProperty<Number> offset = StyleablePropertyFactory.getInstance().createStyleableNumberProperty(this, "offset", "-my-offset", s -> ((MyButton)s).offset);
 * <br>
 * </pre></code>
 * @since JavaFX 8.0.40
 */
public final class StyleablePropertyFactory {

    private static class Holder {
        static final StyleablePropertyFactory INSTANCE = new StyleablePropertyFactory();
    }

    /* @return The single instance of StyleablePropertyFactory */
    public static StyleablePropertyFactory getInstance() {
        return Holder.INSTANCE;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // StyleableProperty<Boolean>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<Boolean> createStyleableBooleanProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Boolean>> function,
            Boolean initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Boolean> cssMetaData = (CssMetaData<S,Boolean>)
                sharedData.getCssMetaData(cssProperty, prop -> createBooleanCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableBooleanProperty(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable> StyleableProperty<Boolean> createStyleableBooleanProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Boolean>> function,
            Boolean initialValue) {
        return createStyleableBooleanProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt;. The initialValue and inherit flag default to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Boolean> createStyleableBooleanProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Boolean>> function) {
        return createStyleableBooleanProperty(styleable, propertyName, cssProperty, function, false, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Color>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Color&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Color&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */    
    public <S extends Styleable> StyleableProperty<Color> createStyleableColorProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Color>> function,
            Color initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Color> cssMetaData = (CssMetaData<S,Color>)
                sharedData.getCssMetaData(cssProperty, prop -> createColorCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<Color>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Color&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Color&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable> StyleableProperty<Color> createStyleableColorProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Color>> function,
            Color initialValue) {
        return createStyleableColorProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Color&gt;. The initial value defaults to Color.BLACK and the 
     * inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Color&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Color> createStyleableColorProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Color>> function) {
        return createStyleableColorProperty(styleable, propertyName, cssProperty, function, Color.BLACK, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Effect>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Effect&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable, E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function,
            E initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,E> cssMetaData = (CssMetaData<S,E>)
                sharedData.getCssMetaData(cssProperty, prop -> createEffectCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<E>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Effect&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable, E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function,
            E initialValue) {
        return createStyleableEffectProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Effect&gt;. The initial value is null and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     */
    public <S extends Styleable, E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function) {
        return createStyleableEffectProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<? extends Enum<?>>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; with initial value and inherit flag. 
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example, 
     * <code><pre>
     *     StyleableProperty&lt;Orientation&gt; orientation =
     *         StyleablePropertyFactory.getInstance().createStyleableEnumProperty(
     *             this, 
     *             "orientation", 
     *             "-my-orientation", 
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class,
     *             Orientation.HORIZONTAL,
     *             false);
     * </pre></code>
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable, E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function,
            @NotNull Class<E> enumClass,
            E initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,E> cssMetaData = (CssMetaData<S,E>)
                sharedData.getCssMetaData(cssProperty, prop -> createEnumCssMetaData(enumClass, prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<E>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; with initial value. The inherit flag defaults to false. 
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example, 
     * <code><pre>
     *     StyleableProperty&lt;Orientation&gt; orientation =
     *         StyleablePropertyFactory.getInstance().createStyleableEnumProperty(
     *             this, 
     *             "orientation", 
     *             "-my-orientation", 
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class,
     *             Orientation.HORIZONTAL);
     * </pre></code>
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable, E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function,
            @NotNull Class<E> enumClass,
            E initialValue) {
        return createStyleableEnumProperty(styleable, propertyName, cssProperty, function, enumClass, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;. The initial value is null and inherit flag defaults to false. 
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example, 
     * <code><pre>
     *     StyleableProperty&lt;Orientation&gt; orientation =
     *         StyleablePropertyFactory.getInstance().createStyleableEnumProperty(
     *             this, 
     *             "orientation", 
     *             "-my-orientation", 
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class);
     * </pre></code>
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     */
    public <S extends Styleable, E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<E>> function,
            @NotNull Class<E> enumClass) {
        return createStyleableEnumProperty(styleable, propertyName, cssProperty, function, enumClass, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Font>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Font&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<Font> createStyleableFontProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Font>> function,
            Font initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Font> cssMetaData = (CssMetaData<S,Font>)
                sharedData.getCssMetaData(cssProperty, prop -> createFontCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<Font>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Font&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable> StyleableProperty<Font> createStyleableFontProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Font>> function,
            Font initialValue) {
        return createStyleableFontProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /** Create a StyleableProperty&lt;Font&gt; with initial value of {@link javafx.scene.text.Font#getDefault() default font} and inherit false. */
    /**
     * Create a StyleableProperty&lt;Font&gt;. The initial value defaults to {@link javafx.scene.text.Font#getDefault()}
     * and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Font> createStyleableFontProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Font>> function) {
        return createStyleableFontProperty(styleable, propertyName, cssProperty, function, Font.getDefault(), false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Insets>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Inset&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Inset&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Inset&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<Insets> createStyleableInsetsProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Insets>> function,
            Insets initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Insets> cssMetaData = (CssMetaData<S,Insets>)
                sharedData.getCssMetaData(cssProperty, prop -> createInsetsCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<Insets>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Inset&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Inset&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Inset&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable> StyleableProperty<Insets> createStyleableInsetsProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Insets>> function,
            Insets initialValue) {
        return createStyleableInsetsProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Inset&gt;. The initial value is {@link Insets#EMPTY} and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Inset&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Inset&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Insets> createStyleableInsetsProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Insets>> function) {
        return createStyleableInsetsProperty(styleable, propertyName, cssProperty, function, Insets.EMPTY, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Paint>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Paint&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<Paint> createStyleablePaintProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Paint>> function,
            Paint initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Paint> cssMetaData = (CssMetaData<S,Paint>)
                sharedData.getCssMetaData(cssProperty, prop -> createPaintCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<Paint>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Paint&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     */
    public <S extends Styleable> StyleableProperty<Paint> createStyleablePaintProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Paint>> function,
            Paint initialValue) {
        return createStyleablePaintProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Paint&gt;. The initial value defautls to Color.BLACK and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Paint> createStyleablePaintProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Paint>> function) {
        return createStyleablePaintProperty(styleable, propertyName, cssProperty, function, Color.BLACK, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Number>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Number&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     */
    public <S extends Styleable> StyleableProperty<Number> createStyleableNumberProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Number>> function,
            Number initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,Number> cssMetaData = (CssMetaData<S,Number>)
                sharedData.getCssMetaData(cssProperty, prop -> createSizeCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Number&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     */
    public <S extends Styleable> StyleableProperty<Number> createStyleableNumberProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Number>> function,
            Number initialValue) {
        return createStyleableNumberProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Number&gt;. The initial value defaults to zero. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<Number> createStyleableNumberProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<Number>> function) {
        return createStyleableNumberProperty(styleable, propertyName, cssProperty, function, 0d, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<String>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableStringProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function,
            String initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,String> cssMetaData = (CssMetaData<S,String>)
                sharedData.getCssMetaData(cssProperty, prop -> createStringCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableObjectProperty<String>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableStringProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function,
            String initialValue) {
        return createStyleableStringProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt;. The initial value defaults to null and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableStringProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function) {
        return createStyleableStringProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<String> where String is a URL
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value and inherit flag. Here, the String value represents
     * a URL converted from a <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("<path>").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value. 
     * @param inherits Whether or not the CSS style can be inherited by child nodes                     
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableUrlProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function,
            String initialValue,
            boolean inherits) {

        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);

        @SuppressWarnings("unchecked")
        CssMetaData<S,String> cssMetaData = (CssMetaData<S,String>)
                sharedData.getCssMetaData(cssProperty, prop -> createUrlCssMetaData(prop, function, initialValue, inherits));

        return new SimpleStyleableStringProperty(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("<path>").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableUrlProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function,
            String initialValue) {
        return createStyleableUrlProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("<path>").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     */
    public <S extends Styleable> StyleableProperty<String> createStyleableUrlProperty(
            @NotNull S styleable,
            @NotNull String propertyName,
            @NotNull String cssProperty,
            @NotNull Function<S, StyleableProperty<String>> function) {
        return createStyleableUrlProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    /**
     * Get the CssMetaData for the given Styleable. For a Node other than a Control, this method should be
     * called from the {@link javafx.css.Styleable#getCssMetaData()} method. For a Control, this method should be called
     * from the {@link javafx.scene.control.Control#getControlCssMetaData()} method.
     * @param styleable The Styleable, typically the 'this' reference.
     */
    public <S extends Styleable> List<CssMetaData<? extends Styleable, ?>> getCssMetaData(@NotNull S styleable) {
        SharedCssMetaData sharedData = getSharedCssMetaData(styleable);
        return sharedData.getCssMetaData();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // StyleablePropertyFactory support - there should be one method for each StyleConverter                        //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static <S extends Styleable> CssMetaData<S, Boolean>
    createBooleanCssMetaData(final String property, final Function<S,StyleableProperty<Boolean>> function, final boolean initialValue, final boolean inherits)
    {
        final StyleConverter<String,Boolean> converter = StyleConverter.getBooleanConverter();
        return new SimpleCssMetaData<S, Boolean>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<S, Color>
    createColorCssMetaData(final String property, final Function<S,StyleableProperty<Color>> function, final Color initialValue, final boolean inherits)
    {
        final StyleConverter<String,Color> converter = StyleConverter.getColorConverter();
        return new SimpleCssMetaData<S, Color>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable, E extends Effect> CssMetaData<S, E>
    createEffectCssMetaData(final String property, final Function<S,StyleableProperty<E>> function, final E initialValue, final boolean inherits)
    {
        final StyleConverter<ParsedValue[], Effect> converter = StyleConverter.getEffectConverter();
        return new SimpleCssMetaData(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable, E extends Enum<E>> CssMetaData<S, E>
    createEnumCssMetaData(Class<? extends Enum> enumClass, final String property, final Function<S,StyleableProperty<E>> function, final E initialValue, final boolean inherits)
    {
        final EnumConverter<E> converter = new EnumConverter(enumClass);
        return new SimpleCssMetaData<S, E>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, Font>
    createFontCssMetaData(final String property, final Function<S,StyleableProperty<Font>> function, final Font initialValue, final boolean inherits)
    {
        final StyleConverter<ParsedValue[],Font> converter = StyleConverter.getFontConverter();
        return new SimpleCssMetaData<S, Font>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, Insets>
    createInsetsCssMetaData(final String property, final Function<S,StyleableProperty<Insets>> function, final Insets initialValue, final boolean inherits)
    {
        final StyleConverter<ParsedValue[],Insets> converter = StyleConverter.getInsetsConverter();
        return new SimpleCssMetaData<S, Insets>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, Paint>
    createPaintCssMetaData(final String property, final Function<S,StyleableProperty<Paint>> function, final Paint initialValue, final boolean inherits)
    {
        final StyleConverter<ParsedValue<?, Paint>,Paint> converter = StyleConverter.getPaintConverter();
        return new SimpleCssMetaData<S, Paint>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, Number>
    createSizeCssMetaData(final String property, final Function<S,StyleableProperty<Number>> function, final Number initialValue, final boolean inherits)
    {
        final StyleConverter<?,Number> converter = StyleConverter.getSizeConverter();
        return new SimpleCssMetaData<S, Number>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, String>
    createStringCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue, final boolean inherits)
    {
        final StyleConverter<String,String> converter = StyleConverter.getStringConverter();
        return new SimpleCssMetaData<S, String>(property, function, converter, initialValue, inherits);
    }

    static <S extends Styleable> CssMetaData<? extends Styleable, String>
    createUrlCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue, final boolean inherits)
    {
        final StyleConverter<ParsedValue[],String> converter = StyleConverter.getUrlConverter();
        return new SimpleCssMetaData<S, String>(property, function, converter, initialValue, inherits);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // SimpleCssMetaData is an implementation of CssMetaData that uses a Function<S, StyleableProperty<V>>          //
    // to get the StyleableProperty from the Styleable. This is the function that is passed in on the
    // various create methods.
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class SimpleCssMetaData<S extends Styleable,V> extends CssMetaData<S,V> {

        SimpleCssMetaData(
                final String property,
                final Function<S, StyleableProperty<V>> function,
                final StyleConverter<?, V> converter,
                final V initialValue,
                final boolean inherits)
        {
            super(property, converter, initialValue, inherits);
            this.function = function;
        }

        private final Function<S,StyleableProperty<V>> function;

        public boolean isSettable(S styleable) {
            final StyleableProperty<V> prop = getStyleableProperty(styleable);
            if (prop instanceof Property) {
                return !((Property)prop).isBound();
            }
            // can't set this property if getStyleableProperty returns null!
            return prop != null;
        }

        /** {@inheritDoc} */
        @Override
        public StyleableProperty<V> getStyleableProperty(S styleable) {
            if (styleable != null) {
                StyleableProperty<V> property = function.apply(styleable);
                return property;
            }
            return null;
        }

    }

    // for testing only
    static void clearDataForTesting() {
        sharedDataMap.clear();
    }

    /** container for CssMetaData that is shared by the same class */
    private static class SharedCssMetaData {

        private final Map<String, CssMetaData<? extends Styleable,?>> metaDataMap;
        private List<CssMetaData<? extends Styleable,?>> metaDataList = null;

        private CssMetaData<? extends Styleable,?> getCssMetaData(String cssProperty, Function<String, CssMetaData<? extends Styleable,?>> createFunction) {
            CssMetaData<? extends Styleable,?> cssMetaData = metaDataMap.get(cssProperty);
            if (cssMetaData == null && createFunction != null) {
                cssMetaData = createFunction.apply(cssProperty);
                metaDataMap.put(cssProperty, cssMetaData);
            }
            return cssMetaData;
        }

        private SharedCssMetaData(Class<? extends Styleable> styleableClass) {

            metaDataMap = new HashMap<>();

            Class<?> clazz = styleableClass.getSuperclass();
            if (clazz != null) {

                Method method = null;

                while (clazz != Object.class && method == null) {
                    try {
                        method = clazz.getDeclaredMethod("getClassCssMetaData");
                    } catch (NoSuchMethodException nsme) {
                        clazz = clazz.getSuperclass();
                    }
                }

                if (method != null) {
                    try {

                        @SuppressWarnings({"unchecked"}) // we know the method returns the type cast
                        final List<CssMetaData<? extends Styleable, ?>> list =
                                (List<CssMetaData<? extends Styleable, ?>>) method.invoke(null);

                        if (list != null) {
                            for (CssMetaData<? extends Styleable, ?> datum : list) {
                                metaDataMap.put(datum.getProperty(), datum);
                            }
                        }

                    } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                    }
                }
            }

        }

        private List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            if (metaDataList == null) {
                metaDataList = Collections.unmodifiableList(new ArrayList<>(metaDataMap.values()));
            }
            return metaDataList;
        }

    }

    private static Map<Class<? extends Styleable>, SharedCssMetaData> sharedDataMap = new HashMap<>();

    private static <S extends Styleable> SharedCssMetaData getSharedCssMetaData(S styleable) {

        if (styleable == null) {
            throw new IllegalArgumentException("styleable is null");
        }

        Class<? extends Styleable> c = styleable.getClass();
        SharedCssMetaData sharedData = sharedDataMap.get(c);
        if (sharedData == null) {
            sharedData = new SharedCssMetaData(c);
            sharedDataMap.put(c,sharedData);
        }
        return sharedData;
    }

    private StyleablePropertyFactory() {}
}
