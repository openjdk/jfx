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

package test.com.sun.javafx.css;

import javafx.css.Declaration;
import javafx.css.Rule;
import javafx.css.Selector;
import com.sun.javafx.css.StyleManager;
import javafx.css.Stylesheet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.scene.paint.Color;
import org.junit.Ignore;
import org.junit.Test;

import java.security.Permission;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

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

    @SuppressWarnings("removal")
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

    @SuppressWarnings("removal")
    @Test
    public void testRT_38395_import_local() throws Exception {
        System.setSecurityManager(new TestSecurityManager());
        Stylesheet stylesheet = StyleManager.loadStylesheet("test/com/sun/javafx/css/StylesheetTest_importLocal.css");
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
        Color expected = Color.web("#cccccc");
        assertEquals(expected.getRed(), base.getGreen(), 1E-6);
        assertEquals(expected.getGreen(), base.getGreen(), 1E-6);
        assertEquals(expected.getBlue(), base.getBlue(), 1E-6);
    }

    //
    // The code in URLConverter that this attempts to test only checks to see whether or not there is a SecurityManager.
    //
    @SuppressWarnings("removal")
    static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            return;
        }
    }
}
