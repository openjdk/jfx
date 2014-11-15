package com.sun.javafx.css;


import com.sun.javafx.css.Declaration;
import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Selector;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.Stylesheet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.scene.paint.Color;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.PropertyPermission;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Created by dgrieve on 9/17/14.
 */
public class StylesheetWithSecurityManagerTest {

    static final Styleable styleable = new Styleable() {
        @Override
        public String getTypeSelector() {
            return "*";
        }

        @Override
        public String getId() {
            return null;
        }

        ObservableList<String> styleClasses = FXCollections.observableArrayList("root");
        @Override
        public ObservableList<String> getStyleClass() {
            return styleClasses;
        }

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return null;
        }

        @Override
        public Styleable getStyleableParent() {
            return null;
        }

        @Override
        public ObservableSet<PseudoClass> getPseudoClassStates() {
            return FXCollections.<PseudoClass>emptyObservableSet();
        }
    };

    @Test
    public void testRT_38395() throws Exception {

        System.setSecurityManager(new TestSecurityManager());
        Stylesheet stylesheet = StyleManager.loadStylesheet("com/sun/javafx/scene/control/skin/modena/modena.css");
        assertNotNull(stylesheet);

        Color base = null;
        for(Rule rule : stylesheet.getRules()) {
            for (Selector s : rule.getSelectors()) {
                if (s.applies(styleable)) {
                    for(Declaration decl : rule.getDeclarations()) {
                        if ("-fx-base".equals(decl.getProperty())) {
                            base = (Color)decl.getParsedValue().convert(null);
                        }
                    }
                }
            }
        }
        assertNotNull(base);
        Color expected = Color.web("#ececec");
        assertEquals(expected.getRed(), base.getGreen(), 1E-6);
        assertEquals(expected.getGreen(), base.getGreen(), 1E-6);
        assertEquals(expected.getBlue(), base.getBlue(), 1E-6);
    }

    @Test
    public void testRT_38395_the_sequel() throws Exception {

        System.setSecurityManager(new TestSecurityManager());
        Stylesheet stylesheet = StyleManager.loadStylesheet("com/sun/javafx/css/StylesheetTest.css");
        assertNotNull(stylesheet);

        Color hoverBase = null;
        for(Rule rule : stylesheet.getRules()) {
            for (Selector s : rule.getSelectors()) {
                if (s.applies(styleable)) {
                    for(Declaration decl : rule.getDeclarations()) {
                        if ("-fx-hover-base".equals(decl.getProperty())) {
                            hoverBase = (Color)decl.getParsedValue().convert(null);
                        }
                    }
                }
            }
        }
        assertNotNull(hoverBase);
        assertEquals(Color.YELLOW.getRed(), hoverBase.getRed(), 1E-6);
        assertEquals(Color.YELLOW.getGreen(), hoverBase.getGreen(), 1E-6);
        assertEquals(Color.YELLOW.getBlue(), hoverBase.getBlue(), 1E-6);
    }

    //
    // The code in URLConverter that this attempts to test only checks to see whether or not there is a SecurityManager.
    //
    static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            return;
        }
    }
}
