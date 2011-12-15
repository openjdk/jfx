/*
 * Copyright (c) 2010, 2011, Oracle  and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Skin related tests for Control. This test case will construct
 * a control, skin, and tool tip but does not by default wire them
 * all together. This is done so that in the various tests, we can
 * set things up the way we want for the test.
 */
@Ignore
public class ControlSkinTest {
    private ControlStub c;
    private SkinStub<ControlStub> s;
    private Tooltip t;

    /**
     * Creates the control, skin, and tool tip but does not wire them up.
     * The tool tip does have a SkinStub created for it though, so the
     * tool tip is completely setup.
     * 
     * By necessity, however, the skin does have a reference back to the control.
     */
    @Before public void setUp() {
        c = new ControlStub();
        s = new SkinStub<ControlStub>(c);
        t = new Tooltip();
//        t.setSkin(new SkinStub<Tooltip>(t));
    }

    @Test public void controlWithNoSkinShouldReportNullEvenIfSkinKnowsAboutControl() {
        assertNull(c.getSkin());
    }

    @Test public void skinCanBeSetOnControlManually() {
        c.setSkin(s);
        assertSame(s, c.getSkin());
        assertTrue(c.getChildrenUnmodifiable().contains(s.getNode()));
        assertEquals(1, c.getChildrenUnmodifiable().size());
    }

    @Test public void skinCanBeReplacedOnControlManually() {
        c.setSkin(s);
        final Skin<ControlStub> s2 = new SkinStub<ControlStub>(c);
        c.setSkin(s2);
        assertSame(s2, c.getSkin());
        assertTrue(c.getChildrenUnmodifiable().contains(s2.getNode()));
        assertEquals(1, c.getChildrenUnmodifiable().size());
    }
    
    @Test public void disposeIsCalledOnReplacedSkin() {
        final boolean[] result = new boolean[1];
        s = new SkinStub<ControlStub>(c) {
            @Override public void dispose() {
                super.dispose();
                result[0] = true;
            }
        };
        c.setSkin(s);
        final Skin<ControlStub> s2 = new SkinStub<ControlStub>(c);
        c.setSkin(s2);
        assertTrue(result[0]);
    }
    
    @Ignore ("I need some means of being able to check whether CSS has been told this property is set manually")
    @Test public void whenSkinIsChangedCSSIsNotified() {
    }

    /*
     * Binding a skin is honored
     * Be sure to check that "dispose" is called on the old skin
     * Test that if skin is bound, then impl_cssSettable returns false
     */
//    @Test public void skinCanBeBound() {
//        ObjectProperty<SkinStub<ControlStub>> skin = new SimpleObjectProperty<SkinStub<ControlStub>>();
//        c.skinProperty().bind(skin);
//    }
    
    @Test public void shouldBeAbleToSpecifyTheSkinViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(c.skinClassNameProperty());
        styleable.set(c, "javafx.scene.control.ControlSkinTest$MySkinStub");
        assertTrue(c.getSkin() instanceof MySkinStub);
    }
    
    @Test public void impl_cssSetCalledTwiceWithTheSameValueHasNoEffectTheSecondTime() {
        StringProperty skinClassName = c.skinClassNameProperty();
        skinClassName.addListener(new InvalidationListener() {
            boolean calledOnce = false;
            @Override
            public void invalidated(Observable o) {
                if (calledOnce) org.junit.Assert.fail();
                calledOnce = true;
            }
        });
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(skinClassName);
        styleable.set(c, "javafx.scene.control.ControlSkinTest$MySkinStub");
        Skin<?> s = c.getSkin();
        styleable.set(c, "javafx.scene.control.ControlSkinTest$MySkinStub");
        assertSame(s, c.getSkin());
    }
    
    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreNullNames() {
        c.setSkin(s);
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(c.skinClassNameProperty());
        styleable.set(c, null); // indirectly calls loadSkinClass
        assertSame(s, c.getSkin()); // shouldn't have changed
    }
    
    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreEmptyStrings() {
        c.setSkin(s);
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(c.skinClassNameProperty());
        styleable.set(c, ""); // indirectly calls loadSkinClass
        assertSame(s, c.getSkin()); // shouldn't have changed
    }
    
    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreSkinsWithoutAProperConstructor() {
        c.setSkin(s);
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(c.skinClassNameProperty());
        styleable.set(c, "javafx.scene.control.ControlSkinTest$UnloadableSkinStub"); // indirectly calls loadSkinClass
        assertSame(s, c.getSkin()); // shouldn't have changed
    }
    
    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreBogusOrUnfindableSkins() {
        c.setSkin(s);
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(c.skinClassNameProperty());
        styleable.set(c, "javafx.scene.control.ControlSkinTest$FooSkinWhichDoesntExist"); // indirectly calls loadSkinClass
        assertSame(s, c.getSkin()); // shouldn't have changed
    }
    
    @Test public void controlWithoutSkinShouldHaveZeroSizes() {
        assertEquals(0f, c.minWidth(-1), 0f);
        assertEquals(0f, c.minHeight(-1), 0f);
        assertEquals(0f, c.prefWidth(-1), 0f);
        assertEquals(0f, c.prefHeight(-1), 0f);
        assertEquals(0f, c.maxWidth(-1), 0f);
        assertEquals(0f, c.maxHeight(-1), 0f);
    }
    
    public static final class MySkinStub<C extends ControlStub> extends SkinStub<C> {
        public MySkinStub(C c) {
            super(c);
        }
    }
    
    public static final class UnloadableSkinStub<C extends ControlStub> extends SkinStub<C> {
        public UnloadableSkinStub() {
            super(null);
        }
    }
}
