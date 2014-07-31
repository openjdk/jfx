package javafx.css;

import com.sun.javafx.property.PropertyReference;
import javafx.scene.paint.LinearGradient;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class StyleablePropertyFactoryTest {

    private static class Data<T> {

        final PropertyReference propertyReference;
        final String style;
        final T value;
        final Matcher<T> matcher;

        Data(String name, String style, T value) {
            this(name,style,value,CoreMatchers.equalTo(value));
        }

        Data(String name, String style, T value, Matcher<T> matcher) {
            this.propertyReference = new PropertyReference(MyStyleable.class, name);
            this.style = style;
            this.value = value;
            this.matcher = matcher;
        }
    }

    private final Data data;

    public StyleablePropertyFactoryTest(Data data) {
        this.data = data;
    }

    @Parameterized.Parameters
    public static Collection<Data[]> data() {

        return Arrays.asList(new Data[][]{
                {new Data("myBoolean", "-my-boolean: true;", Boolean.TRUE)},
                {new Data("myColor", "-my-color: red;", Color.RED)},
                {new Data("myEffect", "-my-effect: innershadow(gaussian, red, 10, .5, 1, 1);",
                        new InnerShadow(BlurType.GAUSSIAN, Color.RED, 10, .5, 1, 1),
                        new BaseMatcher<InnerShadow>() {
                            @Override
                            public boolean matches(Object o) {
                                InnerShadow actual = (InnerShadow)o;
                                return (actual.getBlurType() == BlurType.GAUSSIAN &&
                                        actual.getColor().equals(Color.RED) &&
                                        Double.compare(actual.getRadius(),10d) ==  0 &&
                                        Double.compare(actual.getChoke(),.5d) ==  0 &&
                                        Double.compare(actual.getOffsetX(),1d) ==  0 &&
                                        Double.compare(actual.getOffsetY(),1d) ==  0);
                            }
                            @Override
                            public void describeTo(Description description) {
                                description.appendText("InnerShadow(BlurType.GAUSSIAN, Color.RED, 10, .5, 1, 1)");
                            }
                        })
                },
                {new Data("myPos", "-my-pos: bottom-right;", Pos.BOTTOM_RIGHT)},
                {new Data("myFont", "-my-font: 18 system;", Font.font("system", 18))},
// Pending RT-37727
//                {new Data("myInsets", "-my-insets: 1 2 3 4;", new Insets(1,2,3,4))},
                {new Data("myPaint", "-my-paint: linear-gradient(from 0% 0% to 100% 100%, red 0%, black 100%);",
                        new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE,new Stop[] { new Stop(0,Color.RED), new Stop(1,Color.BLACK) }))
                },
                {new Data("myNumber", "-my-number: 2em;", Font.getDefault().getSize()*2)},
                {new Data("myString", "-my-string: \"yaba daba do\";", "yaba daba do")},
                {new Data("myUrl", "-my-url: url('http://www.oracle.com');", "http://www.oracle.com")}
        });

    }

    @Test
    public void theTest() {
        MyStyleable styleable = new MyStyleable();
        styleable.setStyle(data.style);

        Scene scene = new Scene(styleable);
        styleable.applyCss();

        ReadOnlyProperty prop = data.propertyReference.getProperty(styleable);
        assertThat(prop.getValue(), data.matcher);
    }

    public static class MyStyleable extends Group {

        public MyStyleable() {
        }

        private static final StyleablePropertyFactory fac = new StyleablePropertyFactory<>(null);

        public ObservableValue<Boolean> myBooleanProperty () { return (ObservableValue<Boolean>) myBoolean; }
        public Boolean getMyBoolean() { return myBoolean.getValue(); }
        public void setMyBoolean(Boolean value) { myBoolean.setValue(value); }
        private final StyleableProperty<Boolean> myBoolean = fac.createStyleableBooleanProperty(this, "myBoolean", "-my-boolean", s -> ((MyStyleable) s).myBoolean);

        public ObservableValue<Color> myColorProperty () { return (ObservableValue<Color>) myColor; }
        public Color getMyColor() { return myColor.getValue(); }
        public void setMyColor(Color value) { myColor.setValue(value); }
        private final StyleableProperty<Color> myColor = fac.createStyleableColorProperty(this, "myColor", "-my-color", s -> ((MyStyleable) s).myColor);

        public ObservableValue<Effect> myEffectProperty () { return (ObservableValue<Effect>) myEffect; }
        public Effect getMyEffect() { return myEffect.getValue(); }
        public void setMyEffect(Effect value) { myEffect.setValue(value); }
        private final StyleableProperty<Effect> myEffect = fac.createStyleableEffectProperty(this, "myEffect", "-my-effect", s -> ((MyStyleable) s).myEffect);

        public ObservableValue<Pos> myPosProperty () { return (ObservableValue<Pos>) myPos; }
        public Pos getMyPos() { return myPos.getValue(); }
        public void setMyPos(Pos value) { myPos.setValue(value); }
        private final StyleableProperty<Pos> myPos = fac.createStyleableEnumProperty(this, "myPos", "-my-pos", s -> ((MyStyleable) s).myPos, Pos.class);

        public ObservableValue<Font> myFontProperty () { return (ObservableValue<Font>) myFont; }
        public Font getMyFont() { return myFont.getValue(); }
        public void setMyFont(Font value) { myFont.setValue(value); }
        private final StyleableProperty<Font> myFont = fac.createStyleableFontProperty(this, "myFont", "-my-font", s -> ((MyStyleable) s).myFont);

        public ObservableValue<Insets> myInsetsProperty () { return (ObservableValue<Insets>) myInsets; }
        public Insets getMyInsets() { return myInsets.getValue(); }
        public void setMyInsets(Insets value) { myInsets.setValue(value); }
        private final StyleableProperty<Insets> myInsets = fac.createStyleableInsetsProperty(this, "myInsets", "-my-insets", s -> ((MyStyleable) s).myInsets);

        public ObservableValue<Paint> myPaintProperty () { return (ObservableValue<Paint>) myPaint; }
        public Paint getMyPaint() { return myPaint.getValue(); }
        public void setMyPaint(Paint value) { myPaint.setValue(value); }
        private final StyleableProperty<Paint> myPaint = fac.createStyleablePaintProperty(this, "myPaint", "-my-paint", s -> ((MyStyleable) s).myPaint);

        public ObservableValue<Double> myNumberProperty () { return (ObservableValue<Double>) myNumber; }
        public Double getMyNumber() { return myNumber.getValue().doubleValue(); }
        public void setMyNumber(Double value) { myNumber.setValue(value); }
        private final StyleableProperty<Number> myNumber = fac.createStyleableNumberProperty(this, "myNumber", "-my-number", s -> ((MyStyleable) s).myNumber);

        public ObservableValue<String> myStringProperty () { return (ObservableValue<String>) myString; }
        public String getMyString() { return myString.getValue(); }
        public void setMyString(String value) { myString.setValue(value); }
        private final StyleableProperty<String> myString = fac.createStyleableStringProperty(this, "myString", "-my-string", s -> ((MyStyleable) s).myString);

        public ObservableValue<String> myUrlProperty () { return (ObservableValue<String>) myUrl; }
        public String getMyUrl() { return myUrl.getValue(); }
        public void setMyUrl(String value) { myUrl.setValue(value); }
        private final StyleableProperty<String> myUrl = fac.createStyleableUrlProperty(this, "myUrl", "-my-url", s -> ((MyStyleable) s).myUrl);

        @Override
        public String getTypeSelector() {
            return "MyStyleable";
        }

        @Override
        public Styleable getStyleableParent() {
            return null;
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return fac.getCssMetaData();
        }
    }
}
