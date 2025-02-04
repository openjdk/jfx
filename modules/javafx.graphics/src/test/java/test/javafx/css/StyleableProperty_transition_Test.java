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
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableFloatProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableLongProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.SimpleStyleableStringProperty;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.ColorConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.converter.StringConverter;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundShim;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.javafx.pgstub.StubToolkit;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static test.util.ReflectionUtils.*;

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

    static final CssMetaData<Styleable, String> stringPropertyMetadata = new CssMetaData<>(
            "-fx-string-property", StringConverter.getInstance(), null) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<String> getStyleableProperty(Styleable styleable) { return stringProperty; }
    };

    static final CssMetaData<Styleable, Color> interpolatableObjectPropertyMetadata = new CssMetaData<>(
            "-fx-interpolatable-property", ColorConverter.getInstance(), Color.RED) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Color> getStyleableProperty(Styleable styleable) {
            return interpolatableObjectProperty;
        }
    };

    static final CssMetaData<Styleable, Background> componentTransitionableObjectPropertyMetadata = new CssMetaData<>(
            "-fx-component-transitionable-property", BackgroundShim.getConverter(),
            Background.fill(Color.RED), false, Background.getClassCssMetaData()) {
        @Override public boolean isSettable(Styleable styleable) { return true; }
        @Override public StyleableProperty<Background> getStyleableProperty(Styleable styleable) {
            return componentTransitionableObjectProperty;
        }
    };

    static final Duration ONE_SECOND = Duration.seconds(1);

    static final Group testBean = new Group() {
        {
            NodeHelper.getTransitionProperty(this).setValue(new TransitionDefinition[] {
                new TransitionDefinition("-fx-boolean-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-double-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-float-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-integer-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-long-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-string-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-interpolatable-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR),
                new TransitionDefinition("-fx-component-transitionable-property", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR)
            });
        }
    };

    static StyleableBooleanProperty booleanProperty;
    static StyleableDoubleProperty doubleProperty;
    static StyleableFloatProperty floatProperty;
    static StyleableIntegerProperty integerProperty;
    static StyleableLongProperty longProperty;
    static StyleableStringProperty stringProperty;
    static StyleableObjectProperty<Color> interpolatableObjectProperty;
    static StyleableObjectProperty<Background> componentTransitionableObjectProperty;

    @SuppressWarnings("rawtypes")
    record TestRun(StyleableProperty property, String fieldName, Object defaultValue, Object newValue) {}

    static Stream<TestRun> transitionParameters() {
        booleanProperty = new SimpleStyleableBooleanProperty(booleanPropertyMetadata, testBean, null);
        doubleProperty = new SimpleStyleableDoubleProperty(doublePropertyMetadata, testBean, null);
        floatProperty = new SimpleStyleableFloatProperty(floatPropertyMetadata, testBean, null);
        integerProperty = new SimpleStyleableIntegerProperty(integerPropertyMetadata, testBean, null);
        longProperty = new SimpleStyleableLongProperty(longPropertyMetadata, testBean, null);
        stringProperty = new SimpleStyleableStringProperty(stringPropertyMetadata, testBean, null);
        interpolatableObjectProperty = new SimpleStyleableObjectProperty<>(
            interpolatableObjectPropertyMetadata, testBean, null, Color.RED);
        componentTransitionableObjectProperty = new SimpleStyleableObjectProperty<>(
            componentTransitionableObjectPropertyMetadata, testBean, null, Background.fill(Color.RED));

        return Stream.of(
            new TestRun(booleanProperty, "mediator", false, true),
            new TestRun(doubleProperty, "mediator", 0, 1),
            new TestRun(floatProperty, "mediator", 0, 1),
            new TestRun(integerProperty, "mediator", 0, 1),
            new TestRun(longProperty, "mediator", 0, 1),
            new TestRun(stringProperty, "mediator", "foo", "bar"),
            new TestRun(interpolatableObjectProperty, "controller", Color.RED, Color.GREEN),
            new TestRun(componentTransitionableObjectProperty, "controller",
                        Background.fill(Color.RED), Background.fill(Color.GREEN))
        );
    }

    StubToolkit toolkit;
    Scene scene;
    Stage stage;

    void setAnimationTime(long time) {
        toolkit.setCurrentTime(time);
        toolkit.handleAnimation();
    }

    @BeforeEach
    void setup() {
        toolkit = (StubToolkit)Toolkit.getToolkit();
        scene = new Scene(new Group());
        stage = new Stage();
        stage.setScene(scene);
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
        ((Group)scene.getRoot()).getChildren().setAll(testBean);

        // Setting a value for the first time doesn't start a transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator1 = getFieldValue(testRun.property, testRun.fieldName);
        assertNull(mediator1);
        stage.show();

        // Start the transition. This adds it to the list of running transitions.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator2 = getFieldValue(testRun.property, testRun.fieldName);
        assertNotNull(mediator2);

        // The next call to applyStyle() has the same target value as the last one,
        // making it redundant. No new transition is started.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator3 = getFieldValue(testRun.property, testRun.fieldName);
        assertSame(mediator2, mediator3);
    }

    @ParameterizedTest
    @MethodSource("transitionParameters")
    @SuppressWarnings("unchecked")
    void testReversingTransitionIsNotDiscarded(TestRun testRun) {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);

        // Setting a value for the first time doesn't start a transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator1 = getFieldValue(testRun.property, testRun.fieldName);
        assertNull(mediator1);
        stage.show();

        // Start the transition. This adds it to the list of running transitions.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.newValue);
        var mediator2 = getFieldValue(testRun.property, testRun.fieldName);
        assertNotNull(mediator2);

        // The next call to applyStyle() has a different target value as the last one,
        // which makes this a reversing transition.
        testRun.property.applyStyle(StyleOrigin.USER, testRun.defaultValue);
        var mediator3 = getFieldValue(testRun.property, testRun.fieldName);
        assertNotNull(mediator3);
        assertNotSame(mediator2, mediator3);
    }

    @Test
    void testExistingTransitionOfComponentTransitionableIsPreserved() {
        var bean = new Group();
        ((Group)scene.getRoot()).getChildren().setAll(bean);
        var border1 = new Background(new BackgroundFill(Color.RED, new CornerRadii(5), Insets.EMPTY));
        var border2 = new Background(new BackgroundFill(Color.GREEN, new CornerRadii(10), Insets.EMPTY));
        var border3 = new Background(new BackgroundFill(Color.BLUE, new CornerRadii(10), Insets.EMPTY));
        var property = new SimpleStyleableObjectProperty<>(componentTransitionableObjectPropertyMetadata, bean, null);

        NodeHelper.getTransitionProperty(bean).setValue(new TransitionDefinition[] {
            new TransitionDefinition("-fx-background-color", Duration.seconds(1), Duration.ZERO, Interpolator.LINEAR),
            new TransitionDefinition("-fx-background-radius", Duration.seconds(1), Duration.ZERO, Interpolator.LINEAR)
        });

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, border1);
        stage.show();

        // Start the transition and capture a copy of the sub-property mediator list.
        // -fx-background-color will transition from RED to GREEN
        // -fx-background-radius will transition rom 5 to 10
        property.applyStyle(StyleOrigin.USER, border2);
        var oldMediators = List.copyOf((List<?>)getFieldValue(getFieldValue(property, "controller"), "mediators"));

        // Advance the animation time and start the second transition.
        // -fx-background-color will transition from (mix of RED/GREEN) to BLUE
        // -fx-background-radius will pick up the previous transition, because its target value is the same (10)
        setAnimationTime(500);
        property.applyStyle(StyleOrigin.USER, border3);
        var newMediators = (List<?>)getFieldValue(getFieldValue(property, "controller"), "mediators");

        // The result is that now we have a new mediator for -fx-background-color, but the same
        // mediator as in the previous transition for -fx-background-radius.
        assertEquals(2, oldMediators.size());
        assertEquals(2, newMediators.size());
        assertNotSame(oldMediators.get(0), newMediators.get(0)); // -fx-background-color
        assertSame(oldMediators.get(1), newMediators.get(1));    // -fx-background-radius
    }

    @Test
    void testIntegerTransitionsInRealNumberSpace() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableIntegerProperty(integerPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, 0);
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, 2);
        setAnimationTime(249);
        assertEquals(0, property.get());
        setAnimationTime(250);
        assertEquals(1, property.get());
        setAnimationTime(500);
        assertEquals(1, property.get());
        setAnimationTime(749);
        assertEquals(1, property.get());
        setAnimationTime(750);
        assertEquals(2, property.get());
    }

    @Test
    void testLongTransitionsInRealNumberSpace() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableLongProperty(longPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, 0);
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, 2);
        setAnimationTime(249);
        assertEquals(0, property.get());
        setAnimationTime(250);
        assertEquals(1, property.get());
        setAnimationTime(500);
        assertEquals(1, property.get());
        setAnimationTime(749);
        assertEquals(1, property.get());
        setAnimationTime(750);
        assertEquals(2, property.get());
    }

    @Test
    void testBooleanTransitionsAsDiscrete() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableBooleanProperty(booleanPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, false);
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, true);
        setAnimationTime(499);
        assertFalse(property.get());
        setAnimationTime(500);
        assertTrue(property.get());
    }

    @Test
    void testStringTransitionsAsDiscrete() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableStringProperty(stringPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, "foo");
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, "bar");
        setAnimationTime(499);
        assertEquals("foo", property.get());
        setAnimationTime(500);
        assertEquals("bar", property.get());

        // This is a reversing transition, so it only needs half the time to flip the value.
        property.applyStyle(StyleOrigin.USER, "foo");
        setAnimationTime(749);
        assertEquals("bar", property.get());
        setAnimationTime(750);
        assertEquals("foo", property.get());
    }

    @Test
    void testNonInterpolatableObjectTransitionsAsDiscrete() {
        enum Fruit { APPLE, ORANGE }

        CssMetaData<Styleable, Fruit> metadata = new CssMetaData<>(
                "-fx-fruit", StyleConverter.getEnumConverter(Fruit.class), Fruit.APPLE) {
            @Override public boolean isSettable(Styleable styleable) { return true; }
            @Override public StyleableProperty<Fruit> getStyleableProperty(Styleable styleable) {
                throw new UnsupportedOperationException();
            }
        };

        var bean = new Group();
        NodeHelper.getTransitionProperty(bean).setValue(new TransitionDefinition[] {
            new TransitionDefinition("-fx-fruit", ONE_SECOND, Duration.ZERO, Interpolator.LINEAR)
        });

        ((Group)scene.getRoot()).getChildren().setAll(bean);
        var property = new SimpleStyleableObjectProperty<Fruit>(metadata, bean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, Fruit.APPLE);
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, Fruit.ORANGE);
        setAnimationTime(499);
        assertSame(Fruit.APPLE, property.get());
        setAnimationTime(500);
        assertSame(Fruit.ORANGE, property.get());

        // This is a reversing transition, so it only needs half the time to flip the value.
        property.applyStyle(StyleOrigin.USER, Fruit.APPLE);
        setAnimationTime(749);
        assertSame(Fruit.ORANGE, property.get());
        setAnimationTime(750);
        assertSame(Fruit.APPLE, property.get());
    }

    @Test
    void testNullObjectTransitionsAsDiscrete_withInterpolatableValue() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableObjectProperty<>(interpolatableObjectPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, Color.RED);
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, null);
        setAnimationTime(499);
        assertSame(Color.RED, property.get());
        setAnimationTime(500);
        assertNull(property.get());

        // This is a reversing transition, so it only needs half the time to flip the value.
        property.applyStyle(StyleOrigin.USER, Color.RED);
        setAnimationTime(749);
        assertNull(property.get());
        setAnimationTime(750);
        assertSame(Color.RED, property.get());
    }

    @Test
    void testNullObjectTransitionsAsDiscrete_withComponentTransitionableValue() {
        ((Group)scene.getRoot()).getChildren().setAll(testBean);
        var property = new SimpleStyleableObjectProperty<>(componentTransitionableObjectPropertyMetadata, testBean, null);

        // Setting a value for the first time doesn't start a transition.
        setAnimationTime(0);
        property.applyStyle(StyleOrigin.USER, Background.fill(Color.RED));
        stage.show();

        // Start the transition and sample the outputs.
        property.applyStyle(StyleOrigin.USER, null);
        setAnimationTime(499);
        assertEquals(Background.fill(Color.RED), property.get());
        setAnimationTime(500);
        assertNull(property.get());

        // This is a reversing transition, so it only needs half the time to flip the value.
        property.applyStyle(StyleOrigin.USER, Background.fill(Color.RED));
        setAnimationTime(749);
        assertNull(property.get());
        setAnimationTime(750);
        assertEquals(Background.fill(Color.RED), property.get());
    }
}
