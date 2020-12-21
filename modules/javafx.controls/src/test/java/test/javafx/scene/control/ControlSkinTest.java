/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import javafx.css.StyleableProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javafx.scene.Scene;
import javafx.scene.control.ControlShim;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
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
     * Test that if skin is bound, then CssMetaData_isSettable returns false
     */
//    @Test public void skinCanBeBound() {
//        ObjectProperty<SkinStub<ControlStub>> skin = new SimpleObjectProperty<SkinStub<ControlStub>>();
//        c.skinProperty().bind(skin);
//    }


    @Test public void shouldBeAbleToSpecifyTheSkinViaCSS() {
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$MySkinStub");
        assertTrue(c.getSkin() instanceof MySkinStub);
    }

    @Test public void applyStyleCalledTwiceWithTheSameValueHasNoEffectTheSecondTime() {
        StringProperty skinClassName = ControlShim.skinClassNameProperty(c);
        skinClassName.addListener(new InvalidationListener() {
            boolean calledOnce = false;
            @Override
            public void invalidated(Observable o) {
                if (calledOnce) org.junit.Assert.fail();
                calledOnce = true;
            }
        });
        ((StyleableProperty)skinClassName).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$MySkinStub");
        Skin<?> s = c.getSkin();
        ((StyleableProperty)skinClassName).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$MySkinStub");
        assertSame(s, c.getSkin());
    }

    @Test public void shouldNotSeeErrorMessageWhenSettingTheSkinToNullViaCSS() {
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$MySkinStub");
        assertTrue(c.getSkin() instanceof MySkinStub);
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, null);
        assertTrue(c.getSkin() instanceof MySkinStub);
    }


    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreNullNames() {
        c.setSkin(s);
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, null);
        assertSame(s, c.getSkin()); // shouldn't have changed
    }

    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreEmptyStrings() {
        c.setSkin(s);
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "");
        assertSame(s, c.getSkin()); // shouldn't have changed
    }

    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreSkinsWithoutAProperConstructor() {
        c.setSkin(s);
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$UnloadableSkinStub");
        assertSame(s, c.getSkin()); // shouldn't have changed
    }

    @Ignore ("This spits out annoying debug statements, re-enable when we can disable all logging")
    @Test public void loadSkinClassShouldIgnoreBogusOrUnfindableSkins() {
        c.setSkin(s);
        ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlSkinTest$FooSkinWhichDoesntExist");
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

    @Test public void getUserAgentStylesheet() {

        org.junit.Assume.assumeNotNull(ControlSkinTest.class.getResource("ControlSkinTest.css"));

        ControlStub control = new ControlStub() {
            @Override public String getUserAgentStylesheet() {
                return "test/javafx/scene/control/ControlSkinTest.css";
            }
            {
                setId("get-user-agent-stylesheet-test");
            }
        };

        Scene scene = new Scene(control);
        control.applyCss();

        //  opacity set in ControlSkinTest.css
        assertEquals(.7, control.getOpacity(), 1E-6);
        assertNull(control.getSkin());

    }

    @Test public void getUserAgentStylesheetDoesNotOverrideUserSetValue() {

        org.junit.Assume.assumeNotNull(ControlSkinTest.class.getResource("ControlSkinTest.css"));

        ControlStub control = new ControlStub() {
            @Override public String getUserAgentStylesheet() {
                return "test/javafx/scene/control/ControlSkinTest.css";
            }
            {
                setId("get-user-agent-stylesheet-test");
                setOpacity(.3);
            }
        };
        Scene scene = new Scene(control);
        control.applyCss();

        //  opacity set by setOpacity
        assertEquals(.3, control.getOpacity(), 1E-6);
        assertNull(control.getSkin());

    }

    @Test public void getUserAgentStylesheetInlineStylePrevails() {

        org.junit.Assume.assumeNotNull(ControlSkinTest.class.getResource("ControlSkinTest.css"));

        ControlStub control = new ControlStub() {
            @Override public String getUserAgentStylesheet() {
                return "test/javafx/scene/control/ControlSkinTest.css";
            }
            {
                setId("get-user-agent-stylesheet-test");
                setStyle("-fx-opacity: 42%;");
            }
        };

        Scene scene = new Scene(control);
        control.applyCss();

        //  opacity set by setStyle
        assertEquals(.42, control.getOpacity(), 1E-6);
        assertNull(control.getSkin());

    }

    @Test public void getUserAgentStylesheetWithSkin() {

        org.junit.Assume.assumeNotNull(ControlSkinTest.class.getResource("ControlSkinTest.css"));

        ControlStub control = new ControlStub() {
            @Override public String getUserAgentStylesheet() {
                return "test/javafx/scene/control/ControlSkinTest.css";
            }
            @Override protected Skin<ControlStub> createDefaultSkin() {
                assert false : "createDefaultSkin should not be called";
                return null;
            }
            {
                setId("get-user-agent-stylesheet-test-with-skin");
            }
        };
        control.setSkin(new SkinStub<>(control));

        Scene scene = new Scene(control);
        control.applyCss();

        //  opacity set in ControlSkinTest.css
        assertEquals(.85, control.getOpacity(), 1E-6);
        assertEquals(SkinStub.class, control.getSkin().getClass());

    }

}
