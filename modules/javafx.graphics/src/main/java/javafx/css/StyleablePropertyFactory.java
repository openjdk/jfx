/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.converter.EnumConverter;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Methods for creating instances of StyleableProperty with corresponding CssMetaData created behind the scenes.
 * These methods greatly reduce the amount of boiler-plate code needed to implement the StyleableProperty
 * and CssMetaData.
 * These methods take a Function&lt;? extends Styleable, StyleableProperty&lt;?&gt;&gt; which returns a
 * reference to the property itself. See the example below. Note that for efficient use of memory and for better
 * CSS performance, creating the <code>StyleablePropertyFactory</code> as a static member, as shown below, is recommended.
 * <pre><code>
 public final class MyButton extends Button {

     private static final {@literal StyleablePropertyFactory<MyButton>} FACTORY = new {@literal StyleablePropertyFactory<>}(Button.getClassCssMetaData());

     MyButton(String labelText) {
         super(labelText);
         getStyleClass().add("my-button");
     }

     // Typical JavaFX property implementation
     public {@literal ObservableValue<Boolean>} selectedProperty() { return ({@literal ObservableValue<Boolean>})selected; }
     public final boolean isSelected() { return selected.getValue(); }
     public final void setSelected(boolean isSelected) { selected.setValue(isSelected); }

     // StyleableProperty implementation reduced to one line
     private final {@literal StyleableProperty<Boolean>} selected =
         FACTORY.createStyleableBooleanProperty(this, "selected", "-my-selected", s {@literal ->} s.selected);

     {@literal @}Override
     public {@literal List<CssMetaData<? extends Styleable, ?>>} getControlCssMetaData() {
         return FACTORY.getCssMetaData();
     }

 }
 * </code></pre>
 * <p>The example above is the simplest use of <code>StyleablePropertyFactory</code>. But, this use does not provide the
 * static CssMetaData that is useful for {@code getClassCssMetaData()}, which is described in the javadoc for
 * {@link CssMetaData}. Static CssMetaData can, however, be created via <code>StyleablePropertyFactory</code> methods
 * and will be returned by the methods which create StyleableProperty instances, as the example below illustrates.
 * Note that the static method <code>getClassCssMetaData()</code> is a convention used throughout the JavaFX code base
 * but the <code>getClassCssMetaData()</code> method itself is not used at runtime.</p>
 * <pre><code>
 public final class MyButton extends Button {

     private static final {@literal StyleablePropertyFactory<MyButton>} FACTORY =
         new {@literal StyleablePropertyFactory<>}(Button.getClassCssMetaData());

     private static final {@literal CssMetaData<MyButton, Boolean>} SELECTED =
         FACTORY.createBooleanCssMetaData("-my-selected", s {@literal ->} s.selected, false, false);

     MyButton(String labelText) {
         super(labelText);
         getStyleClass().add("my-button");
     }

     // Typical JavaFX property implementation
     public {@literal ObservableValue<Boolean>} selectedProperty() { return ({@literal ObservableValue<Boolean>})selected; }
     public final boolean isSelected() { return selected.getValue(); }
     public final void setSelected(boolean isSelected) { selected.setValue(isSelected); }

     // StyleableProperty implementation reduced to one line
     private final {@literal StyleableProperty<Boolean>} selected =
         new SimpleStyleableBooleanProperty(SELECTED, this, "selected");

     public static {@literal List<CssMetaData<? extends Styleable, ?>>} getClassCssMetaData() {
         return FACTORY.getCssMetaData();
     }

     {@literal @}Override
     public {@literal List<CssMetaData<? extends Styleable, ?>>} getControlCssMetaData() {
         return FACTORY.getCssMetaData();
     }
 }
 * </code></pre>
 * <p>The same can be accomplished with an inner-class. The previous example called {@code new SimpleStyleableBooleanProperty}
 * to create the <code>selected</code> property. This example uses the factory to access the <code>CssMetaData</code>
 * that was created along with the anonymous inner-class. For all intents and purposes, the two examples are the same.</p>
 * <pre><code>
 public final class MyButton extends Button {

     private static final {@literal StyleablePropertyFactory<MyButton>} FACTORY =
         new {@literal StyleablePropertyFactory<>}(Button.getClassCssMetaData()) {
         {
             createBooleanCssMetaData("-my-selected", s {@literal ->} s.selected, false, false);
         }
     }


     MyButton(String labelText) {
         super(labelText);
         getStyleClass().add("my-button");
     }

     // Typical JavaFX property implementation
     public {@literal ObservableValue<Boolean>} selectedProperty() { return ({@literal ObservableValue<Boolean>})selected; }
     public final boolean isSelected() { return selected.getValue(); }
     public final void setSelected(boolean isSelected) { selected.setValue(isSelected); }

     // StyleableProperty implementation reduced to one line
     private final {@literal StyleableProperty<Boolean>} selected =
         new SimpleStyleableBooleanProperty(this, "selected", "my-selected");

     public static {@literal List<CssMetaData<? extends Styleable, ?>>} getClassCssMetaData() {
         return FACTORY.getCssMetaData();
     }

     {@literal @}Override
     public {@literal List<CssMetaData<? extends Styleable, ?>>} getControlCssMetaData() {
         return FACTORY.getCssMetaData();
     }
 }
 * </code></pre>
 * <p><span class="simpleTagLabel">Caveats:</span></p>
 * The only option for creating a StyleableProperty with a number value is to create a StyleableProperty&lt;Number&gt;.
 * The return value from the <code>getValue()</code> method of the StyleableProperty is a Number. Therefore,
 * the <code>get</code> method of the JavaFX property needs to call the correct <code>value</code> method for the return type.
 * For example,
 * <pre>{@code
     public ObservableValue<Double> offsetProperty() { return (ObservableValue<Double>)offset; }
     public Double getOffset() { return offset.getValue().doubleValue(); }
     public void setOffset(Double value) { offset.setValue(value); }
     private final StyleableProperty<Number> offset = FACTORY.createStyleableNumberProperty(this, "offset", "-my-offset", s -> ((MyButton)s).offset);
 * }</pre>
 * @param <S> The type of Styleable
 * @since JavaFX 8u40
 */
public class StyleablePropertyFactory<S extends Styleable> {

    /**
     * The constructor is passed the CssMetaData of the parent class of &lt;S&gt;, typically by calling the
     * static <code>getClassCssMetaData()</code> method of the parent.
     * @param parentCssMetaData The CssMetaData of the parent class of &lt;S&gt;, or null.
     */
    public StyleablePropertyFactory(List<CssMetaData<? extends Styleable, ?>> parentCssMetaData) {
        this.metaDataList = new ArrayList<>();
        this.unmodifiableMetaDataList = Collections.unmodifiableList(this.metaDataList);
        if (parentCssMetaData != null) this.metaDataList.addAll(parentCssMetaData);
        this.metaDataMap = new HashMap<>();
    }


    /**
     * Get the CssMetaData for the given Styleable. For a Node other than a Control, this method should be
     * called from the {@link javafx.css.Styleable#getCssMetaData()} method. For a Control, this method should be called
     * from the {@link javafx.scene.control.Control#getControlCssMetaData()} method.
     * @return the CssMetaData for the given Styleable
     */
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return unmodifiableMetaDataList;
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Boolean> createStyleableBooleanProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Boolean>> function,
            boolean initialValue,
            boolean inherits) {

        CssMetaData<S,Boolean> cssMetaData = createBooleanCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableBooleanProperty(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value
     */
    public final StyleableProperty<Boolean> createStyleableBooleanProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Boolean>> function,
            boolean initialValue) {
        return createStyleableBooleanProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt;. The initialValue and inherit flag default to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that was created by this method call.
     * @return a StyleableProperty created with default initialValue and inherit flag
     */
    public final StyleableProperty<Boolean> createStyleableBooleanProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Boolean>> function) {
        return createStyleableBooleanProperty(styleable, propertyName, cssProperty, function, false, false);
    }

    /**
     * Create a StyleableProperty&lt;Boolean&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Boolean&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Boolean> createStyleableBooleanProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Boolean> cssMetaData = (CssMetaData<S,Boolean>)getCssMetaData(Boolean.class, cssProperty);
        return new SimpleStyleableBooleanProperty(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Color> createStyleableColorProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Color>> function,
            Color initialValue,
            boolean inherits) {

        CssMetaData<S,Color> cssMetaData = createColorCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<Color>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Color&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Color&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value
     */
    public final StyleableProperty<Color> createStyleableColorProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Color>> function,
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
     * @return a StyleableProperty created with default initial value and inherit flag
     */
    public final StyleableProperty<Color> createStyleableColorProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Color>> function) {
        return createStyleableColorProperty(styleable, propertyName, cssProperty, function, Color.BLACK, false);
    }

    /**
     * Create a StyleableProperty&lt;Color&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Color&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Color> createStyleableColorProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Color> cssMetaData = (CssMetaData<S,Color>)getCssMetaData(Color.class, cssProperty);
        return new SimpleStyleableObjectProperty<Color>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Duration>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Duration&gt; with initial value and inherit flag.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Duration&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Duration> createStyleableDurationProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Duration>> function,
            Duration initialValue,
            boolean inherits) {

        CssMetaData<S,Duration> cssMetaData = createDurationCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<Duration>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Duration&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Duration&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Duration> createStyleableDurationProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Duration>> function,
            Duration initialValue) {
        return createStyleableDurationProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Duration&gt;. The initial value defaults to Duration.BLACK and the
     * inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Duration&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that was created by this method call.
     * @return a StyleableProperty created with default initial value and false inherit flag
     */
    public final StyleableProperty<Duration> createStyleableDurationProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Duration>> function) {
        return createStyleableDurationProperty(styleable, propertyName, cssProperty, function, Duration.UNKNOWN, false);
    }

    /**
     * Create a StyleableProperty&lt;Duration&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Duration&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Duration> createStyleableDurationProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Duration> cssMetaData = (CssMetaData<S,Duration>)getCssMetaData(Duration.class, cssProperty);
        return new SimpleStyleableObjectProperty<Duration>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<Effect>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;Effect&gt; with initial value and inherit flag.
     * @param <E> The type of StyleableProperty
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final <E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function,
            E initialValue,
            boolean inherits) {

        CssMetaData<S,E> cssMetaData = createEffectCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<E>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Effect&gt; with initial value. The inherit flag defaults to false.
     * @param <E> The StyleableProperty created with initial value and false inherit flag
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final <E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function,
            E initialValue) {
        return createStyleableEffectProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Effect&gt;. The initial value is null and the inherit flag defaults to false.
     * @param <E> The StyleableProperty created with null initial value and false inherit flag
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that was created by this method call.
     * @return a StyleableProperty created with null initial value and false inherit flag
     */
    public final <E extends Effect> StyleableProperty<E> createStyleableEffectProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function) {
        return createStyleableEffectProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    /**
     * Create a StyleableProperty&lt;Effect&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Effect&gt;
     * @param cssProperty The CSS property name
     * @return StyleableProperty created using previously created CssMetaData for the given <code>cssProperty</code>
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Effect> createStyleableEffectProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Effect> cssMetaData = (CssMetaData<S,Effect>)getCssMetaData(Effect.class, cssProperty);
        return new SimpleStyleableObjectProperty<Effect>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<? extends Enum<?>>
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; with initial value and inherit flag.
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example,
     * <pre><code>
     * {@literal
     *     private static final StyleablePropertyFactory<MyControl> FACTORY = new StyleablePropertyFactory<>();
     *     StyleableProperty<Orientation> orientation =
     *         FACTORY.createStyleableEnumProperty(
     *             this,
     *             "orientation",
     *             "-my-orientation",
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class,
     *             Orientation.HORIZONTAL,
     *             false);
     * }
     * </code></pre>
     * @param <E> The StyleableProperty created with initial value and inherit flag
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final <E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function,
            Class<E> enumClass,
            E initialValue,
            boolean inherits) {

        CssMetaData<S,E> cssMetaData = createEnumCssMetaData(enumClass, cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<E>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; with initial value. The inherit flag defaults to false.
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example,
     * <pre><code>
     * {@literal
     *     private static final StyleablePropertyFactory<MyControl> FACTORY = new StyleablePropertyFactory<>();
     *     StyleableProperty<Orientation> orientation =
     *         FACTORY.createStyleableEnumProperty(
     *             this,
     *             "orientation",
     *             "-my-orientation",
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class,
     *             Orientation.HORIZONTAL);
     * }
     * </code></pre>
     * @param <E> The StyleableProperty created with initial value and false inherit flag
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final <E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function,
            Class<E> enumClass,
            E initialValue) {
        return createStyleableEnumProperty(styleable, propertyName, cssProperty, function, enumClass, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;. The initial value is null and inherit flag defaults to false.
     * The <code>enumClass</code> parameter is the Class of the Enum that is the value of the property. For example,
     * <pre><code>
     * {@literal
     *     private static final StyleablePropertyFactory<MyControl> FACTORY = new StyleablePropertyFactory<>();
     *     StyleableProperty<Orientation> orientation =
     *         FACTORY.createStyleableEnumProperty(
     *             this,
     *             "orientation",
     *             "-my-orientation",
     *             s -> ((MyControl)s).orientation,
     *             Orientation.class);
     * }
     * </code></pre>
     * @param <E> The StyleableProperty created with null initial value and false inherit flag
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; that was created by this method call.
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @return a StyleableProperty created with null initial value and false inherit flag
     */
    public final <E extends Enum<E>> StyleableProperty<E> createStyleableEnumProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<E>> function,
            Class<E> enumClass) {
        return createStyleableEnumProperty(styleable, propertyName, cssProperty, function, enumClass, null, false);
    }

    /**
     * Create a StyleableProperty&lt;E extends Enum&lt;E&gt;&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param <E> The StyleableProperty created using previously created CssMetaData
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;
     * @param cssProperty The CSS property name
     * @param enumClass The Enum class that is the type of the StyleableProperty&lt;E extends Enum&lt;E&gt;&gt;.
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final <E extends Enum<E>> StyleableProperty<E> createStyleableEffectProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Class<E> enumClass) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,E> cssMetaData = (CssMetaData<S,E>)getCssMetaData(enumClass, cssProperty);
        return new SimpleStyleableObjectProperty<E>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Font> createStyleableFontProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Font>> function,
            Font initialValue,
            boolean inherits) {

        CssMetaData<S,Font> cssMetaData = createFontCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<Font>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Font&gt; with initial value. The inherit flag defaults to true.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and true inherit flag
     */
    public final StyleableProperty<Font> createStyleableFontProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Font>> function,
            Font initialValue) {
        return createStyleableFontProperty(styleable, propertyName, cssProperty, function, initialValue, true);
    }

    /**
     * Create a StyleableProperty&lt;Font&gt;. The initial value defaults to {@link javafx.scene.text.Font#getDefault()}
     * and the inherit flag defaults to true.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that was created by this method call.
     * @return a StyleableProperty created with default font initial value and true inherit flag
     */
    public final StyleableProperty<Font> createStyleableFontProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Font>> function) {
        return createStyleableFontProperty(styleable, propertyName, cssProperty, function, Font.getDefault(), true);
    }

    /**
     * Create a StyleableProperty&lt;Font&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Font&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Font> createStyleableFontProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Font> cssMetaData = (CssMetaData<S,Font>)getCssMetaData(Font.class, cssProperty);
        return new SimpleStyleableObjectProperty<Font>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Insets> createStyleableInsetsProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Insets>> function,
            Insets initialValue,
            boolean inherits) {

        CssMetaData<S,Insets> cssMetaData = createInsetsCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<Insets>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Inset&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Inset&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Inset&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Insets> createStyleableInsetsProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Insets>> function,
            Insets initialValue) {
        return createStyleableInsetsProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Inset&gt;. The initial value is {@link Insets#EMPTY} and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Inset&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Inset&gt; that was created by this method call.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Insets> createStyleableInsetsProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Insets>> function) {
        return createStyleableInsetsProperty(styleable, propertyName, cssProperty, function, Insets.EMPTY, false);
    }

    /**
     * Create a StyleableProperty&lt;Insets&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Insets&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Insets> createStyleableInsetsProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Insets> cssMetaData = (CssMetaData<S,Insets>)getCssMetaData(Insets.class, cssProperty);
        return new SimpleStyleableObjectProperty<Insets>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Paint> createStyleablePaintProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Paint>> function,
            Paint initialValue,
            boolean inherits) {

        CssMetaData<S,Paint> cssMetaData = createPaintCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<Paint>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Paint&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Paint> createStyleablePaintProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Paint>> function,
            Paint initialValue) {
        return createStyleablePaintProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Paint&gt;. The initial value defautls to Color.BLACK and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that was created by this method call.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Paint> createStyleablePaintProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Paint>> function) {
        return createStyleablePaintProperty(styleable, propertyName, cssProperty, function, Color.BLACK, false);
    }

    /**
     * Create a StyleableProperty&lt;Paint&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Paint&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Paint> createStyleablePaintProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Paint> cssMetaData = (CssMetaData<S,Paint>)getCssMetaData(Paint.class, cssProperty);
        return new SimpleStyleableObjectProperty<Paint>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<Number> createStyleableNumberProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Number>> function,
            Number initialValue,
            boolean inherits) {

        CssMetaData<S,Number> cssMetaData = createSizeCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableObjectProperty<>(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;Number&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<Number> createStyleableNumberProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Number>> function,
            Number initialValue) {
        return createStyleableNumberProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;Number&gt;. The initial value defaults to zero. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that was created by this method call.
     * @return a StyleableProperty created with zero initial value and false inherit flag
     */
    public final StyleableProperty<Number> createStyleableNumberProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<Number>> function) {
        return createStyleableNumberProperty(styleable, propertyName, cssProperty, function, 0d, false);
    }

    /**
     * Create a StyleableProperty&lt;Number&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;Number&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<Number> createStyleableNumberProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,Number> cssMetaData = (CssMetaData<S,Number>)getCssMetaData(Number.class, cssProperty);
        return new SimpleStyleableObjectProperty<Number>(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
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
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<String> createStyleableStringProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function,
            String initialValue,
            boolean inherits) {

        CssMetaData<S,String> cssMetaData = createStringCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableStringProperty(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<String> createStyleableStringProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function,
            String initialValue) {
        return createStyleableStringProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt;. The initial value defaults to null and the inherit flag defaults to false.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @return a StyleableProperty created with null initial value and false inherit flag
     */
    public final StyleableProperty<String> createStyleableStringProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function) {
        return createStyleableStringProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<String> createStyleableStringProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,String> cssMetaData = (CssMetaData<S,String>)getCssMetaData(String.class, cssProperty);
        return new SimpleStyleableStringProperty(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // create StyleableProperty<String> where String is a URL
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value and inherit flag. Here, the String value represents
     * a URL converted from a <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("{@literal <path>}").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a StyleableProperty created with initial value and inherit flag
     */
    public final StyleableProperty<String> createStyleableUrlProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function,
            String initialValue,
            boolean inherits) {

        CssMetaData<S,String> cssMetaData = createUrlCssMetaData(cssProperty, function, initialValue, inherits);
        return new SimpleStyleableStringProperty(cssMetaData, styleable, propertyName, initialValue);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("{@literal <path>}").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<String> createStyleableUrlProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function,
            String initialValue) {
        return createStyleableUrlProperty(styleable, propertyName, cssProperty, function, initialValue, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; with initial value. The inherit flag defaults to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("{@literal <path>}").
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @param function A function that returns the StyleableProperty&lt;String&gt; that was created by this method call.
     * @return a StyleableProperty created with initial value and false inherit flag
     */
    public final StyleableProperty<String> createStyleableUrlProperty(
            S styleable,
            String propertyName,
            String cssProperty,
            Function<S, StyleableProperty<String>> function) {
        return createStyleableUrlProperty(styleable, propertyName, cssProperty, function, null, false);
    }

    /**
     * Create a StyleableProperty&lt;String&gt; using previously created CssMetaData for the given <code>cssProperty</code>.
     * @param styleable The <code>this</code> reference of the returned property. This is also the property bean.
     * @param propertyName The field name of the StyleableProperty&lt;String&gt;
     * @param cssProperty The CSS property name
     * @return a StyleableProperty created using previously created CssMetaData
     * @throws java.lang.IllegalArgumentException if <code>cssProperty</code> is null or empty
     * @throws java.util.NoSuchElementException if the CssMetaData for <code>cssProperty</code> was not created prior to this method invocation
     */
    public final StyleableProperty<String> createStyleableUrlProperty(
            S styleable,
            String propertyName,
            String cssProperty) {

        if (cssProperty == null || cssProperty.isEmpty()) {
            throw new IllegalArgumentException("cssProperty cannot be null or empty string");
        }

        @SuppressWarnings("unchecked")
        CssMetaData<S,String> cssMetaData = (CssMetaData<S,String>)getCssMetaData(String.class, cssProperty);
        return new SimpleStyleableStringProperty(cssMetaData, styleable, propertyName, cssMetaData.getInitialValue(styleable));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Boolean>                                                                               //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Boolean&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Boolean>
    createBooleanCssMetaData(final String property, final Function<S,StyleableProperty<Boolean>> function, final boolean initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S, Boolean> cssMetaData =
                (CssMetaData<S, Boolean>)getCssMetaData(Boolean.class, property, key -> {
                    final StyleConverter<String, Boolean> converter = StyleConverter.getBooleanConverter();
                    return new SimpleCssMetaData<S, Boolean>(key, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Boolean&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Boolean>
    createBooleanCssMetaData(final String property, final Function<S,StyleableProperty<Boolean>> function, final boolean initialValue)
    {
        return createBooleanCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Boolean&gt; with initial value and inherit flag both defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Boolean&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with false initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Boolean>
    createBooleanCssMetaData(final String property, final Function<S,StyleableProperty<Boolean>> function)
    {
        return createBooleanCssMetaData(property, function, false, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Color>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Color&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Color>
    createColorCssMetaData(final String property, final Function<S,StyleableProperty<Color>> function, final Color initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S, Color> cssMetaData =
                (CssMetaData<S, Color>)getCssMetaData(Color.class, property, key -> {
                    final StyleConverter<String,Color> converter = StyleConverter.getColorConverter();
                    return new SimpleCssMetaData<S, Color>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Color&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Color>
    createColorCssMetaData(final String property, final Function<S,StyleableProperty<Color>> function, final Color initialValue)
    {
        return createColorCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Color&gt; with initial value of Color.BLACK, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Color&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Color>
    createColorCssMetaData(final String property, final Function<S,StyleableProperty<Color>> function)
    {
        return createColorCssMetaData(property, function, Color.BLACK, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Duration>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Duration&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Duration>
    createDurationCssMetaData(final String property, final Function<S,StyleableProperty<Duration>> function, final Duration initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
                CssMetaData<S, Duration> cssMetaData =
                (CssMetaData<S, Duration>)getCssMetaData(Duration.class, property, key -> {
                    final StyleConverter<?,Duration> converter = StyleConverter.getDurationConverter();
                    return new SimpleCssMetaData<S, Duration>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Duration&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Duration>
    createDurationCssMetaData(final String property, final Function<S,StyleableProperty<Duration>> function, final Duration initialValue)
    {
        return createDurationCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Duration&gt; with initial value of Duration.BLACK, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Duration&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Duration>
    createDurationCssMetaData(final String property, final Function<S,StyleableProperty<Duration>> function)
    {
        return createDurationCssMetaData(property, function, Duration.UNKNOWN, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Effect>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Effect&gt; with initial value, and inherit flag.
     * @param <E> The CssMetaData created with initial value and inherit flag
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Effect> CssMetaData<S, E>
    createEffectCssMetaData(final String property, final Function<S,StyleableProperty<E>> function, final E initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S, E> cssMetaData =
                (CssMetaData<S, E>)getCssMetaData(Effect.class, property, key -> {
                    final StyleConverter<ParsedValue[], Effect> converter = StyleConverter.getEffectConverter();
                    return new SimpleCssMetaData(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Effect&gt; with initial value, and inherit flag defaulting to false.
     * @param <E> The CssMetaData created with initial value and false inherit flag
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Effect> CssMetaData<S, E>
    createEffectCssMetaData(final String property, final Function<S,StyleableProperty<E>> function, final E initialValue) {
        return createEffectCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Effect&gt; with initial value of null, and inherit flag defaulting to false.
     * @param <E> The CssMetaData created with null initial value and false inherit flag
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Effect&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with null initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Effect> CssMetaData<S, E>
    createEffectCssMetaData(final String property, final Function<S,StyleableProperty<E>> function) {
        return createEffectCssMetaData(property, function, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Enum>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Enum&gt; with initial value, and inherit flag.
     * @param <E> The CssMetaData created with initial value and inherit flag
     * @param enumClass The Enum class that is the type of the CssMetaData&lt;E extends Enum&lt;E&gt;&gt;.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Enum&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Enum<E>> CssMetaData<S, E>
    createEnumCssMetaData(Class<? extends Enum> enumClass, final String property, final Function<S,StyleableProperty<E>> function, final E initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S, E> cssMetaData =
                (CssMetaData<S, E>)getCssMetaData(enumClass, property, key -> {
                    final EnumConverter<E> converter = new EnumConverter(enumClass);
                    return new SimpleCssMetaData<S, E>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Enum&gt; with initial value, and inherit flag defaulting to false.
     * @param <E> The CssMetaData created with initial value and false inherit flag
     * @param enumClass The Enum class that is the type of the CssMetaData&lt;E extends Enum&lt;E&gt;&gt;.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Enum&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Enum<E>> CssMetaData<S, E>
    createEnumCssMetaData(Class<? extends Enum> enumClass, final String property, final Function<S,StyleableProperty<E>> function, final E initialValue) {
        return createEnumCssMetaData(enumClass, property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Enum&gt; with initial value of null, and inherit flag defaulting to false.
     * @param <E> The CssMetaData created with null initial value and false inherit flag
     * @param enumClass The Enum class that is the type of the CssMetaData&lt;E extends Enum&lt;E&gt;&gt;.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Enum&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with null initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final <E extends Enum<E>> CssMetaData<S, E>
    createEnumCssMetaData(Class<? extends Enum> enumClass, final String property, final Function<S,StyleableProperty<E>> function) {
        return createEnumCssMetaData(enumClass, property, function, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Font>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Font&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Font>
    createFontCssMetaData(final String property, final Function<S,StyleableProperty<Font>> function, final Font initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,Font> cssMetaData =
                (CssMetaData<S,Font>)getCssMetaData(Font.class, property, key -> {
                    final StyleConverter<ParsedValue[],Font> converter = StyleConverter.getFontConverter();
                    return new SimpleCssMetaData<S, Font>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Font&gt; with initial value, and inherit flag defaulting to true.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and true inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Font>
    createFontCssMetaData(final String property, final Function<S,StyleableProperty<Font>> function, final Font initialValue) {
        return createFontCssMetaData(property, function, initialValue, true);
    }

    /**
     * Create a CssMetaData&lt;S, Font&gt; with initial value of {@link javafx.scene.text.Font#getDefault()}, and inherit flag defaulting to true.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Font&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and true inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Font>
    createFontCssMetaData(final String property, final Function<S,StyleableProperty<Font>> function) {
        return createFontCssMetaData(property, function, Font.getDefault(), true);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Insets>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Insets&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Insets&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Insets>
    createInsetsCssMetaData(final String property, final Function<S,StyleableProperty<Insets>> function, final Insets initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,Insets> cssMetaData =
                (CssMetaData<S,Insets>)getCssMetaData(Insets.class, property, key -> {
                    final StyleConverter<ParsedValue[],Insets> converter = StyleConverter.getInsetsConverter();
                    return new SimpleCssMetaData<S, Insets>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Insets&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Insets&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Insets>
    createInsetsCssMetaData(final String property, final Function<S,StyleableProperty<Insets>> function, final Insets initialValue)
    {
        return createInsetsCssMetaData(property, function, initialValue, false);

    }
    /**
     * Create a CssMetaData&lt;S, Insets&gt; with initial value of {@link Insets#EMPTY}, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Insets&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Insets>
    createInsetsCssMetaData(final String property, final Function<S,StyleableProperty<Insets>> function)
    {
        return createInsetsCssMetaData(property, function, Insets.EMPTY, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Paint>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Paint&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Paint>
    createPaintCssMetaData(final String property, final Function<S,StyleableProperty<Paint>> function, final Paint initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,Paint> cssMetaData =
                (CssMetaData<S,Paint>)getCssMetaData(Paint.class, property, key -> {
                    final StyleConverter<ParsedValue<?, Paint>,Paint> converter = StyleConverter.getPaintConverter();
                    return new SimpleCssMetaData<S, Paint>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Paint&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Paint>
    createPaintCssMetaData(final String property, final Function<S,StyleableProperty<Paint>> function, final Paint initialValue) {
        return createPaintCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Paint&gt; with initial value of Color.BLACK, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Paint&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Paint>
    createPaintCssMetaData(final String property, final Function<S,StyleableProperty<Paint>> function) {
        return createPaintCssMetaData(property, function, Color.BLACK, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, Number>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, Number&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Number>
    createSizeCssMetaData(final String property, final Function<S,StyleableProperty<Number>> function, final Number initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,Number> cssMetaData =
                (CssMetaData<S,Number>)getCssMetaData(Number.class, property, key -> {
                    final StyleConverter<?,Number> converter = StyleConverter.getSizeConverter();
                    return new SimpleCssMetaData<S, Number>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, Number&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Number>
    createSizeCssMetaData(final String property, final Function<S,StyleableProperty<Number>> function, final Number initialValue) {
        return createSizeCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, Number&gt; with initial value of <code>0d</code>, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;Number&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, Number>
    createSizeCssMetaData(final String property, final Function<S,StyleableProperty<Number>> function) {
        return createSizeCssMetaData(property, function, 0d, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, String>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value, and inherit flag.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createStringCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,String> cssMetaData =
                (CssMetaData<S,String>)getCssMetaData(String.class, property, key -> {
                    final StyleConverter<String,String> converter = StyleConverter.getStringConverter();
                    return new SimpleCssMetaData<S, String>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createStringCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue) {
        return createStringCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value of null, and inherit flag defaulting to false.
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with null initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createStringCssMetaData(final String property, final Function<S,StyleableProperty<String>> function) {
        return createStringCssMetaData(property, function, null, false);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                              //
    // create CssMetaData<S, String>                                                                                 //
    //                                                                                                              //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value, and inherit flag.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url({@literal "<path>"}).
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @param inherits Whether or not the CSS style can be inherited by child nodes
     * @return a CssMetaData created with initial value, and inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createUrlCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue, final boolean inherits)
    {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property cannot be null or empty string");
        }

        if (function == null) {
            throw new IllegalArgumentException("function cannot be null");
        }

        @SuppressWarnings("unchecked") // getCssMetaData checks and will throw a ClassCastException
        CssMetaData<S,String> cssMetaData =
                (CssMetaData<S,String>)getCssMetaData(java.net.URL.class, property, key -> {
                    final StyleConverter<ParsedValue[],String> converter = StyleConverter.getUrlConverter();
                    return new SimpleCssMetaData<S, String>(property, function, converter, initialValue, inherits);
                });
        return cssMetaData;
    }

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value, and inherit flag defaulting to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url({@literal "<path>"}).
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @param initialValue The initial value of the property. CSS may reset the property to this value.
     * @return a CssMetaData created with initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createUrlCssMetaData(final String property, final Function<S,StyleableProperty<String>> function, final String initialValue) {
        return createUrlCssMetaData(property, function, initialValue, false);
    }

    /**
     * Create a CssMetaData&lt;S, String&gt; with initial value of null, and inherit flag defaulting to false.
     * Here, the String value represents a URL converted from a
     * <a href="http://www.w3.org/TR/CSS21/syndata.html#uri">CSS</a> url("{@literal <path>}").
     * @param property The CSS property name.
     * @param function A function that returns the StyleableProperty&lt;String&gt; that corresponds to this CssMetaData.
     * @return a CssMetaData created with null initial value, and false inherit flag
     * @throws java.lang.IllegalArgumentException if <code>property</code> is null or an empty string, or <code>function</code> is null.
     */
    public final CssMetaData<S, String>
    createUrlCssMetaData(final String property, final Function<S,StyleableProperty<String>> function) {
        return createUrlCssMetaData(property, function, null, false);
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

        public final boolean isSettable(S styleable) {
            final StyleableProperty<V> prop = getStyleableProperty(styleable);
            if (prop instanceof Property) {
                return !((Property)prop).isBound();
            }
            // can't set this property if getStyleableProperty returns null!
            return prop != null;
        }

        /** {@inheritDoc} */
        @Override
        public final StyleableProperty<V> getStyleableProperty(S styleable) {
            if (styleable != null) {
                StyleableProperty<V> property = function.apply(styleable);
                return property;
            }
            return null;
        }

    }

    // for testing only
    void clearDataForTesting() {
        metaDataMap.clear();
        metaDataList.clear();
    }

    private CssMetaData<S, ?> getCssMetaData(final Class ofClass, String property) {
        return getCssMetaData(ofClass, property, null);
    }

    private CssMetaData<S, ?> getCssMetaData(final Class ofClass, String property, final Function<String,CssMetaData<S,?>> createFunction) {

        final String key = property.toLowerCase();

        Pair<Class,CssMetaData<S,?>> entry = metaDataMap.get(key);
        if (entry != null) {
            if (entry.getKey() == ofClass) {
                return entry.getValue();
            } else {
                throw new ClassCastException("CssMetaData value is not " + ofClass + ": " + entry.getValue());
            }
        } else if (createFunction == null) {
            throw new NoSuchElementException("No CssMetaData for " + key);
        }

        // Entry was null
        CssMetaData<S,?> cssMetaData = createFunction.apply(key);
        metaDataMap.put(key, new Pair(ofClass, cssMetaData));
        metaDataList.add(cssMetaData);
        return cssMetaData;
    }

    private final Map<String,Pair<Class,CssMetaData<S,?>>> metaDataMap;
    private final List<CssMetaData<? extends Styleable,?>> unmodifiableMetaDataList;
    private final List<CssMetaData<? extends Styleable,?>> metaDataList;

}
