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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StyleablePropertyFactory_createMethod_Test {

    @Before public void setup() {
        MyStyleable.styleablePropertyFactory.clearDataForTesting();
        MyStyleable1.styleablePropertyFactory.clearDataForTesting();
        MyStyleable2.styleablePropertyFactory.clearDataForTesting();
        MyStyleableEnum.styleablePropertyFactory.clearDataForTesting();
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

    private final Data data;
    public StyleablePropertyFactory_createMethod_Test(Data data) {
        this.data = data;
    }

    @Parameters
    public static Collection<Data[]> data() {

        return Arrays.asList(new Data[][] {
                { new Data("createStyleableBooleanProperty", StyleConverter.getBooleanConverter(), Boolean.TRUE) },
                { new Data("createStyleableColorProperty",   StyleConverter.getColorConverter(), Color.YELLOW)   },
                { new Data("createStyleableEffectProperty",  StyleConverter.getEffectConverter(), new InnerShadow(10d, Color.RED)) },
                { new Data("createStyleableEnumProperty", StyleConverter.getEnumConverter(Pos.class), Pos.CENTER) },
                { new Data("createStyleableFontProperty", StyleConverter.getFontConverter(), Font.font(18)) },
                { new Data("createStyleableInsetsProperty", StyleConverter.getInsetsConverter(), new Insets(1,1,1,1)) },
                { new Data("createStyleableNumberProperty", StyleConverter.getSizeConverter(), Double.valueOf(42d)) },
                { new Data("createStyleablePaintProperty", StyleConverter.getPaintConverter(), Color.BLUE) },
                { new Data("createStyleableStringProperty", StyleConverter.getStringConverter(), "ABC") },
                { new Data("createStyleableUrlProperty", StyleConverter.getUrlConverter(), "http://oracle.com") }
        });

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
        assertEquals(styleable.getProp().getCssMetaData().toString(), Boolean.valueOf(inherits), styleable.getProp().getCssMetaData().isInherits());

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

    @Test
    public void theTest() {

        if ("createStyleableEnumProperty".equals(data.createMethodName)) {
            testEnum();
        } else {
            testOther();
        }

    }

    void testEnum() {
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

    void testOther() {
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
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleable1.styleablePropertyFactory; }

    }

    private static class MyStyleable2<T> extends MyStyleable<T> {
        MyStyleable2() { super(); }
        static final StyleablePropertyFactory<MyStyleable2> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleable2.styleablePropertyFactory; }

    }

    private static class MyStyleableEnum<T extends Enum<T>> extends MyStyleable<T> {
        MyStyleableEnum() { super(); }
        static final StyleablePropertyFactory<MyStyleableEnum> styleablePropertyFactory = new StyleablePropertyFactory<>(null);
        protected StyleablePropertyFactory<? extends MyStyleable> getFactory() { return MyStyleableEnum.styleablePropertyFactory; }
    }
}
