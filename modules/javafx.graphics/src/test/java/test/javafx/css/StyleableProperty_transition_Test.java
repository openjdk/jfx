/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css;

import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionMediator;
import com.sun.javafx.scene.NodeHelper;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableFloatProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableLongProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.ColorConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class StyleableProperty_transition_Test {

    static final CssMetaData<Styleable, Boolean> booleanPropertyMetadata = new CssMetaData<>(
            "-fx-boolean-property", BooleanConverter.getInstance(), false) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Boolean> getStyleableProperty(Styleable styleable) { return booleanProperty; }
    };

    static final CssMetaData<Styleable, Number> doublePropertyMetadata = new CssMetaData<>(
            "-fx-double-property", SizeConverter.getInstance(), 0) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Number> getStyleableProperty(Styleable styleable) { return doubleProperty; }
    };

    static final CssMetaData<Styleable, Number> floatPropertyMetadata = new CssMetaData<>(
            "-fx-float-property", SizeConverter.getInstance(), 0) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Number> getStyleableProperty(Styleable styleable) { return floatProperty; }
    };

    static final CssMetaData<Styleable, Number> integerPropertyMetadata = new CssMetaData<>(
            "-fx-integer-property", SizeConverter.getInstance(), 0) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Number> getStyleableProperty(Styleable styleable) { return integerProperty; }
    };

    static final CssMetaData<Styleable, Number> longPropertyMetadata = new CssMetaData<>(
            "-fx-long-property", SizeConverter.getInstance(), 0) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Number> getStyleableProperty(Styleable styleable) { return longProperty; }
    };

    static final CssMetaData<Styleable, Color> objectPropertyMetadata = new CssMetaData<>(
            "-fx-object-property", ColorConverter.getInstance(), Color.RED) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Color> getStyleableProperty(Styleable styleable) { return objectProperty; }
    };

    static final Group testBean = new Group() {
        {
            NodeHelper.getTransitionProperty(this).setValue(new TransitionDefinition[] {
                new TransitionDefinition("-fx-boolean-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-double-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-float-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-integer-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-long-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-object-property", Duration.ONE, Duration.ZERO, Interpolator.LINEAR)
            });
        }
    };

    static StyleableBooleanProperty booleanProperty;
    static StyleableDoubleProperty doubleProperty;
    static StyleableFloatProperty floatProperty;
    static StyleableIntegerProperty integerProperty;
    static StyleableLongProperty longProperty;
    static StyleableObjectProperty<Color> objectProperty;

    static TransitionMediator getTransitionMediator(StyleableProperty<?> property) {
        Function<Class<?>, Field> getField = cls -> {
            try {
                var field = cls.getDeclaredField("mediator");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                return null;
            }
        };

        Class<?> cls = property.getClass();
        while (cls != null) {
            Field field = getField.apply(cls);
            if (field != null) {
                try {
                    return (TransitionMediator)field.get(property);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }

            cls = cls.getSuperclass();
        }

        throw new AssertionError();
    }

    @SuppressWarnings("rawtypes")
    record TestRun(StyleableProperty property, Object defaultValue, Object newValue) {}

    static Stream<TestRun> transitionParameters() {
        booleanProperty = new SimpleStyleableBooleanProperty(booleanPropertyMetadata, testBean, null);
        doubleProperty = new SimpleStyleableDoubleProperty(doublePropertyMetadata, testBean, null);
        floatProperty = new SimpleStyleableFloatProperty(floatPropertyMetadata, testBean, null);
        integerProperty = new SimpleStyleableIntegerProperty(integerPropertyMetadata, testBean, null);
        longProperty = new SimpleStyleableLongProperty(longPropertyMetadata, testBean, null);
        objectProperty = new SimpleStyleableObjectProperty<>(objectPropertyMetadata, testBean, null, Color.RED);

        return Stream.of(
            new TestRun(booleanProperty, false, true),
            new TestRun(doubleProperty, 0, 1),
            new TestRun(floatProperty, 0, 1),
            new TestRun(integerProperty, 0, 1),
            new TestRun(longProperty, 0, 1),
            new TestRun(objectProperty, Color.RED, Color.GREEN)
        );
    }

    Scene scene;
    Stage stage;

    @BeforeEach
    void setup() {
        scene = new Scene(new Group(testBean));
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    void teardown() {
        ((Group)scene.getRoot()).getChildren().clear();
        stage.close();
    }

    @ParameterizedTest
    @MethodSource("transitionParameters")
    @SuppressWarnings("unchecked")
    void testRedundantTransitionIsDiscarded(TestRun testRun) {
        // Setting a value for the first time doesn't start a transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator1 = getTransitionMediator(testRun.property);
        assertNull(mediator1);

        // Start the transition. This adds it to the list of running transitions.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator2 = getTransitionMediator(testRun.property);
        assertNotNull(mediator2);

        // The next call to applyStyle() has the same target value as the last one,
        // making it redundant. No new transition is started.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator3 = getTransitionMediator(testRun.property);
        assertSame(mediator2, mediator3);
    }

    @ParameterizedTest
    @MethodSource("transitionParameters")
    @SuppressWarnings("unchecked")
    void testReversingTransitionIsNotDiscarded(TestRun testRun) {
        // Setting a value for the first time doesn't start a transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator1 = getTransitionMediator(testRun.property);
        assertNull(mediator1);

        // Start the transition. This adds it to the list of running transitions.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator2 = getTransitionMediator(testRun.property);
        assertNotNull(mediator2);

        // The next call to applyStyle() has a different target value as the last one,
        // which makes this a reversing transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator3 = getTransitionMediator(testRun.property);
        assertNotNull(mediator3);
        assertNotSame(mediator2, mediator3);
    }
}
