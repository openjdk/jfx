/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import java.util.List;
import java.util.function.Function;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.css.StyleablePropertyFactoryShim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class StyleablePropertyFactory_createMethod_Test {

    @BeforeEach
    public void setup() {
        StyleablePropertyFactoryShim.clearDataForTesting(MyStyleable.styleablePropertyFactory);
        StyleablePropertyFactoryShim.clearDataForTesting(MyStyleable1.styleablePropertyFactory);
        StyleablePropertyFactoryShim.clearDataForTesting(MyStyleable2.styleablePropertyFactory);
        StyleablePropertyFactoryShim.clearDataForTesting(MyStyleableEnum.styleablePropertyFactory);
    }

    private static class Data<T> {
        final String createMethodName;
        final StyleConverter converter;
        final T initialValue;

        Data(String createMethodName, StyleConverter converter, T initialValue) {
            this.createMethodName = createMethodName;
            this.converter = converter;
            this.initialValue = initialValue;
        }
    }

    public static Stream<Arguments> data() {

        return Stream.of(
            Arguments.of( new Data("createStyleableBooleanProperty", StyleConverter.getBooleanConverter(), Boolean.TRUE) ),
            Arguments.of( new Data("createStyleableColorProperty",   StyleConverter.getColorConverter(), Color.YELLOW)   ),
            Arguments.of( new Data("createStyleableDurationProperty",   StyleConverter.getDurationConverter(), Duration.millis(30))   ),
            Arguments.of( new Data("createStyleableEffectProperty",  StyleConverter.getEffectConverter(), new InnerShadow(10d, Color.RED)) ),
            Arguments.of( new Data("createStyleableEnumProperty", StyleConverter.getEnumConverter(Pos.class), Pos.CENTER) ),
            Arguments.of( new Data("createStyleableFontProperty", StyleConverter.getFontConverter(), Font.font(18)) ),
            Arguments.of( new Data("createStyleableInsetsProperty", StyleConverter.getInsetsConverter(), new Insets(1,1,1,1)) ),
            Arguments.of( new Data("createStyleableNumberProperty", StyleConverter.getSizeConverter(), Double.valueOf(42d)) ),
            Arguments.of( new Data("createStyleablePaintProperty", StyleConverter.getPaintConverter(), Color.BLUE) ),
            Arguments.of( new Data("createStyleableStringProperty", StyleConverter.getStringConverter(), "ABC") ),
            Arguments.of( new Data("createStyleableUrlProperty", StyleConverter.getUrlConverter(), "http://oracle.com") )
        );
    }

    void check(MyStyleable<?>  styleable, StyleConverter<?,?> converter, boolean inherits) {

        assertNotNull(styleable.getProp());
        assertSame(styleable, ((Property) styleable.getProp()).getBean());
        assertEquals("prop", ((Property) styleable.getProp()).getName());
        assertNotNull(styleable.getProp().getCssMetaData());
        assertEquals("-my-prop", styleable.getProp().getCssMetaData().getProperty());
        if (styleable instanceof MyStyleableEnum) {
            // for Enum, there is no static Enum converter instance, so assertSame fails
            assertEquals(converter, styleable.getProp().getCssMetaData().getConverter());
        } else {
            assertSame(converter, styleable.getProp().getCssMetaData().getConverter());
        }

        assertEquals(styleable.getProp().getCssMetaData().getInitialValue(null), styleable.getProp().getValue());
        assertEquals(Boolean.valueOf(inherits), styleable.getProp().getCssMetaData().isInherits(), styleable.getProp().getCssMetaData().toString());

        List<CssMetaData<? extends Styleable,?>> list = styleable.getCssMetaData();
        assert list != null;
        assert list.isEmpty() == false;

        for(CssMetaData metaData : list) {
            if ("-my-prop".equals(metaData.getProperty())) {
                StyleableProperty prop = metaData.getStyleableProperty(styleable);
                assert  prop == styleable.getProp();
                assert prop.getCssMetaData() == metaData;
            }
        }

    }

    void check(MyStyleable<?> styleable1, MyStyleable<?> styleable2, StyleConverter converter, boolean inherits) {
        assertNotSame(styleable1, styleable2);
        check(styleable1, converter, inherits);
        check(styleable2, converter, inherits);
        assertNotSame(styleable1.getProp(), styleable2.getProp());
        assertSame(styleable1.getProp().getCssMetaData(), styleable2.getProp().getCssMetaData());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void theTest(Data data) {

        if ("createStyleableEnumProperty".equals(data.createMethodName)) {
            testEnum(data);
        } else {
            testOther(data);
        }

    }

    void testEnum(Data data) {
        final Class c = data.initialValue.getClass();
        // zero
        final MyStyleable<?> styleable = new MyStyleableEnum();
        styleable.setProp(data.createMethodName, c);

        final MyStyleable<?> other = new MyStyleableEnum();
        other.setProp(data.createMethodName, c);

        check(styleable, other, data.converter, false);

//        // one
//        final MyStyleable<?> styleable1 = new MyStyleable1();
//        styleable1.setProp(data.createMethodName, c, data.initialValue);
//
//        final MyStyleable<?> other1 = new MyStyleable1();
//        other1.setProp(data.createMethodName, c, data.initialValue);
//
//        check(styleable1, other1, data.converter, false);
//
//        assertNotSame(styleable.getProp().getCssMetaData(), styleable1.getProp().getCssMetaData());
//
//        // two
//        final MyStyleable<?> styleable2 = new MyStyleable2();
//        styleable2.setProp(data.createMethodName, c, data.initialValue, true);
//
//        final MyStyleable<?> other2 = new MyStyleable2();
//        other2.setProp(data.createMethodName, c, data.initialValue, true);
//
//        check(styleable2, other2, data.converter, true);
//
//        assertNotSame(styleable.getProp().getCssMetaData(), styleable2.getProp().getCssMetaData());
//        assertNotSame(styleable1.getProp().getCssMetaData(), styleable2.getProp().getCssMetaData());
    }

    void testOther(Data data) {
         // zero
        final MyStyleable<?> styleable = new MyStyleable();
        styleable.setProp(data.createMethodName);

        final MyStyleable<?> other = new MyStyleable();
        other.setProp(data.createMethodName);

        check(styleable, other, data.converter, data.converter == StyleConverter.getFontConverter());

        // one
        final MyStyleable<?> styleable1 = new MyStyleable1();
        styleable1.setProp(data.createMethodName, data.initialValue);

        final MyStyleable<?> other1 = new MyStyleable1();
        other1.setProp(data.createMethodName, data.initialValue);

        check(styleable1, other1, data.converter, data.converter == StyleConverter.getFontConverter());

        assertNotSame(styleable.getProp().getCssMetaData(), styleable1.getProp().getCssMetaData());

        // two
        final MyStyleable<?> styleable2 = new MyStyleable2();
        styleable2.setProp(data.createMethodName, data.initialValue, true);

        final MyStyleable<?> other2 = new MyStyleable2();
        other2.setProp(data.createMethodName, data.initialValue, true);

        check(styleable2, other2, data.converter, true);

        assertNotSame(styleable.getProp().getCssMetaData(), styleable2.getProp().getCssMetaData());
        assertNotSame(styleable1.getProp().getCssMetaData(), styleable2.getProp().getCssMetaData());
    }

    private static class MyStyleable<T> implements Styleable {

        MyStyleable() {
        }

        static final StyleablePropertyFactory<MyStyleable> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleable.styleablePropertyFactory; }

        protected StyleableProperty<T> prop = null;
        StyleableProperty<T> getProp() { return prop; }

        final protected Function<Styleable, StyleableProperty<T>> function = s -> ((MyStyleable)s).prop;

        void setProp(String createMethodName) {
            try {
                Method createMethod = StyleablePropertyFactory.class.getMethod(createMethodName, Styleable.class, String.class, String.class, Function.class);
                prop = (StyleableProperty<T>) createMethod.invoke(getFactory(), this, "prop", "-my-prop", function);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }

        void setProp(String createMethodName, Object initialValue) {
            try {
                Class c = null;
                switch(createMethodName) {
                    case "createStyleablePaintProperty": c = Paint.class; break;
                    case "createStyleableNumberProperty": c = Number.class; break;
                    case "createStyleableEffectProperty": c = Effect.class; break;
                    default:
                        if (initialValue instanceof Boolean) c = boolean.class;
                        else c = initialValue.getClass();
                        break;
                }
                Method createMethod = StyleablePropertyFactory.class.getMethod(createMethodName, Styleable.class, String.class, String.class, Function.class, c);
                prop = (StyleableProperty<T>)createMethod.invoke(getFactory(), this, "prop", "-my-prop", function, initialValue);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }

        void setProp(String createMethodName, Object initialValue, boolean inherits) {
            try {
                Class c = null;
                switch(createMethodName) {
                    case "createStyleablePaintProperty": c = Paint.class; break;
                    case "createStyleableNumberProperty": c = Number.class; break;
                    case "createStyleableEffectProperty": c = Effect.class; break;
                    default:
                        if (initialValue instanceof Boolean) c = boolean.class;
                        else c = initialValue.getClass();
                        break;
                }
                Method createMethod = StyleablePropertyFactory.class.getMethod(createMethodName, Styleable.class, String.class, String.class, Function.class, c, boolean.class);
                prop = (StyleableProperty<T>)createMethod.invoke(getFactory(), this, "prop", "-my-prop", function, initialValue, inherits);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        @Override
        public String getTypeSelector() {
            return "MyStyleable";
        }

        @Override
        public String getId() {
            return "my-styleable-id";
        }

        private ObservableList<String> styleClass = FXCollections.observableArrayList("my-styleable");
        @Override
        public ObservableList<String> getStyleClass() {
            return styleClass;
        }

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public Styleable getStyleableParent() {
            return null;
        }

        @Override
        public ObservableSet<PseudoClass> getPseudoClassStates() {
            return null;
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return getFactory().getCssMetaData();
        }
    }

    private static class MyStyleable1<T> extends MyStyleable<T> {
        MyStyleable1() { super(); }
        static final StyleablePropertyFactory<MyStyleable1> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        @Override
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleable1.styleablePropertyFactory; }

    }

    private static class MyStyleable2<T> extends MyStyleable<T> {
        MyStyleable2() { super(); }
        static final StyleablePropertyFactory<MyStyleable2> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        @Override
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleable2.styleablePropertyFactory; }

    }

    private static class MyStyleableEnum<T extends Enum<T>> extends MyStyleable<T> {
        MyStyleableEnum() { super(); }
        static final StyleablePropertyFactory<MyStyleableEnum> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        @Override
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleableEnum.styleablePropertyFactory; }
    }
}
